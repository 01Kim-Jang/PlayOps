# TODO

## 프로젝트 소스 git 연결 생성

## 프로젝트 소스 코드검사 - 소나큐브

## 시나리오 케이스 사용여부 관리

- 프로젝트별 테스트 케이스 enabled/disabled 상태를 DB에 저장한다.
- 기본 전체 실행 시 disabled 케이스 제외 기준을 정한다.
- 소스 변경으로 grep/title/line이 바뀌었을 때 기존 설정 매칭 전략을 설계한다.

## 개발 API 재시작 안정화

- `docker-compose.dev.yml`의 API command가 `gradle classes --continuous`와 `gradle bootRun`을 동시에 실행해 재시작 시 `build/classes` 삭제 충돌이 발생할 수 있다.
- devtools/continuous build 실행 방식을 하나로 정리하거나, 컨테이너 내부 build 디렉터리 분리 여부를 검토한다.

## Docker 디스크 사용량 지표 보강

- 현재 컨테이너 관리 화면은 Docker `stats` 기반 CPU, 메모리, 네트워크 I/O, Block I/O를 표시한다.
- 컨테이너별 writable layer 디스크 사용량은 `docker ps --size` 또는 inspect 기반 별도 조회가 필요하다.
- 디스크 조회는 느릴 수 있고 환경별 편차가 있어 별도 설계 후 추가한다.

## 메인대시보드, 진입첨 , 프로젝트 변경 처리

## 사용자별 프로젝트 권한 제어 

## 실행/결과 API 확인

## 알림 기능 추가

## 큐 목록 조회 ..(대기열, 실행 예약 기능)

## 템플릿 등록 기능 /templates

## 러너 상태 동기화 및 정리
- API 시작 시 DB의 프로젝트 러너 설정과 실제 playops-runner-* Docker 컨테이너 상태를 비교해 동기화

- 병렬 실행을 위한 러너 슬롯 검토
프로젝트별 여러 슬롯 운영 가능
예: playops-runner-test-npims-stg-skax-co-kr-1
playops-runner-test-npims-stg-skax-co-kr-2

- 스케줄러는 같은 컨테이너를 공유하지 않고 유휴(Idle) 슬롯을 선택해 실행하도록 개선 필요


## Runner lifecycle and sharing

- Reconcile runner state on API startup.
  - Compare DB project runner settings with real `playops-runner-*` Docker containers.
  - Mark missing containers as stopped.
  - Detect orphan runners that have no DB project or belong to projects with Docker disabled.
  - Add a safe bulk cleanup action for orphan/disabled runners when no execution is running.

- Include environment identity in runner container names.
  - Example: `playops-runner-test-npims-stg-skax-co-kr`.
  - Keep backward cleanup for the old `playops-runner-{projectId}` names.

- Keep project-scoped runners as the default until shared runners are proven safe.
  - Current container identity is project-based, while runner images are shared by Node/Playwright version.
  - A shared runner by version can reduce containers, but needs strict isolation for working directory, env vars, caches, reports, and concurrent `docker exec` calls.
  - Revisit shared runners only after execution queueing, per-run workspace isolation, and cleanup guarantees are implemented.

- Consider project runner slots for parallel execution.
  - Example: `playops-runner-test-npims-stg-skax-co-kr-1`, `...-2`.
  - The scheduler should pick an idle slot instead of running multiple tests in the same project container.
