package com.msapay.sagaorchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.sagaorchestrator.saga.IncreaseMoneySaga;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

@Component
@Slf4j
public class TaskConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final IncreaseMoneySaga increaseMoneySaga;

    public TaskConsumer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                        @Value("${task.topic}") String topic,
                        IncreaseMoneySaga increaseMoneySaga) {
        this.increaseMoneySaga = increaseMoneySaga;
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "my-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false");
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        Thread consumerThread = new Thread(() -> {
            try {
                log.info("Starting Kafka consumer thread for topic: {}", topic);
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                    ObjectMapper mapper = new ObjectMapper();
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            log.debug("Processing message: {}", record.value());
                            RechargingMoneyTask task = mapper.readValue(record.value(), RechargingMoneyTask.class);
                            
                            // Saga 시작 및 ID 반환
                            String sagaId = this.increaseMoneySaga.beginIncreaseMoneySaga(task);
                            
                            log.info("Successfully processed task: {} with saga tracking, sagaId: {}", 
                                task.getTaskID(), sagaId);
                            
                            // 메시지 처리 성공 후 수동 커밋
                            consumer.commitSync();
                            log.debug("Offset committed successfully for record: {}", record.offset());
                                
                        } catch (JsonProcessingException e) {
                            log.error("Failed to deserialize message: {}", record.value(), e);
                        } catch (Exception e) {
                            log.error("Failed to process message: {}", record.value(), e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Consumer thread error", e);
            } finally {
                log.info("Closing Kafka consumer");
                consumer.close();
            }
        });
        consumerThread.start();
    }
}
