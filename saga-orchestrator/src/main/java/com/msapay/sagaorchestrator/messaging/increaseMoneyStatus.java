package com.msapay.sagaorchestrator.messaging;

import com.msapay.sagaorchestrator.framework.SagaStepStatus;

public enum increaseMoneyStatus {
    REQUESTED, CANCELLED, FAILED, COMPLETED;

    public SagaStepStatus toSagaStepStatus() {
        switch (this) {
            case CANCELLED:
                return SagaStepStatus.COMPENSATED;
            case COMPLETED:
            case REQUESTED:
                return SagaStepStatus.SUCCEEDED;
            case FAILED:
                return SagaStepStatus.FAILED;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}