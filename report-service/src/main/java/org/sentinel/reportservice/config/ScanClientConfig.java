package org.sentinel.reportservice.config;

import org.sentinel.reportservice.client.ScanClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.client.support.RestClientAdapter;

@Configuration
public class ScanClientConfig {

    @Value("${scan-service.base-url}")
    private String scanServiceBaseUrl;

    /**
     * PRIMARY RestClient builder
     * Used by Eureka and default Spring components
     */
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Load balanced builder
     * Used ONLY for scan-service
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    /**
     * Scan-service client using load balanced builder
     */
    @Bean
    public ScanClient scanClient(
            @Qualifier("loadBalancedRestClientBuilder")
            RestClient.Builder loadBalancedRestClientBuilder) {

        RestClient restClient = loadBalancedRestClientBuilder
                .baseUrl(scanServiceBaseUrl)
                .build();

        HttpServiceProxyFactory factory =
                HttpServiceProxyFactory
                        .builderFor(RestClientAdapter.create(restClient))
                        .build();

        return factory.createClient(ScanClient.class);
    }
}

