package org.sentinel.nmap_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${topics.input-name}")
    private String inputTopic;

    @Value("${topics.output-name}")
    private String outputTopic;

    @Bean
    public NewTopic inputTopic() {
        return TopicBuilder
                .name(inputTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic outputTopic() {
        return TopicBuilder
                .name(outputTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}