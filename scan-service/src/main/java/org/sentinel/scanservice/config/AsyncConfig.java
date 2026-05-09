package org.sentinel.scanservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
public class AsyncConfig {

    /**
     * Dedicated thread pool for CVE enrichment.
     * Kept intentionally small — enrichment calls are I/O bound (NVD API)
     * and must not starve other async operations in the application.
     * <p>
     * Queue capacity of 50 means up to 50 enrichment jobs can wait.
     * Beyond that, CallerRunsPolicy runs the task on the Kafka consumer thread
     * as a backpressure mechanism — slowing ingestion rather than dropping work.
     */
    @Bean(name = "enrichmentExecutor")
    public Executor enrichmentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("enrichment-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated thread pool for NVD API calls within each enrichment job.
     * Bounded to prevent a single large scan from opening too many
     * concurrent HTTP connections to the NVD API.
     */
    @Bean(name = "nvdCallExecutor")
    public Executor nvdCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4); // hard cap — NVD unauthenticated limit is 5 requests/30s, will get an API key in future to get more requests.
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("nvd-call-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}