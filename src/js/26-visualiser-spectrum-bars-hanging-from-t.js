/* ---------- visualiser: spectrum bars hanging from the top of the screen ---------- */
const VZ={n:56,v:new Float32Array(56),pk:new Float32Array(56),raf:0,t0:0,cv:null,cx:null,w:0,h:0,dpr:1,idle:0};
const IM={on:false,last:Date.now(),guard:0,pm:null,ic:"",cv:null,cx:null,w:1,h:1};
const vzReduce=()=>{try{return matchMedia("(prefers-reduced-motion: reduce)").matches}catch(e){return false}};
function vzSize(){const c=VZ.cv=VZ.cv||$("viz");if(!c)return;const r=c.getBoundingClientRect();VZ.dpr=Math.min(2,window.devicePixelRatio||1);VZ.w=Math.max(1,r.width);VZ.h=Math.max(1,r.height);
  c.width=Math.round(VZ.w*VZ.dpr);c.height=Math.round(VZ.h*VZ.dpr);VZ.cx=c.getContext("2d");if(VZ.cx)VZ.cx.setTransform(VZ.dpr,0,0,VZ.dpr,0,0);
  /* where the curved top edge of the content sheet sits inside this canvas */
  try{const sh=$("sheet").getBoundingClientRect();VZ.desk=matchMedia("(min-width:860px)").matches;VZ.sx=sh.left-r.left;VZ.sy=sh.top-r.top;VZ.sr=VZ.desk?28:32}catch(e){VZ.desk=false;VZ.sx=0;VZ.sy=VZ.h-14;VZ.sr=32}}
function vzLat(){const C=S.ctx;if(!C)return .04;const o=typeof C.outputLatency==="number"?C.outputLatency:.03;return Math.min(.4,Math.max(0,(C.baseLatency||0)+o))}
function vzSource(t){/* real spectrum from the audio graph or the room mic; a gentle generated motion only for sources the browser can't analyse */
  const out=new Float32Array(VZ.n);VZ.real=false;VZ.mic=false;
  try{
    if(S.mic){S.mic.an.getByteFrequencyData(S.mic.buf);const b=S.mic.buf,L=Math.min(b.length,96);for(let i=0;i<VZ.n;i++){const k=Math.min(L-1,Math.floor(Math.pow(i/VZ.n,1.6)*L));out[i]=Math.min(1,b[k]/255*1.5)}VZ.real=true;VZ.mic=true;return out}
    if(S.an&&t&&t.url&&!t.yt&&((!t.radio&&!t.stream)||A===RB)){S.an.getByteFrequencyData(S.ab);const b=S.ab,L=b.length,raw=new Float32Array(VZ.n);let sum=0,m=0;
      for(let i=0;i<VZ.n;i++){const k=Math.min(L-1,Math.floor(Math.pow(i/VZ.n,1.55)*L*.9)),v=b[k]/255;raw[i]=v;sum+=v;if(v>m)m=v}
      if(sum>.02){VZ.pkv=Math.max(m,(VZ.pkv||0)*.996);const g=1/Math.max(.45,VZ.pkv);for(let i=0;i<VZ.n;i++)out[i]=Math.min(1,raw[i]*g*(1+i/VZ.n*.9));VZ.real=true;return out}}}catch(e){}
  const tm=(Date.now()-VZ.t0)/1000;
  for(let i=0;i<VZ.n;i++){const f=i/VZ.n,e=Math.pow(1-f,1.3);out[i]=Math.max(0,Math.min(1,(.14+.34*e)*(.6+.4*Math.sin(tm*1.1+i*.5)*Math.sin(tm*.7+i*.23))+.03))}
  return out}
const VP=[];VZ.acc=0;VZ.lt=0;VZ.hist=[];VZ.prev=null;VZ.fa=.05;VZ.lastOn=0;VZ.real=false;
/* soft motes drift off the curved top edge of the sheet; more of them where the music is loudest, with a burst on every beat */
function vzSpawn(k){k=k||1;if(VP.length>=80)return;const w=VZ.w,r=VZ.sr,desk=VZ.desk;let u=0,best=-1;
  for(let q0=0;q0<3;q0++){const q=Math.random(),bin=Math.min(VZ.n-1,Math.round(Math.abs(q-.5)*2*(VZ.n-1))),sc=VZ.v[desk?Math.min(VZ.n-1,Math.round(q*10)):bin]+Math.random()*.15;if(sc>best){best=sc;u=q}}
  const arc=Math.PI*r/2;let px,py,nx,ny;
  if(desk){const th=Math.PI+u*Math.PI/2;px=VZ.sx+r+r*Math.cos(th);py=VZ.sy+r+r*Math.sin(th);nx=Math.cos(th);ny=Math.sin(th)}
  else{const flat=Math.max(0,VZ.w-2*r),total=2*arc+flat,d=u*total;
    if(d<arc){const th=Math.PI+d/arc*Math.PI/2;px=VZ.sx+r+r*Math.cos(th);py=VZ.sy+r+r*Math.sin(th);nx=Math.cos(th);ny=Math.sin(th)}
    else if(d<arc+flat){px=VZ.sx+r+(d-arc);py=VZ.sy;nx=0;ny=-1}
    else{const th=1.5*Math.PI+(d-arc-flat)/arc*Math.PI/2;px=VZ.sx+VZ.w-r+r*Math.cos(th);py=VZ.sy+r+r*Math.sin(th);nx=Math.cos(th);ny=Math.sin(th)}}
  const sp=(12+Math.random()*20)*k;VP.push({x:px,y:py,vx:nx*sp+(Math.random()-.5)*8,vy:ny*sp-4,age:0,life:1.1+Math.random()*1.4,s:(.8+Math.random()*1.3)*(k>1?1.25:1),ph:Math.random()*6.28,ac:Math.random()<.3})}
function vzFrame(){VZ.raf=0;const c=VZ.cv,x=VZ.cx;if(!c||!x)return;{const n0=performance.now();if(!IM.on&&VZ.lf&&n0-VZ.lf<30){VZ.raf=requestAnimationFrame(vzFrame);return}VZ.lf=n0}
  const now=performance.now(),dt=Math.min(.05,VZ.lt?(now-VZ.lt)/1000:.016);VZ.lt=now;
  const t=T[S.cur],live=!!((S.viz||IM.on)&&S.playing&&t&&!document.hidden&&!vzReduce());let burst=0,low=0,max=0;
  if(live){const a=vzSource(t);
    /* beat detection on the live frame: a sudden rise in the low bands */
    let flux=0;if(VZ.prev)for(let i=0;i<12;i++)flux+=Math.max(0,a[i]-VZ.prev[i]);VZ.prev=a;
    const on=VZ.real&&flux>VZ.fa*1.7+.18&&now-VZ.lastOn>150;VZ.fa=VZ.fa*.92+flux*.08;if(on)VZ.lastOn=now;
    VZ.hist.push({t:now,a,on,s:Math.min(1,flux/1.2)});if(VZ.hist.length>90)VZ.hist.shift()}
  else{VZ.hist.length=0;VZ.prev=null}
  /* show the frame that matches what you can hear now: the analyser sees audio a little before the speakers play it */
  let fr=null;if(VZ.hist.length){const lat=VZ.real&&!VZ.mic?vzLat():0,target=now-lat*1000,h=VZ.hist;while(h.length>1&&h[1].t<=target)h.shift();fr=h[0];if(fr.on&&!fr.f){fr.f=1;burst=.4+fr.s*.6}}
  for(let i=0;i<VZ.n;i++){const tg=fr?fr.a[i]:0,cur=VZ.v[i];VZ.v[i]=tg>cur?cur+(tg-cur)*.75:cur+(tg-cur)*.2;if(VZ.v[i]<.003)VZ.v[i]=0;max=Math.max(max,VZ.v[i]);if(i<8)low+=VZ.v[i]}
  low/=8;
  if(live){VZ.acc+=(1.2+low*9)*dt;while(VZ.acc>=1){VZ.acc--;vzSpawn(1)}if(burst){const n=3+Math.round(burst*6);for(let q=0;q<n;q++)vzSpawn(1.5+burst)}}
  x.clearRect(0,0,VZ.w,VZ.h);
  const dark=$("app").dataset.theme==="dark",ink=VZ.desk&&!dark?"138,92,31":"243,237,224",glow=t?acc(t.c):"201,151,90";
  for(let i=VP.length-1;i>=0;i--){const p=VP[i];p.age+=dt;if(p.age>=p.life){VP.splice(i,1);continue}
    p.vx+=Math.sin(p.age*3+p.ph)*5*dt;p.x+=p.vx*dt;p.y+=p.vy*dt;p.vx*=.992;p.vy*=.992;
    const f=Math.sin(Math.PI*p.age/p.life),a=f*(.22+Math.min(.28,low*.5)),rgb=p.ac?glow:ink;
    x.fillStyle=`rgba(${rgb},${(a*.22).toFixed(3)})`;x.beginPath();x.arc(p.x,p.y,p.s*3,0,6.2832);x.fill();
    x.fillStyle=`rgba(${rgb},${a.toFixed(3)})`;x.beginPath();x.arc(p.x,p.y,p.s,0,6.2832);x.fill()}
  if(IM.on)imDraw(t);
  c.classList.toggle("on",!!(S.viz&&S.cur>=0&&(S.playing||VP.length)));
  if(live||VP.length)VZ.raf=requestAnimationFrame(vzFrame);else{VZ.idle=1;VZ.lt=0}}
function vzKick(){const c=$("viz");if(!c)return;if(!VZ.cx)vzSize();if(!VZ.t0)VZ.t0=Date.now();
  if((!S.viz&&!IM.on)||vzReduce()){c.classList.remove("on");VZ.v.fill(0);if(VZ.cx)VZ.cx.clearRect(0,0,VZ.w,VZ.h);return}
  if(!VZ.raf){VZ.idle=0;VZ.raf=requestAnimationFrame(vzFrame)}}
addEventListener("resize",()=>{vzSize();vzKick()});
document.addEventListener("visibilitychange",()=>{if(!document.hidden)vzKick()});
vzSize();vzKick();

