/* ===== SimpMusic-inspired sources: SimpMusic Lyrics, BetterLyrics, SimpMusic charts, SponsorBlock, Return YouTube Dislike ===== */
const SMU={charts:null,chLoad:0,sb:{},sbLast:0,ryd:{}};
async function simpLyrics(vid){try{const d=await jget("https://api-lyrics.simpmusic.org/v1/"+encodeURIComponent(vid),7000),a=((d&&d.data)||[]).filter(x=>x&&(x.syncedLyrics||x.plainLyric)).sort((p,q)=>(q.syncedLyrics?1:0)-(p.syncedLyrics?1:0)||(q.vote||0)-(p.vote||0))[0];
  if(!a)return null;return{syncedLyrics:a.syncedLyrics||"",plainLyrics:a.plainLyric||"",src:"SimpMusic"}}catch(e){return null}}
function ttmlToLrc(x){const tm=v=>{v=String(v).trim();if(/s$/.test(v))return parseFloat(v);let s=0;v.split(":").forEach(p=>{s=s*60+parseFloat(p)});return s};
  const dec=s=>s.replace(/<[^>]+>/g,"").replace(/&amp;/g,"&").replace(/&lt;/g,"<").replace(/&gt;/g,">").replace(/&quot;/g,'"').replace(/&apos;|&#39;/g,"'").replace(/\s+/g," ").trim();
  const out=[],re=/<p\b[^>]*?\bbegin="([^"]+)"[^>]*>([\s\S]*?)<\/p>/g;let m;
  while((m=re.exec(x))){const tx=dec(m[2]),s=tm(m[1]);if(!tx||!isFinite(s))continue;const mm=Math.floor(s/60);out.push(`[${String(mm).padStart(2,"0")}:${(s-mm*60).toFixed(2).padStart(5,"0")}]${tx}`)}
  return out.join("\n")}
async function betterLyrics(ti,ar,d){try{const j=await jget(`https://lyrics-api.boidu.dev/getLyrics?s=${encodeURIComponent(ti)}&a=${encodeURIComponent(ar||"")}${d?"&d="+Math.round(d):""}`,8000),l=j&&j.ttml?ttmlToLrc(j.ttml):"";
  return l?{syncedLyrics:l,plainLyrics:"",src:"BetterLyrics"}:null}catch(e){return null}}
/* weekly charts published by SimpMusic, each one is a YouTube playlist */
async function smCharts(){if(SMU.charts||SMU.chLoad)return;SMU.chLoad=1;
  try{const d=await jget("https://chart.simpmusic.org/api/playlists",8000);SMU.charts=((d&&d.data)||[]).filter(x=>x&&x.name&&x.youtubePlaylistId).map(x=>({n:x.name,id:x.youtubePlaylistId}))}catch(e){SMU.charts=[]}
  SMU.chLoad=0;if(S.tab==="Discover"&&S.dm==="music"&&S.ms==="yt")refreshM()}
async function chartGo(id,name){S.mg=name;S.mload=1;S.mErr="";refreshM();
  try{const d=await jget(`https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=${encodeURIComponent(id)}&key=${YTK}`);
    if(S.ms!=="yt")return;
    S.mres=(d.items||[]).filter(x=>x.snippet&&x.snippet.resourceId&&x.snippet.resourceId.videoId&&!/^(Deleted|Private) video$/.test(x.snippet.title)).map(x=>{const sn=x.snippet,v=sn.resourceId.videoId,ti=ytDecode(sn.title);
      return addStream({id:"yt"+v,yt:v,t:lclean(ti)||ti,a:ytArtist(ytDecode(sn.videoOwnerChannelTitle||sn.channelTitle||"")),d:0,url:"",img:(sn.thumbnails&&(sn.thumbnails.high||sn.thumbnails.medium||sn.thumbnails.default)||{}).url||"",src:"YouTube",lic:"YouTube",page:"https://www.youtube.com/watch?v="+v,g:""})});
    if(!S.mres.length)S.mErr="This chart is empty right now."}
  catch(e){S.mErr="Couldn't load this chart. Check your connection."}
  S.mload=0;refreshM()}
document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-ch]");if(!b)return;e.preventDefault();chartGo(b.dataset.ch,b.dataset.cn||"Chart")});
const smChartRow=()=>{if(S.ms!=="yt")return"";if(!SMU.charts){smCharts();return'<p class="empty" style="margin:6px 0">Loading SimpMusic charts…</p>'}
  if(!SMU.charts.length)return"";return`<div class="row" style="margin-top:6px"><h2>SimpMusic charts</h2></div><div class="seg chips">${SMU.charts.map(c=>`<button data-ch="${esc(c.id)}" data-cn="${esc(c.n)}" class="${S.mg===c.n?"on":""}">${esc(c.n)}</button>`).join("")}</div>`};
/* SponsorBlock: skip the non-music bits of YouTube videos */
async function sbGet(vid){if(!vid||SMU.sb[vid]!==undefined)return SMU.sb[vid]||[];SMU.sb[vid]=[];
  try{const c=encodeURIComponent(JSON.stringify(["music_offtopic","sponsor","selfpromo","interaction"])),d=await jget(`https://sponsor.ajay.app/api/skipSegments?videoID=${encodeURIComponent(vid)}&categories=${c}&actionTypes=${encodeURIComponent('["skip"]')}`,6000);
    SMU.sb[vid]=(Array.isArray(d)?d:[]).map(x=>x&&x.segment).filter(s=>s&&s.length===2&&s[1]>s[0]).sort((a,b)=>a[0]-b[0])}catch(e){}return SMU.sb[vid]}
function sbSkip(t){if(S.sb===false||!t||!t.yt||!YTX.ready)return;const g=SMU.sb[t.yt];if(!g||!g.length)return;const p=S.pos,s=g.find(x=>p>=x[0]&&p<x[1]-.6);
  if(s&&Date.now()-SMU.sbLast>1500){SMU.sbLast=Date.now();try{YTX.p.seekTo(s[1],true);S.pos=s[1];toast("Skipped a non-music part")}catch(e){}}}
/* Return YouTube Dislike: likes and views for YouTube songs */
const fmtK=n=>n>=1e9?(n/1e9).toFixed(1).replace(/\.0$/,"")+"B":n>=1e6?(n/1e6).toFixed(1).replace(/\.0$/,"")+"M":n>=1e3?(n/1e3).toFixed(1).replace(/\.0$/,"")+"K":String(n);
async function rydGet(t){if(!t||!t.yt||t.ryd||t.rydTried)return;t.rydTried=1;
  try{const d=await jget("https://returnyoutubedislikeapi.com/Votes?videoId="+encodeURIComponent(t.yt),6000);if(d&&(d.likes||d.viewCount)){t.ryd={likes:d.likes||0,views:d.viewCount||0};if(IM.on&&T[S.cur]===t)imRender()}}catch(e){}}

/* Viberate music data: a snapshot of this week's charts, pulled through the Viberate connector (free plan, 3 rows per page).
   Tapping a song looks it up on YouTube Music and plays it. */
const VB={asof:"12 Oct 2026",img:"https://viberate-upload.ams3.cdn.digitaloceanspaces.com/prod/entity/artist/",
 tracks:[["BbY WOW","Karol G, Judeline, Rusowsky","Latin",38028459],["Babydoll","Jamie Miller, Antonio Cipriano","Pop",31240989],["Nicole Kidman","ADÉLA","Pop",29904123],["Patient Zero","Taylor Swift","Pop",28577970],["Ain't In LA","ADÉLA","Pop",25616347],["the cure","Olivia Rodrigo","Pop",24460681],["Beauty And A Beat","Justin Bieber, Nicki Minaj","Pop",23219560],["The One That Got Away","Katy Perry","Pop",23057329],["Earrings","Malcolm Todd","R&B",22669743]],
 artists:[["ADÉLA","adela-709-Em6rS",4255896],["Taylor Swift","taylor-swift.jpg",2934243],["Judeline","judeline-6CMIc",2136734],["Rusowsky","rusowsky-L4k8U",2037621],["Yung Lean","yung-lean.jpg",1742772],["Ashe","ashe.jpg",1661163]]};
const vbRow=()=>{if(S.ms!=="yt"||S.mqv)return"";
 return `<div class="vbh"><h2>Trending this week</h2><small>Viberate · ${VB.asof}</small></div>
 <div class="story">${VB.artists.map((a,i)=>`<button data-vba="${i}" aria-label="${esc(a[0])}"><i style="background:#2a2a32 center/cover url('${VB.img}${a[1]}')"></i><small>${esc(a[0])}</small></button>`).join("")}</div>
 <div class="vbl">${VB.tracks.map((t,i)=>`<button data-vbt="${i}" aria-label="Play ${esc(t[0])}"><span class="n">${i+1}</span><span class="x"><b>${esc(t[0])}</b><span>${esc(t[1])}</span></span><span class="s">${fmtK(t[3])}<em>STREAMS · 1W</em></span></button>`).join("")}</div>
 <p class="vbn">Spotify streams in the last week. Chart data by Viberate (free tier snapshot).</p>`};
async function vbPlay(i,btn){const t=VB.tracks[i];if(!t||(btn&&btn.classList.contains("ld")))return;if(btn)btn.classList.add("ld");toast("Finding it on YouTube…");
 try{const q=t[1].split(",")[0]+" "+t[0],d=await jget(`https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&videoEmbeddable=true&maxResults=3&q=${encodeURIComponent(q)}&key=${YTK}`);
  const x=(d.items||[]).find(v=>v.id&&v.id.videoId&&v.snippet);if(!x){toast("Couldn't find that one on YouTube");return}
  const v=x.id.videoId,ti=ytDecode(x.snippet.title),n=addStream({id:"yt"+v,yt:v,t:t[0],a:t[1].split(",")[0],d:0,url:"",img:(x.snippet.thumbnails&&(x.snippet.thumbnails.high||x.snippet.thumbnails.medium||x.snippet.thumbnails.default)||{}).url||"",src:"YouTube",lic:"YouTube",page:"https://www.youtube.com/watch?v="+v,g:t[2]==="R&B"?"R&B":t[2]});
  play(n,[n])}catch(e){toast("Couldn't reach YouTube. Check your connection.")}finally{if(btn)btn.classList.remove("ld")}}
document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-vbt],[data-vba]");if(!b)return;e.preventDefault();
 if(b.dataset.vbt!=null)vbPlay(+b.dataset.vbt,b);else{const a=VB.artists[+b.dataset.vba];if(a){const q=$("mq");if(q)q.value="";S.mqv="";musicGo(a[0],a[0]+" official")}}});

