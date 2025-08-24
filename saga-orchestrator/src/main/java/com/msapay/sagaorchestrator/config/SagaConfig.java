package com.msapay.sagaorchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.common.outbox.OutboxScheduler;
import com.msapay.common.outbox.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableScheduling
@EntityScan(basePackages = {"com.msapay.sagaorchestrator.framework", "com.msapay.common.outbox"})
@EnableJpaRepositories(basePackages = {"com.msapay.sagaorchestrator.repository", "com.msapay.common.outbox"})
public class SagaConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }



    @Bean
    OutboxScheduler outboxScheduler(OutboxRepository outboxRepository, @Value("${kafka.clusters.bootstrapservers}") String bootstrapServers) {
        return new OutboxScheduler(outboxRepository, bootstrapServers);
    }
}

