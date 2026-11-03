/* ----- premium motion: the song art and titles glide in the way you went (next = from the right, previous = from the left) ----- */
const MO={dir:1,bg:-9,view:""};
const moRed=()=>window.matchMedia&&matchMedia("(prefers-reduced-motion: reduce)").matches;
function moPlay(el,cls){if(!el)return;el.classList.remove("artF","artB","txtF","txtB");void el.offsetWidth;el.classList.add(cls)}
{const _n=next,_p=prev;next=function(){MO.dir=1;return _n.apply(this,arguments)};prev=function(){const back=!(S.pos>3||S.qi<=0);if(back)MO.dir=-1;return _p.apply(this,arguments)}}
{const _s=sync;sync=function(){const was=S.bgCur,r=_s.apply(this,arguments);
  try{if(S.bgCur!==was&&was!==undefined&&!moRed()){const f=MO.dir>=0,a=f?"artF":"artB",x=f?"txtF":"txtB";
    ["mArt","big","imA","imG","imTh","lyArt"].forEach(id=>moPlay($(id),a));
    ["mT","mA","fT","fA","imT","imAr","imAl","lyT"].forEach(id=>moPlay($(id),x));MO.dir=1}}catch(e){}return r}}
/* soft entrance when you change tab or open a page */
{const _r=render;render=function(){const r=_r.apply(this,arguments);try{const k=S.tab+"|"+S.nav+"|"+S.lib;if(k!==MO.view){MO.view=k;if(!moRed()){const v=$("view");v.classList.remove("vin");void v.offsetWidth;v.classList.add("vin")}}}catch(e){}return r}}
/* skeleton shapes used while lists and lyrics load */
const skRows=n=>`<div class="skr" aria-hidden="true">${Array.from({length:n||6},()=>'<b><i class="sk"></i><span><i class="sk"></i><i class="sk"></i></span></b>').join("")}</div>`;
const skLines=()=>'<div class="skl" aria-hidden="true">'+'<i class="sk"></i>'.repeat(5)+'</div>';
