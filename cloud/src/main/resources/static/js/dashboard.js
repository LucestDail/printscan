// 클라우드 대시보드 — 플릿/집계/재고 조회 + 네트워크 출력 지시. 3초 폴링.
'use strict';
const $ = (id) => document.getElementById(id);
function toast(m){const t=$('toast');t.textContent=m;t.style.display='block';setTimeout(()=>t.style.display='none',2600);}

// 관리자 요청 — 서버 세션(JSESSIONID 쿠키)로 인증. 헤더/localStorage 키 노출 없음.
function adminFetch(url, opts) {
  return fetch(url, opts || {});
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

    const tpls = await adminFetch('/api/admin/templates').then(r=>r.json());
    $('tplBody').innerHTML = (tpls||[]).map(x=>`<tr><td>${x.name}</td><td class="muted">${x.widthMm}×${x.heightMm}mm</td>
      <td><a class="text-link" href="#" onclick="delTpl(${x.id});return false;">✕</a></td></tr>`).join('')
      || '<tr><td class="muted">–</td></tr>';

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

async function addTpl() {
  const body = { name: $('tplName').value || 'template', widthMm: 40, heightMm: 25, dpi: 203, elementsJson: $('tplElements').value || '[]' };
  const res = await adminFetch('/api/admin/templates', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  if (res.ok) { $('tplName').value=''; $('tplElements').value=''; refresh(); } else toast(t('tpl.addFail'));
}
window.delTpl = async (id) => { await adminFetch('/api/admin/templates/'+id, {method:'DELETE'}); refresh(); };

// ── org-key 로테이션 ──
async function loadOrgKey() {
  try {
    const k = await adminFetch('/api/admin/org/key').then(r=>r.ok?r.json():null);
    if (!k) return;
    $('orgKeyVal').value = k.apiKey;
    let status = '';
    if (k.previousKeyActive) status = t('org.key.previousActive', [fmt(k.previousKeyExpiresAt)]);
    else if (k.keyRotatedAt) status = t('org.key.rotatedAt', [fmt(k.keyRotatedAt)]);
    $('orgKeyStatus').textContent = status;
  } catch(_) {}
}
async function rotateKey() {
  if (!confirm(t('org.key.confirmRotate'))) return;
  const grace = parseInt($('orgGrace').value);
  const res = await adminFetch('/api/admin/org/rotate-key', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({graceMinutes: isNaN(grace)?60:grace})});
  if (!res.ok) { toast(t('org.key.rotateFail')); return; }
  const j = await res.json();
  $('orgKeyVal').value = j.apiKey;
  toast(t('org.key.rotated'));
  loadOrgKey();
}
async function revokeKey() {
  if (!confirm(t('org.key.confirmRevoke'))) return;
  const res = await adminFetch('/api/admin/org/revoke-previous-key', {method:'POST'});
  if (!res.ok) { toast(t('org.key.rotateFail')); return; }
  toast(t('org.key.revoked'));
  loadOrgKey();
}

document.addEventListener('DOMContentLoaded', async () => {
  await loadI18n(document.documentElement.lang || 'ko');
  const at = document.getElementById('btnAddTpl'); if (at) at.addEventListener('click', addTpl);
  const lo = $('navLogout');
  if (lo) lo.addEventListener('click', async (e) => { e.preventDefault(); try { await fetch('/api/logout', {method:'POST'}); } catch(_){} location.href = '/login'; });
  $('npElements').value = DEFAULT_ELEMENTS;
  $('btnNetPrint').addEventListener('click', netPrint);
  const br = $('btnRotateKey'); if (br) br.addEventListener('click', rotateKey);
  const bv = $('btnRevokeKey'); if (bv) bv.addEventListener('click', revokeKey);
  const bc = $('btnCopyKey'); if (bc) bc.addEventListener('click', () => { navigator.clipboard?.writeText($('orgKeyVal').value); toast(t('org.key.copied')); });
  loadOrgKey();
  refresh();
  setInterval(refresh, 3000);
});
