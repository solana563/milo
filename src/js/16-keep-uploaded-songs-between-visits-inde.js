/* ---------- keep uploaded songs between visits (IndexedDB) ---------- */
const IMGB={};
let dbP=null;
function idb(){if(dbP)return dbP;
  dbP=new Promise((res,rej)=>{try{const r=indexedDB.open("milo-lib",2);
    r.onupgradeneeded=()=>{const d=r.result;if(!d.objectStoreNames.contains("songs"))d.createObjectStore("songs",{keyPath:"key"});if(!d.objectStoreNames.contains("lrc"))d.createObjectStore("lrc",{keyPath:"base"});if(!d.objectStoreNames.contains("kv"))d.createObjectStore("kv",{keyPath:"k"})};
    r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error);r.onblocked=()=>rej(new Error("blocked"))}catch(e){rej(e)}});
  dbP.catch(()=>{dbP=null});return dbP}
const rq=r=>new Promise((res,rej)=>{r.onsuccess=()=>res(r.result);r.onerror=()=>rej(r.error)});
const txDone=tx=>new Promise((res,rej)=>{tx.oncomplete=res;tx.onerror=()=>rej(tx.error);tx.onabort=()=>rej(tx.error)});
function rec(t){return{key:t.key,blob:t.file,added:t.added||(t.added=Date.now()),cover:IMGB[t.img]||null,
  m:{t:t.t,a:t.a,al:t.al,y:t.y,g:t.g,n:t.n,d:t.d,c:t.c,base:t.base,fname:t.fname,lrc:t.lrc||null,plain:t.plain||null,lsrc:t.lsrc||"",imgUrl:t.img&&!String(t.img).startsWith("blob:")?t.img:""}}}
async function idbPut(t){try{const db=await idb(),tx=db.transaction("songs","readwrite");tx.objectStore("songs").put(rec(t));await txDone(tx);
    try{if(navigator.storage&&navigator.storage.persist)navigator.storage.persist().catch(()=>{})}catch(e){}}
  catch(e){if(!S.saveErr){S.saveErr=1;toast(e&&e.name==="QuotaExceededError"?"This device is out of space, so this song won't be kept. Free some up in Profile > Storage.":"Couldn't keep this song for next time. Storage may be full or blocked.")}}}
function saveSoon(t){if(!t||!t.file)return;clearTimeout(t._st);t._st=setTimeout(()=>idbPut(t),500)}
async function idbDel(key){try{const db=await idb(),tx=db.transaction("songs","readwrite");tx.objectStore("songs").delete(key);await txDone(tx)}catch(e){}}
async function idbLrc(base,lines){try{const db=await idb(),tx=db.transaction("lrc","readwrite");tx.objectStore("lrc").put({base,lines});await txDone(tx)}catch(e){}}
async function restoreLibrary(){
  try{const db=await idb();
    const [songs,lrcs]=await Promise.all([rq(db.transaction("songs").objectStore("songs").getAll()),rq(db.transaction("lrc").objectStore("lrc").getAll())]);
    lrcs.forEach(x=>{S.lrc[x.base]=x.lines});
    songs.sort((a,b)=>(a.added||0)-(b.added||0));
    for(const r of songs){if(!r.blob||T.some(t=>t.key===r.key&&!t.gone))continue;const m=r.m||{};
      const t={t:m.t||"Untitled",a:m.a||"On this device",al:m.al||"",y:m.y||"",g:m.g||"",n:m.n||0,d:m.d||0,c:m.c||"84,120,140",url:URL.createObjectURL(r.blob),key:r.key,base:m.base||"",fname:m.fname||"",lrc:m.lrc||undefined,plain:m.plain||undefined,lsrc:m.lsrc||"",file:r.blob,added:r.added};
      if(r.cover){t.img=URL.createObjectURL(r.cover);IMGB[t.img]=r.cover}else if(m.imgUrl)t.img=m.imgUrl;
      T.push(t);
      if(!t.d){const a=new Audio(),idx=T.length-1;a.preload="metadata";a.src=t.url;
        a.onloadedmetadata=()=>{if(isFinite(a.duration)){t.d=a.duration;const e=document.querySelector('.li[data-i="'+idx+'"] em');if(e)e.textContent=fmt(t.d);saveSoon(t)}}}}
    T.forEach(t=>{if(t.file&&!t.lrc&&S.lrc[t.base]){t.lrc=S.lrc[t.base];t.lsrc=".lrc file"}});
    if(songs.length){render();if(S.cur>=0)lyrRender()}}
  catch(e){}}
function removeSong(i){const t=T[i];if(t.dev){DVRM.add(t.key);LS.set("milo.dvrm",[...DVRM])}t.gone=true;t.hid=true;idbDel(t.key);if(S.cur!==i&&t.url){try{URL.revokeObjectURL(t.url)}catch(e){}}toast("Removed from your library");render()}
async function clearLibrary(){T.forEach((t,i)=>{if(t.file&&!t.gone){t.gone=true;t.hid=true;if(S.cur!==i){try{URL.revokeObjectURL(t.url)}catch(e){}}}});S.lrc={};
  try{const db=await idb(),tx=db.transaction(["songs","lrc"],"readwrite");tx.objectStore("songs").clear();tx.objectStore("lrc").clear();await txDone(tx)}catch(e){}
  toast("Uploaded songs removed");render()}
function libStats(){const a=T.filter(t=>t.file&&!t.gone),b=a.reduce((n,t)=>n+(t.file.size||0),0);return{n:a.length,mb:b/1048576}}
function stgHtml(){const s=libStats();
  return `<div class="row" style="margin-top:24px"><h2>Your music storage</h2></div><p class="empty" style="padding-top:0">${s.n?`${s.n} uploaded song${s.n===1?"":"s"} · ${s.mb<10?s.mb.toFixed(1):Math.round(s.mb)} MB kept on this device, so they're here next time you open the app.`:"Songs you add are kept on this device, so they're here next time you open the app."}</p>${s.n?'<button class="ghost" data-clr>Remove all uploaded songs</button>':""}`}

(function(){const f=()=>document.documentElement.style.setProperty("--H",innerHeight+"px");f();addEventListener("resize",f);addEventListener("orientationchange",()=>setTimeout(f,150))})();
/* glow behind the cover: the song's own art shrunk to 16px and stretched back up, so it's naturally soft */
function glowSet(t){const a=$("amb");if(!a)return;
  if(t.glowFor!==t.img){t.glow=null;t.lum=null;t.glowFor=t.img;
    if(t.img){const im=new Image();im.crossOrigin="anonymous";
      im.onload=()=>{try{const c=document.createElement("canvas");c.width=c.height=16;const x=c.getContext("2d");try{x.filter="saturate(1.6)"}catch(e){}x.drawImage(im,0,0,16,16);try{const dt=x.getImageData(0,0,16,16).data;let sm=0;for(let i=0;i<dt.length;i+=4)sm+=.2126*dt[i]+.7152*dt[i+1]+.0722*dt[i+2];t.lum=sm/(dt.length/4)/255;if(T[S.cur]===t)applyWash()}catch(e){}t.glow=c.toDataURL("image/png");if(T[S.cur]===t)glowSet(t)}catch(e){}};
      im.src=t.img}}
  a.style.background=t.glow?`url(${t.glow}) center/100% 100%`:art(t.c)}
/* lyrics follow the music: look them up for every song that starts, and warm up the next one */
const needLy=t=>t&&(t.url||t.yt)&&!t.radio&&!t.ph&&!t.lrc&&!t.lyBusy&&(!t.lyTried||Date.now()-(t.lyTs||0)>36e5);
function autoLyrics(){const i=S.cur;if(needLy(T[i]))findLyrics(i,false);
  clearTimeout(S.pft);S.pft=setTimeout(()=>{const ni=peekNext();if(ni!=null&&needLy(T[ni]))findLyrics(ni,false)},2500)}

