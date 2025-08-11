package com.msapay.common.outbox;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

/**
 * Entity class representing an Outbox Event to store domain events in a persistent store.
 */
@Entity
@Table(name = "outboxevent")
@NoArgsConstructor(access = PRIVATE)
public class Outbox implements Serializable {

    @Id
    private UUID id;
    private Instant timestamp;

    @Column(name = "aggregateid")
    private String aggregateId;

    @Column(name = "aggregatetype")
    private String aggregateType;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    Outbox(OutboxEvent<?, ?> event) {
        requireNonNull(event, "event cannot be null");
        this.id = UUID.randomUUID();
        this.timestamp = requireNonNull(event.timestamp(), "issuedOn cannot be null");
        this.aggregateId = requireNonNull(event.aggregateId(), "aggregateId cannot be null").toString();
        this.aggregateType = requireNonNull(event.aggregateType(), "aggregateType cannot be null");
        this.type = requireNonNull(event.type(), "type cannot be null");
        this.payload = requireNonNull(event.payload(), "payload cannot be null").toString();
    }
    
    public UUID getId() {
        return id;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public String getType() {
        return type;
    }
    
    public String getPayload() {
        return payload;
    }
}
