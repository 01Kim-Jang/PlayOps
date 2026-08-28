# PlayOps

**Playwright 기반 테스트 자동화 플랫폼 — 프로젝트 등록부터 시나리오 작성, 실행, 결과 분석, 팀 협업까지 하나의 웹 서비스에서 처리합니다.** AI 어시스턴트가 시나리오 생성·실패 원인 분석·자율 수정을 곳곳에서 보조합니다.

전담 QA 인력이 없는 개발팀도 스스로 신뢰할 수 있는 테스트 자동화를 운영할 수 있게 하는 것이 PlayOps의 목표입니다.

---

## 왜 PlayOps인가

- **반복적인 회귀 테스트 부담** — 배포 주기는 빨라지는데 수동 QA로는 속도를 따라갈 수 없습니다.
- **테스트 코드 작성 진입장벽** — Playwright 문법을 몰라도 자연어로 요구사항을 적으면 AI가 시나리오를 생성합니다.
- **실패 원인 파악에 드는 인건비** — AI가 실패 로그를 분석하고, 필요하면 스스로 코드를 수정해 재검증까지 시도합니다(사람 승인 후 반영).
- **결과 가시성 부족** — 실패/AI 검토 필요/AI 자동 되돌림 시 Slack으로 즉시 알림이 갑니다.

## 핵심 기능

### 프로젝트 & 시나리오
- GitHub 저장소 연동 — 프로젝트 소유 저장소를 격리된 Docker 샌드박스에서 clone·실행
- Playwright 시나리오 작성/편집 — 소스 탐색기 + Monaco 에디터, 파일 트리 기반 관리
- 자연어 → 테스트 시나리오 생성 — 기존 프로젝트에 자연어 요구사항으로 새 케이스 추가 (승인 전까지는 검토 대기 상태)
- Playwright 템플릿 — 신규 프로젝트 등록 시 기본 소스 자동 생성(`default`, `e2e-standard`)
- 로그인/설정 선행 시나리오 — 세션(storageState)을 재사용해 매 실행마다 로그인 반복 없이 시작

### 실행 & 오케스트레이션
- 프로젝트별 Docker Runner 컨테이너에서 격리 실행 (Ephemeral Scoped Runner)
- 예약 실행(스케줄러) — 요일·시각을 지정한 자동 실행, API 프로세스 내 경량 스케줄러가 트리거만 담당
- Test Suite(테스트 묶음) — 선택한 케이스들을 이름 붙여 저장하고 한 번에 재실행
- 실행 로그 실시간 스트리밍, 케이스별 재실행 이력 조회

### AI 어시스턴트
- **AI 시나리오 생성** — 자연어 요구사항을 Playwright 코드로 변환
- **AI 자율 수정 루프(CODE_FIX)** — 실패한 테스트를 AI가 스스로 반복 분석·수정 시도 후 재실행으로 통과 검증. 수정 결과는 자동 반영되지 않고 **AI 검토 대기** 화면에서 사람이 승인해야 실제 파일에 적용됩니다. 승인된 변경도 사후 검증(회귀 감지)을 한 번 더 거칩니다.
- **AI 분석** — 실패 원인을 기술 수준(비전공자·주니어·시니어)에 맞춰 진단·해설
- **전역 AI 챗봇** — 로그인 후 모든 화면에서 접근 가능한 도우미, 페이지를 이동해도 대화가 유지됩니다
- 변경 허용 범위는 프로젝트별 `allowedPathGlobs` / `deniedPathGlobs`로 제한되는 안전장치가 있습니다

### 결과 & 협업
- 테스트결과(실패) UI — 실패 케이스를 제목·에러 메시지와 함께 인라인 표시
- 대시보드 — 실행 현황·추이 시각화
- Slack 알림 — 테스트 실패 / AI 검토 필요 / AI 자동 되돌림 시 Webhook으로 즉시 알림
- 공지/게시판

### 운영 & 보안
- 사용자 관리 — 계정(ADMIN/USER) 생성·수정, 비밀번호 해싱(BCrypt), 삭제/역할변경 안전장치, 감사 로그
- Runner 컨테이너 관리 — Docker 상태·정리
- AI 공급자 키(Claude/GPT) 관리자 등록 — AES-GCM으로 암호화 저장, 모든 프로젝트가 공용으로 사용
- 실행 컨테이너에는 git 자격 증명과 `docker.sock`이 전달되지 않으며, egress는 AI 공급자 API와 콜백 엔드포인트로 제한됩니다

## 아키텍처

```text
apps/
  web     # React + Vite 웹 화면
  api     # Spring Boot API 서버 (핵심 오케스트레이션, AI 게이트웨이, 스케줄러)
  worker  # Playwright 실행 워커(Node)
packages/
  common
  playwright-engine
  report-core
infra/
  docker
```

**설계 원칙 — Ephemeral Scoped AI Runner**

- `api`만 `docker.sock`에 접근하는 유일한 컴포넌트이며, 기존 Playwright Runner 패턴을 AI 작업에도 동일하게 적용합니다.
- AI/테스트 실행 컨테이너는 작업 단위로 생성되고 종료되는 일회성 컨테이너입니다. 상시 구동되는 프로젝트별 서버는 두지 않습니다.
- 프로젝트마다 독립된 Docker 볼륨을 사용해 다른 프로젝트의 코드에 접근할 수 없습니다.
- Git 자격 증명은 `api`만 보유하며, 실행 컨테이너는 이미 체크아웃된 코드만 받고 결과/diff를 볼륨에 기록하면 `api`가 커밋·푸시를 대행합니다.
- 실행 컨테이너의 네트워크는 AI 공급자 API·콜백 엔드포인트로만 제한된 격리 네트워크를 사용합니다.

자세한 배경은 [`docs/ai-integration-architecture.md`](docs/ai-integration-architecture.md), [`docs/ai-job-spec.md`](docs/ai-job-spec.md) 참고.

## 기술 스택

| 영역 | 기술 |
|------|------|
| Web | React, Vite, TypeScript, Tailwind CSS, ag-Grid, ECharts, Monaco Editor |
| API | Spring Boot 3(Java 21), Spring Data JPA, PostgreSQL |
| Worker | Node.js, Playwright |
| 실행 격리 | Docker (프로젝트/AI 작업별 컨테이너) |
| AI | Claude / GPT (관리자 등록 공용 키, LLM Gateway로 단일화) |
| 인프라 | Docker Compose, Nginx(리버스 프록시) |

## 빠른 시작

### Docker (운영/통합 확인 모드)

```bash
cp .env.example .env
docker compose up --build
```

- Web: http://localhost:3000
- API: http://localhost:8080/actuator/health
- PostgreSQL: localhost:5432
- 기본 로그인: `admin` / `admin`

### 개발 모드 (권장)

FE/API 코드를 수정하며 개발할 때 사용합니다. Web은 Vite HMR로 브라우저에 바로 반영되고,
API는 Gradle continuous compile + Spring DevTools restart로 변경 사항을 자동 반영합니다.
프로젝트별 Playwright Runner는 Docker 컨테이너로 유지됩니다.

```bash
npm run dev
# 백그라운드 실행이 필요하면:
docker compose -f docker-compose.dev.yml up -d --build
```

종료: `npm run down:dev` · 로그: `npm run logs:dev`

- Web: http://localhost:3000
- API: http://localhost:8080/actuator/health
- API Debug: localhost:5005
- PostgreSQL: localhost:5432

반영 방식:

- `apps/web/src` 수정 → Vite HMR로 브라우저에 즉시 반영
- `apps/api/src/main/java` 수정 → 컨테이너 안에서 재컴파일 후 Spring DevTools가 API 재시작
- `apps/api/src/main/resources` 수정 → DevTools 재시작 대상
- `apps/web/package.json` 또는 API Gradle 의존성 변경 → 컨테이너 재빌드 권장

### 모드 전환

개발 모드와 운영 모드는 같은 컨테이너 이름과 포트를 사용합니다. 전환할 때는 먼저 기존 모드를 내린 뒤 다른 모드를 올립니다.

```bash
docker compose down
docker compose -f docker-compose.dev.yml down

npm run dev   # 또는 npm run prod
```

### 로컬 개발 (dev 프로필 + PostgreSQL, Docker 없이)

1. PostgreSQL만 기동: `docker compose up postgres`
2. API (Java 21, Gradle):

```bash
cd apps/api
gradle bootRun
# 기본: SPRING_PROFILES_ACTIVE=dev → localhost PostgreSQL
```

Java가 PATH에 없다면 프로젝트 내 JDK 사용:

```powershell
$env:JAVA_HOME = (Resolve-Path .\utils\bin\jdk-21).Path
cd apps/api
.\gradlew.bat test
```

3. Web:

```bash
cd apps/web
$env:VITE_API_PROXY="http://localhost:8080"   # Windows PowerShell
npm run dev
```

## Playwright 템플릿

프로젝트 등록 시 **기본 Playwright 소스를 자동 생성**합니다 (권장).

| 템플릿 ID | 설명 |
|-----------|------|
| `default` | example.com 샘플 spec + `playwright.config.ts` |
| `e2e-standard` | fixtures, Page Object 샘플 포함 E2E 구조 |

- 소스 위치: `apps/api/src/main/resources/templates/{templateId}/`
- Web: **Playwright 템플릿** 메뉴에서 파일 트리·미리보기
- API: `GET /api/templates`, `POST /api/projects/{id}/scaffold`

등록 폼에서 템플릿 선택 및 「등록 시 기본 소스 자동 생성」 체크 가능. 기존 파일이 있으면 건너뛰며, 상세 화면에서 **덮어쓰기**로 재생성할 수 있습니다.

## 프로젝트 워크스페이스 (상세 화면)

프로젝트 상세는 탭으로 구성됩니다.

| 탭 | 기능 |
|----|------|
| **소스 탐색기** | 파일 트리, 텍스트 편집·저장, ZIP/템플릿, AI 수정 도움 |
| **시나리오** | spec 파싱 → describe/test 트리, 케이스별 실행, 자연어 케이스 생성 |
| **실행** | 전체/필터 실행, 이력 그리드 (3초 폴링) |
| **결과** | 통과/실패 요약, HTML 리포트 iframe, trace/video/screenshot 링크, 실패 케이스 인라인 표시 |
| **AI 분석** | 수준별(비전공자/주니어/시니어) 실패 원인 진단, AI 자율 수정(Sandbox 루프) 요청 |
| **예약 실행** | 반복 자동 실행 스케줄 관리 |
| **설정** | 환경변수·Docker Runner |

실행은 프로젝트별 Docker Runner 컨테이너에서 `npm install` → `playwright test` 후 `/storage/reports/{projectId}/{executionId}/`에 결과를 저장합니다. `PLAYOPS_DOCKER_ENABLED=true` 필요.

### 고급 기능

- **Monaco 에디터**: 소스 탭에서 TypeScript/JSON 등 구문 강조 편집, AI에게 수정을 요청하면 편집 중인 버퍼가 바로 갱신되고 저장/취소로 확정
- **로그 스트리밍**: 실행 탭·결과 탭에서 `stream.log` 실시간 폴링 (500ms)
- **케이스별 재실행 이력**: 시나리오 탭에서 테스트 케이스 클릭 → 동일 grep 필터 실행 이력 조회·재실행
- **AI 검토 대기**: AI가 생성/수정한 코드는 관리자가 diff를 확인하고 승인해야 실제 파일에 반영

## 운영 방향

- 프로젝트별 Playwright 소스는 `playwright-projects/{projectKey}` 아래 볼륨으로 관리합니다.
- 프로젝트별 Node/Playwright 버전 차이가 크면 워커 이미지를 프로젝트별로 분리하거나, 실행 컨테이너를 동적으로 생성하는 구조로 확장합니다.
- API DB는 dev/prod 모두 PostgreSQL을 사용합니다.
- 예약 실행은 API 프로세스 내 경량 스케줄러가 "지금이 실행 시각인가"만 판단하고, 실제 실행은 수동 실행과 동일하게 일회성 Runner를 생성해 위임합니다.

## 문서

- [Architecture](docs/architecture.md)
- [To-Be Architecture](docs/tobe-architecture.md)
- [AI Integration Architecture](docs/ai-integration-architecture.md)
- [AI Job Spec](docs/ai-job-spec.md)
- [Scenario and Environment Management](docs/scenario-env-management.md)
- [Runtime Sizing](docs/ops-sizing.md)
- [First Run Troubleshooting](docs/first-run-troubleshooting.md)

PRD(요구사항분석서)·TRD(기술설계서)·UI 화면설계서는 별도 산출물 문서로 관리됩니다.
