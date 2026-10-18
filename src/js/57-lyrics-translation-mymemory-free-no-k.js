/* ----- lyrics translation (MyMemory, free, no key; needs a connection) ----- */
const LTR=LS.get("milo.ltr",{});const ltrKey=()=>{const t=T[S.cur];return t?t.key+"|"+LANG:""};let ltrBusy="";
function ltrChunks(ls){const o=[];let cur=[],n=0;ls.forEach((x,i)=>{const s=String(x).replace(/\s+/g," ").trim();if(n+s.length>420&&cur.length){o.push(cur);cur=[];n=0}cur.push([i,s]);n+=s.length+1});if(cur.length)o.push(cur);return o}
async function ltrOne(q){const r=await fetch("https://api.mymemory.translated.net/get?q="+encodeURIComponent(q)+"&langpair=Autodetect|"+LANG);const j=await r.json();if(!j||j.responseStatus!==200)throw new Error("limit");return String(j.responseData&&j.responseData.translatedText||"")}
async function ltrFetch(){const key=ltrKey(),i=S.cur;if(!key||ltrBusy===key)return;const ls=lines(i);if(!ls.length)return;ltrBusy=key;const out=new Array(ls.length).fill("");
  try{for(const ch of ltrChunks(ls)){const txt=ch.map(c=>c[1]).join("\n");let parts=(await ltrOne(txt)).split(/\n/);
      if(parts.length!==ch.length){parts=[];for(const c of ch){parts.push(c[1]?await ltrOne(c[1]):"")}}
      ch.forEach((c,j)=>{out[c[0]]=(parts[j]||"").trim()})}
    LTR[key]=out;const ks=Object.keys(LTR);if(ks.length>30)delete LTR[ks[0]];LS.set("milo.ltr",LTR);ltrBusy="";if(S.cur===i){lyrRender();IM.kk=null}}
  catch(e){ltrBusy="";toast(/limit/.test(e.message)?"Translation limit reached for now. Try again later":"Couldn't translate the lyrics. Check your connection")}}
function ltrApply(){if(!S.lyTr)return;const arr=LTR[ltrKey()];if(!arr){ltrFetch();return}
  document.querySelectorAll("#lyL [data-k]").forEach(e=>{const t=arr[+e.dataset.k];if(t&&!e.querySelector(".tr")&&t.toLowerCase()!==e.textContent.trim().toLowerCase()){const s=document.createElement("span");s.className="tr";s.textContent=t;e.appendChild(s)}})}
const _lr2=lyrRender;lyrRender=function(){const r=_lr2.apply(this,arguments);ltrApply();return r};
const _ik2=imKar;imKar=function(sy,ls,k){const r=_ik2.apply(this,arguments);if(S.lyTr&&k>=0){const arr=LTR[ltrKey()];const el=$("imLy");if(!arr)ltrFetch();else if(el&&arr[k]&&!el.querySelector(".trl")){const cu=el.querySelector(".cu");if(cu){const p=document.createElement("p");p.className="trl";p.textContent=arr[k];cu.after(p)}}}return r};
function lyTrBtn(){let b=$("lyTr");if(!b){const h=$("lyF");if(!h)return;b=document.createElement("button");b.className="ic";b.id="lyTr";b.setAttribute("aria-label","Translate lyrics");b.innerHTML='<span style="font:700 14px/1 system-ui">A文</span>';b.onclick=()=>{S.lyTr=!S.lyTr;LS.set("milo.lytr",S.lyTr);lyTrBtn();lyrRender();IM.kk=null;toast(S.lyTr?"Translating lyrics":"Translation off")};h.before(b)}b.classList.toggle("on",!!S.lyTr);b.style.opacity=S.lyTr?1:.6}
lyTrBtn();if(LANG!=="en")try{render()}catch(e){}

/* now playing v3: palette from the song, dot-matrix scale, title selector */
(function(){
const G={0:"01110,10001,10011,10101,11001,10001,01110",1:"00100,01100,00100,00100,00100,00100,01110",2:"01110,10001,00001,00010,00100,01000,11111",3:"11110,00001,00001,01110,00001,00001,11110",4:"00010,00110,01010,10010,11111,00010,00010",5:"11111,10000,11110,00001,00001,10001,01110",6:"00110,01000,10000,11110,10001,10001,01110",7:"11111,00001,00010,00100,01000,01000,01000",8:"01110,10001,10001,01110,10001,10001,01110",9:"01110,10001,10001,01111,00001,00010,01100",":":"000,010,010,000,010,010,000"};
const dm=(txt,h)=>{let x=0,c="";const u=h/7;for(const ch of txt){const g=(G[ch]||"").split(",");if(!g[0])continue;const w=g[0].length;g.forEach((r,y)=>{for(let i=0;i<w;i++)if(r[i]==="1")c+=`<circle cx="${((x+i)+.5)*u}" cy="${(y+.5)*u}" r="${u*.36}"/>`});x+=w+1}return `<svg width="${Math.max(0,(x-1)*u)}" height="${h}" viewBox="0 0 ${Math.max(1,(x-1)*u)} ${h}">${c}</svg>`};
const hsl=(r,g,b)=>{r/=255;g/=255;b/=255;const M=Math.max(r,g,b),m=Math.min(r,g,b),l=(M+m)/2;let h=0,s=0;if(M!==m){const d=M-m;s=l>.5?d/(2-M-m):d/(M+m);h=M===r?(g-b)/d+(g<b?6:0):M===g?(b-r)/d+2:(r-g)/d+4;h*=60}return[h,s,l]};
const rgb=(h,s,l)=>{h=((h%360)+360)%360/360;const f=(p,q,t)=>{t<0&&(t+=1);t>1&&(t-=1);return t<1/6?p+(q-p)*6*t:t<.5?q:t<2/3?p+(q-p)*(2/3-t)*6:p},q=l<.5?l*(1+s):l+s-l*s,p=2*l-q;return[f(p,q,h+1/3),f(p,q,h),f(p,q,h-1/3)].map(v=>Math.round(v*255)).join(",")};
let lastC="",lastG="",lastSec=-1,lastKey="",built=0;
function build(){return;const w=document.querySelector("#full .artw");if(!w||built)return;built=1;
  const d=document.createElement("div");d.id="dmx";d.setAttribute("aria-hidden","true");w.appendChild(d);
  const s=document.createElement("div");s.id="npSel";w.appendChild(s);
  d.onclick=e=>{const b=e.target.closest("button");if(b&&b.dataset.t!=null)seekTo(+b.dataset.t)};
  s.onclick=e=>{const b=e.target.closest("button");if(!b)return;const k=b.dataset.k;const el=document.getElementById(k);el&&el.click()}}
function tick(){try{build();const t=T[S.cur],ap=document.getElementById("app");if(!t||!ap)return;
  if(t.c&&t.c!==lastC){lastC=t.c;const[r,g,b]=t.c.split(",").map(Number),[h,s0]=hsl(r,g,b),sv=Math.max(.7,Math.min(1,s0*1.4+.25));
    ap.style.setProperty("--n1",rgb(h,Math.min(.7,sv),.09));ap.style.setProperty("--n2",rgb(h,sv,.5));ap.style.setProperty("--n3",rgb(h+28,.95,.74));ap.style.setProperty("--n4",rgb(h-26,1,.46))}
  const gl=t.glow||"";if(gl!==lastG){lastG=gl;gl?ap.style.setProperty("--glow",`url("${gl}")`):ap.style.removeProperty("--glow")}
  return;const f=document.getElementById("full");if(!f||!f.classList.contains("open"))return;
  const dur=t.d||0,sec=Math.floor(S.pos||0);
  if(sec!==lastSec||lastKey!==S.cur+":"+S.qi){const key=S.cur+":"+S.qi;lastSec=sec;lastKey=key;
    const rows=[[30,13,.3],[20,17,.5],[10,24,.75],[0,36,1],[-10,24,.75],[-20,17,.5],[-30,13,.3]];
    document.getElementById("dmx").innerHTML=rows.map(([o,h,op])=>{const v=sec+o;const ok=v>=0&&(!dur||v<=dur);return ok?`<button data-t="${v}" style="opacity:${op}" aria-label="Seek to ${fmt(v)}">${dm(fmt(v),h)}</button>`:`<button style="height:${h}px" tabindex="-1"></button>`}).join("");
    const nm=i=>i==null||!T[i]?"":esc(T[i].t||T[i].n||"");const pv=S.q[S.qi-1],nx=peekNext();
    document.getElementById("npSel").innerHTML=`<button data-k="prev">${nm(pv)}</button><b>${nm(S.cur)}</b><button data-k="next">${nm(nx)}</button>`}
}catch(e){}}
setInterval(tick,300)})();
