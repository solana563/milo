/* ---------- playback engine, queue, controls ---------- */
/* Web Audio graph: decks + synth -> EQ -> compressor -> stereo width -> out */
function ctxGet(){if(S.ctx)return S.ctx;const AC=window.AudioContext||window.webkitAudioContext;if(!AC)return null;
  try{let C;try{C=new AC({latencyHint:"playback"})}catch(e){C=new AC()}S.ctx=C;const eqIn=S.eqIn=C.createGain();
    S.flt=EQF.map((f,k)=>{const b=C.createBiquadFilter();b.type=k===0?"lowshelf":k===9?"highshelf":"peaking";b.frequency.value=f;b.Q.value=1.1;return b});
    let n=eqIn;S.flt.forEach(b=>{n.connect(b);n=b});
    const cp=S.cp=C.createDynamicsCompressor();n.connect(cp);
    const sp=C.createChannelSplitter(2),mg=C.createChannelMerger(2),mid=C.createGain(),lm=C.createGain(),rm=C.createGain(),sl=C.createGain(),sr=C.createGain(),sd=S.wd=C.createGain(),inv=C.createGain(),out=C.createGain();
    lm.gain.value=rm.gain.value=sl.gain.value=.5;sr.gain.value=-.5;inv.gain.value=-1;
    cp.connect(sp);sp.connect(lm,0);sp.connect(rm,1);lm.connect(mid);rm.connect(mid);sp.connect(sl,0);sp.connect(sr,1);sl.connect(sd);sr.connect(sd);
    mid.connect(mg,0,0);sd.connect(mg,0,0);mid.connect(mg,0,1);sd.connect(inv);inv.connect(mg,0,1);mg.connect(out);const lim=C.createDynamicsCompressor();lim.threshold.value=-2;lim.knee.value=0;lim.ratio.value=20;lim.attack.value=.003;lim.release.value=.1;const mv=S.mv=C.createGain();mv.gain.value=Math.pow(S.vol==null?1:S.vol,2);out.connect(mv);mv.connect(lim);lim.connect(C.destination);S.an=C.createAnalyser();S.an.fftSize=512;S.an.smoothingTimeConstant=.5;out.connect(S.an);S.ab=new Uint8Array(S.an.frequencyBinCount);
    try{const sr=C.createMediaElementSource(RB),rg=C.createGain();sr.connect(rg);rg.connect(eqIn);S.rbg=rg}catch(e){S.rbg=null}
    S.dg=DK.map(e=>{const s=C.createMediaElementSource(e),g=C.createGain();s.connect(g);g.connect(eqIn);return g});
    applyEQ();return C}catch(e){S.ctx=null;S.dg=null;return null}}
function applyEQ(){const C=S.ctx;if(!C||!S.flt)return;const e=S.eq,t=C.currentTime,on=S.eqOn;
  S.flt.forEach((f,k)=>f.gain.setTargetAtTime(on?e.g[k]:0,t,.05));
  S.eqIn.gain.setTargetAtTime(Math.pow(10,-(on?Math.max(0,...e.g):0)*.8/20),t,.05);
  S.wd.gain.setTargetAtTime(on?e.wide:1,t,.05);
  const c=S.cp;if(on&&e.comp){c.threshold.value=-24;c.ratio.value=4;c.knee.value=12}else{c.threshold.value=0;c.ratio.value=1}}
function engine(i,tog){const t=T[i];if(t.yt){ytPlay(t,tog);return}ytStop();
  if(t.radio||t.stream){DK.forEach(e=>e.pause());audioStop();
    const msg=()=>toast(t.stream?"Couldn't stream this song here":"Couldn't play this station here");
    if(!t.nocors&&ctxGet()&&S.rbg){/* through the audio graph, so the visualiser and EQ see the real sound */
      RA.pause();if(S.ctx.state!=="running")S.ctx.resume().catch(()=>{});A=RB;if(!tog)RB.src=offU(t);RB.defaultPlaybackRate=RB.playbackRate=t.stream?S.rate:1;S.playing?RB.play().catch(()=>{}):RB.pause();return}
    RB.pause();A=RA;if(!tog)RA.src=offU(t);RA.defaultPlaybackRate=RA.playbackRate=t.stream?S.rate:1;S.playing?RA.play().catch(msg):RA.pause();return}
  RA.pause();RB.pause();
  if(t.url){ctxGet();if(S.ctx&&S.ctx.state!=="running")S.ctx.resume().catch(()=>{});unlockDecks();audioStop();
    if(!tog){clearTimeout(S.xt);S.xfb=0;S.pre=null;DK.forEach(e=>e.pause());A=DK[S.dk];
      if(S.dg){const g=S.dg[S.dk];g.gain.cancelScheduledValues(0);g.gain.setValueAtTime(1,S.ctx.currentTime)}A.src=offU(t);A.currentTime=0}
    A.defaultPlaybackRate=A.playbackRate=S.rate;
    if(S.playing)A.play().catch(()=>toast("Couldn't play this file here"));else DK.forEach(e=>e.pause());
    setTimeout(preload,600)}
  else{DK.forEach(e=>e.pause());RA.pause();RB.pause();S.playing?audioStart(i):audioStop()}}
/* gapless + crossfade: next song is preloaded on the idle deck */
function peekNext(){if(!S.q.length||S.rep===2)return null;const n=S.qi+1;if(n<S.q.length)return S.q[n];if(S.rep===1&&!S.shuf)return S.q[0];return null}
function preload(){if(S.cur<0||!S.dg||S.lite)return;const ni=peekNext();if(ni==null||!T[ni].url||T[ni].radio||T[ni].stream||ni===S.cur){S.pre=null;return}
  const k=1-S.dk;if(S.pre&&S.pre.i===ni&&S.pre.k===k)return;const e=DK[k];if(!e.paused)return;e.src=offU(T[ni]);e.load();S.pre={i:ni,k}}
function crossTo(i,dur){const C=S.ctx,k=1-S.dk,e=DK[k],old=A,og=S.dg[S.dk],ng=S.dg[k],now=C.currentTime;
  if(!(S.pre&&S.pre.i===i&&S.pre.k===k))e.src=offU(T[i]);
  e.currentTime=0;e.defaultPlaybackRate=e.playbackRate=S.rate;
  ng.gain.cancelScheduledValues(now);ng.gain.setValueAtTime(dur?0:1,now);
  if(dur){ng.gain.linearRampToValueAtTime(1,now+dur);og.gain.cancelScheduledValues(now);og.gain.setValueAtTime(og.gain.value,now);og.gain.linearRampToValueAtTime(0,now+dur)}
  e.play().catch(()=>toast("Couldn't play this file here"));
  S.dk=k;A=e;S.pre=null;S.xfb=Date.now()+dur*1000+400;clearTimeout(S.xt);
  S.xt=setTimeout(()=>{old.pause();S.xfb=0;og.gain.cancelScheduledValues(0);preload()},dur*1000+300)}
function autoAdvance(dur){const ni=peekNext();if(ni==null||!T[ni].url||T[ni].radio||T[ni].stream||!S.dg)return false;
  S.qi=(S.qi+1)%S.q.length;S.cur=ni;S.pos=0;S.playing=true;pushHist(ni);crossTo(ni,dur);sync();return true}
setInterval(()=>{if(!S.playing||S.cur<0||S.sleep==="end"||Date.now()<S.xfb)return;const t=T[S.cur];
  if(!t||!t.url||t.radio||t.stream||!isFinite(A.duration)||A.duration<=0)return;
  const xf=S.xf>0&&A.duration>S.xf*2+2,lead=xf?S.xf:S.gap?.12:0;if(!lead)return;
  if((A.duration-A.currentTime)/S.rate>lead)return;autoAdvance(xf?S.xf:0)},60);
function seekTo(x){S.pos=x;if(T[S.cur].yt){if(YTX.ready)try{YTX.p.seekTo(x,true)}catch(e){}}else if(T[S.cur].url)A.currentTime=x;prog();msPos(true)}
function start(i,auto){S.lis=0;S.cnt=0;const pc=S.cur,pt=T[pc],was=S.playing;S.cur=i;S.pos=0;S.playing=true;pushHist(i);
  const loc=x=>x&&x.url&&!x.radio&&!x.stream&&!(window.NE&&NE.use(x));
  if(!auto&&S.xf>0&&was&&S.dg&&loc(pt)&&loc(T[i])&&pc!==i)crossTo(i,Math.min(S.xf,2.5));
  else if(auto&&S.dg&&loc(pt)&&loc(T[i])&&S.pre&&S.pre.i===i&&pc!==i)crossTo(i,0);
  else engine(i);
  sync();miFill();miNote(i,pc)}
function shufQ(l,f){const a=l.filter(x=>x!==f);for(let i=a.length-1;i>0;i--){const j=Math.random()*(i+1)|0;[a[i],a[j]]=[a[j],a[i]]}return f>=0?[f,...a]:a}
function play(i,list){
  if(T[i].ph)return;
  if(i===S.cur){toggle();return}
  if(list){S.base=list.slice();S.q=S.shuf?shufQ(S.base,i):S.base.slice();S.qi=S.q.indexOf(i)}
  else{const k=S.q.indexOf(i);if(k>=0)S.qi=k;else{S.base=[i];S.q=[i];S.qi=0}}
  start(i)}
function playList(list,sh){if(!list.length)return;S.shuf=sh;save();const i=sh?list[Math.random()*list.length|0]:list[0];S.base=list.slice();S.q=sh?shufQ(S.base,i):S.base.slice();S.qi=S.q.indexOf(i);start(i)}
function toggle(){if(S.cur<0)return;S.playing=!S.playing;engine(S.cur,true);sync()}
function next(auto,err){if(!S.q.length)return;if(!auto&&!err)miSkip();
  if(auto&&S.sleep==="end"){S.sleep=null;S.playing=false;engine(S.cur,true);seekTo(0);sync();toast("Sleep timer ended");return}
  if(auto&&S.rep===2){S.pos=0;S.lis=0;S.cnt=0;engine(S.cur);sync();return}
  let n=S.qi+1;
  if(n>=S.q.length){if(S.rep===1||!auto){if(S.shuf)S.q=shufQ(S.base,-1);n=0}else{if(auto&&S.auto&&autoPick())return;S.playing=false;engine(S.cur,true);seekTo(0);sync();return}}
  S.qi=n;start(S.q[n],auto)}
function prev(){if(!S.q.length)return;if(S.pos>3||S.qi<=0){seekTo(0);return}S.qi--;start(S.q[S.qi])}
function setShuf(){S.shuf=!S.shuf;if(S.cur>=0&&S.base.length){if(S.shuf){S.q=shufQ(S.base,S.cur);S.qi=0}else{S.q=S.base.slice();S.qi=Math.max(0,S.q.indexOf(S.cur))}}save();updCtl()}
function qIns(i,nx){if(S.cur<0){play(i);return}if(nx)S.q.splice(S.qi+1,0,i);else S.q.push(i)}
function toggleLike(i){const t=T[i],k=t.key;let on;if(S.likes.has(k)){S.likes.delete(k);on=0}else{S.likes.add(k);on=1}
  if(t.stream&&on)rememberStream(i);
  if(t.radio){S.rfav=S.rfav.filter(x=>x.id!==t.sid);if(on)S.rfav.push({id:t.sid,name:t.t,url:t.url,country:t.country,tag:t.g,logo:t.logo,home:t.home||"",lg:t.lg?1:0,c:t.lg?t.c:undefined})}
  toast(t.radio?(on?"Station saved":"Station removed"):(on?"Added to Liked songs":"Removed from Liked songs"));
  save();updCtl();if(S.nav==="pl:liked"||(S.tab==="Home"&&!S.nav))render();else if(S.tab==="Discover"){refreshR();refreshM()}}
function updCtl(){$("shf").classList.toggle("on",S.shuf);const r=$("rep");r.classList.toggle("on",S.rep>0);r.innerHTML=svg(I.rep)+(S.rep===2?'<b class="bd">1</b>':"");$("dsh").classList.toggle("on",S.shuf);const r2=$("drp");r2.classList.toggle("on",S.rep>0);r2.innerHTML=r.innerHTML;
  $("spd").textContent=S.rate+"×";$("spd").classList.toggle("on",S.rate!==1);$("slp").classList.toggle("on",!!S.sleep);$("dSl").classList.toggle("on",!!S.sleep);$("imSl").classList.toggle("on",!!S.sleep);$("snd").classList.toggle("on",S.eqOn&&S.eq.n!=="Flat");
  const t=T[S.cur];if(t){const on=S.likes.has(t.key);$("fL").classList.toggle("on",on);$("imLk").classList.toggle("on",on);$("mLk").classList.toggle("on",on);$("imLt").textContent=t.radio?(on?"Saved":"Save station"):(on?"Liked":"Like");$("imLk").setAttribute("aria-pressed",on)}}

