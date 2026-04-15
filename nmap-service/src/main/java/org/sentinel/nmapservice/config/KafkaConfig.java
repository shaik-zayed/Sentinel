package org.sentinel.nmapservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.sentinel.nmapservice.model.ScanCommandMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

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

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ScanCommandMessage> kafkaListenerContainerFactory(ConsumerFactory<String, ScanCommandMessage> cf) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, ScanCommandMessage>();
        factory.setConsumerFactory(cf);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
}