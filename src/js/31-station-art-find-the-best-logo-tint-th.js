/* ---------- station art: find the best logo, tint the app from it ---------- */
var SA={q:[],n:0,rt:0};
const hostOf=u=>{try{return new URL(/^https?:/.test(u)?u:"http://"+u).hostname.replace(/^www\./,"")}catch(e){return""}};
function loadImg(u,min){return new Promise(res=>{const go=cors=>{const im=new Image();let d=0;const f=ok=>{if(d)return;d=1;if(ok&&im.naturalWidth>=min&&im.naturalHeight>=min)res({im,cors,w:im.naturalWidth});else if(cors)go(false);else res(null)};im.onload=()=>f(1);im.onerror=()=>f(0);setTimeout(()=>f(0),7000);if(cors)im.crossOrigin="anonymous";im.referrerPolicy="no-referrer";im.src=u};go(true)})}
function artCands(t){const h=hostOf(t.home),o=[];if(t.logo)o.push([t.logo,24]);
  if(h)o.push([`https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=${encodeURIComponent("https://"+h)}&size=256`,64],[`https://icons.duckduckgo.com/ip3/${h}.ico`,32],[`https://${h}/apple-touch-icon.png`,96],[`https://${h}/favicon.ico`,32]);
  return o}
function logoColor(im){try{const c=document.createElement("canvas");c.width=c.height=32;const x=c.getContext("2d");x.drawImage(im,0,0,32,32);const d=x.getImageData(0,0,32,32).data;let r=0,g=0,b=0,w=0;
  for(let i=0;i<d.length;i+=4){if(d[i+3]<60)continue;const mx=Math.max(d[i],d[i+1],d[i+2]),mn=Math.min(d[i],d[i+1],d[i+2]);if(mn>238||mx<22)continue;const s=(mx-mn)/(mx||1),k=.05+s*s*s;r+=d[i]*k;g+=d[i+1]*k;b+=d[i+2]*k;w+=k}
  return w>0?[r,g,b].map(v=>Math.round(v/w)).join(","):null}catch(e){return null}}
async function stationArt(t){if(!t||!t.radio||t.artDone)return;t.artDone=1;
  const rs=await Promise.all(artCands(t).map(([u,m])=>loadImg(u,m).then(r=>r&&{...r,u})));
  const ok=rs.filter(Boolean);if(!ok.length)return;ok.sort((a,b)=>b.w-a.w);const best=ok[0];
  t.logo=best.u;t.lgw=best.w;t.lg=1;let col=null;for(const x of [best,...ok]){if(x.cors){col=logoColor(x.im);if(col)break}}
  if(col)t.c=col;
  const f=S.rfav.find(x=>x.id===t.sid);if(f){f.logo=t.logo;f.home=t.home;f.lg=1;if(col)f.c=col;save()}
  if(T[S.cur]===t){bgArt(t);glowSet(t);sync();if(IM.on)imRender()}
  else{clearTimeout(SA.rt);SA.rt=setTimeout(()=>{refreshR()},700)}}
function artScan(){T.forEach(t=>{if(t.radio&&t.needArt&&!t.artDone&&!t.lg&&!SA.q.includes(t))SA.q.push(t)});artPump()}
function artPump(){while(SA.n<3&&SA.q.length){const t=SA.q.shift();SA.n++;stationArt(t).catch(()=>{}).finally(()=>{SA.n--;artPump()})}}
artScan();

