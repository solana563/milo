/* ---------- accounts on launch, profile + terms, friends, song DNA sharing, disappearing chat ---------- */
const TERMS_V=1,SO_CAP=5242880,SO_DAY=86400000;
const TERMS_HTML=`<h4>1. Who we are</h4><p>milo. is a music player. These terms are between you and the milo. developer. Using milo. means you accept them.</p>
<h4>2. What we store, and why</h4><p><b>Your account:</b> your email, username, display name and avatar. This identifies you to friends and lets you sign in on any device.</p>
<p><b>Synced data:</b> your playlists, liked songs, listening history, listening statistics and sound settings are saved to your account so they follow you to every device you sign in on. Songs you upload from your device are <b>never</b> uploaded. They stay on that device.</p>
<p><b>Shared with friends:</b> if you turn sharing on (it is on by default and you can turn it off at any time in Profile), your accepted friends can see your playlists and what you are listening to. They receive only the "song DNA": title, artist, album, length, artwork link and the streaming source. They never receive your files. Songs that only exist on your device show as a title with no way to play them.</p>
<p><b>Messages:</b> chats are private between you and one friend. Each chat keeps at most 5 MB. When it is full the oldest messages are deleted first, and every message is deleted after 24 hours. Messages are not backed up.</p>
<h4>3. Services milo. talks to</h4><p>To work, milo. contacts third-party services, such as YouTube, iTunes previews, lyrics and radio directories, and your cloud database provider. Those services see your device's network address and the song or station you ask for, under their own privacy policies.</p>
<h4>4. What we do not do</h4><p>We do not sell your data, show ads, or read your private chats. We do not track you across other apps.</p>
<h4>5. Your choices</h4><p>You can change your display name and avatar, turn sharing off, remove a friend (which deletes your chat with them), sign out, or delete your account in Profile. Deleting your account removes your profile, synced data, friendships and messages from our database.</p>
<h4>6. Your conduct</h4><p>Be kind. Do not send unlawful, abusive or infringing content, and do not impersonate others. We may suspend accounts that do. Only play and share music you have the right to use. Songs streamed from other services are subject to those services' own terms.</p>
<h4>7. No warranty</h4><p>milo. is provided as is. Streaming sources can change or disappear, and sync can fail. Keep a backup (Profile, Back up) of anything you cannot lose.</p>
<h4>8. Changes</h4><p>If these terms change in a way that matters, you will be asked to approve them again before you continue. Version ${TERMS_V}.</p>`;
const UN_RE=/^[a-z0-9_]{3,20}$/;
const SO={on:0,v:"home",fid:null,pid:null,fr:[],rq:[],out:[],prof:{},sh:{},unread:{},msgs:[],timer:0,found:null,err:"",busy:0};
S.termsAt=LS.get("milo.terms",null);S.prof=LS.get("milo.prof",null);if(S.prof&&(!S.cloud||S.prof.id!==S.cloud.id))S.prof=null;let soTimer2=0;
const termsOk=()=>!!(S.termsAt&&S.termsAt.v>=TERMS_V&&S.cloud&&S.termsAt.uid===S.cloud.id);
async function sb(path,o){if(!(await sbFresh())&&S.cloud)throw new Error("Your session expired. Sign in again.");return sbReq(path,o)}
const isoAgo=ms=>new Date(Date.now()-ms).toISOString();
const dnaImg=u=>/^https:\/\//.test(u||"")?u:"";
function dnaOf(t){if(!t||t.radio)return null;
  if(t.stream&&t.raw&&!t.ph){const r=t.raw,o={id:r.id,t:r.t,a:r.a,src:r.src||t.al||"",g:r.g||"",d:Math.round(r.d||t.d||0)};if(r.url&&/^https:\/\//.test(r.url))o.url=r.url;if(dnaImg(r.img))o.img=r.img;if(r.yt)o.yt=r.yt;if(r.iaid)o.iaid=r.iaid;if(r.lic)o.lic=r.lic;if(r.page&&/^https:\/\//.test(r.page))o.page=r.page;return o}
  return{t:t.t,a:t.a,local:1}}
function dnaOfKey(k){const t=T.find(x=>x.key===k);if(t)return dnaOf(t);const s=(S.sfav||[]).find(x=>"stream:"+x.id===k);if(s)return{id:s.id,t:s.t,a:s.a,src:s.src||"",g:s.g||"",d:Math.round(s.d||0),url:/^https:\/\//.test(s.url||"")?s.url:undefined,img:dnaImg(s.img)||undefined,yt:s.yt,iaid:s.iaid,lic:s.lic};return null}
function dnaPlay(d){if(!d||d.local||!d.id){toast("That song only exists on their device");return}const i=addStream(Object.assign({},d));play(i,[i])}
function dnaList(a,shuf){const ix=(a||[]).filter(d=>d&&!d.local&&d.id).map(d=>addStream(Object.assign({},d)));if(!ix.length){toast("Nothing here can be streamed");return}playList(ix,!!shuf)}
const dnaLink=d=>location.origin+location.pathname+"#dna="+btoa(unescape(encodeURIComponent(JSON.stringify(d))));
