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

| 지표 | 초기 목표 |
|---|---:|
| Availability | 99.9% 이상 |
| HTTP 5xx Rate | 1% 미만 |
| API p95 Latency | 300ms 이하 |
| 중복 이벤트 처리 | 0건 |
| 장애 탐지 시간 | 1분 이내 |
| Peak Load | 최소 3,000 RPS 검증 |
| API Auto Scaling | HPA 정상 동작 |
| Worker Auto Scaling | Kafka Lag 기반 KEDA 정상 동작 |

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

초기에는 테스트용 사용자 식별자를 Request Body 또는 Header로 전달한다.

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
```

예시:

```json
{
  "userId": 1001,
  "candidateId": 3
}
```

동일 사용자는 같은 이벤트에서 중복 투표할 수 없다.

Database Unique Constraint를 최종 방어선으로 사용한다.

---

# 8. Claim

```http
POST /api/v1/events/{eventId}/claims
```

예시:

```json
{
  "userId": 1001
}
```

동일 사용자는 같은 이벤트를 중복 Claim할 수 없다.

향후 다음 Header 사용을 검토한다.

```http
Idempotency-Key: <uuid>
```

초기에는 동기 방식으로 구현한다.

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

---

# 9. Ranking

```http
GET /api/v1/rankings
```

Redis Sorted Set을 활용한 실시간 Ranking을 구현한다.

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
status
startAt
endAt
createdAt
updatedAt
```

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
id
eventId
userId
status
createdAt
processedAt
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

---

# 24. SLI / SLO

## SLI

```text
Availability
Latency
Error Rate
```

## 초기 SLO

```text
Availability >= 99.9%

p95 Latency <= 300ms

HTTP 5xx < 1%
```

Grafana에서 별도 SLO Dashboard를 구성한다.

---

# 25. Load Test

k6를 사용한다.

## Baseline Test

```text
100 RPS
```

정상 환경 성능 기준을 확보한다.

---

## Load Test

점진적으로 요청을 증가시킨다.

```text
100
500
1,000
2,000 RPS
```

---

## Spike Test

이벤트 오픈 상황을 재현한다.

```text
100 RPS
   ↓
3,000+ RPS
```

---

## Stress Test

서비스가 어느 수준부터 성능이 급격하게 저하되는지 찾는다.

---

## Soak Test

일정 트래픽을 장시간 유지한다.

확인 대상:

```text
Memory Leak
Connection Leak
GC
DB Connection
Kafka Lag
```

---

# 26. Failure Scenario

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

---

## Scenario 2. Redis 장애

```text
Redis Down
 ↓
Cache Miss
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
ArgoCD Rollback
 ↓
v1 복구
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
```

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

프로젝트 목표 AWS 비용:

```text
30,000 KRW 이하
```

비용 또한 프로젝트의 하나의 운영 지표로 관리한다.

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
│   ├── incidents/
│   └── performance/
│
├── .github/
│   └── workflows/
│
├── docker-compose.yml
├── README.md
├── AGENTS.md
└── docs/
    └── PROJECT_SPEC.md
```

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

구현:

- Maven 프로젝트
- Maven Wrapper
- Event
- Vote
- Claim
- Ranking 기본 구조
- PostgreSQL
- Flyway
- Event 목록
- Event 상세
- Vote
- Claim
- Validation
- Exception Handling
- Seed Data
- Test
- Actuator

---

## Phase 2 — Redis

```text
Event Cache
Ranking
Cache Hit / Miss
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
Kafka
 ↓
Claim Worker
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

Codex의 첫 작업에서는 **Phase 1의 첫 번째 Milestone만 구현한다.**

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
├── vote
├── claim
├── ranking
│
└── common
    ├── config
    ├── exception
    └── response
```

첫 번째 Milestone에서는 Event Domain 구현에 집중한다.

나머지 Domain은 필요 이상의 빈 Class를 생성하지 않는다.

---

# 41. Event 구현

Event Entity:

```text
id
title
description
category
status
startAt
endAt
createdAt
updatedAt
```

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

첫 번째 Milestone에서는 다음 API만 우선 구현한다.

```http
GET /api/v1/events
```

```http
GET /api/v1/events/{id}
```

Entity를 직접 반환하지 않는다.

Response DTO를 사용한다.

---

# 43. Database

PostgreSQL을 사용한다.

Local PostgreSQL은 Docker Compose로 실행한다.

Database Schema는 Flyway로 관리한다.

JPA 자동 Schema 생성 기능을 Production 방식처럼 사용하지 않는다.

권장:

```text
ddl-auto=validate
```

또는 해당 Phase에 적절한 안전한 설정을 사용한다.

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

가능하다면 PostgreSQL Integration Test에는 Testcontainers를 사용한다.

단, Testcontainers 도입 때문에 첫 번째 Milestone이 불필요하게 복잡해질 경우 이후에 추가할 수 있다.

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

# 49. Phase 1 첫 번째 Milestone 완료 조건

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

[ ] GET /api/v1/events 정상 동작

[ ] GET /api/v1/events/{id} 정상 동작

[ ] Entity 직접 노출 없음

[ ] /actuator/health = UP

[ ] Maven Test 통과

[ ] ./mvnw clean verify 성공

[ ] docker compose 환경 재현 가능

[ ] README 작성

[ ] Repository Secret 없음

[ ] 불필요한 Microservice 없음

[ ] Redis / Kafka / Kubernetes를 아직 추가하지 않음
```

---

# 50. Codex 작업 제한

Codex는 다음 지침을 반드시 지킨다.

```text
PROJECT_SPEC.md의 현재 Phase 범위만 작업한다.

명시적으로 요청되지 않은 다음 Phase 작업을 선행하지 않는다.

불필요하게 새로운 Framework나 Library를 추가하지 않는다.

Microservice를 임의로 생성하지 않는다.

먼저 동작하는 최소 구현을 완성한다.

작업 완료 후 실행한 Test와 결과를 요약한다.

구현 과정에서 PROJECT_SPEC.md와 충돌하는 판단이 필요한 경우,
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