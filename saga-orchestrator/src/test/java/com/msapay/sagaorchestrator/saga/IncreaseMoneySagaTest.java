package com.msapay.sagaorchestrator.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msapay.common.SagaManager;
import com.msapay.sagaorchestrator.framework.SagaState;
import com.msapay.sagaorchestrator.framework.SagaStepStatus;
import com.msapay.sagaorchestrator.TaskResultProducer;
import com.msapay.sagaorchestrator.messaging.increaseMoneyEvent;
import com.msapay.sagaorchestrator.messaging.firmbankingEvent;
import com.msapay.sagaorchestrator.messaging.increaseMoneyStatus;
import com.msapay.sagaorchestrator.messaging.firmbankingStatus;
import com.msapay.sagaorchestrator.service.SagaStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;


import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncreaseMoneySagaTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;



    @Mock
    private TaskResultProducer taskResultProducer;

    @Mock
    private SagaState sagaState;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SagaStateService sagaStateService;

    @Mock
    private SagaManager sagaManager;

    private IncreaseMoneySaga saga;
    private UUID sagaId;

    @BeforeEach
    void setUp() throws Exception {
        sagaId = UUID.randomUUID();
        
        // Mock SagaState behavior
        when(sagaState.id()).thenReturn(sagaId);
        when(sagaState.payload()).thenReturn(objectMapper.createObjectNode());
        when(sagaState.currentStep()).thenReturn(null);
        
        saga = new IncreaseMoneySaga(eventPublisher, taskResultProducer, objectMapper, sagaStateService, sagaManager);
        saga.setSagaState(sagaState);
    }

    @Test
    void init_ShouldPublishSagaEvent() {
        // When
        saga.init();
        
        // Then
        verify(eventPublisher).publishEvent(any(SagaEvent.class));
    }

    @Test
    void onIncreaseMoneyEvent_ShouldUpdateStepStatus() {
        // Given
        UUID eventId = UUID.randomUUID();
        increaseMoneyEvent event = new increaseMoneyEvent(increaseMoneyStatus.COMPLETED, sagaId, eventId);
        
        // When
        saga.onIncreaseMoneyEvent(eventId, event);
        
        // Then
        verify(sagaState).updateStepStatus(anyString(), eq(SagaStepStatus.SUCCEEDED));
    }

    @Test
    void onFirmbankingEvent_ShouldUpdateStepStatus() {
        // Given
        UUID eventId = UUID.randomUUID();
        firmbankingEvent event = new firmbankingEvent(firmbankingStatus.COMPLETED, sagaId, eventId);
        
        // When
        saga.onFirmbankingEvent(eventId, event);
        
        // Then
        verify(sagaState).updateStepStatus(anyString(), eq(SagaStepStatus.SUCCEEDED));
    }
}
