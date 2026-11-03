/* ----- scrobbling (ListenBrainz + Last.fm) and Listen Together (live, over the realtime socket) ----- */
const SC={get:()=>{try{return JSON.parse(localStorage.getItem("milo.scrobble")||"{}")}catch(e){return{}}},set:o=>{try{localStorage.setItem("milo.scrobble",JSON.stringify(o))}catch(e){}},q:{lb:[],fm:[]},np:""};
function md5(s){/* compact MD5 (needed to sign Last.fm calls) */
  const u=unescape(encodeURIComponent(s)),n=u.length,w=[];for(let i=0;i<n;i++)w[i>>2]|=u.charCodeAt(i)<<(i%4*8);w[n>>2]|=0x80<<(n%4*8);w[(((n+8)>>6)+1)*16-2]=n*8;
  const K=[],R=[7,12,17,22,5,9,14,20,4,11,16,23,6,10,15,21];for(let i=0;i<64;i++)K[i]=Math.floor(Math.abs(Math.sin(i+1))*4294967296)|0;
  let a=1732584193,b=-271733879,c=-1732584194,d=271733878;const rl=(x,k)=>x<<k|x>>>32-k;
  for(let o=0;o<w.length;o+=16){let A=a,B=b,C=c,D=d;for(let i=0;i<64;i++){let f,g;
    if(i<16){f=B&C|~B&D;g=i}else if(i<32){f=D&B|~D&C;g=(5*i+1)%16}else if(i<48){f=B^C^D;g=(3*i+5)%16}else{f=C^(B|~D);g=7*i%16}
    const t=D;D=C;C=B;B=B+rl(A+f+K[i]+(w[o+g]|0)|0,R[(i>>4)*4+i%4]);A=t}
    a=a+A|0;b=b+B|0;c=c+C|0;d=d+D|0}
  return[a,b,c,d].map(x=>{let h="";for(let i=0;i<4;i++)h+=("0"+(x>>>i*8&255).toString(16)).slice(-2);return h}).join("")}
async function scPost(url,headers,json,form){
  const D=window.miloDesktop,H=NV.P&&NV.P.CapacitorHttp,body=form?new URLSearchParams(form).toString():JSON.stringify(json);
  const h=Object.assign({"Content-Type":form?"application/x-www-form-urlencoded":"application/json"},headers||{});
  let st,tx;
  if(D&&D.http){const r=await D.http({url,method:"POST",headers:h,body});st=r.status;tx=r.text}
  else if(NV.cap&&H){const r=await H.request({url,method:"POST",headers:h,data:form||json,responseType:"text",connectTimeout:12000,readTimeout:20000});st=r.status;tx=typeof r.data==="string"?r.data:JSON.stringify(r.data)}
  else{const r=await fetch(url,{method:"POST",headers:h,body});st=r.status;tx=await r.text()}
  return{status:st,text:tx}}
const scArtist=t=>String(t.a||"").split(/,\s*|\s+&\s+|\s+feat\.?\s+/i)[0].trim();
const scOk=t=>t&&!t.radio&&!t.ph&&t.t&&scArtist(t)&&scArtist(t)!=="On this device";
function fmSig(p,secret){return md5(Object.keys(p).filter(k=>k!=="format"&&k!=="callback").sort().map(k=>k+p[k]).join("")+secret)}
async function fmCall(method,p){const c=SC.get().fm;if(!c||!c.key||!c.secret)throw new Error("Last.fm is not set up");
  const q=Object.assign({method,api_key:c.key},p);q.api_sig=fmSig(q,c.secret);q.format="json";
  const r=await scPost("https://ws.audioscrobbler.com/2.0/",{},null,q);let j={};try{j=JSON.parse(r.text)}catch(e){}
  if(j.error)throw new Error(j.message||"Last.fm error "+j.error);if(r.status>=400)throw new Error("Last.fm answered "+r.status);return j}
async function lbSend(type,t,ts){const c=SC.get().lb;if(!c||!c.token)return;
  const meta={track_name:t.t,artist_name:scArtist(t),additional_info:{media_player:"milo.",submission_client:"milo.",duration_ms:t.d?Math.round(t.d*1000):undefined}};if(t.al&&t.al!==t.src&&!/^(YouTube|Internet Archive)/i.test(t.al))meta.release_name=t.al;
  const r=await scPost("https://api.listenbrainz.org/1/submit-listens",{Authorization:"Token "+c.token},{listen_type:type,payload:[type==="playing_now"?{track_metadata:meta}:{listened_at:ts,track_metadata:meta}]});
  if(r.status===401)throw new Error("ListenBrainz token was refused");if(r.status>=400)throw new Error("ListenBrainz answered "+r.status)}
async function fmSend(type,t,ts){const c=SC.get().fm;if(!c||!c.sk)return;
  const p={artist:scArtist(t),track:t.t,sk:c.sk};if(t.d)p.duration=String(Math.round(t.d));if(type==="playing_now")await fmCall("track.updateNowPlaying",p);else{p.timestamp=String(ts);await fmCall("track.scrobble",p)}}
function scFlush(){["lb","fm"].forEach(async k=>{const q=SC.q[k];if(!q||!q.length||q.busy)return;q.busy=1;try{while(q.length){const x=q[0];await(k==="lb"?lbSend:fmSend)("single",x.t,x.ts);q.shift()}}catch(e){}q.busy=0})}
const _miListen=miListen;miListen=function(t){try{_miListen(t)}catch(e){}
  if(!scOk(t))return;const c=SC.get(),ts=Math.floor(Date.now()/1000)-30,it={t:{t:t.t,a:t.a,al:t.al,d:t.d,src:t.src},ts};
  if(c.lb&&c.lb.token)SC.q.lb.push(it);if(c.fm&&c.fm.sk)SC.q.fm.push(it);while(SC.q.lb.length>60)SC.q.lb.shift();while(SC.q.fm.length>60)SC.q.fm.shift();scFlush()};
setInterval(()=>{const t=T[S.cur];if(!t||!S.playing||!scOk(t)||SC.np===t.key)return;SC.np=t.key;const c=SC.get();
  if(c.lb&&c.lb.token)lbSend("playing_now",t).catch(()=>{});if(c.fm&&c.fm.sk)fmSend("playing_now",t).catch(()=>{})},2500);
/* ---- settings card in Profile ---- */
function scCard(){const c=SC.get(),lb=c.lb&&c.lb.token,fm=c.fm&&c.fm.sk;
  return `<button class="sxb" data-sc="lb"><b>ListenBrainz</b><small>${lb?"Connected · tap to change":"Send what you play to your ListenBrainz profile"}</small></button>
  <button class="sxb" data-sc="fm"><b>Last.fm</b><small>${fm?"Connected as "+esc(c.fm.user||"you")+" · tap to change":"Scrobble what you play to Last.fm"}</small></button>
  <button class="sxb" data-sc="lt"><b>Listen together</b><small>${LT.code?"In session "+esc(LT.code)+(LT.host?" · hosting":" · listening"):"Play the same song in sync with a friend (needs sign-in)"}</small></button>`}
const _sxExtra=sxExtra;sxExtra=function(){return _sxExtra()+scCard()};
document.addEventListener("click",e=>{const b=e.target.closest("[data-sc]");if(!b)return;const k=b.dataset.sc,c=SC.get();
  if(k==="lb")modal(`<h3>ListenBrainz</h3><p>Paste your user token from listenbrainz.org/settings. Plays are sent after 30 seconds of listening.</p><input id="scTok" placeholder="User token" value="${esc((c.lb&&c.lb.token)||"")}" autocomplete="off" style="width:100%;margin:8px 0"><button class="addb" data-sc="lbok">Save</button> <button class="ghost" data-sc="lbno">Disconnect</button> <button class="ghost" data-x>Cancel</button>`);
  else if(k==="lbok"){const v=($("scTok").value||"").trim();c.lb=v?{token:v}:null;SC.set(c);closeM();toast(v?"ListenBrainz connected":"ListenBrainz disconnected")}
  else if(k==="lbno"){c.lb=null;SC.set(c);closeM();toast("ListenBrainz disconnected")}
  else if(k==="fm")modal(`<h3>Last.fm</h3><p>Create a free API account at last.fm/api/account/create, then enter its key and secret with your Last.fm login. Your password is only used once to get a session key and is never stored.</p><input id="scK" placeholder="API key" value="${esc((c.fm&&c.fm.key)||"")}" style="width:100%;margin:4px 0"><input id="scS" placeholder="Shared secret" value="${esc((c.fm&&c.fm.secret)||"")}" style="width:100%;margin:4px 0"><input id="scU" placeholder="Username" value="${esc((c.fm&&c.fm.user)||"")}" style="width:100%;margin:4px 0"><input id="scP" type="password" placeholder="Password" style="width:100%;margin:4px 0"><button class="addb" data-sc="fmok">Connect</button> <button class="ghost" data-sc="fmno">Disconnect</button> <button class="ghost" data-x>Cancel</button>`);
  else if(k==="fmno"){c.fm=null;SC.set(c);closeM();toast("Last.fm disconnected")}
  else if(k==="fmok"){const key=$("scK").value.trim(),secret=$("scS").value.trim(),user=$("scU").value.trim(),pw=$("scP").value;if(!key||!secret||!user||!pw){toast("Fill in all four boxes");return}
    c.fm={key,secret};SC.set(c);fmCall("auth.getMobileSession",{username:user,password:pw}).then(j=>{const c2=SC.get();c2.fm={key,secret,user:j.session.name,sk:j.session.key};SC.set(c2);closeM();toast("Last.fm connected")}).catch(er=>{const c2=SC.get();c2.fm=null;SC.set(c2);toast(er.message||"Could not connect to Last.fm")})}
  else if(k==="lt")ltModal()});
/* ---- Listen together: the host's song, position and play/pause are broadcast; guests follow. Needs YouTube/streamed songs so everyone can play them ---- */
const LT={code:"",host:false,tm:0,last:"",at:0};
const ltName=c=>"milo-together-"+c.toLowerCase();
function ltSend(ev,p){if(!LT.code)return;rtSend({topic:rtTopic(ltName(LT.code)),event:"broadcast",payload:{type:"broadcast",event:ev,payload:p}})}
function ltState(){const t=T[S.cur];if(!t||!t.yt){return{none:1}}return{yt:t.yt,t:t.t,a:t.a,img:t.img||"",d:t.d||0,pos:S.pos,playing:!!S.playing,ts:Date.now()}}
function ltHostTick(){if(!LT.host||!LT.code)return;const s=ltState(),sig=s.yt+"|"+s.playing;
  if(sig!==LT.last||Date.now()-LT.at>4000){LT.last=sig;LT.at=Date.now();ltSend("st",s)}}
function ltJoin(code,host){if(!S.cloud||!soUid()){toast("Sign in to use Listen together");return}
  ltLeave(true);LT.code=code.toUpperCase();LT.host=host;LT.last="";LT.at=0;rtJoin(ltName(LT.code),{broadcast:{self:false},presence:{key:""}});
  LT.tm=setInterval(ltHostTick,1000);if(!host)setTimeout(()=>ltSend("hello",{}),1500);toast(host?"Hosting "+LT.code:"Joined "+LT.code)}
function ltLeave(q){if(LT.code){try{rtLeave(ltName(LT.code))}catch(e){}}clearInterval(LT.tm);const was=LT.code;LT.code="";LT.host=false;if(was&&!q)toast("Left the session")}
function ltModal(){if(LT.code){modal(`<h3>Listen together</h3><p>${LT.host?"You're hosting":"You're listening in"}. Code: <b>${esc(LT.code)}</b></p><button class="addb" data-sc="ltx">Leave session</button> <button class="ghost" data-x>Close</button>`);return}
  modal(`<h3>Listen together</h3><p>Host a session and share the code, or enter a friend's code. Everyone hears the host's song at the same spot. It works for YouTube and streamed songs, and everyone needs to be signed in.</p><button class="addb" data-sc="lth">Host a session</button><p style="margin:14px 0 4px">Or join one</p><input id="ltC" placeholder="Code" maxlength="6" style="text-transform:uppercase;width:100%;margin:4px 0"><button class="addb" data-sc="ltj">Join</button> <button class="ghost" data-x>Cancel</button>`)}
document.addEventListener("click",e=>{const b=e.target.closest("[data-sc]");if(!b)return;const k=b.dataset.sc;
  if(k==="lth"){const c=Array.from(crypto.getRandomValues(new Uint8Array(6)),x=>"ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[x%32]).join("");closeM();ltJoin(c,true);setTimeout(()=>modal(`<h3>Hosting</h3><p>Share this code: <b style="font-size:26px;letter-spacing:.12em">${c}</b></p><p>Play a YouTube song and friends who join will hear it with you.</p><button class="ghost" data-x>Done</button>`),200)}
  else if(k==="ltj"){const c=($("ltC").value||"").trim().toUpperCase();if(c.length<4){toast("Enter the code");return}closeM();ltJoin(c,false)}
  else if(k==="ltx"){closeM();ltLeave()}});
const _rtBc=rtBc;rtBc=function(topic,p){try{_rtBc(topic,p)}catch(e){}
  if(!LT.code||topic!==rtTopic(ltName(LT.code))||!p)return;
  if(p.event==="hello"&&LT.host){LT.last="";ltHostTick();return}
  if(p.event!=="st"||LT.host)return;const s=p.payload||{};if(s.none||!s.yt)return;
  const lag=s.playing?(Date.now()-s.ts)/1000:0,want=(s.pos||0)+Math.max(0,Math.min(lag,5)),cur=T[S.cur];
  if(!cur||cur.yt!==s.yt){const i=addStream({id:"yt"+s.yt,yt:s.yt,t:s.t,a:s.a,d:s.d,url:"",img:s.img});playList([i],false);if(!s.playing)setTimeout(()=>{if(S.playing)toggle()},800);setTimeout(()=>seekTo(want),1200);return}
  if(Math.abs(S.pos-want)>3)seekTo(want);if(!!S.playing!==!!s.playing)toggle()};
