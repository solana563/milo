/* ===== v8: playback guard (local music keeps playing through stalls, interruptions, file hiccups and app restarts) ===== */
var PG={rec:{},mem:new Map(),warm:new Map(),tk:0,hold:0,still:0,ct:-1,k:-1,seek:null,lp:0};
function pgLocal(t){return !!(t&&t.url&&!t.radio&&!t.stream&&!t.yt&&!t.ph)}
function pgNeeds(t){return !!(t&&t.key&&(t.fh||(t.dev&&NV.cap)))}
function pgMime(n,b){if(b&&b.type)return b.type;const x=(String(n||"").match(/\.([a-z0-9]+)$/i)||[])[1];return({mp3:"audio/mpeg",m4a:"audio/mp4",aac:"audio/aac",flac:"audio/flac",wav:"audio/wav",ogg:"audio/ogg",opus:"audio/ogg",weba:"audio/webm"})[(x||"").toLowerCase()]||"audio/mpeg"}
function pgPut(k,u){const o=PG.mem.get(k);PG.mem.delete(k);PG.mem.set(k,u);if(o&&o!==u)try{URL.revokeObjectURL(o)}catch(e){}
  const keep=new Set([T[S.cur]&&T[S.cur].key,S.pre&&T[S.pre.i]&&T[S.pre.i].key]);
  for(const [kk,uu] of PG.mem){if(PG.mem.size<=3)break;if(keep.has(kk))continue;PG.mem.delete(kk);try{URL.revokeObjectURL(uu)}catch(e){}}}
/* copy the song into memory so the disk, the file route or a changing file can't interrupt it */
function pgWarm(t){if(!pgNeeds(t))return Promise.resolve("");const k=t.key;if(PG.mem.has(k))return Promise.resolve(PG.mem.get(k));
  if(PG.warm.has(k))return PG.warm.get(k);
  const p=(async()=>{let b;
    if(t.fh){const f=await t.fh.getFile();if(f.size>25e7)return"";b=new Blob([await f.arrayBuffer()],{type:pgMime(f.name,f)})}
    else{const r=await fetch(t.url);if(!r.ok)throw new Error("http");const n=+r.headers.get("content-length")||0;if(n>25e7)return"";const x=await r.blob();b=new Blob([x],{type:pgMime(t.url,x)})}
    const u=URL.createObjectURL(b);pgPut(k,u);return u})();
  PG.warm.set(k,p);const done=()=>PG.warm.delete(k);p.then(done,done);return p}
var _pgOffU=offU;offU=function(t){return (t&&t.key&&PG.mem.get(t.key))||_pgOffU(t)};
var _pgEng=engine;
engine=function(i,tog){const t=T[i];
  const run=()=>{_pgEng(i,tog);if(PG.seek&&PG.seek.i===i&&!tog){const p=PG.seek.p;PG.seek=null;const e=A,f=()=>{e.removeEventListener("loadedmetadata",f);try{e.currentTime=p}catch(x){}};e.addEventListener("loadedmetadata",f)}};
  if(!tog&&pgNeeds(t)&&!PG.mem.has(t.key)){const my=++PG.tk;PG.hold=1;try{if(S.playing)A.pause()}catch(e){}let d=0;
    const go=()=>{if(d)return;d=1;if(my!==PG.tk)return;PG.hold=0;if(S.cur===i)run()};
    pgWarm(t).then(go,go);setTimeout(go,900);return}
  PG.tk++;PG.hold=0;run()};
var _pgPre=preload;
preload=function(){const ni=peekNext();if(ni!=null&&T[ni]&&pgNeeds(T[ni])&&!PG.mem.has(T[ni].key)){pgWarm(T[ni]).then(()=>_pgPre(),()=>{});return}return _pgPre()};
/* recover: reload the song from where it stopped, trying another route and a fresh copy */
function pgRecover(){const t=T[S.cur];if(!pgLocal(t)||!S.playing)return false;
  const n=PG.rec[t.key]=(PG.rec[t.key]||0)+1;if(n>5)return false;
  const e=A,pos=e.currentTime||S.pos||0,old=PG.mem.get(t.key);PG.mem.delete(t.key);PG.warm.delete(t.key);
  (async()=>{try{
      if(t.fh){try{const f=await t.fh.getFile();t.url=URL.createObjectURL(f)}catch(x){}}
      else if(t.alt&&n%2===0){const u=t.url;t.url=t.alt;t.alt=u}
      await pgWarm(t).catch(()=>"");
      if(T[S.cur]!==t||A!==e)return;
      e.src=offU(t);e.load();
      if(old)try{URL.revokeObjectURL(old)}catch(x){}
      const f=()=>{e.removeEventListener("loadedmetadata",f);try{e.currentTime=pos}catch(x){}if(S.playing)e.play().catch(()=>{})};e.addEventListener("loadedmetadata",f)}catch(x){}})();
  return true}
DK.forEach(e=>{const o=e.onerror;e.onerror=function(ev){if(e===A&&e.getAttribute("src")&&pgLocal(T[S.cur])&&pgRecover())return;return o&&o.call(this,ev)};
  e.onplaying=(f=>function(ev){if(e===A){const t=T[S.cur];if(t)PG.rec[t.key]=0}return f&&f.call(this,ev)})(e.onplaying)});
/* watchdog: audio engine suspended, element paused by the system, or frozen output */
function pgTick(force){if(!S.playing||S.cur<0||PG.hold)return;const t=T[S.cur];if(!pgLocal(t))return;const e=A;
  if(S.ctx){if(!S.ctx.__pg){S.ctx.__pg=1;S.ctx.onstatechange=()=>{if(S.playing&&S.ctx.state!=="running")S.ctx.resume().catch(()=>{})}}
    if(S.ctx.state!=="running")S.ctx.resume().catch(()=>{})}
  if(Date.now()<S.xfb||e.ended||e.seeking||!e.getAttribute("src"))return;
  if(e.paused){PG.still=0;if(e.readyState>0&&(force||Date.now()-PG.lp>5000)){PG.lp=Date.now();e.play().catch(()=>{})}return}
  const ct=e.currentTime;
  if(PG.k===S.cur&&Math.abs(ct-PG.ct)<.05&&e.readyState<4){if(++PG.still>=3){PG.still=0;pgRecover()}}else PG.still=0;
  PG.ct=ct;PG.k=S.cur}
setInterval(pgTick,1500);
["pageshow","focus","online","resume"].forEach(n=>addEventListener(n,()=>setTimeout(()=>pgTick(true),300)));
document.addEventListener("visibilitychange",()=>{if(!document.hidden)setTimeout(()=>pgTick(true),300)});
/* remember what was playing so an update, a crash or a closed app never loses your place */
function pgSave(){try{if(S.cur<0)return;const t=T[S.cur];if(!t||!t.key||t.radio||t.stream||t.yt||t.ph)return;
  LS.set("milo.np",{k:t.key,p:Math.floor(A.currentTime||S.pos||0),q:S.q.slice(0,300).map(i=>T[i]&&T[i].key).filter(Boolean)})}catch(e){}}
setInterval(()=>{if(S.playing)pgSave()},4000);addEventListener("pagehide",pgSave);
document.addEventListener("visibilitychange",()=>{if(document.hidden)pgSave()});
function pgRestore(){if(S.cur>=0)return true;const r=LS.get("milo.np",null);if(!r||!r.k)return true;
  const f=k=>T.findIndex(t=>t.key===k&&!t.gone&&!t.hid),i=f(r.k);if(i<0||!pgLocal(T[i]))return false;
  const q=(r.q||[]).map(f).filter(x=>x>=0);if(!q.includes(i))q.unshift(i);
  S.base=q.slice();S.q=q.slice();S.qi=q.indexOf(i);S.cur=i;S.pos=r.p||0;S.playing=false;PG.seek={i,p:r.p||0};engine(i);sync();return true}
(function(){let n=0;const iv=setInterval(()=>{if(++n>10||pgRestore())clearInterval(iv)},1500)})();

