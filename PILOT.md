# printscan 파일럿 준비 — 실기계(Pi+Zebra) 리세일 로드맵

> 작성 2026-08-10. 근거: 실제 코드/배포 자산(`deploy/`)과 `.25` 라이브 검증. 지어낸 항목 없음.
> 목표: **공장 1곳 × 소수 라인**에 라즈베리파이+Zebra 유닛을 실제로 설치·인계하는 파일럿까지의 실행 계획.

이 문서는 "무엇이 이미 준비됐고(코드/스크립트), 무엇이 실기계·구매·멀티월이 필요한가"를 정직하게 구분한다.

---

## 0. 지금 준비된 것 (코드레벨 — 검증됨)

| 자산 | 위치 | 상태 |
|---|---|---|
| edge 앱(래스터^GFA·로컬출력·허브동기화) | `edge/` | ✅ edge 28/28 테스트, `.25` 라이브 |
| cloud 허브(멀티테넌트·잡큐·집계·org-key 로테이션) | `cloud/` | ✅ cloud 23/23, Flyway+Postgres 검증 |
| Pi 자동설치(java17·cups·한글폰트·raw큐·systemd) | `deploy/install-edge.sh` | ✅ 문법 검증 |
| 유닛별 프로비저닝(라인·허브·org-key→edge.env+재시작) | `deploy/provision-edge.sh` | ✅ 신규(오늘) |
| **필드 수용검사(비개발자용 PASS/FAIL)** | `deploy/acceptance-test.sh` | ✅ 신규·**`.25` 실 edge+hub 6/6 PASS** |
| systemd 유닛(부팅상주·크래시 재시작) | `deploy/printscan-edge.service` | ✅ |
| 백업/복원 | `BackupService`(매일 03:00) · `deploy/restore.sh` | ✅ |
| 관측성/알림 | `/actuator/health`(printer·cloud 컴포넌트)·prometheus·webhook 알림 | ✅ `.25` 검증 |
| 설치 문서 | `deploy/README-PI.md` · `deploy/README-HUB.md` | ✅ |

⇒ **소프트웨어와 설치 스크립트는 파일럿 가능 수준.** 남은 건 대부분 하드웨어·현장·재현성.

---

## 1. 파일럿 토폴로지 결정 (권장)

**첫 파일럿 = 온프렘 MAIN(공장장 PC) + 유선 LAN.** 이유:
- TLS/도메인/결제/계정 = **불필요**(폐쇄망, 디바이스 토큰 경량 인증). SaaS 트랙(TLS·admin-token·Postgres)은 파일럿 성공 후로.
- DB = **H2 파일**(무설정). 규모 커지면 Postgres+Flyway(이미 준비됨)로 전환.
- 리스크 최소·셋업 최속. `README-HUB.md` A안 그대로.

> SaaS는 멀티공장 계약이 생길 때. 그 전엔 온프렘으로 "동작·가치"를 먼저 증명.

---

## 2. 하드웨어 BOM (라인 1대 기준)

- [ ] 라즈베리파이 (4/5, **ARM64**, 2GB+ RAM) + 정품 전원
- [ ] microSD 32GB+ (Class10/A1) — 골든 이미지용
- [ ] Zebra 프린터 (ZD421 203dpi 검증됨) + 라벨 미디어(실측 필요)
- [ ] USB 케이블 (Zebra USB-B → Pi USB-A)
- [ ] 3D 프린팅 케이스(Pi+배선 고정) — 도면 별도
- [ ] 유선 LAN 케이블(공장망) — Wi-Fi보다 유선 권장(안정성)
- [ ] 허브: 공장장 PC 또는 사내 서버 1대(java17)

---

## 3. Pi 이미지화 (재현성 — 파일럿의 핵심 공백)

현재는 유닛마다 `scp + install-edge.sh` 수동. 파일럿(수 대)까진 이대로도 되나, **리세일 규모엔 골든 이미지가 필수.** 두 경로:

> 실행 절차서: [`deploy/README-IMAGE.md`](deploy/README-IMAGE.md)(굽기 전 초기화 체크리스트 포함). 현장 장애 대응: [`deploy/RUNBOOK.md`](deploy/RUNBOOK.md).

### 3-A. 골든 이미지 (권장, 파일럿에 적합)
1. Pi 1대에 Raspberry Pi OS Lite(64-bit) 설치.
2. `install-edge.sh <app.jar> USB` 실행 → java/cups/폰트/raw큐/systemd 구성.
3. **유닛 고유값은 굽지 않는다**(org-key·라인명 제외) → edge.env는 프로비저닝 단계에서 주입.
4. `sudo systemctl disable printscan-edge` 대신, **first-boot 프로비저닝 대기 상태**로 두거나 edge.env 없이 기동(앱은 설정 없어도 기동됨 — systemd `EnvironmentFile=-` 로 옵셔널).
5. `dd` 또는 `rpi-imager` 로 SD 이미지 추출 → 나머지 Pi에 복제.
6. 복제 후 유닛별로 `provision-edge.sh --line ... --org-key ...` 1회.

> 장점: 빠름, 파일럿 즉시 가능. 단점: OS 업데이트 시 재이미징. **파일럿엔 충분.**

### 3-B. 자동 프로비저닝 이미지 (리세일 확장 시)
- `cloud-init` 또는 first-boot 스크립트가 부팅 시 (a) 고유 org-key/라인을 USB 설정파일 또는 QR에서 읽어 edge.env 생성, (b) 허브 등록.
- OTA(원격 jar 갱신)와 결합 → 무인 대량 배포. **멀티월 트랙, 파일럿 이후.**

---

## 4. 유닛 설치 절차 (라인 1대, 반복 가능)

```bash
# (개발 PC) edge fat jar 빌드
./mvnw -f edge/pom.xml clean package -DskipTests

# (개발 PC → Pi) 앱 + deploy 복사
scp edge/target/printscan-edge-0.0.1-SNAPSHOT.jar pi@<PI_IP>:/tmp/app.jar
scp -r deploy pi@<PI_IP>:/tmp/deploy

# (Pi) 1) 설치 — java/cups/폰트/raw큐/systemd
sudo bash /tmp/deploy/install-edge.sh /tmp/app.jar USB

# (Pi) 2) 프로비저닝 — 이 유닛의 라인·허브·org-key
sudo bash /tmp/deploy/provision-edge.sh \
  --line "1라인" --hub "http://<HUB_IP>:8092" \
  --org-key "<조직키>" --device "edge-1라인"

# (Pi) 3) 수용검사 — 통과해야 인계
EDGE_URL=http://localhost:8091 EDGE_USER=admin EDGE_PASS=<pw> \
HUB_URL=http://<HUB_IP>:8092 DEVICE_NAME=edge-1라인 \
bash /tmp/deploy/acceptance-test.sh --print   # --print: 실물 라벨 1장까지
```

**수용검사 통과 기준**(FAIL=0): edge 헬스 UP · printer 컴포넌트 UP · 한글 렌더 200 · cloud 연동 UP · 허브가 유닛 온라인 인지 · (옵션)실물 인쇄 200.

---

## 5. 물리 캘리브레이션 체크리스트 (현장, 자 없이)

1. **미디어 실측**: `POST /api/labels/ruler/print?widthMm=60` → 잘리는 지점 = 실제 인쇄 가능폭. 디자이너에서 라벨 폭/높이(mm) 확정.
2. **자동 캘리브**: `POST /api/labels/calibrate`(~JC) — 라벨 길이/갭 재측정(치우침 교정).
3. **농도/속도**: 스캔 품질 낮으면 `PRINTSCAN_PRINTER_DARKNESS`(0~30)·`_SPEED` 조정.
4. **QR 잘림**: 미리보기=인쇄물이므로 디자이너에서 QR을 우측 배치·콰이엇존 확보(기존 반영됨).

---

## 6. 업데이트 / 롤백 (파일럿 중 — 수동, OTA 이전)

```bash
# 업데이트: 새 jar 교체 후 재시작
scp edge/target/printscan-edge-*.jar pi@<PI>:/tmp/app.jar
sudo install -o printscan -g printscan /tmp/app.jar /opt/printscan-edge/app.jar
sudo systemctl restart printscan-edge && bash acceptance-test.sh   # 즉시 재검증

# 롤백: 데이터 백업본에서 복원
sudo bash deploy/restore.sh <backup.zip> printscan-edge /opt/printscan-edge/data
```
> jar 이전본을 `app.jar.prev` 로 보관하면 즉시 롤백 가능(운영 팁 — README-PI 에 추가 권장).

---

## 7. 파일럿 중 모니터링/지원

- **허브 대시보드**(`http://<HUB>:8092/`): 라인별 장비 온라인·인쇄수·소비 집계 실시간.
- **오프라인 감지**: 허브 `offlineCheck`(60초) — 유닛 60초+ 미접속 시 경고.
- **알림 webhook**: `provision-edge.sh --alert-webhook`로 저재고·프린터 미발견·백업 실패를 ntfy/Slack 수신.
- **원격 로그**: `journalctl -u printscan-edge -f`.

---

## 8. 파일럿 Punch-list (우선순위 · 검증법)

| # | 항목 | 상태 | 검증/필요조건 |
|---|---|---|---|
| P1 | edge/cloud SW | ✅ 완료 | 테스트 GREEN + `.25` 라이브 |
| P2 | 설치/프로비저닝/수용검사 스크립트 | ✅ 완료(오늘) | `.25` 6/6 PASS, 문법 검증 |
| P3 | **실 Pi(ARM64)에서 install-edge 실행** | ⏳ 하드웨어 | Pi 확보 후 1회 검증 필요(현재 `.25`=x86) |
| P4 | **실 Pi+Zebra 인쇄 스모크** | ⏳ 하드웨어 | `acceptance-test.sh --print` 로 실물 확인 |
| P5 | 라벨 미디어 실측·캘리브 | ⏳ 현장 | §5 체크리스트 |
| P6 | 골든 SD 이미지 1개 제작 | ⏳ 하드웨어 | §3-A, Pi 1대로 30분 |
| P7 | 3D 케이스 도면·출력 | ⏳ 기구 | 별도 |
| P8 | 공장장 PC 허브 설치 | ⏳ 현장 | README-HUB A안 |
| P9 | 파일럿 계약/설치 일정 | ⏳ 영업 | — |

**멀티월(파일럿 이후, 이 문서 범위 밖)**: SaaS(TLS·결제·계정), OTA 대량배포, org-key 만료정책 자동화, 실기계 부하/장기 회귀.

---

## 8-1. 부록 — `.25`를 Pi로 가정한 전체 드라이런 (2026-08-10)

실 Pi 입고 지연(~3주)으로, `.25`(x86 Ubuntu 24.04 + 실 Zebra ZD421 USB)를 Pi로 간주해 **§4 절차 전체를 정식 systemd 경로로 완주**. 목적=실 Pi 오기 전에 자동화의 깨지는 지점 선제거.

| 단계 | 명령 | 결과 |
|---|---|---|
| 설치 | `install-edge.sh /tmp/app.jar USB` | ✅ printscan 유저·`/opt/printscan-edge`·raw큐 'zebra'(실 URI)·systemd enable+start. 패키지 idempotent |
| 프로비저닝 | `provision-edge.sh --line 파일럿-1라인 --hub localhost:8092 --org-key … --device edge-pilot-01` | ✅ edge.env 작성(org-key 마스킹 출력)·재시작 |
| 수용검사 | `acceptance-test.sh --print` | ✅ **7/7 PASS** (헬스·printer·한글렌더·눈금자·cloud·허브온라인·**실물 인쇄 200**) |
| 크래시 복구 | `kill -9 <MainPID>` | ✅ systemd `Restart=always` → **6초 내 새 PID active**·헬스 200 재확보 |
| 부팅 상주 | `systemctl is-enabled` | ✅ `enabled` |
| 허브 인지 | `GET /api/admin/devices` | ✅ `edge-pilot-01` online:true |

**검증된 것**: 설치·프로비저닝·수용검사·크래시 자동복구·부팅 상주·허브 등록·**실 Zebra 한글+QR 실물 인쇄** 전 경로.
**여전히 미검증(실 Pi 필요)**: ① ARM64 아키(패키지 arch만 다름, 이름 동일 → 로직 리스크 낮음) ② **물리 전원 재투입(reboot)** — 공유 홈랩이라 프로세스 크래시로 대체 검증(전체 리부팅은 미실시). 실 Pi에서 `sudo reboot` 후 자동 상주만 확인하면 됨.
> ⇒ 드라이런으로 P3(설치 실행)·P4(실물 인쇄)를 **x86 기준으로 사실상 통과**. 실 Pi에선 이 절차 그대로 + 리부팅 1회 확인이면 인계 가능.

**드라이런 후 `.25` 상태**: edge = **systemd `/opt/printscan-edge`(8091)** 가 정식(기존 수동 `~/printscan-edge` nohup은 중지·방치). 허브 cloud(8092)는 기존 nohup 유지.

## 9. 한 줄 결론

> **소프트웨어·설치 자동화·수용검사는 파일럿 준비 완료**, 그리고 **`.25`(x86+실 Zebra)에서 §4 전체 드라이런 통과**(설치·프로비저닝·수용검사 7/7·실물 인쇄·크래시 자동복구·부팅 상주 — §8-1).
> 남은 실검증은 **실 Pi(ARM64) 입고(~3주) 후 동일 절차 재실행 + 리부팅 1회**뿐. 자동화가 실 리눅스에서 이미 돌았으므로 실 Pi 인계는 반나절 이내 예상.
