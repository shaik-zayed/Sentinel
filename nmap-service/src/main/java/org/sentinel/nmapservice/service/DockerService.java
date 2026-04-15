package org.sentinel.nmapservice.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
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

        // Fail fast: verify connection on startup so you know immediately if remote is unreachable
        try {
            dockerClient.pingCmd().exec();
            log.info("Successfully connected to Docker daemon at {}", dockerHost);
        } catch (Exception e) {
            log.error("Cannot reach Docker daemon at {}. " +
                    "Make sure the remote host has TCP port open (see setup guide).", dockerHost);
            throw new RuntimeException("Docker connection failed: " + dockerHost, e);
        }
    }

    public String startContainer(String imageName, String containerName, List<String> command) {
        String containerId = null;

        try {
            // Step 1: Pull image on remote host if not already cached
            try {
                dockerClient.inspectImageCmd(imageName).exec();
                log.info("Image already cached on remote host: {}", imageName);
            } catch (NotFoundException e) {
                log.warn("Image not on remote host, pulling {}...", imageName);
                dockerClient.pullImageCmd(imageName)
                        .withPlatform("linux/amd64")
                        .exec(new PullImageResultCallback())
                        .awaitCompletion();
                log.info("Image pulled on remote host successfully");
            }

            // Step 2: Host config
            // Remote is Linux so host networking works — nmap gets direct network access
            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withAutoRemove(false)
                    .withNetworkMode("host");

            // Step 3: Create container on remote Docker host
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withUser("root")
                    .withTty(false)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(command)
                    .exec();

            containerId = container.getId();
            log.info("Container created on remote host: {}", containerId);

            // Step 4: Start the container
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container started on remote host, waiting for nmap to finish...");

            // Step 5: Wait for completion (5 min max)
            WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
            boolean completed = dockerClient.waitContainerCmd(containerId)
                    .exec(waitCallback)
                    .awaitCompletion(scanTimeout, TimeUnit.MINUTES);

            if (!completed) {
                throw new ContainerExecutionException("Scan timed out after 5 minutes");
            }

            WaitResponse waitResponse = waitCallback.getLastResponse();
            int exitCode = waitResponse != null ? waitResponse.getStatusCode() : -1;
            log.info("Container finished with exit code: {}", exitCode);

            // Step 6: Stream logs back from remote container to local nmap-service
            LogToStringCallback logCallback = new LogToStringCallback();
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .exec(logCallback)
                    .awaitCompletion();

            String logs = logCallback.getLogs();
            log.info("Scan output received from remote host: {} chars", logs.length());

            // Step 7: Cleanup remote container
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
            log.error("Error running container on remote Docker host: {}", e.getMessage(), e);
            if (containerId != null) cleanupContainer(containerId);
            throw new ContainerExecutionException("Failed to run container on remote host", e);
        }
    }

    private void cleanupContainer(String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();
            log.info("Remote container cleaned up: {}", containerId);
        } catch (NotFoundException e) {
            log.warn("Remote container already removed: {}", containerId);
        } catch (Exception e) {
            log.error("Failed to clean up remote container {}: {}", containerId, e.getMessage());
        }
    }
}