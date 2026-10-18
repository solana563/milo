/* ----- friends overlay ----- */
const soUid=()=>S.cloud&&S.cloud.id;
const soAv=p=>avEl({name:p&&(p.name||p.username)||"?",av:p&&p.av&&p.av.t?p.av:{t:"color",c:"249,156,0"}},"");
function soEl(){let e=$("soc");if(!e){e=document.createElement("div");e.id="soc";e.hidden=true;$("app").appendChild(e)}return e}
function soOpen(v,id){if(!soUid()){gateOpen("in");return}SO.on=1;SO.v=v||"home";if(id)SO.fid=id;soEl().hidden=false;soRender();soLoad();soPoll()}
function soClose(){SO.on=0;soEl().hidden=true;clearInterval(SO.timer);SO.timer=0;soBadge()}
function soPoll(){clearInterval(SO.timer);SO.timer=setInterval(()=>{if(SO.on)soLoad(SO.v==="chat")},SO.v==="chat"?4000:15000)}
function soStart(){soLoad(true);if(soTimer2)return;soTimer2=1;setInterval(()=>{if(soUid()&&termsOk()&&!SO.on&&!document.hidden)soLoad(true)},60000)}
function soStop(){SO.fr=[];SO.rq=[];SO.out=[];SO.prof={};SO.sh={};SO.unread={};soBadge();if(SO.on)soClose()}
async function soLoad(quiet){if(!soUid()||!termsOk()||SO.busy)return;SO.busy=1;
  try{const me_=soUid(),f=await sb("/rest/v1/friendships?select=a,b,status,created_at&or=(a.eq."+me_+",b.eq."+me_+")");
    const acc=[],rq=[],out=[];(f||[]).forEach(x=>{const o=x.a===me_?x.b:x.a;if(x.status==="accepted")acc.push(o);else if(x.b===me_)rq.push(o);else out.push(o)});
    const ids=[...new Set([...acc,...rq,...out])];SO.fr=acc;SO.rq=rq;SO.out=out;
    if(ids.length){const ps=await sb("/rest/v1/profiles?select=id,username,name,av&id=in.("+ids.join(",")+")");(ps||[]).forEach(p=>SO.prof[p.id]=p);
      if(acc.length){const sh=await sb("/rest/v1/shared?select=user_id,np,np_at,pl,pl_at&user_id=in.("+acc.join(",")+")");SO.sh={};(sh||[]).forEach(r=>SO.sh[r.user_id]=r)}}
    const ms=await sb("/rest/v1/messages?select=sender,created_at&rcpt=eq."+me_+"&created_at=gt."+encodeURIComponent(isoAgo(SO_DAY))+"&order=created_at.desc&limit=200");
    const seen=LS.get("milo.seen",{});SO.unread={};(ms||[]).forEach(m=>{if(!(seen[m.sender]&&seen[m.sender]>=m.created_at))SO.unread[m.sender]=(SO.unread[m.sender]||0)+1});
    if(SO.on&&SO.v==="chat")await soMsgs();
    soBadge();if(SO.on&&!(SO.v==="chat"&&quiet))soRender(quiet)}catch(e){if(SO.on&&!quiet){SO.err=sbMsg(e);soRender()}}
  SO.busy=0}
function soBadge(){const n=SO.rq.length+Object.keys(SO.unread).length;document.querySelectorAll("[data-sx=friends]").forEach(b=>b.classList.toggle("dot",n>0))}
const live=s=>s&&s.np&&s.np.p&&s.np_at&&Date.now()-new Date(s.np_at).getTime()<240000;
const soRow=(id,sub,act)=>{const p=SO.prof[id]||{};return `<div class="so-row" data-sc="${act}:${id}">${soAv(p)}<span><b>${esc(p.name||p.username||"?")}</b><small>${sub}</small></span></div>`};
function soRender(keep){const el=soEl();if(!SO.on)return;const sc=el.querySelector(".so-body"),top=sc?sc.scrollTop:0;let h="";const v=SO.v;const ki={};["so-q","so-t"].forEach(i=>{const e=$(i);if(e)ki[i]=[e.value,document.activeElement===e,e.selectionStart]});
  const head=(t,back)=>`<div class="so-head"><button class="so-b" data-sc="${back||"close"}" aria-label="Back"><svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg></button><h2>${t}</h2></div>`;
  if(v==="home"){const liv=SO.fr.filter(id=>live(SO.sh[id]));
    h=head("Friends")+`<div class="so-body"><div class="so-add"><input id="so-q" placeholder="Find a friend by username" autocapitalize="none" spellcheck="false" maxlength="21"><button class="addb" data-sc="find:">Find</button></div><p class="so-err" id="so-err">${esc(SO.err)}</p>${SO.found===0?`<p class="so-note">No one has that username.</p>`:""}
    ${SO.found&&SO.found.id?`<div class="so-row">${soAv(SO.found)}<span><b>${esc(SO.found.name||SO.found.username)}</b><small>@${esc(SO.found.username)}</small></span>${SO.fr.includes(SO.found.id)?`<em>Friends</em>`:SO.out.includes(SO.found.id)?`<em>Requested</em>`:SO.rq.includes(SO.found.id)?`<button class="addb sm" data-sc="acc:${SO.found.id}">Accept</button>`:`<button class="addb sm" data-sc="req:${SO.found.id}">Add</button>`}</div>`:""}
    ${SO.rq.length?`<h3>Requests</h3>${SO.rq.map(id=>`<div class="so-row">${soAv(SO.prof[id])}<span><b>${esc((SO.prof[id]||{}).name||"?")}</b><small>@${esc((SO.prof[id]||{}).username||"")}</small></span><button class="addb sm" data-sc="acc:${id}">Accept</button><button class="ghost sm" data-sc="dec:${id}">Decline</button></div>`).join("")}`:""}
    ${liv.length?`<h3>Listening now</h3>${liv.map(id=>{const n=SO.sh[id].np;return `<div class="so-row" data-sc="fr:${id}">${soAv(SO.prof[id])}<span><b>${esc((SO.prof[id]||{}).name||"?")}</b><small>♪ ${esc(n.t)} · ${esc(n.a||"")}</small></span>${n.local?"":`<button class="addb sm" data-sc="listen:${id}">Listen</button>`}</div>`}).join("")}`:""}
    <h3>Your friends${SO.fr.length?" · "+SO.fr.length:""}</h3>${SO.fr.length?SO.fr.map(id=>{const s=SO.sh[id],n=SO.unread[id];return `<div class="so-row" data-sc="fr:${id}">${soAv(SO.prof[id])}<span><b>${esc((SO.prof[id]||{}).name||"?")}</b><small>@${esc((SO.prof[id]||{}).username||"")}${live(s)?" · listening now":""}</small></span><button class="so-ic${n?" dot":""}" data-sc="chat:${id}" aria-label="Message">${CHAT}</button></div>`}).join(""):`<p class="so-note">Add friends by username. You'll see what they listen to, browse their playlists and stream the songs, and chat with disappearing messages.</p>`}
    ${SO.out.length?`<h3>Sent requests</h3>${SO.out.map(id=>`<div class="so-row">${soAv(SO.prof[id])}<span><b>${esc((SO.prof[id]||{}).name||"?")}</b><small>@${esc((SO.prof[id]||{}).username||"")} · waiting</small></span><button class="ghost sm" data-sc="dec:${id}">Cancel</button></div>`).join("")}`:""}
    <div class="so-me">You are <b>@${esc((S.prof||{}).username||"")}</b>. Friends can find you with it.</div></div>`}
  else if(v==="friend"){const p=SO.prof[SO.fid]||{},s=SO.sh[SO.fid]||{},n=live(s)?s.np:null,pl=Array.isArray(s.pl)?s.pl:[];
    h=head(esc(p.name||"Friend"),"home")+`<div class="so-body"><div class="so-hero">${soAv(p)}<b>${esc(p.name||"")}</b><small>@${esc(p.username||"")}</small><div class="so-acts"><button class="addb sm" data-sc="chat:${SO.fid}">Message</button><button class="ghost sm" data-sc="rmf:${SO.fid}">Remove friend</button></div></div>
    <h3>Listening</h3>${n?`<div class="so-song"><i style="background:${n.img?`center/cover url(${esc(n.img)})`:art("201,151,90")}"></i><span><b>${esc(n.t)}</b><small>${esc(n.a||"")}</small></span>${n.local?`<em>On their device</em>`:`<button class="addb sm" data-sc="listen:${SO.fid}">Listen</button>`}</div>`:`<p class="so-note">Not listening right now.</p>`}
    <h3>Playlists${pl.length?" · "+pl.length:""}</h3>${pl.length?pl.map((q,i)=>`<div class="so-row" data-sc="pl:${i}"><i class="so-art">♪</i><span><b>${esc(q.name)}</b><small>${q.t.length} song${q.t.length===1?"":"s"}${q.t.some(d=>d.local)?" · some only on their device":""}</small></span></div>`).join(""):`<p class="so-note">${s.pl_at||s.user_id?"No playlists shared.":"No playlists shared yet."}</p>`}</div>`}
  else if(v==="pl"){const s=SO.sh[SO.fid]||{},q=(s.pl||[])[SO.pid]||{t:[]};
    h=head(esc(q.name||"Playlist"),"friend")+`<div class="so-body"><div class="so-acts"><button class="addb sm" data-sc="playall:0">Play all</button><button class="ghost sm" data-sc="playall:1">Shuffle</button></div>${q.t.map((d,i)=>`<div class="so-song"><i style="background:${d.img?`center/cover url(${esc(d.img)})`:art("120,120,120")}"></i><span><b>${esc(d.t)}</b><small>${esc(d.a||"")}${d.local?" · only on their device":""}</small></span>${d.local?"":`<button class="so-ic" data-sc="song:${i}" aria-label="Play">${PLAY}</button>`}</div>`).join("")||`<p class="so-note">Empty.</p>`}</div>`}
  else if(v==="chat"){const p=SO.prof[SO.fid]||{},used=SO.msgs.reduce((a,m)=>a+(m.bytes||0),0),pct=Math.min(100,used/SO_CAP*100);
    h=head(esc(p.name||"Chat"),"home")+`<div class="so-note so-top">Messages disappear after 24 hours. Each chat holds 5 MB, then the oldest go first.<div class="so-meter"><i style="width:${pct.toFixed(1)}%"></i></div><small>${(used/1048576).toFixed(2)} of 5 MB used</small></div>
    <div class="so-body so-msgs" id="so-msgs">${SO.msgs.length?SO.msgs.map(soBubble).join(""):`<p class="so-note">Say hi. Nothing is kept for more than a day.</p>`}</div>
    <p class="so-err" id="so-err">${esc(SO.err)}</p><div class="so-comp"><button class="so-ic" data-sc="msong:" aria-label="Share what's playing">♪</button><label class="so-ic" aria-label="Send a photo">▣<input type="file" accept="image/*" data-so-img hidden></label><input id="so-t" placeholder="Message" maxlength="2000" autocomplete="off"><button class="addb sm" data-sc="send:">Send</button></div>`}
  el.innerHTML=`<div class="so-box">${h}</div>`;Object.keys(ki).forEach(i=>{const e=$(i);if(e){e.value=ki[i][0];if(ki[i][1]){try{e.focus({preventScroll:true});e.setSelectionRange(ki[i][2],ki[i][2])}catch(x){}}}});const nb=el.querySelector(".so-body");if(nb){if(v==="chat"&&!keep)nb.scrollTop=nb.scrollHeight;else if(v==="chat"&&sc&&sc.scrollHeight-sc.scrollTop-sc.clientHeight<80)nb.scrollTop=nb.scrollHeight;else nb.scrollTop=top}}
const CHAT='<svg viewBox="0 0 24 24"><path d="M4 5h16v11H9l-5 4z"/></svg>';
function soBubble(m){const mine=m.sender===soUid();let b="";
  if(m.kind==="song"){let d={};try{d=JSON.parse(m.body)}catch(e){}b=`<div class="so-song in"><i style="background:${d.img?`center/cover url(${esc(d.img)})`:art("201,151,90")}"></i><span><b>${esc(d.t||"Song")}</b><small>${esc(d.a||"")}</small></span>${d.local?"":`<button class="so-ic" data-sc="mplay:${m.id}" aria-label="Play">${PLAY}</button>`}</div>`}
  else if(m.kind==="img")b=`<img class="so-img" alt="Photo" src="${esc(m.body)}">`;else b=`<span>${esc(m.body)}</span>`;
  return `<div class="so-msg ${mine?"me":"they"}"><div class="so-bub">${b}</div><small>${new Date(m.created_at).toLocaleTimeString([],{hour:"2-digit",minute:"2-digit"})}</small></div>`}
const chatId=o=>[soUid(),o].sort().join(":");
async function soMsgs(){if(!SO.fid)return;const r=await sb("/rest/v1/messages?select=id,sender,rcpt,kind,body,bytes,created_at&chat=eq."+encodeURIComponent(chatId(SO.fid))+"&created_at=gt."+encodeURIComponent(isoAgo(SO_DAY))+"&order=created_at.asc&limit=300");SO.msgs=r||[];
  const seen=LS.get("milo.seen",{});const last=SO.msgs.length?SO.msgs[SO.msgs.length-1].created_at:new Date().toISOString();if(!(seen[SO.fid]>=last)){seen[SO.fid]=last;LS.set("milo.seen",seen)}delete SO.unread[SO.fid]}
async function soSend(kind,body){if(!SO.fid)return;SO.err="";try{await sb("/rest/v1/messages",{m:"POST",h:{Prefer:"return=minimal"},b:{rcpt:SO.fid,kind,body}});await soMsgs();soRender(false)}catch(e){SO.err=/too_large/i.test(e.message)?"That is too big to send.":/rate/i.test(e.message)?"Slow down a little.":sbMsg(e);soRender(true)}}
function imgSmall(f){return new Promise((res,rej)=>{const im=new Image(),u=URL.createObjectURL(f);im.onload=()=>{try{const m=1024,s=Math.min(1,m/Math.max(im.width,im.height)),c=document.createElement("canvas");c.width=Math.round(im.width*s);c.height=Math.round(im.height*s);c.getContext("2d").drawImage(im,0,0,c.width,c.height);URL.revokeObjectURL(u);res(c.toDataURL("image/jpeg",.65))}catch(e){rej(e)}};im.onerror=()=>{URL.revokeObjectURL(u);rej(new Error("bad image"))};im.src=u})}
document.addEventListener("change",async e=>{if(e.target.dataset&&e.target.dataset.soImg!=null){const f=e.target.files[0];e.target.value="";if(!f)return;try{const d=await imgSmall(f);if(d.length>380000)return toast("That photo is too big");soSend("img",d)}catch(er){toast("Couldn't use that photo")}}});
document.addEventListener("click",async e=>{const x=e.target.closest("[data-sc]");if(!x||!x.closest("#soc"))return;const [k,id]=(x.dataset.sc+":").split(/:(.*)/s).slice(0,2).map(s=>s.replace(/:$/,""));
  if(k==="close")return soClose();if(k==="home"){SO.v="home";SO.err="";soPoll();return soRender()}if(k==="friend"||k==="fr"){SO.v="friend";SO.fid=id||SO.fid;return soRender()}
  if(k==="find"){const q=($("so-q").value||"").trim().toLowerCase().replace(/^@/,"");if(!UN_RE.test(q)){SO.err="Usernames are 3 to 20 letters, numbers or _.";SO.found=null;return soRender()}SO.err="";
    try{const r=await sb("/rest/v1/rpc/find_user",{m:"POST",b:{u:q}});const p=Array.isArray(r)?r[0]:r;if(p&&p.id&&p.id!==soUid()){SO.found=p;SO.prof[p.id]=p}else if(p&&p.id===soUid()){SO.found=null;SO.err="That's you."}else SO.found=0}catch(er){SO.err=sbMsg(er)}return soRender()}
  if(k==="req"){try{await sb("/rest/v1/friendships",{m:"POST",h:{Prefer:"return=minimal"},b:{a:soUid(),b:id,status:"pending"}});toast("Request sent");SO.found=null;soLoad()}catch(er){SO.err=/duplicate|unique/i.test(er.message)?"You already have a request with them.":sbMsg(er);soRender()}return}
  if(k==="acc"){try{await sb("/rest/v1/friendships?a=eq."+id+"&b=eq."+soUid(),{m:"PATCH",h:{Prefer:"return=minimal"},b:{status:"accepted"}});toast("You're friends");SO.found=null;soLoad()}catch(er){SO.err=sbMsg(er);soRender()}return}
  if(k==="dec"){try{await sb("/rest/v1/friendships?or=(and(a.eq."+id+",b.eq."+soUid()+"),and(a.eq."+soUid()+",b.eq."+id+"))",{m:"DELETE"});soLoad()}catch(er){}return}
  if(k==="rmf"){modal(`<h3>Remove friend?</h3><p>You will stop seeing each other's playlists and listening, and your chat with them is deleted.</p><button class="addb" data-a="rmfc:${id}">Remove</button>`);return}
  if(k==="listen"){const n=(SO.sh[id]||{}).np;if(n)dnaPlay(n);return}
  if(k==="pl"){SO.pid=+id;SO.v="pl";return soRender()}
  if(k==="playall"){const q=((SO.sh[SO.fid]||{}).pl||[])[SO.pid];if(q)dnaList(q.t,id==="1");return}
  if(k==="song"){const q=((SO.sh[SO.fid]||{}).pl||[])[SO.pid];if(q)dnaPlay(q.t[+id]);return}
  if(k==="chat")return;
  if(k==="send"){const i=$("so-t"),v=(i.value||"").trim();if(!v)return;i.value="";return soSend("text",v)}
  if(k==="msong"){const d=dnaOf(T[S.cur]);if(!d||d.local)return toast(d?"Local songs can't be shared, only streams":"Play a song first");return soSend("song",JSON.stringify(d))}
  if(k==="mplay"){const m=SO.msgs.find(z=>String(z.id)===id);if(m){try{dnaPlay(JSON.parse(m.body))}catch(er){}}return}});
document.addEventListener("keydown",e=>{if(e.key==="Enter"&&e.target.id==="so-t"){const b=document.querySelector('#soc [data-sc="send:"]');if(b)b.click()}else if(e.key==="Enter"&&e.target.id==="so-q"){const b=document.querySelector('#soc [data-sc="find:"]');if(b)b.click()}});
async function rmFriend(id){try{await sb("/rest/v1/friendships?or=(and(a.eq."+id+",b.eq."+soUid()+"),and(a.eq."+soUid()+",b.eq."+id+"))",{m:"DELETE"});closeM();SO.v="home";toast("Friend removed");soLoad()}catch(er){toast(sbMsg(er))}}
