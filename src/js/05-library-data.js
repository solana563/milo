/* ---------- library data ---------- */
const idxOfKey=k=>{for(let i=T.length-1;i>=0;i--)if(T[i].key===k&&!T[i].gone)return i;return -1};
const showable=i=>i>=0&&(!T[i].hid||T[i].radio||T[i].stream);
const likedIdx=()=>[...S.likes].map(idxOfKey).filter(showable);
const plIdx=p=>p.keys.map(idxOfKey).filter(showable);
const rep=idx=>idx.find(i=>T[i].img)??idx[0];
function albums(){const m=new Map();T.forEach((t,i)=>{if(!t.al||t.hid)return;const k=t.al.toLowerCase();let o=m.get(k);if(!o)m.set(k,o={id:k,name:t.al,artist:t.aa||t.a,y:t.y,idx:[]});o.idx.push(i)});
  m.forEach(o=>o.idx.sort((a,b)=>(T[a].n||99)-(T[b].n||99)));return[...m.values()].sort((a,b)=>a.name.localeCompare(b.name))}
function artists(){const m=new Map();T.forEach((t,i)=>{if(t.hid)return;new Set([...(t.a||"").split(/,\s*/),t.aa].filter(n=>n&&n!=="On this device")).forEach(n=>{const k=n.toLowerCase();let o=m.get(k);if(!o)m.set(k,o={id:k,name:n,idx:[]});o.idx.push(i)})});return[...m.values()].sort((a,b)=>a.name.localeCompare(b.name))}
const nav1=(k,id)=>`${k}:${encodeURIComponent(id)}`;
const li=(i,L)=>`<div class="li" data-i="${i}" data-l="${L}" role="button" tabindex="0"><i style="background:${artOf(T[i])}"><u class="eq"><s></s><s></s><s></s></u></i><span><b>${esc(T[i].t)}</b><small>${esc(T[i].a)}${T[i].al?" · "+esc(T[i].al):""}</small></span>${T[i].radio?'<em class="lv">LIVE</em>':`<em>${T[i].d?fmt(T[i].d):""}</em>`}<button class="mo" data-m="${i}" aria-label="More">${DOTS}</button></div>`;
function list(idx,L){S.lists[L]=idx;return `<div class="list">${idx.map(i=>li(i,L)).join("")}</div>`}
const alCard=o=>`<div class="cd" role="button" tabindex="0" data-nav="${nav1("album",o.id)}"><i style="background:${artOf(T[rep(o.idx)])}"></i><b>${esc(o.name)}</b><small>${esc(o.artist)}${o.y?" · "+o.y:""}</small>${cmo(nav1("album",o.id))}</div>`;
const arRow=o=>`<div class="li ar" data-nav="${nav1("artist",o.id)}" role="button" tabindex="0"><i style="background:${artOf(T[rep(o.idx)])}"></i><span><b>${esc(o.name)}</b><small>${o.idx.length} song${o.idx.length===1?"":"s"}</small></span><button class="mo" data-cm="${nav1("artist",o.id)}" aria-label="More">${DOTS}</button></div>`;
const layer=(t,pos,size)=>`${t.img?`url(${t.img})`:art(t.c)} ${pos}/${size} no-repeat`;
function collage(idx){const seen=new Set(),u=[];
  for(const i of idx){const t=T[i],k=t.al?"al:"+t.al.toLowerCase():(t.img||t.c);if(seen.has(k))continue;seen.add(k);u.push(t);if(u.length===4)break}
  if(!u.length)return"var(--card)";if(u.length===1)return artOf(u[0]);
  if(u.length===2)return layer(u[0],"left top","50% 100%")+","+layer(u[1],"right top","50% 100%");
  return["left top","right top","left bottom","right bottom"].map((p,k)=>layer(u[k]||u[0],p,"50% 50%")).join(",")}
const plArt=(id,idx)=>idx.length?collage(idx):id==="liked"?art("249,156,0"):"var(--card)";
const plCard=(id,n,idx)=>`<div class="cd" role="button" tabindex="0" data-nav="${nav1("pl",id)}"><i style="background:${plArt(id,idx)}"></i><b>${esc(n)}</b><small>${idx.length} song${idx.length===1?"":"s"}</small>${cmo(nav1("pl",id))}</div>`;
const plRow=(id,n,idx)=>`<div class="li" data-nav="${nav1("pl",id)}" role="button" tabindex="0"><i style="background:${plArt(id,idx)}"></i><span><b>${esc(n)}</b><small>${idx.length} song${idx.length===1?"":"s"}</small></span><button class="mo" data-cm="${nav1("pl",id)}" aria-label="More">${DOTS}</button></div>`;
const newCard='<button class="cd" data-np><i class="nw">+</i><b>New playlist</b><small>Create one</small></button>';
const tile=i=>`<button class="tile" data-i="${i}" data-l="home" style="--c:${T[i].c};background:${artOf(T[i])}"><u class="eq"><s></s><s></s><s></s></u><b>${esc(T[i].t)}</b><span>${esc(T[i].a)}</span></button>`;
const greet=()=>{const h=new Date().getHours();return h<12?"Good morning":h<18?"Good afternoon":"Good evening"};
function devSec(){const L=T.map((t,i)=>t.url&&!t.hid?i:-1).filter(i=>i>=0);
  return `<div class="row" style="margin-top:8px"><h2>On this device</h2>${L.length?'<button data-go="Library" data-lib="Songs">See all</button>':""}</div>`+(L.length?list(L.slice(0,5),"dev")+`<div class="dvrow">${addBtn("Add music",MUSIC,"m")}${dvBar()}</div>`:`<p class="empty">${dvM()?"Play the songs on your phone straight from its storage. Nothing is copied.":dvWebOk()?"Link a folder and milo. plays your songs right where they are. Nothing is copied.":"Play songs stored on this device. Choose audio files and they will show up here."}</p><div class="dvrow">${dvBar()}${addBtn("Add music",MUSIC,"m")}</div>`)}

