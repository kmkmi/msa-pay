package com.msapay.sagaorchestrator.framework;

public enum SagaStatus {
    STARTED, ABORTING, ABORTED, COMPLETED;

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isAborted() {
        return this == ABORTED;
    }
    
    public boolean isStarted() {
        return this == STARTED;
    }
    
    public boolean isAborting() {
        return this == ABORTING;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == ABORTED;
    }
}
