package com.msapay.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Properties;

public class OutboxScheduler {

    private final EntityManager entityManager;
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public OutboxScheduler(EntityManager entityManager, @Value("${kafka.clusters.bootstrapservers}") String bootstrapServers) {
        this.entityManager = entityManager;
        this.objectMapper = new ObjectMapper();

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        this.producer = new KafkaProducer<>(props);
    }

    @Scheduled(fixedDelay = 1000L)
    @Transactional
    public void publishEvents() {
        try {
            var outboxEvents = entityManager.createQuery("SELECT o FROM Outbox o ORDER BY o.timestamp ASC", Outbox.class)
                    .setMaxResults(50)
                    .getResultList();

            if (outboxEvents.isEmpty()) {
                return;
            }

            for (Outbox outbox : outboxEvents) {
                try {
                    ProducerRecord<String, String> record = new ProducerRecord<>(outbox.getAggregateType(), outbox.getAggregateId().toString(), outbox.getPayload());
                    producer.send(record, (metadata, exception) -> {
                        if (exception == null) {
                            try {
                                entityManager.remove(outbox);
                            } catch (Exception e) {
                                // Log error but don't fail the entire transaction
                                System.err.println("Failed to remove outbox event: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Failed to process outbox event: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("OutboxScheduler error: " + e.getMessage());
            // Don't rethrow to prevent scheduler from stopping
        }
    }
}
