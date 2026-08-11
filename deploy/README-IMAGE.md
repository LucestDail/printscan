# 골든 SD 이미지 제작 — Pi 유닛 대량 복제

> 유닛 여러 대를 깔 때, Pi 1대를 "골든 이미지"로 만들어 SD 복제 → 유닛별 프로비저닝만 1회.
> 배경/전략은 [`../PILOT.md`](../PILOT.md) §3. 파일럿(수 대)엔 이 방식으로 충분.

## 핵심 원칙
1. **유닛 고유값(org-key·라인·device 이름·장비 식별)은 굽지 않는다.** → 클론 후 `provision-edge.sh`로 주입.
2. **`DeviceIdentity`(허브 등록 결과)를 이미지에 남기지 않는다.** edge는 로컬 H2 `device_identity`(단일 행)에 deviceId/token을 저장하는데, 이게 구워지면 **모든 클론이 같은 장비로 등록**된다 → 반드시 데이터 초기화.

---

## A. 골든 이미지 만들기 (Pi 1대)

```bash
# 1) Raspberry Pi OS Lite (64-bit) 설치 후 부팅. SSH 활성.

# 2) edge 설치 (앱/CUPS/한글폰트/raw큐/systemd). USB Zebra 연결 상태 권장.
scp edge/target/printscan-edge-0.0.1-SNAPSHOT.jar pi@<PI>:/tmp/app.jar
scp -r deploy pi@<PI>:/tmp/deploy
sudo bash /tmp/deploy/install-edge.sh /tmp/app.jar USB

# 3) 앱 기동 확인 (허브 미설정이라 cloud=DOWN→health 503 정상. printer/db UP이면 OK)
systemctl is-active printscan-edge
curl -s -u admin:printscan http://localhost:8091/actuator/health | grep -o '"printer":{"status":"[A-Z]*"'
```

## B. 이미지 일반화 (굽기 직전 — 필수)

```bash
sudo systemctl stop printscan-edge

# (1) 유닛 고유 데이터 제거 — H2 DB(등록·재고·템플릿) 초기화 → 클론마다 새 장비로 등록
sudo rm -f /opt/printscan-edge/data/printscan.mv.db /opt/printscan-edge/data/printscan.lock.db /opt/printscan-edge/data/*.trace.db
sudo rm -rf /opt/printscan-edge/data/backup/*

# (2) 유닛 고유 설정 제거 — 프로비저닝 전까지 허브 미연동 상태로
sudo rm -f /opt/printscan-edge/edge.env

# (3) 로그/식별자 정리(복제 후 충돌·오염 방지)
sudo truncate -s 0 /opt/printscan-edge/app.log 2>/dev/null || true
sudo rm -f /etc/ssh/ssh_host_*           # 클론마다 새 SSH 호스트키 생성되게
sudo truncate -s 0 /etc/machine-id       # 부팅 시 재생성(고유 machine-id)
# (선택) CUPS 큐는 남겨도 됨(USB URI 시리얼이 프린터별로 달라 첫 인쇄 전 재검출 권장)

sudo shutdown -h now
```

## C. 이미지 추출 & 복제

```bash
# 개발 PC에서 SD 리더로 이미지 추출 (macOS 예: diskutil list 로 디스크 확인)
sudo dd if=/dev/diskN of=printscan-golden.img bs=4m status=progress
# 축소/압축은 pishrink 등 별도 도구 권장.

# 복제: 새 SD마다
sudo dd if=printscan-golden.img of=/dev/diskM bs=4m status=progress
# 또는 Raspberry Pi Imager 로 커스텀 이미지(printscan-golden.img) 굽기.
```

## D. 클론 유닛 첫 부팅 (유닛별 1회)

```bash
# 프로비저닝: 이 유닛의 라인·허브·org-key
sudo bash /opt/deploy/provision-edge.sh \
  --line "N라인" --hub "http://<HUB_IP>:8092" \
  --org-key "<조직키>" --device "edge-N라인"
#   (deploy 폴더가 없으면 scp -r deploy 후 경로 맞춰 실행)

# 프린터 재검출(클론이라 CUPS 큐 URI 시리얼이 다를 수 있음)
URI=$(lpinfo -v | awk '/usb:.*[Zz]ebra/{print $2;exit}')
[ -n "$URI" ] && sudo lpadmin -p zebra -E -v "$URI" -m raw && sudo lpadmin -d zebra

# 수용검사(통과=인계)
EDGE_URL=http://localhost:8091 EDGE_USER=admin EDGE_PASS=<pw> \
HUB_URL=http://<HUB_IP>:8092 DEVICE_NAME=edge-N라인 \
bash /opt/deploy/acceptance-test.sh --print
```

각 클론은 **빈 H2로 시작 → 프로비저닝 시 허브에 새 장비로 등록**되므로 장비 식별 충돌이 없다.

---

## 체크리스트 (굽기 전)
- [ ] `edge.env` 제거됨(유닛 고유값 미포함)
- [ ] H2 `data/*.mv.db` 제거됨(장비 식별 초기화)
- [ ] SSH 호스트키·machine-id 초기화됨
- [ ] systemd `printscan-edge` **enabled**(부팅 자동 상주)
- [ ] `install-edge.sh`가 심은 java/cups/폰트/raw큐 존재

> 리세일 대량 배포(무인 프로비저닝·OTA)는 [`../PILOT.md`](../PILOT.md) §3-B(cloud-init/first-boot) — 파일럿 이후 트랙.
