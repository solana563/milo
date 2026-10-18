/* ---------- modals: menus, playlists, queue, speed, sleep ---------- */
function modal(h,focus){S.qopen=false;const m=$("mdl");m.innerHTML=`<div class="scr" data-x></div><div class="pan">${h}</div>`;m.hidden=false;requestAnimationFrame(()=>{m.classList.add("open");if(focus){const i=$("mi");if(i)setTimeout(()=>{try{i.focus({preventScroll:true})}catch(e){i.focus()}i.select()},matchMedia("(min-width:860px)").matches?60:380)}})}
function closeM(){S.qopen=false;const m=$("mdl");m.classList.remove("open");setTimeout(()=>{if(!m.classList.contains("open"))m.hidden=true},260)}
function trackMenu(i){const t=T[i],lk=S.likes.has(t.key);
  const head=`<div class="mh"><i class="art" style="background:${artOf(t)}"></i><div><b>${esc(t.t)}</b><br><small>${esc(t.a)}${miBpm(t)?" · "+miBpm(t)+" BPM":""}</small></div></div>`;
  if(t.radio){modal(head+`<button class="mi" data-a="lk:${i}">${lk?"Remove saved station":"Save station"}</button>`);return}
  modal(head+`<button class="mi" data-a="pn:${i}">Play next</button><button class="mi" data-a="aq:${i}">Add to queue</button><button class="mi" data-a="rad:${i}">Start radio</button><button class="mi" data-a="ap:${i}">Add to playlist</button>
  <button class="mi" data-a="lk:${i}">${lk?"Remove from Liked songs":"Add to Liked songs"}</button><button class="mi" data-a="shr:${i}">Share</button><button class="mi" data-a="shrc:${i}">Share as image</button>${dlOk(t)?`<button class="mi" data-a="off:${i}">${OFFB[t.url]?"Remove offline copy":"Save for offline"}</button>`:""}${t.al&&!t.stream?`<button class="mi" data-a="ga:${i}">Go to album</button>`:""}${t.stream?"":`<button class="mi" data-a="gr:${i}">Go to artist</button>`}
  ${t.url&&!t.stream?`<button class="mi" data-a="fi:${i}">Find art &amp; info (iTunes)</button>`:""}${t.url?`<button class="mi" data-a="fl:${i}">Find lyrics online</button>`:""}${t.page?`<button class="mi" data-a="op:${i}">Open original page</button>`:""}${t.yt?`<button class="mi" data-a="smu:${i}">Open in SimpMusic</button>`:""}${t.file?`<button class="mi" data-a="rmv:${i}">Remove from library</button>`:""}<button class="mi" data-a="el:${i}">Listen on other services</button>
  ${S.nav&&S.nav.startsWith("pl:")&&S.plv?`<button class="mi" data-a="rm:${i}">Remove from this playlist</button>`:""}`)}
function addPlM(i){modal(`<h3>Add to playlist</h3>${S.pls.map(p=>`<button class="mi" data-a="tp:${i}:${p.id}">${esc(p.name)}${p.keys.includes(T[i].key)?" ✓":""}</button>`).join("")}<button class="mi" data-a="np:${i}">+ New playlist</button>`)}
function newPlM(i){modal(`<h3>New playlist</h3><input id="mi" placeholder="Playlist name" maxlength="40" data-t="${i??""}"><button class="addb" data-a="mk:">Create</button>`,1)}
function mkPl(n,i){const p={id:"p"+Date.now().toString(36),name:n.trim()||"Untitled playlist",keys:[]};if(i!=null){p.keys.push(T[i].key);rememberStream(i)}S.pls.push(p);save();return p}
const queueHtml=()=>`<h3>Queue</h3>${S.q.map((i,k)=>`<div class="li${k===S.qi?" on":""}" data-a="qj:${k}" role="button"><i style="background:${artOf(T[i])}"><u class="eq"><s></s><s></s><s></s></u></i><span><b>${esc(T[i].t)}</b><small>${esc(T[i].a)}</small></span><em>${k===S.qi?"Playing":MI.q===S.q&&MI.set.has(i)?"Autoplay":""}</em></div>`).join("")}<button class="jmp q" type="button" data-d="down"><svg viewBox="0 0 24 24"><path d="M6 9l6 6 6-6"/></svg><span>Now playing</span></button>`;
function openQueue(){if(!S.q.length){toast("Nothing in the queue yet");return}modal(queueHtml());S.qopen=true;setTimeout(()=>{const p=$("mdl").querySelector(".pan"),r=p&&p.querySelector(".li.on");if(r){p.scrollTop=Math.max(0,r.offsetTop-p.clientHeight/2+r.offsetHeight/2);jmpUpd()}},60)}
function qRefresh(){const p=$("mdl").querySelector(".pan");if(!p||$("mdl").hidden||!S.qopen)return;const y=p.scrollTop;p.innerHTML=queueHtml();p.scrollTop=y}
const openSpeed=()=>modal(`<h3>Playback speed</h3>${[.75,1,1.25,1.5,2].map(r=>`<button class="mi" data-a="sp:${r}">${r}×${r===S.rate?" ✓":""}</button>`).join("")}`);
const openSleep=()=>modal(`<h3>Sleep timer</h3>${[["Off","0"],["15 minutes","15"],["30 minutes","30"],["60 minutes","60"],["End of song","end"]].map(([n,v])=>`<button class="mi" data-a="sl:${v}">${n}</button>`).join("")}`);
function goNav(v){$("full").classList.remove("open");$("lyr").classList.remove("open");S.nav=v;S.tab="Library";nav();render();$("view").scrollTop=0}
const BANDS=["32","64","125","250","500","1k","2k","4k","8k","16k"];
function soundM(){updCtl();const e=S.eq;
  modal(`<h3>Sound</h3><div class="row" style="margin:0 0 10px"><b style="font-weight:500">Equalizer</b><button class="dm" data-a="eqt:">${S.eqOn?"On":"Off"}</button></div>
  <div class="seg pr">${Object.keys(PRESETS).map(n=>`<button data-a="eq:${n}" class="${e.n===n?"on":""}">${n}</button>`).join("")}</div>
  <div class="bands">${BANDS.map((b,k)=>`<div class="bandw"><input type="range" min="-12" max="12" step=".5" value="${e.g[k]}" data-b="${k}" aria-label="${b} Hz"><small>${b}</small></div>`).join("")}</div>
  <p>${S.cur>=0&&(T[S.cur].radio||T[S.cur].stream)?"Streams and radio get the EQ only when their server allows it, and crossfade never applies to them.":"EQ applies to your songs and the demo songs. Dolby and IMAX here are sound profiles that widen and fatten the mix, not licensed Dolby or IMAX processing."}</p>
  <div class="row" style="margin:6px 0 10px"><b style="font-weight:500">Crossfade</b></div>
  <div class="seg">${[[0,"Off"],[3,"3s"],[6,"6s"],[9,"9s"],[12,"12s"]].map(([v,n])=>`<button data-a="xf:${v}" class="${S.xf===v?"on":""}">${n}</button>`).join("")}</div>
  <div class="row" style="margin:20px 0 10px"><b style="font-weight:500">Gapless playback</b><button class="dm" data-a="gp:">${S.gap?"On":"Off"}</button></div>
  <div class="row" style="margin:12px 0 6px"><b style="font-weight:500">Autoplay similar songs</b><button class="dm" data-a="auto:">${S.auto?"On":"Off"}</button></div>
  <p style="margin:2px 0 10px">Autoplay queues songs close to the one playing: same artist and genre, a similar tempo and energy, and what you like and skip.</p>
  <div class="row" style="margin:6px 0 6px"><b style="font-weight:500">Discover from the internet</b><button class="dm" data-a="minet:">${S.miNet!==false?"On":"Off"}</button></div>
  <div class="row" style="margin:14px 0 6px"><b style="font-weight:500">Skip non-music parts</b><button class="dm" data-a="sb:">${S.sb!==false?"On":"Off"}</button></div>
  <p style="margin:2px 0 10px">For YouTube songs, skips sponsor reads, intros and silent talking parts that the SponsorBlock community has marked.</p>
  <p style="margin:2px 0 10px">While you're online, autoplay also uses similar artists and songs from iTunes, Deezer and YouTube. Turn off to keep it to your own music.</p>
  <div class="row" style="margin:20px 0 6px"><b style="font-weight:500">Header motes</b><button class="dm" data-a="viz:">${S.viz?"On":"Off"}</button></div>
  <p>Soft light motes that rise from the curved edge of the header and gather where the music is loudest. For your own songs they follow the real audio; for streams and radio, which the browser keeps out of the audio analyser, they move with the room microphone if it is on, otherwise they dance to a generated rhythm.</p>
  <div class="row" style="margin:20px 0 10px"><b style="font-weight:500">Ambient light</b></div>
  <div class="seg">${[[0,"Off"],[1,"Soft"],[2,"Vivid"]].map(([v,n])=>`<button data-a="amb:${v}" class="${S.amb===v?"on":""}">${n}</button>`).join("")}</div>
  <p style="margin-top:10px">A glow around the cover in the song's colours. For your own songs it also pulses with the bass.</p>
  <div class="row" style="margin:20px 0 6px"><b style="font-weight:500">Room sound (microphone)</b><button class="dm" data-a="mic:">${S.mic?"On":"Off"}</button></div>
  <p>Makes the glow react to sound in the room, so it works with radio, streams and music playing elsewhere. Audio is analysed on your device and never recorded or sent.</p>
  <div class="row" style="margin:6px 0 6px"><b style="font-weight:500">Room light sensor</b><button class="dm" data-a="lux:">${S.luxs?"On":"Off"}</button></div>
  <p>${"AmbientLightSensor" in window?"Softens or brightens the glow with the light in the room, and switches the Auto background to dark in dim rooms.":"This browser doesn't give pages access to a light sensor, so this has no effect here."}</p>`)}
function act(k,x){
  if(k==="cpl"){closeM();playList(S.cidx||[],false)}
  else if(k==="csh"){closeM();playList(S.cidx||[],true)}
  else if(k==="cpn"){closeM();qAddList(S.cidx,true)}
  else if(k==="cqu"){closeM();qAddList(S.cidx,false)}
  else if(k==="cap")addListPlM();
  else if(k==="tpl"){const p=S.pls.find(q=>q.id===x);if(p){addKeys(p,S.cidx||[]);toast("Added to "+p.name)}closeM();render()}
  else if(k==="npl"){S.pendList=(S.cidx||[]).slice();newPlM(null)}
  else if(k==="cop"){closeM();S.nav=decodeURIComponent(x);render();$("view").scrollTop=0}
  else if(k==="rmv"){closeM();removeSong(+x)}
  else if(k==="clr"){closeM();clearLibrary()}
  else if(k==="op"){try{window.open(T[+x].page,"_blank","noopener")}catch(e){}closeM()}
  else if(k==="el")elseM(+x);
  else if(k==="su")doSignUp();
  else if(k==="si")doSignIn();
  else if(k==="gsu")signUpM();
  else if(k==="gsi")signInM();
  else if(k==="avc"){const a=S.ed.av;S.ed.name=$("mi").value;S.ed.av=a.t==="emoji"?{...a,c:x}:{t:"color",c:x};editM()}
  else if(k==="ave"){S.ed.name=$("mi").value;S.ed.av={t:"emoji",e:x,c:S.ed.av.c||AVC[0]};editM()}
  else if(k==="avi"){S.ed.name=$("mi").value;S.ed.av={t:"color",c:S.ed.av.c||AVC[0]};editM()}
  else if(k==="edsave")edSave();
  else if(k==="delc"&&S.acct&&S.acct.cid){delAcct()}else if(k==="rmfc"){rmFriend(x)}else if(k==="dnap"){if(S.dnaIn){closeM();dnaPlay(S.dnaIn)}}else if(k==="delc"){const id=S.acct.id;S.accts=S.accts.filter(a=>a.id!==id);LS.set("milo.accts",S.accts);try{localStorage.removeItem("milo.v2."+id)}catch(e){}S.acct=null;LS.set("milo.cur","");closeM();toast("Account deleted");afterSwitch()}
  else if(k==="eq"){S.eq={n:x,...JSON.parse(JSON.stringify(PRESETS[x]))};S.eqOn=true;applyEQ();save();soundM()}
  else if(k==="eqt"){S.eqOn=!S.eqOn;applyEQ();save();soundM()}
  else if(k==="ps"){S.ps=x;save();setStyle();soundM()}
  else if(k==="mic")micToggle();
  else if(k==="lux")luxToggle();
  else if(k==="clauth")doCloudAuth(x)
  else if(k==="clsql")sqlCopy()
  else if(k==="auto"){S.auto=!S.auto;save();soundM()}
  else if(k==="minet"){S.miNet=S.miNet===false;save();soundM()}
  else if(k==="sb"){S.sb=S.sb===false;save();soundM()}
  else if(k==="smu"){closeM();window.open("https://simpmusic.org/app/watch?v="+encodeURIComponent(T[+x].yt),"_blank","noopener")}
  else if(k==="rad"){closeM();miRadio(+x)}
  else if(k==="shr"){closeM();shareTrack(+x)}else if(k==="shrc"){closeM();shareCard(+x)}else if(k==="off"){closeM();OFFB[T[+x].url]?dlDrop(+x):dlTrack(+x)}
  else if(k==="idle"){S.idle=!S.idle;save();soundM()}
  else if(k==="viz"){S.viz=!S.viz;save();vzKick();soundM()}
  else if(k==="amb"){S.amb=+x;save();ambApply();ambKick();soundM()}
  else if(k==="xf"){S.xf=+x;save();soundM()}
  else if(k==="gp"){S.gap=!S.gap;save();soundM()}
  else if(k==="fi"){closeM();findInfo(+x)}
  else if(k==="fl"){closeM();findLyrics(+x,true)}
  else if(k==="pn"){qIns(+x,true);toast("Playing next");closeM()}
  else if(k==="aq"){qIns(+x,false);toast("Added to queue");closeM()}
  else if(k==="ap")addPlM(+x);
  else if(k==="tp"){const [ii,id]=x.split(":"),p=S.pls.find(q=>q.id===id);if(p&&!p.keys.includes(T[+ii].key)){p.keys.push(T[+ii].key);rememberStream(+ii);save()}toast("Added to "+(p?p.name:"playlist"));closeM();render()}
  else if(k==="np")newPlM(+x);
  else if(k==="mk"){const inp=$("mi"),t=inp.dataset.t,p=mkPl(inp.value,t===""?null:+t),pend=S.pendList;S.pendList=null;if(pend)addKeys(p,pend);closeM();toast(`Created "${p.name}"`);render()}
  else if(k==="rn"){const p=S.pls.find(q=>q.id===x);if(p){p.name=$("mi").value.trim()||p.name;save()}closeM();render()}
  else if(k==="dl"){S.pls=S.pls.filter(p=>p.id!==x);S.nav=null;save();closeM();render()}
  else if(k==="lk"){toggleLike(+x);closeM()}
  else if(k==="ga"){closeM();goNav("album:"+T[+x].al.toLowerCase())}
  else if(k==="gr"){closeM();goNav("artist:"+T[+x].a.split(/,\s*/)[0].toLowerCase())}
  else if(k==="rm"){const p=S.pls.find(q=>q.id===S.plv);if(p){p.keys=p.keys.filter(z=>z!==T[+x].key);save()}closeM();render()}
  else if(k==="sp"){S.rate=+x;A.playbackRate=A.defaultPlaybackRate=S.rate;save();updCtl();msPos(true);closeM()}
  else if(k==="sl"){S.sleep=x==="0"?null:x==="end"?"end":Date.now()+ +x*60000;toast(x==="0"?"Sleep timer off":"Sleep timer set");updCtl();closeM()}
  else if(k==="qj"){S.qi=+x;start(S.q[S.qi]);closeM()}}
$("mdl").onclick=e=>{if(e.target.closest("[data-x]")){closeM();return}const b=e.target.closest("[data-a]");if(!b)return;const a=b.dataset.a,c=a.indexOf(":");act(a.slice(0,c),a.slice(c+1))};
$("mdl").oninput=e=>{const b=e.target.dataset.b;if(b==null)return;S.eq.g[+b]=+e.target.value;S.eq.n="Custom";S.eqOn=true;applyEQ();save();updCtl();document.querySelectorAll('[data-a^="eq:"]').forEach(z=>z.classList.remove("on"))};
$("mdl").onkeydown=e=>{if(e.key==="Enter"&&e.target.tagName==="INPUT"&&e.target.type!=="range"&&e.target.type!=="file"){const b=$("mdl").querySelector(".addb[data-a]");if(b)b.click()}};

