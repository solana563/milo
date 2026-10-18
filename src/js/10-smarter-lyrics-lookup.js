/* ---------- smarter lyrics lookup ---------- */
const FEAT1=/\s*[\(\[]\s*(feat\.?|ft\.?|featuring)\s[^\)\]]*[\)\]]/gi,FEAT2=/\s+(feat\.?|ft\.?|featuring)\s.+$/i;
const lnorm=s=>String(s||"").toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g,"").replace(/&/g," and ").replace(/[^a-z0-9\s]/g," ").replace(/\s+/g," ").trim();
function lclean(s){return String(s||"").replace(/\.(mp3|m4a|flac|wav|ogg|opus)$/i,"").replace(/_+/g," ").replace(/\[[^\]]*\]/g," ")
  .replace(/\((?:[^)]*\b(?:official|video|audio|lyrics?|remaster(?:ed)?|live|hd|hq|explicit|mono|stereo|version|visualizer|bonus|deluxe)\b[^)]*)\)/gi," ").replace(/\(\s*(?:19|20)\d{2}\s*\)/g," ")
  .replace(/\s+[-–—]\s+(?:remaster(?:ed)?|live|single version|radio edit)\b.*$/i,"").replace(/\bwww\.\S+/gi," ").replace(/\b\d{2,3}\s?kbps\b/gi," ")
  .replace(/^\s*\d{1,3}\s*[-.)]\s+/,"").replace(/\s+/g," ").trim()}
const ltok=s=>new Set(lnorm(s).split(" ").filter(w=>w.length>1));
function sim(a,b){const A=ltok(a),B=ltok(b);if(!A.size||!B.size)return 0;let n=0;A.forEach(w=>{if(B.has(w))n++});return 2*n/(A.size+B.size)}
function lyPick(cs,V,t){let best=null,bs=-9;
  for(const c of cs){if(c.instrumental||!(c.syncedLyrics||c.plainLyrics))continue;let s=-9;
    for(const v of V){const ts=sim(c.trackName,v.t);if(ts<.5)continue;const as=v.a?sim(c.artistName,v.a):.3,df=t.d&&c.duration&&!t.yt?Math.abs(c.duration-t.d):null;
      if(v.a&&as<.3&&!(df!=null&&df<=4))continue;
      const sc=ts*3+(v.a?as*2:.6)+(df==null?0:df<=2?1.2:df<=5?.7:df<=12?.2:-.6);if(sc>s)s=sc}
    if(s<0)continue;if(c.syncedLyrics)s+=.35;if(s>bs){bs=s;best=c}}
  return bs>=2.4?best:null}
async function lyLookup(t){
  if(t.yt){const sr=await simpLyrics(t.yt);if(sr)return sr}
  if(t.yt)await ytInfo(t);const yv=t.yt?ytVariants(t):null;
  const a0=(t.a||"").split(/,\s*/)[0],ar=yv?yv.a:a0==="On this device"?"":lclean(a0),ti=lclean(yv?yv.t:t.t).replace(FEAT1,"").replace(FEAT2,"").trim()||t.t,base=yv?yv.base:lclean(t.fname||"");
  const V=[{t:ti,a:ar}];if(yv&&t.tags)t.tags.filter(g=>g&&g.length<=32&&lnorm(g)!==lnorm(ti)).slice(0,4).forEach(g=>V.push({t:ti,a:lclean(g)}));
  if(base){const p=base.split(/\s+-\s+/);if(p.length>1){V.push({t:p.slice(1).join(" "),a:p[0]});V.push({t:p[0],a:p.slice(1).join(" ")})}else if(lnorm(base)!==lnorm(ti))V.push({t:base,a:""})}
  const L="https://lrclib.net/api/",e=encodeURIComponent;
  const run=async urls=>{const rs=await Promise.allSettled(urls.map(u=>jget(u)));if(rs.every(r=>r.status==="rejected"))throw new Error("net");
    const m=new Map();rs.forEach(r=>{if(r.status==="fulfilled")(Array.isArray(r.value)?r.value:[r.value]).forEach(c=>{if(c&&c.id!=null)m.set(c.id,c)})});return[...m.values()]};
  const urls=[];
  if(ar){urls.push(L+`get?track_name=${e(ti)}&artist_name=${e(ar)}`+(t.al&&!t.yt?`&album_name=${e(t.al)}`:"")+(t.d&&!t.yt?`&duration=${Math.round(t.d)}`:""));urls.push(L+`search?track_name=${e(ti)}&artist_name=${e(ar)}`)}
  urls.push(L+`search?q=${e((ar+" "+ti).trim())}`,L+`search?q=${e(ti)}`);
  V.slice(1).forEach(v=>urls.push(L+`search?q=${e((v.a+" "+v.t).trim())}`));
  let r=lyPick(await run(urls),V,t);
  if(!r){try{const d=await jget("https://itunes.apple.com/search?media=music&entity=song&limit=6&term="+e((ar+" "+ti).trim()||base));
    const m=(d.results||[]).find(x=>V.some(v=>sim(x.trackName,v.t)>=.5));
    if(m)r=lyPick(await run([L+`search?track_name=${e(m.trackName)}&artist_name=${e(m.artistName)}`,L+`search?q=${e(m.artistName+" "+m.trackName)}`]),[{t:m.trackName,a:m.artistName}],t)}catch(x){}}
  if(!r)r=await betterLyrics(ti,ar,t.d);
  return r}
function lyHead(){const t=T[S.cur];if(!t)return;$("lyA").textContent=t.a+(t.lyBusy?" · Searching…":t.lsrc?" · "+t.lsrc:"")}
async function findLyrics(i,manual,force){const t=T[i];
  if(!t||(!t.url&&!t.yt)||t.radio){if(manual)toast("No online lyrics for this one");return}
  if(t.lrc&&!force){if(manual)toast("This song already has synced lyrics");return}
  t.lyTried=1;t.lyTs=Date.now();t.lyBusy=1;if(S.cur===i){if(!t.lrc&&!t.plain)$("lyL").innerHTML='<p class="none">Searching for lyrics…</p>';lyHead()}
  let msg;
  try{const r=await lyLookup(t);
    if(!r)msg=t.plain||t.lrc?"No better lyrics found online":"No lyrics found for this song";
    else{const o={};if(r.syncedLyrics){const l=parseLrc(r.syncedLyrics);if(l.length)o.lrc=l}if(!o.lrc&&r.plainLyrics)lyr(r.plainLyrics,o);
      if(o.lrc){t.lrc=o.lrc;t.plain=null;t.lsrc="Synced · "+(r.src||"online");msg="Synced lyrics found"}
      else if(o.plain&&!t.plain&&!t.lrc){t.plain=o.plain;t.lsrc=r.src||"Online";msg="Lyrics found (not timed)"}
      else msg=t.plain||t.lrc?"Only plain lyrics online, kept yours":"No lyrics found for this song"}}
  catch(e){msg="Couldn't reach the lyrics service";t.lyTried=0}
  saveSoon(t);t.lyBusy=0;if(manual)toast(msg);if(S.cur===i)lyrRender()}
function openLyrics(){$("lyr").classList.add("open");S.lyUser=0;S.lyInstant=true;S.lk=-2;lyrSync();const t=T[S.cur];if(t&&(t.url||t.yt)&&!t.radio&&!t.lrc&&!t.lyTried)findLyrics(S.cur,false)}
async function findInfo(i){const t=T[i];if(!t||!t.url||t.radio)return;
  const a0=(t.a||"").split(/,\s*/)[0],ar=a0==="On this device"?"":a0,n=x=>String(x||"").toLowerCase().replace(/[^a-z0-9]/g,"");
  try{toast("Checking iTunes…");
    const d=await jget("https://itunes.apple.com/search?media=music&entity=song&limit=8&term="+encodeURIComponent((ar+" "+t.t).trim()));
    const r=(d.results||[]).find(x=>n(x.trackName)===n(t.t)&&(!ar||n(x.artistName).includes(n(ar))||n(ar).includes(n(x.artistName))));
    if(!r){toast("No match found on iTunes");return}
    if(!t.al&&r.collectionName)t.al=r.collectionName;if(!t.y&&r.releaseDate)t.y=r.releaseDate.slice(0,4);if(!t.g&&r.primaryGenreName)t.g=r.primaryGenreName;if(!t.n&&r.trackNumber)t.n=r.trackNumber;
    if(!ar&&r.artistName)t.a=r.artistName;
    if(!t.img&&r.artworkUrl100){t.img=r.artworkUrl100.replace("100x100bb","600x600bb");t.sw=0;S.bgCur=-1}
    toast("Updated from iTunes");saveSoon(t);render()}
  catch(e){toast("Couldn't reach iTunes")}}

