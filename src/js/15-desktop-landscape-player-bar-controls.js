/* ---------- desktop / landscape: player bar controls, volume, keyboard shortcuts ---------- */
S.vol=Math.max(0,Math.min(1,+LS.get("milo.vol",1)));RA.volume=S.vol;
const mqDesk=matchMedia("(min-width:860px)");
function setVol(v){v=Math.max(0,Math.min(1,v));S.vol=v;if(S.mv&&S.ctx)S.mv.gain.setTargetAtTime(v*v,S.ctx.currentTime,.03);else DK.forEach(e=>e.volume=v);RA.volume=v;try{if(YTX.ready)YTX.p.setVolume(Math.round(v*100))}catch(e){}LS.set("milo.vol",v);const el=$("dV");if(el)el.value=Math.round(v*100);const e2=$("imV");if(e2){e2.value=Math.round(v*100);e2.style.setProperty("--p",Math.round(v*100)+"%")}const e3=$("fV");if(e3){e3.value=Math.round(v*100);e3.style.setProperty("--p",Math.round(v*100)+"%")}}
$("dsh").innerHTML=svg(I.shuf);$("dprev").innerHTML='<svg viewBox="0 0 24 24"><path d="M6 5h2v14H6zM20 5v14L9 12z"/></svg>';$("dnext").innerHTML='<svg viewBox="0 0 24 24"><path d="M16 5h2v14h-2zM4 5v14l11-7z"/></svg>';
$("dL").innerHTML=svg('<path d="M5 6h14M5 10h14M5 14h9M5 18h6"/>');$("dQ").innerHTML=svg(I.queue);$("dSl").innerHTML=svg('<path d="M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z"/>');$("dS").innerHTML=svg('<path d="M5 20v-7M5 9V4M12 20v-3M12 13V4M19 20v-9M19 7V4M3 13h4M10 17h4M17 11h4"/>');
$("dF").innerHTML=svg('<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>');$("dVi").innerHTML=svg('<path d="M4 10v4h4l5 4V6L8 10zM16 9a4 4 0 010 6"/>');
$("dV").value=Math.round(S.vol*100);
$("mini").onclick=e=>{if(mqDesk.matches&&!e.target.closest(".mr"))return;openFull()};
$("dP").onclick=toggle;$("dprev").onclick=prev;$("dnext").onclick=()=>next(false);$("dsh").onclick=setShuf;
$("drp").onclick=()=>{S.rep=(S.rep+1)%3;save();updCtl()};
$("dL").onclick=()=>{if(S.cur>=0)openLyrics()};$("dQ").onclick=openQueue;$("dSl").onclick=openSleep;$("dS").onclick=soundM;$("dF").onclick=openFull;
$("dV").oninput=e=>setVol(e.target.value/100);
$("dSeek").oninput=e=>{if(S.cur>=0)seekTo(e.target.value/1000*(T[S.cur].d||0))};
document.addEventListener("keydown",e=>{
  if(e.metaKey||e.ctrlKey||e.altKey)return;const el=e.target,tag=el&&el.tagName,k=e.key;
  if(tag==="TEXTAREA"||tag==="SELECT"||(tag==="INPUT"&&el.type!=="range"&&el.type!=="checkbox"&&el.type!=="file"))return;
  if(k==="Escape"){if(!$("mdl").hidden)closeM();else if($("lyr").classList.contains("open"))lyClose();else if($("full").classList.contains("open"))$("full").classList.remove("open");return}
  if(!$("mdl").hidden)return;
  if(k==="?"){e.preventDefault();keysM();return}
  if(k==="/"){e.preventDefault();go("Search");const q=$("q");if(q)q.focus();return}
  if(S.cur<0)return;const t=T[S.cur],rng=tag==="INPUT"&&el.type==="range";
  if(k===" "&&tag!=="BUTTON"&&tag!=="A"){e.preventDefault();toggle()}
  else if(k==="ArrowRight"&&!rng){e.preventDefault();e.shiftKey?next(false):(!t.radio&&seekTo(Math.min(t.d||1e9,S.pos+5)))}
  else if(k==="ArrowLeft"&&!rng){e.preventDefault();e.shiftKey?prev():(!t.radio&&seekTo(Math.max(0,S.pos-5)))}
  else if(k==="n"||k==="N")next(false);
  else if(k==="p"||k==="P")prev();
  else if(k==="+"||k==="=")setVol(S.vol+.05);
  else if(k==="-"||k==="_")setVol(S.vol-.05);
  else if(k==="m"||k==="M"){if(S.vol>0){S.pv=S.vol;setVol(0)}else setVol(S.pv||1)}
  else if(k==="l"||k==="L")openLyrics();
  else if(k==="f"||k==="F")openFull();
  else if(k==="s"||k==="S")setShuf();
  else if(k==="r"||k==="R"){S.rep=(S.rep+1)%3;save();updCtl()}
});


