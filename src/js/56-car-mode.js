/* ----- car mode ----- */
let carWL=null;
function carOpen(){if(S.cur<0){toast("Play a song first");return}let c=$("car");if(!c){c=document.createElement("div");c.id="car";$("app").appendChild(c);
  c.innerHTML=`<button class="car-x" data-car="x" aria-label="Close">✕</button><div class="car-m"><div class="car-t" id="carT"></div><div class="car-a" id="carA"></div></div>
  <div class="car-c"><button data-car="p" aria-label="Previous"><svg viewBox="0 0 24 24"><path d="M6 5h2v14H6zM20 5v14L9 12z"/></svg></button><button class="big" data-car="t" id="carP" aria-label="Play or pause"></button><button data-car="n" aria-label="Next"><svg viewBox="0 0 24 24"><path d="M16 5h2v14h-2zM4 5v14l11-7z"/></svg></button></div>
  <div class="car-b"><i id="carB"></i></div><div class="car-s"><button data-car="l" id="carL" aria-label="Like"></button></div>`;
  c.addEventListener("click",e=>{const b=e.target.closest("[data-car]");if(!b)return;const k=b.dataset.car;if(k==="x")carClose();else if(k==="p")prev();else if(k==="n")next();else if(k==="t")toggle();else if(k==="l"&&S.cur>=0){toggleLike(S.cur);carUpd()}})}
  c.hidden=false;carUpd();try{if(navigator.wakeLock)navigator.wakeLock.request("screen").then(w=>{carWL=w}).catch(()=>{})}catch(e){}}
function carClose(){const c=$("car");if(c)c.hidden=true;try{if(carWL){carWL.release();carWL=null}}catch(e){}}
function carUpd(){const c=$("car");if(!c||c.hidden)return;const t=T[S.cur];if(!t)return;$("carT").textContent=t.t;$("carA").textContent=t.a||"";$("carP").innerHTML=S.playing?PAUSE:PLAY;
  $("carL").textContent=S.likes.has(t.key)?"♥":"♡";$("carB").style.width=(t.radio?0:Math.min(100,S.pos/(t.d||1)*100))+"%"}
const _sync2=sync;sync=function(){const r=_sync2.apply(this,arguments);carUpd();return r};
const _prog2=prog;prog=function(){const r=_prog2.apply(this,arguments);if($("car")&&!$("car").hidden){const t=T[S.cur];if(t&&$("carB"))$("carB").style.width=(t.radio?0:Math.min(100,S.pos/(t.d||1)*100))+"%"}return r};
