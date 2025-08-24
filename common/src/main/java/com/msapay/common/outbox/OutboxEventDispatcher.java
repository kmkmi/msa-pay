package com.msapay.common.outbox;

import javax.persistence.EntityManager;
import org.hibernate.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

@Component
@Slf4j
public class OutboxEventDispatcher {

    private final EntityManager entityManager;
    private final boolean removeAfterInsert = true; 

    @Autowired
    public OutboxEventDispatcher(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventListener
    @Transactional(propagation = REQUIRED)
    public void on(OutboxEvent<?, ?> event) {
        try (var session = entityManager.unwrap(Session.class)) {
            log.info("An exported event was found for type {}", event.type());

            // Unwrap to Hibernate session and save
            var outbox = new Outbox(event);
            session.persist(outbox);

            // Remove entity if the configuration deems doing so, leaving useful
            // for debugging
            if (removeAfterInsert) {
                session.remove(outbox);
            }
        }
    }
}
