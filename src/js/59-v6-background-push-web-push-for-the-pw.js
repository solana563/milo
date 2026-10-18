/* ===== v6: background push (Web Push for the PWA, FCM for the Android app) ===== */
const VAPID_PUB="BGyladh3H-ckDlHBTVIz6vB8Z9P7rpYMi7B2qE6J78RtqooQe-4b_0u404_L75Hv250wF5UAoiT6ym_Z0cyqdNE";
const PU={tok:"",kind:""};
function pu64(s){const p="=".repeat((4-s.length%4)%4),b=atob((s+p).replace(/-/g,"+").replace(/_/g,"/")),o=new Uint8Array(b.length);for(let i=0;i<b.length;i++)o[i]=b.charCodeAt(i);return o}
function puOk(){return NV.cap?!!(NV.P&&NV.P.PushNotifications):("serviceWorker" in navigator&&"PushManager" in window&&"Notification" in window&&window.isSecureContext)}
async function puSave(kind,ep,keys){if(!soUid())return false;try{await sbReq("/rest/v1/rpc/push_save",{m:"POST",b:{k:kind,e:ep,x:keys||null}});PU.tok=ep;PU.kind=kind;LS.set("milo.pushAt",Date.now());return true}catch(e){return false}}
async function pushDrop(){try{if(PU.tok||LS.get("milo.pushEp",""))await sbReq("/rest/v1/rpc/push_drop",{m:"POST",b:{e:PU.tok||LS.get("milo.pushEp","")}})}catch(e){}LS.set("milo.pushEp","");PU.tok=""}
async function pushWeb(ask){
  if(!puOk()||!soUid())return false;
  if(Notification.permission==="default"&&ask)try{await Notification.requestPermission()}catch(e){}
  if(Notification.permission!=="granted")return false;
  try{const reg=await navigator.serviceWorker.ready;let sub=await reg.pushManager.getSubscription();
    if(!sub)sub=await reg.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:pu64(VAPID_PUB)});
    const j=sub.toJSON();LS.set("milo.pushEp",sub.endpoint);return await puSave("web",sub.endpoint,j.keys)}catch(e){return false}}
async function pushNative(ask){
  const PN=NV.P&&NV.P.PushNotifications;if(!PN||!soUid())return false;
  try{let st=await PN.checkPermissions();if(st.receive==="prompt"&&ask)st=await PN.requestPermissions();if(st.receive!=="granted")return false;
    try{await PN.createChannel({id:"milo_messages",name:"Messages and friend requests",importance:4,visibility:0,vibration:true})}catch(e){}
    return await new Promise(res=>{let done=0;const fin=v=>{if(!done){done=1;res(v)}};
      PN.addListener("registration",async t=>{LS.set("milo.pushEp",t.value);fin(await puSave("fcm",t.value))});
      PN.addListener("registrationError",()=>fin(false));
      PN.register();setTimeout(()=>fin(false),15000)})}catch(e){return false}}
async function pushEnable(ask){return NV.cap?pushNative(ask):pushWeb(ask)}
function pushOpen(act,chat){if(act==="chat"&&chat&&SO.fr&&(SO.fr.includes(chat)||1)){mcOpen(chat);soLoad(true)}else{SO.tab="friends";if(!SO.on){SO.on=1;soEl().hidden=false}SO.v="home";soRender();soLoad(true)}}
(function(){
  if(navigator.serviceWorker)navigator.serviceWorker.addEventListener("message",e=>{const d=e.data||{};if(d.type==="milo-open")pushOpen(d.act,d.chat);else if(d.type==="milo-resub")pushWeb(false)});
  if(NV.cap&&NV.P&&NV.P.PushNotifications)try{NV.P.PushNotifications.addListener("pushNotificationActionPerformed",a=>{const d=(a.notification&&a.notification.data)||{};pushOpen(d.act,d.chat)})}catch(e){}
  const q=new URLSearchParams(location.search),ch=q.get("chat"),fr=q.get("friends");
  if(ch||fr)setTimeout(()=>{if(soUid()){pushOpen(ch?"chat":"friends",ch);try{history.replaceState(null,"",location.pathname)}catch(e){}}},1800);
  const so=signOutAll;signOutAll=function(){try{pushDrop()}catch(e){}return so.apply(this,arguments)};
  /* keep the subscription fresh: once a day while signed in and allowed */
  setInterval(()=>{if(soUid()&&Date.now()-LS.get("milo.pushAt",0)>864e5&&(NV.cap||(window.Notification&&Notification.permission==="granted")))pushEnable(false)},60000);
  setTimeout(()=>{if(soUid()&&Date.now()-LS.get("milo.pushAt",0)>864e5&&(NV.cap||(window.Notification&&Notification.permission==="granted")))pushEnable(false)},6000);
  /* the permission card: turn on = ask + subscribe */
  document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest('[data-sc="notif:on"]');if(!b)return;pushEnable(true).then(ok=>{if(!ok&&puOk()===false)toast("Notifications need the installed app or HTTPS");else if(ok)toast("Notifications on")})},true);
})();

