package org.sentinel.nmapservice.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.WaitResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.nmapservice.exception.ContainerExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DockerService {

    @Value("${docker.host}")
    private String dockerHost;

    @Value("${nmap.scan-timeout-minutes}")
    private int scanTimeout;

    private DockerClient dockerClient;

    @PostConstruct
    public void init() {
        log.info("Initializing Docker client -> {}", dockerHost);

        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofMinutes(5))
                .build();

        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);

        try {
            dockerClient.pingCmd().exec();
            log.info("Successfully connected to Docker daemon at {}", dockerHost);
        } catch (Exception e) {
            log.error("Cannot reach Docker daemon at {}. " +
                    "Make sure the Docker socket is mounted", dockerHost);
            throw new RuntimeException("Docker connection failed: " + dockerHost, e);
        }
    }

    public String startContainer(String imageName, String containerName, List<String> command) {
        String containerId = null;

        try {
            // Step 1: Pull image if not cached
            try {
                dockerClient.inspectImageCmd(imageName).exec();
                log.info("Image already cached: {}", imageName);
            } catch (NotFoundException e) {
                log.warn("Image not cached, pulling {}...", imageName);
                dockerClient.pullImageCmd(imageName)
                        .withPlatform("linux/amd64")
                        .exec(new PullImageResultCallback())
                        .awaitCompletion();
                log.info("Image pulled successfully");
            }

            // Step 2: Host config : This explains the detailed info.
            // Capabilities granted to the nmap *child* container only, NOT to nmap-service itself (nmap-service has no cap_add in docker-compose):
            //
            // NET_RAW is required for nmap SYN (-sS) and UDP (-sU) scans, which use raw sockets. Without it nmap falls back to connect scans (-sT) and reports degraded results.
            //
            // Explicitly dropped:
            //   NET_ADMIN is not needed. nmap does not configure interfaces, routing tables, or firewall rules. Granting it would allow the nmap container to modify the host network stack.
            //
            // No `withUser("root")` : the instrumentisto/nmap image runs nmap as root by default because it owns the binary; granting NET_RAW is sufficient for raw socket access and is the minimal privilege needed.

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withAutoRemove(false)
                    .withNetworkMode("host")
                    .withCapAdd(Capability.NET_RAW)
                    .withCapDrop(Capability.NET_ADMIN);

            // Step 3: Create container — no explicit .withUser(root) call.
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withTty(false)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(command)
                    .exec();

            containerId = container.getId();
            log.info("Container created: {}", containerId);

            // Step 4: Start
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container started, waiting for nmap to finish...");

            // Step 5: Wait for completion
            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            boolean completed = dockerClient.waitContainerCmd(containerId)
                    .exec(waitCallback)
                    .awaitCompletion(scanTimeout, TimeUnit.MINUTES);

            if (!completed) {
                throw new ContainerExecutionException(
                        "Scan timed out after " + scanTimeout + " minutes");
            }

            WaitResponse waitResponse = waitCallback.getLastResponse();
            int exitCode = waitResponse != null ? waitResponse.getStatusCode() : -1;
            log.info("Container finished with exit code: {}", exitCode);

            // Step 6: Collect output
            LogToStringCallback logCallback = new LogToStringCallback();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(logCallback)
                    .awaitCompletion();

            String logs = logCallback.getLogs();
            log.info("Scan output received: {} chars", logs.length());

            // Step 7: Cleanup
            cleanupContainer(containerId);

            return logs;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (containerId != null) cleanupContainer(containerId);
            throw new ContainerExecutionException("Container operation interrupted", e);

        } catch (ContainerExecutionException e) {
            if (containerId != null) cleanupContainer(containerId);
            throw e;

        } catch (Exception e) {
            log.error("Error running container: {}", e.getMessage(), e);
            if (containerId != null) cleanupContainer(containerId);
            throw new ContainerExecutionException("Failed to run nmap container", e);
        }
    }

    private void cleanupContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("Container cleaned up: {}", containerId);
        } catch (NotFoundException e) {
            log.warn("Container already removed: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to clean up container {}: {}", containerId, e.getMessage());
        }
    }
}