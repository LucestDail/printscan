// 클라우드 대시보드 — 플릿/집계/재고 조회 + 네트워크 출력 지시. 3초 폴링.
'use strict';
const $ = (id) => document.getElementById(id);
function toast(m){const t=$('toast');t.textContent=m;t.style.display='block';setTimeout(()=>t.style.display='none',2600);}

// 관리자 요청 — 로그인한 org-key(localStorage)를 X-Org-Key 로 주입(온프렘 단일org면 없어도 폴백).
function adminFetch(url, opts) {
  opts = opts || {};
  const h = Object.assign({}, opts.headers || {});
  const k = localStorage.getItem('PS_ORG_KEY');
  if (k) h['X-Org-Key'] = k;
  return fetch(url, Object.assign({}, opts, { headers: h }));
}

const DEFAULT_ELEMENTS = JSON.stringify([
  {type:'TEXT',xMm:1.5,yMm:2,value:'한글 {{name}}',sizeMm:2.2,bold:true},
  {type:'TEXT',xMm:1.5,yMm:5.5,value:'{{code}}',sizeMm:2.2},
  {type:'BARCODE',xMm:1.5,yMm:9.5,value:'{{code}}',sizeMm:6.5,widthMm:18},
  {type:'QR',xMm:21.5,yMm:2,value:'{{code}}',sizeMm:17}
], null, 2);

async function refresh() {
  try {
    const statsRes = await adminFetch('/api/admin/stats');
    if (statsRes.status === 400) { location.href = '/login'; return; } // 멀티테넌트: org-key 필요/오류
    const stats = await statsRes.json();
    const [devs, snaps, jobs, consume] = await Promise.all([
      adminFetch('/api/admin/devices').then(r=>r.json()),
      adminFetch('/api/admin/snapshots').then(r=>r.json()),
      adminFetch('/api/admin/jobs').then(r=>r.json()),
      adminFetch('/api/admin/consumption').then(r=>r.json())
    ]);
    $('sDevices').textContent = stats.devices;
    $('sOnline').textContent = stats.online;
    $('sPrinted').textContent = stats.totalPrinted;
    $('sQueued').textContent = stats.queued;

    $('devBody').innerHTML = devs.map(d => `<tr>
      <td><span class="dot ${d.online?'on':'off'}"></span>${d.online?t('dev.online'):t('dev.offline')}</td>
      <td>${d.id}</td><td>${d.name}</td><td>${d.line||'-'}</td><td>${d.printerMode||'-'}</td><td>${d.printCount}</td>
      <td class="muted">${fmt(d.lastSeenAt)}</td></tr>`).join('') || `<tr><td colspan="7" class="muted">${t('empty.devices')}</td></tr>`;

    renderMap('cByLine', consume.byLine); renderMap('cByOp', consume.byOperator); renderMap('cByProd', consume.byProduct);
    $('cTotal').textContent = consume.total || 0;

    const sel = $('npDevice'), cur = sel.value;
    sel.innerHTML = devs.map(d=>`<option value="${d.id}">#${d.id} ${d.name}</option>`).join('');
    if (cur) sel.value = cur;

    $('snapBody').innerHTML = snaps.map(s=>`<tr><td>#${s.deviceId}</td><td>${s.code}</td><td>${s.name}</td><td>${s.quantity}</td></tr>`).join('')
      || `<tr><td colspan="4" class="muted">${t('empty.stock')}</td></tr>`;

    $('jobBody').innerHTML = jobs.map(j=>`<tr><td>${j.id}</td><td>#${j.deviceId}</td>
      <td><span class="badge ${j.status}">${t('job.status.'+j.status)}</span></td><td class="muted">${j.message||''}</td></tr>`).join('')
      || `<tr><td colspan="4" class="muted">${t('empty.jobs')}</td></tr>`;
  } catch(e) { /* noop */ }
}
function fmt(s){ return s ? String(s).replace('T',' ').slice(5,16) : '-'; }
function renderMap(id, obj){
  const rows = Object.entries(obj||{});
  $(id).innerHTML = rows.length ? rows.map(([k,v])=>`<tr><td>${k}</td><td style="text-align:right;font-weight:600;">${v}</td></tr>`).join('')
    : '<tr><td class="muted">–</td></tr>';
}

async function netPrint() {
  const id = $('npDevice').value;
  if (!id) { toast(t('toast.selectDevice')); return; }
  const count = parseInt($('npCount').value)||1;
  const body = {
    widthMm: parseFloat($('npW').value), heightMm: parseFloat($('npH').value), dpi: parseInt($('npDpi').value),
    elementsJson: $('npElements').value,
    variables: { name: $('npName').value, code: $('npCode').value },
    copies: parseInt($('npCopies').value)||1,
    seqVar: $('npSeqVar').value||'code', serialPrefix: $('npPrefix').value||'',
    serialStart: parseInt($('npStart').value)||1, serialCount: count, serialPad: parseInt($('npPad').value)||0
  };
  const res = await adminFetch(`/api/admin/devices/${id}/print`, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  if (!res.ok) { toast(t('toast.enqueueFail')); return; }
  const j = await res.json();
  toast(t('toast.enqueued', [j.id]));
  refresh();
}

document.addEventListener('DOMContentLoaded', async () => {
  await loadI18n(document.documentElement.lang || 'ko');
  const lo = $('navLogout');
  if (lo) lo.addEventListener('click', (e) => { e.preventDefault(); localStorage.removeItem('PS_ORG_KEY'); location.href = '/login'; });
  $('npElements').value = DEFAULT_ELEMENTS;
  $('btnNetPrint').addEventListener('click', netPrint);
  refresh();
  setInterval(refresh, 3000);
});
