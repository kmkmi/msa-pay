package com.msapay.remittance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class RemittanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RemittanceApplication.class, args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Received SIGINT (Ctrl+C)");
        }));
    }
}
