package com.msapay.money.inbound.kafka;

import com.msapay.common.CountDownLatchManager;
import com.msapay.common.LoggingProducer;
import com.msapay.common.RechargingMoneyTask;
import com.msapay.common.SubTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.money.domain.RegisteredBankAccountAggregateIdentifier;
import com.msapay.money.service.port.BankingServicePort;
import com.msapay.money.service.port.GetMembershipPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class RechargingMoneyResultConsumer {
    
    private final KafkaConsumer<String, String> consumer;
    private final LoggingProducer loggingProducer;
    @NotNull
    private final CountDownLatchManager countDownLatchManager;
    private final GetMembershipPort membershipPort;
    private final BankingServicePort bankingServicePort;
    
    private final KafkaProducer<String, String> resultProducer;
    private final String resultTopic;
    
    private final AtomicBoolean running = new AtomicBoolean(true);
    private Thread consumerThread;
    
    public RechargingMoneyResultConsumer(@Value("${kafka.clusters.bootstrapservers}") String bootstrapServers,
                                         @Value("${task.topic}")String topic,
                                         @Value("${task.result.topic}")String resultTopic,
                                         LoggingProducer loggingProducer,
                                         CountDownLatchManager countDownLatchManager, GetMembershipPort membershipPort, BankingServicePort bankingServicePort) {
        this.loggingProducer = loggingProducer;
        this.countDownLatchManager = countDownLatchManager;
        this.resultTopic = resultTopic; // msa.task.result.topic
        this.membershipPort = membershipPort;
        this.bankingServicePort = bankingServicePort;

        // Consumer 설정 - 태스크를 받기 위해 msa.task.topic 구독
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", bootstrapServers);
        consumerProps.put("group.id", "money-service-group");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("enable.auto.commit", "false");
        
        this.consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(topic)); // msa.task.topic 구독
        
        // Producer 설정 - 결과 전송용
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", bootstrapServers);
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        this.resultProducer = new KafkaProducer<>(producerProps);
        
        log.info("RechargingMoneyResultConsumer initialized - consuming from: {}, producing to: {}", topic, this.resultTopic);
        
        startConsumerThread();
    }
    
    private void startConsumerThread() {
        consumerThread = new Thread(() -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                log.info("Starting Kafka consumer thread for task processing");
                
                while (running.get()) {
                    try {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                        
                        for (ConsumerRecord<String, String> record : records) {
                            try {
                                processRecord(record, mapper);
                            } catch (Exception e) {
                                log.error("Error processing record: {}", record.value(), e);
                            }
                        }
                    } catch (Exception e) {
                        if (running.get()) {
                            log.error("Error during Kafka polling", e);
                        }
                    }
                }
            } finally {
                consumer.close();
                log.info("Kafka consumer thread stopped");
            }
        });
        consumerThread.setName("RechargingMoneyResultConsumer-Thread");
        consumerThread.start();
    }
    
    private void processRecord(ConsumerRecord<String, String> record, ObjectMapper mapper) throws JsonProcessingException {
        log.info("Received task message: key={}, value={}", record.key(), record.value());
        
        RechargingMoneyTask task = mapper.readValue(record.value(), RechargingMoneyTask.class);
        List<SubTask> subTaskList = task.getSubTaskList();

        // 실제 태스크 검증 수행
        processSubTasks(task, subTaskList);
        
        // 전체 태스크 결과 판단
        boolean taskResult = evaluateTaskResult(subTaskList);
        
        try {
            // 결과에 따른 처리
            if (taskResult) {
                log.info("Task completed successfully: {}", task.getTaskID());
                this.loggingProducer.sendMessage(task.getTaskID(), "task success");
                this.countDownLatchManager.setDataForKey(task.getTaskID(), "success");
                
                // 결과를 msa.task.result.topic으로 전송
                sendTaskResult(task.getTaskID(), Map.of("status", "success", "taskId", task.getTaskID()));
            } else {
                log.warn("Task failed: {}", task.getTaskID());
                this.loggingProducer.sendMessage(task.getTaskID(), "task failed");
                this.countDownLatchManager.setDataForKey(task.getTaskID(), "failed");
                
                // 실패 결과를 msa.task.result.topic으로 전송
                sendTaskResult(task.getTaskID(), Map.of("status", "failed", "taskId", task.getTaskID()));
            }

            // CountDownLatch가 존재하는지 확인 후 countDown 호출
            CountDownLatch latch = this.countDownLatchManager.getCountDownLatch(task.getTaskID());
            if (latch != null) {
                latch.countDown();
                log.debug("CountDownLatch countDown called for task: {}", task.getTaskID());
            } else {
                log.warn("CountDownLatch not found for task: {}, skipping countDown", task.getTaskID());
            }
            
            // 메시지 처리 성공 후 수동 커밋
            consumer.commitSync();
            log.debug("Offset committed successfully for record: {}", record.offset());
            
        } catch (Exception e) {
            log.error("Error processing record: {}", record.value(), e);
            // 처리 실패 시 커밋하지 않음 - 재처리를 위해
            throw e;
        }
    }
    
    private void processSubTasks(RechargingMoneyTask task, List<SubTask> subTaskList) {
        log.info("Processing subTasks for task: {}", task.getTaskID());
        
        for (SubTask subTask : subTaskList) {
            try {
                String status = executeSubTask(subTask, task);
                subTask.setStatus(status);
                log.info("SubTask {} completed with status: {}", subTask.getSubTaskName(), status);
            } catch (Exception e) {
                log.error("SubTask {} failed: {}", subTask.getSubTaskName(), e.getMessage());
                subTask.setStatus("fail");
            }
        }
    }
    
    private String executeSubTask(SubTask subTask, RechargingMoneyTask task) {
        switch (subTask.getTaskType()) {
            case "membership":
                return validateMembership(task.getMembershipID());
            case "banking":
                return validateBankingAccount(task.getMembershipID(), task.getMoneyAmount());
            case "corpAccount":
                return validateCorpAccount();
            default:
                log.warn("Unknown task type: {}", subTask.getTaskType());
                return "fail";
        }
    }
    
    private String validateMembership(String membershipId) {
        try {
            if (!membershipPort.getMembership(membershipId).isValid()){
                return "fail";
            }
            log.info("Validating membership: {}", membershipId);
            return "success";
        } catch (Exception e) {
            log.error("Membership validation failed for: {}", membershipId, e);
            return "fail";
        }
    }
    
    private String validateBankingAccount(String membershipId, int moneyAmount) {
        try {
            RegisteredBankAccountAggregateIdentifier bankAccount = bankingServicePort.getRegisteredBankAccount(membershipId);
            if (null == bankAccount) {
                return "fail";
            }
            if(moneyAmount > bankingServicePort.getBanckAccountBalance(bankAccount.getBankName(), bankAccount.getBankAccountNumber())){
                log.info("Banking account balance check failed for membership: {}", membershipId);
                return "fail";
            }
            log.info("Validating banking account for membership: {}", membershipId);
            return "success";
        } catch (Exception e) {
            log.error("Banking account validation failed for membership: {}", membershipId, e);
            return "fail";
        }
    }

    private String validateCorpAccount() {
        try {
            if (!bankingServicePort.verifyCorpAccount()) {
                return "fail";
            }
            log.info("Validating corporate account");
            return "success";
        } catch (Exception e) {
            log.error("Corporate account validation failed", e);
            return "fail";
        }
    }
    
    private boolean evaluateTaskResult(List<SubTask> subTaskList) {
        for (SubTask subTask : subTaskList) {
            if ("fail".equals(subTask.getStatus())) {
                log.warn("Subtask failed: {}", subTask.getSubTaskName());
                return false;
            }
        }
        return true;
    }
    
    private void sendTaskResult(String taskId, Map<String, Object> result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResult = mapper.writeValueAsString(result);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(resultTopic, taskId, jsonResult);
            resultProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    log.info("Task result sent successfully to topic: {} for task: {}", resultTopic, taskId);
                } else {
                    log.error("Failed to send task result for task: {}", taskId, exception);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize task result for task: {}", taskId, e);
        }
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down RechargingMoneyResultConsumer");
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
        
        if (resultProducer != null) {
            resultProducer.close();
        }
    }
}