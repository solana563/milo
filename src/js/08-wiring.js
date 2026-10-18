/* ---------- wiring ---------- */
$("fQ").innerHTML=svg(I.queue);$("shf").innerHTML=svg(I.shuf);$("fL").innerHTML=svg(I.heart);
$("mP").onclick=e=>{e.stopPropagation();toggle()};

$("cl").onclick=()=>$("full").classList.remove("open");
$("fP").onclick=toggle;$("lyP").onclick=toggle;$("prev").onclick=prev;$("next").onclick=()=>next(false);
$("shf").onclick=setShuf;$("rep").onclick=()=>{S.rep=(S.rep+1)%3;save();updCtl()};
$("fL").onclick=()=>{if(S.cur>=0)toggleLike(S.cur)};$("fQ").onclick=openQueue;$("spd").onclick=openSpeed;$("snd").onclick=soundM;$("slp").onclick=openSleep;$("apl").onclick=()=>{if(S.cur>=0)addPlM(S.cur)};
function lyClose(){$("lyr").classList.remove("open");if(S.cur>=0){if(mqDesk.matches){if(!IM.on)imOpen()}else if(!$("full").classList.contains("open"))openFull()}}
$("lyX").onclick=lyClose;
$("seek").oninput=e=>seekTo(e.target.value/1000*(T[S.cur].d||0));
$("lyL").onclick=e=>{if(e.target.closest("[data-fl]")){findLyrics(S.cur,true);return}const p=e.target.closest("p");if(!p||p.dataset.k==null||$("lyL").classList.contains("static"))return;S.lyUser=0;seekTo(lt(S.cur,+p.dataset.k));if(!S.playing)toggle()};

