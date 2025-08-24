package com.msapay.money.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"com.msapay.common", "com.msapay.money"})
@EntityScan(basePackages = {"com.msapay.money.persistence", "com.msapay.common.outbox"})
@EnableJpaRepositories(basePackages = {"com.msapay.money.persistence", "com.msapay.common.outbox"})
public class MoneyConfig {
}

