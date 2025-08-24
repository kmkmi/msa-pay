package com.msapay.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.transaction.Transactional;
import java.util.Properties;

@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    public OutboxScheduler(OutboxRepository outboxRepository, @Value("${kafka.clusters.bootstrapservers}") String bootstrapServers) {
        this.outboxRepository = outboxRepository;
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
            Pageable pageable = PageRequest.of(0, 50, Sort.by("timestamp").ascending());
            var outboxEvents = outboxRepository.findAll(pageable).getContent();

            if (outboxEvents.isEmpty()) {
                return;
            }

            for (Outbox outbox : outboxEvents) {
                try {
                    ProducerRecord<String, String> record = new ProducerRecord<>(outbox.getAggregateType(), outbox.getAggregateId().toString(), outbox.getPayload());
                    producer.send(record, (metadata, exception) -> {
                        if (exception == null) {
                            try {
                                outboxRepository.delete(outbox);
                            } catch (Exception e) {
                                // Log error but don't fail the entire transaction
                                log.error("Failed to remove outbox event: {}", e.getMessage(), e);
                            }
                        }
                    });
                } catch (Exception e) {
                    log.error("Failed to process outbox event: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("OutboxScheduler error: {}", e.getMessage(), e);
            // Don't rethrow to prevent scheduler from stopping
        }
    }
}
