# MSA-Pay : MSA로 구현한 간편 결제 시스템

## 프로젝트 개요

MSA-Pay는 마이크로서비스 아키텍처(MSA)를 기반으로 한 결제 및 송금 시스템입니다. 각 서비스는 독립적으로 운영되며, Kafka를 통한 비동기 통신과 Saga 패턴을 통한 분산 트랜잭션 관리를 구현했습니다.

## 시스템 아키텍처


## 서비스 구성

- **Membership Service (8081)**: 회원 관리
- **Banking Service (8082)**: 은행 연동
- **Money Service (8083)**: 머니 관리
- **Remittance Service (8084)**: 송금 서비스
- **Payment Service (8085)**: 결제 서비스
- **Settlement Service (8086)**: 정산 서비스
- **Saga Orchestrator (8090)**: 트랜잭션 조율

## 실행 방법

```bash
# docker 빌드
./gradlew docker

# 전체 서비스 실행
docker-compose up -d
```

## 서비스 접근

- **Membership Service**: http://localhost:8081
  - Swagger UI: http://localhost:8081/swagger-ui.html
- **Banking Service**: http://localhost:8082
  - Swagger UI: http://localhost:8082/swagger-ui.html
- **Money Service**: http://localhost:8083
  - Swagger UI: http://localhost:8083/swagger-ui.html
- **Remittance Service**: http://localhost:8084
  - Swagger UI: http://localhost:8084/swagger-ui.html
- **Payment Service**: http://localhost:8085
  - Swagger UI: http://localhost:8085/swagger-ui.html
- **Settlement Service**: http://localhost:8086
  - Swagger UI: http://localhost:8086/swagger-ui.html
- **Saga Orchestrator**: http://localhost:8090
  - Swagger UI: http://localhost:8090/swagger-ui.html
- **Kafka UI**: http://localhost:8989
- **Vault**: http://localhost:8200




