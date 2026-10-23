/* ---------- sync UI with current track ---------- */
function bgArt(t){const c=t.c;S.bl=1-(S.bl||0);const n=S.bl;
  const css=t.img?artOf(t):t.radio&&t.logo&&t.lg?`center/34% no-repeat url(${t.logo}),radial-gradient(circle at 25% 20%,rgba(${c},1),transparent 55%),radial-gradient(circle at 80% 55%,rgba(${c},.75),transparent 55%),${art(c)}`:`radial-gradient(circle at 25% 20%,rgba(${c},1),transparent 55%),radial-gradient(circle at 80% 55%,rgba(${c},.75),transparent 55%),${art(c)}`;
  document.querySelectorAll("#hd,#sheet,#full,#lyr,#im").forEach(el=>{const L=el.querySelectorAll(".bl");L[n].style.background=css;L[n].style.opacity=1;L[1-n].style.opacity=0})}
/* the song's true colour: group the cover's pixels into hue families and take the biggest one,
   instead of averaging everything (an average of red + blue art is a muddy purple that is neither) */
function swatch(t){const im=new Image();im.crossOrigin="anonymous";im.onload=()=>{try{const N=40,c=document.createElement("canvas");c.width=c.height=N;const x=c.getContext("2d");x.drawImage(im,0,0,N,N);const d=x.getImageData(0,0,N,N).data;
  const bins={};let all=[0,0,0,0];
  for(let i=0;i<d.length;i+=4){if(d[i+3]<200)continue;const r=d[i],g=d[i+1],b=d[i+2],[h,s,l]=rgb2hsl(r,g,b);all[0]+=r;all[1]+=g;all[2]+=b;all[3]++;
    if(l<.1||l>.94)continue;
    const k=s<.12?"n"+Math.floor(l*4):Math.floor(h*12)%12,w=(.25+s*s*1.6)*(1-Math.abs(l-.5)*.9),o=bins[k]||(bins[k]={w:0,r:0,g:0,b:0,s:0});o.w+=w;o.r+=r*w;o.g+=g*w;o.b+=b*w;o.s+=s*w}
  let best=null;for(const k in bins)if(!best||bins[k].w>best.w)best=bins[k];
  let col=best?[best.r/best.w,best.g/best.w,best.b/best.w]:all[3]?[all[0]/all[3],all[1]/all[3],all[2]/all[3]]:null;if(!col)return;
  t.c=col.map(v=>Math.round(v)).join(",");saveSoon(t);if(T[S.cur]===t){$("app").style.setProperty("--acc",acc(t.c))}}catch(e){}};im.src=t.img}
function sync(){
  const t=T[S.cur];if(!t)return;S.lastIdx=S.cur;
  $("mini").hidden=false;if(S.playing)$("app").classList.add("started");$("app").style.setProperty("--acc",acc(t.c));applyWash();
  if(S.bgCur!==S.cur){S.bgCur=S.cur;S.lis=0;S.cnt=0;if(IM.on)imRender();bgArt(t);glowSet(t);lyrRender();meta(t);autoLyrics();if(t.img&&!t.sw){t.sw=1;swatch(t)}if(t.radio&&!t.artDone&&!t.lg)stationArt(t)}
  $("mArt").style.background=t.yt&&!t.img&&!S.lite?`center/cover url(https://i.ytimg.com/vi/${t.yt}/mqdefault.jpg),${art(t.c)}`:artOf(t);$("mini").classList.toggle("ytk",!!t.yt);$("mT").textContent=t.t;$("mA").textContent=t.a;
  $("big").style.background=artOf(t);$("fT").textContent=t.t;$("fA").textContent=t.a;$("fM").textContent=[t.al,t.y,t.g,t.lic].filter(Boolean).join(" · ");
  $("fD").textContent=$("dD").textContent=$("lyD").textContent=t.radio?"LIVE":fmt(t.d);$("seek").style.visibility=t.radio?"hidden":"visible";
  const ic=S.playing?PAUSE:PLAY;$("mP").innerHTML=ic;$("fP").innerHTML=ic;$("lyP").innerHTML=ic;$("dP").innerHTML=ic;$("imPpI").innerHTML=ic;$("imPpT").textContent=S.playing?"Pause":"Play";
  document.querySelectorAll("[data-i]").forEach(e=>e.classList.toggle("on",+e.dataset.i===S.cur));
  $("app").dataset.pl=S.playing?"1":"0";markNow();if(S.qopen)qRefresh();
  updCtl();prog();ambKick();msState();if(window.vzKick)vzKick();if(S.tab==="Home"&&!S.nav){if($("npHero"))npUpd();else render()}}
function prog(){const t=T[S.cur];if(!t)return;lyrSync();const f=t.radio?0:S.pos/(t.d||1);$("mB").style.width=f*100+"%";$("fC").textContent=$("dC").textContent=t.radio?"":fmt(S.pos);if(document.activeElement!==$("seek"))$("seek").value=f*1000;if(document.activeElement!==$("dSeek"))$("dSeek").value=f*1000;$("lyC").textContent=t.radio?"":fmt(S.pos);{const pp=f*100+"%";["seek","dSeek","lySeek"].forEach(i=>$(i).style.setProperty("--p",pp))}if(document.activeElement!==$("lySeek"))$("lySeek").value=f*1000;msPos();if(IM.on)imProg()}
setInterval(()=>{
  if(S.sleep&&S.sleep!=="end"&&Date.now()>=S.sleep){S.sleep=null;if(S.playing)toggle();toast("Sleep timer ended");updCtl()}
  if(!S.playing)return;const t=T[S.cur];
  S.lis=(S.lis||0)+.25;statTick();if(S.lis>=30&&!S.cnt&&t&&t.key&&!t.ph){S.cnt=1;S.plays=S.plays||{};S.plays[t.key]=(S.plays[t.key]||0)+1;save();miListen(t);if(IM.on)imRender()}
  if(t.url&&!t.radio&&!t.stream&&!S.pre&&Date.now()>=S.xfb&&S.pos>1)preload();
  if(t.yt){ytTick(t);prog();return}
  if(t.url){S.pos=A.currentTime||0;if(!t.radio&&isFinite(A.duration)&&A.duration!==t.d){t.d=A.duration;$("fD").textContent=$("dD").textContent=$("lyD").textContent=fmt(t.d);msPos(true)}prog();return}
  S.pos+=.25*S.rate;if(S.pos>=t.d){next(true);return}prog()},250);

