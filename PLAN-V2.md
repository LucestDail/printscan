# printscan v2 — 아키텍처 & 로드맵 (전면 재구축)

> 작성 2026-08-07 · 구버전(커밋 `de0f26c`, Spring Boot MES/ERP)은 .25에서 실기계 END-TO-END 검증 완료 → 이제 **제품화 재구축**.
> 제품: **라즈베리파이 + Zebra 프린터 어플라이언스** 리세일. 온디바이스 직접출력 + 클라우드 통합관리/네트워크출력. **엔터프라이즈급**.

---

## 0. 확정 결정 (2026-08-07 사용자)

| 축 | 결정 | 이유 |
|---|---|---|
| 프론트엔드 | **서버렌더 유지**(Thymeleaf + 경량 JS) | Pi 경량·오프라인 단순, 빌드스텝 최소 |
| 구성 | **로컬퍼스트 + 클라우드 동기화** | 오프라인에도 현장 인쇄 계속(어플라이언스 필수) |
| 라벨 렌더 | **서버 래스터화 → ZPL `^GFA`** | 한글/유니코드 완전지원 + 디자이너 100% WYSIWYG + QR잘림 원천해결 |
| 디자인 | **Apple 스타일** (`design/tokens.css`) | 사진우선·단일 블루·저밀도 갤러리형 |

---

## 1. 시스템 구성

### 1-1. On-Device — `printscan-edge` (Pi에서 구동)
- **단일 Spring Boot 앱** (ARM64 Pi OS, OpenJDK 17). UI·렌더·인쇄·동기화를 한 프로세스에서.
- **로컬 UI**: Thymeleaf 서버렌더 + Apple 디자인. 관리자는 Pi의 웹(같은 LAN) 또는 Pi 화면(키오스크)에서 접속.
- **로컬 DB**: 파일 기반(SQLite 또는 H2 file) — 오프라인 완전동작, Pi 경량.
- **프린터 직결**: 구버전 `PrintTransport` 계승 — USB(CUPS raw 큐 / `/dev/usb/lp0`) + 네트워크(IP:9100). `printer.mode`로 흡수.
- **라벨 래스터 엔진(신규 핵심)**: 템플릿(요소 JSON) → Java2D 렌더 → 1bpp 디더 → `^GFA`. 미리보기 PNG도 동일 렌더 → **WYSIWYG**. (Labelary 외부 의존 제거)
- **스캐너**: 바코드 HID(브라우저 전역 캡처) → 제품조회 → 입출고.
- **오프라인 큐**: 출력이력·재고변동·상태를 로컬 append 로그 → 온라인 시 클라우드로 업싱크(idempotency key).
- **디바이스 아이덴티티**: 최초 클라우드 페어링으로 device token/cert 발급 → 이후 아웃바운드 연결.

### 1-2. Cloud — `printscan-cloud` (우리가 제공)
- **멀티테넌트 관리 SaaS**: 조직 → 디바이스 → 사용자.
- 기능: 디바이스 통합 대시보드, **출력 건수/집계 리포트**, 템플릿 중앙배포, 재고 통합, **네트워크 출력 지시**.
- **통신**: 디바이스가 **아웃바운드 상시연결**(WebSocket/STOMP; 방화벽·NAT 친화). 인쇄지시는 클라우드→디바이스 push. 상태/이력은 디바이스→클라우드.
- 저장: 출력 이벤트 로그, 집계, 템플릿(원천), 재고 스냅샷, 디바이스 상태.

### 1-3. 공유 — `shared`
- 라벨 템플릿 스키마(요소 모델), DTO, 동기화 이벤트 계약. edge/cloud 공용.

---

## 2. 동기화 & 네트워크 출력

### 2-1. 동기화 방향/충돌
- **업싱크**(device→cloud): 출력이력·재고변동·디바이스상태 = **append-only 이벤트**(디바이스가 진실원천, 출력은 물리행위라 되돌릴 수 없음). 클라우드는 집계만.
- **다운싱크**(cloud→device): 템플릿·설정·인쇄지시 = **클라우드 원천**, 버전/ETag로 캐시.
- **오프라인**: 로컬 append 큐 → 재연결 시 배치 flush, idempotency key로 중복방지.

### 2-2. 네트워크 출력 흐름
```
[클라우드 UI] 디바이스X · 템플릿T · 변수V · N장  인쇄 지시
   → 클라우드가 PrintJob 생성(고유 id)
   → 상시연결 채널로 디바이스X에 push
   → 디바이스X: 로컬 래스터(^GFA) + 로컬 인쇄
   → 결과 ack(성공/실패·시각) 업싱크 → 클라우드 집계 반영
```
→ **온디바이스=직접 출력관리 / 클라우드 연결 시=통합관리 + 원격(네트워크) 출력**, 지시하신 두 모드 모두 성립.

---

## 3. 라벨 래스터 파이프라인 (v2 핵심 기술)

1. **템플릿** = 요소 배열(`text`/`qr`/`barcode`/`box`/`image`) + 규격(widthMm, heightMm, dpi).
2. **변수 바인딩** `{{key}}` → 실데이터.
3. **Java2D 렌더**: mm→px 캔버스, **CJK 폰트로 한글 텍스트**, ZXing로 QR/바코드 이미지, 좌표 배치. (디자이너와 동일 코드)
4. **1bpp 변환**: 임계값/디더 → 흑백 비트맵.
5. **`^GFA` 인코딩** → `PrintTransport` 전송.
6. **미리보기** = 4단계 비트맵의 PNG. 화면과 인쇄물이 **완전 동일**.

해결되는 것: ① 한글 인쇄(폰트 자유) ② QR 잘림(경계 계산이 렌더에 내장) ③ Labelary 외부 의존 제거(오프라인 미리보기) ④ 임의 디자인(그라데이션 제외, Apple급 라벨) 인쇄.

---

## 4. 모듈 구조 (제안)

```
printscan/                 # 이 repo — v2로 전환(구버전 코드는 legacy/ 보존)
  edge/                    # 온디바이스 Spring Boot 앱 (Pi)
    src/main/java/.../edge
    src/main/resources/templates   # Thymeleaf (Apple 디자인)
    src/main/resources/static/design → ../../design 링크
  cloud/                   # 클라우드 SaaS Spring Boot 앱 (멀티테넌트)
  shared/                  # 공통 모델·DTO·라벨 스키마
  design/                  # Apple 디자인 토큰/컴포넌트 CSS (edge·cloud 공용) ← 이번 세션
  deploy/                  # Pi 이미지·systemd·docker-compose(cloud)
```
> 서버렌더라 프론트 빌드 스텝 없음. `design/`의 CSS를 edge/cloud 양쪽 Thymeleaf가 그대로 링크.

---

## 5. 로드맵 (Phase)

| Phase | 내용 | 산출 |
|---|---|---|
| **P0 설계·디자인기초** ← *이번 세션* | 아키텍처 + `design/tokens.css`·`components.css` + `preview.html` | 방향 확정·눈으로 검증 |
| P1 Edge 코어 | 래스터 엔진(^GFA) + 로컬출력(USB/9100) + 서버렌더 UI 셸(Apple) + 로컬 DB | Pi에서 한글 라벨 1장 |
| P2 디자이너 | 서버렌더 라벨 디자이너(요소배치→래스터 프리뷰 WYSIWYG) + 템플릿 CRUD | 디자인→인쇄 왕복 |
| P3 스캔/재고 | HID 스캔→제품조회→입출고 트랜잭션 | 현장 한 바퀴 |
| P4 Cloud | 멀티테넌트·디바이스 페어링·대시보드·집계 | 통합관리 |
| P5 동기화+네트워크출력 | 상시연결·업/다운싱크·원격 인쇄지시 | 네트워크 출력 |
| P6 Pi 패키징 | ARM 이미지·systemd·키오스크·OTA | 리세일 이미지 |

---

## 6. 구버전 이관/폐기
- 구버전(`de0f26c`)은 `legacy/`로 보존(참조용). Edge가 **PrintTransport·LabelTemplate·QR 개념 계승**, javax.print→CUPS/9100 유지, ZPL 텍스트생성은 래스터로 대체.
- `.25` 8090 스모크앱은 P1 착수 시 폐기.
- ⚠️ 보안 부채(시크릿 평문·admin/admin·CORS `*`)는 재구축에서 정산(env·시크릿매니저·디바이스 cert).
