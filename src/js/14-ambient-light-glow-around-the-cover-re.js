/* ---------- ambient light: glow around the cover; reacts to the song, the room's sound and the room's light ---------- */
let ambRaf=0;
const ambK=()=>[0,.75,1.3][S.amb]*(S.luxk||1);
function ambApply(){const a=$("amb");if(!a)return;a.style.display=S.amb?"":"none";a.style.setProperty("--ak",ambK())}
function micLevel(){const m=S.mic;m.an.getByteFrequencyData(m.buf);let n=0;for(let i=1;i<14;i++)n+=m.buf[i];const l=n/13/255;m.pk=Math.max(m.pk*.997,l,.05);return Math.min(1,l/m.pk)}
function ambTick(){ambRaf=0;const a=$("amb"),f=$("full");
  if(!S.amb||!f.classList.contains("open")||S.cur<0||(!S.playing&&!S.mic)){a.classList.remove("breathe");a.style.transform="";a.style.opacity="";return}
  const t=T[S.cur];let lv=null;
  if(S.mic){try{lv=micLevel()}catch(e){}}
  else if(S.an&&t&&!t.yt&&((!t.radio&&!t.stream)||A===RB)){try{S.an.getByteFrequencyData(S.ab);let n=0;for(let i=1;i<9;i++)n+=S.ab[i];lv=Math.min(1,n/8/255*1.4)}catch(e){}}
  if(lv!=null){S.lv=(S.lv||0)*.78+lv*.22;a.style.transform=`scale(${1+S.lv*.16})`;a.style.opacity=Math.min(1,(.38+S.lv*.6)*ambK());a.classList.remove("breathe")}
  else{a.classList.toggle("breathe",!!S.playing);a.style.transform="";a.style.opacity=""}
  ambRaf=requestAnimationFrame(ambTick)}
function ambKick(){if(!ambRaf)ambRaf=requestAnimationFrame(ambTick)}
function openFull(){const t=T[S.cur];if(mqDesk.matches&&t){imOpen();return}$("full").classList.add("open");ambKick()}
function micStop(){if(!S.mic)return;try{S.mic.st.getTracks().forEach(t=>t.stop());S.mic.c.close()}catch(e){}S.mic=null}
async function micToggle(){
  if(S.mic){micStop();toast("Room sound off");soundM();return}
  try{const st=await navigator.mediaDevices.getUserMedia({audio:{echoCancellation:false,noiseSuppression:false,autoGainControl:false}});
    const AC=window.AudioContext||window.webkitAudioContext,c=new AC(),src=c.createMediaStreamSource(st),an=c.createAnalyser();an.fftSize=512;an.smoothingTimeConstant=.7;src.connect(an);
    S.mic={st,c,an,buf:new Uint8Array(an.frequencyBinCount),pk:.05};toast("Listening to the room");ambKick()}
  catch(e){toast("The microphone isn't available or is blocked here")}
  soundM()}
function luxStop(){try{S.luxs&&S.luxs.stop()}catch(e){}S.luxs=null;S.lux=null;S.luxk=1;ambApply();theme()}
function luxToggle(){
  if(S.luxs){luxStop();toast("Room light off");soundM();return}
  if(!("AmbientLightSensor" in window)){toast("This browser doesn't give pages access to the light sensor");return}
  try{const s=new AmbientLightSensor({frequency:2});
    s.onreading=()=>{S.lux=s.illuminance;const k=s.illuminance<20?.7:s.illuminance<250?1:1.35;if(k!==S.luxk){S.luxk=k;ambApply()}if(S.th==="auto")theme()};
    s.onerror=()=>{toast("The light sensor is blocked or unavailable");luxStop();soundM()};s.start();S.luxs=s;toast("Reading the room's light")}
  catch(e){toast("The light sensor is blocked or unavailable")}
  soundM()}
const SIL="data:audio/wav;base64,UklGRigAAABXQVZFZm10IBIAAAABAAEARKwAAIhYAQACABAAAABkYXRhAgAAAAEA";
function unlockDecks(){if(S.unl)return;S.unl=1;const e=DK[1-S.dk];try{e.muted=true;e.src=SIL;const p=e.play();if(p&&p.then)p.then(()=>{if(!S.pre)e.pause();e.muted=false}).catch(()=>{e.muted=false});else e.muted=false}catch(x){e.muted=false}}
document.addEventListener("visibilitychange",()=>{if(!document.hidden&&S.ctx&&S.ctx.state!=="running"&&S.playing)S.ctx.resume().catch(()=>{});if(!document.hidden)ambKick()});
ambApply();

