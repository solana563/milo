/* ===== v9: native audio engine (Android app): songs on the phone play through Media3 / ExoPlayer ===== */
/* The HTML player is still the interface. In the Android app, songs found on the phone are handed to the native
   player (background-safe, lock-screen controls, gapless). Everything else, and every other platform, uses the
   web audio path exactly as before. If the native player isn't there or errors, the web path takes over. */
var NE={ok:false,act:false,key:null,nk:null,pos:0,dur:0,at:0,pw:false,pl:false,cmd:0,rate:1};
function neP(){try{if(!NV.cap)return null;let p=NV.P&&NV.P.MiloAudio;if(!p&&window.Capacitor&&Capacitor.registerPlugin)p=Capacitor.registerPlugin("MiloAudio");return p||null}catch(e){return null}}
NE.call=(m,o)=>{const P=neP();if(!P||typeof P[m]!=="function")return Promise.resolve();try{return Promise.resolve(P[m](o||{})).catch(()=>{})}catch(e){return Promise.resolve()}};
NE.use=t=>!!(NE.ok&&t&&t.dev&&t.did&&!t.gone&&!t.nne&&!t.radio&&!t.stream&&!t.yt&&!t.ph&&LS.get("milo.native",1)!==0);
NE.cur=()=>{const t=T[S.cur];return t&&NE.act&&t.key===NE.key?t:null};
NE.item=t=>{const k=t.aid>0?"a"+t.aid:"t"+t.did,ap=DV.artp&&DV.artp.get(k);
  return{key:t.key,did:t.did,title:t.t||"",artist:t.a||"",album:t.al||"",art:ap?(/^(file|content|https?):/.test(ap)?ap:"file://"+ap):""}};
NE.eq=()=>{if(NE.ok&&NE.act)NE.call("eq",{on:!!S.eqOn,g:(S.eq&&S.eq.g||[]).slice(),f:EQF.slice()})};
NE.vol=v=>{if(NE.ok)NE.call("volume",{v:v*v})};
/* looks like an <audio> element to the rest of the app, so the progress bar, seek, speed and ended logic keep working */
const NA={
  get currentTime(){const d=NE.dur||1e9;return Math.min(d,NE.pos+(NE.pl?(Date.now()-NE.at)/1000*NE.rate:0))},
  set currentTime(v){NE.pos=+v||0;NE.at=Date.now();NE.call("seek",{pos:NE.pos})},
  get duration(){return NE.dur||NaN},get paused(){return !NE.pw},ended:false,seeking:false,readyState:4,
  get playbackRate(){return NE.rate},set playbackRate(v){NE.rate=+v||1;NE.call("rate",{v:NE.rate})},
  get defaultPlaybackRate(){return NE.rate},set defaultPlaybackRate(v){},
  pause(){NE.pw=false;NE.pl=false;NE.cmd=Date.now();NE.call("pause")},
  play(){NE.pw=true;NE.pl=true;NE.at=Date.now();NE.cmd=Date.now();NE.call("play");return Promise.resolve()},
  getAttribute(){return null},addEventListener(){},removeEventListener(){},load(){}};
function neStop(){if(!NE.act)return;NE.act=false;NE.key=null;NE.nk=null;NE.pw=NE.pl=false;NE.call("stop")}
function neEngine(i,tog){const t=T[i];
  RA.pause();RB.pause();DK.forEach(e=>e.pause());try{ytStop()}catch(e){}try{audioStop()}catch(e){}
  clearTimeout(S.xt);S.xfb=0;S.pre=null;A=NA;NE.cmd=Date.now();
  if(tog&&NE.act&&NE.key===t.key){S.playing?NA.play():NA.pause();return}
  let p=0;if(PG&&PG.seek&&PG.seek.i===i){p=PG.seek.p||0;PG.seek=null}
  NE.act=true;NE.key=t.key;NE.nk=null;NE.pos=p;NE.dur=t.d||0;NE.at=Date.now();NE.pw=NE.pl=!!S.playing;NE.rate=S.rate||1;
  NE.call("load",Object.assign(NE.item(t),{pos:Math.round(p*1000),play:!!S.playing,rate:NE.rate,vol:Math.pow(S.vol==null?1:S.vol,2)}));
  NE.eq();setTimeout(preload,400)}
var _neEng=engine;
engine=function(i,tog){const t=T[i];if(NE.use(t)){neEngine(i,tog);return}
  if(NE.act){const keep=A===NA;neStop();if(keep)A=DK[S.dk]}
  _neEng(i,tog)};
/* the next song is queued natively so the change is gapless; the app keeps the queue and is told when it happens */
var _nePre=preload;
preload=function(){
  if(NE.act&&NE.cur()){const ni=peekNext(),n=ni!=null&&S.sleep!=="end"&&NE.use(T[ni])&&ni!==S.cur?T[ni]:null,k=n?n.key:"";
    if(NE.nk!==k){NE.nk=k;NE.call("next",n?NE.item(n):{})}S.pre=null;return}
  const ni=peekNext();if(ni!=null&&NE.use(T[ni])){S.pre=null;return}
  return _nePre()};
var _neAE=applyEQ;applyEQ=function(){_neAE.apply(this,arguments);NE.eq()};
var _neSv=setVol;setVol=function(v){_neSv(v);NE.vol(S.vol)};
var _neAA=autoAdvance;autoAdvance=function(d){const ni=peekNext();if(NE.act||(ni!=null&&NE.use(T[ni])))return false;return _neAA(d)};
function neAdvance(k){const t=NE.cur();if(!t)return;const ni=T.findIndex(x=>x.key===k&&!x.gone);if(ni<0){next(true);return}
  const qi=S.q.indexOf(ni);if(qi>=0)S.qi=qi;else{S.q.splice(S.qi+1,0,ni);S.qi++}
  S.cur=ni;S.pos=0;S.lis=0;S.cnt=0;S.playing=true;NE.key=k;NE.nk=null;NE.pos=0;NE.at=Date.now();NE.pw=NE.pl=true;NE.dur=T[ni].d||0;
  pushHist(ni);sync();miFill();miNote(ni,ni);setTimeout(preload,300)}
function neEvent(e){if(!e)return;
  if(e.ev==="state"){NE.pos=e.pos||0;if(e.dur)NE.dur=e.dur;NE.at=Date.now();NE.pl=!!e.pl;
    /* buttons on the lock screen / headset / audio focus can change play state outside the app */
    if(NE.cur()&&Date.now()-NE.cmd>800&&S.playing!==!!e.pw){S.playing=!!e.pw;NE.pw=S.playing;sync()}}
  else if(e.ev==="end"){if(NE.cur()){NE.pl=false;next(true)}}
  else if(e.ev==="advance"){neAdvance(e.k)}
  else if(e.ev==="skip"){if(S.cur>=0){e.d==="prev"?prev():next(false)}}
  else if(e.ev==="err"){const t=NE.cur();if(!t)return;
    /* native couldn't play this one: hand it to the normal web player from the same spot */
    t.nne=1;const pos=NA.currentTime||0;neStop();A=DK[S.dk];PG.seek={i:S.cur,p:pos};engine(S.cur)}}
(function(){const P=neP();if(!P)return;
  try{P.addListener("ne",neEvent)}catch(e){}
  P.ready().then(r=>{NE.ok=!!(r&&r.ok);if(NE.ok){NE.vol(S.vol==null?1:S.vol);if(S.cur>=0&&T[S.cur]&&NE.use(T[S.cur])&&!S.playing){/* restored song: re-route to native, paused */engine(S.cur)}}}).catch(()=>{})})();
