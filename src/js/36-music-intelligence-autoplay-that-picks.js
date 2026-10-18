/* ===== music intelligence: autoplay that picks songs like the one playing (artist, genre, tempo, energy, your taste) and, online, learns from the internet ===== */
const MI={set:new Set(),q:null,target:8,rad:0,said:0,skip:{},ask:{},bpm:{},gen:{},net:{},busy:0,last:""};
try{const o=JSON.parse(localStorage.getItem("milo.mi")||"{}");MI.bpm=o.bpm||{};MI.skip=o.skip||{};MI.ask=o.ask||{};MI.gen=o.gen||{}}catch(e){}
const miSave=()=>{try{localStorage.setItem("milo.mi",JSON.stringify({bpm:MI.bpm,skip:MI.skip,ask:MI.ask,gen:MI.gen}))}catch(e){}};
const miNetOn=()=>S.miNet!==false&&navigator.onLine!==false;
const miN=s=>String(s||"").toLowerCase().replace(/r\s*&\s*b/g,"rnb").replace(/\(.*?\)|\[.*?\]/g," ").replace(/[^a-z0-9À-￿ ]+/g," ").replace(/\s+/g," ").trim();
const miArtist=t=>{const n=miN(((t&&t.a)||"").split(/,|&| feat\.? | ft\.? | x | with /i)[0]);return /^(on this device|live radio|unknown artist|unknown|various artists)$/.test(n)?"":n};
const miGenres=t=>{const g=miN((t&&(t.g||t.mg))||MI.gen[miArtist(t)]||"");const o=new Set();g.split(" ").forEach(w=>{if(w.length>1&&!/^(and|music|the|pop)$/.test(w))o.add(w==="rap"||w==="hop"?"hip":w);else if(w==="pop")o.add("pop")});return[...o]};
const miBpm=t=>(t&&(t.bpm||(MI.bpm[t.key]&&MI.bpm[t.key].b)))||0;
const miE=t=>{const r=t&&MI.bpm[t.key];return r&&r.e!=null?r.e:null};
function miSim(c,t){let s=0;const ca=miArtist(c),ta=miArtist(t);if(ca&&ca===ta)s+=4;
  const cg=miGenres(c),tg=miGenres(t);if(cg.length&&tg.length)s+=3*cg.filter(x=>tg.includes(x)).length/Math.max(cg.length,tg.length);
  if(c.al&&t.al&&c.al===t.al&&!c.stream&&!t.stream)s+=1;
  const cy=+c.y,ty=+t.y;if(cy&&ty)s+=Math.max(0,1-Math.abs(cy-ty)/10);
  const cb=miBpm(c),tb=miBpm(t);if(cb&&tb){const d=Math.min(Math.abs(cb-tb),Math.abs(cb-tb*2),Math.abs(cb*2-tb))/cb;s+=3*Math.max(0,1-d/.18)}
  const ce=miE(c),te=miE(t);if(ce!=null&&te!=null)s+=1.5*(1-Math.min(1,Math.abs(ce-te)*1.6));
  return s}
function miTaste(t){let s=0;if(S.likes.has(t.key))s+=1;const p=(S.plays&&S.plays[t.key])||0;s+=Math.min(1,Math.log(1+p)*.35);
  s-=(MI.skip[t.key]||0)*1.6;const a=miArtist(t);if(a)s-=(MI.ask[a]||0)*.5;return s}
const miKey=t=>miArtist(t)||miGenres(t).join(" ")||miN(t&&t.t);
/* choose n songs to follow `seed`: local library ranked by similarity, blended ~40% with internet suggestions */
function miPick(seed,n){const hist=new Set(S.hist.slice(0,25)),inq=new Set(S.q),seen=new Set([miN(seed.t)+"|"+miArtist(seed)]),byK=new Map();
  T.forEach(t=>byK.set(t.key,t));const recent=S.hist.slice(1,4).map(k=>byK.get(k)).filter(Boolean);
  const loc=[],onl=[];
  T.forEach((t,i)=>{if(i===S.cur||inq.has(i)||t.radio||t.ph||t.yt||t.gone||t.mi||(!t.url&&!t.n)||!showable(i)||hist.has(t.key))return;
    let s=miSim(seed,t);recent.forEach((r,k)=>{s+=miSim(r,t)*(.28/(k+1))});loc.push([s+miTaste(t)+Math.random()*.9,i])});
  if(miNetOn()){const ent=MI.net[miKey(seed)]||MI.net[MI.last];
    if(ent&&Date.now()-ent.t<72e5)ent.items.forEach(r=>{const t=T[r.i];if(!t||r.i===S.cur||inq.has(r.i)||hist.has(t.key))return;
      const base={artist:4.5,related:4,genre:3,tag:2}[r.rel]||2;onl.push([base+miSim(seed,t)*.6+(t.yt||t.d>60?.8:0)+miTaste(t)+Math.random()*1.2,r.i])})}
  loc.sort((a,b)=>b[0]-a[0]);onl.sort((a,b)=>b[0]-a[0]);
  const pools=[loc,onl],ptr=[0,0],last=[miArtist(seed)],out=[];
  const pull=p=>{const a=pools[p];while(ptr[p]<a.length){const i=a[ptr[p]++][1],t=T[i],ak=miArtist(t),key=miN(t.t)+"|"+ak;if(seen.has(key))continue;
    if(ak&&last[last.length-1]===ak&&last[last.length-2]===ak)continue;seen.add(key);last.push(ak);return i}return -1};
  while(out.length<n){const net=onl.length&&(out.length%5===1||out.length%5===3);let i=pull(net?1:0);if(i<0)i=pull(net?0:1);if(i<0)break;out.push(i)}
  return out}
/* keep a few suggested songs queued after the current one, so "Up next" is never empty */
function miFill(force){if(!(S.auto||MI.rad)||S.cur<0||S.rep===1||S.rep===2)return 0;
  if(MI.q!==S.q){MI.q=S.q;MI.set.clear();MI.target=8;MI.rad=0;MI.said=0}
  const left=S.q.length-1-S.qi;if(!force&&left>2)return 0;
  const seed=T[S.cur];if(!seed||seed.radio||seed.ph)return 0;
  miNetGo(seed);const out=miPick(seed,Math.max(1,MI.target-left));
  out.forEach(i=>{S.q.push(i);MI.set.add(i)});if(out.length)qRefresh();return out.length}
function miNote(i,pc){if(MI.q===S.q&&MI.set.has(i)&&!MI.said){MI.said=1;const p=T[pc];toast("Autoplay: songs like "+(p?(miArtist(p)?p.a.split(/,\s*/)[0]:p.t):"your music"))}}
function autoPick(){miFill(true);if(S.qi+1<S.q.length){S.qi++;start(S.q[S.qi],true);return true}return false}
function miRebuild(){if(MI.q!==S.q)return;for(let k=S.q.length-1;k>S.qi+1;k--)if(MI.set.has(S.q[k])){MI.set.delete(S.q[k]);S.q.splice(k,1)}miFill(true)}
function miRadio(i){const seed=T[i];if(!seed||seed.radio||seed.ph)return;S.base=[i];S.q=[i];S.qi=0;MI.q=S.q;MI.set.clear();MI.target=24;MI.rad=1;MI.said=1;
  miNetGo(seed);miPick(seed,23).forEach(k=>{S.q.push(k);MI.set.add(k)});start(i);toast("Radio: songs like "+(miArtist(seed)?seed.a.split(/,\s*/)[0]:seed.t))}
function miSkip(){const t=T[S.cur];if(!t||!S.playing||t.radio||!t.key||S.pos>=12)return;MI.skip[t.key]=Math.min(5,(MI.skip[t.key]||0)+1);const a=miArtist(t);if(a)MI.ask[a]=Math.min(6,(MI.ask[a]||0)+1);miSave()}
function miListen(t){const a=miArtist(t);let d=0;if(MI.skip[t.key]){MI.skip[t.key]--;d=1}if(a&&MI.ask[a]){MI.ask[a]--;d=1}if(d)miSave()}
/* the internet side: iTunes, Deezer "fans also like" and YouTube, cached for two hours per artist */
function miJsonp(u,ms=6000){return new Promise((res,rej)=>{const cb="mi_"+Math.random().toString(36).slice(2),s=document.createElement("script");let done=0,tm;
  const fin=(v,e)=>{if(done)return;done=1;clearTimeout(tm);try{delete window[cb]}catch(x){window[cb]=undefined}s.remove();e?rej(e):res(v)};
  window[cb]=d=>fin(d);s.onerror=()=>fin(0,new Error("jsonp"));tm=setTimeout(()=>fin(0,new Error("timeout")),ms);s.src=u+"&callback="+cb;document.head.appendChild(s)})}
async function miNetGo(seed){if(!miNetOn())return;const ar=miArtist(seed),ge=miGenres(seed).join(" "),k=miKey(seed);if(!k)return;
  const old=MI.net[k];if(old&&(old.p||Date.now()-old.t<72e5))return;
  const ent=MI.net[k]={t:Date.now(),p:1,items:[],gc:{}},E=encodeURIComponent;
  const add=(raw,rel)=>{const i=addStream(raw);T[i].mi=1;if(!ent.items.some(x=>x.i===i))ent.items.push({i,rel})};
  const it=async(term,rel,lim)=>{const d=await jget(`https://itunes.apple.com/search?media=music&entity=song&limit=${lim}&term=${E(term)}`,7000);
    (d.results||[]).filter(x=>x.previewUrl&&x.trackName).forEach(x=>{const same=ar&&miN(x.artistName).includes(ar);if(same&&x.primaryGenreName)ent.gc[x.primaryGenreName]=(ent.gc[x.primaryGenreName]||0)+1;
      add({id:"it"+x.trackId,t:x.trackName,a:x.artistName||"Unknown artist",d:30,url:x.previewUrl,img:(x.artworkUrl100||"").replace("100x100","600x600"),src:x.collectionName||"iTunes",lic:"30s preview",page:x.trackViewUrl||"",g:x.primaryGenreName||""},rel==="artist"&&!same?"tag":rel)})};
  const tasks=[];
  if(ar)tasks.push(it(ar,"artist",20));
  if(ar)tasks.push((async()=>{const a=await miJsonp("https://api.deezer.com/search/artist?limit=1&output=jsonp&q="+E(ar)),id=a&&a.data&&a.data[0]&&a.data[0].id;if(!id)return;
    const r=await miJsonp(`https://api.deezer.com/artist/${id}/related?limit=8&output=jsonp`),names=((r&&r.data)||[]).map(x=>x.name).filter(Boolean).slice(0,4);
    await Promise.allSettled(names.map(n=>it(n,"related",5)))})());
  const gq=(seed.g||seed.mg||MI.gen[ar]||"").trim();if(gq)tasks.push(it(gq+" music","genre",15));
  tasks.push((async()=>{const q=(ar||gq||seed.t)+(ar?"":" music"),d=await jget(`https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoCategoryId=10&videoEmbeddable=true&maxResults=12&q=${E(q)}&key=${YTK}`,7000);
    (d.items||[]).filter(x=>x.id&&x.id.videoId&&x.snippet).forEach(x=>{const sn=x.snippet,v=x.id.videoId,ti=ytDecode(sn.title),a=ytArtist(ytDecode(sn.channelTitle||""));
      add({id:"yt"+v,yt:v,t:lclean(ti)||ti,a,d:0,url:"",img:(sn.thumbnails&&(sn.thumbnails.high||sn.thumbnails.medium||sn.thumbnails.default)||{}).url||"",src:"YouTube",lic:"YouTube",page:"https://www.youtube.com/watch?v="+v,g:""},ar&&miN(a+" "+ti).includes(ar)?"artist":"tag")})})());
  await Promise.allSettled(tasks);ent.p=0;ent.t=Date.now();
  const top=Object.entries(ent.gc).sort((a,b)=>b[1]-a[1])[0];if(ar&&top&&!seed.g&&MI.gen[ar]==null){MI.gen[ar]=top[0];miSave()}
  if(ent.items.length){MI.last=k;if(S.cur>=0&&miKey(T[S.cur])===k)miRebuild()}else MI.net[k]={t:Date.now()-72e5+6e4,p:0,items:[]}}
/* tempo + energy of your own songs, worked out on this device (onset autocorrelation, half/double-time aware) */
async function miAnalyze(t){const r=await fetch(t.url),buf=await r.arrayBuffer(),AC=window.OfflineAudioContext||window.webkitOfflineAudioContext,sr=22050;
  const ctx=new AC(1,sr,sr),ab=await new Promise((res,rej)=>{const p=ctx.decodeAudioData(buf,res,rej);if(p&&p.catch)p.catch(rej)});
  const x=ab.getChannelData(0),st=Math.min(x.length*.15|0,Math.max(0,x.length-75*sr)),len=Math.min(x.length-st,75*sr);if(len<8*sr)return null;
  const W=512,H=256,N=Math.floor((len-W)/H),en=new Float32Array(N);let tot=0,zc=0;
  for(let j=0;j<N;j++){let s=0,o=st+j*H;for(let k=0;k<W;k++){const v=x[o+k];s+=v*v;if(k&&(v>=0)!==(x[o+k-1]>=0))zc++}en[j]=Math.sqrt(s/W);tot+=en[j]}
  const lg=en.map(v=>Math.log(1+100*v)),on=new Float32Array(N);for(let j=1;j<N;j++)on[j]=Math.max(0,lg[j]-lg[j-1]);
  const loc=new Float32Array(N);for(let j=0;j<N;j++){let m=0,c=0;for(let k=Math.max(0,j-21);k<Math.min(N,j+22);k++){m+=on[k];c++}loc[j]=Math.max(0,on[j]-m/c)}
  const fps=sr/H,lo=Math.ceil(fps*60/190),hi=Math.floor(fps*60/55),ac=new Float32Array(hi*2+2);
  for(let L=0;L<ac.length&&L<N-8;L++){let s=0;for(let j=0;j+L<N;j++)s+=loc[j]*loc[j+L];ac[L]=s/(N-L)}
  let best=0,bl=0;for(let L=lo;L<=hi;L++){const b=60*fps/L,pr=Math.exp(-.5*Math.pow(Math.log2(b/118)/.75,2)),v=(ac[L]+.5*(ac[L*2]||0))*pr;if(v>best){best=v;bl=L}}
  if(!bl||!ac[0])return null;let L=bl;const a0=ac[L-1],a1=ac[L],a2=ac[L+1],dn=a0-2*a1+a2;if(dn<0)L+=.5*(a0-a2)/dn;
  const rms=tot/N,e=Math.min(1,Math.pow(Math.min(1,rms/.18),.8)*.7+Math.min(1,(zc/(N*W))/.25)*.3);
  return{b:Math.round(60*fps/L),e:Math.round(e*100)/100,c:Math.round(Math.min(1,ac[bl]/ac[0])*100)/100}}
async function miIdle(){if(MI.busy||document.hidden)return;
  const t=T.find(x=>x&&x.file&&x.url&&!x.gone&&!x.miBad&&!MI.bpm[x.key]&&(x.file.size||0)<90e6);
  if(t){MI.busy=1;try{const r=await miAnalyze(t);if(r){MI.bpm[t.key]=r;t.bpm=r.b;miSave()}else t.miBad=1}catch(e){t.miBad=1}MI.busy=0;setTimeout(miIdle,1200);return}
  if(!miNetOn())return;const a=T.map(x=>x&&x.file&&!x.gone&&!x.g?miArtist(x):"").find(n=>n&&MI.gen[n]==null);
  if(a){MI.busy=1;try{const d=await jget("https://itunes.apple.com/search?media=music&entity=song&limit=6&term="+encodeURIComponent(a),7000),gc={};
    (d.results||[]).forEach(x=>{if(x.primaryGenreName&&miN(x.artistName).includes(a))gc[x.primaryGenreName]=(gc[x.primaryGenreName]||0)+1});
    const top=Object.entries(gc).sort((p,q)=>q[1]-p[1])[0];MI.gen[a]=top?top[0]:""}catch(e){MI.gen[a]=""}miSave();MI.busy=0;setTimeout(miIdle,1500)}}
setTimeout(miIdle,5000);setInterval(miIdle,20000);window.addEventListener("online",()=>setTimeout(miIdle,1500));
/* jump back to the playing song */
(function(){const ARR='<svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg>';
  const mk=(id,par)=>{let b=$(id);if(!b&&par){b=document.createElement("button");b.id=id;b.className="jmp";b.type="button";b.innerHTML=ARR+"<span>Now playing</span>";b.setAttribute("aria-label","Jump to the playing song");par.appendChild(b)}return b};
  const state=(box,b,m)=>{if(!b)return;const r=box&&box.querySelector(".li.on");if(!r||!box.offsetParent){b.classList.remove("on");return}
    const c=box.getBoundingClientRect(),q=r.getBoundingClientRect(),top=c.top+(box.id==="view"?8:0),bot=c.bottom-m;
    if(q.bottom<top+6){b.dataset.d="up";b.classList.add("on")}else if(q.top>bot){b.dataset.d="down";b.classList.add("on")}else b.classList.remove("on")};
  window.jmpGo=box=>{const r=box&&box.querySelector(".li.on");if(!r)return;r.scrollIntoView({block:"center",behavior:matchMedia("(prefers-reduced-motion:reduce)").matches?"auto":"smooth"});r.classList.remove("flash");void r.offsetWidth;r.classList.add("flash");setTimeout(()=>r.classList.remove("flash"),1300)};
  window.jmpUpd=()=>{const v=$("view"),mb=mk("jmpM",$("app"));state(v,mb,mqDesk.matches?16:180);
    const sb=$("smb"),sl=$("sml");if(sl&&sb)state(sb,mk("jmpS",sl),12);
    const p=$("mdl")&&!$("mdl").hidden&&$("mdl").querySelector(".pan"),qb=p&&p.querySelector(".jmp.q");if(qb)state(p,qb,16)};
  document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest(".jmp");if(!b)return;e.preventDefault();e.stopPropagation();
    jmpGo(b.id==="jmpM"?$("view"):b.id==="jmpS"?$("smb"):b.closest(".pan"));setTimeout(jmpUpd,500)},true);
  let rf=0;document.addEventListener("scroll",()=>{if(rf)return;rf=requestAnimationFrame(()=>{rf=0;jmpUpd()})},true);setInterval(jmpUpd,700)})();
