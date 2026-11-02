/* ---------- native readiness: storage, back button, wake lock, share, Capacitor hooks ---------- */
const NV=(()=>{const C=window.Capacitor,P=(C&&C.Plugins)||{},ua=navigator.userAgent||"",cap=!!(C&&C.isNativePlatform&&C.isNativePlatform()),wv=/; wv\)/.test(ua)||!!window.AndroidDownloader,
  sa=(window.matchMedia&&matchMedia("(display-mode: standalone)").matches)||navigator.standalone===true;
  return{cap,wv,sa,P,app:cap||wv||sa,mode:cap?"capacitor":wv?"webview":sa?"pwa":"web"}})();
try{$("app").dataset.native=NV.mode}catch(e){}
const fmtB=n=>n>=1073741824?(n/1073741824).toFixed(1)+" GB":n>=1048576?Math.round(n/1048576)+" MB":n>=1024?Math.round(n/1024)+" KB":(n||0)+" B";
/* 1. storage: ask the browser not to clear our data, and show how much is used */
async function stPersist(){try{if(navigator.storage&&navigator.storage.persisted){if(await navigator.storage.persisted())return true;if(navigator.storage.persist)return await navigator.storage.persist()}}catch(e){}return false}
async function stInfo(){const o={used:0,quota:0,persisted:null,songs:0,bytes:0,idb:true};
  try{if(navigator.storage&&navigator.storage.estimate){const e=await navigator.storage.estimate();o.used=e.usage||0;o.quota=e.quota||0}}catch(e){}
  try{if(navigator.storage&&navigator.storage.persisted)o.persisted=await navigator.storage.persisted()}catch(e){}
  T.forEach(t=>{if(t.file&&!t.gone){o.songs++;o.bytes+=t.file.size||0}});
  try{await idb()}catch(e){o.idb=false}return o}
async function storageM(){const i=await stInfo(),pct=i.quota?Math.min(100,Math.max(1,i.used/i.quota*100)):0;
  modal(`<h3>Storage</h3>
  <div class="stbar" aria-hidden="true"><i style="width:${pct}%"></i></div>
  <p class="stp">${i.quota?`Using ${fmtB(i.used)} of about ${fmtB(i.quota)} available to milo.`:`Using ${fmtB(i.used)}.`}</p>
  <div class="stg"><div><b>${i.songs}</b><small>Uploaded songs</small></div><div><b>${fmtB(i.bytes)}</b><small>Song files</small></div><div><b>${i.idb?(i.persisted?"Protected":i.persisted===false?"Not yet":"Unknown"):"Off"}</b><small>${i.idb?"Kept when space is low":"Saving is blocked"}</small></div></div>
  ${i.idb?"":'<p class="stw">This browser is blocking saved songs, so uploads last only until you close the app.</p>'}
  ${i.idb&&i.persisted===false?'<button class="mi" data-stor="persist">Protect my data</button>':""}
  <button class="mi" data-stor="bk">Back up settings and playlists</button>
  <label class="mi">Restore a backup<input type="file" accept=".json,application/json" data-bk hidden></label>
  ${window.miloDesktop?'<button class="mi" data-stor="dopen">Open the data folder</button><button class="mi" data-stor="dchg">Change the data folder</button>':""}
  ${window.miloDesktop?'<button class="mi" data-stor="dvdir">Link a music folder</button>':window.showDirectoryPicker?'<button class="mi" data-stor="dir">Add a music folder</button>':""}
  ${i.songs?'<button class="mi stdng" data-stor="rm">Remove all uploaded songs</button>':""}
  <p class="empty" style="font-size:12px;margin-top:10px">Songs you upload stay on this device. A backup holds your playlists, likes and settings, not the song files.</p>`)}
document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-stor]");if(!b)return;const k=b.dataset.stor;
  if(k==="persist"){stPersist().then(ok=>{toast(ok?"Your data is protected":"This browser said no. Installing milo. as an app usually fixes that.");storageM()})}
  else if(k==="bk"){backupOut()}
  else if(k==="dir"){pickDir()}
  else if(k==="dvdir"){closeM();dvFolder()}
  else if(k==="dopen"){miloDesktop.openDataDir()}
  else if(k==="dchg"){miloDesktop.chooseDataDir().then(d=>{if(d){toast("Data folder changed");kvDisk()}})}
  else if(k==="rm"){modal(`<h3>Remove all uploaded songs?</h3><p class="empty">This deletes the songs you added from this device. Your playlists and likes stay, but they will lose these songs.</p><button class="mi stdng" data-stor="rm2">Remove songs</button><button class="mi" data-stor="no">Cancel</button>`)}
  else if(k==="rm2"){clearLibrary();closeM();toast("Uploaded songs removed")}
  else if(k==="no"){storageM()}});
document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-tool=stor]");if(b)storageM()});
/* 2. keep a second copy of settings in IndexedDB, in case the app's localStorage is cleared */
const KV={t:0};
async function kvSnap(){try{const o={};for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);if(/^milo\./.test(k))o[k]=localStorage.getItem(k)}if(!Object.keys(o).length)return;const db=await idb(),tx=db.transaction("kv","readwrite");tx.objectStore("kv").put({k:"snap",at:Date.now(),o});await txDone(tx)}catch(e){}}
function kvSoon(){clearTimeout(KV.t);KV.t=setTimeout(kvSnap,1500)}
{const _s=LS.set;LS.set=(k,v)=>{_s(k,v);kvSoon()}}
addEventListener("pagehide",kvSnap);document.addEventListener("visibilitychange",()=>{if(document.hidden)kvSnap()});
(async()=>{try{if(localStorage.getItem(dkey())||sessionStorage.getItem("milo.rec"))return;const db=await idb(),r=await rq(db.transaction("kv").objectStore("kv").get("snap"));
  if(r&&r.o&&r.o[dkey()]){Object.entries(r.o).forEach(([k,v])=>{try{localStorage.setItem(k,v)}catch(e){}});sessionStorage.setItem("milo.rec","1");location.reload()}}catch(e){}})();
/* 2b. desktop app: keep a readable copy of settings, playlists and likes in the data folder (with daily backups) */
const KD={t:0};
async function kvDisk(){try{const D=window.miloDesktop;if(!D)return;const o={};for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);if(/^milo\./.test(k)&&!/tok|sess|auth|secret|pass/i.test(k))o[k]=localStorage.getItem(k)}if(Object.keys(o).length)await D.saveData(JSON.stringify(o))}catch(e){}}
if(window.miloDesktop){{const _d=LS.set;LS.set=(k,v)=>{_d(k,v);clearTimeout(KD.t);KD.t=setTimeout(kvDisk,2500)}}
  addEventListener("pagehide",kvDisk);setTimeout(kvDisk,6000)}
/* 3. saving files: native share sheet / save dialog where there is one, plain download otherwise */
async function saveFile(blob,name){
  try{const FS=NV.P.Filesystem,SH=NV.P.Share;if(NV.cap&&FS){const d=await new Promise((res,rej)=>{const r=new FileReader();r.onload=()=>res(String(r.result).split(",")[1]);r.onerror=rej;r.readAsDataURL(blob)});
    const w=await FS.writeFile({path:name,data:d,directory:"CACHE"});if(SH)await SH.share({title:name,url:w.uri});return true}}catch(e){}
  try{if(window.showSaveFilePicker){const h=await showSaveFilePicker({suggestedName:name,types:[{description:"milo. backup",accept:{"application/json":[".json"]}}]});const w=await h.createWritable();await w.write(blob);await w.close();return true}}catch(e){if(e&&e.name==="AbortError")return true}
  const a=document.createElement("a");a.href=URL.createObjectURL(blob);a.download=name;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(a.href),4000);return true}
backupOut=function(){try{const o=LS.get(dkey(),{}),b=new Blob([JSON.stringify({milo:2,at:Date.now(),data:o},null,1)],{type:"application/json"});
  saveFile(b,"milo-backup-"+dayKey()+".json").then(()=>toast("Backup saved. Songs you uploaded are not included."))}catch(e){toast("Couldn't create the backup")}};
/* add a whole folder (desktop Chrome / Edge) */
async function pickDir(){try{const d=await showDirectoryPicker(),out=[];
  const walk=async h=>{for await(const [n,x] of h.entries()){if(x.kind==="directory")await walk(x);else if(/\.(mp3|m4a|aac|flac|wav|ogg|opus|weba|lrc)$/i.test(n))out.push(await x.getFile())}};
  await walk(d);closeM();if(!out.length){toast("No music found in that folder");return}await addFiles(out)}catch(e){if(!e||e.name!=="AbortError")toast("Couldn't open that folder")}}
/* 4. share target: songs shared to milo. from other apps (installed web app) */
(async()=>{try{if(!/[?&]shared=1/.test(location.search)||!window.caches)return;const c=await caches.open("milo-share"),ks=await c.keys(),fs=[];
  for(const k of ks){const r=await c.match(k);if(!r)continue;const b=await r.blob();let n="song";try{n=decodeURIComponent(r.headers.get("x-name")||"song")}catch(e){}fs.push(new File([b],n,{type:b.type||"audio/mpeg"}));await c.delete(k)}
  try{history.replaceState({milo:0},"",location.pathname)}catch(e){}if(fs.length)await addFiles(fs)}catch(e){}})();
/* 5. keep the screen on while the player is open and playing */
let WL=null;
async function wlSync(){const want=!!(S.playing&&!document.hidden&&(IM.on||$("full").classList.contains("open")||$("lyr").classList.contains("open")));
  try{if(want&&!WL&&navigator.wakeLock){WL=await navigator.wakeLock.request("screen");WL.addEventListener("release",()=>{WL=null})}else if(!want&&WL){await WL.release();WL=null}}catch(e){WL=null}
  try{const K=NV.P.KeepAwake;if(K&&NV.wl!==want){NV.wl=want;want?K.keepAwake():K.allowSleep()}}catch(e){}}
setInterval(wlSync,2000);document.addEventListener("visibilitychange",wlSync);
/* 6. network and system bars follow the app */
function netUpd(){$("app").dataset.net=navigator.onLine===false?"off":"on"}
addEventListener("offline",()=>{netUpd();toast("You're offline. Your own songs still play.")});addEventListener("online",()=>{netUpd();toast("Back online")});netUpd();
function barUpd(){try{const ph=innerWidth<860&&!S.nav,bg=ph?"#000000":(getComputedStyle($("app")).getPropertyValue("--bg").trim()||"#0a0a0b"),m=document.querySelector('meta[name=theme-color]');if(m&&m.content!==bg)m.content=bg;
  const dark=ph||$("app").dataset.theme==="dark"||(getComputedStyle($("app")).colorScheme||"").indexOf("dark")>=0;
  const SB=NV.P.StatusBar;if(SB&&NV.dk!==dark){NV.dk=dark;SB.setStyle({style:dark?"DARK":"LIGHT"});if(NV.cap&&/android/i.test(navigator.userAgent))SB.setBackgroundColor({color:bg})}}catch(e){}}
setInterval(barUpd,1500);barUpd();
/* 7. Capacitor: hardware back button, haptics, resume */
if(NV.cap){const A=NV.P.App;
  if(A&&A.addListener){A.addListener("backButton",()=>{if(backStep())return;if(Date.now()-(NV.bk||0)<2000){try{A.exitApp()}catch(e){}}else{NV.bk=Date.now();toast("Press back again to exit")}});
    A.addListener("appStateChange",s=>{if(s&&s.isActive){try{if(S.ctx&&S.ctx.state!=="running"&&S.playing)S.ctx.resume()}catch(e){}wlSync()}})}
  if(NV.P.Haptics){buzz=function(n){try{NV.P.Haptics.impact({style:(n||8)>20?"MEDIUM":"LIGHT"})}catch(e){}}}}
if(NV.app)stPersist();
/* 8. Capacitor: native splash hand-off and background audio (foreground service while a song plays) */
if(NV.cap){try{const SP=NV.P.SplashScreen;if(SP)setTimeout(()=>{try{SP.hide({fadeOutDuration:250})}catch(e){}},60)}catch(e){}
  let BGon=false;const BG=NV.P.BackgroundMode;
  if(BG){setInterval(()=>{try{const want=!!S.playing;if(want&&!BGon){BGon=true;BG.enable({title:"milo.",text:((T[S.cur]&&T[S.cur].t)||"Playing music"),silent:false,hidden:false,resume:true}).catch(()=>{BGon=false});BG.requestNotificationsPermission&&BG.requestNotificationsPermission().catch(()=>{})}
    else if(!want&&BGon){BGon=false;BG.disable().catch(()=>{})}}catch(e){}},1500)}}


