/* ---------- live radio, iTunes info, online lyrics ---------- */
const vis=()=>T.map((t,i)=>t.hid?-1:i).filter(i=>i>=0);
const RC=["84,120,140","150,100,130","110,140,110","170,110,90","120,110,160","201,151,90"];
function addStation(s){let i=T.findIndex(t=>t.key==="radio:"+s.id);if(i>=0){if(s.logo&&!T[i].logo)T[i].logo=s.logo;return i}
  const c=RC[[...s.id].reduce((a,ch)=>(a*31+ch.charCodeAt(0))>>>0,7)%6];
  T.push({t:s.name,a:s.country||"Live radio",country:s.country,al:"Live radio",g:s.tag||"",y:"",d:0,c,url:s.url,logo:s.logo||"",home:s.home||"",lg:!!s.lg,needArt:!s.lg,radio:true,hid:true,sid:s.id,key:"radio:"+s.id});if(s.c)T[T.length-1].c=s.c;return T.length-1}
S.rfav.forEach(addStation);S.sfav.forEach(addStream);
const RS=["https://de1.api.radio-browser.info","https://at1.api.radio-browser.info","https://nl1.api.radio-browser.info"];
async function jget(u,ms=9000){const c=new AbortController(),t=setTimeout(()=>c.abort(),ms);try{const r=await fetch(u,{signal:c.signal});if(!r.ok)throw new Error(r.status);return await r.json()}finally{clearTimeout(t)}}
async function radioFetch(qs){let err;for(const b of RS){try{return await jget(`${b}/json/stations/search?${qs}&limit=60&hidebroken=true&order=clickcount&reverse=true`)}catch(e){err=e}}throw err}
const RCHIPS=[["Top",""],["Lo-fi","tag=lofi"],["Jazz","tag=jazz"],["Pop","tag=pop"],["Hip hop","tag=hip%20hop"],["Classical","tag=classical"],["Ambient","tag=ambient"],["News","tag=news"]];
function radioRes(){const sv=S.rfav.map(s=>idxOfKey("radio:"+s.id)).filter(i=>i>=0);let h="";
  if(sv.length)h+=`<div class="row"><h2>Saved stations</h2></div>`+list(sv,"rs");
  if(S.rload)h+='<p class="empty">Tuning in…</p>';else if(S.rerr)h+=`<p class="empty">${esc(S.rerr)}</p>`;
  else if(S.rs.length)h+=`<div class="row"><h2>${esc(S.rc||"Results")}</h2></div>`+list(S.rs,"rr");
  else h+='<p class="empty">Pick a genre or search by name to find live stations.</p>';return h}
const radioPane=()=>`<input id="rq" placeholder="Search stations" value="${esc(S.rqv||"")}" autocomplete="off"><div class="seg chips">${RCHIPS.map(([n])=>`<button data-rc="${n}" class="${S.rc===n?"on":""}">${n}</button>`).join("")}</div><div id="rres">${radioRes()}</div>`;
function refreshR(){if(S.tab!=="Discover"||S.dm!=="radio"||S.nav)return;const r=$("rres");if(r){r.innerHTML=radioRes();bind();sync()}document.querySelectorAll("[data-rc]").forEach(b=>b.classList.toggle("on",b.dataset.rc===S.rc))}
async function radioGo(qs,label){S.rc=label;S.rload=1;S.rerr="";refreshR();
  try{const a=await radioFetch(qs),seen=new Set();
    S.rs=a.filter(x=>{const n=(x.name||"").trim().toLowerCase();if(!x.url_resolved||!x.url_resolved.startsWith("https:")||x.hls||!n||seen.has(n))return false;seen.add(n);return true}).slice(0,40)
      .map(x=>addStation({id:x.stationuuid,name:x.name.trim(),url:x.url_resolved,country:x.country,tag:(x.tags||"").split(",")[0],logo:(x.favicon||"").replace(/^http:/,"https:"),home:x.homepage||""}));
    artScan();
    if(!S.rs.length)S.rerr="No stations found. Try another search."}
  catch(e){S.rs=[];S.rerr="Couldn't reach the radio directory. Hosted pages can block outside connections, so try the downloaded file."}
  S.rload=0;refreshR()}
function radioBind(){const q=$("rq");let tm;
  q.oninput=()=>{clearTimeout(tm);S.rqv=q.value;tm=setTimeout(()=>{if(q.value.trim().length>1)radioGo("name="+encodeURIComponent(q.value.trim()),"Results")},450)};
  document.querySelectorAll("[data-rc]").forEach(b=>b.onclick=()=>{const c=RCHIPS.find(x=>x[0]===b.dataset.rc);q.value="";S.rqv="";radioGo(c[1],c[0])});
  if(!S.rs.length&&!S.rload&&!S.rerr&&!S.rtried){S.rtried=1;radioGo("","Top")}}
const noLy=t=>t.radio?'<p class="none">Live radio has no lyrics.</p>':(t.yt||t.url)?'<p class="none">No lyrics in this song yet. Add a .lrc file with the same name, or look them up online.</p><button class="chip" data-fl style="margin-top:14px;flex:none;padding:10px 18px">Find lyrics online</button>':'<p class="none">No lyrics for this demo song.</p>';
