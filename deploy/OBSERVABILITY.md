# 관측성 — Prometheus 스크레이프 + Grafana

> printscan edge/cloud는 Micrometer로 **JVM/HTTP 기본 + 도메인 메트릭**을 `/actuator/prometheus`로 노출한다.
> 메트릭 이름은 `.25` 라이브에서 확인됨(2026-08-11). 상세 명세는 [`../SPEC.md`](../SPEC.md) §12.

## 1. 노출 엔드포인트 / 인증

| 모듈 | 엔드포인트 | 인증 |
|---|---|---|
| edge (8091) | `/actuator/prometheus` | **HTTP Basic 필요**(SecurityConfig가 `/actuator/health`만 개방) |
| cloud (8092) | `/actuator/prometheus` | 개방(온프렘). SaaS는 리버스 프록시 뒤 |

> `management.endpoints.web.exposure.include=health,info,metrics,prometheus` (기본 설정).

## 2. 도메인 메트릭 카탈로그

| 메트릭 | 모듈 | 타입 | 태그 | 의미 |
|---|---|---|---|---|
| `printscan_labels_printed_total` | edge | counter | `line` | 실제 인쇄한 라벨 수(장) |
| `printscan_jobs_enqueued_total` | cloud | counter | — | 원격 인쇄 지시(잡 큐잉) 수 |
| `printscan_jobs_completed_total` | cloud | counter | `result`=done\|failed | 잡 완료(성공/실패) 수 |
| `printscan_consumption_total` | cloud | counter | — | 소비(출고) 수량 누계 |

보조(기본 제공): `jvm_*`, `http_server_requests_seconds_*`, `process_*`, `hikaricp_*`.

## 3. Prometheus 스크레이프 설정 (`prometheus.yml`)

```yaml
scrape_configs:
  - job_name: printscan-edge
    metrics_path: /actuator/prometheus
    basic_auth:
      username: admin
      password: <PRINTSCAN_SECURITY_PASSWORD>   # edge 는 인증 필요
    static_configs:
      - targets: ['<PI_IP>:8091']
        labels: { unit: 'line-1' }               # 라인/유닛 식별 라벨 부여 권장
  - job_name: printscan-cloud
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['<HUB_IP>:8092']
```
> 여러 Pi가 있으면 `printscan-edge` job의 `targets`에 유닛별로 추가(라벨로 라인 구분).

## 4. 유용한 PromQL

```promql
# 라인별 분당 인쇄량
sum by (line) (rate(printscan_labels_printed_total[5m])) * 60

# 원격 잡 성공률(최근 1시간)
sum(increase(printscan_jobs_completed_total{result="done"}[1h]))
  / clamp_min(sum(increase(printscan_jobs_enqueued_total[1h])), 1)

# 실패 잡 발생(알림 후보)
sum(increase(printscan_jobs_completed_total{result="failed"}[10m])) > 0

# 소비(출고) 분당 추이
rate(printscan_consumption_total[5m]) * 60

# 유닛 다운 감지(스크레이프 실패)
up{job="printscan-edge"} == 0
```

## 5. Grafana

- 데이터소스: Prometheus 추가.
- 대시보드: [`grafana-dashboard.json`](grafana-dashboard.json) 임포트(Dashboards → Import → Upload JSON). 임포트 시 Prometheus 데이터소스 선택.
- ⚠️ 이 대시보드 JSON은 위 메트릭 이름 기준으로 작성됨. Grafana 버전에 따라 임포트 후 패널 데이터소스 재바인딩이 필요할 수 있음(라이브 Grafana 미검증 — 스크레이프/메트릭 이름은 실확인).

## 6. 알림 연동
- 앱 자체 webhook 알림(저재고·프린터·백업·오프라인)은 [`RUNBOOK.md`](RUNBOOK.md) 부록 참고.
- Prometheus Alertmanager로는 위 PromQL(실패 잡·유닛 다운)을 규칙화 권장.
