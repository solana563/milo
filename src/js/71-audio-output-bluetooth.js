/* ----- audio output (Bluetooth speakers and headphones, USB DACs): pick where milo. plays, pause if the device goes away, and a lyrics delay for Bluetooth lag ----- */
const AO={id:LS.get("milo.sink",""),name:LS.get("milo.sinkName",""),delay:+LS.get("milo.btdelay",0)||0};
const aoSupported=()=>typeof Audio!=="undefined"&&"setSinkId" in Audio.prototype;
function aoApply(){if(!aoSupported())return Promise.resolve(false);const id=AO.id||"default";
  return Promise.all([RA,RB,DK[0],DK[1]].map(a=>a.setSinkId?a.setSinkId(id).catch(()=>false):false)).then(()=>true)}
async function aoDevices(){try{const d=await navigator.mediaDevices.enumerateDevices();return d.filter(x=>x.kind==="audiooutput")}catch(e){return[]}}
async function aoPick(){
  if(!aoSupported()){modal(`<h3>Audio output</h3><p>This browser can't choose an output. Pick your Bluetooth speaker or headphones in your device's sound settings and milo. will follow.</p><button class="ghost" data-x>Close</button>`);return}
  if(navigator.mediaDevices&&navigator.mediaDevices.selectAudioOutput){try{const d=await navigator.mediaDevices.selectAudioOutput();AO.id=d.deviceId;AO.name=d.label||"Selected device";aoSave();await aoApply();toast("Playing on "+AO.name);return}catch(e){if(e&&e.name==="NotAllowedError")return}}
  const ds=await aoDevices();
  modal(`<h3>Audio output</h3><div class="ao">${[{deviceId:"",label:"System default"}].concat(ds.filter(d=>d.deviceId&&d.deviceId!=="default")).map(d=>`<button class="ghost" data-ao="${esc(d.deviceId)}" data-aon="${esc(d.label||"Output device")}" style="display:block;width:100%;text-align:left;margin:6px 0">${esc(d.label||"Output device")}${d.deviceId===AO.id?" ✓":""}</button>`).join("")}</div><button class="ghost" data-x>Close</button>`)}
function aoSave(){LS.set("milo.sink",AO.id);LS.set("milo.sinkName",AO.name)}
function aoCard(){return `<button class="sxb" data-ao="pick"><b>Audio output</b><small>${esc(AO.name||"System default")} · pick Bluetooth, USB or speakers</small></button>
  <label class="sxs"><span><b>Lyrics delay for Bluetooth</b><small>${AO.delay} ms · raise it until the highlighted line matches what you hear</small></span><input type="range" min="0" max="800" step="50" value="${AO.delay}" data-aod style="width:120px"></label>`}
{const _s=sxExtra;sxExtra=function(){return _s()+aoCard()}}
document.addEventListener("click",e=>{const b=e.target.closest("[data-ao]");if(!b)return;const k=b.dataset.ao;
  if(k==="pick"){aoPick();return}
  AO.id=k;AO.name=k?b.dataset.aon:"";aoSave();aoApply().then(()=>{toast(AO.id?"Playing on "+AO.name:"Using the system default");closeM()})});
document.addEventListener("input",e=>{const r=e.target.closest&&e.target.closest("[data-aod]");if(!r)return;AO.delay=+r.value;LS.set("milo.btdelay",AO.delay);const s=r.closest("label").querySelector("small");if(s)s.textContent=AO.delay+" ms · raise it until the highlighted line matches what you hear"});
{const _l=lyrSync;lyrSync=function(){const p=S.pos;if(AO.delay)S.pos=p-AO.delay/1000;try{return _l.apply(this,arguments)}finally{S.pos=p}}}
/* if the chosen device disappears (Bluetooth switched off, DAC unplugged): fall back to the default and pause, so music doesn't blast from the speaker */
if(navigator.mediaDevices&&navigator.mediaDevices.addEventListener)navigator.mediaDevices.addEventListener("devicechange",async()=>{if(!AO.id)return;const ds=await aoDevices();if(ds.length&&!ds.some(d=>d.deviceId===AO.id)){AO.id="";AO.name="";aoSave();aoApply();if(S.playing){try{toggle()}catch(x){}}toast("Output device disconnected, paused")}});
aoApply();
