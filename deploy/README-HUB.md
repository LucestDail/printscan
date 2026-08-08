# 허브 설치 (통합 관리) — 온프렘 MAIN / AWS SaaS

동일한 `cloud/` 모듈. 배포 위치와 보안만 다름.

## A. 온프렘 MAIN (공장장 PC / 사내 서버, LAN 폐쇄망)
인터넷 불필요. 보안 경량(디바이스 토큰만).
```bash
./mvnw -f cloud/pom.xml clean package -DskipTests   # cloud/target/printscan-cloud-*.jar
# 서버로 복사 후:
sudo install -d /opt/printscan-cloud && sudo cp printscan-cloud-*.jar /opt/printscan-cloud/app.jar
sudo cp deploy/printscan-cloud.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now printscan-cloud
# 대시보드: http://<HUB_IP>:8092/
```
- 각 Pi 의 `edge.env` 에서 `PRINTSCAN_CLOUD_BASE_URL=http://<HUB_IP>:8092`.
- admin-token 미설정 → 대시보드/API 개방(사내망 신뢰).

## B. AWS SaaS (인터넷, 멀티공장)
보안 강화: **TLS + admin-token**.
```bash
# EC2(예: t3.small, Ubuntu) 에 java17 + jar + systemd (위와 동일)
# hub.env 에 admin-token 설정 → /api/admin/** 보호 활성
echo 'PRINTSCAN_HUB_MODE=saas'                >> /opt/printscan-cloud/hub.env
echo 'PRINTSCAN_HUB_ADMIN_TOKEN=<강력한난수>' >> /opt/printscan-cloud/hub.env
sudo systemctl restart printscan-cloud
```
- **TLS**: 앱 자체가 아니라 앞단 리버스 프록시로. 예) Caddy 한 줄:
  ```
  hub.example.com {
      reverse_proxy 127.0.0.1:8092
  }
  ```
  (Caddy 가 Let's Encrypt 인증서 자동 발급/갱신)
- 디바이스는 `PRINTSCAN_CLOUD_BASE_URL=https://hub.example.com` + 조직별 `ORG_API_KEY`(테넌트 격리).
- 관리자 대시보드는 `X-Admin-Token` 필요(향후 로그인 UI로 대체 예정).

## DB: 온프렘 H2 → SaaS Postgres
- **온프렘(기본)**: H2 파일(`application.properties`). 무설정.
- **SaaS**: `--spring.profiles.active=prod` → `application-prod.properties`(Postgres, env `DB_URL`/`DB_USER`/`DB_PASSWORD`). postgresql 드라이버 포함.
  ```bash
  DB_URL=jdbc:postgresql://db.internal:5432/printscan DB_USER=printscan DB_PASSWORD=*** \
  PRINTSCAN_HUB_ADMIN_TOKEN=*** java -jar app.jar --server.port=8092 --spring.profiles.active=prod
  ```
- 현재 prod 는 `ddl-auto=update`(부트스트랩 용이). **스키마 안정 후 Flyway + `validate` 전환 권장**(마이그레이션 스크립트는 스키마 확정 시 생성 — 무검증 DDL 선반영 회피).

## 통신/보안 요약
- 디바이스 → 허브: **아웃바운드 HTTP 폴링**(방화벽/NAT 친화). 인쇄지시는 허브 큐 → 폴링 수령.
- 온프렘 LAN: TLS/공인인증 불필요(폐쇄망). SaaS: TLS + admin-token + 테넌트 격리.
