# printscan — 공장 라인용 라벨 어플라이언스

> **생산 라인마다 "라즈베리파이 + Zebra 프린터" 유닛을 두고, 라벨을 디자인·인쇄하고,
> 무엇을 얼마나 인쇄/소비했는지 온프렘 또는 클라우드 허브로 통합 관리하는 어플라이언스.**

한글 라벨을 **서버에서 이미지로 래스터화(^GFA)** 해 "화면 미리보기 = 인쇄물"(WYSIWYG)을 보장하고,
인쇄를 곧 **자동 출고 기록**으로 연결해 라인/작업자/제품별 소비를 추적한다. 리세일(제품 판매)을 목표로 한다.

- 📖 제품 정의: [`SOLUTION.md`](SOLUTION.md) · 🧭 아키텍처/로드맵: [`PLAN-V2.md`](PLAN-V2.md)
- 📑 **상세 기능 명세서: [`SPEC.md`](SPEC.md)** (API 레퍼런스·데이터 모델·프로토콜·설정 전수)
- 🚀 파일럿 배포: [`PILOT.md`](PILOT.md) · [`deploy/README-PI.md`](deploy/README-PI.md) · [`deploy/README-HUB.md`](deploy/README-HUB.md)

---

## 1. 구성 (2 모듈)

| 모듈 | 역할 | 포트 | DB |
|---|---|---|---|
| [`edge/`](edge) | **온디바이스(Pi)** — 로컬 UI + 라벨 래스터 렌더(^GFA) + 로컬 인쇄 + 스캔/재고 + 허브 동기화 | 8091 | H2(파일) |
| [`cloud/`](cloud) | **허브** — 멀티테넌트 플릿 관리 + 출력/소비 집계 + 네트워크 출력 지시 | 8092 | H2(온프렘) / Postgres(SaaS) |
| [`design/`](design) | Apple 스타일 디자인 토큰(edge·cloud 공용) | — | — |
| [`deploy/`](deploy) | systemd 유닛 + Pi 설치/프로비저닝/수용검사 스크립트 | — | — |

```
   [ Zebra 프린터 ] --USB--> [ 라즈베리파이 : edge(8091) ] --아웃바운드 HTTP 폴링--> [ 허브 : cloud(8092) ]
        라벨 실물                로컬 UI·래스터·재고                            플릿 관리·집계·원격 인쇄 지시
```

- **통신**: 디바이스가 허브로 **나가기만** 하는 아웃바운드 HTTP 폴링(방화벽/NAT 친화). 인쇄 지시는 허브 잡 큐 → 디바이스가 폴링으로 수령 → 로컬 인쇄 → ack.
- **토폴로지**: 허브는 동일 `cloud/` 모듈을 **(A) 온프렘 MAIN**(공장장 PC, 폐쇄망) 또는 **(B) AWS SaaS**(TLS+admin-token+테넌트 격리)로 배포.

---

## 2. 핵심 기능

### 2-1. 라벨 엔진 (서버 래스터화 → ^GFA)
- Java2D + **Noto Sans CJK KR** 로 라벨을 이미지로 렌더 → Zebra `^GFA` 그래픽으로 변환. **미리보기 PNG = 인쇄물** 동일 렌더.
- 요소 종류: **TEXT(한글)·QR·DataMatrix·Code128 바코드(옵션 GS1-128)·BOX**. 좌표·크기 모두 **mm 기준**(물리 규격 독립, dpi로 px 변환).
- 캔버스 드래그 디자이너 + 템플릿 CRUD. `{{변수}}` 치환.

### 2-2. 소비 추적 (인쇄 = 자동 출고)
- 라벨이 **제품 코드**(`{{code}}`)를 담고 그 코드가 등록 제품이면, 인쇄 매수만큼 재고를 **자동 OUT** 하고 **라인 + 작업자 + 시각**을 이력에 남긴다.
- 허브 대시보드에서 **라인별 / 작업자별 / 제품별 소비 집계** → "누가 뭘 얼마나 가져갔나".

### 2-3. 일련번호 배치 출력
- `prefix + 시작번호 + 자리수 패딩`으로 N장 연속 증가 출력. 예: `NET-0001 … NET-0100`. 라벨별 소비 기록(중간 실패해도 드리프트 없음).

### 2-4. 네트워크(원격) 출력
- 허브에서 대상 장비에 인쇄 지시 → 잡 큐(`QUEUED`) → 디바이스 폴링 수령(`SENT`, 원자적 클레임) → 로컬 인쇄 → ack(`DONE`/`FAILED`). **멱등**(재전달 시 재인쇄 금지) + **정체 잡 리퍼**(재큐).

### 2-5. 멀티테넌시 & 보안
- 조직(org)별 **org-key** 로 테넌트 격리. **키 로테이션**(유예기간 무중단 교체 + 유출 시 즉시 폐기 + 감사).
- edge: HTTP Basic. cloud: 세션 로그인(온프렘) / admin-token(SaaS) + IP 레이트리밋 + 입력 상한.

### 2-6. 국제화 (비개발자·외국인 작업자)
- UI + **서버 에러 메시지**까지 **한국어/영어/베트남어/인도네시아어** 로케일 번역(쿠키/`?lang=`). 상태·용어는 비개발자 친화.

### 2-7. 운영/관측성
- systemd 상주(부팅 자동·크래시 재시작). `/actuator/health`(printer·cloud 실도달성 컴포넌트) + Prometheus 메트릭 + webhook 알림(저재고·프린터 미발견·백업 실패·오프라인) + 자동 백업/복원.

---

## 3. 기술 스택

| 구분 | 기술 |
|---|---|
| 언어/런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.3.10 (Web, Data JPA, Security, Actuator) |
| 라벨 렌더 | Java2D + Noto Sans CJK KR → `^GFA`(ACS 압축) |
| 바코드/2D | ZXing (QR·DataMatrix·Code128·GS1-128) |
| 프린터 I/O | CUPS raw 큐(USB) / TCP 9100(network) / `/dev/usb/lp0`(rawdev) |
| DB | H2(파일, 온프렘 기본) · PostgreSQL(SaaS, Flyway 마이그레이션) |
| 프론트 | 서버렌더(Thymeleaf) + Vanilla JS + Apple 스타일 디자인 토큰 |
| 관측성 | Actuator, micrometer-prometheus |
| API 문서 | OpenAPI 3(springdoc): `/swagger-ui.html` · `/v3/api-docs` |
| 빌드 | Maven (`./mvnw`) |
| 배포 | systemd + `deploy/` 스크립트, (SaaS) Caddy TLS 프록시 |

---

## 4. 빠른 시작 (개발)

```bash
# edge (온디바이스)
./mvnw -f edge/pom.xml spring-boot:run       # http://localhost:8091  (Basic: admin/printscan)

# cloud (허브)
./mvnw -f cloud/pom.xml spring-boot:run      # http://localhost:8092  (기본 조직 ORG-DEMO-KEY 자동 부트스트랩)

# 테스트
./mvnw -f edge/pom.xml test                  # edge 28
./mvnw -f cloud/pom.xml test                 # cloud 23

# 패키지(fat jar)
./mvnw -f edge/pom.xml clean package -DskipTests
./mvnw -f cloud/pom.xml clean package -DskipTests
```

**실기계 파일럿 설치**는 [`PILOT.md`](PILOT.md) §4 (install → provision → acceptance) 참조.

---

## 5. 문서 맵

| 문서 | 내용 |
|---|---|
| [`SPEC.md`](SPEC.md) | **상세 기능 명세** — API 레퍼런스, 데이터 모델, 라벨 스키마, 동기화 프로토콜, 설정 전수 |
| [`SOLUTION.md`](SOLUTION.md) | 제품 관점 정의(물리 구성·토폴로지·핵심 기능) |
| [`PLAN-V2.md`](PLAN-V2.md) | v2 아키텍처/구현 로드맵 |
| [`PILOT.md`](PILOT.md) | 실기계 파일럿 준비(BOM·이미지화·설치·수용검사·punch-list) |
| [`deploy/README-PI.md`](deploy/README-PI.md) · [`deploy/README-HUB.md`](deploy/README-HUB.md) | Pi·허브 설치 가이드 |
| [`deploy/README-IMAGE.md`](deploy/README-IMAGE.md) | 골든 SD 이미지 제작·복제(대량 배포) |
| [`deploy/RUNBOOK.md`](deploy/RUNBOOK.md) | **운영 Runbook** — 현장/지원 장애 대응(증상→진단→조치) |
| [`deploy/OBSERVABILITY.md`](deploy/OBSERVABILITY.md) · [`deploy/grafana-dashboard.json`](deploy/grafana-dashboard.json) | Prometheus 스크레이프·메트릭 카탈로그·PromQL + Grafana 대시보드 |
| [`deploy/CASE-SPEC.md`](deploy/CASE-SPEC.md) | 라인 유닛 3D 케이스 요구사항 명세 |
| [`REVIEW-2026-08-08.md`](REVIEW-2026-08-08.md) | 종합 검토 기록 |

> 저장소: github.com/LucestDail/printscan · 라이선스: 사내/리세일(별도 명시 전까지 비공개).
