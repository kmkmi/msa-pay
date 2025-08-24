package com.msapay.sagaorchestrator.framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static com.msapay.sagaorchestrator.framework.SagaStepStatus.*;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "sagastate")
@TypeDef(name = "json", typeClass = JsonType.class)
@NoArgsConstructor(access = PRIVATE, force = true) // JPA compliant
@Accessors(fluent = true)
@Slf4j
public class SagaState {
    
    @Id
    private UUID id;

    @Column(name = "type", nullable = false)
    private String type;

    @Type(type = "json")
    @Column(columnDefinition = "json", nullable = false)
    private JsonNode payload;

    @Column(name = "current_step")
    private String currentStep;

    @Type(type = "json")
    @Column(columnDefinition = "json", nullable = false)
    private ObjectNode stepStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", nullable = false)
    private SagaStatus sagaStatus;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SagaState(String sagaType, JsonNode payload) {
        this.id = UUID.randomUUID();
        this.type = sagaType;
        this.payload = payload;
        this.sagaStatus = SagaStatus.STARTED;
        this.stepStatus = JsonNodeFactory.instance.objectNode();
        this.currentStep = "INIT"; // 기본값 설정
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

    public void updateStepStatus(String step, SagaStepStatus sagaStepStatus) {
        if (step == null || step.trim().isEmpty()) {
            throw new IllegalArgumentException("Step name cannot be null or empty");
        }
        if (sagaStepStatus == null) {
            throw new IllegalArgumentException("SagaStepStatus cannot be null");
        }
        
        if (this.stepStatus == null) {
            this.stepStatus = JsonNodeFactory.instance.objectNode();
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
        var stepStatusSet = stepStatusToSet();
        log.info("advanceSagaStatus called - current stepStatusSet: {}, current sagaStatus: {}", 
            stepStatusSet, sagaStatus);
        
        if (stepStatusSet.isEmpty()) {
            sagaStatus = SagaStatus.STARTED;
            log.info("Step status set is empty, setting sagaStatus to STARTED");
            return;
        }
        
        var bitmask = stepStatusSet.stream()
                .mapToInt(status -> 1 << status.ordinal())
                .reduce(0, (a, b) -> a | b);
        
        log.info("Calculated bitmask: {} for stepStatusSet: {}", bitmask, stepStatusSet);

        // INIT 단계를 제외한 핵심 단계들만 확인
        Set<String> coreSteps = Set.of("FIRMBANKING", "INCREASE_MONEY");
        boolean allCoreStepsSucceeded = true;
        
        for (String stepName : coreSteps) {
            if (stepStatus.has(stepName)) {
                String stepStatusText = stepStatus.get(stepName).asText();
                if (!"SUCCEEDED".equals(stepStatusText)) {
                    allCoreStepsSucceeded = false;
                    break;
                }
            } else {
                allCoreStepsSucceeded = false;
                break;
            }
        }
        
        // STARTED 상태가 있는지 확인
        boolean hasStarted = stepStatusSet.contains(STARTED);
        // FAILED나 COMPENSATED 상태가 있는지 확인
        boolean hasFailedOrCompensated = stepStatusSet.stream().anyMatch(status -> status == FAILED || status == COMPENSATED);
        // COMPENSATING 상태가 있는지 확인
        boolean hasCompensating = stepStatusSet.contains(COMPENSATING);
        
        log.info("Status analysis - allCoreStepsSucceeded: {}, hasStarted: {}, hasFailedOrCompensated: {}, hasCompensating: {}", 
            allCoreStepsSucceeded, hasStarted, hasFailedOrCompensated, hasCompensating);
        
        if (allCoreStepsSucceeded) {
            sagaStatus = SagaStatus.COMPLETED;
            log.info("All core steps succeeded, setting sagaStatus to COMPLETED");
        } else if (hasStarted && !hasFailedOrCompensated) {
            sagaStatus = SagaStatus.STARTED;
            log.info("Some steps still in progress, keeping sagaStatus as STARTED");
        } else if (hasFailedOrCompensated) {
            sagaStatus = SagaStatus.ABORTED;
            log.info("Some steps failed/compensated, setting sagaStatus to ABORTED");
        } else if (hasCompensating) {
            sagaStatus = SagaStatus.ABORTING;
            log.info("Some steps are compensating, setting sagaStatus to ABORTING");
        } else {
            sagaStatus = SagaStatus.STARTED;
            log.info("Default case, setting sagaStatus to STARTED");
        }
        
        log.info("Final sagaStatus set to: {}", sagaStatus);
    }

    private EnumSet<SagaStepStatus> stepStatusToSet() {
        EnumSet<SagaStepStatus> allStatus = EnumSet.noneOf(SagaStepStatus.class);
        
        log.info("stepStatusToSet called - current stepStatus: {}", stepStatus);
        
        if (stepStatus == null || stepStatus.isEmpty()) {
            log.info("stepStatus is null or empty, returning empty set");
            return allStatus;
        }
        
        stepStatus.fields()
                .forEachRemaining(entry -> {
                    try {
                        String statusText = entry.getValue().asText();
                        if (statusText != null && !statusText.trim().isEmpty()) {
                            SagaStepStatus status = SagaStepStatus.valueOf(statusText);
                            allStatus.add(status);
                            log.info("Added step status: {} for step: {}", status, entry.getKey());
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid saga step status: {}", entry.getValue().asText());
                    }
                });

        log.info("Final stepStatusSet: {}", allStatus);
        return allStatus;
    }

    public SagaStatus sagaStatus() {
        return sagaStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public ObjectNode getStepStatus() {
        return stepStatus;
    }
    
    public void setStepStatus(ObjectNode stepStatus) {
        this.stepStatus = stepStatus;
    }
    
    public void setSagaStatus(SagaStatus sagaStatus) {
        this.sagaStatus = sagaStatus;
    }
}