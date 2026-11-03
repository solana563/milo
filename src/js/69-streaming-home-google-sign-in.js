/* ----- milo. as a streaming service: on-device music first, then YouTube Music shelves (recommended, trending, charts). Offline shows only your own music. ----- */
const YH={shelves:LS.get("milo.yhome",[]),at:+LS.get("milo.yhomeAt",0)||0,busy:0,err:""};
const yhOnline=()=>navigator.onLine!==false&&ymOk();
function yhShelf(n){/* one carousel shelf -> {t, items:[{k,id,t,sub,img,a}]} */
  const hd=ymFirst(n.header||{},"title"),t=ymRuns(hd);if(!t)return null;const items=[];const cs=n.contents||[];
  for(const c of cs){
    if(c.musicTwoRowItemRenderer){const x=ymCard(c.musicTwoRowItemRenderer,true);if(x)items.push(x)}
    else if(c.musicResponsiveListItemRenderer){const r=c.musicResponsiveListItemRenderer,sg=ymSong(r);if(sg)items.push({k:"song",id:sg.id,t:sg.t,a:sg.a,img:sg.img||ymThumbOf(r),d:sg.d||0});else{const x=ymCard(r,false);if(x)items.push(x)}}}
  return items.length?{t,items:items.slice(0,14)}:null}
function yhParse(j){const out=[];for(const k of["musicCarouselShelfRenderer","musicImmersiveCarouselShelfRenderer"])for(const n of ymAll(j,k)){const s=yhShelf(n);if(s)out.push(s)}return out}
function yhCont(j){const a=ymAll(j,"nextContinuationData")[0];return a&&a.continuation||""}
async function yhBrowse(id,extra){try{let j=await ymPost("browse",Object.assign({browseId:id},extra||{}),YMC.REMIX),sh=yhParse(j);
  for(let i=0;i<2;i++){const c=yhCont(j);if(!c)break;try{j=await ymPost("browse",{continuation:c},YMC.REMIX);sh=sh.concat(yhParse(j))}catch(e){break}}return sh}catch(e){return[]}}
async function yhLoad(force){if(YH.busy||!yhOnline())return;if(!force&&YH.shelves.length&&Date.now()-YH.at<20*60*1000)return;
  YH.busy=1;try{const[h,c]=await Promise.all([yhBrowse("FEmusic_home"),yhBrowse("FEmusic_charts")]);const seen=new Set(),all=[];
    h.concat(c).forEach(s=>{if(!seen.has(s.t)){seen.add(s.t);all.push(s)}});
    if(all.length){YH.shelves=all.slice(0,10);YH.at=Date.now();YH.err="";LS.set("milo.yhome",YH.shelves);LS.set("milo.yhomeAt",YH.at)}else YH.err="empty"}
  catch(e){YH.err=(e&&e.message)||"failed"}YH.busy=0;if(S.tab==="Home"&&!S.nav)try{render()}catch(e){}}
function yhCard(c){return c.k==="song"?ymSongCard(c):c.k==="artist"?ymPoster(c):ymSquare(c)}
function yhHtml(){if(!yhOnline())return"";if(!YH.shelves.length)return YH.busy?`<div class="row"><h2>Discover</h2></div><div class="ysklist"><i class="ysk"></i><i class="ysk"></i><i class="ysk"></i></div>`:"";
  return YH.shelves.map(s=>{const ar=s.items.every(x=>x.k==="artist");return`<div class="row"><h2>${ymEsc(s.t)}</h2></div><div class="pls${ar?" ypls":""}">${s.items.map(yhCard).join("")}</div>`}).join("")}
{const _h=home;home=function(){let h=_h.apply(this,arguments);try{const x=yhHtml();if(x)h=h.replace('<div class="row"><h2>Recently played</h2>',x+'<div class="row"><h2>Recently played</h2>');if(yhOnline())setTimeout(()=>yhLoad(false),50);
  /* bottom of Home: the newest songs on this device, up to 50 */
  const ra=[];for(let i=T.length-1;i>=0&&ra.length<50;i--){const t=T[i];if(t&&!t.radio&&!t.stream&&!t.hid&&!t.gone&&!t.ph)ra.push(i)}
  if(ra.length){S.lists.ra=ra;const k=h.lastIndexOf("</div>");if(k>0)h=h.slice(0,k)+`<div class="row"><h2>Recently added</h2><button data-go="Library" data-lib="Songs">See all</button></div><div class="list">${ra.map(i=>li(i,"ra")).join("")}</div>`+h.slice(k)}}catch(e){}return h}}
window.addEventListener("online",()=>{try{yhLoad(true)}catch(e){}});window.addEventListener("offline",()=>{try{render()}catch(e){}});
/* ----- Google sign-in (Google Identity Services -> Supabase id_token). Needs the Google provider switched on in Supabase. ----- */
const GCLIENT="607031598574-svt5i48ppk9udu7mujo76k2j89j2qk3u.apps.googleusercontent.com";
const gOk=()=>/^https?:$/.test(location.protocol)&&!NV.cap&&!window.miloDesktop&&navigator.onLine!==false;
function gLoad(){return new Promise((ok,no)=>{if(window.google&&google.accounts&&google.accounts.id)return ok();const s=document.createElement("script");s.src="https://accounts.google.com/gsi/client";s.async=true;s.onload=()=>ok();s.onerror=()=>no(new Error("Google sign-in could not load"));document.head.appendChild(s)})}
async function gCred(r){if(!r||!r.credential)return;GATE.busy=1;gtErr("");
  try{const j=await sbReq("/auth/v1/token?grant_type=id_token",{m:"POST",b:{provider:"google",id_token:r.credential},anon:1});if(!j||!j.access_token)throw new Error("Google sign-in was not accepted");await gtSession(j,"")}
  catch(e){GATE.busy=0;GATE.err=/provider|disabled|not enabled/i.test(String(e.message))?"Google sign-in isn't switched on for this app yet.":sbMsg(e);gateRender()}}
async function gMount(){const h=document.getElementById("gt-g");if(!h||h.dataset.on)return;h.dataset.on="1";
  try{await gLoad();google.accounts.id.initialize({client_id:GCLIENT,callback:gCred,ux_mode:"popup",auto_select:false});google.accounts.id.renderButton(h,{theme:"filled_black",size:"large",shape:"pill",text:"continue_with",width:Math.min(300,h.clientWidth||300)})}
  catch(e){h.innerHTML=""}}
{const _g=gateRender;gateRender=function(){const r=_g.apply(this,arguments);try{const st=GATE.step;if(gOk()&&(st==="welcome"||st==="up"||st==="in")){const w=document.querySelector(".gw-in")||document.querySelector(".gt-form");
  if(w&&!w.querySelector("#gt-g")){const d=document.createElement("div");d.id="gt-g";d.style.cssText="display:flex;justify-content:center;margin:12px 0;min-height:44px";const a=w.querySelector(".gt-fine")||w.lastElementChild;w.insertBefore(d,a);gMount()}}}catch(e){}return r}}
