/* ---------- views ---------- */
function mixList(){const o=[],cap=n=>n.length>22?n.slice(0,21)+"…":n;
  const rc=recentIdx().filter(i=>!T[i].radio&&!T[i].stream);
  if(rc.length>=3)o.push({id:"repeat",name:"On repeat",sub:"What you keep coming back to",idx:rc.slice(0,15)});
  const lk=likedIdx();if(lk.length>=2)o.push({id:"liked",name:"Liked mix",sub:lk.length+" songs you love",idx:lk});
  artists().filter(a=>a.idx.length>=2).slice(0,4).forEach(a=>o.push({id:"ar:"+a.id,name:cap(a.name)+" mix",sub:a.idx.length+" songs",idx:a.idx}));
  const g=new Map();vis().forEach(i=>{const t=T[i];if(t.g&&!t.radio&&!t.stream){const k=t.g.split(/[,/]/)[0].trim();if(k){if(!g.has(k))g.set(k,[]);g.get(k).push(i)}}});
  [...g].filter(([,v])=>v.length>=2).slice(0,3).forEach(([k,v])=>o.push({id:"g:"+k,name:cap(k)+" mix",sub:v.length+" songs",idx:v}));
  S.mixes=o;return o}
const mixCard=m=>`<div class="cd mixc" role="button" tabindex="0" data-nav="${nav1("mix",m.id)}"><i style="background:${collage(m.idx)}"><s class="pl" data-mixplay="${esc(m.id)}"><svg viewBox="0 0 24 24"><path d="M8 5.1v13.8c0 .8.9 1.3 1.6.9l10.6-6.9c.6-.4.6-1.3 0-1.7L9.6 4.2C8.9 3.8 8 4.3 8 5.1z"/></svg></s><span class="bd">MIX</span><span class="mt">${esc(m.name)}<small>${esc(m.sub)}</small></span></i></div>`;
const HUB=[["music","Free music","YouTube Music, SimpMusic charts, iTunes previews",'<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',"84,120,140"],
 ["radio","Live radio","Thousands of stations worldwide",'<path d="M4 9h16v10H4zM8 9l9-5M8 14h4"/><circle cx="16.5" cy="14" r="1.5"/>',"170,110,90"],
 ["lyrics","Lyrics","Synced, fetched for every song",'<path d="M5 6h14M5 10h14M5 14h9M5 18h6"/>',"150,100,130"],
 ["sound","Sound lab","EQ, Dolby, IMAX, crossfade, gapless",'<path d="M5 20v-7M5 9V4M12 20v-3M12 13V4M19 20v-9M19 7V4M3 13h4M10 17h4M17 11h4"/>',"110,140,110"],
 ["ambient","Ambient room","Glow that reacts to the music and the room",'<circle cx="12" cy="12" r="4"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2"/>',"201,151,90"],
 ["yt","YouTube","Search the YouTube music catalogue",'<rect x="3" y="6" width="18" height="12" rx="4"/><path d="M10.5 9.5v5l4.5-2.5z"/>',"200,60,60"],
 ["cloud","Cloud sync","Playlists and settings on every device",'<path d="M7 18a4 4 0 010-8 5.5 5.5 0 0110.6 1.5A3.3 3.3 0 0117 18z"/>',"84,150,190"],
 ["library","Your vault","Songs kept on this device, tags and art",'<path d="M6 4h12v17l-6-4-6 4z"/>',"120,110,160"]];
const hubCard=h=>`<button class="hub" data-hub="${h[0]}" style="--c:${h[4]}"><svg viewBox="0 0 24 24">${h[3]}</svg><b>${h[1]}</b><small>${h[2]}</small></button>`;
function qpicks(){const o=[],lk=likedIdx();
  o.push(`<button class="qp" data-nav="${nav1("pl","liked")}"><i style="background:${plArt("liked",lk)}"></i><b>Liked songs</b></button>`);
  S.pls.slice(0,3).forEach(p=>o.push(`<button class="qp" data-nav="${nav1("pl",p.id)}"><i style="background:${plArt(p.id,plIdx(p))}"></i><b>${esc(p.name)}</b></button>`));
  albums().slice(0,Math.max(0,6-o.length)).forEach(a=>o.push(`<button class="qp" data-nav="${nav1("album",a.id)}"><i style="background:${artOf(T[rep(a.idx)])}"></i><b>${esc(a.name)}</b></button>`));
  if(o.length<6)o.push(`<button class="qp" data-hub="music"><i class="hi"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/></svg></i><b>Discover free music</b></button>`);
  if(o.length<6)o.push(`<button class="qp" data-hub="radio"><i class="hi"><svg viewBox="0 0 24 24"><path d="M4 9h16v10H4zM8 9l9-5M8 14h4"/><circle cx="16.5" cy="14" r="1.5"/></svg></i><b>Live radio</b></button>`);
  return o.slice(0,6).join("")}
function storyRow(){const ar=artists().filter(o=>o.idx.length).slice(0,14);if(ar.length<2)return"";const cur=T[S.cur];return `<div class="row" style="margin-bottom:10px"><h2>Your artists</h2></div><div class="story">${ar.map(o=>`<button data-nav="${nav1("artist",o.id)}" aria-label="${esc(o.name)}"><i style="background:${artOf(T[rep(o.idx)])}"></i>${cur&&o.idx.includes(S.cur)&&S.playing?"<em></em>":""}<small>${esc(o.name)}</small></button>`).join("")}</div>`}

function npIdx(){if(S.cur>=0&&T[S.cur])return S.cur;if(S.lastIdx>=0&&T[S.lastIdx])return S.lastIdx;const r=recentIdx();return r.length?r[0]:-1}
function npHero(){const ix=npIdx(),t=T[ix];
  if(!t)return `<div class="nph" id="npHero"><div class="npc" style="max-width:none"><div class="npl"><i><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg></i>Your player</div><div class="npt">Nothing playing yet</div><div class="npm"><span>Pick a song and it shows up here</span></div><div class="npb"><button class="npk" onclick="go('Discover')">Discover free music</button></div></div></div>`;
  const lk=S.likes.has(t.key),pl=S.playing&&ix===S.cur;
  const img=t.yt&&!t.img?`center/cover url(https://i.ytimg.com/vi/${t.yt}/hqdefault.jpg),${art(t.c)}`:artOf(t);
  const meta=[t.a,t.al&&!t.radio?t.al:""].filter(Boolean).map(esc).join(" · ");
  return `<div class="nph" id="npHero"><div class="npi" style="background:${img};background-size:cover;background-position:center 30%"></div><div class="npc"><div class="npl"><i><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg></i>${pl?"Now playing":ix===S.cur?"Paused":"Last played"}${t.radio?" · Live":""}</div><div class="npt">${esc(t.t)}</div><div class="npm"><svg viewBox="0 0 24 24"><path d="M4 15v-3a8 8 0 0116 0v3"/><rect x="3" y="14" width="4" height="7" rx="1.5"/><rect x="17" y="14" width="4" height="7" rx="1.5"/></svg><span>${meta}</span></div><div class="npb"><button class="npp" aria-label="${pl?"Pause":"Play"}" onclick="${ix===S.cur?"toggle()":"start("+ix+")"}">${pl?PAUSE:PLAY}</button>${t.radio?"":`<button class="npk${lk?" on":""}" aria-pressed="${lk}" onclick="toggleLike(${ix})"><svg viewBox="0 0 24 24"><path d="M12 21s-7-4.5-9.5-9C.8 8.7 2.7 5 6.2 5c2 0 3.4 1.1 5.8 3.3C14.4 6.1 15.800 5 17.800 5c3.500 0 5.400 3.700 3.700 7-2.500 4.500-9.500 9-9.500 9z"/></svg>${lk?"Liked":"Like"}</button>`}</div></div></div>`}
function npUpd(){const e=$("npHero");if(!e)return;const n=document.createElement("div");n.innerHTML=npHero();e.replaceWith(n.firstChild)}
function home(){const rc=recentIdx().slice(0,3);S.lists.home=rc.slice();const al=albums(),nm=S.acct?S.acct.name:S.guest.name!=="Guest"?S.guest.name:"",mx=mixList();
  return `<div class="mcard"><div class="top2"><div class="gt"><small>${new Date().toLocaleDateString(LOC(),{weekday:"long"})}</small><h1 class="hello">${greet()}${nm?", "+esc(nm):""}</h1></div><button class="rb2" data-go="Search" aria-label="Search"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg></button><button class="rb2" data-sx="friends" aria-label="Friends"><svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.6-3.6 3.2-5.5 6.5-5.5s5.9 1.9 6.5 5.5"/><path d="M16 4.8a3.5 3.5 0 010 6.4M18 14.8c2 .6 3.2 2.3 3.5 5.2"/></svg></button>${avEl(me(),"","button","data-acct aria-label=\"Account\"")}</div>
  ${npHero()}
  <div class="qpg">${qpicks()}</div>
  ${storyRow()}</div><div class="mcard">
  <div class="row"><h2>Recently played</h2><button data-nav="recent:all">See all</button></div>
  <div id="recentBox">${recentHtml(rc)}</div>
  ${mx.length?`<div class="row"><h2>Made for you</h2></div><div class="pls">${mx.map(mixCard).join("")}</div>`:""}
  <div class="row"><h2>Your milo world</h2></div><div class="hubg">${HUB.map(hubCard).join("")}</div>
  <div class="row"><h2>Your playlists</h2><button data-go="Library" data-lib="Playlists">See all</button></div>
  <div class="pls">${plCard("liked","Liked songs",likedIdx())}${S.pls.map(p=>plCard(p.id,p.name,plIdx(p))).join("")}${newCard}</div>
  ${al.length?`<div class="row"><h2>Albums</h2><button data-go="Library" data-lib="Albums">See all</button></div><div class="pls">${al.slice(0,10).map(alCard).join("")}</div>`:""}
  ${devSec()}</div>`}
function resHtml(){const q=S.sq.toLowerCase(),m=s=>!q||s.toLowerCase().includes(q);
  const sg=vis().filter(i=>m([T[i].t,T[i].a,T[i].al,T[i].g].join(" ")));
  const al=q?albums().filter(o=>m(o.name+" "+o.artist)):[],ar=q?artists().filter(o=>m(o.name)):[];let h="";
  if(ar.length)h+=`<div class="row"><h2>Artists</h2></div><div class="list">${ar.map(arRow).join("")}</div>`;
  if(al.length)h+=`<div class="row"><h2>Albums</h2></div><div class="pls">${al.map(alCard).join("")}</div>`;
  if(sg.length)h+=`<div class="row"><h2>Songs</h2></div>`+list(sg,"s");else if(!h)h='<p class="empty">No matches. Try an artist or album name.</p>';return h}
const searchV=()=>`<h1 style="margin-top:20px">Find something to sit with</h1><input id="q" placeholder="Songs, albums or artists" value="${esc(S.sq)}" autocomplete="off"><div id="res">${resHtml()}</div>`;
function libV(){let h=`<h1 style="margin-top:20px">Your library</h1><div class="dvrow">${addBtn("Add music",MUSIC,"m")}${addBtn("Add lyrics (.lrc)","","l")}${dvBar()}</div><div class="seg" style="margin:6px 0 18px">${["Songs","Albums","Artists","Playlists"].map(k=>`<button data-lib="${k}" class="${S.lib===k?"on":""}">${k}</button>`).join("")}</div>`;
  if(S.lib==="Songs")h+=vis().length?list(byTitle(vis()),"lib"):'<p class="empty">Add music to build your library. Songs you add are kept on this device for next time.</p>';
  else if(S.lib==="Albums"){const a=albums();h+=a.length?`<div class="grid">${a.map(alCard).join("")}</div>`:'<p class="empty">Albums are built from your songs\' tags.</p>'}
  else if(S.lib==="Artists"){const a=artists();h+=a.length?`<div class="list">${a.map(arRow).join("")}</div>`:'<p class="empty">Artists appear here.</p>'}
  else h+=`<button class="ghost" data-np style="margin-bottom:14px">+ New playlist</button><div class="list">${plRow("liked","Liked songs",likedIdx())}${S.pls.map(p=>plRow(p.id,p.name,plIdx(p))).join("")}</div>`;
  return h}
const demoRow=i=>`<div class="li"><i style="background:${artOf(T[i])}"></i><span><b>${esc(T[i].t)}</b><small>${esc(T[i].a)} · ${T[i].hid?"In the demo folder":"In your library"}</small></span><button class="dm" data-demo="${i}">${T[i].hid?"Add":"Remove"}</button></div>`;
const profV=()=>`<h1 style="margin-top:20px">Profile</h1>${acctCard()}${sxHtml()}<div class="row" style="margin-top:6px"><h2>Cloud sync</h2></div>${cloudHtml()}<div class="row" style="margin-top:6px"><h2>Insights &amp; tools</h2></div><div class="tools"><button data-tool="ins">Listening insights</button><button data-tool="bkx">Back up</button><label>Restore<input type="file" accept=".json,application/json" data-bk hidden></label><button data-tool="stor">Storage</button><button data-tool="keys">Shortcuts</button><button data-tool="tv">${tvOn()?"TV mode: on":"TV mode: off"}</button></div><div class="row" style="margin-top:6px"><h2>Background</h2></div><div class="seg">${["light","dark","auto"].map(m=>`<button data-th="${m}" class="${S.th===m?"on":""}">${m[0].toUpperCase()+m.slice(1)}</button>`).join("")}</div>
  <div class="row" style="margin-top:24px"><h2>Text contrast</h2></div><div class="seg"><button data-hcb="0" class="${S.hc?"":"on"}">Standard</button><button data-hcb="1" class="${S.hc?"on":""}">High</button></div>
  <div class="row" style="margin-top:24px"><h2>Sound &amp; playback</h2></div><button class="ghost" data-snd>EQ, crossfade &amp; gapless</button>
  ${installHtml()}${stgHtml()}
  <div class="row" style="margin-top:24px"><h2>Demo songs</h2><button data-demo="all">Add all</button></div>
  <p class="empty" style="padding-top:0">Demo songs live here until you add them to your library.</p><div class="list">${T.map((t,i)=>t.demo?demoRow(i):"").join("")}</div>
  <div class="stat"><div><b>${vis().length}</b><span>songs</span></div><div><b>${albums().length}</b><span>albums</span></div><div><b>${artists().length}</b><span>artists</span></div><div><b>${S.pls.length}</b><span>playlists</span></div></div>
  <p class="empty" style="margin-top:18px">Playlists, liked songs and sound settings are saved on this device.</p>`;
function totDur(idx){const s=idx.reduce((a,i)=>a+(T[i].radio?0:(+T[i].d||0)),0);if(!s)return"";const m=Math.round(s/60);return m>=60?Math.floor(m/60)+" hr "+(m%60)+" min":Math.max(1,m)+" min"}
const DP='<path d="M8 5v14l11-7z"/>',DPA='<path d="M7 5h4v14H7zM13 5h4v14h-4z"/>';
function dpUpd(){const l=S.lists&&S.lists.d;if(!l)return;const on=S.playing&&S.cur>=0&&l.includes(S.cur);document.querySelectorAll("[data-pa] svg").forEach(s=>s.innerHTML=on?DPA:DP)}
function detail(){const c=S.nav.indexOf(":"),k=S.nav.slice(0,c),id=S.nav.slice(c+1);let ti,type,m1="",m2="",idx,bg,pl=null,round=false,extra="",tint="201,151,90";S.plv=null;
  const tc=ix=>{const t=T[rep(ix)];return t&&t.c?t.c:tint};
  if(k==="recent"){idx=recentIdx();ti="Recently played";type="History";m1=idx.length+" song"+(idx.length===1?"":"s");bg=idx.length?collage(idx):"var(--card)";if(idx.length)tint=tc(idx)}
  else if(k==="album"){const o=albums().find(x=>x.id===id);if(!o){S.nav=null;return home()}ti=o.name;type="Album";idx=o.idx;bg=artOf(T[rep(idx)]);tint=tc(idx);m1=`<b>${esc(o.artist)}</b>${o.y?" · "+o.y:""}`;m2=idx.length+" song"+(idx.length===1?"":"s")+(totDur(idx)?" · "+totDur(idx):"")}
  else if(k==="artist"){const o=artists().find(x=>x.id===id);if(!o){S.nav=null;return home()}ti=o.name;type="Artist";idx=o.idx;round=true;bg=artOf(T[rep(idx)]);tint=tc(idx);const als=albums().filter(a=>a.idx.some(i=>idx.includes(i))&&a.name&&a.name!=="Singles");m1=idx.length+" song"+(idx.length===1?"":"s")+(als.length>1?" · "+als.length+" albums":"");m2=totDur(idx);
    if(als.length>1)extra=`<div class="dsec">Albums</div><div class="pls">${als.map(alCard).join("")}</div><div class="dsec">Songs</div>`}
  else if(k==="mix"){const m=mixList().find(x=>x.id===id);if(!m){S.nav=null;return home()}ti=m.name;type="Mix";idx=m.idx;bg=collage(idx);tint=tc(idx);m1=m.sub;m2=totDur(idx)}
  else if(id==="liked"){ti="Liked songs";type="Playlist";idx=likedIdx();bg=`linear-gradient(145deg,#6b4de6,#b9a7ff)`;tint="107,77,230";m1=idx.length+" song"+(idx.length===1?"":"s");m2=totDur(idx)}
  else{pl=S.pls.find(p=>p.id===id);if(!pl){S.nav=null;return home()}S.plv=pl.id;ti=pl.name;type="Playlist";idx=plIdx(pl);const miss=pl.keys.length-idx.length;bg=plArt(id,idx);if(idx.length)tint=tc(idx);m1=idx.length+" song"+(idx.length===1?"":"s")+(totDur(idx)?" · "+totDur(idx):"");m2=miss?miss+" not loaded on this device":""}
  S.dtc=tint;S.dti=ti;
  const ib=(a,p,l,x)=>`<button class="rb" ${a} aria-label="${l}" title="${l}">${svg(p)}</button>`;
  const num=k==="album";
  const rows=idx.length?(S.lists.d=idx,`<div class="list${num?" num":" dlist"}">${idx.map((i,n)=>{const h=li(i,"d");return num?h.replace(/<i style="background:[^"]*">/,`<i>${n+1}`):h}).join("")}</div>`):`<p class="empty">${k==="recent"?"Nothing played yet. Songs you listen to will show up here.":"No songs here yet. Use the ⋯ menu on any song to add it."}</p>`;
  return `<button class="dbk" data-back aria-label="Back"><svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg></button>
  <div class="dhero"><i class="dbd" style="background:${bg}"></i><i class="dcv${round?" rd":""}" style="background:${bg}"></i><div class="dtx"><div class="dky">${type}</div><h1>${esc(ti)}</h1><div class="dm2">${m1?m1:""}${m2?`<small>${m2}</small>`:""}</div></div></div>
  <div class="dact">${ib("data-ps",I.shuf,"Shuffle")}${ib("data-pq",I.queue,"Add to queue")}${ib("data-pn",'<path d="M5 5v14l9-7zM18 5v14"/>',"Play next")}${ib("data-pd",'<path d="M12 4v11M7 11l5 5 5-5M5 20h14"/>',"Save for offline")}<span class="sp"></span><button class="pg" data-pa aria-label="Play">${svg(DP)}</button></div>
  ${k==="recent"&&idx.length||pl?`<div class="dx">${k==="recent"&&idx.length?'<button class="ghost" data-clrh>Clear history</button>':""}${pl?`<button class="ghost" data-rn="${pl.id}">Rename</button><button class="ghost" data-dl="${pl.id}">Delete</button>`:""}</div>`:""}
  ${extra}${rows}`}
function shellUpd(nb){
  const app=$("app");let p=$("pfc");
  if(!p){p=document.createElement("div");p.id="pfc";p.tabIndex=0;p.setAttribute("role","button");p.setAttribute("aria-label","Your profile");app.appendChild(p);
    p.onclick=()=>go("Profile");p.onkeydown=e=>{if(e.key==="Enter"||e.key===" "){e.preventDefault();go("Profile")}};
    const m=document.createElement("div");m.id="sml";m.innerHTML='<div class="smh"><b>My music</b><input id="smq" type="search" placeholder="Search your music" autocomplete="off" aria-label="Search your music"></div><div class="smb" id="smb"></div>';app.appendChild(m);
    $("smq").oninput=e=>{S.smq=e.target.value;smList()};
    const r=document.createElement("aside");r.id="rcol";r.setAttribute("aria-label","Popular albums");app.appendChild(r)}
  const u=me(),h=(u.name||"").toLowerCase().replace(/[^a-z0-9]+/g,"")||"guest";
  p.innerHTML=`${avEl(u)}<div><b>${esc(u.name)}</b><small>@${esc(h)}</small><div class="st"><span><em>${S.likes.size}</em> liked</span><span><em>${S.pls.length}</em> playlists</span></div></div>`;
  smList(true);
  const al=albums().sort((a,b)=>b.idx.length-a.idx.length).slice(0,12);
  $("rcol").innerHTML=`<h3>Popular albums</h3><div class="rcb">${al.length?al.map(o=>`<button class="rca" data-nav="${nav1("album",o.id)}"><i style="background:${artOf(T[o.idx[0]])}"></i><span><b>${esc(o.name)}</b><small>${o.y?o.y+" · ":""}${esc(o.artist||"")}</small></span></button>`).join(""):'<p class="sme">Albums from your library show up here.</p>'}</div>`}
function smList(nb){const b=$("smb");if(!b)return;const q=(S.smq||"").trim().toLowerCase();let idx=vis();if(q)idx=idx.filter(i=>(T[i].t+" "+(T[i].a||"")+" "+(T[i].al||"")).toLowerCase().includes(q));idx=idx.slice(0,80);S.lists.sml=idx;
  b.innerHTML=idx.length?idx.map(i=>li(i,"sml")).join(""):`<p class="sme">${q?"No matches":"Your songs appear here"}</p>`;if(!nb){bind();sync()}}
function render(){S.lists={};const v=$("view");
  v.innerHTML=S.nav?detail():S.tab==="Home"?home():'<div class="mcard">'+(S.tab==="Search"?searchV():S.tab==="Discover"?discoverV():S.tab==="Library"?libV():profV())+"</div>";
  if(!S.nav&&S.tab==="Discover")S.dm==="radio"?radioBind():musicBind();
  if(!S.nav&&S.tab==="Search"){const q=$("q");q.oninput=()=>{S.sq=q.value;$("res").innerHTML=resHtml();bind();sync()}}
  {const ct=T[S.cur];v.classList.toggle("dtl",!S.nav);if(!S.nav)v.style.setProperty("--dt",ct&&ct.c&&!ct.radio?ct.c:"201,151,90")}v.classList.toggle("dt",!!S.nav);if(S.nav){v.style.setProperty("--dt",S.dtc||"201,151,90");if(S._pn!==S.nav)v.scrollTop=0}S._pn=S.nav;
  shellUpd();bind();sync();dpUpd();let sb=$("dsb");if(!sb){sb=document.createElement("div");sb.id="dsb";$("app").appendChild(sb)}
  if(S.nav){sb.innerHTML=`<b>${esc(S.dti||"")}</b><button class="pg" data-pa aria-label="Play">${svg(DP)}</button>`;sb.style.setProperty("--dt",S.dtc);sb.classList.remove("show");bind();dpUpd();v.onscroll=()=>sb.classList.toggle("show",v.scrollTop>(innerWidth>=860?250:300))}else{sb.classList.remove("show");sb.innerHTML="";v.onscroll=null}}
function bind(){
  document.querySelectorAll("[data-pick]").forEach(i=>i.onchange=e=>{const f=[...e.target.files];e.target.value="";addFiles(f)});
  document.querySelectorAll("[data-i]").forEach(e=>e.onclick=ev=>{if(ev.target.closest("[data-m]"))return;play(+e.dataset.i,S.lists[e.dataset.l])});
  document.querySelectorAll("[data-m]").forEach(e=>e.onclick=ev=>{ev.stopPropagation();trackMenu(+e.dataset.m)});
  document.querySelectorAll("[data-go]").forEach(e=>e.onclick=()=>{if(e.dataset.lib)S.lib=e.dataset.lib;go(e.dataset.go)});
  document.querySelectorAll("[data-lib]:not([data-go])").forEach(e=>e.onclick=()=>{S.lib=e.dataset.lib;render()});
  document.querySelectorAll("[data-cm]").forEach(e=>e.onclick=ev=>{ev.stopPropagation();collMenu(e.dataset.cm)});
  document.querySelectorAll("[data-pn]").forEach(e=>e.onclick=()=>qAddList(S.lists.d,true));
  document.querySelectorAll("[data-pq]").forEach(e=>e.onclick=()=>qAddList(S.lists.d,false));document.querySelectorAll("[data-pd]").forEach(e=>e.onclick=()=>dlList(S.lists.d));
  document.querySelectorAll("[data-nav]").forEach(e=>e.onclick=()=>{S.nav=decodeURIComponent(e.dataset.nav);render();$("view").scrollTop=0});
  document.querySelectorAll("[data-np]").forEach(e=>e.onclick=()=>newPlM(null));
  document.querySelectorAll("[data-back]").forEach(e=>e.onclick=()=>{S.nav=null;render()});
  document.querySelectorAll("[data-pa]").forEach(e=>e.onclick=()=>{const l=S.lists.d||[];if(S.playing&&S.cur>=0&&l.includes(S.cur))toggle();else playList(l,false);setTimeout(dpUpd,60)});
  document.querySelectorAll("button[data-ps]").forEach(e=>e.onclick=()=>{playList(S.lists.d||[],true);setTimeout(dpUpd,60)});
  document.querySelectorAll("[data-rn]").forEach(e=>e.onclick=()=>{const p=S.pls.find(x=>x.id===e.dataset.rn);modal(`<h3>Rename playlist</h3><input id="mi" value="${esc(p.name)}" maxlength="40"><button class="addb" data-a="rn:${p.id}">Save</button>`,1)});
  document.querySelectorAll("[data-dl]").forEach(e=>e.onclick=()=>modal(`<h3>Delete playlist?</h3><p>The songs stay in your library.</p><button class="addb" data-a="dl:${e.dataset.dl}">Delete</button> <button class="ghost" data-x>Cancel</button>`));
  document.querySelectorAll("[data-dm]").forEach(e=>e.onclick=()=>{S.dm=e.dataset.dm;render()});
  document.querySelectorAll("[data-inst]").forEach(e=>e.onclick=()=>{if(ipEvt){ipEvt.prompt();if(ipEvt.userChoice)ipEvt.userChoice.then(()=>{ipEvt=null;render()})}});
  document.querySelectorAll("[data-clr]").forEach(e=>e.onclick=()=>{const n=libStats().n;modal(`<h3>Remove all uploaded songs?</h3><p>This deletes ${n} song${n===1?"":"s"} from this device's storage. Playlists keep their entries and light up again if you add the songs back.</p><button class="addb" data-a="clr:">Remove all</button> <button class="ghost" data-x>Cancel</button>`)});
  document.querySelectorAll("[data-acct]").forEach(e=>e.onclick=()=>go("Profile"));
  document.querySelectorAll("[data-edit]").forEach(e=>e.onclick=openEdit);
  document.querySelectorAll("[data-su]").forEach(e=>e.onclick=()=>gateOpen("up"));
  document.querySelectorAll("[data-si]").forEach(e=>e.onclick=()=>gateOpen("in"));
  document.querySelectorAll("[data-so]").forEach(e=>e.onclick=()=>{if(S.cloud||S.acct&&S.acct.cid)signOutAll();else{S.acct=null;LS.set("milo.cur","");toast("Signed out");afterSwitch()}});
  document.querySelectorAll("[data-del]").forEach(e=>e.onclick=()=>modal(`<h3>Delete account?</h3><p>This removes "${esc(S.acct.name)}", its playlists, likes, friends and messages${S.acct.cid?" from our servers and this device":" from this device"}. This can't be undone.</p><button class="addb" data-a="delc:">Delete</button> <button class="ghost" data-x>Cancel</button>`));
  document.querySelectorAll("[data-clrh]").forEach(e=>e.onclick=()=>{S.hist=[];save();render();toast("History cleared")});
  document.querySelectorAll("[data-cloud]").forEach(e=>e.onclick=()=>{const k=e.dataset.cloud;if(k==="up"||k==="in")cloudM(k);else if(k==="out")cloudOut();else if(k==="pull")sbPull(true);else if(k==="sql")sqlM()});
  document.querySelectorAll("[data-mixplay]").forEach(e=>e.onclick=ev=>{ev.stopPropagation();const m=mixList().find(x=>x.id===e.dataset.mixplay);if(m){playList(m.idx,false);toast("Playing "+m.name)}});
  document.querySelectorAll("[data-hub]").forEach(e=>e.onclick=()=>{const k=e.dataset.hub;
    if(k==="music"||k==="radio"){S.dm=k;go("Discover")}
    else if(k==="yt"){S.dm="music";S.ms="yt";S.mg="Trending";S.mres=[];S.mErr="";S.mqv="";S.mtried=0;go("Discover")}
    else if(k==="cloud")go("Profile");
    else if(k==="library")go("Library");
    else if(k==="lyrics"){if(S.cur>=0)openLyrics();else toast("Play a song to see its lyrics")}
    else soundM()});
  document.querySelectorAll("[data-hcb]").forEach(e=>e.onclick=()=>{S.hc=e.dataset.hcb==="1";save();applyWash();render()});
  document.querySelectorAll("[data-pst]").forEach(e=>e.onclick=()=>{S.ps=e.dataset.pst;save();setStyle();render()});
  document.querySelectorAll("[data-snd]").forEach(e=>e.onclick=soundM);
  document.querySelectorAll("[data-demo]").forEach(e=>e.onclick=()=>{const v=e.dataset.demo;if(v==="all")T.forEach(t=>{if(t.demo)t.hid=false});else T[+v].hid=!T[+v].hid;S.demoOn=T.filter(t=>t.demo&&!t.hid).map(t=>t.key);save();toast(v==="all"?"Demo songs added to your library":T[+v].hid?"Moved back to the demo folder":"Added to your library");render()});
  document.querySelectorAll("[data-th]").forEach(e=>e.onclick=()=>{S.th=e.dataset.th;save();theme();render()})}
function theme(){const d=S.th==="dark"||(S.th==="auto"&&(S.lux!=null?S.lux<40:matchMedia("(prefers-color-scheme: dark)").matches));$("app").dataset.theme=d?"dark":"light";applyWash()}
function go(t){S.tab=t;S.nav=null;nav();render();$("view").scrollTop=0}
function nav(){$("nav").innerHTML=Object.keys(ICON).map(k=>`<button class="${S.tab===k?"on":""}" data-t="${k}"><svg viewBox="0 0 24 24">${ICON[k]}</svg>${k}</button>`).join("");
  document.querySelectorAll("[data-t]").forEach(e=>e.onclick=()=>go(e.dataset.t))}

