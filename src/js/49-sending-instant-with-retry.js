/* ----- sending (instant, with retry) ----- */
soSend=async function(kind,body){if(!SO.fid)return;SO.err="";
  const m={id:"t"+(++SO.tn),tmp:1,st:"sending",sender:soUid(),rcpt:SO.fid,kind,body,bytes:String(body).length+64,created_at:new Date().toISOString(),reply_to:SO.reply||null,rx:{},read_at:null};
  SO.reply=null;SO.msgs.push(m);SO.newc=0;mcRender(false);return soPost(m)};
async function soPost(m){m.st="sending";m.err="";
  try{const b={rcpt:m.rcpt,kind:m.kind,body:m.body};if(typeof m.reply_to==="number")b.reply_to=m.reply_to;
    const r=await sb("/rest/v1/messages?select=id,created_at,bytes",{m:"POST",h:{Prefer:"return=representation"},b});const row=Array.isArray(r)?r[0]:r;
    if(row&&row.id){if(SO.msgs.some(x=>x.id===row.id&&x!==m))SO.msgs=SO.msgs.filter(x=>x!==m);else Object.assign(m,row,{tmp:0,st:"sent"});SO.last[m.rcpt]=Object.assign({},m,{pv:m.kind==="text"?m.body.slice(0,80):m.kind==="song"?m.body.slice(0,300):null})}else{m.tmp=0;m.st="sent"}}
  catch(e){m.st="fail";m.err=/too_large/i.test(e.message)?"Too big to send":/rate/i.test(e.message)?"Slow down a little":/rls|policy|403/i.test(e.message)?"You're not friends anymore":"Not sent"}
  if(SO.on&&SO.v==="chat")mcRender(true)}
document.addEventListener("input",e=>{if(e.target.id==="so-t"&&RT.chat&&Date.now()-RT.ts>2500){RT.ts=Date.now();rtSend({topic:rtTopic(RT.chat),event:"broadcast",payload:{type:"broadcast",event:"typing",payload:{uid:soUid()}}})}});
