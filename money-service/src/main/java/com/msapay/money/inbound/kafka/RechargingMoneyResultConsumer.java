package com.msapay.money.inbound.kafka;

import com.msapay.common.CountDownLatchManager;
import com.msapay.common.LoggingProducer;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.common.SubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RechargingMoneyResultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RechargingMoneyResultConsumer.class);
    
    private final KafkaConsumer<String, String> consumer;
    private final LoggingProducer loggingProducer;
    @NotNull
    private final CountDownLatchManager countDownLatchManager;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread consumerThread;
    
    public RechargingMoneyResultConsumer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                                         @Value("${task.result.topic}")String topic, 
                                         LoggingProducer loggingProducer, 
                                         CountDownLatchManager countDownLatchManager) {
        this.loggingProducer = loggingProducer;
        this.countDownLatchManager = countDownLatchManager;
        
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", "money-service-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "false");
        
        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));
        
        logger.info("RechargingMoneyResultConsumer initialized with topic: {}", topic);
        
        startConsumerThread();
    }
    
    private void startConsumerThread() {
        consumerThread = new Thread(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                logger.info("Starting Kafka consumer thread for task result processing");
                
                while (running.get()) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                        
                        for (ConsumerRecord<String, String> record : records) {
                            try {
                                processRecord(record, mapper);
                            } catch (Exception e) {
                                logger.error("Error processing record: {}", record.value(), e);
                            }
                        }
                    } catch (Exception e) {
                        if (running.get()) {
                            logger.error("Error during Kafka polling", e);
                        }
                    }
                }
            } finally {
                consumer.close();
                logger.info("Kafka consumer thread stopped");
            }
        });
        consumerThread.setName("RechargingMoneyResultConsumer-Thread");
        consumerThread.start();
    }
    
    private void processRecord(ConsumerRecord<String, String> record, ObjectMapper mapper) throws JsonProcessingException {
        logger.info("Received message: key={}, value={}", record.key(), record.value());
        
        RechargingMoneyTask task = mapper.readValue(record.value(), RechargingMoneyTask.class);
        List<SubTask> subTaskList = task.getSubTaskList();

        boolean taskResult = true;
        // validation membership
        // validation banking
        for (SubTask subTask : subTaskList) {
            // 한번만 실패해도 실패한 task 로 간주.
            if (subTask.getStatus().equals("fail")) {
                taskResult = false;
                logger.warn("Subtask failed: {}", subTask.getSubTaskName());
                break;
            }
        }

        try {
            // 모두 정상적으로 성공했다면
            if (taskResult) {
                logger.info("Task completed successfully: {}", task.getTaskID());
                this.loggingProducer.sendMessage(task.getTaskID(), "task success");
                this.countDownLatchManager.setDataForKey(task.getTaskID(), "success");
            } else {
                logger.warn("Task failed: {}", task.getTaskID());
                this.loggingProducer.sendMessage(task.getTaskID(), "task failed");
                this.countDownLatchManager.setDataForKey(task.getTaskID(), "failed");
            }

            this.countDownLatchManager.getCountDownLatch(task.getTaskID()).countDown();
            
            // 메시지 처리 성공 후 수동 커밋
            consumer.commitSync();
            logger.debug("Offset committed successfully for record: {}", record.offset());
            
        } catch (Exception e) {
            logger.error("Error processing record: {}", record.value(), e);
            // 처리 실패 시 커밋하지 않음 - 재처리를 위해
            throw e;
        }
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down RechargingMoneyResultConsumer");
        running.set(false);
        
        if (consumerThread != null && consumerThread.isAlive()) {
            consumerThread.interrupt();
            try {
                consumerThread.join(5000); // 5초 대기
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (consumer != null) {
            consumer.close();
        }
    }
}