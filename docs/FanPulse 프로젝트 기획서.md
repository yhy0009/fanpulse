# FanPulse
### Kubernetes 기반 Entertainment Event Platform & SRE Reliability Lab

---

# 1. 프로젝트 개요

FanPulse는 게임, 영화, 애니메이션, 음악 등 다양한 엔터테인먼트 콘텐츠의 이벤트를 조회하고 사용자가 투표하거나 한정 이벤트에 참여할 수 있는 플랫폼이다.

본 프로젝트의 핵심 목적은 일반적인 CRUD 서비스 구현이 아니라, 이벤트 오픈 시 발생하는 급격한 트래픽 증가와 장애 상황을 직접 재현하고 서비스의 **확장성, 가용성, Observability, 장애 대응 및 복구 과정​**을 검증하는 것이다.

최종적으로 다음 흐름을 기술적으로 증명한다.

```text
서비스 구축
    ↓
Observability 구축
    ↓
부하 테스트
    ↓
병목 및 장애 발생
    ↓
Prometheus / Grafana / Loki 기반 탐지
    ↓
원인 분석
    ↓
Auto Scaling / Cache / 비동기 처리 구조 개선
    ↓
재부하 테스트
    ↓
Before / After 비교
    ↓
Postmortem 작성
```

---

# 2. 프로젝트 핵심 질문

FanPulse의 모든 기술 선택은 다음 질문에 답하기 위한 방향으로 진행한다.

> 이벤트 시작과 동시에 평상시 대비 수십 배의 트래픽이 증가하면 서비스에는 어떤 문제가 발생하며, 이를 어떻게 관측하고 원인을 좁혀 안정적인 서비스로 개선할 수 있는가?

MSA, Kafka, Kubernetes 등의 기술 자체를 사용하는 것이 목표가 아니다.

**실제 운영 문제를 발견하고 이를 해결하기 위해 필요한 기술을 선택하는 것**이 프로젝트의 핵심이다.

---

# 3. 프로젝트 목표

## 3.1 Application

- Java 21 + Spring Boot 기반 서비스 구현
- Domain 중심 Modular Monolith 구조 적용
- PostgreSQL 기반 데이터 저장
- Redis 기반 Cache 및 Ranking 구현
- 이벤트 중복 투표 방지
- 이벤트 중복 Claim 방지
- 향후 Kafka 기반 비동기 Claim 처리 구현
- 필요성이 검증된 영역만 독립 서비스로 분리

## 3.2 Infrastructure

- Docker 기반 Local 환경 구성
- Kubernetes 기반 Container Orchestration
- k3d 기반 Local Kubernetes 환경 구축
- Terraform 기반 AWS Infrastructure as Code
- 최종 AWS EKS 환경 검증

## 3.3 CI/CD

- GitHub Actions 기반 CI
- ArgoCD 기반 GitOps CD
- GitHub Actions와 ArgoCD 역할 분리
- AWS 인증은 OIDC 기반으로 구성

## 3.4 Observability

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Loki
- JVM / HTTP / Kubernetes / Redis / PostgreSQL / Kafka 지표 관측

## 3.5 Reliability

- k6 기반 부하 테스트
- HPA 기반 API Auto Scaling
- KEDA 기반 Worker Auto Scaling
- 장애 시나리오 재현
- SLI / SLO 정의
- Before / After 성능 비교
- Postmortem 작성

---

# 4. 프로젝트 성공 기준

초기 목표이며 실제 테스트 환경과 결과에 따라 합리적으로 조정할 수 있다.

| 지표 | 초기 목표 | 측정 범위 |
|---|---:|---|
| Availability | 99.9% 이상 | 유효 요청 중 서버가 정상 처리한 요청 비율 |
| HTTP 5xx Rate | 0.1% 이하 | 전체 유효 요청 중 5xx 응답 비율 |
| Read API p95 Latency | 300ms 이하 | Event 목록 / 상세 / Ranking |
| Write API 접수 p95 Latency | 500ms 이하 | Vote 및 동기·비동기 Claim 접수 |
| 비동기 Claim 완료 p95 | 5초 이하 | `createdAt`부터 `processedAt`까지 |
| 중복 Vote / Claim | 0건 | 최종 Database 상태 기준 |
| Claim 수량 초과 처리 | 0건 | 성공 Claim 수가 설정 수량을 초과하지 않음 |
| 장애 탐지 시간 | 1분 이내 | 장애 발생부터 Alert firing까지 |
| Peak Load | 최소 3,000 RPS 검증 | 정의된 혼합 트래픽과 테스트 환경 기준 |
| API Auto Scaling | HPA 정상 동작 | 부하 증가·감소 시 Scale Out/In 확인 |
| Worker Auto Scaling | Kafka Lag 기반 KEDA 정상 동작 | Lag 증가·회복 과정 확인 |

Availability와 5xx Rate는 동일한 유효 요청 집합을 기준으로 측정한다. Validation 실패, 존재하지 않는 리소스 조회, 중복 요청과 수량 소진으로 인한 예상된 4xx 응답은 서버 장애로 계산하지 않되 별도 비즈니스 지표로 기록한다.

모든 결과에는 다음 정보를 함께 기록한다.

```text
Git Commit / Image Digest
테스트 환경(Local k3d 또는 AWS EKS)
Pod / Node / DB / Redis / Kafka Resource
Seed Data 규모
API별 Traffic Mix
테스트 시간과 Warm-up 시간
k6 실행 장비의 CPU / Memory / Network 사용률
```

Local과 AWS 결과를 같은 표에서 직접 비교하지 않는다. 짧은 부하 테스트 결과를 월간 운영 SLO 달성으로 표현하지 않고, **해당 테스트 구간에서의 SLO 적합 여부**로 기록한다.

수치 자체보다 **개선 전후의 변화와 그 원인을 설명할 수 있는 것**을 더 중요하게 평가한다.

실제 검증되지 않은 성능 수치는 README나 포트폴리오에 사용하지 않는다.

---

# 5. 서비스 범위

## 포함

- 콘텐츠 및 이벤트 목록 조회
- 이벤트 상세 조회
- 투표
- 한정 이벤트 Claim
- 실시간 Ranking
- 중복 요청 방지
- Cache
- 비동기 처리
- 부하 테스트
- 장애 대응

## 제외

다음 기능은 프로젝트 핵심 목적과 직접적인 관련이 없으므로 초기 범위에서 제외한다.

- 결제
- 실시간 채팅
- 커뮤니티
- 댓글
- DM
- 복잡한 회원 관리
- OAuth
- AI 추천
- 구독
- 팔로우
- 실제 외부 콘텐츠 API 연동

회원 인증 시스템 구현에 과도한 시간을 사용하지 않는다.

초기에는 모든 참여 API에서 다음 테스트용 Header로 사용자를 식별한다.

```http
X-Test-User-Id: 1001
```

`X-Test-User-Id`는 Local 및 부하 테스트 전용이며 신뢰 가능한 인증 수단이 아니다. 외부에 공개하는 운영 환경에서는 비활성화해야 한다. 이 프로젝트의 중복 Vote / Claim 방지는 **주어진 사용자 식별자에 대한 정합성 보장**이며, 사용자 인증이나 Header 위조 방지를 구현했다고 주장하지 않는다.

---

# 6. 콘텐츠 카테고리

```text
GAME
MOVIE
ANIME
MUSIC
```

초기 콘텐츠 데이터는 Seed Data를 사용한다.

실제 게임, 영화, 애니메이션, 음악 서비스의 데이터를 그대로 복제하지 않는다.

---

# 7. 주요 사용자 시나리오

## 7.1 Event 목록 조회

```http
GET /api/v1/events
```

```text
User
 ↓
Event API
 ↓
Redis Cache
 ↓ Cache Miss
PostgreSQL
```

---

## 7.2 Event 상세 조회

```http
GET /api/v1/events/{eventId}
```

사용자는 다음 정보를 확인한다.

- 제목
- 카테고리
- 설명
- 이벤트 상태
- 시작 시간
- 종료 시간
- 참여 정보

---

## 7.3 Vote

```http
POST /api/v1/events/{eventId}/votes
X-Test-User-Id: 1001
```

예시:

```json
{
  "candidateId": 3
}
```

동일 사용자는 같은 이벤트에서 중복 투표할 수 없다.

투표는 다음 조건을 모두 만족해야 한다.

```text
Event가 현재 OPEN 상태
Candidate가 존재함
Candidate가 요청한 Event에 속함
userId와 candidateId가 양수
동일 Event에 대한 기존 Vote가 없음
```

Database Unique Constraint를 최종 방어선으로 사용한다.

```text
UNIQUE (event_id, user_id)
FOREIGN KEY (candidate_id) REFERENCES candidates(id)
```

중복 투표는 `409 Conflict`, 존재하지 않는 Event 또는 Candidate는 `404 Not Found`, 닫힌 Event는 `409 Conflict`로 응답한다. 동시에 같은 사용자의 요청이 도착하더라도 최종 Database에는 하나의 Vote만 존재해야 한다.

---

# 8. Claim

```http
POST /api/v1/events/{eventId}/claims
X-Test-User-Id: 1001
```

Claim 요청은 Body를 사용하지 않는다.

동일 사용자는 같은 이벤트를 중복 Claim할 수 없다.

Claim은 이벤트별로 설정된 한정 수량 안에서 참여 권리를 확보하는 기능이다. 단순 중복 방지뿐 아니라 전체 성공 수량이 `totalQuantity`를 넘지 않아야 한다.

```text
EventInventory

eventId
totalQuantity
claimedQuantity
updatedAt
```

수량 확보는 Application의 조회 후 저장 방식으로 처리하지 않는다. 다음과 같은 조건부 갱신 또는 동등한 원자적 연산을 사용한다.

```sql
UPDATE event_inventory
SET claimed_quantity = claimed_quantity + 1,
    updated_at = CURRENT_TIMESTAMP
WHERE event_id = :eventId
  AND claimed_quantity < total_quantity;
```

영향받은 Row가 1개이면 수량 확보 성공, 0개이면 수량 소진으로 판단한다. 동일 Transaction 안에서 Claim 상태를 변경하며 `(event_id, user_id)` Unique Constraint를 최종 중복 방어선으로 유지한다.

Phase 8의 비동기 Claim부터 다음 Header를 필수로 사용한다.

```http
Idempotency-Key: <uuid>
```

비동기 Claim을 도입하는 Phase 8부터 `Idempotency-Key`를 필수로 사용한다. 같은 Key와 같은 요청은 최초 결과를 반환하고, 같은 Key에 다른 `eventId` 또는 `userId`를 사용하면 `409 Conflict`로 처리한다. Redis가 없어도 Database Unique Constraint와 Claim 상태 전이로 최종 정합성을 보장해야 한다.

초기에는 동기 방식으로 구현한다.

동기 방식의 응답 계약:

```text
201 Created  Claim 성공
404 Not Found  Event 없음
409 Conflict  중복 Claim, 닫힌 Event 또는 수량 소진
```

향후 부하 테스트 결과에 따라 Kafka 기반 비동기 구조로 변경한다.

```text
Before

Client
 ↓
Event API
 ↓
PostgreSQL
```

```text
After

Client
 ↓
Event API
 ↓
Kafka
 ↓
Claim Worker
 ↓
PostgreSQL
```

비동기 방식의 응답 계약:

```http
POST /api/v1/events/{eventId}/claims
X-Test-User-Id: 1001
Idempotency-Key: <uuid>

HTTP/1.1 202 Accepted
Location: /api/v1/claims/{claimId}
```

```json
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING"
}
```

```http
GET /api/v1/claims/{claimId}
```

Claim 상태는 다음 단방향 전이만 허용한다.

```text
PENDING → SUCCESS
PENDING → FAILED
```

`FAILED`에는 최소한 `SOLD_OUT`, `EVENT_CLOSED`, `PROCESSING_ERROR` 사유를 기록한다. Kafka는 at-least-once 전달을 전제로 하며 Worker는 같은 `claimId`를 여러 번 받아도 결과가 바뀌지 않도록 멱등하게 처리한다.

Phase 8에서는 Claim PENDING 저장과 Kafka 발행 사이의 Dual Write 문제를 피하기 위해 Transactional Outbox를 사용한다.

```text
Event API Transaction
  ├ Claim(PENDING) 저장
  └ Outbox Event 저장
          ↓
Outbox Publisher
          ↓
Kafka
          ↓
Claim Worker
```

API는 Claim과 Outbox Event가 함께 Commit된 뒤 `202 Accepted`를 반환한다. Outbox Publisher 장애 시 재시도할 수 있어야 하며, 처리 지연은 Claim 상태와 Outbox 적체 지표로 관측한다.

---

# 9. Ranking

```http
GET /api/v1/events/{eventId}/rankings?limit=20
```

Redis Sorted Set을 활용한 실시간 Ranking을 구현한다.

PostgreSQL의 Vote를 최종 Source of Truth로 사용하고 Redis Ranking은 재생성 가능한 Projection으로 취급한다.

```text
Vote Database Commit
 ↓ After Commit
Redis ZINCRBY
```

Redis 갱신 실패 때문에 이미 Commit된 Vote를 실패로 응답하지 않는다. 실패한 갱신은 재시도하거나 Database 집계를 통해 Ranking을 재구축한다. 재구축 명령과 절차를 `docs/runbooks/`에 기록한다.

동점은 다음 순서로 결정한다.

```text
score 내림차순
candidateId 오름차순
```

초기에는 Event API 내부 Ranking Module로 구현한다.

별도의 Ranking Service로 바로 분리하지 않는다.

---

# 10. 기술 스택

## Application

```text
Java 21
Spring Boot 3.x
Maven
Spring Web
Spring Data JPA
Spring Validation
Spring Boot Actuator
Micrometer
Flyway
```

## Database

```text
PostgreSQL
```

## Cache / Ranking

```text
Redis
```

## Messaging

```text
Apache Kafka
```

## Container

```text
Docker
Docker Compose
```

## Kubernetes

```text
k3d
Kubernetes
Helm
```

## Scaling

```text
HPA
KEDA
```

## CI/CD

```text
GitHub Actions
ArgoCD
```

## Observability

```text
Prometheus
Grafana
Loki
Spring Boot Actuator
Micrometer
```

## Load Testing

```text
k6
```

## Infrastructure as Code

```text
Terraform
```

## Cloud

```text
AWS
Amazon EKS
Amazon ECR
Application Load Balancer
IAM
```

## Automation

```text
Python
Shell Script
```

---

# 11. Application Architecture 원칙

FanPulse는 처음부터 MSA를 전제로 설계하지 않는다.

초기 애플리케이션은 **Domain 중심 Modular Monolith** 구조로 구현한다.

```text
FanPulse Event API

├── Event
├── Vote
├── Claim
└── Ranking
```

각 Domain은 하나의 Spring Boot 애플리케이션 안에서 실행되지만 코드 수준에서는 명확하게 분리한다.

---

# 12. Package 구조

다음과 같이 Domain을 최상위 기준으로 구성한다.

```text
com.fanpulse

├── event
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
├── vote
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
├── claim
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
├── ranking
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
└── common
    ├── config
    ├── exception
    └── response
```

다음과 같은 전체 Layer 중심 구조는 사용하지 않는다.

```text
domain/
application/
infrastructure/
presentation/
```

대신 Domain을 먼저 분리하고 Domain 내부에서 Layer를 나눈다.

---

# 13. Domain 책임

## Event

```text
이벤트 조회
이벤트 상태 관리
이벤트 시작 / 종료 관리
콘텐츠 카테고리 관리
이벤트 후보 관리
```

## Vote

```text
투표 요청 처리
중복 투표 방지
후보별 투표 관리
```

## Claim

```text
이벤트 Claim 요청
중복 Claim 방지
Claim 상태 관리
```

## Ranking

```text
이벤트 Ranking
후보 Ranking
Redis Sorted Set 관리
```

## Common

특정 Domain에 속하지 않는 최소한의 공통 기능만 둔다.

```text
Global Exception Handler
공통 Response
Configuration
```

비즈니스 로직을 `common`에 배치하지 않는다.

---

# 14. 서비스 분리 원칙

MSA 자체를 프로젝트 목표로 삼지 않는다.

다음 필요성이 실제 테스트를 통해 확인되는 경우에만 서비스를 분리한다.

```text
독립적인 Scaling 요구

서로 다른 부하 특성

장애 격리 필요

비동기 처리 필요

독립 배포 필요

성능 분석 결과 확인된 병목
```

Application Architecture는 다음 방식으로 발전한다.

```text
Modular Monolith
        ↓
Observability
        ↓
k6 Load Test
        ↓
병목 분석
        ↓
독립 확장 필요성 확인
        ↓
필요 영역만 Service 분리
```

---

# 15. Claim Worker 분리

가장 먼저 독립 실행 단위로 분리할 후보는 Claim Worker이다.

```text
Event API
    │
    ▼
   Kafka
    │
    ▼
Claim Worker
```

분리 이유:

```text
API 요청 처리와 실제 작업 분리

순간적인 Write 부하 흡수

DB 직접 요청 감소

API Response Time 감소

Consumer 독립 확장

Kafka Consumer Lag 기반 Scaling
```

API와 Worker의 Scaling 기준도 서로 다르다.

```text
Event API

HTTP Request
CPU
Latency
 ↓
HPA
```

```text
Claim Worker

Kafka Consumer Lag
 ↓
KEDA
```

따라서 Claim Worker는 단순한 MSA 경험을 위한 분리가 아니라 **서로 다른 부하 특성에 따른 독립 실행 단위**로 분리한다.

---

# 16. Ranking Service 분리

Ranking은 초기에는 Event API 내부에 존재한다.

```text
Event API

├ Event
├ Vote
├ Claim
└ Ranking
```

부하 테스트 결과 Ranking 조회 부하가 다른 기능과 크게 다르고 독립 Scaling이 필요하다고 판단될 경우에만 분리한다.

```text
Event API

├ Event
├ Vote
└ Claim
```

```text
Ranking Service

└ Ranking
   ↓
 Redis
```

명확한 이유가 없다면 프로젝트 종료 시점까지 Event API 내부에 유지한다.

---

# 17. Domain Model

## Event

```text
id
title
description
category
startAt
endAt
createdAt
updatedAt
```

`status`는 별도 Database 컬럼으로 독립 관리하지 않고 조회 시점을 기준으로 `startAt`, `endAt`에서 계산한다.

```text
now < startAt                → SCHEDULED
startAt <= now < endAt       → OPEN
endAt <= now                 → CLOSED
```

모든 시간은 Database에 UTC `timestamptz`로 저장하고 API에서는 ISO-8601 offset 형식으로 반환한다. `startAt < endAt`을 반드시 검증한다.

### Category

```text
GAME
MOVIE
ANIME
MUSIC
```

### Status

```text
SCHEDULED
OPEN
CLOSED
```

초기 범위에는 관리자에 의한 강제 종료를 포함하지 않는다. 향후 필요할 경우 시간 기반 상태와 섞지 않고 별도의 `forcedClosedAt` 또는 명시적인 운영 상태 모델로 추가한다.

---

## Candidate

```text
id
eventId
name
displayOrder
createdAt
```

Candidate는 반드시 하나의 Event에 속한다. Vote 처리 시 URL의 `eventId`와 Candidate의 `eventId`가 동일해야 한다.

Database Constraint:

```text
FOREIGN KEY (event_id) REFERENCES events(id)
UNIQUE (event_id, name)
```

---

## Vote

```text
id
eventId
userId
candidateId
createdAt
```

Database Unique Constraint:

```text
event_id + user_id
```

Application 코드만으로 중복을 방지하지 않는다.

Database Constraint를 최종 정합성 보호 장치로 사용한다.

---

## Claim

```text
id (UUID)
eventId
userId
idempotencyKey
status
createdAt
processedAt
failureReason
```

Status:

```text
PENDING
SUCCESS
FAILED
```

Database Unique Constraint:

```text
event_id + user_id
idempotency_key
```

`idempotencyKey`는 Phase 8부터 저장하며 같은 Key가 다른 요청 내용에 재사용되지 않도록 요청 Fingerprint도 함께 비교한다.

---

## EventInventory

```text
eventId
totalQuantity
claimedQuantity
updatedAt
```

Constraint:

```text
PRIMARY KEY / FOREIGN KEY (event_id) REFERENCES events(id)
total_quantity >= 0
claimed_quantity >= 0
claimed_quantity <= total_quantity
```

---

# 18. Redis 사용 목적

Redis를 단순히 기술 스택에 넣기 위해 사용하지 않는다.

## Event Cache

```text
GET Event
 ↓
Redis
 ↓ Cache Miss
PostgreSQL
 ↓
Redis 저장
```

Cache Key와 기본 TTL:

```text
event:{eventId}                TTL 60 seconds
events:{queryHash}:{page}      TTL 30 seconds
```

Event가 변경되는 기능이 추가되면 Transaction Commit 이후 관련 Cache를 무효화한다. Cache는 Source of Truth가 아니므로 전체 삭제 후에도 Database에서 재구성할 수 있어야 한다.

Redis 연결 실패는 Cache Miss와 구분한다. 짧은 연결·명령 Timeout을 적용하고 Redis 장애 시 PostgreSQL로 Fallback하되, DB 과부하 방지를 위해 요청 제한과 Alert를 적용한다. Redis 장애가 API Thread를 장시간 점유하지 않아야 한다.

다음 항목을 관측한다.

```text
Cache Hit
Cache Miss
Latency
Memory
Connection
```

---

## Ranking

Redis Sorted Set을 사용한다.

예:

```text
ranking:event:{eventId}
```

Ranking Key는 TTL로 자동 삭제하지 않는다. Event 종료 후 보존 기간과 삭제 정책을 별도로 적용한다. Redis 데이터 유실 또는 정합성 불일치 시 PostgreSQL Vote 집계로 재구축한다.

---

## Idempotency

향후 Claim 중복 요청 방어에 Redis를 사용할 수 있다.

예:

```text
claim:{eventId}:{userId}
```

단, Redis만을 최종 정합성 보장 장치로 사용하지 않는다.

Database Constraint도 유지한다.

---

# 19. Kafka 사용 목적

이벤트 오픈 시 발생하는 대량 Claim 요청을 API에서 바로 Database로 처리하지 않도록 한다.

```text
Before

Client
 ↓
Event API
 ↓
PostgreSQL
```

```text
After

Client
 ↓
Event API
 ↓
Kafka
 ↓
Claim Worker
 ↓
PostgreSQL
```

Kafka 도입 전후의 성능을 반드시 비교한다.

예:

```text
API p95
HTTP Error Rate
DB Connection
DB Transaction
Queue Lag
Worker Throughput
```

Kafka 처리 계약:

```text
Delivery Semantics: at-least-once
Message Key: claimId
Producer Acks: all
Worker Commit: Database Transaction 성공 후 Offset Commit
Retry: 제한된 횟수와 Exponential Backoff
DLQ: 재시도 소진 또는 처리 불가능한 Schema
```

Claim 메시지는 최소 다음 정보를 포함한다.

```text
eventId
claimId
userId
idempotencyKey
requestedAt
schemaVersion
correlationId
```

DLQ 메시지는 자동 폐기하지 않는다. 원인 수정 후 재처리하는 Runbook을 작성하며, DLQ 크기와 가장 오래된 메시지 시간을 Alert 대상으로 사용한다.

여러 Partition을 사용하므로 모든 사용자에 대한 엄격한 전역 선착순 순서는 보장하지 않는다. 프로젝트의 Claim 성공 조건은 **수량을 초과하지 않는 것**과 **중복 성공이 없는 것**이다. 접수 순서까지 엄격히 보장해야 하는 요구가 추가되면 Partition 전략과 처리량의 Trade-off를 별도로 검증한다.

---

# 20. Local Architecture

대부분의 개발 및 장애 실험은 로컬 Kubernetes 환경에서 수행한다.

```text
MacBook

k3d Kubernetes

├ Event API
├ Claim Worker
├ PostgreSQL
├ Redis
├ Kafka
├ Prometheus
├ Grafana
├ Loki
├ ArgoCD
├ HPA
└ KEDA

외부
└ k6
```

AWS에 항상 리소스를 유지하지 않는다.

---

# 21. 최종 AWS Architecture

최종 검증 단계의 목표 구조이다.

```text
                         GitHub
                            │
                    GitHub Actions
                            │
                            ▼
                           ECR
                            │
                            ▼
                         ArgoCD
                            │
                            ▼

Client / k6
     │
     ▼
    ALB
     │
     ▼
┌──────────────── EKS ─────────────────┐
│                                     │
│             Event API               │
│            Spring Boot              │
│                 │                   │
│        ┌────────┴─────────┐         │
│        │                  │         │
│      Redis           PostgreSQL     │
│        │                            │
│     Ranking                         │
│                                     │
│ Event API                           │
│    │                                │
│    ▼                                │
│  Kafka                              │
│    │                                │
│    ▼                                │
│ Claim Worker                        │
│    │                                │
│    ▼                                │
│ PostgreSQL                          │
│                                     │
│ HPA                KEDA             │
│  │                  │               │
│ API Pods        Worker Pods         │
│                                     │
└─────────────────────────────────────┘
              │
              ▼
       Prometheus / Loki
              │
              ▼
           Grafana
```

Ranking Service는 필요성이 실제로 검증된 경우에만 추가한다.

이 AWS 구성의 검증 범위는 **Event API와 Claim Worker의 배포, 관측, 확장, 장애 복구 과정**이다. 비용 절감을 위해 PostgreSQL, Redis, Kafka를 단일 인스턴스 또는 Kubernetes 내부 Stateful Workload로 운영하는 경우 데이터 계층 자체의 Multi-AZ 고가용성을 검증했다고 주장하지 않는다.

AWS 결과 문서에는 다음 한계를 명시한다.

```text
Application Layer HA 검증 여부
Data Layer HA 미검증 또는 제한 사항
부하 발생 위치와 Network 경로
검증 시간
실제 발생 비용
```

---

# 22. Auto Scaling 전략

## Event API

HPA 사용.

초기 기준:

```text
minReplicas: 2
maxReplicas: 10
CPU Target: 60%
```

이 값은 임의의 정답이 아니며 부하 테스트 결과를 바탕으로 수정한다.

향후 고려 지표:

```text
CPU
Memory
HTTP Request Rate
Latency
```

---

## Claim Worker

KEDA 사용.

핵심 Scaling Metric:

```text
Kafka Consumer Lag
```

동작:

```text
Kafka Lag 증가
      ↓
Worker Scale Out
      ↓
처리량 증가
      ↓
Kafka Lag 감소
      ↓
Worker Scale In
```

---

# 23. Observability

## Kubernetes Metrics

```text
Pod CPU
Pod Memory
Pod Restart
Replica Count
Network
Node Resource
```

## HTTP Metrics

```text
Request Rate
Error Rate
p50
p95
p99
HTTP Status Code
```

## JVM Metrics

```text
Heap
Non Heap
GC Pause
GC Count
Thread Count
```

## Spring / Tomcat Metrics

```text
Tomcat Busy Threads
Request Duration
Active Requests
```

## HikariCP

```text
Active Connections
Idle Connections
Pending Connections
Max Connections
```

## Redis

```text
Cache Hit
Cache Miss
Memory
Connections
Latency
```

## Kafka

```text
Consumer Lag
Producer Rate
Consumer Rate
Message Throughput
```

## PostgreSQL

```text
Connections
Transaction Rate
Query Latency
Slow Query
```

## Observability 운영 원칙

모든 HTTP 요청과 비동기 Claim 메시지에는 `correlationId`를 전달한다. Log에는 `correlationId`, `claimId`, `eventId`, 처리 단계와 실패 사유를 구조화하여 기록하고, 개인 식별이 가능한 값과 Secret은 기록하지 않는다.

Prometheus Label에는 `userId`, `claimId`, `idempotencyKey`처럼 Cardinality가 계속 증가하는 값을 사용하지 않는다. 이 값들은 Trace 또는 Log 검색 필드로만 사용한다.

최소 Alert:

```text
5xx Rate SLO 초과
p95 Latency SLO 초과
Pod Restart 증가
Hikari Pending Connection 발생
Redis Timeout 증가
Kafka Consumer Lag 증가
DLQ 메시지 발생
Outbox Oldest Pending Age 증가
Claim PROCESSING_ERROR 증가
```

Alert에는 원인 후보를 단정하지 않고 Dashboard와 Runbook Link를 포함한다. Phase 5에서 Alert가 실제 장애 주입 후 1분 이내 firing되는지 검증한다.

---

# 24. SLI / SLO

## SLI

```text
Availability = 정상 처리된 유효 요청 수 / 전체 유효 요청 수

HTTP 5xx Rate = 5xx 응답 수 / 전체 유효 요청 수

Endpoint Latency = API별 Request Duration Histogram의 p95 / p99

Claim Completion Latency = processedAt - createdAt

Duplicate Rate = 중복 성공 Vote 또는 Claim 수

Oversell Count = max(성공 Claim 수 - totalQuantity, 0)
```

유효 요청은 인증 형식, 필수 Field, 리소스 식별자가 정상인 요청을 의미한다. 예상 가능한 `404`, 중복·소진에 대한 `409`, Validation `400`은 Availability 분모에서 제외하고 별도 지표로 기록한다. Timeout과 5xx는 실패로 계산한다.

## 초기 SLO

```text
Test Window Availability >= 99.9%

HTTP 5xx Rate <= 0.1%

Read API p95 Latency <= 300ms

Write API 접수 p95 Latency <= 500ms

비동기 Claim Completion p95 <= 5s

Duplicate Vote / Claim = 0

Oversell Count = 0
```

Grafana에서 별도 SLO Dashboard를 구성하고 Test Run 시작·종료 Annotation을 남긴다. Availability 99.9%의 Error Budget은 해당 Test Window 유효 요청의 0.1%이다. 월간 SLO는 실제 장기 운영 데이터가 확보된 이후 별도로 정의한다.

---

# 25. Load Test

k6를 사용한다. 처리량 목표가 있는 테스트는 `constant-arrival-rate` 또는 `ramping-arrival-rate` Executor를 사용하여 목표 RPS와 동시 사용자 수를 구분한다.

기본 혼합 Traffic Profile:

| API | 비율 | 데이터 전략 |
|---|---:|---|
| Event 목록 / 상세 | 70% | 여러 Event ID를 분산 조회 |
| Vote | 20% | 중복되지 않는 userId를 기본으로 사용 |
| Claim | 10% | 중복되지 않는 userId와 충분한 수량 사용 |

중복, 수량 소진, 잘못된 Candidate를 검증하는 Negative Scenario는 정상 성능 시나리오와 분리한다. 각 테스트 전에 Seed Data와 Claim 수량을 초기화하고, 테스트 도중 생성된 데이터 규모를 기록한다.

공통 사전 조건:

```text
동일 Git Commit / Image Digest 사용
Pod와 Dependency Resource 고정 및 기록
최소 2분 Warm-up 후 측정
k6 CPU 70% 미만과 Dropped Iteration 확인
NTP / Timezone 확인
Dashboard Annotation 기록
테스트 중 배포와 수동 설정 변경 금지
```

## Baseline Test

```text
100 RPS / 10 minutes
```

정상 환경 성능 기준을 확보한다.

---

## Load Test

점진적으로 요청을 증가시킨다.

```text
100 RPS   / 5 minutes
500 RPS   / 5 minutes
1,000 RPS / 5 minutes
2,000 RPS / 5 minutes
```

각 단계 사이에 부하를 100 RPS로 낮추어 회복 시간도 측정한다.

---

## Spike Test

이벤트 오픈 상황을 재현한다.

```text
100 RPS / 2 minutes
   ↓
3,000+ RPS / 5 minutes
   ↓
100 RPS / 3 minutes
```

Spike 구간의 성공률뿐 아니라 HPA 반응 지연, 최대 Pod 수, DB Connection, Redis Timeout, Kafka Lag과 정상 상태 복귀 시간을 기록한다.

---

## Stress Test

서비스가 어느 수준부터 성능이 급격하게 저하되는지 찾는다.

500 RPS 단위로 증가시키되 다음 중 하나가 발생하면 중단한다.

```text
5xx Rate > 5%가 1분 이상 지속
p95 > 2초가 1분 이상 지속
DB 또는 Node가 복구되지 않는 포화 상태
k6 Dropped Iteration 증가
```

중단 지점은 프로젝트 성능 수치가 아니라 해당 환경의 포화 지점으로 기록한다.

---

## Soak Test

Baseline과 Stress 결과를 바탕으로 포화 처리량의 30~50%를 최소 60분 유지한다. 고정된 임의 RPS를 모든 환경에 동일하게 적용하지 않는다.

확인 대상:

```text
Memory Leak
Connection Leak
GC
DB Connection
Kafka Lag
```

모든 k6 Script에는 HTTP 실패율, API별 p95, Dropped Iteration Threshold를 명시한다. 결과 원본, 요약, Grafana Screenshot과 환경 정보를 `docs/performance/`의 동일한 Test Run ID 아래 저장한다.

---

# 26. Failure Scenario

각 장애 실험 문서에는 다음을 먼저 작성한다.

```text
Steady State와 정상 범위
실험 가설
장애 주입 방법과 대상
예상 사용자 영향
관측할 Metric / Log / Alert
실험 중단 조건
복구 명령과 담당 단계
실제 결과
```

장애 주입 전에 복구 절차를 검증하고, 동시에 하나의 Failure만 주입한다.

## Scenario 1. API Pod 부족

```text
Traffic Spike
 ↓
CPU 증가
 ↓
p95 증가
 ↓
5xx 증가
```

HPA 적용 전후 비교.

확인 항목:

```text
Scale Out 시작까지 걸린 시간
새 Pod가 Ready가 될 때까지의 시간
최대 Replica와 Resource Saturation
Scale In 후 정상 상태 복귀
```

---

## Scenario 2. Redis 장애

```text
Redis Down
 ↓
Redis Timeout / Connection Failure
 ↓
PostgreSQL 요청 증가
 ↓
DB Load 증가
 ↓
API Latency 증가
```

확인 항목:

```text
Timeout
Fallback
Connection
DB Load
```

Redis 장애를 Cache Miss로 기록하지 않는다. 짧은 Timeout과 PostgreSQL Fallback이 동작하는지, Fallback 때문에 DB가 포화되지 않는지, Redis 복구 후 Cache가 정상적으로 다시 채워지는지 검증한다.

---

## Scenario 3. DB Connection Pool 고갈

```text
Request 증가
 ↓
HikariCP Active 증가
 ↓
Pool 고갈
 ↓
Pending 증가
 ↓
Latency 증가
```

Pool Size만 키워서 해결하지 않는다. DB 최대 Connection, Pod 수, Pod별 Pool Size의 관계를 기록하고 Timeout, Query 개선, 요청 제한 중 어떤 조치가 효과가 있었는지 비교한다.

---

## Scenario 4. Kafka Consumer 장애

```text
Consumer Down
 ↓
Kafka Lag 증가
 ↓
Processing Delay
```

Worker 복구 및 KEDA Scaling을 검증한다.

확인 항목:

```text
Consumer 중단 중 API의 202 접수 지속 여부
Kafka Lag와 Oldest Message Age
Worker 복구 후 중복 성공 0건
Lag 해소 시간
Retry 소진과 DLQ 동작
```

---

## Scenario 5. Bad Deployment

```text
v1 정상
 ↓
v2 Deploy
 ↓
5xx 증가
 ↓
Prometheus Alert
 ↓
Git에서 v2 Manifest Revert
 ↓
ArgoCD Sync
 ↓
v1 복구
```

기본 복구 방식은 Git Revert 후 ArgoCD Sync이다. ArgoCD가 Prometheus Alert만으로 자동 Rollback한다고 가정하지 않는다. 향후 자동 Rollback Controller 또는 Progressive Delivery 도구를 실제로 구성하고 검증한 경우에만 자동 복구로 문서화한다.

다음 시간을 각각 기록한다.

```text
배포부터 장애 발생까지
장애부터 Alert까지(MTTD)
Alert부터 Revert Commit까지
Revert부터 정상화까지(MTTR)
```

---

# 27. Performance 결과 기록

모든 성능 개선은 Before / After 형태로 기록한다.

예:

| Metric | Before | After |
|---|---:|---:|
| p95 Latency | 실제 측정값 | 실제 측정값 |
| Error Rate | 실제 측정값 | 실제 측정값 |
| API Pods | 실제 값 | 실제 값 |
| DB Connections | 실제 값 | 실제 값 |
| Kafka Lag | 실제 값 | 실제 값 |

임의의 수치를 작성하지 않는다.

---

# 28. Postmortem

주요 장애 실험마다 문서를 작성한다.

```text
Incident

Summary

Impact

Timeline

Detection

Root Cause

Resolution

Preventive Action

Before / After
```

문서는 다음 위치에 관리한다.

```text
docs/incidents/
```

---

# 29. CI/CD

## GitHub Actions

역할:

```text
Test
 ↓
Maven Build
 ↓
Docker Image Build
 ↓
Container Registry Push
 ↓
Kubernetes Manifest의 Image Digest 변경 PR 생성
 ↓
Review / Merge
 ↓
ArgoCD Sync
```

Container Image에는 `latest`를 사용하지 않는다. Git Commit SHA Tag와 Image Digest를 기록하며 Kubernetes Manifest는 가능하면 Digest로 고정한다.

GitHub Actions와 ArgoCD의 책임:

```text
GitHub Actions
- Test / Build / Image Scan
- ECR Push
- 배포 Manifest 변경 PR 생성

ArgoCD
- Git에 Merge된 Desired State 감지
- Kubernetes Sync
- Drift 표시
```

같은 Repository의 `infrastructure/kubernetes/environments/dev/`를 GitOps Source로 사용한다. CI가 Cluster에 직접 `kubectl apply`하지 않는다. AWS ECR 인증에는 장기 Access Key 대신 GitHub OIDC를 사용한다.

Spring Boot Build 명령:

```bash
./mvnw clean verify
```

Package:

```bash
./mvnw clean package
```

테스트 제외가 필요한 Container Build 단계에서는 필요에 따라:

```bash
./mvnw clean package -DskipTests
```

사용 가능하다.

단 CI에서는 기본적으로 테스트를 수행한다.

---

# 30. Maven 원칙

Repository에는 Maven Wrapper를 포함한다.

```text
mvnw
mvnw.cmd
.mvn/
pom.xml
```

개발 환경에 설치된 Maven 버전에 의존하지 않는다.

기본 검증 명령:

```bash
./mvnw clean verify
```

Application 실행:

```bash
./mvnw spring-boot:run
```

---

# 31. 향후 Maven Multi-Module

Phase 1에서는 별도의 Multi-Module 구조를 만들지 않는다.

초기:

```text
fanpulse/

└── apps/
    └── event-api/
        └── pom.xml
```

Claim Worker가 분리되는 Phase에서 필요하다면 Maven Multi-Module 구조로 변경한다.

예:

```text
fanpulse/

├── pom.xml
│
└── apps/
    ├── event-api/
    │   └── pom.xml
    │
    └── claim-worker/
        └── pom.xml
```

Root POM은 공통 Version 및 Dependency Management 용도로 사용할 수 있다.

멀티모듈 자체를 미리 만들지 않는다.

---

# 32. ArgoCD

역할:

```text
Git Repository
 ↓
Manifest 변경
 ↓
ArgoCD Detection
 ↓
Kubernetes Sync
```

역할 구분:

```text
GitHub Actions
= CI

ArgoCD
= CD / GitOps
```

배포 이력은 Git Commit으로 추적한다. Bad Deployment의 기본 Rollback은 이전 정상 Image Digest로 Manifest를 Git Revert하는 방식이다. ArgoCD UI에서만 Live State를 변경하지 않는다.

---

# 33. Terraform

예상 구조:

```text
infrastructure/
└── terraform/
    ├── modules/
    │   ├── vpc/
    │   ├── subnet/
    │   ├── route/
    │   ├── security-group/
    │   ├── eks/
    │   ├── node-group/
    │   ├── iam/
    │   ├── irsa/
    │   └── ecr/
    │
    └── environments/
        └── dev/
```

실제 프로젝트 요구사항에 없는 AWS Resource를 불필요하게 추가하지 않는다.

---

# 34. AWS 비용 전략

대부분의 개발:

```text
Local
+
Docker
+
k3d
```

AWS는 최종 검증 시에만 사용한다.

```text
Terraform Apply
 ↓
EKS 검증
 ↓
k6 Test
 ↓
Dashboard / 결과 확보
 ↓
Terraform Destroy
```

최종 검증 후:

```bash
terraform destroy
```

를 수행한다.

초기 AWS 환경에서는 RDS와 ElastiCache를 필수로 사용하지 않는다.

비용 절감을 위해 PostgreSQL과 Redis를 Kubernetes 내부에서 운영할 수 있다.

이 경우 데이터 계층의 고가용성, 자동 Backup, Multi-AZ 복구를 검증 범위에서 제외하고 결과 문서에 명시한다.

프로젝트 목표 AWS 비용:

```text
30,000 KRW 이하
```

비용 또한 프로젝트의 하나의 운영 지표로 관리한다.

AWS 검증 전 다음을 작성한다.

```text
예상 실행 시간과 종료 시각
Terraform Plan Resource 목록
EKS / EC2 / ALB / NAT / EBS / Data Transfer 예상 비용
Cost Allocation Tag
AWS Budget Alert
Terraform Destroy Checklist
```

검증 후 Terraform State와 AWS Console을 함께 확인하여 잔존 Resource가 없는지 점검한다. 목표 비용은 보장 수치가 아니라 실제 청구 내역과 차이를 분석하는 예산 상한으로 사용한다.

---

# 35. Repository 구조

```text
fanpulse/

├── apps/
│   └── event-api/
│       ├── src/
│       ├── pom.xml
│       ├── mvnw
│       ├── mvnw.cmd
│       └── .mvn/
│
├── infrastructure/
│   ├── docker/
│   ├── kubernetes/
│   ├── helm/
│   └── terraform/
│
├── observability/
│   ├── prometheus/
│   ├── grafana/
│   └── loki/
│
├── load-test/
│   └── k6/
│
├── scripts/
│   ├── python/
│   └── shell/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── incidents/
│   ├── performance/
│   ├── runbooks/
│   └── FanPulse 프로젝트 기획서.md
│
├── .github/
│   └── workflows/
│
├── docker-compose.yml
├── README.md
└── AGENTS.md
```

이 문서의 Canonical 경로는 `docs/FanPulse 프로젝트 기획서.md`이다. 다른 이름의 사본을 별도 기준 문서로 운영하지 않는다.

구현 과정에서 디렉터리 구조를 과도하게 미리 생성하지 않는다.

필요한 Phase가 되었을 때 추가한다.

---

# 36. 개발 원칙

Codex는 다음 원칙을 따른다.

1. 한 번에 전체 프로젝트를 구현하지 않는다.
2. 현재 요청된 Phase만 구현한다.
3. 각 Phase 종료 시 애플리케이션은 반드시 실행 가능해야 한다.
4. 새로운 기술을 추가하기 전에 해결하려는 문제를 먼저 정의한다.
5. MSA 자체를 목표로 하지 않는다.
6. 초기 애플리케이션은 Modular Monolith 구조를 유지한다.
7. 서비스 분리는 실제 부하 및 운영 요구가 확인된 경우에만 수행한다.
8. 기능 추가보다 Observability와 Reliability 검증을 우선한다.
9. 불필요한 Design Pattern과 추상화를 피한다.
10. Entity를 API Response로 직접 노출하지 않는다.
11. 모든 환경 변수와 Secret은 코드에 직접 작성하지 않는다.
12. `.env.example`만 Repository에 포함한다.
13. AWS Access Key를 Repository에 저장하지 않는다.
14. AWS 인증은 향후 GitHub OIDC를 사용한다.
15. 성능 수치는 실제 테스트 결과만 사용한다.
16. Production을 과도하게 흉내 내기 위해 범위를 불필요하게 확대하지 않는다.
17. 공통 모듈에 Domain Business Logic을 넣지 않는다.
18. Database Constraint를 정합성 보호 장치로 적극 활용한다.
19. README와 문서는 구현 결과와 항상 일치시킨다.
20. 명시적인 요청 없이 새로운 Microservice를 생성하지 않는다.

---

# 37. 구현 Phase

## Phase 1 — Spring Boot MVP

```text
Spring Boot
+
PostgreSQL
+
Modular Monolith
```

Phase 1은 한 번에 구현하지 않고 다음 두 Milestone으로 분리한다.

### Phase 1A — Event Read MVP

```text
Maven Project / Wrapper
Event Domain
PostgreSQL / Flyway
Event 목록 / 상세
시간 기반 Event Status 계산
Validation / Exception Handling
Seed Data / Test / Actuator
```

### Phase 1B — Participation MVP

```text
Candidate
Vote와 중복 방지
EventInventory
동기 Claim과 수량 초과 방지
PostgreSQL 집계 기반 Ranking
동시성 Integration Test
```

Phase 1B의 PostgreSQL Ranking 결과를 Phase 2 Redis Ranking 도입 전 Baseline으로 사용한다.

---

## Phase 2 — Redis

```text
Event Cache
Redis Sorted Set Ranking Projection
PostgreSQL 기반 Ranking과 결과 일치 검증
Cache Hit / Miss / Timeout
Cache 및 Ranking 재구축 Runbook
Redis Failure Test
```

---

## Phase 3 — Docker

```text
Event API
PostgreSQL
Redis
```

다음 명령으로 실행:

```bash
docker compose up -d
```

---

## Phase 4 — Kubernetes

k3d 사용.

구현:

```text
Deployment
Service
ConfigMap
Secret
Probe
Resource Request
Resource Limit
Persistent Storage
```

---

## Phase 5 — Observability

```text
Actuator
Micrometer
Prometheus
Grafana
Loki
```

Dashboard:

```text
Service Overview
JVM
Kubernetes
PostgreSQL
Redis
```

---

## Phase 6 — k6

```text
Baseline
Load
Spike
Stress
Soak
```

초기 성능 기준을 확보한다.

---

## Phase 7 — HPA

부하 테스트 결과를 바탕으로 HPA를 적용한다.

Before / After를 비교한다.

---

## Phase 8 — Kafka + Claim Worker

Claim 처리를 비동기 구조로 변경한다.

```text
Event API
 ↓
Claim + Outbox 저장
 ↓
Outbox Publisher
 ↓
Kafka
 ↓
Claim Worker
```

구현:

```text
202 Accepted와 Claim 상태 조회 API
Idempotency-Key
Transactional Outbox
at-least-once Consumer
Worker 멱등성
Retry / DLQ
동기 Claim 대비 Before / After
```

이 Phase에서 필요하면 Maven Multi-Module 구조를 도입한다.

---

## Phase 9 — KEDA

Kafka Consumer Lag 기반 Claim Worker Auto Scaling을 구현한다.

---

## Phase 10 — Failure Testing

최소 다음 장애 실험을 수행한다.

```text
Pod Failure
Redis Failure
DB Connection Exhaustion
Kafka Consumer Failure
Bad Deployment
```

---

## Phase 11 — CI/CD

```text
GitHub Actions
ArgoCD
```

구현.

---

## Phase 12 — AWS

Terraform을 이용해 다음 환경을 구성한다.

```text
VPC
EKS
Node Group
ECR
IAM
IRSA
ALB
```

AWS 환경에서 최종 검증한다.

검증 후:

```bash
terraform destroy
```

## Phase 전환 공통 조건

다음 조건을 만족하기 전에는 다음 Phase로 넘어가지 않는다.

```text
현재 Phase 기능과 이전 Phase Regression Test 통과
./mvnw clean verify 성공
Local 재현 명령 검증
새로운 환경 변수와 Secret 문서화
관측 가능한 Metric / Log 확인
실패 시 원상 복구 절차 확인
README와 Canonical 기획서 동기화
실제 측정이 필요한 Phase는 Raw Result와 환경 정보 저장
```

각 Phase에서 새로운 기술을 도입할 때는 `문제 → 가설 → 변경 → 측정 → 결론`을 `docs/performance/` 또는 `docs/architecture/`에 기록한다.

---

# 38. Ranking Service 분리 검토

Phase 6 이후 부하 테스트 결과를 바탕으로 판단한다.

다음이 확인될 경우 분리를 검토한다.

```text
Ranking Traffic이 다른 API보다 현저히 높음

Event API와 다른 Scaling 기준 필요

Redis 중심의 독립적인 처리 특성

Event API 장애와 Ranking 장애를 격리할 필요
```

해당 필요성이 없다면 분리하지 않는다.

---

# 39. Codex 최초 작업 범위

Codex의 첫 작업에서는 **Phase 1A — Event Read MVP만 구현한다.**

아직 다음 기술을 구현하지 않는다.

```text
Redis

Kafka

Claim Worker

Kubernetes

Prometheus

Grafana

Loki

Terraform

AWS

ArgoCD

KEDA
```

---

# 40. Codex 최초 작업 지시

## 40.1 Spring Boot 프로젝트

다음 기반으로 프로젝트를 생성한다.

```text
Java 21
Spring Boot 3.x
Maven
```

Maven Wrapper를 Repository에 포함한다.

---

## 40.2 Dependency

초기 필요 Dependency:

```text
Spring Web

Spring Data JPA

Spring Validation

PostgreSQL Driver

Flyway

Spring Boot Actuator

Spring Boot Test
```

필요하다면 Lombok을 사용할 수 있으나 필수는 아니다.

---

## 40.3 Package

다음 구조를 생성한다.

```text
com.fanpulse

├── event
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
│
└── common
    ├── config
    ├── exception
    └── response
```

Phase 1A에서는 Event Domain 구현에 집중한다.

`vote`, `claim`, `ranking` Package와 빈 Class를 미리 생성하지 않는다. 해당 Package는 Phase 1B에서 실제 구현과 함께 추가한다.

---

# 41. Event 구현

Event Entity:

```text
id
title
description
category
startAt
endAt
createdAt
updatedAt
```

`status`는 Entity Field나 Database Column으로 저장하지 않는다. Domain Method가 주입된 `Clock`의 현재 시각과 `startAt`, `endAt`을 비교하여 계산한다. 테스트에서 System Time에 직접 의존하지 않도록 고정된 `Clock`을 사용한다.

Category:

```text
GAME
MOVIE
ANIME
MUSIC
```

Status:

```text
SCHEDULED
OPEN
CLOSED
```

---

# 42. 최초 API

Phase 1A에서는 다음 API만 우선 구현한다.

```http
GET /api/v1/events?page=0&size=20&category=GAME&status=OPEN
```

```http
GET /api/v1/events/{id}
```

Entity를 직접 반환하지 않는다.

Response DTO를 사용한다.

Query Parameter:

```text
page      기본 0
size      기본 20, 최대 100
category  선택
status    선택, 조회 시각 기준 계산
```

기본 정렬은 `startAt ASC, id ASC`이다.

목록 응답에는 다음 정보를 포함한다.

```text
content
page
size
totalElements
totalPages
```

상세 응답에는 `id`, `title`, `description`, `category`, 계산된 `status`, `startAt`, `endAt`을 포함한다. 존재하지 않는 Event는 `404 Not Found`로 응답한다.

공통 오류 응답:

```json
{
  "code": "EVENT_NOT_FOUND",
  "message": "Event not found",
  "timestamp": "2026-01-01T00:00:00Z",
  "traceId": "..."
}
```

---

# 43. Database

PostgreSQL을 사용한다.

Local PostgreSQL은 Docker Compose로 실행한다.

Database Schema는 Flyway로 관리한다.

JPA 자동 Schema 생성 기능을 Production 방식처럼 사용하지 않는다.

설정:

```text
ddl-auto=validate
```

Application 실행 시 Hibernate가 Table을 생성하거나 변경하지 않는다. Migration과 Entity Mapping이 다르면 시작 또는 Integration Test가 실패해야 한다.

시간 Column은 PostgreSQL `timestamptz`를 사용하고 Application의 기본 저장 기준은 UTC로 통일한다.

환경별 설정:

```text
local  Docker Compose PostgreSQL
test   Testcontainers PostgreSQL
prod   환경 변수로 주입된 PostgreSQL
```

---

# 44. Seed Data

개발 환경에 테스트 Event 데이터를 추가한다.

다음 카테고리를 포함한다.

```text
GAME
MOVIE
ANIME
MUSIC
```

Seed Data가 테스트 실행 시 중복 생성되지 않도록 한다.

Seed Data는 고정 ID를 가진 Flyway Migration으로 한 번만 생성한다. Test는 현재 시각에 따라 바뀔 수 있는 Seed Event의 상태를 가정하지 않고, 고정된 `Clock`과 Test Data를 직접 사용한다.

---

# 45. Actuator

다음 Endpoint가 정상적으로 동작해야 한다.

```http
GET /actuator/health
```

정상:

```json
{
  "status": "UP"
}
```

현재 Phase에서는 Prometheus 전체 구성을 추가하지 않는다.

---

# 46. Test

최소 다음 테스트를 작성한다.

```text
Event Domain Test

Event Service Test

Event Repository Integration Test

Event Controller Test
```

Event Domain Test는 `startAt`, `endAt` 경계 시각과 `startAt < endAt` Validation을 포함한다. Controller Test는 Pagination, Filter, 404와 Entity 비노출을 검증한다.

Event Repository Integration Test에는 PostgreSQL Testcontainers를 사용한다. H2로 PostgreSQL과 Flyway의 호환성을 대신 검증하지 않는다. `./mvnw clean verify` 실행 환경에는 Docker가 필요하며 README와 CI에 이를 명시한다.

---

# 47. README

README에는 최소 다음 내용을 작성한다.

```text
FanPulse 소개

현재 Phase

Requirements

PostgreSQL 실행

Application 실행

Test 실행

구현 API

Architecture

다음 Phase
```

---

# 48. 실행 명령

## Test + Build

```bash
./mvnw clean verify
```

## PostgreSQL

```bash
docker compose up -d
```

## Spring Boot 실행

```bash
./mvnw spring-boot:run
```

---

# 49. Phase 1A — Event Read MVP 완료 조건

```text
[ ] Java 21 사용

[ ] Spring Boot 3.x 사용

[ ] Maven 사용

[ ] Maven Wrapper 포함

[ ] Spring Boot 실행 성공

[ ] Domain 중심 Modular Monolith 구조

[ ] PostgreSQL 연결 성공

[ ] Flyway Migration 성공

[ ] Event Seed Data 생성

[ ] GET /api/v1/events Pagination / Filter 정상 동작

[ ] GET /api/v1/events/{id} 정상 동작

[ ] Event Status가 startAt / endAt 기준으로 계산됨

[ ] 시간 경계 Test가 고정 Clock으로 통과

[ ] Entity 직접 노출 없음

[ ] 공통 오류 응답과 404 동작

[ ] /actuator/health = UP

[ ] Maven Test 통과

[ ] PostgreSQL Testcontainers Integration Test 통과

[ ] ./mvnw clean verify 성공

[ ] docker compose 환경 재현 가능

[ ] README 작성

[ ] Repository Secret 없음

[ ] 불필요한 Microservice 없음

[ ] Redis / Kafka / Kubernetes를 아직 추가하지 않음

[ ] vote / claim / ranking 빈 Package를 미리 생성하지 않음
```

---

# 50. Codex 작업 제한

Codex는 다음 지침을 반드시 지킨다.

```text
docs/FanPulse 프로젝트 기획서.md의 현재 Phase 범위만 작업한다.

명시적으로 요청되지 않은 다음 Phase 작업을 선행하지 않는다.

불필요하게 새로운 Framework나 Library를 추가하지 않는다.

Microservice를 임의로 생성하지 않는다.

먼저 동작하는 최소 구현을 완성한다.

작업 완료 후 실행한 Test와 결과를 요약한다.

구현 과정에서 Canonical 기획서와 충돌하는 판단이 필요한 경우,
기존 구조를 임의로 변경하지 말고 변경 이유를 명확히 기록한다.
```

---

# 51. 프로젝트 핵심 스토리

FanPulse의 최종 포트폴리오 스토리는 다음과 같은 흐름을 목표로 한다.

```text
Modular Monolith 구축

        ↓

Kubernetes 배포

        ↓

Observability 구축

        ↓

이벤트 오픈 Spike Traffic 재현

        ↓

API / JVM / DB 병목 발견

        ↓

HPA / Redis 등을 적용해 개선

        ↓

동기 Claim 처리 병목 발견

        ↓

Kafka + Claim Worker로 분리

        ↓

Kafka Lag 발생

        ↓

KEDA 기반 Worker Scaling

        ↓

장애 실험

        ↓

탐지 → 분석 → 복구 → 개선

        ↓

Before / After 성능 비교

        ↓

Postmortem

        ↓

AWS EKS 최종 검증
```

FanPulse는 단순히 다양한 기술을 연결한 프로젝트가 아니라,

> **실제 부하와 장애 데이터를 바탕으로 애플리케이션 및 인프라 구조를 점진적으로 개선한 DevOps/SRE 프로젝트**

가 되는 것을 최종 목표로 한다.
