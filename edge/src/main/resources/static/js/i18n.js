// 경량 i18n 헬퍼 — /api/i18n/{lang}.json 로드 후 t(key, [args]) 로 조회. 단일 소스=서버 messages.
window.I18N = {};
async function loadI18n(lang) {
  try { window.I18N = await (await fetch('/api/i18n/' + (lang || 'ko') + '.json')).json(); }
  catch (e) { window.I18N = {}; }
}
// t('key') 또는 t('key', [a,b]) — {0},{1} 치환
function t(key, args) {
  let s = (window.I18N && window.I18N[key]) || key;
  if (args) args.forEach((v, i) => { s = s.split('{' + i + '}').join(v); });
  return s;
}
