#!/usr/bin/env bash
# printscan edge 유닛 1대 프로비저닝 — edge.env 생성/갱신 후 서비스 재시작.
# install-edge.sh(앱/큐/systemd 설치) 이후, 유닛별 고유 설정(라인·허브·org-key)을 반복 적용하는 단계.
# 여러 라인을 깔 때 라인별로 값만 바꿔 반복 실행하면 됨.
#
# 사용법(루트):
#   sudo bash provision-edge.sh \
#     --line "1라인" \
#     --hub  "http://hub.factory.lan:8092" \
#     --org-key "ORG-XXXX" \
#     --device "edge-1라인" \
#     [--mode cups|network|rawdev] [--printer-host 192.168.0.50] [--alert-webhook https://ntfy...] \
#     [--font-path /opt/printscan-edge/font.ttf]
set -euo pipefail

APP_DIR=/opt/printscan-edge
ENV="$APP_DIR/edge.env"
MODE="cups"; HOST=""; WEBHOOK=""; FONT=""
LINE=""; HUB=""; ORGKEY=""; DEVICE=""

while [ $# -gt 0 ]; do
  case "$1" in
    --line) LINE="$2"; shift 2;;
    --hub) HUB="$2"; shift 2;;
    --org-key) ORGKEY="$2"; shift 2;;
    --device) DEVICE="$2"; shift 2;;
    --mode) MODE="$2"; shift 2;;
    --printer-host) HOST="$2"; shift 2;;
    --alert-webhook) WEBHOOK="$2"; shift 2;;
    --font-path) FONT="$2"; shift 2;;
    *) echo "알 수 없는 옵션: $1"; exit 2;;
  esac
done

[ -d "$APP_DIR" ] || { echo "❌ $APP_DIR 없음 — 먼저 install-edge.sh 실행"; exit 1; }
[ -n "$LINE" ] && [ -n "$HUB" ] && [ -n "$ORGKEY" ] || { echo "❌ --line, --hub, --org-key 는 필수"; exit 2; }
[ -n "$DEVICE" ] || DEVICE="edge-$LINE"

# 기존 env 백업(재프로비저닝 롤백용)
[ -f "$ENV" ] && cp "$ENV" "$ENV.bak.$(date +%Y%m%d%H%M%S)"

umask 077   # org-key 포함 → 소유자만 읽기
cat > "$ENV" <<EOF
# provision-edge.sh 생성 — 유닛 고유 설정
PRINTSCAN_LINE_NAME=$LINE
PRINTSCAN_PRINTER_MODE=$MODE
PRINTSCAN_PRINTER_NAME=zebra,zd421
$([ -n "$HOST" ] && echo "PRINTSCAN_PRINTER_HOST=$HOST")
PRINTSCAN_LABEL_FONT_FAMILY=Noto Sans CJK KR
$([ -n "$FONT" ] && echo "PRINTSCAN_LABEL_FONT_PATH=$FONT")
PRINTSCAN_CLOUD_ENABLED=true
PRINTSCAN_CLOUD_BASE_URL=$HUB
PRINTSCAN_CLOUD_ORG_API_KEY=$ORGKEY
PRINTSCAN_CLOUD_DEVICE_NAME=$DEVICE
$([ -n "$WEBHOOK" ] && echo "PRINTSCAN_ALERT_WEBHOOK=$WEBHOOK")
EOF
chown printscan:printscan "$ENV" 2>/dev/null || true

echo "✅ $ENV 작성:"
sed 's/\(ORG_API_KEY=\).*/\1***마스킹***/' "$ENV" | sed 's/^/   /'

systemctl restart printscan-edge
echo "✅ printscan-edge 재시작. 확인: systemctl status printscan-edge"
echo "   수용검사: EDGE_URL=http://localhost:8091 HUB_URL=$HUB DEVICE_NAME=$DEVICE bash acceptance-test.sh"
