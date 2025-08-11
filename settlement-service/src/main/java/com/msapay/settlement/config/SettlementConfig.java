package com.msapay.settlement.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"com.msapay.common", "com.msapay.settlement"})
@EntityScan("com.msapay.settlement.persistence")
@EnableJpaRepositories("com.msapay.settlement.persistence")
public class SettlementConfig {
}

