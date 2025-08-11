package com.msapay.sagaorchestrator.framework;

import java.util.UUID;

/**
 * Saga 인터페이스
 */
public interface Saga {

    /**
     * Saga 초기화 메서드
     */
    void init();

    /**
     * 이벤트 처리 메서드
     */
    default void ensureProcessed(UUID eventId, Runnable callback) {
        // 기본 구현은 빈 구현으로 둠
        callback.run();
    }

    enum PayloadType {
        REQUEST, CANCEL
    }
}

