# AI-TestOps

**Opensource(Playwright) 기반 테스트 자동화 플랫폼 — 프로젝트 등록부터 시나리오 작성, 실행, 결과 분석, 팀 협업까지 하나의 웹 서비스에서 처리합니다.** AI 어시스턴트가 시나리오 생성·실패 원인 분석·자율 수정을 곳곳에서 보조합니다.

전담 QA 인력이 없는 개발팀도 스스로 신뢰할 수 있는 테스트 자동화를 운영할 수 있게 하는 것이 AI-TestOps의 목표입니다.

---

## 프로젝트 차터 (Project Charter)

### 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | **AI-TestOps** — Opensource(Playwright) 기반 테스트 자동화 플랫폼 |
| 저장소 | https://github.com/01Kim-Jang/PlayOps |
| 수행 기간 | 2026-08-28 ~ 진행 중 |
| 소속 / 과정 | 한성대학교 · 2026 상상더하기 프로젝트 |
| 프로젝트 유형 | 웹 기반 SaaS형 사내 도구 (자체 서버 배포) |
| 문서 버전 | v1.0 (2026-09-12) |

### 2. 배경 및 문제 정의

배포 주기는 짧아지는데 검증은 여전히 수동에 머물러 있다. 특히 **전담 QA 인력이 없는 개발팀**에서는 다음 문제가 반복된다.

| 문제 | 현재 팀이 겪는 방식 |
|------|--------------------|
| 회귀 테스트 반복 부담 | 배포마다 같은 시나리오를 사람이 다시 확인 |
| 테스트 코드 진입장벽 | Opensource(Playwright) 문법을 알아야 케이스를 작성할 수 있음 |
| 실패 원인 분석 비용 | 로그를 사람이 읽고 원인을 추정 |
| 결과 가시성 부족 | 실패가 났는지 별도로 찾아봐야 함 |

AI-TestOps는 이 네 가지를 **하나의 웹 서비스** 안에서 해결하는 것을 목표로 한다.

### 3. 목적 및 목표

**목적** — Opensource(Playwright) 전문 지식이 없는 개발자도 스스로 신뢰할 수 있는 테스트 자동화를 운영할 수 있게 한다.

| # | 목표 | 측정 지표 |
|---|------|-----------|
| G1 | 자연어로 테스트 케이스를 생성 | 요구사항 입력 → 실행 가능한 Opensource(Playwright) 스펙 산출 |
| G2 | AI 제안 코드의 신뢰성 확보 | AI 제안을 **실제 실행해 통과 여부까지 제시** |
| G3 | 실패 원인 분석 자동화 | 실패 로그 → 기술 수준별 진단 제공 |
| G4 | 플랫폼 자체의 회귀 방지 | CI 파이프라인에 **자체 회귀 테스트 통과를 배포 조건으로** 연결 |
| G5 | 안전한 AI 자율 수정 | 모든 AI 변경은 **사람 승인 후** 반영, 사후 검증 1회 추가 |

### 4. 범위

**In Scope**

- 프로젝트 등록 · GitHub 저장소 연동 · Opensource(Playwright) 소스 관리(소스 탐색기 + 에디터)
- 자연어 → 테스트 시나리오 생성, AI 실패 분석, AI 자율 수정 루프(사람 승인 게이트 포함)
- 테스트 실행 오케스트레이션 — 프로젝트별 격리 컨테이너, 예약 실행, 테스트 묶음(Suite)
- 결과 조회 · 대시보드 · Slack 알림 · 사내 게시판
- 사용자/권한 관리, 감사 로그, 시크릿 암호화 저장
- 테스트 서버 배포 및 CI/CD 자동화

**Out of Scope**

- Opensource(Playwright) 외 테스트 프레임워크(Cypress, Selenium 등) 지원
- 모바일 네이티브 앱 테스트
- 부하/성능 테스트
- 멀티 테넌시 및 외부 고객 대상 상용 서비스 운영
- 온톨로지·지식그래프·LangGraph 기반 분석 대시보드 — **기획 단계**, 별도 계획서로 관리

### 5. 주요 산출물

| 구분 | 산출물 |
|------|--------|
| 소프트웨어 | AI-TestOps 웹 애플리케이션 (Web · API · Worker), Docker Compose 배포 구성 |
| 자동화 | GitHub Actions CI/CD 파이프라인, AI-TestOps 자체 회귀 테스트 스위트(5케이스) |
| 설계 문서 | PRD(요구사항분석서), TRD(기술설계서), UI 화면설계서, ERD |
| 기술 문서 | [Architecture](docs/architecture.md), [To-Be Architecture](docs/tobe-architecture.md), [AI Integration Architecture](docs/ai-integration-architecture.md), [AI Job Spec](docs/ai-job-spec.md) |
| 기획 문서 | 개선점 계획서, 온톨로지·지식그래프 대시보드 개발 계획서, BM 다이어그램 |

### 6. 이해관계자 및 역할

| 구분 | 담당 | 역할 |
|------|------|------|
| 지도 | **지도교수님** | 프로젝트 방향 지도, 산출물 검토 및 피드백 |
| 총괄 · 현업 검토 | **박희운 부장님** | 플랫폼 전체 설계 및 구현, 테스트 서버 제공, 현업 관점 사용성 검토 및 개선 의견 제시 |
| 개발 | **장준호** ([@Junho73](https://github.com/Junho73)) | 백엔드·프론트엔드 기능 구현, AI-TestOps 자체 회귀 테스트 스위트 작성, 테스트 서버 배포 및 운영 |
| 개발 | **김윤재** ([@YoonJae00](https://github.com/YoonJae00)) | CI/CD 파이프라인 구축, LLM 공급자 구조 분리, AI 채팅 대화 히스토리 |
| 문서 | **장동현** | 산출물 문서 작성 — PRD(요구사항분석서)·TRD(기술설계서)·UI 화면설계서 작성, ERD 및 데이터 모델 정리, 프로젝트 차터·개선 계획서 등 산출물 형상 관리 |
| 개발 | **박준서** | AI 연동 — AI 시나리오 생성·실패 원인 분석 기능 연동, AI 자율 수정 루프(CODE_FIX) 및 실행 검증 연동, 프롬프트 설계 및 응답 처리 |

### 7. 마일스톤

| 시점 | 마일스톤 | 상태 |
|------|----------|------|
| 2026-08-28 | 플랫폼 초기 구현 — Web/API/Worker 3계층, 실행 오케스트레이션 | ✅ 완료 |
| 2026-08 ~ 09 | AI 기능 통합 — 시나리오 생성, 실패 분석, 자율 수정 루프 | ✅ 완료 |
| 2026-09 초 | 테스트 서버 배포 + CI/CD 자동화 (self-hosted runner) | ✅ 완료 |
| 2026-09-06 | 현업 피드백 반영 — AI 실행 검증, 자체 회귀 테스트, CI 배포 게이트 | ✅ 완료 |
| 2026-09-11 | ERD 산출물 작성 (15개 테이블 / 17개 관계) | ✅ 완료 |
| 이후 | 온톨로지·지식그래프 기반 분석 대시보드 | 📋 기획 단계 |

### 8. 아키텍처 원칙

기능 확장 과정에서도 다음 원칙은 **타협 대상이 아니다.**

1. **실행 격리** — 모든 테스트/AI 작업은 일회성(Ephemeral) 컨테이너에서 실행하고 작업 후 폐기한다.
2. **권한 최소화** — `docker.sock`과 Git 자격증명은 중앙 `api` 서비스만 보유한다.
3. **사람 승인 게이트** — AI가 생성한 코드 변경은 사람이 승인해야 실제 파일에 반영된다(`aiAutoApplyEnabled` 기본값 `false`).
4. **변경 범위 제한** — AI가 수정할 수 있는 경로를 프로젝트별 allow/deny 글롭으로 제한한다.
5. **사후 검증** — 승인된 변경도 회귀 감지를 위해 한 번 더 검증한다.

### 9. 제약사항 및 가정

**제약사항**

- 테스트 서버는 사내 제공 단일 서버이며, 외부 인바운드는 SSH와 HTTP 포트만 개방되어 있다.
- 외부에서 GitHub 호스티드 러너가 서버에 접속할 수 없어 **self-hosted runner**를 사용한다.
- AI 기능은 외부 LLM 공급자(Claude / GPT) API에 의존하며, 사용량에 따라 비용이 발생한다.
- 동시 실행 수는 서버 RAM에 직접 제약된다(`WORKER_CONCURRENCY`).

**가정**

- 대상 프로젝트는 Opensource(Playwright)로 테스트 가능한 웹 애플리케이션이다.
- 저장소 연동 시 사용하는 자격증명은 최소 권한으로 발급된다.
- 팀원은 GitHub 저장소에 대한 접근 권한을 보유한다.

### 10. 리스크 및 대응

| 리스크 | 영향 | 대응 |
|--------|------|------|
| AI가 사실과 다른 코드를 생성 | 잘못된 테스트가 통과로 오인될 수 있음 | AI 제안을 **실제 실행해 검증**하고 사람 승인 게이트를 거친다 |
| AI 자율 수정이 공용 파일을 훼손 | 다른 케이스까지 연쇄 실패 | 경로 allow/deny 제한 + 위험도 평가 + 사후 검증 |
| 플랫폼 자체의 회귀 | 사용자가 먼저 장애를 발견 | CI에 자체 회귀 테스트를 **배포 차단 게이트**로 연결 |
| 단일 서버 자원 한계 | 동시 실행 시 성능 저하 | 실행 동시성 상한 설정, [Runtime Sizing](docs/ops-sizing.md) 기준 운영 |
| LLM API 비용 증가 | 운영비 부담 | 관리자 등록 공용 키로 단일화하고 사용량을 관리 |
| 시크릿 유출 | 자격증명 탈취 | AES-GCM 암호화 저장, `.env` 형상관리 제외, 운영 환경 암호화 키 필수화 |

### 11. 성공 기준

프로젝트 완료 판정은 다음 실측 기준으로 한다.

| 기준 | 목표 | 현재 실측 |
|------|------|-----------|
| 자체 회귀 테스트 | 전 케이스 통과 및 재현성 확보 | **5/5 통과, 3회 연속 재현** (회당 약 82.6초) |
| CI 배포 게이트 | 회귀 발생 시 배포 차단 | **동작 확인** — 실패 시 워크플로 실패 처리 |
| AI 생성 케이스 실행 검증 | 생성 즉시 통과/실패 판별 | **동작 확인** — 4건 중 2건 실패 검출(AI 추정 오류) |
| AI 변경 안전성 | 사람 승인 없는 자동 반영 0건 | **충족** — 기본값 `false` 유지 |
| 배포 자동화 | main 병합 시 무인 배포 | **충족** — 배포 → 헬스체크 → 회귀 게이트 자동 수행 |

---


## 왜 AI-TestOps인가

- **반복적인 회귀 테스트 부담** — 배포 주기는 빨라지는데 수동 QA로는 속도를 따라갈 수 없습니다.
- **테스트 코드 작성 진입장벽** — Opensource(Playwright) 문법을 몰라도 자연어로 요구사항을 적으면 AI가 시나리오를 생성합니다.
- **실패 원인 파악에 드는 인건비** — AI가 실패 로그를 분석하고, 필요하면 스스로 코드를 수정해 재검증까지 시도합니다(사람 승인 후 반영).
- **결과 가시성 부족** — 실패/AI 검토 필요/AI 자동 되돌림 시 Slack으로 즉시 알림이 갑니다.

## 핵심 기능

### 프로젝트 & 시나리오
- GitHub 저장소 연동 — 프로젝트 소유 저장소를 격리된 Docker 샌드박스에서 clone·실행
- Opensource(Playwright) 시나리오 작성/편집 — 소스 탐색기 + Monaco 에디터, 파일 트리 기반 관리
- 자연어 → 테스트 시나리오 생성 — 기존 프로젝트에 자연어 요구사항으로 새 케이스 추가 (승인 전까지는 검토 대기 상태)
- Opensource(Playwright) 템플릿 — 신규 프로젝트 등록 시 기본 소스 자동 생성(`default`, `e2e-standard`)
- 로그인/설정 선행 시나리오 — 세션(storageState)을 재사용해 매 실행마다 로그인 반복 없이 시작

### 실행 & 오케스트레이션
- 프로젝트별 Docker Runner 컨테이너에서 격리 실행 (Ephemeral Scoped Runner)
- 예약 실행(스케줄러) — 요일·시각을 지정한 자동 실행, API 프로세스 내 경량 스케줄러가 트리거만 담당
- Test Suite(테스트 묶음) — 선택한 케이스들을 이름 붙여 저장하고 한 번에 재실행
- 실행 로그 실시간 스트리밍, 케이스별 재실행 이력 조회

### AI 어시스턴트
- **AI 시나리오 생성** — 자연어 요구사항을 Opensource(Playwright) 코드로 변환
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
  worker  # Opensource(Playwright) 실행 워커(Node)
packages/
  common
  playwright-engine
  report-core
infra/
  docker
```

**설계 원칙 — Ephemeral Scoped AI Runner**

- `api`만 `docker.sock`에 접근하는 유일한 컴포넌트이며, 기존 Opensource(Playwright) Runner 패턴을 AI 작업에도 동일하게 적용합니다.
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
| Worker | Node.js, Opensource(Playwright) |
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
프로젝트별 Opensource(Playwright) Runner는 Docker 컨테이너로 유지됩니다.

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

## Opensource(Playwright) 템플릿

프로젝트 등록 시 **기본 Opensource(Playwright) 소스를 자동 생성**합니다 (권장).

| 템플릿 ID | 설명 |
|-----------|------|
| `default` | example.com 샘플 spec + `playwright.config.ts` |
| `e2e-standard` | fixtures, Page Object 샘플 포함 E2E 구조 |

- 소스 위치: `apps/api/src/main/resources/templates/{templateId}/`
- Web: **Opensource(Playwright) 템플릿** 메뉴에서 파일 트리·미리보기
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

- 프로젝트별 Opensource(Playwright) 소스는 `playwright-projects/{projectKey}` 아래 볼륨으로 관리합니다.
- 프로젝트별 Node/Opensource(Playwright) 버전 차이가 크면 워커 이미지를 프로젝트별로 분리하거나, 실행 컨테이너를 동적으로 생성하는 구조로 확장합니다.
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
