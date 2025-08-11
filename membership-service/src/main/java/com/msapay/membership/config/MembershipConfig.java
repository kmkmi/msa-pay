package com.msapay.membership.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan({"com.msapay.common", "com.msapay.membership"})
@EntityScan("com.msapay.membership.persistence")
@EnableJpaRepositories("com.msapay.membership.persistence")
public class MembershipConfig {
}

