/* ===== v7: music already on the phone, played in place (no copying) ===== */
var DVRM=new Set(LS.get("milo.dvrm",[])||[]);
var DV={on:+LS.get("milo.dv",0)||0,busy:0,last:0,art:new Map(),dir:null,need:0};
function dvM(){try{if(!NV.cap)return null;let p=NV.P&&NV.P.MiloMedia;if(!p&&window.Capacitor&&Capacitor.registerPlugin)p=Capacitor.registerPlugin("MiloMedia");return p||null}catch(e){return null}}
const dvFile=p=>window.Capacitor&&Capacitor.convertFileUrl?Capacitor.convertFileUrl("file://"+p):"file://"+p;
const dvUrl=x=>x.p?dvFile(x.p):location.origin+"/_capacitor_content_"+String(x.u).slice(9);
const dvAlt=x=>x.p&&x.u?location.origin+"/_capacitor_content_"+String(x.u).slice(9):"";
const dvAk=t=>t.aid>0?"a"+t.aid:"t"+t.did;
function dvPaint(){clearTimeout(DV.pt);DV.pt=setTimeout(()=>{try{if(!S.nav&&document.visibilityState==="visible")render()}catch(e){}},500)}
function dvSync(rows,keepAll){/* rows: [{key,o}] -> add new, hide vanished */
  const have=new Map();T.forEach((t,i)=>{if(t.dev)have.set(t.key,t)});const seen=new Set();let add=0;
  for(const r of rows){seen.add(r.key);const h=have.get(r.key);
    if(h){if(h.gone&&!DVRM.has(r.key)){h.gone=false;h.hid=false}continue}
    if(DVRM.has(r.key))continue;T.push(Object.assign({key:r.key,dev:1,g:""},r.o));add++}
  if(!keepAll)T.forEach(t=>{if(t.dev&&!seen.has(t.key)&&!t.gone){t.gone=true;t.hid=true}});return add}
async function dvScan(quiet){
  const M=dvM();if(!M||DV.busy)return 0;DV.busy=1;
  try{const r=await M.scan(),L=(r&&r.tracks)||[];
    const n=dvSync(L.map(x=>{const al=x.al||"",nm=bn(x.f||x.t||"");return{key:"dev:"+x.id,o:{t:x.t||nm,a:x.a||"Unknown artist",al,y:x.y?String(x.y):"",n:x.n||0,d:x.d||0,c:COLS[hs(al||x.t||"")%6],url:dvUrl(x),alt:dvAlt(x),did:x.id,aid:x.aid,base:nm.toLowerCase(),fname:nm,added:(x.add||0)*1000}}}));
    DV.last=Date.now();render();if(!quiet)toast(L.length?`${L.length} songs on this phone`:"No music found on this phone");
    dvArt();return L.length}
  catch(e){if(!quiet)toast("Couldn't read the phone's music");return 0}finally{DV.busy=0}}
async function dvArt(){
  const M=dvM();if(!M)return;const todo=[],seen=new Set();
  T.forEach(t=>{if(t.dev&&!t.gone&&!t.img&&!t.noart){const k=dvAk(t);if(!seen.has(k)){seen.add(k);todo.push(t)}}});
  if(!todo.length)return;let i=0,done=0;
  const w=async()=>{while(i<todo.length){const t=todo[i++],k=dvAk(t);
    if(!DV.art.has(k)){try{const r=await M.art({id:t.did,aid:t.aid||0});DV.art.set(k,r&&r.path?dvFile(r.path):"");(DV.artp=DV.artp||new Map()).set(k,r&&r.path||"")}catch(e){DV.art.set(k,"")}}
    const u=DV.art.get(k);T.forEach(x=>{if(x.dev&&dvAk(x)===k){if(u)x.img=u;else x.noart=1}});
    if(++done===24)dvPaint()}};
  await Promise.all([w(),w(),w()]);dvPaint()}
async function dvConnect(){
  const M=dvM();if(!M){dvFolder();return}
  let g=false;try{g=!!(await M.ask()).granted}catch(e){}
  if(!g){modal(`<h3>Storage access is off</h3><p class="empty">milo. needs permission to read the music on your phone. Open your phone's Settings, then Apps, milo., Permissions, Music and audio, and choose Allow.</p><button class="mi" data-dv="x">OK</button>`);return}
  LS.set("milo.dv",1);DV.on=1;toast("Looking for music on this phone…");await dvScan()}
/* installed web app (desktop and mobile): link folders or files, they stay where they are */
var DVH=[];
function dvWebOk(){return !!(window.showDirectoryPicker||window.showOpenFilePicker)}
async function dvSaveH(){try{const db=await idb(),tx=db.transaction("kv","readwrite");tx.objectStore("kv").put({k:"dvsrc",h:DVH});await txDone(tx)}catch(e){}}
async function dvFolder(){
  if(!dvWebOk()){toast("This browser can't link files in place. Add music copies songs into milo. instead");return}
  try{let h;
    if(window.showDirectoryPicker)h=[await showDirectoryPicker({mode:"read"})];
    else h=await showOpenFilePicker({multiple:true,types:[{description:"Music",accept:{"audio/*":[".mp3",".m4a",".aac",".flac",".wav",".ogg",".opus",".weba"]}}]});
    for(const x of h)if(!(await Promise.all(DVH.map(y=>y.isSameEntry(x)))).some(Boolean))DVH.push(x);
    DV.need=0;LS.set("milo.dv",1);DV.on=1;await dvSaveH();await dvLoadH()}
  catch(e){if(e&&e.name==="AbortError")return;if(/android/i.test(navigator.userAgent))dvAndroidWeb();else toast("Couldn't open that")}}
function dvAndroidWeb(){modal(`<h3>This needs the milo. Android app</h3><p class="empty">Android does not let a browser or an installed web app read your Music folder, so songs can only be added by copying them in. The milo. Android app (APK) asks for music permission and plays your songs right where they are.</p><button class="mi" data-dv="x">OK</button>`)}
async function dvPerm(ask){let ok=true;for(const h of DVH){try{let p=await h.queryPermission({mode:"read"});if(p!=="granted"&&ask)p=await h.requestPermission({mode:"read"});if(p!=="granted")ok=false}catch(e){ok=false}}return ok}
async function dvLoadH(){
  if(DV.busy||!DVH.length)return;DV.busy=1;
  try{const fl=[];const walk=async(d,p)=>{for await(const [n,x] of d.entries()){if(x.kind==="directory")await walk(x,p+n+"/");else if(/\.(mp3|m4a|aac|flac|wav|ogg|opus|weba)$/i.test(n))fl.push({x,p:p+n})}};
    for(const h of DVH){if(h.kind==="directory")await walk(h,h.name+"/");else fl.push({x:h,p:"file/"+h.name})}
    const known=new Map();T.forEach(t=>{if(t.dev)known.set(t.key,t)});const rows=[];let i=0;
    const w=async()=>{while(i<fl.length){const e=fl[i++],key="dvf:"+e.p;
      if(known.has(key)){rows.push({key,o:null});continue}
      try{const f=await e.x.getFile(),r=await readTags(f),nm=fromName(f.name),al=r.al||"";
        rows.push({key,o:{t:r.t||nm.t,a:r.a||r.aa||nm.a||"On this device",al,y:r.y||"",n:r.n||nm.n||0,d:0,c:COLS[hs(al||r.t||nm.t)%6],url:URL.createObjectURL(f),fh:e.x,img:r.img,base:bn(f.name).toLowerCase(),fname:f.name,lrc:r.lrc,plain:r.plain,lsrc:r.lrc?"Embedded · synced":r.plain?"Embedded":"",added:f.lastModified}});
        if(rows.length%40===0){dvSync(rows.filter(z=>z.o),1);render()}}catch(er){}}};
    await Promise.all([w(),w(),w(),w()]);
    dvSync(rows.filter(z=>z.o),1);
    const keep=new Set(rows.map(z=>z.key));
    T.forEach(t=>{if(t.dev&&t.key.startsWith("dvf:")){if(keep.has(t.key)){if(t.gone&&!DVRM.has(t.key)){t.gone=false;t.hid=false}}else if(!t.gone){t.gone=true;t.hid=true}}});
    DV.last=Date.now();render();dvDur();toast(fl.length?`${fl.length} songs linked`:"No music found there")}
  catch(e){toast("Couldn't read that folder")}finally{DV.busy=0}}
function dvDur(){const q=T.filter(t=>t.dev&&!t.gone&&!t.d&&t.url);let i=0;
  const w=()=>new Promise(res=>{const nx=()=>{if(i>=q.length)return res();const t=q[i++],a=new Audio();a.preload="metadata";const dn=()=>{a.onloadedmetadata=a.onerror=null;a.src="";nx()};
    a.onloadedmetadata=()=>{if(isFinite(a.duration))t.d=a.duration;dn()};a.onerror=dn;a.src=t.url};nx()});
  Promise.all([w(),w(),w()]).then(dvPaint)}
async function dvBoot(){
  if(!DV.on)return;const M=dvM();
  if(M){try{if((await M.check()).granted)dvScan(true);else{DV.on=0;LS.set("milo.dv",0)}}catch(e){}return}
  if(!dvWebOk())return;
  try{const db=await idb(),r=await rq(db.transaction("kv").objectStore("kv").get("dvsrc"));if(!r||!r.h||!r.h.length)return;DVH=r.h;
    if(await dvPerm(false))dvLoadH();else{DV.need=1;render();
      /* the browser wants one tap to re-allow access: take it from the first tap anywhere */
      addEventListener("pointerup",async function f(){removeEventListener("pointerup",f);if(await dvPerm(true)){DV.need=0;dvLoadH()}},{once:true})}}catch(e){}}
function dvBar(){
  if(!DV||!DV.art)return"";const M=dvM(),can=M||dvWebOk();if(!can)return(!NV.cap&&/android/i.test(navigator.userAgent))?'<button class="ghost" data-dv="aw">Play music from my phone</button>':"";
  if(!DV.on)return M?'<button class="addb" data-dv="on">Play music on this phone</button>':'<button class="addb" data-dv="dir">'+(window.showDirectoryPicker?"Link a music folder":"Link music files")+'</button>';
  return (DV.need?'<button class="addb" data-dv="re">Reconnect music folder</button>':`<button class="ghost" data-dv="scan">${DV.busy?"Scanning…":"Rescan"}</button>`)+'<button class="ghost" data-dv="off">Turn off</button>'}
document.addEventListener("click",async e=>{const b=e.target.closest&&e.target.closest("[data-dv]");if(!b)return;const k=b.dataset.dv;
  if(k==="on"){closeM();dvConnect()}
  else if(k==="dir"){dvFolder()}
  else if(k==="scan"){if(dvM())dvScan();else dvLoadH()}
  else if(k==="re"){if(await dvPerm(true)){DV.need=0;dvLoadH()}}
  else if(k==="off"){DV.on=0;LS.set("milo.dv",0);T.forEach(t=>{if(t.dev){t.hid=true;t.gone=true}});toast("Phone music hidden");render()}
  else if(k==="later"){LS.set("milo.dvAsk",1);closeM()}
  else if(k==="aw"){dvAndroidWeb()}
  else if(k==="x"){closeM()}});
document.addEventListener("visibilitychange",()=>{if(document.visibilityState==="visible"&&DV.on&&dvM()&&Date.now()-DV.last>45000)dvScan(true)});
/* first launch of the app: ask for the music permission once. It waits for the sign-in screens to finish (it used to give up after 80 seconds and never ask) */
(function(){if(!dvM()||DV.on||LS.get("milo.dvAsk",0))return;
  const t=setInterval(()=>{const g=document.getElementById("gate"),m=document.getElementById("mdl");
    if(g||(m&&!m.hidden)||DV.on||LS.get("milo.dvAsk",0)){if(DV.on||LS.get("milo.dvAsk",0))clearInterval(t);return}
    clearInterval(t);LS.set("milo.dvAsk",1);
    modal(`<h3>Play the music on your phone</h3><p class="empty">Let milo. read your songs right where they are, like any music player. Nothing is copied or uploaded.</p><button class="mi" data-dv="on">Allow access</button><button class="mi" data-dv="later">Not now</button>`)},1500)})();
setTimeout(dvBoot,1200);

