package com.msapay.common.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.persistence.EntityManager;

@Configuration
@EntityScan("com.msapay.common.outbox")
@EnableScheduling
public class OutboxConfig {

    @Bean
    OutboxEventDispatcher outboxEventDispatcher(EntityManager entityManager) {
        return new OutboxEventDispatcher(entityManager, false);
    }

    @Bean
    OutboxScheduler outboxScheduler(EntityManager entityManager, @Value("${kafka.clusters.bootstrapservers}") String bootstrapServers) {
        return new OutboxScheduler(entityManager, bootstrapServers);
    }
}
