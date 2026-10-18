/* ===== v5: sleep fade, share cards, lyric share, offline saves ===== */
const OFFB={};
function offU(t){return (t&&OFFB[t.url])||(t&&t.url)}
function dlOk(t){return !!(t&&t.url&&!t.radio&&!t.yt&&!t.file&&/^https?:/.test(t.url))}
async function offInit(){try{if(!window.caches)return;const c=await caches.open("milo-offline");for(const r of await c.keys()){const m=await c.match(r);if(m)OFFB[r.url]=URL.createObjectURL(await m.blob())}}catch(e){}}
async function dlTrack(i,quiet){const t=T[i];if(!dlOk(t))return false;if(OFFB[t.url])return true;
  try{if(!window.caches)throw 0;const r=await fetch(t.url,{mode:"cors"});if(!r.ok)throw 0;const b=await r.blob();if(!b.size)throw 0;
    const c=await caches.open("milo-offline");await c.put(t.url,new Response(b,{headers:{"content-type":b.type||"audio/mpeg"}}));OFFB[t.url]=URL.createObjectURL(b);try{navigator.storage&&navigator.storage.persist&&navigator.storage.persist()}catch(e){}
    if(!quiet)toast("Saved for offline: "+t.t);return true}
  catch(e){if(!quiet)toast("This song's source doesn't allow saving, or the device is full");return false}}
async function dlDrop(i){const t=T[i];if(!t||!OFFB[t.url])return;try{const c=await caches.open("milo-offline");await c.delete(t.url)}catch(e){}try{URL.revokeObjectURL(OFFB[t.url])}catch(e){}delete OFFB[t.url];toast("Offline copy removed")}
async function dlList(list){const ids=(list||[]).filter(i=>dlOk(T[i]));if(!ids.length){toast("Nothing here can be saved offline. Radio stations and YouTube songs stream live.");return}
  let ok=0,n=0;for(const i of ids){n++;toast("Saving "+n+" of "+ids.length+"…");if(await dlTrack(i,true))ok++}toast(ok?"Saved "+ok+" of "+ids.length+" for offline":"Couldn't save these. Their source blocks downloads.")}
offInit();addEventListener("offline",()=>toast("You're offline. Songs you saved still play."));
/* sleep timer: ease the volume down over the last 25 s */
(function(){let on=0;const set=v=>{try{if(S.mv&&S.ctx)S.mv.gain.setTargetAtTime(v*v,S.ctx.currentTime,.05);else DK.forEach(e=>e.volume=v);RA.volume=v;if(window.NE)NE.vol(v);if(YTX.ready)YTX.p.setVolume(Math.round(v*100))}catch(e){}};
  setInterval(()=>{const t=T[S.cur];let rem=1e9;if(S.sleep==="end"){if(t&&t.d&&S.playing&&!t.radio)rem=(t.d-S.pos)*1000}else if(S.sleep)rem=S.sleep-Date.now();
    if(S.playing&&rem<=25000&&rem>0){on=1;set(S.vol*Math.max(.04,rem/25000))}else if(on){on=0;setTimeout(()=>set(S.vol),400)}},400)})();
/* image cards */
function wrapT(x,txt,w,maxL){const ws=String(txt).split(/\s+/),out=[];let l="";for(const w1 of ws){const tt=l?l+" "+w1:w1;if(x.measureText(tt).width>w&&l){out.push(l);l=w1}else l=tt}if(l)out.push(l);return out.slice(0,maxL)}
function loadImg(src){return new Promise(r=>{if(!src)return r(null);const im=new Image();im.crossOrigin="anonymous";im.onload=()=>r(im);im.onerror=()=>r(null);im.src=src})}
function rr(x,a,b,w,h,r){x.beginPath();x.moveTo(a+r,b);x.arcTo(a+w,b,a+w,b+h,r);x.arcTo(a+w,b+h,a,b+h,r);x.arcTo(a,b+h,a,b,r);x.arcTo(a,b,a+w,b,r);x.closePath()}
async function cardBase(t,H){const c=document.createElement("canvas");c.width=1080;c.height=H;const x=c.getContext("2d"),col=(t.c||"249,156,0");
  const g=x.createLinearGradient(0,0,1080,H);g.addColorStop(0,`rgb(${col})`);g.addColorStop(.55,`rgba(${col},.55)`);g.addColorStop(1,"#0b0a09");x.fillStyle="#0b0a09";x.fillRect(0,0,1080,H);x.fillStyle=g;x.fillRect(0,0,1080,H);
  const im=await loadImg(t.img);return{c,x,im,col}}
function cardOut(c,name,txt){return new Promise(res=>c.toBlob(async b=>{if(!b){toast("Couldn't make the image (the art blocks copying)");return res()}
  const f=new File([b],name,{type:"image/png"});try{if(navigator.canShare&&navigator.canShare({files:[f]})){await navigator.share({files:[f],text:txt});return res()}}catch(e){if(e&&e.name==="AbortError")return res()}
  if(window.NV&&NV.cap)await saveFile(b,name);else{const a=document.createElement("a");a.href=URL.createObjectURL(b);a.download=name;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(a.href),4000)}toast("Image saved");res()},"image/png"))}
async function shareCard(i){const t=T[i];if(!t)return;toast("Making your card…");
  const{c,x,im}=await cardBase(t,1350);
  if(im){x.save();rr(x,160,170,760,760,56);x.clip();x.drawImage(im,160,170,760,760);x.restore()}else{x.save();rr(x,160,170,760,760,56);x.clip();x.fillStyle=`rgba(${t.c||"249,156,0"},.9)`;x.fillRect(160,170,760,760);x.restore()}
  x.fillStyle="#fff";x.textAlign="center";x.font="700 64px Figtree,Inter,Arial,sans-serif";let y=1040;for(const l of wrapT(x,t.t,900,2)){x.fillText(l,540,y);y+=76}
  x.fillStyle="rgba(255,255,255,.75)";x.font="500 40px Figtree,Inter,Arial,sans-serif";x.fillText(String(t.a||"").slice(0,48),540,y+10);
  x.fillStyle="rgba(255,255,255,.55)";x.font="600 34px Figtree,Inter,Arial,sans-serif";x.fillText("Listening on milo.",540,1290);
  await cardOut(c,"milo-"+String(t.t).replace(/[^a-z0-9]+/gi,"-").slice(0,30)+".png",t.t+" — "+t.a+" · on milo.")}
function lyShare(){const t=T[S.cur];if(!t)return;const ls=lines(S.cur);if(!ls.length){toast("No lyrics to share yet");return}
  const k0=S.lk>=0?S.lk:0;S.lySel=new Set([k0,k0+1,k0+2].filter(i=>i<ls.length&&ls[i].trim()));
  modal(`<h3>Share lyrics</h3><p style="margin:0 0 8px;opacity:.7;font-size:13px">Tap up to 6 lines</p><div id="lsl" style="max-height:44vh;overflow:auto">${ls.map((l,i)=>l.trim()?`<button class="mi lsl${S.lySel.has(i)?" on":""}" data-i="${i}" style="text-align:left;white-space:normal">${esc(l)}</button>`:"").join("")}</div><button class="addb" id="lsGo" style="margin-top:12px">Share as image</button>`);
  setTimeout(()=>{const e=document.querySelector("#lsl .on");e&&e.scrollIntoView({block:"center"})},60)}
document.addEventListener("click",async e=>{const b=e.target.closest(".lsl");if(b&&S.lySel){const i=+b.dataset.i;if(S.lySel.has(i))S.lySel.delete(i);else if(S.lySel.size<6)S.lySel.add(i);else toast("Up to 6 lines");b.classList.toggle("on",S.lySel.has(i));return}
  if(e.target.closest("#lsGo")){const t=T[S.cur],ls=lines(S.cur),sel=[...S.lySel||[]].sort((a,b)=>a-b).map(i=>ls[i]);if(!sel.length){toast("Pick a line first");return}closeM();toast("Making your card…");
    const{c,x,im}=await cardBase(t,1350);x.fillStyle="rgba(0,0,0,.28)";x.fillRect(0,0,1080,1350);
    x.fillStyle="#fff";x.textAlign="left";let fs=sel.join(" ").length>140?50:sel.join(" ").length>80?60:72;x.font=`700 ${fs}px Figtree,Inter,Arial,sans-serif`;
    let rows=[];for(const l of sel)rows.push(...wrapT(x,l,880,4));rows=rows.slice(0,12);const lh=fs*1.25,top=Math.max(150,(1000-rows.length*lh)/2+60);rows.forEach((l,k)=>x.fillText(l,100,top+k*lh));
    if(im){x.save();rr(x,100,1110,120,120,22);x.clip();x.drawImage(im,100,1110,120,120);x.restore()}
    x.fillStyle="#fff";x.font="700 38px Figtree,Inter,Arial,sans-serif";x.fillText(String(t.t).slice(0,30),245,1160);x.fillStyle="rgba(255,255,255,.7)";x.font="500 32px Figtree,Inter,Arial,sans-serif";x.fillText(String(t.a||"").slice(0,34),245,1206);
    x.textAlign="right";x.fillStyle="rgba(255,255,255,.6)";x.font="600 32px Figtree,Inter,Arial,sans-serif";x.fillText("milo.",980,1296);
    await cardOut(c,"milo-lyrics.png",sel.join(" / ")+" — "+t.t+" · "+t.a)}});
(function(){const rt=document.querySelector("#lyH .rt");if(rt&&!$("lyS")){const b=document.createElement("button");b.className="ic";b.id="lyS";b.setAttribute("aria-label","Share lyrics");b.innerHTML='<svg viewBox="0 0 24 24"><path d="M12 15V4M8 8l4-4 4 4M5 13v6h14v-6"/></svg>';b.onclick=lyShare;rt.insertBefore(b,rt.firstChild)}})();

