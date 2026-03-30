package org.sentinel.nmap_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ScanExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService scanExecutor() {
        return new ThreadPoolExecutor(
                5,               // corePoolSize  — concurrent scans
                20,                         // maximumPoolSize
                60L, TimeUnit.SECONDS,      // keepAliveTime for idle threads
                new LinkedBlockingQueue<>(100), // bounded queue — backpressure if overloaded
                new ThreadPoolExecutor.CallerRunsPolicy() // if queue full, slow down the consumer (safe fallback)
        );
    }
}