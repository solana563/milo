/* ---------- media session: lock screen, notification shade, headset and keyboard media keys ---------- */
const MS="mediaSession" in navigator?navigator.mediaSession:null;
let msLast=0;
const KA=new Audio();KA.loop=true;
function msSet(a,f){try{MS.setActionHandler(a,f)}catch(e){}}
function msHandlers(t){if(!MS)return;const live=!!(t&&t.radio);
  msSet("play",()=>{if(!S.playing)toggle()});
  msSet("pause",()=>{if(S.playing)toggle()});
  msSet("stop",()=>{if(S.playing)toggle();if(!live&&S.cur>=0)seekTo(0)});
  msSet("previoustrack",()=>prev());
  msSet("nexttrack",()=>next(false));
  msSet("seekto",live?null:d=>{if(S.cur<0)return;const dur=T[S.cur].d||1e9;seekTo(Math.max(0,Math.min(dur,d.seekTime||0)))});
  msSet("seekbackward",live?null:d=>{if(S.cur>=0)seekTo(Math.max(0,S.pos-(d.seekOffset||10)))});
  msSet("seekforward",live?null:d=>{if(S.cur>=0)seekTo(Math.min(T[S.cur].d||1e9,S.pos+(d.seekOffset||10)))})}
function msCanvas(t,logo){return new Promise(res=>{try{const c=document.createElement("canvas");c.width=c.height=512;const x=c.getContext("2d"),[r,g,b]=String(t.c||"84,120,140").split(",").map(Number);
    const gr=x.createLinearGradient(0,0,512,512);gr.addColorStop(0,`rgb(${r},${g},${b})`);gr.addColorStop(1,`rgb(${r*.35|0},${g*.35|0},${b*.35|0})`);x.fillStyle=gr;x.fillRect(0,0,512,512);
    const done=()=>{try{res(c.toDataURL("image/jpeg",.86))}catch(e){res("")}};
    if(logo){const im=new Image();im.crossOrigin="anonymous";im.onload=()=>{try{const s=300,k=Math.min(s/im.naturalWidth,s/im.naturalHeight);const w=im.naturalWidth*k,h=im.naturalHeight*k;x.drawImage(im,(512-w)/2,(512-h)/2,w,h)}catch(e){}done()};im.onerror=done;im.src=logo;return}
    x.fillStyle="rgba(255,255,255,.92)";x.font="600 250px Georgia,serif";x.textAlign="center";x.textBaseline="middle";x.fillText(((t.t||"?").trim()[0]||"?").toUpperCase(),256,282);done()}catch(e){res("")}})}
async function msArt(t){const src=t.img||t.logo;
  if(!src||(t.radio&&t.logo&&!t.img)){const u=await msCanvas(t,t.radio?t.logo:"");return u?[{src:u,sizes:"512x512",type:"image/jpeg"}]:(src?[{src}]:[])}
  if(String(src).startsWith("data:"))return[{src}];
  return new Promise(res=>{const im=new Image();im.crossOrigin="anonymous";
    im.onload=()=>{try{const c=document.createElement("canvas");c.width=c.height=512;const s=Math.min(im.naturalWidth,im.naturalHeight)||512;
      c.getContext("2d").drawImage(im,((im.naturalWidth||s)-s)/2,((im.naturalHeight||s)-s)/2,s,s,0,0,512,512);res([{src:c.toDataURL("image/jpeg",.88),sizes:"512x512",type:"image/jpeg"}])}catch(e){res([{src}])}};
    im.onerror=()=>res([{src}]);im.src=src})}
function meta(t){if(!MS||!t)return;const key=t.img||t.logo||"";if(t.msFor!==key){t.msFor=key;t.msArt=null}
  const base={title:t.t||"",artist:t.a||"",album:t.al||""};
  try{MS.metadata=new MediaMetadata({...base,artwork:t.msArt||[]})}catch(e){}
  msHandlers(t);
  if(!t.msArt)msArt(t).then(a=>{t.msArt=a;if(T[S.cur]===t){try{MS.metadata=new MediaMetadata({...base,artwork:a})}catch(e){}}}).catch(()=>{});
  msPos(true)}
function msPos(force){if(!MS||!MS.setPositionState)return;const n=Date.now();if(!force&&n-msLast<1000)return;msLast=n;const t=T[S.cur];
  try{if(!t||t.radio||!isFinite(t.d)||t.d<=0){MS.setPositionState();return}
    MS.setPositionState({duration:t.d,playbackRate:S.rate||1,position:Math.max(0,Math.min(t.d,S.pos||0))})}catch(e){}}
function keepAlive(){const t=T[S.cur],need=!!(S.playing&&t&&!t.url);
  try{if(need){if(KA.paused){if(!KA.getAttribute("src"))KA.src=SIL;KA.play().catch(()=>{})}}else if(!KA.paused)KA.pause()}catch(e){}}
function msState(){if(MS){try{MS.playbackState=S.cur<0?"none":S.playing?"playing":"paused"}catch(e){}}
  const t=T[S.cur];try{document.title=t?(S.playing?"▶ ":"")+t.t+" · "+t.a:"milo."}catch(e){}
  keepAlive();msPos(true)}
msHandlers(null);
try{matchMedia("(prefers-color-scheme: dark)").addEventListener("change",theme)}catch(e){}
theme();setStyle();nav();render();route();restoreLibrary().then(hideSplash,hideSplash);
if("serviceWorker" in navigator&&/^https?:$/.test(location.protocol))addEventListener("load",()=>navigator.serviceWorker.register("sw.js").catch(()=>{}));

