package com.msapay.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
public class LoggingProducer {
    private final KafkaProducer<String, String> producer;
    private final String topic;
    public LoggingProducer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                           @Value("${logging.topic}")String topic) {

        // Producer Initialization ';'
        Properties props = new Properties();

        // kafka:29092
        props.put("bootstrap.servers", bootstrapServers);

        // "key:value"
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
    }

    // Kafka Cluster [key, value] Produce
    public void sendMessage(String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                log.debug("Message sent successfully. Offset: {}", metadata.offset());
            } else {
                log.error("Failed to send message: {}", exception.getMessage(), exception);
            }
        });
    }
}