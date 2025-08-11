package com.msapay.sagaorchestrator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.common.SagaManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaConfig {

    /**
     * ObjectMapper Bean 설정
     * SagaOrchestrator와 IncreaseMoneySaga에서 사용
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * SagaManager Bean 설정
     * SagaOrchestrator에서 사용
     */
    @Bean
    public SagaManager sagaManager() {
        return new SagaManager();
    }
}

