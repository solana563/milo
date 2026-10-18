/* ----- deep link: #dna=... opens a shared song ----- */
function dnaFromHash(){const m=location.hash.match(/dna=([A-Za-z0-9+/=_-]+)/);if(!m)return;let d;try{d=JSON.parse(decodeURIComponent(escape(atob(m[1].replace(/-/g,"+").replace(/_/g,"/")))))}catch(e){return}
  history.replaceState(null,"",location.pathname+location.search);if(!d||!d.id||!d.t)return;
  setTimeout(()=>modal(`<h3>Play "${esc(d.t)}"?</h3><p>${esc(d.a||"")}. A friend shared this song with you. It streams from its source, nothing is downloaded.</p><button class="addb" data-a="dnap:">Play</button>`),600);S.dnaIn=d}
dnaFromHash();
