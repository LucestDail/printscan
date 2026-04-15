# PrintScan — 상세 구현 계획

## 1. 프로젝트 비전

중소기업 프린터/스캐너 연동을 통한 ERP 연동 자동화 관리 프로그램. 인쇄·스캔·QR 관리를 통합하고, 문서 자동 분류, 재고 관리, 비용 추적까지 제공.

## 2. 현재 상태

- Spring Boot 3.3.10 + JPA + MyBatis + JWT
- 인쇄 요청/관리, Zebra USB ZPL 출력
- QR 코드 생성/검증 (ZXing)
- 작업 큐, 사용자/권한, 알림, 활동 로그
- 정적 HTML + Bootstrap 5 프론트엔드
- `ddl-auto=create-drop` (운영 위험)
- JWT 시크릿, DB 자격증명 하드코딩
- Dockerfile 부재

## 3. 디자인 시스템

**Google Material Design 3 (M3) 준용**

### M3 적용 방침
정적 HTML/Bootstrap → 점진적 M3 전환:

### 컬러 시스템
| 토큰 | Light | Dark | 용도 |
|------|-------|------|------|
| Primary | `#00639B` | `#96CBFE` | 주요 액션, 인쇄 |
| On Primary | `#FFFFFF` | `#003351` | Primary 위 텍스트 |
| Secondary | `#4E616D` | `#B6CAD7` | 보조, 스캔 |
| Tertiary | `#5D5B7D` | `#C5C3EA` | QR, 특수 기능 |
| Surface | `#F7F9FF` | `#1A1C1E` | 카드/배경 |
| Error | `#BA1A1A` | `#FFB4AB` | 에러/장애 |

> 산업용 소프트웨어: 신뢰감 있는 블루 계열 Primary

### 타이포그래피
- 본문: Pretendard / Noto Sans KR
- 코드/QR: Roboto Mono
- M3 Type Scale 적용

### 핵심 컴포넌트
- 데이터 테이블: M3 기반 정렬/필터/페이징
- 상태 칩: 작업 상태 표시 (대기/처리중/완료/오류)
- Navigation Drawer: 좌측 메뉴 (인쇄/스캔/QR/관리)
- 다이얼로그: 승인/확인 플로우
- `border-radius: 12px` (M3 Medium)

---

## 4. 단계별 구현 계획

### Phase 1 — 보안 + 안정화 (3주)

**1.1 운영 안전성 확보**
- [ ] `ddl-auto=create-drop` → `validate` 변경
- [ ] Flyway 마이그레이션 도입 (기존 `schema.sql` → Flyway 스크립트 전환)
- [ ] `data.sql` 초기 데이터 → Flyway 데이터 마이그레이션

**1.2 보안 수정**
- [ ] JWT 시크릿 → 환경 변수 `${JWT_SECRET}`
- [ ] DB 자격증명 → 환경 변수 분리
- [ ] CORS `allowed-origins=*` → 운영 도메인 제한
- [ ] 기본 계정 `admin/admin` → 초기 설정 시 비밀번호 변경 강제
- [ ] 비밀번호 정책 적용 (최소 8자, 복합 문자)

**1.3 Docker 환경 구축**
- [ ] Dockerfile 작성 (multi-stage build)
- [ ] docker-compose.yml (앱 + MariaDB)
- [ ] 환경 변수 기반 설정 (`application-prod.properties`)

**1.4 개발 환경 분리**
- [ ] `application-dev.properties` — H2, ddl-auto=create
- [ ] `application-prod.properties` — MariaDB, ddl-auto=validate
- [ ] 프로필 스위칭 (`SPRING_PROFILES_ACTIVE`)

### Phase 2 — ERP 핵심 기능 (5주)

**2.1 스캔 문서 자동 분류**
- [ ] 스캐너 연동 확장 — 스캔 결과 파일 자동 수신
- [ ] AI OCR 통합 — 스캔 이미지에서 텍스트 추출
- [ ] 문서 타입 분류 — AI가 송장, 영수증, 계약서, 기타로 자동 분류
- [ ] 바코드/QR 자동 인식 — 스캔 이미지에서 코드 자동 추출 → 제품 연결

**2.2 재고 관리**
- [ ] 제품 엔티티 확장 — 재고 수량, 위치, 최소 재고량
- [ ] QR 기반 입출고 — QR 스캔 → 입고/출고 기록
- [ ] 재고 현황 대시보드 — 실시간 재고 현황, 부족 알림
- [ ] 재고 이력 — 입출고 타임라인

**2.3 프린터 상태 모니터링**
- [ ] SNMP 또는 JMX 기반 프린터 상태 조회
- [ ] 토너/잉크 잔량 표시
- [ ] 용지 상태, 에러 상태 감지
- [ ] 장애 발생 시 관리자 알림

**2.4 출력 비용 관리**
- [ ] 부서별/사용자별 인쇄 건수 및 비용 추적
- [ ] 월별 통계 차트
- [ ] 컬러/흑백, 양면/단면별 비용 차등
- [ ] 예산 초과 경고

### Phase 3 — ERP 연동 + 워크플로우 (4주)

**3.1 외부 ERP 연동 어댑터**
- [ ] ERP 연동 인터페이스 설계 (추상화 레이어)
- [ ] 더존 ERP 어댑터 (API 연동)
- [ ] SAP 어댑터 (RFC/REST)
- [ ] CSV 가져오기/내보내기 (범용)

**3.2 승인 워크플로우**
- [ ] 대량 인쇄 승인 (설정 임계값 초과 시)
- [ ] 컬러 인쇄 승인
- [ ] 승인 요청 → 관리자 알림 → 승인/거부
- [ ] 승인 이력 감사 로그

**3.3 모바일 인쇄**
- [ ] 반응형 웹 UI (M3 모바일 대응)
- [ ] 모바일에서 인쇄 요청 제출
- [ ] QR 스캐너 (카메라) — 모바일 카메라로 QR 스캔
- [ ] 인쇄 상태 알림

### Phase 4 — UI 리뉴얼 + 안정화 (3주)

**4.1 M3 UI 전환**
- [ ] Bootstrap → M3 컴포넌트 점진 전환
- [ ] Navigation Drawer (좌측 메뉴)
- [ ] 데이터 테이블 M3 스타일
- [ ] 상태 칩/배지
- [ ] 다크/라이트 모드

**4.2 Zebra 프린터 연동 강화**
- [ ] 하드코딩 이름 패턴 → 자동 검색 + 모델별 프리셋
- [ ] ZPL 템플릿 관리 (라벨 디자인 커스터마이징)
- [ ] 미리보기 기능

**4.3 테스트 및 안정화**
- [ ] JPA 엔티티 테스트
- [ ] API 통합 테스트
- [ ] 프린터 연동 모킹 테스트

---

## 5. 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| 언어 | Java 17 | — |
| 프레임워크 | Spring Boot 3.3.10 | — |
| 보안 | Spring Security + JWT | — |
| DB | MariaDB (운영), H2 (개발) | — |
| ORM | JPA + MyBatis | — |
| 마이그레이션 | Flyway | 신규 추가 |
| QR | ZXing | — |
| OCR | Tesseract 또는 Google Vision | 신규 추가 |
| 프린터 | javax.print (ZPL) | — |
| 디자인 | M3 원칙 적용 | 신규 전환 |
| 컨테이너 | Docker | 신규 추가 |
