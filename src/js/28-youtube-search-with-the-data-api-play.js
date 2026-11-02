/* ---------- YouTube: search with the Data API, play through YouTube's own embedded player ---------- */
const YTK="AIzaSyAFSMixowk9ptdAnT6ap3aK_gKO9fOmZ0E";
const YTX={p:null,ready:0,loading:0,q:[],vid:""};
function ytLoad(){return new Promise((res,rej)=>{if(window.YT&&window.YT.Player)return res();YTX.q.push(res);if(YTX.loading)return;YTX.loading=1;
  window.onYouTubeIframeAPIReady=()=>{YTX.q.splice(0).forEach(f=>f())};
  const s=document.createElement("script");s.src="https://www.youtube.com/iframe_api";s.onerror=()=>{YTX.loading=0;YTX.q=[];rej(new Error("blocked"))};document.head.appendChild(s)})}
function ytApply(){if(!YTX.ready)return;try{S.playing?YTX.p.playVideo():YTX.p.pauseVideo()}catch(e){}}
/* errors: 2 bad id, 5 player fault, 100 removed/private, 101+150 embedding off, 153 the app could not identify itself to YouTube */
function ytErr(e){const c=+(e&&e.data)||0,t=T[S.cur];YTX.errAt=Date.now();YTX.errs=(YTX.errs||0)+1;
  const sys=c===153||c===2||c===5||YTX.errs>=3,id=t&&t.yt;
  if(!sys&&YTX.errs<3){toast(c===100?"This video was removed or is private. Skipping.":"The owner turned off playing this video outside YouTube. Skipping.");setTimeout(()=>{if(T[S.cur]===t)next(true)},900);return}
  YTX.errs=0;try{YTX.p.stopVideo()}catch(x){}S.playing=false;sync();
  modal(`<h3>YouTube won't play here</h3><p class="empty">${c===153?"YouTube refused to start the video inside this app (error 153: the app could not identify itself to YouTube).":"YouTube refused several videos in a row (error "+(c||"?")+")."} milo. stopped instead of skipping through your queue. You can open this song in YouTube, or play your own music.</p>${id?`<a class="mi" href="https://www.youtube.com/watch?v=${encodeURIComponent(id)}" target="_blank" rel="noopener">Open in YouTube</a>`:""}<button class="mi" data-dv="x">Close</button>`)}
function ytState(e){const t=T[S.cur];if(!t||!t.yt)return;const d=e.data;
  if(d===0){if(Date.now()-(YTX.errAt||0)<4000)return;next(true)}
  else if(d===1){YTX.errs=0;if(!S.playing){S.playing=true;sync()}}
  else if(d===2&&S.playing&&!YTX.busy){S.playing=false;sync()}}
function ytPlay(t,tog){ytInfo(t);sbGet(t.yt);DK.forEach(e=>e.pause());RA.pause();RB.pause();audioStop();A=RA;const w=$("ytw");w.hidden=false;ytPlaceSoon();if(!IM.on&&!$("full").classList.contains("open")&&!$("lyr").classList.contains("open")){if(mqDesk.matches)imOpen();else openFull()}
  ytLoad().then(()=>{if(T[S.cur]!==t)return;
    if(!YTX.p){YTX.p=new window.YT.Player("ytp",{width:"100%",height:"100%",videoId:t.yt,playerVars:{playsinline:1,rel:0,start:Math.floor(YTX.rs||0),autoplay:S.playing?1:0,origin:/^https?:/.test(location.protocol)?location.origin:undefined,widget_referrer:/^https?:/.test(location.protocol)?location.href:undefined},
      events:{onReady:()=>{YTX.ready=1;YTX.vid=t.yt;const c=T[S.cur];try{YTX.p.setVolume(Math.round((S.vol==null?1:S.vol)*100))}catch(e){}if(c&&c.yt){if(c.yt!==YTX.vid){YTX.vid=c.yt;YTX.p.loadVideoById(c.yt)}ytApply()}},
        onStateChange:ytState,onError:ytErr}});YTX.vid=t.yt;YTX.rs=0}
    else if(YTX.ready){if(!tog||YTX.vid!==t.yt){YTX.vid=t.yt;YTX.busy=1;S.playing?YTX.p.loadVideoById(t.yt):YTX.p.cueVideoById(t.yt);setTimeout(()=>{YTX.busy=0},900)}else ytApply()}})
  .catch(()=>{toast("YouTube can't load here. Hosted previews block it, so use the installed app.");w.hidden=true;ytPlace();S.playing=false;sync()})}
function ytHome(){const w=$("ytw"),h=mqDesk.matches?$("imBg"):$("big");if(!w||!h||w.parentNode===h)return false;
  const had=!!YTX.p;let pos=0;if(had){try{pos=YTX.p.getCurrentTime()||0;YTX.p.destroy()}catch(e){}YTX.p=null;YTX.ready=0}
  if(!w.querySelector("#ytp")){const d=document.createElement("div");d.id="ytp";w.appendChild(d)}
  h.appendChild(w);
  if(had){const t=T[S.cur];if(t&&t.yt&&!w.hidden){YTX.rs=pos;setTimeout(()=>ytPlay(t,false),0)}}
  return true}
function ytPlace(){return false}
function ytKick(){}
function ytPlaceSoon(){ytHome()}
mqDesk.addEventListener("change",ytHome);
["full","lyr","im"].forEach(id=>{const e=$(id);if(e)new MutationObserver(ytPlaceSoon).observe(e,{attributes:true,attributeFilter:["class"]})});
addEventListener("resize",ytPlaceSoon);
function ytStop(){try{if(YTX.ready)YTX.p.pauseVideo()}catch(e){}const w=$("ytw");if(w)w.hidden=true;ytPlace()}
function ytTick(t){if(!YTX.ready||YTX.vid!==t.yt)return;try{S.pos=YTX.p.getCurrentTime()||0;sbSkip(t);const d=YTX.p.getDuration();if(d&&isFinite(d)&&Math.abs(d-(t.d||0))>.5){t.d=d;$("fD").textContent=$("dD").textContent=$("lyD").textContent=fmt(d);msPos(true)}}catch(e){}}
$("ytx").onclick=()=>{ytStop();if(S.playing){S.playing=false;sync()}};
const ytDecode=s=>{const d=document.createElement("textarea");d.innerHTML=s;return d.value};
const ytArtist=n=>String(n||"").replace(/\s*-\s*Topic$/i,"").replace(/VEVO$/i,"").trim()||"YouTube";

