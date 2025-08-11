package com.msapay.payment.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"com.msapay.common", "com.msapay.payment"})
@EntityScan("com.msapay.payment.persistence")
@EnableJpaRepositories("com.msapay.payment.persistence")
public class PaymentConfig {
}

