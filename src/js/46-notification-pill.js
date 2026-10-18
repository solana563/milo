/* ----- notification pill ----- */
var PL={q:[],on:0,t:0};
function pill(o){if(!o)return;const l=PL.q[PL.q.length-1];if(l&&l.id===o.id&&l.title===o.title&&l.act===o.act)PL.q[PL.q.length-1]=o;else PL.q.push(o);if(PL.q.length>4)PL.q.shift();if(!PL.on)pillNext()}
function pillNext(){const o=PL.q.shift();if(!o){PL.on=0;return}PL.on=1;let e=$("npill");
  if(!e){e=document.createElement("button");e.id="npill";e.setAttribute("aria-live","polite");$("app").appendChild(e);e.onclick=()=>{const a=e._o;pillHide();if(!a)return;if(a.act==="chat"&&a.id)mcOpen(a.id);else if(a.act==="friends"){SO.tab="friends";if(!SO.on){SO.on=1;soEl().hidden=false}SO.v="home";soRender();soLoad();soPoll()}}}
  e._o=o;const p=o.id?SO.prof[o.id]:null;e.innerHTML=(p?soAv(p):'<span class="np-i">↻</span>')+`<span><b>${esc(o.title)}</b><small>${esc(o.text)}</small></span>`;
  e.className="show";clearTimeout(PL.t);PL.t=setTimeout(pillHide,5200)}
function pillHide(){const e=$("npill");if(e)e.className="";clearTimeout(PL.t);PL.on=0;setTimeout(()=>{if(!PL.on)pillNext()},320)}
function mcNotify(m,other){const p=SO.prof[other]||{},nm=p.name||p.username||"New message",tx=mcPrev(m,1);
  if(!(SO.on&&SO.v==="chat"&&SO.fid===other&&!document.hidden))pill({id:other,title:nm,text:tx,act:"chat"});
  try{buzz(8)}catch(e){}
  try{if(document.hidden&&window.Notification&&Notification.permission==="granted"&&LS.get("milo.notif",1)){const n=new Notification(nm,{body:tx,tag:"milo-"+other,icon:"icons/icon-192.png"});n.onclick=()=>{try{window.focus()}catch(e){}mcOpen(other);n.close()}}}catch(e){}}
