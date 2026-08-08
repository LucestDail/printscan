# 라즈베리파이 유닛 설치 (edge)

라인당 유닛 = **라즈베리파이(3D프린팅 케이스) + Zebra 프린터(USB)**. Pi 리눅스가 edge 앱을 상주 구동.

## 하드웨어
- Zebra(예: ZD421) **USB-B** → 라즈베리파이 **USB-A(host)** 케이블.
- Pi 를 공장 유선 네트워크에 연결.

## 설치 (자동)
```bash
# 1) edge fat jar 를 Pi 로 복사 (개발 PC에서)
#    ./mvnw -f edge/pom.xml clean package -DskipTests  → edge/target/printscan-edge-*.jar
scp edge/target/printscan-edge-0.0.1-SNAPSHOT.jar pi@<PI_IP>:/tmp/app.jar
scp -r deploy pi@<PI_IP>:/tmp/deploy

# 2) Pi 에서 설치 스크립트 (java17+cups+한글폰트+raw큐+systemd 자동)
sudo bash /tmp/deploy/install-edge.sh /tmp/app.jar USB

# 3) 라인명/허브주소 설정
sudo nano /opt/printscan-edge/edge.env      # PRINTSCAN_LINE_NAME, PRINTSCAN_CLOUD_BASE_URL 등
sudo systemctl restart printscan-edge
```

## 확인
- 로컬 UI: `http://<PI_IP>:8091/` (디자이너/스캔)
- 서비스: `systemctl status printscan-edge`, 로그 `journalctl -u printscan-edge -f`
- 프린터 스모크: `printf '^XA^FO40,40^A0N,40,40^FDHELLO^FS^XZ' | lp -d zebra -o raw`
- 부팅 자동 상주 + 크래시 자동재시작(systemd Restart=always).

## 백업 / 복원 / 관측성 / 알림
- **백업**: `BackupService` 가 매일 03:00 로컬 H2 를 `data/backup/printscan-<날짜>.zip` 으로 자동 백업(H2만; Postgres 는 pg_dump 별도).
- **복원**: `sudo bash deploy/restore.sh <backup.zip> printscan-edge /opt/printscan-edge/data` — 서비스 중지→기존 data 보존→unzip→재시작(롤백 안내 포함).
- **헬스**: `GET /actuator/health` 에 `printer`(mode별 실도달성)·`cloud`(허브 연결) 구성요소 포함. `GET /actuator/prometheus` 메트릭.
- **알림**: `PRINTSCAN_ALERT_WEBHOOK` 설정 시 저재고·프린터 미발견·백업 실패를 webhook(ntfy/Slack 호환 JSON)으로 발송(+WARN 로그). 미설정이면 로그만.

## 폰트(한글) / 프린터 상태 / 눈금자
- **폰트**: `install-edge.sh` 가 `fonts-noto-cjk` 설치. 호스트 독립(이미지 재현성) 원하면 TTF 를 넣고 `PRINTSCAN_LABEL_FONT_PATH=/opt/printscan-edge/font.ttf` 지정(패밀리명보다 우선).
- **프린터 상태**(용지없음/헤드열림): `GET /api/printer/status`. **network(9100) 모드만 지원**(USB/CUPS는 단방향 → supported=false).
- **라벨 실측(자 없이)**: `POST /api/labels/ruler/print?widthMm=60` 로 mm 눈금자 인쇄 → 잘리는 지점이 실제 인쇄 가능폭. 미리보기 `GET /api/labels/ruler?widthMm=60`.

## 트러블슈팅
- 한글 안 나옴 → `fc-list | grep -i "CJK KR"` 확인, 없으면 `sudo apt install fonts-noto-cjk`.
- 인쇄 안 됨 → 유저 `printscan` 이 `lp` 그룹인지(`id printscan`), CUPS 큐 `lpstat -p zebra`.
- QR/치수 잘림 → 라벨 실측 후 디자이너에서 폭/높이(mm) 조정(미리보기=인쇄물).
