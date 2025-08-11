package com.msapay.remittance.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"com.msapay.common", "com.msapay.remittance"})
@EntityScan("com.msapay.remittance.persistence")
@EnableJpaRepositories("com.msapay.remittance.persistence")
public class RemittanceConfig {
}

