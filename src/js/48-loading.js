/* ----- loading ----- */
const _soBadge=soBadge;
soBadge=function(){const n=SO.rq.length+Object.values(SO.unread).reduce((a,b)=>a+b,0);document.querySelectorAll("[data-sx=friends]").forEach(b=>{b.classList.toggle("dot",n>0);if(n>0)b.setAttribute("data-n",n>9?"9+":n);else b.removeAttribute("data-n")});
  try{const t=document.title.replace(/^\(\d+\+?\)\s*/,"");document.title=(n>0?"("+n+") ":"")+t}catch(e){}};
soLoad=async function(quiet){if(!soUid()||!termsOk()||SO.busy)return;SO.busy=1;SO.at=Date.now();
  try{const me_=soUid(),f=await sb("/rest/v1/friendships?select=a,b,status,created_at&or=(a.eq."+me_+",b.eq."+me_+")");
    const acc=[],rq=[],out=[];(f||[]).forEach(x=>{const o=x.a===me_?x.b:x.a;if(x.status==="accepted")acc.push(o);else if(x.b===me_)rq.push(o);else out.push(o)});
    const ids=[...new Set([...acc,...rq,...out])],prevRq=SO.rq.length,prevUn=Object.assign({},SO.unread),first=!SO.loaded;SO.fr=acc;SO.rq=rq;SO.out=out;
    if(ids.length){const ps=await sb("/rest/v1/profiles?select=id,username,name,av&id=in.("+ids.join(",")+")");(ps||[]).forEach(p=>SO.prof[p.id]=p);
      if(acc.length){const sh=await sb("/rest/v1/shared?select=user_id,np,np_at,pl,pl_at&user_id=in.("+acc.join(",")+")");SO.sh={};(sh||[]).forEach(r=>SO.sh[r.user_id]=r)}}
    const ms=await sb("/rest/v1/messages?select=id,chat,sender,rcpt,kind,created_at,read_at,pv&or=(sender.eq."+me_+",rcpt.eq."+me_+")&created_at=gt."+encodeURIComponent(isoAgo(SO_DAY))+"&order=created_at.desc&limit=300");
    SO.last={};SO.unread={};(ms||[]).forEach(m=>{const o=m.sender===me_?m.rcpt:m.sender;if(!SO.last[o])SO.last[o]=m;if(m.rcpt===me_&&!m.read_at)SO.unread[o]=(SO.unread[o]||0)+1});
    if(SO.on&&SO.v==="chat"&&!document.hidden)delete SO.unread[SO.fid];
    SO.loaded=1;
    if(!first&&!RT.live){Object.keys(SO.unread).forEach(k=>{if(SO.unread[k]>(prevUn[k]||0)&&!(SO.on&&SO.v==="chat"&&SO.fid===k&&!document.hidden))mcNotify(SO.last[k],k)});
      if(SO.rq.length>prevRq){const id=SO.rq[SO.rq.length-1],p=SO.prof[id]||{};pill({id,title:p.name||p.username||"Someone",text:"sent you a friend request",act:"friends"})}}
    if(SO.on&&SO.v==="chat")await soMsgs();
    soBadge();if(SO.on&&SO.v==="chat")mcRender(true);else if(SO.on)soRender(quiet)}catch(e){if(SO.on&&!quiet){SO.err=sbMsg(e);soRender()}}
  SO.busy=0};
soMsgs=async function(){if(!SO.fid)return;const fid=SO.fid;
  const r=await sb("/rest/v1/messages?select=id,sender,rcpt,kind,body,bytes,created_at,read_at,reply_to,rx&chat=eq."+encodeURIComponent(chatId(fid))+"&created_at=gt."+encodeURIComponent(isoAgo(SO_DAY))+"&order=created_at.asc&limit=300");
  if(fid!==SO.fid)return;const srv=r||[],keep=SO.msgs.filter(m=>(m.tmp||m.st==="fail")&&!srv.some(x=>x.id===m.id));
  const l=$("so-msgs"),near=!l||l.scrollHeight-l.scrollTop-l.clientHeight<90,had=SO.msgs.filter(m=>!m.tmp).length;
  if(srv.length>had&&!near&&had)SO.newc+=srv.length-had;
  SO.msgs=srv.concat(keep);const lm=srv[srv.length-1];if(lm)SO.last[fid]=lm;mcRead()};
function soPoll(){clearInterval(SO.timer);SO.timer=setInterval(()=>{if(SO.on)soLoad(SO.v==="chat")},SO.v==="chat"?(RT.live?20000:2500):(RT.live?45000:8000))}
function soStart(){soLoad(true);RT.stop=0;rtConnect();if(soTimer2)return;soTimer2=1;setInterval(()=>{if(soUid()&&termsOk()&&!SO.on&&!document.hidden&&Date.now()-SO.at>(RT.live?150000:20000))soLoad(true);if(!RT.ws&&soUid()&&termsOk())rtConnect()},10000)}
const _soStop=soStop;soStop=function(){_soStop();rtStop();SO.last={};SO.loaded=0};
const _soClose=soClose;soClose=function(){mcLeave();_soClose()};
function mcLeave(){if(RT.chat){rtLeave(RT.chat);RT.chat=""}SO.act=null;SO.view=null;SO.reply=null}
async function mcRead(){const f=SO.fid;if(!f||!SO.on||SO.v!=="chat"||document.hidden)return;const un=SO.msgs.filter(m=>m.sender===f&&!m.read_at&&!m.tmp);
  if(!un.length){if(SO.unread[f]){delete SO.unread[f];soBadge()}return}
  const at=new Date().toISOString();un.forEach(m=>m.read_at=at);delete SO.unread[f];soBadge();if(SO.last[f]&&SO.last[f].sender===f)SO.last[f].read_at=at;
  try{await sb("/rest/v1/messages?rcpt=eq."+soUid()+"&sender=eq."+f+"&read_at=is.null",{m:"PATCH",h:{Prefer:"return=minimal"},b:{read_at:at}})}catch(e){un.forEach(m=>m.read_at=null)}}
