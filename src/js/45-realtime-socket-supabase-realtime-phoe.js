/* ----- realtime socket (Supabase Realtime, Phoenix protocol). Falls back to polling when it is not available. ----- */
const rtTopic=n=>"realtime:"+n;
function rtSend(o){try{if(RT.ws&&RT.ws.readyState===1){o.ref=o.ref||String(++RT.ref);RT.ws.send(JSON.stringify(o))}}catch(e){}}
function rtJoin(name,cfg){RT.joined[name]=cfg;if(RT.open&&S.cloud)rtSend({topic:rtTopic(name),event:"phx_join",payload:{config:cfg,access_token:S.cloud.at}})}
function rtLeave(name){delete RT.joined[name];rtSend({topic:rtTopic(name),event:"phx_leave",payload:{}})}
function rtConnect(){if(RT.ws||RT.stop||!soUid()||!termsOk()||!window.WebSocket||navigator.onLine===false)return;
  let ws;try{ws=new WebSocket(SBC.url.replace(/^http/,"ws")+"/realtime/v1/websocket?apikey="+encodeURIComponent(SBC.key)+"&vsn=1.0.0")}catch(e){return}
  RT.ws=ws;
  ws.onopen=()=>{RT.open=1;RT.rc=0;clearInterval(RT.hb);RT.hb=setInterval(()=>rtSend({topic:"phoenix",event:"heartbeat",payload:{}}),25000);
    RT.joined["milo-inbox"]={broadcast:{self:false},presence:{key:""},postgres_changes:[{event:"*",schema:"public",table:"messages"},{event:"*",schema:"public",table:"friendships"},{event:"*",schema:"public",table:"shared"},{event:"*",schema:"public",table:"milo_sync"}]};
    Object.keys(RT.joined).forEach(n=>rtSend({topic:rtTopic(n),event:"phx_join",payload:{config:RT.joined[n],access_token:S.cloud.at}}))};
  ws.onmessage=e=>{let m;try{m=JSON.parse(e.data)}catch(x){return}
    if(m.event==="phx_reply"){if(m.topic===rtTopic("milo-inbox")){const ok=m.payload&&m.payload.status==="ok";if(ok&&!RT.live){RT.live=1;soPoll();soLoad(true)}else if(!ok&&RT.live){RT.live=0;soPoll()}else if(!ok)RT.live=0}return}
    if(m.event==="postgres_changes"){try{rtChange(m.payload&&m.payload.data)}catch(x){}return}
    if(m.event==="broadcast"){rtBc(m.topic,m.payload);return}
    if((m.event==="phx_close"||m.event==="phx_error")&&m.topic===rtTopic("milo-inbox")){RT.live=0;soPoll()}};
  ws.onclose=()=>{if(RT.ws===ws){RT.ws=null;RT.open=0;RT.live=0;clearInterval(RT.hb);if(SO.on)soPoll();if(!RT.stop)setTimeout(rtConnect,Math.min(30000,1000*Math.pow(2,RT.rc++)))}};
  ws.onerror=()=>{try{ws.close()}catch(e){}}}
function rtStop(){RT.stop=1;RT.joined={};RT.chat="";RT.live=0;try{if(RT.ws)RT.ws.close()}catch(e){}RT.ws=null;RT.open=0;clearInterval(RT.hb)}
setInterval(()=>{if(RT.open&&S.cloud)sbFresh().then(()=>Object.keys(RT.joined).forEach(n=>rtSend({topic:rtTopic(n),event:"access_token",payload:{access_token:S.cloud.at}})))},240000);
function rtBc(topic,p){if(RT.chat&&topic===rtTopic(RT.chat)&&p&&p.event==="typing"){const u=p.payload&&p.payload.uid;if(u&&u!==soUid()){RT.typ[u]=Date.now()+4200;mcSub();setTimeout(mcSub,4300)}}}
function rtChange(d){if(!d||!soUid())return;const t=d.table,r=d.record||{},o=d.old_record||{},me_=soUid();
  if(t==="messages"){if(d.type==="INSERT")soIn(r);else if(d.type==="UPDATE")soUpd(r);else if(d.type==="DELETE")soDel(o.id||r.id)}
  else if(t==="friendships"){const a=r.a||o.a,b=r.b||o.b,other=a===me_?b:a;
    soLoad(true).then(()=>{const p=SO.prof[other]||{},nm=p.name||p.username||"Someone";
      if(d.type==="INSERT"&&r.b===me_)pill({id:other,title:nm,text:"sent you a friend request",act:"friends"});
      else if(d.type==="UPDATE"&&r.status==="accepted"&&r.a===me_)pill({id:other,title:nm,text:"accepted your friend request",act:"chat"})})}
  else if(t==="shared"){const id=r.user_id;if(id&&id!==me_&&SO.fr.includes(id)){const c=SO.sh[id]||(SO.sh[id]={user_id:id});["np","np_at","pl","pl_at"].forEach(k=>{if(k in r)c[k]=r[k]});if(SO.on&&SO.v==="chat")mcSub();else if(SO.on&&SO.v==="home"&&!document.querySelector("#soc input:focus"))soRender(true)}}
  else if(t==="milo_sync"){if(r.user_id===me_&&S.cloud&&r.updated_at&&r.updated_at!==S.cloud.sync&&!S.cloudBusy&&Date.now()-(S.cloud.last||0)>2500){const was=S.cloud.sync;sbPull(false).then(()=>{if(S.cloud&&S.cloud.sync!==was)pill({title:"Synced",text:"Updated from another device",act:""})})}}}
