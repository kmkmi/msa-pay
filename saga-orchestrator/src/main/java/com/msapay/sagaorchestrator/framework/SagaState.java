package com.msapay.sagaorchestrator.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.msapay.sagaorchestrator.framework.SagaStepStatus.*;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "sagastate")
@TypeDef(name = "jsonb-node", typeClass = JsonNodeBinaryType.class)
@NoArgsConstructor(access = PRIVATE, force = true) // JPA compliant
public class SagaState {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaState.class);

    @Id
    private UUID id;

    @Version
    private int version;

    private String type;

    @Type(type = "jsonb-node")
    @Column(columnDefinition = "jsonb")
    private JsonNode payload;

    private String currentStep;

    @Type(type = "jsonb-node")
    @Column(columnDefinition = "jsonb")
    private ObjectNode stepStatus;

    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;
    
    private LocalDateTime createdAt;

    public SagaState(String sagaType, JsonNode payload) {
        this.id = UUID.randomUUID();
        this.type = sagaType;
        this.payload = payload;
        this.sagaStatus = SagaStatus.STARTED;
        this.stepStatus = JsonNodeFactory.instance.objectNode();
        this.createdAt = LocalDateTime.now();
    }

    public UUID id() {
        return id;
    }

    public JsonNode payload() {
        return payload;
    }

    public String currentStep() {
        return currentStep;
    }

    public void currentStep(String currentStep) {
        this.currentStep = currentStep;
    }
    
    public String type() {
        return type;
    }
    
    public void setSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }

    public void updateStepStatus(String step, SagaStepStatus sagaStepStatus) {
        if (step == null || step.trim().isEmpty()) {
            throw new IllegalArgumentException("Step name cannot be null or empty");
        }
        if (sagaStepStatus == null) {
            throw new IllegalArgumentException("SagaStepStatus cannot be null");
        }
        
        this.stepStatus.put(step, sagaStepStatus.name());
    }

    /**
     * Following SagaSteps To SagaStatus mapping:
     * 1. SUCCEEDED -> COMPLETED
     * 2. STARTED, SUCCEEDED -> STARTED
     * 3. FAILED, COMPENSATED -> ABORTED
     * 4. COMPENSATING, other -> ABORTING
     */
    public void advanceSagaStatus() {
        var bitmask = stepStatusToSet().stream()
                .mapToInt(status -> 1 << status.ordinal())
                .reduce(0, (a, b) -> a | b);

        if ((bitmask & (1 << SUCCEEDED.ordinal())) == bitmask) {
            sagaStatus = SagaStatus.COMPLETED;
        } else if ((bitmask & ((1 << STARTED.ordinal()) | (1 << SUCCEEDED.ordinal()))) == bitmask) {
            sagaStatus = SagaStatus.STARTED;
        } else if ((bitmask & ((1 << FAILED.ordinal()) | (1 << COMPENSATED.ordinal()))) == bitmask) {
            sagaStatus = SagaStatus.ABORTED;
        } else {
            sagaStatus = SagaStatus.ABORTING;
        }
    }

    private EnumSet<SagaStepStatus> stepStatusToSet() {
        EnumSet<SagaStepStatus> allStatus = EnumSet.noneOf(SagaStepStatus.class);
        stepStatus.fields()
                .forEachRemaining(entry -> {
                    try {
                        String statusText = entry.getValue().asText();
                        if (statusText != null && !statusText.trim().isEmpty()) {
                            allStatus.add(SagaStepStatus.valueOf(statusText));
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid saga step status: {}", entry.getValue().asText());
                    }
                });

        return allStatus;
    }

    public SagaStatus sagaStatus() {
        return sagaStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}