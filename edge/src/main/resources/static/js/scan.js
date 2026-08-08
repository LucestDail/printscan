// 스캔·재고 — 바코드 HID 캡처(숨김 input 포커스 + Enter 확정) → 제품조회 → 입출고. i18n(t()).
'use strict';
const $ = (id) => document.getElementById(id);
let current = null;

function focusScan() { const s = $('scanInput'); if (document.activeElement !== $('manualCode')) s.focus(); }
function toast(m) { const t = $('toast'); t.textContent = m; t.style.display='block'; setTimeout(()=>t.style.display='none', 2600); }

async function onScan(code) {
  code = (code || '').trim();
  if (!code) return;
  try {
    const res = await fetch('/api/scan/lookup?code=' + encodeURIComponent(code));
    if (res.status === 404) { renderNotFound(code); return; }
    current = await res.json();
    renderProduct(current);
  } catch (e) { toast(t('toast.lookupErr') + ': ' + e.message); }
}

function renderNotFound(code) {
  current = null;
  $('scanResult').innerHTML = `<div class="muted">${t('scan.notFound')}: <b>${code}</b></div>
    <button class="btn btn-secondary-pill" style="margin-top:8px;" onclick="prefillRegister('${code}')">${t('scan.registerThis')}</button>`;
}

function renderProduct(p) {
  $('scanResult').innerHTML = `
    <div class="prod-card">
      <div class="t-body-strong">${p.name}</div>
      <div class="muted">${p.code} · ${p.unit}</div>
      <div class="qty">${p.quantity}<span class="muted" style="font-size:14px;"> ${p.unit}</span></div>
      ${p.lowStock ? '<div class="badge out">'+t('scan.low')+'</div>' : ''}
      <div class="row2" style="margin-top:12px;">
        <input id="moveQty" type="number" value="1" min="1" style="width:80px;">
        <button class="btn btn-primary" style="flex:1;" onclick="doMove('IN')">${t('scan.in')}</button>
        <button class="btn btn-dark-utility" style="flex:1;" onclick="doMove('OUT')">${t('scan.out')}</button>
      </div>
    </div>`;
}

window.prefillRegister = (code) => { $('npCode').value = code; $('npName').focus(); };

window.doMove = async (type) => {
  if (!current) return;
  const qty = parseInt($('moveQty').value) || 1;
  try {
    const res = await fetch('/api/inventory/move', { method:'POST', headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ code: current.code, type, qty, note: 'scan' }) });
    const data = await res.json();
    if (!res.ok) { toast(data.error || t('toast.moveFail')); return; }
    toast(t('toast.moveDone', [type === 'IN' ? t('scan.in') : t('scan.out'), qty, data.resultQty]));
    await onScan(current.code); await loadProducts(); await loadHistory();
  } catch (e) { toast(t('toast.moveFail') + ': ' + e.message); }
};

async function addProduct() {
  const p = { code: $('npCode').value.trim(), name: $('npName').value.trim(), quantity: parseInt($('npQty').value) || 0 };
  if (!p.code || !p.name) { toast(t('toast.codeNameReq')); return; }
  const res = await fetch('/api/products', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(p) });
  const data = await res.json();
  if (!res.ok) { toast(data.error || t('toast.registerFail')); return; }
  toast(t('toast.registered') + ': ' + data.name);
  $('npCode').value = $('npName').value = ''; $('npQty').value = 0;
  await loadProducts();
}

async function loadProducts() {
  const res = await fetch('/api/products'); if (!res.ok) return;
  const list = await res.json();
  $('prodBody').innerHTML = list.map(p => `
    <tr><td>${p.code}</td><td>${p.name}</td><td>${p.quantity}${p.lowStock ? ' ⚠' : ''}</td>
    <td><a class="text-link" href="#" onclick="onScan('${p.code}');return false;">${t('scan.select')}</a></td></tr>`).join('')
    || `<tr><td colspan="4" class="muted">${t('scan.empty.products')}</td></tr>`;
}
async function loadHistory() {
  const res = await fetch('/api/inventory/history?limit=20'); if (!res.ok) return;
  const list = await res.json();
  $('histBody').innerHTML = list.map(m => `
    <tr><td class="muted">${(m.at || '').replace('T',' ').slice(5,16)}</td><td>${m.code}</td>
    <td><span class="badge ${m.type==='IN'?'in':'out'}">${m.type==='IN'?t('scan.in'):t('scan.out')}</span></td>
    <td>${m.delta > 0 ? '+' : ''}${m.delta}</td><td>${m.resultQty}</td></tr>`).join('')
    || `<tr><td colspan="5" class="muted">${t('scan.empty.history')}</td></tr>`;
}

document.addEventListener('DOMContentLoaded', async () => {
  await loadI18n(document.documentElement.lang || 'ko');
  focusScan();
  const s = $('scanInput');
  s.addEventListener('keydown', (e) => { if (e.key === 'Enter') { onScan(s.value); s.value = ''; } });
  $('manualCode').addEventListener('keydown', (e) => { if (e.key === 'Enter') { onScan($('manualCode').value); $('manualCode').value = ''; } });
  $('btnAddProduct').addEventListener('click', addProduct);
  loadProducts(); loadHistory();
});
window.onScan = onScan; window.focusScan = focusScan;
