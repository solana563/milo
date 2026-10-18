/* ---------- device back button: step back through screens instead of leaving the app ---------- */
var HB={n:0,ign:false};
function layerN(){return (!$("mdl").hidden?1:0)+($("lyr").classList.contains("open")?1:0)+(IM.on?1:0)+($("full").classList.contains("open")?1:0)+(S.nav?1:0)+(S.tab&&S.tab!=="Home"?1:0)}
function backStep(){if(!$("mdl").hidden){closeM();return true}
  if($("lyr").classList.contains("open")){lyClose();return true}
  if(IM.on){imClose();return true}
  if($("full").classList.contains("open")){$("full").classList.remove("open");return true}
  if(S.nav){S.nav=null;render();return true}
  if(S.tab&&S.tab!=="Home"){go("Home");return true}
  return false}
function histSync(){const c=layerN();
  if(c>HB.n){try{for(let i=HB.n;i<c;i++)history.pushState({milo:i+1},"")}catch(e){}HB.n=c}
  else if(c<HB.n){const k=HB.n-c;HB.n=c;HB.ign=true;try{history.go(-k)}catch(e){HB.ign=false}setTimeout(()=>{HB.ign=false},600)}}
try{history.replaceState({milo:0},"")}catch(e){}
addEventListener("popstate",()=>{if(HB.ign){HB.ign=false;return}
  if(HB.n>0){HB.n--;if(!backStep()){}histSync()}});
setInterval(histSync,250);
["mdl","lyr","full","im"].forEach(id=>{const e=$(id);if(e)new MutationObserver(histSync).observe(e,{attributes:true,attributeFilter:["class","hidden"]})});

$("imLs").innerHTML=svg(I.heart);$("mLk").innerHTML=svg(I.heart);
$("imLk").onclick=()=>{if(S.cur>=0)toggleLike(S.cur)};$("mLk").onclick=e=>{e.stopPropagation();if(S.cur>=0)toggleLike(S.cur)};
updCtl();

