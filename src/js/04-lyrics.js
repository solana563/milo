/* ---------- lyrics ---------- */
const lsynced=i=>!!T[i].lrc||(!!T[i].demo&&!!LY[i]);
function lyrRender(){const t=T[S.cur];if(!t)return;$("lyT").textContent=t.t;lyHead();$("lyArt").style.background=artOf(t);S.lk=-2;S.lg=null;S.lyInstant=true;
  const L=$("lyL"),ls=lines(S.cur),sy=lsynced(S.cur);L.classList.toggle("static",!!ls.length&&!sy);S.lyEls=[];S.lyGaps={};
  if(!ls.length){L.innerHTML=noLy(t);L.scrollTop=0;return}
  let h=sy?"":'<p class="note">Not time-synced · scroll to read</p>';
  if(sy&&lt(S.cur,0)>4)h+='<p class="gap" data-g="-1"><i></i><i></i><i></i></p>';
  ls.forEach((x,k)=>{const wd=sy&&T[S.cur].lrc[k][2];h+=`<p data-k="${k}">${wd?wd.map((q,j)=>`<span class="wd" data-w="${j}">${esc(q[1])}</span>`).join(" "):esc(x)}</p>`;if(sy&&k<ls.length-1&&lt(S.cur,k+1)-lt(S.cur,k)>9)h+=`<p class="gap" data-g="${k}"><i></i><i></i><i></i></p>`});
  L.innerHTML=h;L.scrollTop=0;
  L.querySelectorAll("[data-k]").forEach(e=>{S.lyEls[+e.dataset.k]=e});L.querySelectorAll("[data-g]").forEach(e=>{S.lyGaps[e.dataset.g]=e})}
function scrollActive(inst){const L=$("lyL");if(!L||L.classList.contains("static"))return;
  const el=S.lg!=null?(S.lyGaps||{})[S.lg]:(S.lyEls||[])[S.lk];if(!el)return;
  L.scrollTo({top:Math.max(0,el.offsetTop-L.clientHeight*.3),behavior:inst?"auto":"smooth"})}
function lyrSync(){const t=T[S.cur];if(!t)return;const ls=lines(S.cur),sy=lsynced(S.cur);let k=-1,g=null;
  if(sy)for(let j=0;j<ls.length;j++)if(S.pos>=lt(S.cur,j))k=j;
  if(sy&&ls.length){if(k<0&&lt(S.cur,0)>4)g=-1;else if(k>=0&&k<ls.length-1&&lt(S.cur,k+1)-lt(S.cur,k)>9&&S.pos>lt(S.cur,k)+6)g=k}
  lyrWords(k);if(k===S.lk&&g===S.lg)return;S.lk=k;S.lg=g;
  $("lyPk").textContent=k>=0&&ls[k]?ls[k]:ls.length?"Slide up for lyrics":(t.radio?"Live radio has no lyrics":"Slide up to find lyrics");
  (S.lyEls||[]).forEach((e,j)=>{if(!e)return;e.classList.toggle("on",j===k&&g==null);e.classList.toggle("past",j<k||(j===k&&g!=null))});
  Object.keys(S.lyGaps||{}).forEach(key=>S.lyGaps[key].classList.toggle("on",g!=null&&+key===g));
  if(Date.now()-(S.lyUser||0)>3500){const inst=S.lyInstant;S.lyInstant=false;scrollActive(inst)}}


/* word-by-word highlight for enhanced-LRC lines: words up to the playing position light up */
function lyrWords(k){const t=T[S.cur];if(!t||!t.lrc)return;if(k==null){k=-1;for(let j=0;j<t.lrc.length;j++)if(S.pos>=t.lrc[j][0])k=j}
  const e=(S.lyEls||[])[k],w=k>=0&&t.lrc[k][2];if(!e||!w)return;const sp=e.querySelectorAll(".wd");let n=0;
  for(let j=0;j<w.length;j++)if(S.pos>=w[j][0])n=j+1;
  if(e._n===n)return;e._n=n;sp.forEach((q,j)=>q.classList.toggle("sung",j<n))}
