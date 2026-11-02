/* ===== v10: YouTube Music inside milo.
   - search: songs, artists and albums from YouTube Music's own catalogue, with square album art
   - direct audio streams: the song plays in milo.'s own player (background audio, lock screen, speed, sleep timer)
     instead of YouTube's embedded player, which refuses to start inside the Android app (error 153)
   - artist, album and playlist pages with big covers, a blurred cover backdrop and poster-style artists
   Works where the app can talk to YouTube without a browser in the way: the Android app and the desktop app.
   In a plain browser or installed web app the old embedded player is used, exactly as before.
   Written for milo. from the way YouTube Music's public web API behaves. Ideas and approach credited in CREDITS.md. */
const YMC={
  REMIX:{n:"WEB_REMIX",v:"1.20260213.01.00",id:"67",ua:"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"},
  VISION:{n:"VISIONOS",v:"0.1",id:"101",ua:"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",os:"visionOS",ov:"1.3.21O771",mk:"Apple",md:"RealityDevice14,1"}};
try{const o=LS.get("milo.ymc",null);if(o&&o.REMIX)Object.assign(YMC.REMIX,o.REMIX);if(o&&o.VISION)Object.assign(YMC.VISION,o.VISION)}catch(e){}/* versions rot: a saved override can replace them without a new app */
const YMF={song:"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D",video:"EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D",album:"EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D",artist:"EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D",playlist:"EgeKAQQoAEABagoQAxAEEAoQCRAF"};
const YM={vd:LS.get("milo.ymvd",""),vdAt:+LS.get("milo.ymvdAt",0)||0,sc:new Map(),pg:new Map(),tk:0,act:null,retry:{},xr:null};
const ymEsc=s=>esc(s==null?"":String(s));
/* ---------- transport: the Android app and the desktop app can call YouTube directly ---------- */
function ymOk(){try{return !!((window.miloDesktop&&miloDesktop.http)||(NV.cap&&NV.P&&NV.P.CapacitorHttp))}catch(e){return false}}
async function ymHttp(r){
  const D=window.miloDesktop;
  if(D&&D.http)return D.http(r);
  const H=NV.P&&NV.P.CapacitorHttp;
  if(NV.cap&&H){const o=await H.request({url:r.url,method:r.method||"GET",headers:r.headers||{},data:r.body?JSON.parse(r.body):undefined,responseType:"text",connectTimeout:12000,readTimeout:20000});
    return{status:o.status,text:typeof o.data==="string"?o.data:JSON.stringify(o.data)}}
  throw new Error("no-transport")}
const ymLoc=()=>{const l=(navigator.language||"en-US").split("-");return{hl:(l[0]||"en").toLowerCase(),gl:((l[1]||"US")+"").toUpperCase().slice(0,2)}};
function ymCtx(c){const L=ymLoc(),cl={clientName:c.n,clientVersion:c.v,gl:L.gl,hl:L.hl};
  if(c.os){cl.osName=c.os;cl.osVersion=c.ov;cl.deviceMake=c.mk;cl.deviceModel=c.md}if(YM.vd)cl.visitorData=YM.vd;
  return{client:cl,request:{internalExperimentFlags:[],useSsl:true},user:{lockedSafetyMode:false}}}
function ymHead(c){const L=ymLoc(),h={"content-type":"application/json","accept":"application/json","x-goog-api-format-version":"1","x-youtube-client-name":c.id,"x-youtube-client-version":c.v,"x-origin":"https://music.youtube.com","referer":"https://music.youtube.com/","user-agent":c.ua,"accept-language":L.hl==="en"?"en-US,en;q=0.9":L.hl+",en;q=0.5"};
  if(YM.vd)h["x-goog-visitor-id"]=YM.vd;return h}
async function ymVisitor(){
  if(YM.vd&&Date.now()-YM.vdAt<6*36e5)return;
  try{const r=await ymHttp({url:"https://music.youtube.com/sw.js_data",method:"GET",headers:{"user-agent":YMC.REMIX.ua,"accept-language":"en-US,en;q=0.9"}});
    const s=r.text||"",i=s.indexOf("[");if(i<0)return;const j=JSON.parse(s.slice(i)),a=j&&j[0]&&j[0][2];
    const v=Array.isArray(a)?a.find(x=>typeof x==="string"&&/^Cg[ts]/.test(x)):"";
    if(v){YM.vd=v;YM.vdAt=Date.now();LS.set("milo.ymvd",v);LS.set("milo.ymvdAt",YM.vdAt)}}catch(e){}}
async function ymPost(path,body,c){
  await ymVisitor();
  const r=await ymHttp({url:"https://music.youtube.com/youtubei/v1/"+path+"?prettyPrint=false",method:"POST",headers:ymHead(c),body:JSON.stringify(Object.assign({context:ymCtx(c)},body))});
  if(r.status>=400){const e=new Error(String(r.status));e.status=r.status;throw e}
  return JSON.parse(r.text)}
/* ---------- reading the answers ---------- */
function ymAll(n,key,out){out=out||[];if(Array.isArray(n)){for(const x of n)ymAll(x,key,out)}else if(n&&typeof n==="object"){for(const k in n){if(k===key)out.push(n[k]);ymAll(n[k],key,out)}}return out}
function ymFirst(n,key){return ymAll(n,key)[0]}
const ymRuns=v=>((v&&v.runs)||[]).map(r=>r.text||"").join("");
function ymThumbOf(n){const a=ymAll(n,"thumbnails");for(const t of a){if(Array.isArray(t)&&t.length)return t[t.length-1].url||""}return""}
function ymStr(n,key){const a=ymAll(n,key);return a.find(x=>typeof x==="string")||""}
const ymDur=s=>{const p=String(s||"").split(":").map(Number);return p.some(isNaN)?0:p.reduce((a,x)=>a*60+x,0)};
const ymIsDur=s=>/^\d+(:\d{2}){1,2}$/.test(String(s||"").trim());
const YM_TYPES=/^(song|video|album|single|ep|playlist|artist|podcast|episode|profile|station)$/i;
function ymArtistLink(r){const b=r.navigationEndpoint&&r.navigationEndpoint.browseEndpoint;return b&&/^UC/.test(b.browseId||"")?b.browseId:""}
function ymGroups(runs){const g=[];let cur={t:"",ar:"",al:""};
  for(const r of runs){const t=r.text||"";if(t.trim()==="•"){g.push(cur);cur={t:"",ar:"",al:""}}else{cur.t+=t;const b=r.navigationEndpoint&&r.navigationEndpoint.browseEndpoint;if(b){if(/^UC/.test(b.browseId||""))cur.ar=cur.ar||b.browseId;if(/^MPRE/.test(b.browseId||""))cur.al=b.browseId}}}
  g.push(cur);g.forEach(x=>x.t=x.t.trim());return g}
/* one song row (search results, playlists, albums, an artist's top songs) */
function ymSong(n){
  const fx=i=>(((((n.flexColumns||[])[i]||{}).musicResponsiveListItemFlexColumnRenderer||{}).text)||{}).runs||[];
  const vid=(n.playlistItemData&&n.playlistItemData.videoId)||(n.navigationEndpoint&&n.navigationEndpoint.watchEndpoint&&n.navigationEndpoint.watchEndpoint.videoId)||ymStr(n.overlay,"videoId");
  const title=fx(0).map(r=>r.text||"").join("");if(!vid||!title)return null;
  let g=ymGroups(fx(1));
  if(g.length>1&&!g[0].ar&&(YM_TYPES.test(g[0].t)||g.slice(1).some(x=>x.ar)||(g.length===2&&ymIsDur(g[1].t))))g=g.slice(1);
  let dur=[...g].reverse().find(x=>ymIsDur(x.t));dur=dur?dur.t:"";
  if(!dur){const fc=(n.fixedColumns||[]).map(c=>ymRuns(((c.musicResponsiveListItemFixedColumnRenderer||{}).text)||{})).find(ymIsDur);dur=fc||""}
  const artists=g[0]&&!ymIsDur(g[0].t)?g[0].t:"";
  const albG=g.slice(1).find(x=>x.al);let alb=albG?albG.t:"",albId=albG?albG.al:"";
  if(!albId){const f=ymAll(n,"browseId").find(x=>typeof x==="string"&&/^MPRE/.test(x));if(f)albId=f}
  if(!alb){for(let i=2;i<(n.flexColumns||[]).length&&!alb;i++){const rs=fx(i);const r=rs.find(x=>x.navigationEndpoint&&x.navigationEndpoint.browseEndpoint&&/^MPRE/.test(x.navigationEndpoint.browseEndpoint.browseId||""));if(r){alb=r.text||"";albId=albId||r.navigationEndpoint.browseEndpoint.browseId}}}
  const mvt=(JSON.stringify(n).match(/MUSIC_VIDEO_TYPE_[A-Z_]+/)||[""])[0],isVideo=!!mvt&&mvt!=="MUSIC_VIDEO_TYPE_ATV";
  const ar=fx(1).map(ymArtistLink).find(Boolean)||"";
  return{k:"song",id:vid,t:title,a:artists,al:alb,alId:albId,arId:ar,d:ymDur(dur),img:ymThumbOf(n),video:isVideo,exp:/"MUSIC_EXPLICIT_BADGE"/.test(JSON.stringify(n.badges||[]))}}
/* a card in a carousel / a non-song search row: artist, album, playlist (or a song) */
function ymKind(id){return /^UC/.test(id)?"artist":/^MPRE/.test(id)?"album":/^(VL|PL|RD|OLAK)/.test(id)?"playlist":/^MPSP/.test(id)?"show":"other"}
function ymBrowse(n){const b=(n.navigationEndpoint&&n.navigationEndpoint.browseEndpoint)||ymFirst(n.navigationEndpoint||{},"browseEndpoint");return b&&b.browseId||""}
function ymCard(n,two){
  const nv=n.navigationEndpoint||{};
  if(two){const title=ymRuns(n.title),sub=ymRuns(n.subtitle),img=ymThumbOf(n);if(!title)return null;
    if(nv.watchEndpoint&&nv.watchEndpoint.videoId){const g=ymGroups((n.subtitle||{}).runs||[]);const a=g.find(x=>x.ar)||g[0]||{t:""};return{k:"song",id:nv.watchEndpoint.videoId,t:title,a:YM_TYPES.test(a.t)&&g[1]?g[1].t:a.t,img,al:"",d:0}}
    if(nv.watchPlaylistEndpoint){return{k:"playlist",id:"VL"+nv.watchPlaylistEndpoint.playlistId,t:title,sub,img}}
    const id=ymBrowse(n);if(!id)return null;return{k:ymKind(id),id,t:title,sub,img}}
  const f=i=>(((((n.flexColumns||[])[i]||{}).musicResponsiveListItemFlexColumnRenderer||{}).text)||{}).runs||[];
  const id=ymBrowse(n),title=f(0).map(r=>r.text||"").join("");if(!id||!title)return null;
  let g=ymGroups(f(1));if(g.length>1&&YM_TYPES.test(g[0].t))g=g.slice(1);
  return{k:ymKind(id),id,t:title,sub:g.map(x=>x.t).filter(Boolean).join(" · "),img:ymThumbOf(n)}}
function ymRows(root){const out=[];for(const n of ymAll(root,"musicResponsiveListItemRenderer")){const s=ymSong(n);if(s)out.push(s)}return out}
/* ---------- artwork: ask for the size a slot really shows, and step back if the CDN lacks it ---------- */
function ymArt(u,px){if(!u)return"";
  const v=/^https:\/\/i\.ytimg\.com\/vi\/([\w-]{11})\//.exec(u);if(v&&px>=720)return"https://i.ytimg.com/vi/"+v[1]+"/maxresdefault.jpg";
  if(/=w\d+-h\d+/.test(u))return u.replace(/=w\d+-h\d+/,"=w"+px+"-h"+px);
  if(/=s\d+/.test(u))return u.replace(/=s\d+/,"=s"+px);return u}
/* YouTube's own 2x2 collage of a playlist's first four songs is not a cover somebody chose */
const YM_COLLAGE=/yt3\.(ggpht|googleusercontent)\.com\/.*=s\d+/;
const ymBgImg=u=>"center/cover url(\""+String(u).replace(/"/g,"%22")+"\")";
/* hero images are checked once: a size the CDN doesn't have 404s (or comes back as a grey 120x90 picture), so use the original */
function ymFixArt(root){(root||document).querySelectorAll("[data-ya]").forEach(el=>{
  const want=el.dataset.ya,orig=el.dataset.yo;el.removeAttribute("data-ya");if(!want||want===orig)return;
  const im=new Image(),back=()=>{el.style.background=ymBgImg(orig)};
  im.onload=()=>{if(im.naturalWidth===120&&im.naturalHeight===90)back()};im.onerror=back;im.src=want})}
/* ---------- search ---------- */
async function ymSearch(q,kind){
  const j=await ymPost("search",{query:q,params:YMF[kind||"song"]},YMC.REMIX);
  if(kind==="artist"||kind==="album"||kind==="playlist"){const o=[];for(const n of ymAll(j,"musicResponsiveListItemRenderer")){const c=ymCard(n,false);if(c&&c.k===kind)o.push(c)}return o}
  return ymRows(j)}
/* songs, shaped for the Discover list */
async function ymSongs(q){
  const L=await ymSearch(q,"song");
  ymExtras(q);
  return L.slice(0,40).map(s=>({id:"yt"+s.id,yt:s.id,t:s.t,a:s.a||"YouTube Music",d:s.d||0,url:"",img:ymArt(s.img,544),src:s.al||"YouTube Music",lic:"YouTube Music",page:"https://music.youtube.com/watch?v="+s.id,g:"",ymAl:s.alId||"",ymAr:s.arId||""}))}
async function ymExtras(q){
  YM.xr={q,artists:[],albums:[],busy:1};
  try{const [ar,al]=await Promise.all([ymSearch(q,"artist").catch(()=>[]),ymSearch(q,"album").catch(()=>[])]);
    if(!YM.xr||YM.xr.q!==q)return;YM.xr={q,artists:ar.slice(0,12),albums:al.slice(0,12)}}catch(e){}
  try{refreshM()}catch(e){}}
function ymPoster(c){return`<div class="ypo" role="button" tabindex="0" data-nav="${encodeURIComponent("yt:"+c.id)}"><i style="background:${ymBgImg(ymArt(c.img,400)||"")}"></i><span><b>${ymEsc(c.t)}</b>${c.sub?`<small>${ymEsc(c.sub)}</small>`:""}</span></div>`}
function ymSquare(c){return`<div class="cd" role="button" tabindex="0" data-nav="${encodeURIComponent("yt:"+c.id)}"><i style="background:${c.img?ymBgImg(ymArt(c.img,400)):"var(--card)"}"></i><b>${ymEsc(c.t)}</b><small>${ymEsc(c.sub||"")}</small></div>`}
function ymExtraHtml(){const x=YM.xr;if(!x||x.busy||!(x.artists.length||x.albums.length))return"";
  return(x.artists.length?`<div class="row" style="margin-top:14px"><h2>Artists</h2></div><div class="pls ypls">${x.artists.map(ymPoster).join("")}</div>`:"")+
    (x.albums.length?`<div class="row"><h2>Albums</h2></div><div class="pls">${x.albums.map(ymSquare).join("")}</div>`:"")}
/* ---------- direct audio streams ---------- */
const YM_RANK={AUDIO_QUALITY_HIGH:3,AUDIO_QUALITY_MEDIUM:2,AUDIO_QUALITY_LOW:1};
function ymPick(sd){
  let a=(sd.adaptiveFormats||[]).filter(f=>!f.width&&f.url&&!f.signatureCipher&&!f.cipher);
  if(!a.length)a=(sd.formats||[]).filter(f=>!f.width&&f.url&&!f.signatureCipher&&!f.cipher);
  if(!a.length)return null;
  const orig=a.filter(f=>!f.audioTrack||(f.audioTrack.isAutoDubbed!==true&&f.audioTrack.audioIsDefault===true));if(orig.length)a=orig;
  const au=new Audio(),can=a.filter(f=>{try{return au.canPlayType(f.mimeType)!==""}catch(e){return true}});if(can.length)a=can;
  const score=f=>[YM_RANK[f.audioQuality]||0,f.audioChannels||2,/opus/.test(f.mimeType)?2:/mp4a/.test(f.mimeType)?1:0,+f.bitrate||0];
  return a.sort((x,y)=>{const p=score(x),q=score(y);for(let i=0;i<p.length;i++)if(p[i]!==q[i])return q[i]-p[i];return 0})[0]}
async function ymStream(vid,force){
  const c=YM.sc.get(vid);if(c&&!force&&c.exp>Date.now()+90000)return c;
  const j=await ymPost("player",{videoId:vid,contentCheckOk:true,racyCheckOk:true},YMC.VISION);
  const ps=j.playabilityStatus||{};if(ps.status&&ps.status!=="OK")throw new Error(ps.reason||ps.status);
  const f=ymPick(j.streamingData||{});if(!f)throw new Error("no direct audio stream");
  const exp=Date.now()+(+((j.streamingData||{}).expiresInSeconds)||18000)*1000,vd=j.videoDetails||{};
  const s={url:f.url,mime:f.mimeType,exp,d:+vd.lengthSeconds||0,ua:YMC.VISION.ua,title:vd.title||"",author:(vd.author||"").replace(/\s*-\s*Topic$/i,"")};
  YM.sc.set(vid,s);return s}
/* where the audio element reads from. On the desktop app the bytes come through a small local proxy that asks YouTube for the
   file in bounded pieces (an open-ended download is throttled to about 2x realtime, pieces are not) and adds the headers a browser can't set */
function ymSrc(s){const D=window.miloDesktop;if(D&&D.http&&D.streamUrl)return D.streamUrl(s.url,s.ua);return s.url}
var _ymEng=engine;
engine=function(i,tog){const t=T[i];
  if(!(t&&t.yt&&ymOk()&&!t.ymFail)){YM.act=null;YM.tk++;return _ymEng(i,tog)}
  ymPlay(i,tog,t)};
async function ymPlay(i,tog,t){
  try{ytStop()}catch(e){}
  DK.forEach(e=>e.pause());RB.pause();try{audioStop()}catch(e){}A=RA;
  if(tog&&YM.act===t&&RA.getAttribute("src")){RA.defaultPlaybackRate=RA.playbackRate=S.rate;S.playing?RA.play().catch(er=>{if(er&&er.name==="NotSupportedError")ymRecover(t,1)}):RA.pause();return}
  const my=++YM.tk;YM.act=null;RA.pause();
  try{sbGet(t.yt)}catch(e){}
  const slow=setTimeout(()=>{if(my===YM.tk&&T[S.cur]===t)toast("Finding the stream…")},1600);
  let s;try{s=await ymStream(t.yt)}catch(e){clearTimeout(slow);if(my!==YM.tk||T[S.cur]!==t)return;ymFallback(i,tog,t,e);return}
  clearTimeout(slow);if(my!==YM.tk||T[S.cur]!==t)return;
  if(s.d&&!t.d){t.d=s.d;try{$("fD").textContent=$("dD").textContent=$("lyD").textContent=fmt(t.d)}catch(e){}}
  YM.act=t;RA.src=ymSrc(s);RA.defaultPlaybackRate=RA.playbackRate=S.rate;
  if(YM.rs){const p=YM.rs;YM.rs=0;const f=()=>{RA.removeEventListener("loadedmetadata",f);try{RA.currentTime=p}catch(e){}};RA.addEventListener("loadedmetadata",f)}
  S.playing?RA.play().catch(er=>{if(er&&er.name==="NotSupportedError")ymRecover(t,1)}):RA.pause();setTimeout(()=>{try{preload()}catch(e){}},600)}
/* the direct route failed: use YouTube's own player as before, and say why once */
function ymFallback(i,tog,t,e){t.ymFail=1;YM.act=null;
  if(!YM.told){YM.told=1;toast("Couldn't get a direct stream ("+String((e&&e.message)||"unknown").slice(0,60)+"). Using YouTube's player.")}
  _ymEng(i,tog)}
/* a stream URL expires or is refused: ask for a fresh one once, then fall back */
function ymRecover(t,fromPlay){
  if(YM.act!==t&&!fromPlay)return;const n=YM.retry[t.yt]=(YM.retry[t.yt]||0)+1;
  if(n>2){delete YM.retry[t.yt];ymFallback(S.cur,false,t,new Error("stream refused"));return}
  YM.rs=S.pos||0;YM.sc.delete(t.yt);YM.act=null;ymPlay(S.cur,false,t)}
{const o=RA.onerror;RA.onerror=function(ev){const t=YM.act;if(t&&A===RA&&T[S.cur]===t&&RA.getAttribute("src")){ymRecover(t);return}return o&&o.call(this,ev)};
 const p=RA.onplaying;RA.onplaying=function(ev){const t=YM.act;if(t)YM.retry[t.yt]=0;return p&&p.call(this,ev)}}
/* progress, seeking and SponsorBlock for a direct stream */
var _ymTick=ytTick;
ytTick=function(t){if(YM.act===t&&A===RA){S.pos=RA.currentTime||0;const d=RA.duration;
    if(isFinite(d)&&d>0&&Math.abs(d-(t.d||0))>.5){t.d=d;try{$("fD").textContent=$("dD").textContent=$("lyD").textContent=fmt(d);msPos(true)}catch(e){}}
    try{if(S.sb!==false){const g=(SMU.sb[t.yt]||[]).find(x=>S.pos>=x[0]&&S.pos<x[1]-.6);if(g){RA.currentTime=g[1];toast("Skipped a non-music part")}}}catch(e){}
    return}
  return _ymTick(t)};
var _ymSeek=seekTo;
seekTo=function(x){const t=T[S.cur];if(t&&YM.act===t&&A===RA){S.pos=x;try{RA.currentTime=x}catch(e){}prog();msPos(true);return}return _ymSeek(x)};
/* ---------- artist, album and playlist pages ---------- */
function ymHeader(j){return ymFirst(j,"musicImmersiveHeaderRenderer")||ymFirst(j,"musicResponsiveHeaderRenderer")||ymFirst(j,"musicDetailHeaderRenderer")||ymFirst(j,"musicEditablePlaylistDetailHeaderRenderer")||ymFirst(j,"musicVisualHeaderRenderer")||ymFirst(j,"musicHeaderRenderer")||{}}
function ymParse(id,j){
  const h=ymHeader(j),k=ymKind(id),title=ymRuns(h.title),desc=ymRuns(h.description)||ymRuns((ymFirst(h,"description")||{})),
    sub=ymRuns(h.subtitle)||ymRuns(h.straplineTextOne)||"",rows=ymRows(j);
  const sec=[];let top=[];
  if(k==="artist"){
    const list=(ymAll(j,"sectionListRenderer").map(s=>s.contents).find(Array.isArray))||[];
    for(const n of list){
      if(n.musicShelfRenderer&&!top.length){top=ymRows(n.musicShelfRenderer)}
      else if(n.musicCarouselShelfRenderer){const c=n.musicCarouselShelfRenderer,hd=ymFirst(c.header||{},"title"),items=(c.contents||[]).map(x=>x.musicTwoRowItemRenderer?ymCard(x.musicTwoRowItemRenderer,true):x.musicResponsiveListItemRenderer?(ymSong(x.musicResponsiveListItemRenderer)||null):null).filter(Boolean);
        if(items.length)sec.push({t:ymRuns(hd)||"More",items})}}
    const sb=ymFirst(h,"subscribeButtonRenderer")||{};
    return{k,id,t:title||"Artist",desc,banner:ymThumbOf(h.thumbnail||h),avatar:ymThumbOf(h.foregroundThumbnail||{}),subs:ymRuns(sb.longSubscriberCountText)||ymRuns(sb.subscriberCountText)||ymRuns(h.monthlyListenerCount)||"",top:top.length?top:rows.slice(0,10),sec,radio:(ymAll(h,"playlistId").find(x=>typeof x==="string"&&/^RD/.test(x)))||""}}
  const cover=ymThumbOf(h.thumbnail||h.thumbnails||h);
  const sg=ymGroups(((h.subtitle||{}).runs)||[]).map(x=>x.t).filter(Boolean),strap=ymRuns(h.straplineTextOne);
  const year=(sg.find(x=>/^(19|20)\d{2}$/.test(x))||""),artist=strap||(sg.find(x=>!YM_TYPES.test(x)&&!/^(19|20)\d{2}$/.test(x)&&!/song|minute|hour/i.test(x))||"");
  const meta=ymRuns(h.secondSubtitle)||sg.filter(x=>/song|minute|hour|view|track/i.test(x)).join(" · ");
  const label=sg[0]&&YM_TYPES.test(sg[0])?sg[0]:(k==="album"?"Album":"Playlist");
  return{k,id,t:title||(k==="album"?"Album":"Playlist"),artist,year,desc,cover,meta,label,rows,collage:YM_COLLAGE.test(cover||"")}}
async function ymLoad(id){
  if(YM.pg.has(id)&&!YM.pg.get(id).err)return;YM.pg.set(id,{busy:1});
  try{const j=await ymPost("browse",{browseId:id},YMC.REMIX);YM.pg.set(id,ymParse(id,j))}
  catch(e){YM.pg.set(id,{err:(e&&e.message)||"failed"})}
  if(S.nav==="yt:"+id)render()}
function ymSongIdx(s,img){return addStream({id:"yt"+s.id,yt:s.id,t:s.t,a:s.a||"",d:s.d||0,url:"",img:ymArt(s.img||img||"",544),src:s.al||"YouTube Music",lic:"YouTube Music",page:"https://music.youtube.com/watch?v="+s.id,g:"",ymAl:s.alId||"",ymAr:s.arId||""})}
function ymBack(){return'<button class="dbk" data-back aria-label="Back"><svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg></button>'}
function ymHero(o){/* o: type, title, m1, m2, img (sized), orig, round, banner */
  const big=o.banner?ymArt(o.banner,1200):ymArt(o.img,800),bd=o.banner||o.img;
  const cv=o.img?ymBgImg(ymArt(o.img,544)):"var(--card)";
  return`<div class="dhero yhero${o.banner?" ybn":""}">${bd?`<i class="dbd" style="background:${ymBgImg(o.banner?ymArt(o.banner,1200):ymArt(bd,96))}" data-ya="${ymEsc(o.banner?ymArt(o.banner,1200):ymArt(bd,96))}" data-yo="${ymEsc(bd)}"></i>`:""}
  <i class="dcv${o.round?" rd":""}" style="background:${cv}"${o.img?` data-ya="${ymEsc(ymArt(o.img,544))}" data-yo="${ymEsc(o.img)}"`:""}></i>
  <div class="dtx"><div class="dky">${ymEsc(o.type)}</div><h1>${ymEsc(o.title)}</h1><div class="dm2">${o.m1||""}${o.m2?`<small>${o.m2}</small>`:""}</div></div></div>`}
function ymActs(){const ib=(a,p,l)=>`<button class="rb" ${a} aria-label="${l}" title="${l}">${svg(p)}</button>`;
  return`<div class="dact">${ib("data-ps",I.shuf,"Shuffle")}${ib("data-pq",I.queue,"Add to queue")}${ib("data-pn",'<path d="M5 5v14l9-7zM18 5v14"/>',"Play next")}<span class="sp"></span><button class="pg" data-pa aria-label="Play">${svg(DP)}</button></div>`}
function ymRowsHtml(idx,num){S.lists.d=idx;return idx.length?`<div class="list${num?" num":" dlist"}">${idx.map((i,n)=>{const h=li(i,"d");return num?h.replace(/<i style="background:[^"]*">/,`<i>${n+1}`):h}).join("")}</div>`:'<p class="empty">No songs found here.</p>'}
function ymView(id){
  const d=YM.pg.get(id);
  if(!d||d.busy){if(!d)ymLoad(id);S.dtc="201,151,90";S.dti="";return ymBack()+`<div class="dhero yhero"><i class="dcv ysk"></i><div class="dtx"><div class="dky">YouTube Music</div><h1 class="ysk2">Loading…</h1></div></div><div class="ysklist"><i></i><i></i><i></i><i></i><i></i></div>`}
  if(d.err){S.dtc="201,151,90";S.dti="";return ymBack()+`<div class="dhero yhero"><div class="dtx"><div class="dky">YouTube Music</div><h1>Couldn't open this</h1><div class="dm2">${ymEsc(d.err)}</div></div></div><div class="dact"><button class="ghost" data-ymretry="${ymEsc(id)}">Try again</button></div>`}
  S.dtc="201,151,90";S.dti=d.t;
  if(d.k==="artist"){
    const idx=d.top.map(s=>ymSongIdx(s,d.avatar||d.banner));
    const sec=d.sec.map(s=>{const cards=s.items.map(c=>c.k==="artist"?ymPoster(c):c.k==="song"?ymSongCard(c):ymSquare(c)).join("");return`<div class="dsec">${ymEsc(s.t)}</div><div class="pls${s.items[0]&&s.items[0].k==="artist"?" ypls":""}">${cards}</div>`}).join("");
    return ymBack()+ymHero({type:"Artist",title:d.t,m1:d.subs?`<b>${ymEsc(d.subs)}</b>`:"",m2:"",img:d.avatar||d.banner,banner:d.avatar?d.banner:"",round:true})+
      ymActs().replace('</div>',(d.radio?`<button class="ghost" data-ymradio="${ymEsc(d.radio)}">Radio</button>`:"")+'</div>')+
      (d.desc?`<p class="yds">${ymEsc(d.desc)}</p>`:"")+(idx.length?`<div class="dsec">Top songs</div>`+ymRowsHtml(idx,true):"")+sec}
  const idx=d.rows.map(s=>ymSongIdx(s,d.cover)),a=d.k==="album";
  const m1=(d.artist?`<b>${ymEsc(d.artist)}</b>`:"")+(d.year?" · "+d.year:""),m2=ymEsc(d.meta||idx.length+" songs");
  return ymBack()+ymHero({type:d.label,title:d.t,m1,m2,img:d.collage&&!idx.length?d.cover:d.cover})+ymActs()+(d.desc?`<p class="yds">${ymEsc(d.desc)}</p>`:"")+ymRowsHtml(idx,a)}
function ymSongCard(c){return`<div class="cd" role="button" tabindex="0" data-ymp="${ymEsc(c.id)}" data-ymt="${ymEsc(c.t)}" data-yma="${ymEsc(c.a||"")}" data-ymi="${ymEsc(c.img||"")}"><i style="background:${c.img?ymBgImg(ymArt(c.img,400)):"var(--card)"}"></i><b>${ymEsc(c.t)}</b><small>${ymEsc(c.a||"")}</small></div>`}
var _ymDetail=detail;
detail=function(){const c=S.nav.indexOf(":");if(S.nav.slice(0,c)==="yt")return ymView(S.nav.slice(c+1));return _ymDetail()};
/* paint the extra hero bits after every render */
{const _r=render;render=function(){const r=_r.apply(this,arguments);try{ymFixArt($("view"))}catch(e){}return r}}
document.addEventListener("click",e=>{
  const p=e.target.closest&&e.target.closest("[data-ymp]");
  if(p){e.preventDefault();const i=addStream({id:"yt"+p.dataset.ymp,yt:p.dataset.ymp,t:p.dataset.ymt,a:p.dataset.yma,d:0,url:"",img:ymArt(p.dataset.ymi,544),src:"YouTube Music",lic:"YouTube Music",page:"https://music.youtube.com/watch?v="+p.dataset.ymp,g:""});playList([i],false);return}
  const r=e.target.closest&&e.target.closest("[data-ymretry]");if(r){YM.pg.delete(r.dataset.ymretry);render();return}
  const rd=e.target.closest&&e.target.closest("[data-ymradio]");
  if(rd){toast("Starting radio…");ymPost("next",{playlistId:rd.dataset.ymradio,isAudioOnly:true},YMC.REMIX).then(j=>{const L=[];for(const n of ymAll(j,"playlistPanelVideoRenderer")){const vid=n.videoId;if(!vid)continue;const by=ymRuns(n.longBylineText||n.shortBylineText).split(" • ")[0];L.push({id:vid,t:ymRuns(n.title),a:by,img:ymThumbOf(n),d:ymDur(ymRuns(n.lengthText)),al:""})}
      if(!L.length){toast("No radio for this artist");return}playList(L.slice(0,50).map(s=>ymSongIdx(s,"")),false)}).catch(()=>toast("Couldn't start radio"));return}});
/* the Discover list shows artists and albums under the songs */
{const _mr=musicRes;musicRes=function(){const h=_mr();return S.ms==="yt"&&ymOk()?h.replace(/(<div class="row" style="margin-top:14px"><h2>Search this on other services)/,ymExtraHtml()+"$1"):h}}
