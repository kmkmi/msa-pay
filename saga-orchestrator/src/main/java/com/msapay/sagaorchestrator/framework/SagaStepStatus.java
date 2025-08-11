package com.msapay.sagaorchestrator.framework;

public enum SagaStepStatus {
    STARTED, FAILED, SUCCEEDED, COMPENSATING, COMPENSATED;

    public boolean isSucceeded() {
        return SUCCEEDED == this;
    }

    public boolean isFailedOrCompensated() {
        return this == FAILED || this == COMPENSATED;
    }
    
    public boolean isStarted() {
        return STARTED == this;
    }
    
    public boolean isCompensating() {
        return COMPENSATING == this;
    }
    
    public boolean isFailed() {
        return FAILED == this;
    }
    
    public boolean isCompensated() {
        return COMPENSATED == this;
    }
}
