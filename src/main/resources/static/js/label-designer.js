// 라벨 디자이너 — 요소 배치 → ZPL 생성 → Labelary 미리보기 → 실제 인쇄 왕복.
// ZPL 원문(#zplBody)이 미리보기/인쇄의 단일 기준. 요소 테이블은 ZPL 생성을 돕는 보조 도구.

let currentTemplateId = null;

// ── 유틸 ─────────────────────────────────────────────
function dpmm(dpi) { return Number(dpi) >= 300 ? 12 : 8; }
function mmToDots(mm, dpi) { return Math.round(Number(mm) * dpmm(dpi)); }

function el(id) { return document.getElementById(id); }

// {{key}} → value 치환 (서버 LabelTemplateService.bindVariables 와 동일 규약)
function bindVariables(zpl, vars) {
    if (!zpl) return '';
    let out = zpl;
    Object.entries(vars || {}).forEach(([k, v]) => {
        out = out.split('{{' + k + '}}').join(v == null ? '' : v);
    });
    return out;
}

// ── 요소 테이블 ─────────────────────────────────────────────
const ELEMENT_TYPES = {
    text:    { label: '텍스트',   size: 28,  ph: '표시할 텍스트 또는 {{name}}' },
    qr:      { label: 'QR',       size: 6,   ph: 'QR 데이터 또는 {{code}}' },
    barcode: { label: '바코드',    size: 80,  ph: 'Code128 값 또는 {{code}}' },
    box:     { label: '박스/선',   size: 2,   ph: '폭,높이 (dots) 예: 280,2' },
};

function addElementRow(preset) {
    const p = preset || {};
    const tr = document.createElement('tr');
    tr.className = 'element-row';
    const typeOpts = Object.entries(ELEMENT_TYPES)
        .map(([k, v]) => `<option value="${k}" ${p.type === k ? 'selected' : ''}>${v.label}</option>`).join('');
    tr.innerHTML = `
        <td><select class="form-select form-select-sm el-type">${typeOpts}</select></td>
        <td><input type="number" class="form-control form-control-sm el-x" value="${p.x ?? 2}"></td>
        <td><input type="number" class="form-control form-control-sm el-y" value="${p.y ?? 2}"></td>
        <td><input type="number" class="form-control form-control-sm el-size" value="${p.size ?? ''}"></td>
        <td><input type="text" class="form-control form-control-sm el-value" value="${p.value ?? ''}"></td>
        <td><button type="button" class="btn btn-sm btn-link text-danger el-del"><i class="mdi mdi-close"></i></button></td>
    `;
    el('elementBody').appendChild(tr);

    const typeSel = tr.querySelector('.el-type');
    const sizeInp = tr.querySelector('.el-size');
    const valInp = tr.querySelector('.el-value');
    const applyType = () => {
        const def = ELEMENT_TYPES[typeSel.value];
        if (!sizeInp.value) sizeInp.value = def.size;
        valInp.placeholder = def.ph;
    };
    applyType();
    typeSel.addEventListener('change', () => { sizeInp.value = ELEMENT_TYPES[typeSel.value].size; applyType(); });
    tr.querySelector('.el-del').addEventListener('click', () => tr.remove());
}

function collectElements() {
    return Array.from(document.querySelectorAll('#elementBody .element-row')).map(tr => ({
        type: tr.querySelector('.el-type').value,
        x: Number(tr.querySelector('.el-x').value) || 0,
        y: Number(tr.querySelector('.el-y').value) || 0,
        size: tr.querySelector('.el-size').value,
        value: tr.querySelector('.el-value').value,
    }));
}

// 요소 → ZPL 본문 생성
function generateZpl() {
    const dpi = el('dpi').value;
    const pw = mmToDots(el('widthMm').value, dpi);
    const ll = mmToDots(el('heightMm').value, dpi);
    const lines = ['^XA', `^PW${pw}`, `^LL${ll}`];
    collectElements().forEach(e => {
        const x = mmToDots(e.x, dpi);
        const y = mmToDots(e.y, dpi);
        const size = e.size;
        const v = e.value;
        switch (e.type) {
            case 'text':
                lines.push(`^FO${x},${y}^A0N,${size},${size}^FD${v}^FS`);
                break;
            case 'qr':
                lines.push(`^FO${x},${y}^BQN,2,${size}^FDLA,${v}^FS`);
                break;
            case 'barcode':
                lines.push(`^FO${x},${y}^BCN,${size},Y,N,N^FD${v}^FS`);
                break;
            case 'box':
                // value = "폭,높이[,두께]" (dots)
                const parts = String(v || '').split(',').map(s => s.trim());
                const w = parts[0] || '100';
                const h = parts[1] || '2';
                const t = parts[2] || size || '2';
                lines.push(`^FO${x},${y}^GB${w},${h},${t}^FS`);
                break;
        }
    });
    lines.push('^XZ');
    el('zplBody').value = lines.join('\n');
    refreshVariables();
}

// ── 변수 감지/입력 ─────────────────────────────────────────────
function detectVariables(zpl) {
    const set = new Set();
    const re = /\{\{\s*([\w.-]+)\s*\}\}/g;
    let m;
    while ((m = re.exec(zpl || '')) !== null) set.add(m[1]);
    return Array.from(set);
}

function refreshVariables() {
    const vars = detectVariables(el('zplBody').value);
    const box = el('variableInputs');
    const prev = collectVariables();
    if (vars.length === 0) {
        box.innerHTML = '<div class="col-12 text-muted small">감지된 변수가 없습니다.</div>';
        return;
    }
    box.innerHTML = vars.map(v => `
        <div class="col-6">
            <label class="form-label small mb-1">${v}</label>
            <input type="text" class="form-control form-control-sm var-input" data-key="${v}" value="${prev[v] || ''}">
        </div>`).join('');
}

function collectVariables() {
    const vars = {};
    document.querySelectorAll('#variableInputs .var-input').forEach(i => { vars[i.dataset.key] = i.value; });
    return vars;
}

function updateDotsHint() {
    const dpi = el('dpi').value;
    el('dotsHint').textContent =
        `${mmToDots(el('widthMm').value, dpi)} x ${mmToDots(el('heightMm').value, dpi)} dots`;
}

// ── 미리보기 ─────────────────────────────────────────────
async function doPreview() {
    const zpl = bindVariables(el('zplBody').value, collectVariables());
    try {
        showLoading();
        const res = await fetch('/api/labels/preview', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                zpl,
                dpi: Number(el('dpi').value),
                widthMm: Number(el('widthMm').value),
                heightMm: Number(el('heightMm').value),
            }),
        });
        hideLoading();
        if (!res.ok) { showToast('error', '미리보기 실패', 'Labelary 렌더 오류 (' + res.status + '). ZPL 문법 확인.'); return; }
        const blob = await res.blob();
        const img = el('previewImg');
        if (img.dataset.url) URL.revokeObjectURL(img.dataset.url);
        const url = URL.createObjectURL(blob);
        img.dataset.url = url;
        img.src = url;
        img.style.display = 'block';
        el('previewPlaceholder').style.display = 'none';
    } catch (e) {
        hideLoading();
        showToast('error', '미리보기 실패', e.message);
    }
}

// ── 인쇄 (변수 치환 후 raw ZPL 전송) ─────────────────────────────────────────────
async function doPrint() {
    let zpl = bindVariables(el('zplBody').value, collectVariables());
    const copies = Math.max(1, Number(el('copies').value) || 1);
    if (copies > 1 && zpl.includes('^XZ')) {
        const idx = zpl.lastIndexOf('^XZ');
        zpl = zpl.slice(0, idx) + `^PQ${copies}\n` + zpl.slice(idx);
    }
    try {
        showLoading();
        const res = await fetch('/api/print/zpl', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ zpl }),
        });
        const msg = await res.text();
        hideLoading();
        if (!res.ok) { showToast('error', '인쇄 실패', msg); return; }
        showToast('success', '인쇄', msg);
    } catch (e) {
        hideLoading();
        showToast('error', '인쇄 실패', e.message);
    }
}

// ── 템플릿 CRUD ─────────────────────────────────────────────
function currentTemplatePayload() {
    return {
        name: el('tplName').value || '이름없는 템플릿',
        widthMm: Number(el('widthMm').value) || null,
        heightMm: Number(el('heightMm').value) || null,
        dpi: Number(el('dpi').value) || null,
        zplBody: el('zplBody').value,
        elementsJson: JSON.stringify(collectElements()),
    };
}

async function loadTemplateList() {
    try {
        const res = await fetch('/api/labels/templates');
        if (!res.ok) return;
        const list = await res.json();
        const sel = el('templateSelect');
        sel.innerHTML = '<option value="">저장된 템플릿 불러오기…</option>'
            + list.map(t => `<option value="${t.id}">${t.name}</option>`).join('');
    } catch (_) { /* 목록 로드 실패는 조용히 무시 */ }
}

async function loadTemplate(id) {
    if (!id) return;
    try {
        showLoading();
        const res = await fetch('/api/labels/templates/' + id);
        hideLoading();
        if (!res.ok) { showToast('error', '불러오기 실패', res.status); return; }
        const t = await res.json();
        currentTemplateId = t.id;
        el('tplName').value = t.name || '';
        if (t.widthMm) el('widthMm').value = t.widthMm;
        if (t.heightMm) el('heightMm').value = t.heightMm;
        if (t.dpi) el('dpi').value = t.dpi;
        el('zplBody').value = t.zplBody || '';
        el('elementBody').innerHTML = '';
        if (t.elementsJson) {
            try { JSON.parse(t.elementsJson).forEach(addElementRow); } catch (_) { /* 무시 */ }
        }
        el('btnDelete').disabled = false;
        updateDotsHint();
        refreshVariables();
        showToast('success', '템플릿', `'${t.name}' 불러왔습니다.`);
    } catch (e) {
        hideLoading();
        showToast('error', '불러오기 실패', e.message);
    }
}

async function saveTemplate() {
    const payload = currentTemplatePayload();
    const method = currentTemplateId ? 'PUT' : 'POST';
    const url = currentTemplateId ? '/api/labels/templates/' + currentTemplateId : '/api/labels/templates';
    try {
        showLoading();
        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        hideLoading();
        if (!res.ok) { showToast('error', '저장 실패', res.status); return; }
        const t = await res.json();
        currentTemplateId = t.id;
        el('btnDelete').disabled = false;
        await loadTemplateList();
        el('templateSelect').value = t.id;
        showToast('success', '템플릿', `'${t.name}' 저장되었습니다.`);
    } catch (e) {
        hideLoading();
        showToast('error', '저장 실패', e.message);
    }
}

async function deleteTemplate() {
    if (!currentTemplateId) return;
    if (!confirm('이 템플릿을 삭제할까요?')) return;
    try {
        showLoading();
        const res = await fetch('/api/labels/templates/' + currentTemplateId, { method: 'DELETE' });
        hideLoading();
        if (!res.ok) { showToast('error', '삭제 실패', res.status); return; }
        currentTemplateId = null;
        el('btnDelete').disabled = true;
        await loadTemplateList();
        el('templateSelect').value = '';
        showToast('success', '템플릿', '삭제되었습니다.');
    } catch (e) {
        hideLoading();
        showToast('error', '삭제 실패', e.message);
    }
}

// ── 초기화 ─────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    // 기본 예시 요소 2개
    addElementRow({ type: 'text', x: 2, y: 2, size: 28, value: '{{name}}' });
    addElementRow({ type: 'qr', x: 2, y: 8, size: 6, value: '{{code}}' });

    el('addElement').addEventListener('click', () => addElementRow());
    el('genZpl').addEventListener('click', generateZpl);
    el('btnPreview').addEventListener('click', doPreview);
    el('btnPrint').addEventListener('click', doPrint);
    el('btnSave').addEventListener('click', saveTemplate);
    el('btnDelete').addEventListener('click', deleteTemplate);
    el('zplBody').addEventListener('input', refreshVariables);
    ['widthMm', 'heightMm', 'dpi'].forEach(id => el(id).addEventListener('input', updateDotsHint));
    el('templateSelect').addEventListener('change', e => loadTemplate(e.target.value));

    updateDotsHint();
    refreshVariables();
    loadTemplateList();
});
