package org.sentinel.scan_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sentinel.scan_service.model.ScanCommandMessage;
import org.sentinel.scan_service.model.ScanResultMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;

    @Value("${topics.output-name}")
    private String outputTopic;

    @Value("${topics.input-name}")
    private String inputTopic;

//    @Bean
//    public ProducerFactory<String, ScanCommandMessage> scanCommandProducerFactory() {
//        Map<String, Object> config = new HashMap<>();
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
//        config.put(ProducerConfig.ACKS_CONFIG, "all");
//        config.put(ProducerConfig.RETRIES_CONFIG, 3);
//        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
//        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
//        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
//        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//    @Bean
//    public KafkaTemplate<String, ScanCommandMessage> kafkaTemplate() {
//        return new KafkaTemplate<>(scanCommandProducerFactory());
//    }
//
//    @Bean
//    public ConsumerFactory<String, ScanResultMessage> scanResultConsumerFactory() {
//        Map<String, Object> config = new HashMap<>();
//        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
//        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class.getName());
//        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "org.sentinel.scan_service.model");
//        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, ScanResultMessage.class.getName());
//        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
//        config.put(ConsumerConfig.GROUP_ID_CONFIG, "scan-service-results");
//        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
//        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
//
//        return new DefaultKafkaConsumerFactory<>(config);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, ScanResultMessage>
//    kafkaListenerContainerFactory() {
//
//        ConcurrentKafkaListenerContainerFactory<String, ScanResultMessage> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(scanResultConsumerFactory());
//        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//        factory.setConcurrency(3);
//
//        return factory;
//    }

    @Bean
    public NewTopic scanCommandTopic() {
        return TopicBuilder
                .name(outputTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }

    @Bean
    public NewTopic scanResultTopic() {
        return TopicBuilder
                .name(inputTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}