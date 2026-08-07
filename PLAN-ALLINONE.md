# PrintScan — 올인원 솔루션 기획안 (실기계 연동 대비)

> 작성: 2026-08-05 · 대상: **금요일 재택 세션(실제 프린터/스캐너 연결 + 세팅)** 사전 준비용
> 목표: 스캐너 실연동 + 라벨 프린팅 디자인/설정 UI + GUI 관리 → **현장 올인원 솔루션**

---

## 0. 이 문서의 사용법 (금요일 흐름)

1. **§1 사전지식**을 미리 읽어 ZPL·Zebra·스캐너 동작 원리를 잡는다.
2. **§4 결정 매트릭스**에서 실환경(프린터 연결/스캐너 종류/실행 위치)을 확정한다.
3. **§6 금요일 세팅 체크리스트**를 순서대로 실행(연결성 → ZPL 스모크 → 디자이너 MVP).
4. 이후 **§5 로드맵** Phase 순으로 개발.

---

## 0.5 ✅ 확정 아키텍처 (2026-08-05 사용자 결정)

> **앱과 프린터를 둘 다 `.25` 우분투 서버에 둔다. 프린터를 .25에 직결(USB 또는 LAN — 현 연결방식 미확인)하고, .25에서 실행되는 앱이 로컬로 직접 인쇄한다.**

- **핵심 이점**: 앱과 프린터가 **동일 호스트(.25)** → "웹서버가 클라이언트 USB 못 잡는" 문제가 **원천 소멸**. .25의 JVM이 .25에 붙은 프린터를 직접 제어 가능. 현행 `javax.print` 방식(§경로 C)이 **오히려 자연스럽게 성립**한다(단, .25 CUPS에 raw 큐 필요 — §1-6).
- **연결 방식 미확정**: 기존이 USB였는지 LAN이었는지 불명 → **.25에서 직접 판별**(§1-6, §6). USB면 `/dev/usb/lp0` raw 또는 CUPS raw 큐, LAN이면 IP:9100 raw.
- ⚠️ **작업 환경 제약**:
  - **회사 맥(현재 PC)**: 프린터 물리 연결 불가 + **Zscaler로 .25 SSH 접속도 막힘** → 여기선 **코드 작성만**, 실기계·프린터 검증은 **집/VPN에서 .25 접속** 또는 .25 콘솔에서.
  - 즉 "프린터 실제 동작 확인"은 전부 **.25 우분투 기준**. 회사 맥에서 되는지는 무의미(테스트 안 함).
- **결론**: 출력 경로 기본값 = **.25 로컬 직결**. LAN이면 9100 raw(권장), USB면 CUPS raw 큐/`/dev/usb/lp0`. 스캐너는 웹(브라우저) 접속 클라이언트에서 HID 캡처(스캐너는 관리자 PC에 연결, 프린터와 별개).

---

## 1. 사전 지식 (미리 읽어둘 것)

### 1-1. ZPL (Zebra Programming Language) — 라벨의 "HTML"
Zebra 프린터는 이미지가 아니라 **ZPL 텍스트 명령**을 받아 인쇄한다. 라벨 1장 = `^XA`(시작) … `^XZ`(끝).

| 명령 | 뜻 | 예 |
|---|---|---|
| `^XA` / `^XZ` | 라벨 시작/끝 | — |
| `^PW` / `^LL` | 인쇄 폭 / 라벨 길이 (dots) | `^PW812 ^LL609` |
| `^FO x,y` | 필드 위치(좌상단 기준, dots) | `^FO50,50` |
| `^A0N,h,w` | 폰트(0=스케일러블, N=정방향, h/w=크기) | `^A0N,40,40` |
| `^FD ... ^FS` | 데이터 + 필드 종료 | `^FDPROD-001^FS` |
| `^BQN,2,cell` | QR 코드 | `^BQN,2,8` |
| `^BCN,h,Y,N,N` | Code128 바코드 | `^BCN,80,Y,N,N` |
| `^GB w,h,t` | 사각형/선(박스) | `^GB760,2,2` |
| `~SD` / `^PR` | 농도(0~30) / 속도 | `~SD15` |
| `^PQ n` | 인쇄 매수 | `^PQ3` |

- **해상도**: 203dpi = **8 dots/mm**, 300dpi = 12 dots/mm. 좌표는 전부 dots. (40×30mm 라벨 @203dpi = 320×240 dots)
- 현재 소스가 `generateQrZpl / generateTextZpl / generateQrWithTextZpl`로 이미 ZPL을 만들고 있음 → 여기서 확장한다.

### 1-2. 라벨을 프린터로 "보내는" 4가지 경로 (가장 중요)
웹 서버는 사용자 PC의 USB 장치에 직접 접근 **불가**. 그래서 출력 경로 선택이 핵심.

| 경로 | 방법 | 장점 | 제약 |
|---|---|---|---|
| **A. 네트워크 Raw 9100** | 프린터 IP:9100 TCP 소켓에 ZPL 바이트 write | 서버가 어디 있든 직접 출력, 원격배포 OK | 프린터가 유선/WiFi(IP) 있어야 |
| **B. Zebra Browser Print** | Zebra 공식 로컬앱 + `BrowserPrint.js` → 웹페이지가 localhost로 USB/네트워크 Zebra 제어 | USB 프린터를 웹앱에서 정석적으로 사용 | 각 PC에 Browser Print 설치 필요 |
| **C. javax.print (현행)** | `PrintServiceLookup`로 **서버 호스트 OS**의 등록 프린터에 전송 | 코드 이미 있음 | 앱이 프린터 붙은 PC에서 돌 때만 |
| **D. OS 스풀러/CUPS** | 서버 OS에 프린터 등록 후 raw 전송 | — | C와 동일 한계 |

> **권장 기본값 = A(네트워크 9100)**, USB 전용이면 **B(Browser Print)**. C는 단일 현장 PC 데모용.

### 1-3. 라벨 미리보기 — Labelary API (무료, 키 불필요)
ZPL을 이미지로 렌더해 **WYSIWYG 디자이너**를 만들 수 있다.
```
POST http://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/
Body: <ZPL 원문>   →   응답: PNG (또는 Accept: application/pdf)
```
- `8dpmm`=203dpi, `12dpmm`=300dpi / `4x6`=라벨 inch 크기. 디자이너 편집 → 실시간 프리뷰에 이상적.

### 1-4. 스캐너 두 종류 — 접근 방식이 완전히 다름
| 종류 | 동작 | 웹 연동 |
|---|---|---|
| **바코드/QR 핸디 스캐너** | 대부분 **HID(키보드) 에뮬레이션** — 스캔 시 코드를 "타이핑"하고 Enter | **드라이버·서버코드 불필요**. 웹에서 포커스된 input이 값을 받음. 즉시 가능 |
| **문서/평판 스캐너** | TWAIN/WIA(윈도우)·SANE(리눅스) 드라이버 | 웹 직접 불가 → **스캔-투-폴더**(네트워크 스캐너가 SMB/FTP로 저장) 서버 감시, 또는 로컬 에이전트 |

- 바코드 스캐너 웹 캡처 요령: 숨김 input에 항상 포커스, keydown 버스트(수십ms 내 연속입력) + Enter로 1건 확정.

### 1-5. QR/바코드 생성
- **인쇄용**: ZPL 네이티브 `^BQ`(QR)·`^BC`(바코드) — 프린터가 직접 렌더(현행).
- **화면표시/검증용**: ZXing(Java)로 QR 이미지 생성·디코드(현행 QR 관리).

### 1-6. 우분투(.25) 프린터 직결 — 판별 & 출력 경로 (이번 아키텍처 핵심)
리눅스 인쇄는 **CUPS**가 표준. Zebra는 ZPL을 그대로 받아야 하므로 **드라이버가 데이터를 변형하지 않는 "raw"** 로 다뤄야 한다.

**(1) 현재 연결이 USB인지 LAN인지 .25에서 판별**
```bash
# USB 연결이면 lsusb 에 Zebra 보이고 /dev/usb/lp0 생성됨
lsusb | grep -i zebra
ls -l /dev/usb/lp*                 # USB raw 장치 노드
# CUPS 등록 프린터 확인
lpstat -p -d ; lpinfo -v           # usb://Zebra... 또는 socket://<IP>:9100 확인
# LAN이면 프린터 IP:9100 열려있는지
nc -z <PRINTER_IP> 9100 && echo "9100 open (LAN)"
```

**(2-A) LAN 직결 (권장, 드라이버 불요)**
- 앱 → `Socket(IP, 9100)` 에 ZPL 바이트 write. 가장 단순·안정.
- .25에서: `printf '^XA^FO50,50^A0N,40,40^FDHELLO^FS^XZ' | nc <IP> 9100`

**(2-B) USB 직결**
- 방법①(가장 raw): 장치 노드에 직접 write → `cat label.zpl > /dev/usb/lp0`
  - 앱(JVM)이 쓰려면 서비스 실행 유저가 `/dev/usb/lp0` 쓰기권한 필요 → 유저를 `lp`(또는 `lpadmin`) 그룹에 추가: `sudo usermod -aG lp <svc-user>` + udev 권한.
- 방법②(CUPS raw 큐 → 현행 javax.print 그대로 동작):
  ```bash
  # raw 큐 생성 (드라이버 없이 그대로 전달)
  sudo lpadmin -p zebra -E -v usb://Zebra/ZD421... -m raw
  sudo lpadmin -d zebra                       # 기본 프린터
  lp -d zebra -o raw label.zpl                # CLI 테스트
  ```
  - CUPS raw 큐가 있으면 **`javax.print.PrintServiceLookup`가 "zebra"를 찾아 현행 `printZpl` 코드가 그대로 동작**(§경로 C). → 이번 아키텍처의 최소변경 경로.

**(3) 데몬/서비스 주의(.25 헤드리스)**
- .25는 GUI 없는 서버 → `javax.print`는 CUPS만 있으면 headless에서도 동작(CUPS 데몬 필요: `systemctl status cups`).
- systemd로 앱 구동 시: 서비스 유저가 **CUPS 접근 & (USB면) `/dev/usb/lp0` 권한** 있어야 함.
- **권장**: LAN이면 9100 raw(권한/CUPS 무관), USB면 CUPS raw 큐(권한만 정리하면 현행 코드 재사용).

---

## 2. 현재 소스 상태 (정밀 진단, 2026-08-05 기준)

### 2-1. 스택
- **Spring Boot 3.3.10** / **JPA + MyBatis 병용**(dual persistence) / **JWT**(jjwt 0.11.5)
- DB: **MariaDB**(운영) + H2(의존성 존재) / QR: ZXing + ZPL 네이티브
- 프론트: **정적 HTML + Bootstrap 5** (SPA 아님) — 38개 페이지
- 규모: Java 74 · 컨트롤러 14 · 서비스 13 · 엔티티 16 · 화면 38

### 2-2. 구현된 기능 (MVP 골격 ~완성)
- **인증/권한**: JWT 발급·필터·프로바이더, User/Role, 활동 로그(UserActivityLog)
- **인쇄**: PrintJob(제목/매수/우선순위/상태/양면/컬러/스테이플/노트), 작업 큐(JobQueue: jobType·jobStatus·jobOptions(JSON)·priority), **Zebra ZD421 ZPL 출력**
- **스캔**: 요청/상태/이력 화면(백엔드 수신 로직은 얕음 — 확장 대상)
- **QR**: QrCode(code·type PRINT/SCAN·used·expiresAt), 제품 QR 이력(ProductQrHistory)
- **제품/재고**: Product(UUID·code·name·unit·qrCode·**min/maxStockQuantity**·status), Category — 재고 필드는 있으나 입출고 로직 미구현
- **부가**: 알림(Notification), 파일(FileInfo/FileStorage), 회사/계정, 리포트 대시보드 화면

### 2-3. 프린팅 핵심 코드 (확장 기준점)
- `PrinterService.findPrinter(String...)` → **`javax.print.PrintServiceLookup`** (서버 호스트 OS 프린터 조회, "zebra"/"zd421" 이름 매칭)
- `PrinterService.printZpl(printer, zpl)` → `DocPrintJob` + `DocFlavor.BYTE_ARRAY.AUTOSENSE`
- ZPL 생성: `generateQrZpl(data,x,y,rotation,cellSize)`, `generateTextZpl(text,x,y,fontSize)`, `generateQrWithTextZpl(data,text)`
- `PrinterController`: `/usb/qr`, `/usb/text` (하드코딩된 zd421 대상)
- ⚠️ **한계**: javax.print라 "앱이 프린터 붙은 PC에서 실행" 전제. 프린터/라벨 크기/DPI 하드코딩, 템플릿 개념 없음.

### 2-4. 위험/부채 (금요일 전 인지)
- `ddl-auto=create-drop` → **재시작 시 DB 초기화** (실데이터 세팅 전 반드시 `validate`/`update`로)
- MariaDB 접속정보(원격 IP)·**JWT 시크릿 평문**·`admin/admin`·CORS `*` 하드코딩
- JPA+MyBatis 이중 영속 계층(혼란 소지)
- Dockerfile/프로필 분리 없음

---

## 3. 지향점 — "현장 올인원 솔루션"

**한 화면에서 제품 등록 → 라벨 디자인 → QR/바코드 인쇄 → 스캔 입출고 → 재고/이력/리포트**까지 도는 통합 관리 툴.

핵심 가치:
1. **라벨 디자이너**: 코딩 없이 드래그로 라벨 설계, 실시간 미리보기, 템플릿 재사용·변수 바인딩.
2. **원터치 스캔 워크플로우**: 바코드 스캔 → 제품 자동 인식 → 입/출고·검수 즉시 기록.
3. **하드웨어 무관 아키텍처**: 네트워크/USB 프린터·스캐너를 브리지로 흡수, 현장 PC·원격 서버 모두 지원.
4. **운영 GUI**: 프린터 상태·큐·재고·이력·비용을 관리자 화면에서 통제.

---

## 4. 결정 매트릭스 (금요일 시작 전 확정)

| 결정 | 옵션 | **확정/권장** | 비고 |
|---|---|---|---|
| **앱 실행 위치** | 현장 PC / **.25 우분투** | ✅ **.25 우분투 확정** | 프린터와 동일 호스트 |
| **프린터 연결** | LAN(IP) / USB | **.25 직결(방식 판별 필요)** | §1-6로 USB/LAN 판별 |
| **출력 경로** | A(9100)/B(BrowserPrint)/**C(javax)**/D(raw dev) | LAN→A(9100) · USB→C(CUPS raw 큐, 현행코드 재사용) 또는 D(`/dev/usb/lp0`) | 동일호스트라 C 성립 |
| **스캐너** | 바코드 HID / 문서 스캐너 | 바코드 HID 우선 | 관리자 브라우저 PC에서 HID 캡처(프린터와 분리) |
| **프론트** | 정적 HTML 강화 / Vue SPA | 디자이너만 별도 SPA 모듈 | 점진 |

---

## 5. 방법론 & 로드맵 (Phase)

### Phase 0 — 안정화 (실데이터 넣기 전 필수, 30분~반나절)
- `ddl-auto` → `validate`(또는 최초 1회 `update`) + 스키마 고정 (create-drop 절대 금지)
- 시크릿/DB/CORS → 환경변수(`application-local.properties` / `-prod`) 분리
- 프린터 설정 외부화: `printscan.printer.mode=network|browserprint|javax`, `printer.host`, `printer.port=9100`, `dpi`

### Phase 1 — 출력 경로 추상화 (금요일 핵심 1)
- `PrintTransport` 인터페이스 도입:
  - `NetworkPrintTransport`(신규): `Socket(host, 9100)` → ZPL 바이트 write
  - `JavaxPrintTransport`(현행 래핑)
  - `BrowserPrintTransport`(프론트 SDK 경유 — 서버는 ZPL만 반환)
- `PrinterController` → 모드에 따라 transport 선택. 하드코딩 zd421 제거.
- **연결성 스모크**: 프린터 IP ping → 9100 소켓 → `^XA^FO50,50^A0N,40,40^FDHELLO^FS^XZ` 1장.

### Phase 2 — 라벨 디자이너 (금요일 핵심 2, 최대 작업)
- 데이터 모델 신설: `LabelTemplate`(id, name, widthMm, heightMm, dpi, zplBody, elementsJson, variables[])
- 프론트 캔버스 디자이너(fabric.js/konva.js): 텍스트/바코드/QR/이미지/박스 요소 드래그 → 좌표→dots 변환 → **ZPL 생성기**
- **Labelary 프리뷰** 실시간 렌더(편집 즉시 PNG)
- **변수 바인딩**: `{{product.code}}` 등 placeholder → 인쇄 시 실데이터 치환
- 템플릿 CRUD + "이 제품에 이 템플릿으로 N장 인쇄"

### Phase 3 — 스캐너 워크플로우 (금요일 핵심 3)
- 바코드 HID: 전역 스캔 캡처 컴포넌트(숨김 input 포커스 + 버스트 감지) → 스캔 이벤트 버스
- 스캔 → 제품 조회 → 액션(입고/출고/검수) 화면. QrCode/ProductQrHistory에 기록
- (선택) 문서 스캐너: 스캔-투-폴더 감시(WatchService) → FileInfo 등록 → (후속) AI OCR

### Phase 4 — 재고/운영 (후속)
- QR 스캔 기반 입출고 트랜잭션 + 재고 현황/부족 알림(min/maxStockQuantity 활용)
- 프린터 상태(SNMP/9100 `~HS` host status 질의)·토너/용지·에러 알림
- 비용/건수 통계 리포트

### Phase 5 — 배포 (집/VPN)
- Dockerfile(멀티스테이지) + compose(앱+MariaDB) + 프로필 스위칭

---

## 6. 금요일 세팅 체크리스트 (순서대로)

**환경**: 모든 프린터 작업은 **.25 우분투에서** (집/VPN으로 .25 SSH 접속 or .25 콘솔). 회사 맥에선 코드만.
**준비물**: Zebra 프린터 · 라벨지 · USB케이블 또는 랜케이블(.25에 직결) · 바코드 스캐너(관리자 PC용) 

0. **[.25 접속]** 집/VPN에서 `ssh seunghyun@192.168.11.25` (회사 맥·Zscaler는 불가)
1. **[연결 방식 판별]** (§1-6) `.25`에서:
   - `lsusb | grep -i zebra` / `ls /dev/usb/lp*` (USB?) · `lpstat -p` · 프린터에 IP 있으면 `nc -z <IP> 9100` (LAN?)
   - → USB인지 LAN인지 여기서 확정
2. **[안정화]** `ddl-auto=validate`(또는 백업) — 실데이터 날림 방지 + 시크릿/DB env 분리
3. **[ZPL 스모크 — .25 로컬]**
   - LAN: `printf '^XA^FO50,50^A0N,40,40^FDHELLO^FS^XZ' | nc <IP> 9100`
   - USB(CUPS raw 큐): `sudo lpadmin -p zebra -E -v usb://Zebra/... -m raw` → `lp -d zebra -o raw hello.zpl`
   - USB(장치 직접): `cat hello.zpl > /dev/usb/lp0` (권한 필요 시 `usermod -aG lp`)
4. **[앱 연동]** `.25`에서 앱 실행 + `printscan.printer.mode=network(9100)|javax(cups)|rawdev` 설정 → QR/텍스트 1장 인쇄. systemd면 서비스 유저 CUPS/`lp` 권한 확인
5. **[스캐너]** 관리자 브라우저 PC에서 스캔 → 메모장에 값+Enter(=HID) → 웹 캡처 컴포넌트 테스트
6. **[디자이너 MVP]** 텍스트+QR 배치 → Labelary 프리뷰 → .25 프린터로 실제 인쇄 왕복
7. **[제품 연동]** 제품 1건 등록 → QR 라벨 인쇄 → 스캔 → 조회

**성공 기준(금요일 종료 시)**: `.25`에서 제품 등록 → 라벨 디자인/인쇄 → 스캔으로 그 제품 조회, 한 바퀴가 실기계로 돈다.

---

## 7. 사전 확인 (확정/대기)
**확정됨**
- [x] 앱 실행 위치 = **.25 우분투** (프린터와 동일 호스트 직결)
- [x] 작업 제약 = 회사 맥은 코드만, 프린터 검증은 .25(집/VPN)

**금요일 .25에서 판별/확정 (지금 몰라도 됨 — §1-6로 판별)**
- [ ] 프린터 연결 = **USB or LAN** (`lsusb`/`lpstat`/`nc :9100`로 .25에서 판별)
- [ ] 프린터 모델·**DPI(203/300)**, 라벨 규격(mm, 예 40×30)
- [ ] 스캐너 모델/종류(바코드 HID? 문서?)
- [ ] MariaDB 실데이터 유지 필요 여부(→ `create-drop` 정리 판단)

> 실행위치가 .25로 확정됐으므로, **Phase 1(PrintTransport 추상화: network9100 + cups-raw + rawdev) + Phase 2(라벨 디자이너)** 를 금요일 전에 미리 스캐폴딩 가능. USB/LAN은 런타임 설정(`printer.mode`)으로 흡수하도록 설계하면 판별 결과와 무관하게 붙일 수 있음.
