#!/usr/bin/env bash
# printscan edge 라즈베리파이 설치 스크립트 (Debian/Raspberry Pi OS, ARM64).
# 사용법: sudo bash install-edge.sh <app.jar 경로> [USB|LAN]
#   USB(기본): CUPS raw 큐 자동 생성(usb://Zebra...). LAN: 9100 모드(주소는 edge.env 에서).
set -euo pipefail

JAR="${1:-app.jar}"
CONN="${2:-USB}"
APP_DIR=/opt/printscan-edge
SVC=/etc/systemd/system/printscan-edge.service

echo "== 1) 패키지(java17 / cups / 한글폰트) =="
apt-get update -y
apt-get install -y openjdk-17-jre-headless cups fonts-noto-cjk netcat-openbsd

echo "== 2) 서비스 유저/디렉토리 =="
id printscan >/dev/null 2>&1 || useradd -r -s /usr/sbin/nologin -G lp,lpadmin printscan
install -d -o printscan -g printscan "$APP_DIR"
install -o printscan -g printscan "$JAR" "$APP_DIR/app.jar"
[ -f "$APP_DIR/edge.env" ] || install -o printscan -g printscan "$(dirname "$0")/edge.env.example" "$APP_DIR/edge.env"

echo "== 3) 프린터 (USB → CUPS raw 큐) =="
if [ "$CONN" = "USB" ]; then
  systemctl enable --now cups
  URI="$(lpinfo -v 2>/dev/null | awk '/usb:.*[Zz]ebra/{print $2; exit}')"
  if [ -n "${URI:-}" ]; then
    lpadmin -p zebra -E -v "$URI" -m raw && lpadmin -d zebra
    echo "  raw 큐 'zebra' 생성: $URI"
  else
    echo "  ⚠ Zebra USB 미검출. 프린터 연결 후: lpinfo -v 로 URI 확인 → lpadmin -p zebra -E -v <URI> -m raw"
  fi
fi

echo "== 4) systemd 서비스 =="
install "$(dirname "$0")/printscan-edge.service" "$SVC"
systemctl daemon-reload
systemctl enable --now printscan-edge
echo "== 완료. 상태: systemctl status printscan-edge / 로그: journalctl -u printscan-edge -f =="
echo "== 설정: $APP_DIR/edge.env (라인명·허브주소) 수정 후 systemctl restart printscan-edge =="
