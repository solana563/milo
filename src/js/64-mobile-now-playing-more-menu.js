/* Mobile now-playing (phone portrait): the "more" menu.
   The mockup has four round buttons (Like, Queue, Sound, More). Everything else
   (speed, sleep, + playlist) lives in the More menu. We MOVE the
   real buttons (ids and handlers stay), and move them back on wide/landscape layouts. */
(function(){
  const full=$("full"),menu=$("fMoreP"),btn=$("fMore"),ctl=full.querySelector(".ctl"),ex=full.querySelector(".ex");
  if(!full||!menu||!btn||!ctl||!ex)return;
  const mq=matchMedia("(max-width:859px) and (max-aspect-ratio:11/10)");
  const shf=$("shf"),rep=$("rep"),items=[$("spd"),$("slp"),$("apl")];
  function shut(){full.classList.remove("mo");btn.setAttribute("aria-expanded","false")}
  function place(){
    if(mq.matches){items.forEach(e=>menu.appendChild(e))}
    else{const lg=ex.querySelector(".exlogo");[$("spd"),$("slp"),$("apl")].forEach(e=>ex.insertBefore(e,lg))}
    shut();
  }
  btn.onclick=e=>{e.stopPropagation();const o=!full.classList.contains("mo");full.classList.toggle("mo",o);btn.setAttribute("aria-expanded",String(o))};
  full.addEventListener("click",e=>{if(full.classList.contains("mo")&&!e.target.closest("#fMoreP,#fMore"))shut()});
  menu.addEventListener("click",e=>{if(e.target.closest("#slp,#apl,#spd"))setTimeout(shut,0)});
  new MutationObserver(()=>{if(!full.classList.contains("open"))shut()}).observe(full,{attributes:true,attributeFilter:["class"]});
  (mq.addEventListener?mq.addEventListener("change",place):mq.addListener(place));
  place();
})();
