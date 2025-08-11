package com.msapay.money.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.common.SagaManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SagaConfig {

    @Bean
    public SagaManager sagaManager() {
        return new SagaManager();
    }
}
