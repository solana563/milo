/* ----- Discord Rich Presence (Windows desktop app): shows what you're playing on your Discord profile ----- */
const DCP={cfg:LS.get("milo.discord",{on:false,id:""}),sig:""};
const dpOn=()=>!!(window.miloDesktop&&miloDesktop.presence);
function dpPush(force){if(!dpOn())return;const t=T[S.cur],c=DCP.cfg||{};
  const info=t&&!t.radio&&c.on?{title:t.t,artist:t.a,album:t.al&&!/^(YouTube|Internet Archive)/i.test(t.al)?t.al:"",playing:!!S.playing,pos:S.pos||0,dur:t.d||0,img:/^https:/.test(t.img||"")?t.img:""}:null;
  const sig=info?[info.title,info.playing,Math.round(info.pos/10)].join("|"):"off";if(!force&&sig===DCP.sig)return;DCP.sig=sig;
  try{miloDesktop.presence(c,info)}catch(e){}}
setInterval(()=>dpPush(false),5000);
{const _s=sync;sync=function(){const r=_s.apply(this,arguments);try{dpPush(true)}catch(e){}return r}}
function dpCard(){if(!dpOn())return"";const c=DCP.cfg||{};
  return `<button class="sxb" data-dp="open"><b>Discord presence</b><small>${c.on&&c.id?"On · shows what you're playing on Discord":"Show what you're playing on your Discord profile"}</small></button>`}
{const _x=sxExtra;sxExtra=function(){return _x()+dpCard()}}
document.addEventListener("click",e=>{const b=e.target.closest("[data-dp]");if(!b)return;const k=b.dataset.dp,c=DCP.cfg||{};
  if(k==="open")modal(`<h3>Discord presence</h3><p>Create an application at discord.com/developers/applications (name it <b>milo.</b>), then paste its Application ID here. The Discord desktop app must be running. The cover shows for songs that have an online picture.</p><input id="dpId" placeholder="Application ID" value="${esc(c.id||"")}" inputmode="numeric" style="width:100%;margin:8px 0"><button class="addb" data-dp="on">Turn on</button> <button class="ghost" data-dp="off">Turn off</button> <button class="ghost" data-x>Cancel</button>`);
  else if(k==="on"){const id=($("dpId").value||"").trim();if(!/^\d{15,25}$/.test(id)){toast("That doesn't look like an Application ID");return}DCP.cfg={on:true,id};LS.set("milo.discord",DCP.cfg);closeM();dpPush(true);toast("Discord presence on")}
  else if(k==="off"){DCP.cfg={on:false,id:(c.id||"")};LS.set("milo.discord",DCP.cfg);closeM();dpPush(true);toast("Discord presence off")}});
