# printscan 운영 Runbook (현장/지원)

> 대상: 설치·지원 담당(비개발자 포함). 증상 → 진단(그대로 복붙) → 조치 순.
> 명령의 `<PI_IP>`/`<HUB_IP>`/`<pw>` 는 현장 값으로 치환. 아래 명령은 실 유닛(`.25`)에서 동작 검증됨(2026-08-11).
> 참고: 설치 [`README-PI.md`](README-PI.md) · 허브 [`README-HUB.md`](README-HUB.md) · 명세 [`../SPEC.md`](../SPEC.md).

---

## 0. 30초 빠른 진단 (한 방)

```bash
# edge(Pi)에서
systemctl is-active printscan-edge          # active 여야 정상
curl -s -u admin:<pw> http://localhost:8091/actuator/health   # 각 component UP 확인
lpstat -p zebra                              # 'idle'=정상 대기 / 'disabled'=큐 정지
```
`/actuator/health` 컴포넌트 의미: **printer**(프린터 도달성)·**cloud**(허브 연결)·**db**·**diskSpace**. 하나라도 DOWN이면 전체 503.

허브 관점:
```bash
curl -s http://<HUB_IP>:8092/api/admin/devices   # 각 라인 유닛 online/printCount
```

---

## 1. 인쇄가 안 나온다

**진단**
```bash
lpstat -p zebra                 # 'disabled' 또는 미존재?
lpstat -o                       # 큐에 밀린 작업이 쌓였나
curl -s -u admin:<pw> http://localhost:8091/actuator/health | grep -o '"printer":{"status":"[A-Z]*"'
lsusb | grep -i zebra           # USB 물리 연결 확인
journalctl -u printscan-edge -n 50 --no-pager | grep -i "인쇄\|print\|error"
```
**조치**
- `printer` DOWN / `lsusb`에 Zebra 없음 → **USB 케이블·전원 재연결** 후 `sudo systemctl restart cups`.
- 큐 `disabled` → `cupsenable zebra` (또는 `sudo cupsenable zebra`).
- 큐 자체가 없음 → 재생성: `sudo lpadmin -p zebra -E -v "$(lpinfo -v | awk '/usb:.*[Zz]ebra/{print $2;exit}')" -m raw && sudo lpadmin -d zebra`.
- 밀린 작업 정리: `cancel -a zebra`.
- 스모크: `printf '^XA^FO40,40^A0N,40,40^FDHELLO^FS^XZ' | lp -d zebra -o raw`.

## 2. 한글이 안 나온다 / 깨진다

**진단**
```bash
fc-list | grep -i "CJK KR"      # Noto Sans CJK KR 설치 여부
grep FONT /opt/printscan-edge/edge.env
```
**조치**
- 폰트 없음 → `sudo apt install -y fonts-noto-cjk && sudo systemctl restart printscan-edge`.
- 이미지 재현성 필요 시 TTF 동봉 후 `PRINTSCAN_LABEL_FONT_PATH=/opt/printscan-edge/font.ttf`(패밀리명보다 우선) → 재시작.
- 미리보기 = 인쇄물이므로, 디자이너 미리보기에서 한글이 정상이면 인쇄도 정상.

## 3. 라벨이 잘린다 / 치우친다 / QR이 잘린다

**진단·조치**
```bash
# 자 없이 실제 인쇄 가능폭 측정 → 잘리는 지점이 실폭
curl -s -u admin:<pw> -X POST "http://localhost:8091/api/labels/ruler/print?widthMm=60&heightMm=25&dpi=203"
# 미디어 길이/갭 자동 재측정(치우침 교정)
curl -s -u admin:<pw> -X POST "http://localhost:8091/api/labels/calibrate"
```
- 측정한 실폭에 맞춰 **디자이너에서 라벨 폭/높이(mm) 조정**. QR은 우측 배치 + 콰이엇존 확보.
- 인쇄가 흐림/진함 → `edge.env`에 `PRINTSCAN_PRINTER_DARKNESS=0~30`, `PRINTSCAN_PRINTER_SPEED` 조정 후 재시작.

## 4. 허브에 유닛이 오프라인으로 뜬다

**진단**
```bash
# 유닛(Pi)에서
curl -s -u admin:<pw> http://localhost:8091/actuator/health | grep -o '"cloud":{"status":"[A-Z]*"'
grep CLOUD /opt/printscan-edge/edge.env      # BASE_URL·ORG_API_KEY 확인
curl -s -o /dev/null -w "%{http_code}\n" http://<HUB_IP>:8092/actuator/health   # 허브 도달?
```
**조치**
- `cloud` DOWN → 허브 주소 오타/네트워크. `PRINTSCAN_CLOUD_BASE_URL` 수정 → 재시작.
- 허브 미도달 → 방화벽/케이블. 통신은 **디바이스→허브 아웃바운드**뿐이므로 Pi에서 허브 8092가 열려야 함.
- 등록 실패(허브에 아예 없음) → `ORG_API_KEY`가 허브 조직 키와 일치하는지(§7 참고). 재프로비저닝: `sudo bash provision-edge.sh --line ... --hub ... --org-key ... --device ...`.
- 하트비트 주기 15s → 잠시 대기 후 재확인. 오프라인 감지 알림은 60s.

## 5. 저재고 경고가 왔다

**진단**
```bash
curl -s -u admin:<pw> "http://localhost:8091/api/products" | python3 -m json.tool | grep -A1 code   # 재고/최소재고
```
**조치**: 실물 보충 후 스캔 입고(`/scan` 화면) 또는 `POST /api/inventory/move {type:IN}`. 경고 기준 = 제품 `minQty`.

## 6. 원격(네트워크) 인쇄 잡이 안 나간다

**진단**
```bash
curl -s http://<HUB_IP>:8092/api/admin/jobs | python3 -m json.tool | grep -E 'status|id'
```
**조치** — 잡 상태머신: `QUEUED→SENT→DONE/FAILED`.
- `QUEUED` 그대로 → 대상 유닛 오프라인(§4). 유닛 폴링(2s)이 살아나면 자동 수령.
- `SENT`에서 정체 → 유닛이 수령 후 다운. 허브 **리퍼(30s)** 가 60s+ 정체 잡을 자동 `QUEUED` 재큐 → 다른/복구된 유닛이 인쇄. (재인쇄는 멱등키로 중복 방지)
- `FAILED` → 유닛 프린터 문제(§1). 원인 해결 후 재지시.

## 7. org-key 유출 / 교체 (무중단)

```bash
# 현재 키·상태
curl -s http://<HUB_IP>:8092/api/admin/org/key
# 교체(직전 키 60분 유예 — 그동안 기존 유닛 계속 동작, 순차 재프로비저닝)
curl -s -X POST -H "Content-Type: application/json" -d '{"graceMinutes":60}' \
  http://<HUB_IP>:8092/api/admin/org/rotate-key
# 유출 즉시 대응(유예 없이 직전 키 폐기)
curl -s -X POST http://<HUB_IP>:8092/api/admin/org/revoke-previous-key
```
> SaaS(admin-token 설정 시)는 위 요청에 `-H "X-Admin-Token: <토큰>"` 추가. 교체 후 각 유닛 `provision-edge.sh --org-key <새키>` 로 순차 갱신(유예 안에).

## 8. 백업 / 복원

- 자동: edge 매일 **03:00** → `/opt/printscan-edge/data/backup/printscan-<날짜>.zip` (H2). *실동작 확인됨*.
```bash
ls -la /opt/printscan-edge/data/backup/          # 백업 목록
# 복원(서비스 중지→기존 data 보존→unzip→재시작)
sudo bash /tmp/deploy/restore.sh <backup.zip> printscan-edge /opt/printscan-edge/data
```
- 백업 실패 알림이 오면 디스크 여유(`df -h`)·권한 확인.

## 9. 앱 업데이트 / 롤백

```bash
# 업데이트: 새 jar 교체 → 재시작 → 즉시 재검증
sudo install -o printscan -g printscan /tmp/app.jar /opt/printscan-edge/app.jar
sudo systemctl restart printscan-edge
EDGE_URL=http://localhost:8091 EDGE_USER=admin EDGE_PASS=<pw> bash /tmp/deploy/acceptance-test.sh
# 롤백: 교체 전 jar 보관본 or 데이터 백업 복원(§8)
```
> 운영 팁: 교체 전 `cp /opt/printscan-edge/app.jar /opt/printscan-edge/app.jar.prev` 로 즉시 롤백 대비.

## 10. 서비스 제어 / 로그

```bash
sudo systemctl status  printscan-edge         # 상태
sudo systemctl restart printscan-edge         # 재시작
journalctl -u printscan-edge -f               # 실시간 로그
journalctl -u printscan-edge -n 100 --no-pager
```
- 크래시 시 systemd `Restart=always`(5s)로 자동 재기동. 부팅 자동 상주(`enabled`).

---

## 부록: 알림(webhook) 의미

`PRINTSCAN_ALERT_WEBHOOK` 설정 시 아래를 ntfy/Slack 호환 JSON으로 발송(미설정=로그만, 5분 dedup):

| 알림 | 의미 | 조치 |
|---|---|---|
| `lowstock:<code>` | 제품 재고 ≤ minQty | §5 보충 |
| `printer` 미발견 | 프린터 도달 실패 | §1 |
| `backup` 실패 | 백업 쓰기 실패 | §8 디스크/권한 |
| `offline:<id>` | 디바이스 60s+ 미접속 | §4 |
