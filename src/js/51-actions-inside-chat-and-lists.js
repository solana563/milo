/* ----- actions inside chat and lists ----- */
async function mcReact(m,e){const me_=soUid(),rx=Object.assign({},m.rx||{});if(rx[me_]===e)delete rx[me_];else rx[me_]=e;const prev=m.rx;m.rx=rx;SO.act=null;mcRender(true);if(m.tmp)return;
  try{await sb("/rest/v1/messages?id=eq."+m.id,{m:"PATCH",h:{Prefer:"return=minimal"},b:{rx}})}catch(er){m.rx=prev;mcRender(true);toast("Couldn't react")}}
document.addEventListener("click",e=>{const x=e.target.closest("[data-sc=home]");if(x&&x.closest("#soc")&&RT.chat){SO.tab="chats";mcLeave()}},true);
document.addEventListener("click",async e=>{const x=e.target.closest("[data-sc]");if(!x||!x.closest("#soc"))return;const s=x.dataset.sc,i=s.indexOf(":"),k=i<0?s:s.slice(0,i),id=i<0?"":s.slice(i+1);
  if(k==="home"||k==="fr"||k==="friend"||k==="close"){const was=!!RT.chat||window.__mcWas;window.__mcWas=0;mcLeave();if(k==="home"&&was){SO.tab="chats";soRender()}return}
  if(k==="tab"){SO.tab=id==="friends"?"friends":"chats";SO.v="home";return soRender()}
  if(k==="notif"){LS.set("milo.nbn",1);if(id==="on"&&window.Notification){try{await Notification.requestPermission()}catch(er){}}return soRender(true)}
  if(k==="mnew"){SO.newc=0;const l=$("so-msgs");if(l)l.scrollTo({top:l.scrollHeight,behavior:"smooth"});const b=$("mc-new");if(b)b.hidden=true;return}
  if(k==="mx"){SO.act=null;SO.view=null;return mcRender(true)}
  if(k==="mclose"){SO.view=null;return mcRender(true)}
  if(k==="mkeep")return;
  if(k==="mimg"){const m=SO.msgs.find(z=>String(z.id)===id);if(m){SO.view=m.body;mcRender(true)}return}
  if(k==="mrx"){const m=SO.msgs.find(z=>String(z.id)===String(SO.act));if(m)mcReact(m,id);return}
  if(k==="mrep"){const m=SO.msgs.find(z=>String(z.id)===String(SO.act));SO.act=null;if(m&&!m.tmp)SO.reply=m.id;mcRender(true);const t=$("so-t");if(t)t.focus();return}
  if(k==="mrcl"){SO.reply=null;return mcRender(true)}
  if(k==="mcopy"){const m=SO.msgs.find(z=>String(z.id)===String(SO.act));SO.act=null;if(m){try{navigator.clipboard.writeText(m.body);toast("Copied")}catch(er){}}return mcRender(true)}
  if(k==="mun"){const m=SO.msgs.find(z=>String(z.id)===String(SO.act));SO.act=null;if(!m||m.tmp)return mcRender(true);SO.msgs=SO.msgs.filter(z=>z!==m);mcRender(true);try{await sb("/rest/v1/messages?id=eq."+m.id,{m:"DELETE"})}catch(er){SO.msgs.push(m);SO.msgs.sort((a,b)=>String(a.created_at).localeCompare(String(b.created_at)));mcRender(true);toast("Couldn't unsend")}return}
  if(k==="mretry"){const m=SO.msgs.find(z=>String(z.id)===id);if(m){mcRender(true);soPost(m)}return}
  if(k==="mjump"){const t=document.querySelector('#soc [data-mid="'+id+'"]');if(t){t.scrollIntoView({block:"center",behavior:"smooth"});t.classList.add("hl");setTimeout(()=>t.classList.remove("hl"),1400)}return}
  if(k==="chat"){return mcChat(id)}});
/* long press / double tap / right click on a message */
(function(){let t=0,sx=0,sy=0;const open=id=>{SO.act=id;mcRender(true);try{buzz(10)}catch(e){}};
  document.addEventListener("pointerdown",e=>{const b=e.target.closest&&e.target.closest("#soc .mc-b");if(!b||e.target.closest("[data-sc^=mplay],a"))return;sx=e.clientX;sy=e.clientY;clearTimeout(t);t=setTimeout(()=>{t=0;open(b.dataset.lp)},430)});
  ["pointerup","pointercancel","scroll"].forEach(n=>document.addEventListener(n,()=>{clearTimeout(t)},true));
  document.addEventListener("pointermove",e=>{if(t&&(Math.abs(e.clientX-sx)>8||Math.abs(e.clientY-sy)>8)){clearTimeout(t);t=0}});
  document.addEventListener("contextmenu",e=>{const b=e.target.closest&&e.target.closest("#soc .mc-b");if(b){e.preventDefault();clearTimeout(t);open(b.dataset.lp)}});
  document.addEventListener("dblclick",e=>{const b=e.target.closest&&e.target.closest("#soc .mc-b");if(!b)return;const m=SO.msgs.find(z=>String(z.id)===String(b.dataset.lp));if(m&&!m.tmp)mcReact(m,"❤️")})})();
/* come back to the app: catch up fast, mark read, pull cloud changes */
var mcVis=0;function mcWake(){if(!soUid()||!termsOk())return;if(Date.now()-mcVis<4000)return;mcVis=Date.now();if(!RT.ws)rtConnect();soLoad(true);mcRead();if(S.cloud)sbPull(false)}
document.addEventListener("visibilitychange",()=>{if(!document.hidden)mcWake()});addEventListener("online",()=>{mcVis=0;mcWake()});addEventListener("focus",mcWake);
sbPushSoon=function(o){if(!S.cloud||S.cloudApplying)return;S.cloudPend=o;clearTimeout(sbT);sbT=setTimeout(sbPush,600)};

