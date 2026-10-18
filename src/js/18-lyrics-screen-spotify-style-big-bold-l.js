/* ---------- lyrics screen, Spotify style: big bold lines, active line in focus, breaks shown as dots ---------- */
(function(){const L=$("lyL");let rt;const rearm=()=>{clearTimeout(rt);rt=setTimeout(()=>{S.lyUser=0;scrollActive(false)},3600)};
  L.addEventListener("touchstart",()=>{S.lyUser=Date.now()},{passive:true});L.addEventListener("touchend",rearm,{passive:true});L.addEventListener("wheel",()=>{S.lyUser=Date.now();rearm()},{passive:true})})();
$("lySeek").oninput=e=>{if(S.cur>=0)seekTo(e.target.value/1000*(T[S.cur].d||0))};

