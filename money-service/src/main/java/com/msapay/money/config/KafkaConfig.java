package com.msapay.money.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka 토픽 설정
 */
@Configuration
public class KafkaConfig {
    
    /**
     * 머니 도메인 이벤트 토픽 생성
     */
    @Bean
    public NewTopic moneyDomainEventsTopic() {
        return TopicBuilder.name("money-domain-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    /**
     * 머니 변경 요청 토픽 생성
     */
    @Bean
    public NewTopic moneyChangingRequestsTopic() {
        return TopicBuilder.name("money-changing-requests")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
