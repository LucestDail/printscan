# printscan v2

**공장 생산라인용 라벨 어플라이언스** — 라인마다 "라즈베리파이 + Zebra 프린터"를 두고,
라벨을 디자인·인쇄하고, 무엇을 얼마나 인쇄/소비했는지 온프렘 또는 클라우드 허브로 통합 관리한다.

> 제품 정의: [`SOLUTION.md`](SOLUTION.md) · 아키텍처/로드맵: [`PLAN-V2.md`](PLAN-V2.md)

## 구성
| 모듈 | 역할 | 포트 |
|---|---|---|
| [`edge/`](edge) | 온디바이스(Pi) — 서버렌더 UI + 라벨 래스터 렌더(^GFA) + 로컬 인쇄 + 스캔/재고 + 허브 동기화 | 8091 |
| [`cloud/`](cloud) | 허브 — 멀티테넌트 플릿 관리 + 출력/소비 집계 + 네트워크 출력 지시. 온프렘 MAIN 또는 AWS SaaS 배포 | 8092 |
| [`design/`](design) | Apple 스타일 디자인 토큰(edge·cloud 공용) | — |
| [`deploy/`](deploy) | systemd 유닛 + Pi 설치 스크립트 + 배포 가이드 | — |

## 핵심 기능
- **라벨 엔진**: 서버 래스터화 → ZPL `^GFA`. 한글/QR/바코드/박스, **미리보기 = 인쇄물(WYSIWYG)**. 캔버스 드래그 디자이너.
- **소비 추적**: 인쇄 = 자동 출고. 라인/작업자/제품별 집계로 "누가 뭘 얼마나 가져갔나" 확인.
- **일련번호 배치**: `NET-0001…0100` 자동증가 연속 출력.
- **네트워크 출력**: 허브에서 원격 인쇄 지시 → 디바이스 폴링 → 로컬 인쇄 → 결과 집계.

## 빌드 & 실행 (개발)
```bash
# edge (온디바이스)
./mvnw -f edge/pom.xml spring-boot:run          # http://localhost:8091
# cloud (허브)
./mvnw -f cloud/pom.xml spring-boot:run         # http://localhost:8092
# 테스트
./mvnw -f edge/pom.xml test
```
- edge 클라우드 연동: `--printscan.cloud.enabled=true --printscan.cloud.base-url=http://<hub>:8092`
- 프린터: `printscan.printer.mode=cups|network|rawdev` (USB=cups raw 큐 권장). 한글=Noto CJK 폰트.

## 배포
- **Pi(라인 유닛)**: [`deploy/README-PI.md`](deploy/README-PI.md) — `install-edge.sh`로 java17+CUPS+폰트+raw큐+systemd 자동.
- **허브(온프렘/AWS)**: [`deploy/README-HUB.md`](deploy/README-HUB.md) — 온프렘은 경량, SaaS는 TLS(리버스프록시)+admin-token.

## 상태
P0~P7 완료 · 실기계(Zebra ZD421) end-to-end 실증(한글 인쇄·네트워크 출력·소비추적·배치). 상세는 커밋 히스토리/`PLAN-V2.md`.
