package org.sentinel.scanservice.config;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class NvdRestTemplateConfig {

    @Bean
    public RateLimiter nvdRateLimiter() {
        // NVD public API limit:
        // 5 requests per 30 seconds
        return RateLimiter.create(5.0 / 30.0);
    }


    @Bean(name = "nvdRestTemplate")
    public RestTemplate nvdRestTemplate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(10);
        connectionManager.setDefaultMaxPerRoute(4);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(15))
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);

        restTemplate.setInterceptors(List.of((ClientHttpRequestInterceptor) (request, body, execution) -> {
            request.getHeaders().set("User-Agent", "SentinelScanService/1.0");
            return execution.execute(request, body);
        }));

        return restTemplate;
    }
}