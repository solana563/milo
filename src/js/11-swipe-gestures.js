/* ---------- swipe gestures ---------- */
function swipe(el,cb,skip){let x0=0,y0=0,t0=0,ok=false;
  el.addEventListener("touchstart",e=>{const t=e.touches[0];ok=!(skip&&e.target.closest&&e.target.closest(skip));x0=t.clientX;y0=t.clientY;t0=Date.now()},{passive:true});
  el.addEventListener("touchend",e=>{if(!ok)return;ok=false;const t=e.changedTouches[0],dx=t.clientX-x0,dy=t.clientY-y0,ax=Math.abs(dx),ay=Math.abs(dy);if(Date.now()-t0>800)return;
    if(ax>60&&ax>ay*1.5)cb(dx<0?"left":"right",e);else if(ay>80&&ay>ax*1.5)cb(dy<0?"up":"down",e)},{passive:true})}
function nudge(id,dir){const el=$(id);if(!el)return;el.style.transition="transform .18s";el.style.transform=`translateX(${dir==="left"?-36:36}px)`;setTimeout(()=>{el.style.transform=""},180)}
function skipSong(d,id){if(S.cur<0)return;nudge(id,d);d==="left"?next(false):prev()}
$("lyF").innerHTML=svg(ICON.Search);$("lyF").onclick=()=>{if(S.cur>=0)findLyrics(S.cur,true,true)};
$("lyBtn").onclick=openLyrics;

