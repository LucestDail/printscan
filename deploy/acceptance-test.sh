#!/usr/bin/env bash
# printscan edge 유닛 필드 수용검사(acceptance test).
# 비개발자 설치자가 "이 유닛이 제대로 동작하는가"를 한 번에 확인하는 스크립트.
# jq 불필요(HTTP 코드/문자열 매칭만 사용). Pi/설치 PC 어디서든 edge 에 도달하면 실행 가능.
#
# 사용법:
#   EDGE_URL=http://localhost:8091 EDGE_USER=admin EDGE_PASS=printscan \
#   HUB_URL=http://hub.factory.lan:8092 DEVICE_NAME=edge-1라인 \
#   bash acceptance-test.sh [--print]
#
#   --print : 실제 라벨 1장 출력 스모크까지 수행(라벨 1장 소모). 생략 시 렌더까지만.
set -uo pipefail

EDGE_URL="${EDGE_URL:-http://localhost:8091}"
EDGE_USER="${EDGE_USER:-admin}"
EDGE_PASS="${EDGE_PASS:-printscan}"
HUB_URL="${HUB_URL:-}"
DEVICE_NAME="${DEVICE_NAME:-}"
DO_PRINT="no"
[ "${1:-}" = "--print" ] && DO_PRINT="yes"

AUTH=(-u "${EDGE_USER}:${EDGE_PASS}")
PASS=0; FAIL=0; WARN=0
ok()   { echo "  ✅ $1"; PASS=$((PASS+1)); }
bad()  { echo "  ❌ $1"; FAIL=$((FAIL+1)); }
warn() { echo "  ⚠️  $1"; WARN=$((WARN+1)); }

echo "== printscan edge 수용검사 =="
echo "   edge=$EDGE_URL  hub=${HUB_URL:-(생략)}  print=$DO_PRINT"
echo

# 1) edge 헬스(200=UP, 503=DOWN). actuator 가 상태를 HTTP 코드로 반영.
echo "[1] edge 헬스"
CODE=$(curl -s -o /tmp/ps_health.json -w "%{http_code}" "${AUTH[@]}" "$EDGE_URL/actuator/health")
if [ "$CODE" = "200" ]; then ok "actuator/health = UP (200)"
else bad "actuator/health HTTP=$CODE (503=구성요소 DOWN, 000=미도달)"; fi
# 프린터 구성요소 개별 확인(DOWN이면 인쇄 불가 신호)
if grep -q '"printer":{"status":"UP"' /tmp/ps_health.json 2>/dev/null; then ok "printer 구성요소 UP"
elif grep -q '"printer":{"status":"DOWN"' /tmp/ps_health.json 2>/dev/null; then bad "printer 구성요소 DOWN — 프린터 연결/큐 확인"
else warn "printer 구성요소 상태 미확인"; fi

# 2) 래스터 렌더(미리보기=인쇄물). 200 + image/png 면 렌더엔진 정상.
echo "[2] 라벨 렌더(래스터)"
CT=$(curl -s -o /dev/null -w "%{http_code} %{content_type}" "${AUTH[@]}" -H "Content-Type: application/json" \
  -d '{"widthMm":60,"heightMm":25,"dpi":203,"elementsJson":"[{\"type\":\"TEXT\",\"xMm\":2,\"yMm\":2,\"value\":\"한글 TEST\",\"sizeMm\":3}]","variables":{}}' \
  "$EDGE_URL/api/labels/preview")
case "$CT" in
  "200 image/png"*) ok "preview 렌더 = 200 image/png (한글 포함)";;
  *) bad "preview 렌더 = $CT (폰트/렌더 오류 의심)";;
esac
RC=$(curl -s -o /dev/null -w "%{http_code} %{content_type}" "${AUTH[@]}" "$EDGE_URL/api/labels/ruler?widthMm=60&heightMm=25&dpi=203")
case "$RC" in "200 image/png"*) ok "mm 눈금자 렌더 = 200";; *) warn "ruler = $RC";; esac

# 3) 클라우드 동기화 상태(허브 연동 유닛만)
echo "[3] 클라우드 동기화"
if grep -q '"cloud":{"status":"UP"' /tmp/ps_health.json 2>/dev/null; then ok "cloud 구성요소 UP(허브 접촉 정상)"
elif grep -q '"cloud":{"status":"DOWN"' /tmp/ps_health.json 2>/dev/null; then bad "cloud 구성요소 DOWN — 허브 주소/네트워크 확인"
else warn "cloud 구성요소 없음(허브 비활성 유닛일 수 있음)"; fi

# 4) 허브가 이 유닛을 온라인으로 인지하는가(HUB_URL+DEVICE_NAME 지정 시)
if [ -n "$HUB_URL" ] && [ -n "$DEVICE_NAME" ]; then
  echo "[4] 허브 등록/온라인"
  DEVJSON=$(curl -s "$HUB_URL/api/admin/devices" 2>/dev/null)
  if echo "$DEVJSON" | grep -q "\"name\":\"$DEVICE_NAME\""; then
    if echo "$DEVJSON" | grep -o "\"name\":\"$DEVICE_NAME\"[^}]*" | grep -q '"online":true'; then
      ok "허브가 '$DEVICE_NAME' 온라인으로 인지"
    else warn "허브에 '$DEVICE_NAME' 있으나 online=false(하트비트 대기)"; fi
  else bad "허브에 '$DEVICE_NAME' 미등록 — org-key/허브주소 확인"; fi
else
  echo "[4] 허브 등록/온라인 — HUB_URL/DEVICE_NAME 미지정, 건너뜀"
fi

# 5) 실제 인쇄 스모크(--print). 라벨 1장 소모.
if [ "$DO_PRINT" = "yes" ]; then
  echo "[5] 실제 인쇄 스모크(라벨 1장)"
  PC=$(curl -s -o /tmp/ps_print.txt -w "%{http_code}" "${AUTH[@]}" -H "Content-Type: application/json" \
    -d '{"widthMm":60,"heightMm":25,"dpi":203,"elementsJson":"[{\"type\":\"TEXT\",\"xMm\":2,\"yMm\":2,\"value\":\"ACCEPTANCE\",\"sizeMm\":3},{\"type\":\"QR\",\"xMm\":40,\"yMm\":2,\"value\":\"ACCEPT-OK\",\"sizeMm\":18}]","variables":{},"copies":1}' \
    "$EDGE_URL/api/labels/print")
  if [ "$PC" = "200" ]; then ok "인쇄 요청 200 — 프린터에서 라벨(ACCEPTANCE+QR) 실물 확인하세요"
  else bad "인쇄 요청 HTTP=$PC ($(cat /tmp/ps_print.txt))"; fi
else
  echo "[5] 실제 인쇄 스모크 — 생략(--print 로 활성화)"
fi

echo
echo "== 결과: PASS=$PASS  FAIL=$FAIL  WARN=$WARN =="
[ "$FAIL" -eq 0 ] && { echo "✅ 수용검사 통과 — 유닛 인계 가능"; exit 0; } || { echo "❌ 실패 항목 있음 — 위 ❌ 확인"; exit 1; }
