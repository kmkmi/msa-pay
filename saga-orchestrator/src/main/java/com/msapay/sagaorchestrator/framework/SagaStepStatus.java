package com.msapay.sagaorchestrator.framework;

public enum SagaStepStatus {
    STARTED, FAILED, SUCCEEDED, COMPENSATING, COMPENSATED;
    
    public boolean isFailed() {
        return FAILED == this;
    }
}
