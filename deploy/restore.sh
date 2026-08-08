#!/usr/bin/env bash
# H2 백업(zip) 복원. BackupService 가 만든 data/backup/*.zip 을 되돌린다.
# 사용법: sudo bash restore.sh <backup.zip> [service] [data-dir]
#   기본 service=printscan-edge, data-dir=/opt/printscan-edge/data
# 절차: 서비스 중지 → 기존 data 보존 이동 → unzip → 재시작.
set -euo pipefail

ZIP="${1:?backup.zip 경로 필요}"
SVC="${2:-printscan-edge}"
DATA="${3:-/opt/printscan-edge/data}"

[ -f "$ZIP" ] || { echo "백업 파일 없음: $ZIP"; exit 1; }
command -v unzip >/dev/null || { echo "unzip 필요: apt install unzip"; exit 1; }

echo "== $SVC 중지 =="
systemctl stop "$SVC" || true
sleep 2

ts=$(date +%s)
if [ -d "$DATA" ]; then mv "$DATA" "${DATA}.bak.${ts}"; echo "기존 data 보존: ${DATA}.bak.${ts}"; fi
mkdir -p "$DATA"

echo "== 복원: $ZIP → $DATA =="
unzip -o "$ZIP" -d "$DATA" >/dev/null

echo "== $SVC 재시작 =="
systemctl start "$SVC"
echo "복원 완료. 문제 시 롤백: systemctl stop $SVC; rm -rf $DATA; mv ${DATA}.bak.${ts} $DATA; systemctl start $SVC"
