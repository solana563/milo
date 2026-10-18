/* ----- incoming, updates, deletes ----- */
function soIn(m){const me_=soUid(),mine=m.sender===me_,other=mine?m.rcpt:m.sender;if(!m.id)return;
  if(m.pv==null)m.pv=m.kind==="text"?String(m.body||"").slice(0,80):m.kind==="song"?String(m.body||"").slice(0,300):null;
  const cur=SO.last[other];if(!cur||String(cur.created_at)<=String(m.created_at))SO.last[other]=m;
  const open=SO.on&&SO.v==="chat"&&SO.fid===other;
  if(open){if(SO.msgs.some(x=>x.id===m.id))return;
    if(mine){const t=SO.msgs.find(x=>x.tmp&&x.kind===m.kind&&x.body===m.body);if(t){Object.assign(t,m,{tmp:0,st:"sent"});mcRender(true);return}}
    const l=$("so-msgs"),near=!l||l.scrollHeight-l.scrollTop-l.clientHeight<90;SO.msgs.push(m);if(!mine&&!near)SO.newc++;mcRender(true);if(!mine)mcRead()}
  if(!mine){if(!open||document.hidden){SO.unread[other]=(SO.unread[other]||0)+1}mcNotify(m,other);if(!SO.prof[other])soLoad(true)}
  soBadge();if(SO.on&&SO.v==="home")soRender(true)}
function soUpd(r){const m=SO.msgs.find(x=>x.id===r.id);if(m){["read_at","rx"].forEach(k=>{if(k in r)m[k]=r[k]})}
  Object.keys(SO.last).forEach(k=>{if(SO.last[k]&&SO.last[k].id===r.id&&"read_at" in r)SO.last[k].read_at=r.read_at});if(SO.on&&SO.v==="chat")mcRender(true);else if(SO.on&&SO.v==="home")soRender(true)}
function soDel(id){if(id==null)return;const n=SO.msgs.length;SO.msgs=SO.msgs.filter(x=>x.id!==id);if(SO.msgs.length!==n&&SO.on&&SO.v==="chat")mcRender(true);else if(SO.on&&SO.v==="home")soLoad(true)}
