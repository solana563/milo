/* ---------- full-screen now playing ---------- */
function imQual(t){if(t.radio)return["Live","Radio"];if(t.stream)return[(t.al||"Stream").slice(0,12),"Streaming"];if(!t.url)return["Synth","Demo"];
  const f=t.file,M={"audio/mpeg":"MP3","audio/mp3":"MP3","audio/mp4":"M4A","audio/x-m4a":"M4A","audio/m4a":"M4A","audio/aac":"AAC","audio/flac":"FLAC","audio/x-flac":"FLAC","audio/wav":"WAV","audio/x-wav":"WAV","audio/wave":"WAV","audio/ogg":"OGG","audio/opus":"OPUS","audio/webm":"WEBM"};
  let fm=f&&M[f.type]||"";if(!fm&&f&&f.name){const m=f.name.match(/\.([a-z0-9]+)$/i);if(m)fm=m[1].toUpperCase()}
  const kb=f&&f.size&&t.d>20?Math.round(f.size*8/t.d/1000):0;return[fm||"Audio",kb?kb+" kbps":"Local file"]}
function imStats(t){const k=t.key,pc=(S.plays&&S.plays[k])||0,o=[];const add=(v,l)=>o.push(`<div class="ims1"><b>${esc(v)}</b><small>${esc(l)}</small></div>`);
  if(!t.radio)add(pc,pc===1?"Play":"Plays");
  const a0=(t.a||"").split(/,\s*/)[0];
  if(!t.radio&&a0&&a0!=="On this device"){let ap=0,n=0;T.forEach(x=>{if(x.gone||x.radio||x.hid&&!x.stream)return;if((x.a||"").split(/,\s*/)[0]===a0){n++;ap+=(S.plays&&S.plays[x.key])||0}});add(ap,"Artist plays");if(n>1)add(n,"Songs by them")}
  if(t.al&&!t.stream&&!t.radio){const al=albums().find(x=>x.id===t.al.toLowerCase());if(al){const p=al.idx.indexOf(S.cur);if(p>=0&&al.idx.length>1)add((p+1)+" of "+al.idx.length,"On the album")}}
  if(t.g)add(t.g,"Genre");if(t.y)add(t.y,"Year");
  if(t.ryd){if(t.ryd.views)add(fmtK(t.ryd.views),"YouTube views");if(t.ryd.likes)add(fmtK(t.ryd.likes),"YouTube likes")}
  const q=imQual(t);add(q[0],q[1]);
  const pl=S.pls.filter(p=>p.keys.includes(k)).length;if(pl)add(pl,pl===1?"Playlist":"Playlists");
  if(S.likes.has(k))add("♥","Liked");if(t.d&&!t.radio)add(fmt(t.d),"Length");
  return o.join("")}
function imRender(){const t=T[S.cur];if(!t)return;
  $("imT").textContent=t.t;$("imAr").textContent=t.a||"";$("imAl").textContent=[t.al&&t.al!==t.a?t.al:"",t.y].filter(Boolean).join(" · ");
  $("imK").textContent=t.radio?"Live radio":t.stream?"Streaming from "+(t.al||"the web"):"Now playing";
  $("im").classList.toggle("yt",!!t.yt);$("imTh").style.background=t.yt&&!t.img?`center/cover url(https://i.ytimg.com/vi/${t.yt}/mqdefault.jpg),${art(t.c)}`:artOf(t);const ib=t.img?artOf(t):`radial-gradient(circle at 25% 20%,rgba(${t.c},1),transparent 55%),radial-gradient(circle at 80% 55%,rgba(${t.c},.75),transparent 55%),${art(t.c)}`;$("imBg").style.background=ib;$("im").classList.toggle("rad",!!t.radio);$("imT").classList.toggle("long",(t.t||"").length>18);{const u=me();$("imMe").textContent="Hi, "+((u.name||"").trim().split(/\s+/)[0]||"there")}$("imA").style.background=artOf(t);$("imG").style.background=artOf(t);$("imS").innerHTML=imStats(t);
  const ni=peekNext();$("imN").textContent=ni!=null&&T[ni]?"Up next · "+T[ni].t+" — "+T[ni].a:"";
  $("imD").textContent=t.radio?"LIVE":"-"+fmt(t.d);IM.ic="";imClock(true);imProg()}
function imProg(){const t=T[S.cur];if(!t||!IM.on)return;
  const fr=t.radio?0:Math.min(1,S.pos/(t.d||1));$("imB").style.width=fr*100+"%";const sk=$("imSeek");if(document.activeElement!==sk)sk.value=fr*1000;sk.style.setProperty("--p",fr*100+"%");$("imC").textContent=t.radio?"":fmt(S.pos);if(!t.radio)$("imD").textContent="-"+fmt(Math.max(0,(t.d||0)-S.pos));imClock();{const rem=t.radio?0:Math.max(0,(t.d||0)-S.pos),mm=String(Math.min(99,Math.floor(rem/60))).padStart(2,"0"),ss=String(Math.floor(rem%60)).padStart(2,"0");[["fm1",mm[0]],["fm2",mm[1]],["fs1",ss[0]],["fs2",ss[1]]].forEach(([i,v])=>{const e=$(i);if(e.textContent!==v)e.textContent=v})}
  const ls=lines(S.cur),sy=lsynced(S.cur),k=S.lk,ly=sy&&k>=0&&ls[k]?ls[k]:"";if($("imL").textContent!==ly)$("imL").textContent=ly;
  imKar(sy,ls,k)}
/* karaoke: the current line fills word by word, timed across the line (words weighted by length) */
function imKar(sy,ls,k){const el=$("imLy");if(!el)return;const t=T[S.cur],key=S.cur+"|"+(sy?k:"x")+"|"+ls.length;
  if(IM.kk!==key){IM.kk=key;IM.kw=null;
    if(!ls.length||!sy){el.innerHTML=`<p class="cu msg"><span class="w">${ls.length?"Tap to read the lyrics":"Tap to find the lyrics"}</span></p>`}
    else if(k<0){el.innerHTML=`<p class="cu msg"><span class="w">♪</span></p>`+[0,1].map(j=>ls[j]?`<p>${esc(ls[j])}</p>`:"").join("")}
    else{const ws=ls[k].split(/\s+/).filter(Boolean),tot=ws.reduce((a,w)=>a+w.length+1,0)||1,t0=lt(S.cur,k),nx=k<ls.length-1?lt(S.cur,k+1):Math.min(t0+6,t.d||t0+6),dur=Math.max(.6,Math.min(nx-t0,12)*.9);let c=0;
      const st=ws.map(w=>{const s=t0+dur*c/tot;c+=w.length+1;return[s,t0+dur*c/tot]});
      el.innerHTML=(ls[k-1]?`<p class="pv">${esc(ls[k-1])}</p>`:"")+`<p class="cu">${ws.map(w=>`<span class="w">${esc(w)}</span>`).join("")}</p>`+[1,2].map(j=>ls[k+j]?`<p>${esc(ls[k+j])}</p>`:"").join("");
      IM.kw={els:[...el.querySelectorAll(".cu .w")],st,f:[]}}}
  imKfill()}
function imKfill(){const K=IM.kw;if(!K)return;let p=S.pos;if(S.playing&&IM.kt)p+=Math.min(.5,(performance.now()-IM.kt)/1000);
  K.els.forEach((e,i)=>{const[a,b]=K.st[i],f=Math.max(0,Math.min(1,(p-a)/(b-a)));const q=Math.round(f*50)/50;if(K.f[i]!==q){K.f[i]=q;e.style.setProperty("--f",q)}})}
function imKloop(){IM.kraf=0;if(!IM.on)return;if(IM.kp!==S.pos){IM.kp=S.pos;IM.kt=performance.now()}imKfill();IM.kraf=requestAnimationFrame(imKloop)}
function imSize(){const c=IM.cv=IM.cv||$("imv");if(!c)return;const r=c.getBoundingClientRect(),d=Math.min(2,window.devicePixelRatio||1);IM.w=Math.max(1,r.width);IM.h=Math.max(1,r.height);c.width=Math.round(IM.w*d);c.height=Math.round(IM.h*d);IM.cx=c.getContext("2d");if(IM.cx)IM.cx.setTransform(d,0,0,d,0,0)}
/* spectrum rising from the bottom edge, mirrored so the bass sits in the middle; the glow behind the art breathes with it */
function imDraw(t){const x=IM.cx;if(!x)return;
  if(!(IM.fc=(IM.fc||0)+1)%20||!IM.cap){const a=document.querySelector("#im .imart"),c=IM.cv;if(a&&c){const ar=a.getBoundingClientRect(),cr=c.getBoundingClientRect(),avail=cr.bottom-ar.bottom-12;IM.cap=mqDesk.matches?Math.max(22,Math.min(avail,IM.h*.16)):IM.h*.12}else IM.cap=IM.h*.12}const n=VZ.n,w=IM.w,h=IM.h,mid=(n-1)/2,step=w/n,rgb=t?acc(t.c):"201,151,90";let low=0;for(let i=0;i<8;i++)low+=VZ.v[i];low/=8;
  x.clearRect(0,0,w,h);x.lineCap="round";x.lineWidth=Math.max(2,step*.42);
  for(let j=0;j<n;j++){const i=Math.round(Math.abs(j-mid)/mid*(n-1)),v=VZ.v[i];if(v<.01)continue;const bh=4+v*IM.cap,px=(j+.5)*step,g=x.createLinearGradient(0,h,0,h-bh);
    g.addColorStop(0,`rgba(${rgb},.85)`);g.addColorStop(1,`rgba(255,255,255,0)`);x.strokeStyle=g;x.beginPath();x.moveTo(px,h+2);x.lineTo(px,h-bh);x.stroke()}
  const gl=$("imG");if(gl)gl.style.transform=`scale(${(1+low*.14).toFixed(3)})`}
function imOpen(){if(IM.on)return;if(S.cur<0||!T[S.cur]){toast("Play a song and this fills the screen");return}
  IM.on=true;IM.guard=Date.now()+600;IM.pm=null;imRender();updCtl();ytPlaceSoon();imBar(true);const e=$("im");e.classList.add("open");e.setAttribute("aria-hidden","false");imSize();setTimeout(imSize,900);vzKick();if(!IM.kraf)IM.kraf=requestAnimationFrame(imKloop)}
function imClose(){if(!IM.on)return;IM.on=false;clearTimeout(IM.ht);$("app").classList.remove("imfs");IM.last=Date.now();const e=$("im");e.classList.remove("open");e.setAttribute("aria-hidden","true");const c=IM.cx;if(c)c.clearRect(0,0,IM.w,IM.h)}
function imAct(e){if(e.key==="Escape"&&IM.on)imClose()}
addEventListener("keydown",imAct,{capture:true});
$("imX").onclick=imClose;$("imSl").onclick=openSleep;$("imL2").onclick=()=>{imClose();setTimeout(openLyrics,60)};
function imBar(show){const a=$("app");clearTimeout(IM.ht);a.classList.toggle("imfs",!show);setTimeout(imSize,120);setTimeout(imSize,580);if(show&&IM.on&&mqDesk.matches)IM.ht=setTimeout(()=>{if(IM.on)imBar(false)},5000)}
let imMx=0,imMy=0;
["pointermove","pointerdown","keydown","wheel"].forEach(ev=>addEventListener(ev,e=>{if(!IM.on||!mqDesk.matches)return;if(ev==="pointermove"){if(Math.hypot(e.clientX-imMx,e.clientY-imMy)<5)return;imMx=e.clientX;imMy=e.clientY}
  if($("app").classList.contains("imfs"))imBar(true);else{clearTimeout(IM.ht);IM.ht=setTimeout(()=>{if(IM.on)imBar(false)},5000)}},{passive:true,capture:true}));
addEventListener("resize",()=>{if(IM.on)imSize()});
function imClock(force){const m=Math.floor(Date.now()/60000);if(!force&&IM.mk===m)return;IM.mk=m;const d=new Date(),tm=d.toLocaleTimeString([],{hour:"numeric",minute:"2-digit"}).replace(/\s?[AP]\.?M\.?/i,""),dt=d.toLocaleDateString([],{weekday:"long",month:"long",day:"numeric"});["imCk","fCk"].forEach(i=>{const e=$(i);if(e)e.textContent=tm});["imDt","fDt"].forEach(i=>{const e=$(i);if(e)e.textContent=dt})}
setInterval(()=>imClock(),20000);imClock(true);
$("fV").oninput=e=>setVol(e.target.value/100);$("fV").value=Math.round(S.vol*100);$("fV").style.setProperty("--p",Math.round(S.vol*100)+"%");
$("imLogo").src=$("logo").src;document.querySelectorAll(".exlogo").forEach(i=>i.src=$("logo").src);$("imQ").onclick=openQueue;$("imMe").onclick=()=>{imClose();go("Profile")};$("imLy").onclick=()=>{imClose();setTimeout(openLyrics,60)};$("imLy").onkeydown=e=>{if(e.key==="Enter"||e.key===" "){e.preventDefault();$("imLy").click()}};
{const h=document.createElement("div");h.id="vhud";h.setAttribute("aria-hidden","true");h.innerHTML='<svg viewBox="0 0 24 24"><path d="M4 9.5v5h3.5L12 18.5v-13L7.5 9.5zM15.5 8.5a5 5 0 010 7M18 6a8.5 8.5 0 010 12"/></svg><i><b></b></i><span></span>';$("app").appendChild(h);let tm;
  const show=()=>{const p=Math.round(S.vol*100);h.querySelector("b").style.width=p+"%";h.querySelector("span").textContent=p+"%";h.classList.add("on");clearTimeout(tm);tm=setTimeout(()=>h.classList.remove("on"),1100)};
  ["mini","im"].forEach(id=>$(id).addEventListener("wheel",e=>{if(!mqDesk.matches||e.ctrlKey||e.shiftKey)return;if(id==="im"&&!IM.on)return;e.preventDefault();const d=e.deltaMode===1?e.deltaY*.04:e.deltaMode===2?e.deltaY*.2:e.deltaY/2500;setVol(S.vol-d);show()},{passive:false}))}
$("imPp").onclick=toggle;$("imPv").onclick=prev;$("imNx").onclick=()=>next(false);
$("imSeek").oninput=e=>{if(S.cur>=0&&T[S.cur]&&!T[S.cur].radio){seekTo(e.target.value/1000*(T[S.cur].d||0));e.target.style.setProperty("--p",e.target.value/10+"%")}};
$("imV").oninput=e=>setVol(e.target.value/100);$("imV").value=Math.round(S.vol*100);$("imV").style.setProperty("--p",Math.round(S.vol*100)+"%");


