/* ---------- everything that shows "now playing" updates the moment the song changes ---------- */
const recentIdx=()=>S.hist.map(idxOfKey).filter(i=>i>=0&&showable(i));
const recentHtml=rc=>rc.length?`<div class="recent r${rc.length}">${rc.map(tile).join("")}</div>`:'<p class="empty" style="margin-bottom:22px">Songs you play will show up here.</p>';
function pushHist(i){const t=T[i];if(!t||!t.key||t.ph)return;S.hist=[t.key,...S.hist.filter(x=>x!==t.key)].slice(0,80);save();updRecent()}
function updRecent(){
  if(S.nav==="recent:all"){const v=$("view"),y=v.scrollTop;render();v.scrollTop=y;return}
  if(S.tab==="Home"&&!S.nav){const c=$("recentBox");if(c){const rc=recentIdx().slice(0,3);S.lists.home=rc.slice();c.innerHTML=recentHtml(rc);bind();sync()}}}
function markNow(){const t=T[S.cur],refs=new Set();
  if(t){if(t.al&&!t.radio)refs.add("album:"+encodeURIComponent(t.al.toLowerCase()));
    new Set([...(t.a||"").split(/,\s*/),t.aa].filter(Boolean)).forEach(n=>refs.add("artist:"+encodeURIComponent(n.toLowerCase())));
    S.pls.forEach(p=>{if(p.keys.includes(t.key))refs.add("pl:"+encodeURIComponent(p.id))});if(S.likes.has(t.key))refs.add("pl:liked")}
  document.querySelectorAll("[data-nav]").forEach(e=>e.classList.toggle("now",refs.has(e.dataset.nav)))}
