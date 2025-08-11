package com.msapay.money.domain.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 머니 도메인 이벤트의 기본 클래스
 */
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class MoneyDomainEvent {
    
    private final String eventId;
    private final String aggregateId;
    private final String eventType;
    private final LocalDateTime timestamp;
    private final long version;
    
    protected MoneyDomainEvent(String aggregateId, String eventType, long version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.version = version;
    }
    
    public abstract String getEventType();
}
