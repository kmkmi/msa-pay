package com.msapay.common.outbox;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EntityScan("com.msapay.common.outbox")
@EnableScheduling
public class OutboxConfig {


}
