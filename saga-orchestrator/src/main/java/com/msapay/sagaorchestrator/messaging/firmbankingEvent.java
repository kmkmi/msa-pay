package com.msapay.sagaorchestrator.messaging;

import com.msapay.sagaorchestrator.framework.SagaStepStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class firmbankingEvent {
    private final firmbankingStatus status;
    private final UUID sagaId;
    private final UUID eventId;
    
    public SagaStepStatus toSagaStepStatus() {
        switch (status) {
            case COMPLETED:
                return SagaStepStatus.SUCCEEDED;
            case FAILED:
                return SagaStepStatus.FAILED;
            default:
                return SagaStepStatus.STARTED;
        }
    }
}