// 라벨 디자이너 (캔버스 드래그) — 요소 배치/이동/리사이즈 → elementsJson → 서버 래스터 라이브 프리뷰.
'use strict';

const $ = (id) => document.getElementById(id);
let currentId = null;
let SCALE = 9;                 // canvas px per mm
let elements = [];             // {type,xMm,yMm,value,sizeMm,widthMm,heightMm,bold}
let selected = -1;
let drag = null;               // {mode:'move'|'resize', ox, oy, sx, sy}
let previewTimer = null;

const DEFAULTS = {
  TEXT:    () => ({ type:'TEXT',    xMm:1.5, yMm:2,   value:'텍스트 {{name}}', sizeMm:2.2, bold:true }),
  QR:      () => ({ type:'QR',      xMm:21.5,yMm:2,   value:'{{code}}', sizeMm:17 }),
  BARCODE: () => ({ type:'BARCODE', xMm:1.5, yMm:9.5, value:'{{code}}', sizeMm:6.5, widthMm:18 }),
  BOX:     () => ({ type:'BOX',     xMm:1.5, yMm:2,   widthMm:14, heightMm:1, value:'' })
};

// ── 좌표/치수 ──────────────────────────────
function labelW() { return parseFloat($('widthMm').value) || 50; }
function labelH() { return parseFloat($('heightMm').value) || 25; }
function recomputeScale() {
  SCALE = Math.max(4, Math.min(12, Math.floor(480 / labelW())));
  const c = $('canvas');
  c.width = Math.round(labelW() * SCALE);
  c.height = Math.round(labelH() * SCALE);
}
const mm = (px) => Math.round((px / SCALE) * 2) / 2;   // px→mm, 0.5 반올림
const px = (v) => v * SCALE;

// 요소의 mm 바운딩박스(히트테스트/그리기용 근사)
function bbox(e) {
  switch (e.type) {
    case 'TEXT':    return { w: Math.max(4, (e.value || '').length * (e.sizeMm || 4) * 0.62), h: e.sizeMm || 4 };
    case 'QR':      return { w: e.sizeMm || 14, h: e.sizeMm || 14 };
    case 'BARCODE': return { w: e.widthMm || 26, h: e.sizeMm || 8 };
    case 'BOX':     return { w: e.widthMm || 10, h: e.heightMm || 2 };
  }
  return { w: 10, h: 10 };
}

// ── 그리기 ──────────────────────────────
const TYPE_COLOR = { TEXT:'#0066cc', QR:'#1d1d1f', BARCODE:'#7a3fcc', BOX:'#c0392b' };
function draw() {
  const c = $('canvas'), g = c.getContext('2d');
  g.clearRect(0, 0, c.width, c.height);
  g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height);
  g.strokeStyle = '#e0e0e0'; g.strokeRect(0.5, 0.5, c.width - 1, c.height - 1);

  elements.forEach((e, i) => {
    const b = bbox(e);
    const x = px(e.xMm), y = px(e.yMm), w = px(b.w), h = px(b.h);
    const col = TYPE_COLOR[e.type] || '#333';
    g.save();
    g.fillStyle = col + '18';
    g.fillRect(x, y, w, h);
    g.strokeStyle = col;
    g.lineWidth = i === selected ? 2 : 1;
    g.setLineDash(i === selected ? [] : [4, 3]);
    g.strokeRect(x, y, w, h);
    // 라벨
    g.setLineDash([]);
    g.fillStyle = col;
    g.font = '10px Inter, sans-serif';
    const cap = e.type + (e.value ? ' · ' + e.value : '');
    g.fillText(cap.slice(0, 22), x + 3, y + 11);
    // 리사이즈 핸들
    if (i === selected) {
      g.fillStyle = col;
      g.fillRect(x + w - 6, y + h - 6, 6, 6);
    }
    g.restore();
  });
}

// ── 히트테스트 ──────────────────────────────
function hit(mx, my) {
  for (let i = elements.length - 1; i >= 0; i--) {
    const e = elements[i], b = bbox(e);
    const x = px(e.xMm), y = px(e.yMm), w = px(b.w), h = px(b.h);
    if (mx >= x && mx <= x + w && my >= y && my <= y + h) return i;
  }
  return -1;
}
function inHandle(mx, my, i) {
  const e = elements[i], b = bbox(e);
  const hx = px(e.xMm) + px(b.w) - 6, hy = px(e.yMm) + px(b.h) - 6;
  return mx >= hx - 3 && mx <= hx + 9 && my >= hy - 3 && my <= hy + 9;
}

// ── 마우스 ──────────────────────────────
function canvasPos(ev) {
  const r = $('canvas').getBoundingClientRect();
  return { x: ev.clientX - r.left, y: ev.clientY - r.top };
}
function onDown(ev) {
  const p = canvasPos(ev);
  if (selected >= 0 && inHandle(p.x, p.y, selected)) {
    drag = { mode: 'resize' };
  } else {
    const i = hit(p.x, p.y);
    selected = i;
    renderProps();
    if (i >= 0) {
      const e = elements[i];
      drag = { mode: 'move', ox: p.x - px(e.xMm), oy: p.y - px(e.yMm) };
    }
  }
  draw();
}
function onMove(ev) {
  if (!drag || selected < 0) return;
  const p = canvasPos(ev), e = elements[selected];
  if (drag.mode === 'move') {
    e.xMm = Math.max(0, mm(p.x - drag.ox));
    e.yMm = Math.max(0, mm(p.y - drag.oy));
  } else {
    const wMm = Math.max(2, mm(p.x - px(e.xMm)));
    const hMm = Math.max(1, mm(p.y - px(e.yMm)));
    if (e.type === 'QR') { e.sizeMm = Math.max(wMm, hMm); }
    else if (e.type === 'TEXT') { e.sizeMm = hMm; }
    else if (e.type === 'BARCODE') { e.widthMm = wMm; e.sizeMm = hMm; }
    else if (e.type === 'BOX') { e.widthMm = wMm; e.heightMm = hMm; }
  }
  draw(); renderProps(true); syncJson();
}
function onUp() { if (drag) { drag = null; schedulePreview(); } }

// ── 속성 패널 ──────────────────────────────
function renderProps(valuesOnly) {
  const box = $('props');
  if (selected < 0) { box.innerHTML = '<div class="prop-empty">요소를 선택하세요.</div>'; return; }
  const e = elements[selected];
  if (valuesOnly && box.dataset.for === String(selected)) {
    // 드래그 중엔 좌표/크기만 갱신
    const set = (id, v) => { const el = box.querySelector('[data-k="' + id + '"]'); if (el && document.activeElement !== el) el.value = v; };
    set('xMm', e.xMm); set('yMm', e.yMm); set('sizeMm', e.sizeMm ?? '');
    set('widthMm', e.widthMm ?? ''); set('heightMm', e.heightMm ?? '');
    return;
  }
  box.dataset.for = String(selected);
  let html = `<label>종류</label><input value="${e.type}" disabled>`;
  if (e.type !== 'BOX') html += `<label>값 (({{키}}) 가능)</label><input data-k="value" value="${escapeAttr(e.value || '')}">`;
  html += `<div class="size-row"><div><label>x(mm)</label><input data-k="xMm" type="number" step="0.5" value="${e.xMm}"></div>
           <div><label>y(mm)</label><input data-k="yMm" type="number" step="0.5" value="${e.yMm}"></div></div>`;
  if (e.type === 'TEXT') {
    html += `<label>글자 높이(mm)</label><input data-k="sizeMm" type="number" step="0.5" value="${e.sizeMm}">
             <label><input data-k="bold" type="checkbox" ${e.bold ? 'checked' : ''} style="width:auto;margin-right:6px;">굵게</label>`;
  } else if (e.type === 'QR') {
    html += `<label>한 변(mm)</label><input data-k="sizeMm" type="number" step="0.5" value="${e.sizeMm}">`;
  } else if (e.type === 'BARCODE') {
    html += `<div class="size-row"><div><label>폭(mm)</label><input data-k="widthMm" type="number" step="0.5" value="${e.widthMm}"></div>
             <div><label>높이(mm)</label><input data-k="sizeMm" type="number" step="0.5" value="${e.sizeMm}"></div></div>`;
  } else if (e.type === 'BOX') {
    html += `<div class="size-row"><div><label>폭(mm)</label><input data-k="widthMm" type="number" step="0.5" value="${e.widthMm}"></div>
             <div><label>높이(mm)</label><input data-k="heightMm" type="number" step="0.5" value="${e.heightMm}"></div></div>`;
  }
  box.innerHTML = html;
  box.querySelectorAll('[data-k]').forEach(inp => {
    inp.addEventListener('input', () => {
      const k = inp.dataset.k;
      const e2 = elements[selected];
      if (k === 'value') e2.value = inp.value;
      else if (k === 'bold') e2.bold = inp.checked;
      else e2[k] = parseFloat(inp.value) || 0;
      draw(); syncJson(); schedulePreview();
      if (k === 'value') refreshVariables();
    });
  });
}
function escapeAttr(s) { return String(s).replace(/"/g, '&quot;'); }

// ── elementsJson 동기화 ──────────────────────────────
function syncJson() { $('elementsJson').value = JSON.stringify(elements, null, 2); }
function applyJson() {
  try { elements = JSON.parse($('elementsJson').value) || []; selected = -1; draw(); renderProps(); refreshVariables(); schedulePreview(); }
  catch (e) { toast('JSON 파싱 오류'); }
}

// ── 변수 ──────────────────────────────
function detectVariables() {
  const set = new Set(); const re = /\{\{\s*([\w.-]+)\s*\}\}/g;
  elements.forEach(e => { let m; const s = e.value || ''; while ((m = re.exec(s)) !== null) set.add(m[1]); });
  return [...set];
}
function refreshVariables() {
  const vars = detectVariables(), box = $('varInputs'), prev = collectVariables();
  if (!vars.length) { box.innerHTML = '<span class="muted">{{키}} 감지 시 입력란 생성</span>'; return; }
  box.innerHTML = vars.map(v => `<div><label>${v}</label><input class="var-input" data-key="${v}" value="${prev[v] || ''}"></div>`).join('');
  box.querySelectorAll('.var-input').forEach(i => i.addEventListener('input', schedulePreview));
}
function collectVariables() { const o = {}; document.querySelectorAll('.var-input').forEach(i => o[i.dataset.key] = i.value); return o; }

// ── 요청 ──────────────────────────────
function req() {
  return { id: currentId, name: $('name').value, widthMm: labelW(), heightMm: labelH(),
           dpi: parseInt($('dpi').value) || 203, elementsJson: JSON.stringify(elements),
           variables: collectVariables(), copies: parseInt($('copies').value) || 1 };
}

function schedulePreview() { clearTimeout(previewTimer); previewTimer = setTimeout(doPreview, 350); }
async function doPreview() {
  try {
    const res = await fetch('/api/labels/preview', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(req()) });
    if (!res.ok) { $('previewPh').textContent = '미리보기 실패'; $('previewPh').style.display='block'; return; }
    const blob = await res.blob(), img = $('previewImg');
    if (img.dataset.url) URL.revokeObjectURL(img.dataset.url);
    const url = URL.createObjectURL(blob); img.dataset.url = url; img.src = url; img.style.display='block';
    $('previewPh').style.display='none';
  } catch (e) { /* noop */ }
}

async function doPrint() {
  try { const res = await fetch('/api/labels/print', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(req()) });
    const msg = await res.text(); toast(res.ok ? msg : ('인쇄 실패: ' + msg)); }
  catch (e) { toast('인쇄 오류: ' + e.message); }
}

// ── 템플릿 CRUD ──────────────────────────────
function payload() { return { name: $('name').value || '이름없는 템플릿', widthMm: labelW(), heightMm: labelH(), dpi: parseInt($('dpi').value)||203, elementsJson: JSON.stringify(elements) }; }
async function loadList() {
  const res = await fetch('/api/labels/templates'); if (!res.ok) return;
  const list = await res.json();
  $('tplSelect').innerHTML = '<option value="">저장된 템플릿…</option>' + list.map(t => `<option value="${t.id}">${t.name}</option>`).join('');
}
async function loadTemplate(id) {
  if (!id) return;
  const res = await fetch('/api/labels/templates/' + id); if (!res.ok) { toast('불러오기 실패'); return; }
  const t = await res.json(); currentId = t.id;
  $('name').value = t.name || ''; $('widthMm').value = t.widthMm; $('heightMm').value = t.heightMm; if (t.dpi) $('dpi').value = t.dpi;
  try { elements = JSON.parse(t.elementsJson || '[]'); } catch (_) { elements = []; }
  selected = -1; $('btnDelete').disabled = false;
  recomputeScale(); draw(); renderProps(); syncJson(); refreshVariables(); doPreview();
}
async function saveTemplate() {
  const method = currentId ? 'PUT' : 'POST';
  const url = currentId ? '/api/labels/templates/' + currentId : '/api/labels/templates';
  const res = await fetch(url, { method, headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload()) });
  if (!res.ok) { toast('저장 실패'); return; }
  const t = await res.json(); currentId = t.id; $('btnDelete').disabled = false;
  await loadList(); $('tplSelect').value = t.id; toast("'" + t.name + "' 저장됨");
}
async function deleteTemplate() {
  if (!currentId || !confirm('삭제할까요?')) return;
  const res = await fetch('/api/labels/templates/' + currentId, { method:'DELETE' });
  if (!res.ok) { toast('삭제 실패'); return; }
  currentId = null; $('btnDelete').disabled = true; $('tplSelect').value = ''; await loadList(); toast('삭제됨');
}

function toast(m) { const t = $('toast'); t.textContent = m; t.style.display='block'; setTimeout(()=>t.style.display='none', 2600); }

// ── 초기화 ──────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  elements = [ { type:'TEXT', xMm:1.5, yMm:2, value:'한글 {{name}}', sizeMm:2.2, bold:true }, { type:'TEXT', xMm:1.5, yMm:5.5, value:'{{code}}', sizeMm:2.2 }, DEFAULTS.BARCODE(), DEFAULTS.QR() ];
  recomputeScale(); draw(); syncJson(); refreshVariables();

  const c = $('canvas');
  c.addEventListener('mousedown', onDown);
  window.addEventListener('mousemove', onMove);
  window.addEventListener('mouseup', onUp);

  document.querySelectorAll('[data-add]').forEach(btn => btn.addEventListener('click', () => {
    elements.push(DEFAULTS[btn.dataset.add]()); selected = elements.length - 1;
    draw(); renderProps(); syncJson(); refreshVariables(); schedulePreview();
  }));
  $('btnDelSel').addEventListener('click', delSel);
  window.addEventListener('keydown', (e) => { if ((e.key === 'Delete' || e.key === 'Backspace') && selected >= 0 && document.activeElement === document.body) delSel(); });
  function delSel() { if (selected < 0) return; elements.splice(selected, 1); selected = -1; draw(); renderProps(); syncJson(); refreshVariables(); schedulePreview(); }

  ['widthMm','heightMm','dpi'].forEach(id => $(id).addEventListener('input', () => { recomputeScale(); draw(); schedulePreview(); }));
  $('btnApplyJson').addEventListener('click', applyJson);
  $('btnPrint').addEventListener('click', doPrint);
  $('btnPrint2').addEventListener('click', doPrint);
  $('btnSave').addEventListener('click', saveTemplate);
  $('btnDelete').addEventListener('click', deleteTemplate);
  $('tplSelect').addEventListener('change', e => loadTemplate(e.target.value));

  loadList(); doPreview();
});
