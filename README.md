# PrintScan — 소규모 MES/ERP 웹 애플리케이션

> Zebra 프린터·스캐너 연동, QR코드 관리, 인쇄 작업 관리 등을 제공하는 소규모 제조 실행 시스템(MES) 웹 애플리케이션입니다.

## 주요 기능

- **인쇄 관리** — 파일 업로드 기반 인쇄 요청, 상태 조회, 작업 이력
- **Zebra USB 출력** — `javax.print`를 통한 ZPL 직접 출력 (ZD421 등)
- **QR코드** — ZXing 기반 QR 생성, 이미지 변환, 검증/사용 처리
- **작업 큐** — 인쇄 작업 대기열 관리 및 처리 제한
- **사용자/권한** — Spring Security + JWT 기반 인증, 역할(Role) 관리
- **제품 관리** — 제품 CRUD, MyBatis 매퍼 기반 조회
- **알림 & 활동 로그** — 작업 알림 및 사용자 활동 추적

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.3.10 |
| 보안 | Spring Security, JJWT 0.11.5 |
| 데이터 | Spring Data JPA, MyBatis, JDBC |
| DB | MariaDB (운영), H2 (개발) |
| QR | ZXing |
| 프론트엔드 | HTML, Vanilla JS, Bootstrap 5 (CDN), Material Design Icons |
| 빌드 | Maven (`mvnw`) |

## 프로젝트 구조

```
printscan/
├── pom.xml
└── src/main/
    ├── java/com/baeksang/printscan/
    │   ├── PrintscanApplication.java    # 진입점
    │   ├── controller/                  # Auth, Print, QR, Product, User 등
    │   ├── service/                     # 비즈니스 로직
    │   ├── repository/                  # JPA Repository
    │   ├── entity/                      # JPA 엔티티
    │   ├── security/                    # JWT 필터, 인증
    │   └── config/                      # Security, JPA, FileStorage
    └── resources/
        ├── application.properties       # DB, JWT, 파일, QR, CORS
        ├── schema.sql / data.sql        # SQL 초기화
        ├── mapper/ProductMapper.xml     # MyBatis 매퍼
        └── static/                      # HTML, CSS, JS, 페이지
```

## 실행

### 사전 요구사항
- JDK 17
- MariaDB (또는 H2로 전환)

### 실행
```bash
./mvnw spring-boot:run
```
기본 포트: `8080`

### 기본 계정
- ID: `admin` / PW: `admin`

## 설정

`src/main/resources/application.properties`:

| 항목 | 설명 |
|------|------|
| DB | `spring.datasource.url`, 사용자, 비밀번호 |
| JWT | `jwt.secret`, `jwt.expiration` |
| 파일 업로드 | `file.upload-dir`, 크기, 허용 확장자 |
| QR | 만료, 크기, 최대 활성 코드 수 |
| CORS | `allowed-origins` |

> **보안 참고**: DB 자격증명, JWT 시크릿은 운영 시 환경 변수로 분리하는 것을 권장합니다.

## 라이선스

MIT
