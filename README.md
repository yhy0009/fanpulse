# FanPulse

FanPulse는 이벤트 오픈 순간의 급격한 트래픽과 장애를 재현하고, 관측과 개선 과정을 검증하기 위한 엔터테인먼트 이벤트 플랫폼입니다. 현재는 **Phase 1A — Event Read MVP**만 구현되어 있습니다.

기준 문서는 [`docs/FanPulse 프로젝트 기획서.md`](docs/FanPulse%20프로젝트%20기획서.md)입니다.

## 현재 구현 범위

- Java 21 + Spring Boot 3.5 기반 단일 Event API
- Domain 중심 Modular Monolith 패키지 구조
- PostgreSQL + Flyway 스키마 및 고정 Seed Event
- Event 목록 Pagination/Category/Status 필터와 상세 조회
- `Clock`을 주입받아 `startAt`, `endAt`으로 계산하는 Event 상태
- 공통 오류 응답과 요청 `traceId`
- Actuator health endpoint
- PostgreSQL Testcontainers 통합 테스트

Redis, Kafka, Vote, Claim, Ranking, Kubernetes는 아직 포함하지 않습니다.

## Requirements

- Java 21
- Docker와 Docker Compose
- Unix 계열 환경에서는 `bash` 또는 `sh`

Maven은 별도 설치하지 않아도 됩니다. Maven Wrapper가 Maven 3.9.16을 사용합니다. `./mvnw clean verify`는 실제 PostgreSQL Testcontainers 테스트를 수행하므로 Docker daemon이 실행 중이어야 합니다.

## PostgreSQL 실행

저장소 루트에서 실행합니다.

```bash
cp .env.example .env
docker compose up -d
```

기본 로컬 접속 정보는 `fanpulse/fanpulse`, Database는 `fanpulse`, Port는 `5432`입니다. 이 값은 로컬 개발 전용이며 `.env`로 덮어쓸 수 있습니다.

종료:

```bash
docker compose down
```

## Application 실행

```bash
cd apps/event-api
./mvnw spring-boot:run
```

기본 `local` Profile은 다음 환경 변수를 지원합니다.

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

## Test + Build

Docker daemon을 실행한 뒤:

```bash
cd apps/event-api
./mvnw clean verify
```

## API

### Event 목록

```http
GET /api/v1/events?page=0&size=20&category=GAME&status=OPEN
```

- `page`: 기본 `0`
- `size`: 기본 `20`, 범위 `1..100`
- `category`: `GAME`, `MOVIE`, `ANIME`, `MUSIC` 중 선택
- `status`: `SCHEDULED`, `OPEN`, `CLOSED` 중 선택
- 정렬: `startAt ASC, id ASC`

### Event 상세

```http
GET /api/v1/events/{id}
```

없는 Event는 다음 형태의 `404 Not Found`를 반환합니다.

```json
{
  "code": "EVENT_NOT_FOUND",
  "message": "Event not found",
  "timestamp": "2026-01-01T00:00:00Z",
  "traceId": "..."
}
```

### Health

```http
GET /actuator/health
```

## Architecture

```text
com.fanpulse
├── event
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── presentation
└── common
    ├── config
    ├── exception
    └── response
```

애플리케이션은 `apps/event-api`의 단일 Maven Module입니다. Event JPA Entity는 API DTO로 직접 노출하지 않으며, DB 스키마는 Flyway만 변경하고 Hibernate는 `ddl-auto=validate`로 매핑을 검증합니다.

## Branch 전략

- `dev`: 통합 개발 브랜치
- `feat/*`: 기능 추가
- `fix/*`: 버그 수정
- `refactor/*`: 동작 변경 없는 구조 개선
- `docs/*`: 문서 변경
- `chore/*`: 빌드와 유지보수 작업
- `delete/*`: 기능이나 코드 제거

작업 브랜치는 `dev`에서 생성하고 검증 후 `--no-ff` merge로 `dev`에 합칩니다. 운영 릴리스가 필요할 때만 `dev`를 `main`으로 승격합니다.

## 다음 Phase

Phase 1B에서 Candidate, Vote 중복 방지, EventInventory, 동기 Claim, PostgreSQL 집계 Ranking과 동시성 테스트를 추가합니다.
