/* ----- screens ----- */
const mcBack='<svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg>';
function mcSubText(){const f=SO.fid,p=SO.prof[f]||{};if((RT.typ[f]||0)>Date.now())return'<span class="ty">typing…</span>';const s=SO.sh[f];if(live(s)&&s.np&&s.np.t)return"♪ "+esc(s.np.t);return"@"+esc(p.username||"")}
function mcSub(){const e=$("so-sub");if(e)e.innerHTML=mcSubText();const t=$("mc-typ");if(t){const on=(RT.typ[SO.fid]||0)>Date.now();if(t.hidden===on){t.hidden=!on;const l=$("so-msgs");if(on&&l&&l.scrollHeight-l.scrollTop-l.clientHeight<140)l.scrollTop=l.scrollHeight}}}
const mcLink=t=>esc(t).replace(/(https?:\/\/[^\s<]+[^\s<.,;:!?)\]])/g,'<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>');
function mcRxHtml(m){const v=Object.values(m.rx||{});if(!v.length)return"";const c={};v.forEach(e=>c[e]=(c[e]||0)+1);const mine=(m.rx||{})[soUid()];return`<span class="mc-rx">${Object.keys(c).map(e=>`<i${mine===e?' class="on"':""}>${esc(e)}${c[e]>1?"<em>"+c[e]+"</em>":""}</i>`).join("")}</span>`}
function mcBub(m,gs,ge){const mine=m.sender===soUid();let c="";
  if(m.reply_to){const q=SO.msgs.find(x=>x.id===m.reply_to);c+=`<div class="mc-q" data-sc="mjump:${m.reply_to}"><b>${q?(q.sender===soUid()?"You":esc((SO.prof[q.sender]||{}).name||"They")):"Message"}</b><span>${q?esc(mcPrev(q,1)):"Expired"}</span></div>`}
  if(m.kind==="song"){let d={};try{d=JSON.parse(m.body)}catch(e){}c+=`<div class="mc-song"><i style="background:${d.img&&/^https:/.test(d.img)?`center/cover url(${esc(d.img)})`:art("249,156,0")}"></i><span><b>${esc(d.t||"Song")}</b><small>${esc(d.a||"")}</small></span>${d.local?`<em>On their device</em>`:`<button class="mc-play" data-sc="mplay:${m.id}" aria-label="Play">${PLAY}<span>Play</span></button>`}</div>`}
  else if(m.kind==="img")c+=`<img class="so-img" alt="Photo" src="${esc(m.body)}" data-sc="mimg:${m.id}">`;
  else if(m.kind==="voice")c+=`<span>🎤 Voice message · listen in miloCHAT</span>`;
  else{const big=/^(\p{Extended_Pictographic}|️|‍|\s){1,12}$/u.test(m.body)&&[...m.body.trim()].length<=8;c+=`<span class="${big?"mc-big":""}">${mcLink(m.body)}</span>`}
  const meta=ge||m.st==="fail"?(m.st==="fail"?`<button class="mc-fail" data-sc="mretry:${m.id}">${esc(m.err||"Not sent")} · Retry</button>`:`<small class="mc-m">${mcTime(new Date(m.created_at))}${mine?mcTick(m):""}</small>`):"";
  return `<div class="mc-r ${mine?"me":"they"}${gs?" gs":""}${ge?" ge":""}${m.kind==="img"?" im":""}" data-mid="${m.id}"><div class="mc-b${m.st==="fail"?" bad":""}" data-lp="${m.id}">${c}</div>${mcRxHtml(m)}${meta}</div>`}
function mcItems(){const ms=SO.msgs;if(!ms.length)return`<div class="mc-empty"><b>Say hi 👋</b><span>Messages disappear after 24 hours. Songs you share open for your friend as a stream.</span></div>`;
  let out="",day="",prev=null;ms.forEach((m,i)=>{const d=new Date(m.created_at),dl=mcDay(d);if(dl!==day){out+=`<div class="mc-day"><span>${dl}</span></div>`;day=dl;prev=null}
    const nx=ms[i+1],gs=!prev||prev.sender!==m.sender||d-new Date(prev.created_at)>120000||!!m.reply_to,ge=!nx||nx.sender!==m.sender||new Date(nx.created_at)-d>120000||mcDay(new Date(nx.created_at))!==dl||!!nx.reply_to;
    out+=mcBub(m,gs,ge);prev=m});return out}
function mcSheet(){const m=SO.msgs.find(x=>String(x.id)===String(SO.act));if(!m)return"";const mine=m.sender===soUid(),left=Math.max(0,new Date(m.created_at).getTime()+SO_DAY-Date.now()),h=Math.floor(left/36e5),mi=Math.floor(left%36e5/6e4);
  return `<div class="mc-ov" data-sc="mx:"><div class="mc-sheet" data-sc="mkeep:"><div class="mc-rxs">${MC_RX.map(e=>`<button class="${(m.rx||{})[soUid()]===e?"on":""}" data-sc="mrx:${e}">${e}</button>`).join("")}</div>
  <button data-sc="mrep:">Reply</button>${m.kind==="text"?`<button data-sc="mcopy:">Copy text</button>`:""}${mine&&!m.tmp?`<button class="dg" data-sc="mun:">Unsend</button>`:""}<small>Disappears in ${h?h+" h ":""}${mi} min</small></div></div>`}
function mcOv(){return(SO.act?mcSheet():"")+(SO.view?`<div class="mc-lb" data-sc="mclose:"><img alt="" src="${esc(SO.view)}"></div>`:"")}
function mcRp(){const q=SO.reply&&SO.msgs.find(x=>x.id===SO.reply);return q?`<div class="mc-rp"><span><b>Replying to ${q.sender===soUid()?"yourself":esc((SO.prof[q.sender]||{}).name||"them")}</b><small>${esc(mcPrev(q,1))}</small></span><button data-sc="mrcl:" aria-label="Cancel">✕</button></div>`:""}
function mcRender(keep){if(!SO.on||SO.v!=="chat")return;const el=soEl(),f=SO.fid,p=SO.prof[f]||{};let box=el.querySelector('.mc[data-f="'+f+'"]');
  const used=SO.msgs.reduce((a,m)=>a+(m.tmp?0:m.bytes||0),0),pct=Math.min(100,used/SO_CAP*100);
  if(!box){const c=p.av&&p.av.t==="color"?p.av.c:"249,156,0";
    el.innerHTML=`<div class="so-box mc" data-f="${f}" style="--pc:${esc(c)}"><div class="mc-hd"><button class="so-b" data-sc="home" aria-label="Back">${mcBack}</button><button class="mc-who" data-sc="fr:${f}"><span class="mc-av ${live(SO.sh[f])?"lv":""}">${soAv(p)}</span><span class="mc-nm"><b>${esc(p.name||p.username||"Chat")}</b><small id="so-sub"></small></span></button></div>
    <div class="mc-dis"><span>Disappearing · 24 h</span><div class="so-meter"><i id="mc-mt"></i></div><span id="mc-us"></span></div>
    <div class="so-body so-msgs mc-list" id="so-msgs"><div id="mc-it"></div><div class="mc-typ" id="mc-typ" hidden><i></i><i></i><i></i></div></div>
    <button class="mc-new" id="mc-new" data-sc="mnew:" hidden></button><div id="mc-ov"></div><p class="so-err" id="so-err"></p>
    <div class="mc-comp"><div id="mc-rp"></div><div class="mc-in"><button class="mc-ic" data-sc="msong:" aria-label="Share what's playing">♪</button><label class="mc-ic" aria-label="Send a photo"><svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="3"/><circle cx="9" cy="11" r="2"/><path d="M4 18l5-5 4 4 3-3 4 4"/></svg><input type="file" accept="image/*" data-so-img hidden></label><input id="so-t" placeholder="Message" maxlength="2000" autocomplete="off" enterkeyhint="send"><button class="mc-send" data-sc="send:" aria-label="Send"><svg viewBox="0 0 24 24"><path d="M12 19V5M5 12l7-7 7 7"/></svg></button></div></div></div>`;
    box=el.querySelector(".mc");keep=false;const l0=$("so-msgs");l0.onscroll=()=>{if(SO.newc&&l0.scrollHeight-l0.scrollTop-l0.clientHeight<60){SO.newc=0;const b=$("mc-new");if(b)b.hidden=true}}}
  const l=$("so-msgs"),near=l.scrollHeight-l.scrollTop-l.clientHeight<90,top=l.scrollTop;
  $("mc-it").innerHTML=mcItems();$("mc-mt").style.width=pct.toFixed(1)+"%";$("mc-us").textContent=(used/1048576).toFixed(1)+" / 5 MB";$("mc-ov").innerHTML=mcOv();$("mc-rp").innerHTML=mcRp();$("so-err").textContent=SO.err||"";
  const nb=$("mc-new");nb.hidden=!SO.newc;if(SO.newc)nb.textContent="↓ "+SO.newc+" new message"+(SO.newc>1?"s":"");mcSub();
  const stick=!keep||near;if(stick)l.scrollTop=l.scrollHeight;else l.scrollTop=top;
  l.querySelectorAll("img.so-img").forEach(i=>{if(!i.complete)i.onload=()=>{if(stick)l.scrollTop=l.scrollHeight}})}
var mcRender_=mcRender;
/* chats list */
function mcSeg(tab){const un=Object.values(SO.unread).reduce((a,b)=>a+b,0);return`<div class="mc-seg"><button class="${tab==="chats"?"on":""}" data-sc="tab:chats">Chats${un?`<i>${un>99?"99+":un}</i>`:""}</button><button class="${tab==="friends"?"on":""}" data-sc="tab:friends">Friends${SO.rq.length?`<i>${SO.rq.length}</i>`:""}</button></div>`}
function mcHome(keep){const el=soEl(),tab=SO.tab||(SO.fr.some(id=>SO.last[id])?"chats":"friends");
  if(tab==="friends"){_soRender(keep);const hd=el.querySelector(".so-head");if(hd){const h=hd.querySelector("h2");if(h)h.textContent="Messages";hd.insertAdjacentHTML("afterend",mcSeg("friends"))}return}
  const sc=el.querySelector(".so-body"),top=sc?sc.scrollTop:0;
  const rows=SO.fr.filter(id=>SO.last[id]).sort((a,b)=>String(SO.last[b].created_at).localeCompare(String(SO.last[a].created_at)));
  const liv=SO.fr.filter(id=>live(SO.sh[id]));
  const nb=((NV.cap&&puOk()&&Date.now()-LS.get("milo.pushAt",0)>864e5)||(window.Notification&&Notification.permission==="default"&&puOk()))&&!LS.get("milo.nbn",0)?`<div class="mc-nb"><span><b>Get notified</b><small>See new messages and requests when milo. is in the background</small></span><button data-sc="notif:on">Turn on</button><button class="x" data-sc="notif:no" aria-label="Dismiss">✕</button></div>`:"";
  el.innerHTML=`<div class="so-box"><div class="so-head"><button class="so-b" data-sc="close" aria-label="Back">${mcBack}</button><h2>Messages</h2></div>${mcSeg("chats")}<div class="so-body">${nb}
  ${liv.length?`<div class="mc-live">${liv.map(id=>{const p=SO.prof[id]||{},n=SO.sh[id].np;return`<button data-sc="fr:${id}"><span class="mc-av lv">${soAv(p)}</span><b>${esc(p.name||p.username||"?")}</b><small>♪ ${esc(n.t||"")}</small></button>`}).join("")}</div>`:""}
  ${rows.length?rows.map(id=>{const p=SO.prof[id]||{},l=SO.last[id],n=SO.unread[id]||0,mine=l.sender===soUid();return`<div class="mc-row${n?" un":""}" data-sc="chat:${id}"><span class="mc-av${live(SO.sh[id])?" lv":""}">${soAv(p)}</span><span class="mc-tx"><b>${esc(p.name||p.username||"?")}</b><small>${mine?(l.read_at?'<svg class="tk rd" viewBox="0 0 24 24"><path d="M2 13l4 4 8-9M10 16l1 1 9-10"/></svg>':'<svg class="tk" viewBox="0 0 24 24"><path d="M5 13l4 4 10-10"/></svg>'):""}${esc(mcPrev(l))}</small></span><span class="mc-mt"><time>${mcStamp(l.created_at)}</time>${n?`<i>${n>99?"99+":n}</i>`:""}</span></div>`}).join(""):`<div class="mc-empty"><b>No chats yet</b><span>${SO.fr.length?"Open a friend and say hi.":"Add a friend by username to start."}</span><button class="addb sm" data-sc="tab:friends">${SO.fr.length?"Pick a friend":"Add friends"}</button></div>`}
  </div></div>`;const b=el.querySelector(".so-body");if(b&&keep)b.scrollTop=top}
const _soRender=soRender;
soRender=function(keep){if(!SO.on)return;if(SO.v==="chat")return mcRender(keep);if(SO.v==="home")return mcHome(keep);_soRender(keep)};
function mcOpen(id){if(!SO.on){SO.on=1;soEl().hidden=false}mcChat(id)}
async function mcChat(id){SO.fid=id||SO.fid;SO.v="chat";SO.msgs=[];SO.err="";SO.newc=0;SO.act=null;SO.view=null;SO.reply=null;
  const n="milo-chat-"+chatId(SO.fid).replace(/:/g,"_");if(RT.chat&&RT.chat!==n)rtLeave(RT.chat);RT.chat=n;rtJoin(n,{broadcast:{self:false},presence:{key:""}});
  soEl().innerHTML="";soRender();soPoll();try{await soMsgs()}catch(er){SO.err=sbMsg(er)}mcRender(false);soBadge()}
