package com.msapay.money.service.port;

public interface UpdateMoneyChangingRequestStatusPort {
    
    /**
     * MoneyChangingRequest의 상태를 업데이트합니다.
     * 
     * @param uuid MoneyChangingRequest의 UUID
     * @param status 새로운 상태 (0: 요청, 1: 성공, 2: 실패)
     * @return 업데이트 성공 여부
     */
    boolean updateMoneyChangingRequestStatus(String uuid, int status);
}
