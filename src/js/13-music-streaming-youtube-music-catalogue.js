/* ---------- music streaming: YouTube Music catalogue (the source SimpMusic streams from) and iTunes previews ---------- */
const ITG=[["Pop","pop"],["Hip-hop","hip hop"],["Rock","rock"],["R&B","r&b soul"],["Dance","dance"],["Country","country"],["Latin","latin"],["Afrobeats","afrobeats"],["Amapiano","amapiano"],["Jazz","jazz"],["Classical","classical"]];
const YTG=[["Trending","official music video"],["Pop","pop music"],["Hip-hop","hip hop"],["Afrobeats","afrobeats"],["Amapiano","amapiano"],["Rock","rock music"],["Lo-fi","lofi hip hop"],["Jazz","jazz"],["Classical","classical music"]];
const GEN={yt:YTG,it:ITG},DEF={yt:"Trending",it:"Pop"};
const SRCS=[["YouTube Music","yt"],["iTunes previews","it"]];
const NOTE={yt:"Songs from the YouTube Music catalogue, the same source SimpMusic streams from, played in YouTube's own embedded player, so the video stays visible. Needs a connection to youtube.com. Background playback depends on your browser. Weekly charts and synced lyrics come from SimpMusic, non-music parts are skipped with SponsorBlock, and like counts come from Return YouTube Dislike.",it:"30-second previews from the iTunes Store. Use Open original page to hear the full song in Apple Music."};
const SVC=[["Spotify","https://open.spotify.com/search/"],["Apple Music","https://music.apple.com/search?term="],["YouTube Music","https://music.youtube.com/search?q="],["Deezer","https://www.deezer.com/search/"],["Tidal","https://tidal.com/search?q="],["SoundCloud","https://soundcloud.com/search?q="],["Bandcamp","https://bandcamp.com/search?q="],["Amazon Music","https://music.amazon.com/search/"]];
function addStream(o){const key="stream:"+o.id;let i=T.findIndex(t=>t.key===key);if(i>=0)return i;
  const c=RC[[...o.id].reduce((a,ch)=>(a*31+ch.charCodeAt(0))>>>0,7)%6];
  T.push({t:o.t,a:o.a,al:o.src,g:o.g||"",y:"",d:o.d||0,c,url:o.url,img:o.img||"",rimg:!!o.img,lic:o.lic||"",page:o.page||"",stream:true,hid:true,key,sid:o.id,raw:o,ph:!!o.ph,iaid:o.iaid,yt:o.yt});return T.length-1}
function rememberStream(i){const t=T[i];if(!t||!t.stream||!t.raw||t.ph)return;if(!S.sfav.some(x=>x.id===t.sid)){S.sfav.push(t.raw);if(S.sfav.length>300)S.sfav.shift();save()}}
const radioSwitch=()=>`<div class="seg" style="margin-bottom:12px">${[["music","Free music"],["radio","Live radio"]].map(([k,n])=>`<button data-dm="${k}" class="${S.dm===k?"on":""}">${n}</button>`).join("")}</div>`;
const discoverV=()=>`<h1 style="margin-top:20px">Discover</h1>${radioSwitch()}${S.dm==="radio"?radioPane():musicPane()}`;
function musicRes(){let h=(typeof vbRow==="function"?vbRow():"")+smChartRow();
  if(S.mload)h+=(typeof skRows==="function"?skRows(6):'<p class="empty">Loading songs…</p>');else if(S.mErr)h+=`<p class="empty">${esc(S.mErr)}</p>`;
  else if(S.mres.length)h+=`<div class="row"><h2>${esc(S.mg||"Results")}</h2></div>`+list(S.mres,"mr");
  else h+='<p class="empty">Pick a genre or search to start listening for free.</p>';
  const q=encodeURIComponent(S.mqv||S.mg||"");
  return h+`<div class="row" style="margin-top:14px"><h2>Search this on other services</h2></div><div class="lnks">${SVC.map(([n,u])=>`<a href="${u}${q}" target="_blank" rel="noopener">${n}</a>`).join("")}</div>`}
const musicPane=()=>`<div class="seg chips" style="margin-bottom:12px">${SRCS.map(([n,k])=>`<button data-ms="${k}" class="${S.ms===k?"on":""}">${n}</button>`).join("")}</div>
  <input id="mq" placeholder="Search free music" value="${esc(S.mqv||"")}" autocomplete="off">
  <div class="seg chips">${GEN[S.ms].map(g=>`<button data-mg="${g[0]}" class="${S.mg===g[0]?"on":""}">${g[0]}</button>`).join("")}</div>
  <div id="mres">${musicRes()}</div><p class="empty" style="font-size:12px">${NOTE[S.ms]}</p>`;
function refreshM(){if(S.tab!=="Discover"||S.dm!=="music"||S.nav)return;const r=$("mres");if(r){r.innerHTML=musicRes();bind();sync()}document.querySelectorAll("[data-mg]").forEach(b=>b.classList.toggle("on",b.dataset.mg===S.mg))}
async function musicGo(label,q,genre){S.mg=label;S.mload=1;S.mErr="";refreshM();const src=S.ms,e=encodeURIComponent;
  try{let items=[];
    if(src==="it"){const d=await jget(`https://itunes.apple.com/search?media=music&entity=song&limit=40&term=${e(q||label)}`);
      items=(d.results||[]).filter(x=>x.previewUrl&&x.trackName).map(x=>({id:"it"+x.trackId,t:x.trackName,a:x.artistName||"Unknown artist",d:30,url:x.previewUrl,img:(x.artworkUrl100||"").replace("100x100","600x600"),src:x.collectionName||"iTunes",lic:"30s preview",page:x.trackViewUrl||"",g:x.primaryGenreName||""}))}
    else if(src==="yt"&&typeof ymOk==="function"&&ymOk()){try{items=await ymSongs(q||label)}catch(er){items=[];S.mErr="YouTube Music search failed ("+String((er&&er.message)||er).slice(0,60)+"). Try again in a moment."}}
    else if(src==="yt"){const d=await jget(`https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&videoEmbeddable=true&maxResults=30&q=${e(q||label)}&key=${YTK}`);
      items=(d.items||[]).filter(x=>x.id&&x.id.videoId&&x.snippet).map(x=>({id:"yt"+x.id.videoId,yt:x.id.videoId,t:lclean(ytDecode(x.snippet.title))||ytDecode(x.snippet.title),a:ytArtist(ytDecode(x.snippet.channelTitle)),d:0,url:"",img:(x.snippet.thumbnails&&(x.snippet.thumbnails.high||x.snippet.thumbnails.medium||x.snippet.thumbnails.default)||{}).url||"",src:"YouTube",lic:"YouTube",page:"https://www.youtube.com/watch?v="+x.id.videoId,g:""}))}
    if(src!==S.ms)return;
    S.mres=items.slice(0,40).map(addStream);if(!S.mres.length)S.mErr="No songs found. Try another search."}
  catch(err){if(src!==S.ms)return;S.mres=[];S.mErr=src==="yt"&&/^(4|5)\d\d$/.test(String(err&&err.message))?"YouTube refused the search (HTTP "+err.message+"). The API key may be restricted to other websites, or today's quota is used up.":"Couldn't reach the music service. Hosted pages can block outside connections, so try the downloaded file."}
  S.mload=0;refreshM()}
function musicBind(){const q=$("mq");let tm;
  q.oninput=()=>{clearTimeout(tm);S.mqv=q.value;tm=setTimeout(()=>{if(q.value.trim().length>1)musicGo("Results",q.value.trim())},450)};
  document.querySelectorAll("[data-mg]").forEach(b=>b.onclick=()=>{const g=GEN[S.ms].find(x=>x[0]===b.dataset.mg);q.value="";S.mqv="";musicGo(g[0],g[1])});
  document.querySelectorAll("[data-ms]").forEach(b=>b.onclick=()=>{S.ms=b.dataset.ms;S.mres=[];S.mErr="";S.mload=0;S.mqv="";S.mg=DEF[S.ms];S.mtried=0;render()});
  if(!S.mres.length&&!S.mload&&!S.mErr&&!S.mtried){S.mtried=1;const g=GEN[S.ms].find(x=>x[0]===DEF[S.ms]);musicGo(g[0],g[1])}}
function elseM(i){const t=T[i],a=t.a&&t.a!=="On this device"&&t.a!=="Live radio"?t.a:"",q=(a+" "+t.t).trim();
  modal(`<h3>Listen on other services</h3><p>These open a search for "${esc(q)}" in each service. Full songs there need that service's own account or subscription.</p>${SVC.map(([n,u])=>`<a class="mi" href="${u}${encodeURIComponent(q)}" target="_blank" rel="noopener">${n}</a>`).join("")}`)}


