# printscan 사용별(역할별) 상세 가이드

> 역할/상황별로 "무엇을 하려면 어떻게" 를 흐름으로 정리한다. 기능 상세는 [`GUIDE-FEATURES.md`](GUIDE-FEATURES.md), 설치는 [`deploy/`](deploy), 장애 대응은 [`deploy/RUNBOOK.md`](deploy/RUNBOOK.md).

대상 역할
- [A. 현장 작업자](#a-현장-작업자-operator) — 라벨 인쇄·스캔 입출고
- [B. 라인/공장 관리자](#b-라인공장-관리자-admin) — 대시보드·원격 출력·집계·템플릿·키
- [C. 설치 기사](#c-설치-기사-installer) — 유닛 설치·인계
- [D. 시스템 관리자(IT)](#d-시스템-관리자-it) — 배포·백업·보안·관측성

---

## A. 현장 작업자 (operator)
> 위치: 라인 옆 유닛 화면 `http://<PI_IP>:8091` (아이디/비번은 관리자에게).

### A-1. 라벨 인쇄하기
1. `/designer` 열기 → 저장된 **템플릿 선택**(또는 새로 배치).
2. 변수 입력(예: 제품명·코드) → **미리보기 확인**(보이는 그대로 인쇄됨).
3. 매수 입력 → **인쇄**. 프린터에서 라벨이 나온다.
4. 연속 번호가 필요하면(예: 일련번호 라벨) 배치 인쇄로 `NET-0001…` 자동 증가.

### A-2. 스캔으로 입·출고
1. `/scan` 열기 → 바코드 스캐너로 제품 코드 스캔 → 제품 정보 표시.
2. **입고/출고/조정** 선택 → 수량·작업자 입력 → 확정.
3. 처음 보는 코드면 **제품 등록**(코드·이름 필수).
4. 재고가 부족하면 출고가 거부된다 — 실물 확인 후 다시.

### A-3. 자주 겪는 상황
- **한글이 안 나와요** → 관리자에게 폰트 설치 요청([RUNBOOK §2](deploy/RUNBOOK.md)).
- **라벨이 잘려요** → 관리자에게 눈금자 실측 요청([GUIDE-FEATURES §3](GUIDE-FEATURES.md)).
- **언어 바꾸기** → 우측 상단 KO·EN·VI·ID.

---

## B. 라인/공장 관리자 (admin)
> 위치: 허브 대시보드 `http://<HUB_IP>:8092` (조직 키로 로그인, 또는 단일 조직이면 바로).

### B-1. 현황 모니터링
- 대시보드에서 **온라인 유닛·누적 인쇄·대기 잡**, **라인/작업자/제품별 소비 집계**, 재고 스냅샷, 최근 잡 확인.
- 유닛이 오프라인이면 네트워크/전원 확인([RUNBOOK §4](deploy/RUNBOOK.md)).

### B-2. 원격으로 인쇄 지시
1. 대상 장비 선택 → 라벨 정의·매수 입력 → **이 장비로 인쇄**.
2. 잡 상태(`QUEUED→SENT→DONE`)로 진행 확인. 유닛이 꺼져 있으면 켜질 때 자동 출력.

### B-3. 공통 양식 배포(중앙 템플릿)
- 대시보드에서 템플릿을 추가하면 모든 유닛이 자동 동기화 → 현장마다 따로 안 만들어도 됨.

### B-4. 소비/생산 리포트
- 라인별·작업자별·제품별 집계로 "누가 뭘 얼마나" 파악. 상세 시계열은 Grafana([OBSERVABILITY](deploy/OBSERVABILITY.md)).

### B-5. 조직 키 관리
- **정기 교체**: 조직 키 카드 → 유예(예 60분) 지정 후 **키 재발급** → 유예 안에 각 유닛 갱신(무중단).
- **유출 대응**: **직전 키 즉시 폐기**.

---

## C. 설치 기사 (installer)
> 새 라인에 유닛을 설치·인계. 상세: [`deploy/README-PI.md`](deploy/README-PI.md), 대량 복제: [`deploy/README-IMAGE.md`](deploy/README-IMAGE.md).

### C-1. 유닛 1대 설치 (3단계)
```bash
# 1) 설치(앱·CUPS·한글폰트·raw큐·systemd)
sudo bash /tmp/deploy/install-edge.sh /tmp/app.jar USB
# 2) 이 유닛 설정(라인·허브·조직키)
sudo bash /tmp/deploy/provision-edge.sh --line "1라인" --hub "http://<HUB_IP>:8092" --org-key "<키>" --device "edge-1라인"
# 3) 수용검사(통과=인계)
EDGE_URL=http://localhost:8091 EDGE_USER=admin EDGE_PASS=<pw> HUB_URL=http://<HUB_IP>:8092 DEVICE_NAME=edge-1라인 \
  bash /tmp/deploy/acceptance-test.sh --print
```
### C-2. 인계 전 확인
- 수용검사 **FAIL=0**(헬스·프린터·한글렌더·허브 온라인·실물 인쇄).
- 라벨 미디어 실측·캘리브레이션([GUIDE-FEATURES §3](GUIDE-FEATURES.md)).
- 기동 로그에 **보안 경고**가 있으면 관리자에게 비밀번호/조직키 교체 요청.
- 여러 대면 골든 SD 이미지로 복제 후 유닛별 프로비저닝만([README-IMAGE](deploy/README-IMAGE.md)).

---

## D. 시스템 관리자 (IT)
> 서버/보안/운영. 명령 상세: [`deploy/RUNBOOK.md`](deploy/RUNBOOK.md).

### D-1. 배포 형태 결정
- **온프렘 MAIN**(공장장 PC, 폐쇄망): 간단·무설정(H2). 첫 파일럿 권장.
- **SaaS(AWS)**: 인터넷·멀티공장. TLS(리버스 프록시) + admin-token + Postgres. [`deploy/README-HUB.md`](deploy/README-HUB.md).

### D-2. 보안 필수 조치(배포 전)
- edge 비밀번호 교체: `PRINTSCAN_SECURITY_PASSWORD`.
- 조직 키 교체(데모 키 폐기): 대시보드 로테이션(§B-5).
- SaaS면 `PRINTSCAN_HUB_ADMIN_TOKEN` 설정 + TLS 프록시.
- 기동 시 **보안 점검 WARN 배너**가 사라졌는지 확인.

### D-3. 백업 / 복원
- 자동: 매일 03:00 H2 백업(`data/backup/*.zip`).
- 복원: `sudo bash deploy/restore.sh <backup.zip> printscan-edge /opt/printscan-edge/data`.

### D-4. 업데이트 / 롤백
```bash
sudo install -o printscan -g printscan /tmp/app.jar /opt/printscan-edge/app.jar
sudo systemctl restart printscan-edge && bash acceptance-test.sh   # 즉시 재검증
```
- 교체 전 `app.jar.prev` 보관으로 즉시 롤백 대비.

### D-5. 관측성 / 알림
- Prometheus 스크레이프 + Grafana 대시보드([`deploy/OBSERVABILITY.md`](deploy/OBSERVABILITY.md), [`deploy/grafana-dashboard.json`](deploy/grafana-dashboard.json)).
- webhook 알림(`PRINTSCAN_ALERT_WEBHOOK`): 저재고·프린터·백업·오프라인.

### D-6. 장애 대응
- [`deploy/RUNBOOK.md`](deploy/RUNBOOK.md) — 인쇄 불가·오프라인·잡 정체·키 유출 등 증상→진단→조치.

---

## 부록: API 연동(개발자)
- 대화형 문서: `http://<host>:8091/swagger-ui.html`(edge) · `:8092/swagger-ui.html`(cloud).
- 기계가독 스펙: `/v3/api-docs`. 인증·엔드포인트 상세: [`SPEC.md`](SPEC.md).
