/* ---------- accounts: guest + signed-up users (stored on this device) ---------- */
const AVC=["249,156,0","84,120,140","150,100,130","110,140,110","170,110,90","120,110,160","200,90,110","60,60,70"];
const AVE=["🎧","🎵","🎸","🎹","🥁","🎤","🌙","☀️","🌿","🔥","🌊","⭐"];
const me=()=>S.acct||S.guest;
const avCss=a=>a.t==="img"?`center/cover url(${a.u})`:`linear-gradient(145deg,rgba(${a.c==="201,151,90"?"249,156,0":a.c},1),rgba(${a.c==="201,151,90"?"249,156,0":a.c},.5))`;
function avEl(u,cls="",tag="span",attr=""){const a=u.av;return `<${tag} class="avatar ${cls}" ${attr} style="background:${avCss(a)}">${a.t==="img"?"":a.t==="emoji"?a.e:esc(((u.name||"?").trim()[0]||"?").toUpperCase())}</${tag}>`}
const acctCard=()=>{const u=me(),g=!S.acct;
  const hrs=Object.values(S.stat||{}).reduce((a,b)=>a+b,0)/3600,hs=hrs>=10?Math.round(hrs):hrs.toFixed(1);
  const tg=artists().sort((a,b)=>b.idx.length-a.idx.length).slice(0,5).map(o=>`<button class="tg" data-nav="${nav1("artist",o.id)}">@${esc(o.name.toLowerCase().replace(/[^a-z0-9]+/g,""))||"artist"}</button>`).join("");
  const hd=u.un||u.name.toLowerCase().replace(/[^a-z0-9]+/g,"")||"guest";
  return `<div class="pcard"><i class="pbg" style="background:${avCss(u.av)}"></i><i class="pfade"></i>
  <div class="ptop">${avEl(u,"big")}<button class="dm" data-edit>Edit profile</button></div>
  <h2>${esc(u.name)}</h2><div class="hd">@${esc(hd)} · ${g?"Guest, saved on this device":"Signed in"}</div>
  <div class="bio">${vis().length?`Listening to ${vis().length} song${vis().length===1?"":"s"} on milo.`:"Your music, your way."}</div>
  <div class="ptr"><div><b>${S.likes.size}</b><span>Liked</span></div><div><b>${S.pls.length}</b><span>Playlists</span></div><div><b>${hs}</b><span>Hours</span></div></div>
  ${tg?`<div class="tgs">${tg}</div>`:""}
  ${g?`<div class="pacts"><button class="addb" data-su>Sign up</button><button class="ghost" data-si>Sign in</button></div>`:`<div class="pacts"><button class="ghost" data-so>Sign out</button><button class="ghost" data-del>Delete account</button></div>`}</div>`+
  (g?`<p class="empty" style="padding-top:0">Sign up to keep your playlists, likes and sound settings under your own name. Everything you have as a guest carries over.</p>`:"")};
const hex=b=>[...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,"0")).join("");
async function hashPw(pw,salt,alg){alg=alg||(window.crypto&&crypto.subtle?"p":"f");
  if(alg==="p"){try{const en=new TextEncoder(),k=await crypto.subtle.importKey("raw",en.encode(pw),"PBKDF2",false,["deriveBits"]);
    return "p:"+hex(await crypto.subtle.deriveBits({name:"PBKDF2",hash:"SHA-256",salt:en.encode(salt),iterations:100000},k,256))}catch(e){return null}}
  let h=2166136261;const s=salt+pw;for(let r=0;r<2000;r++)for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)>>>0}return "f:"+h.toString(16)}
const rnd=()=>{const a=new Uint8Array(12);try{crypto.getRandomValues(a)}catch(e){for(let i=0;i<12;i++)a[i]=Math.random()*256}return hex(a.buffer)};
const merr=m=>{const e=$("merr");if(e)e.textContent=m};
function afterSwitch(){loadData();setStyle();S.rfav.forEach(addStation);S.sfav.forEach(addStream);S.nav=null;theme();applyEQ();ambApply();updCtl();nav();render()}
function signUpM(){modal(`<h3>Create account</h3><input id="su-n" placeholder="Username" maxlength="24" autocomplete="username"><input id="su-p" type="password" placeholder="Password (6+ characters)" autocomplete="new-password"><input id="su-c" type="password" placeholder="Confirm password" autocomplete="new-password">
  <p class="err" id="merr"></p><button class="addb" data-a="su:">Sign up</button> <button class="ghost" data-a="gsi:">I have an account</button>
  <p style="margin-top:14px;font-size:13px">Accounts are stored on this device only, so there's no password recovery and no sync between devices.</p>`,1);const i=$("su-n");if(i)i.focus()}
function signInM(){modal(`<h3>Sign in</h3><input id="si-n" placeholder="Username" maxlength="24" autocomplete="username"><input id="si-p" type="password" placeholder="Password" autocomplete="current-password">
  <p class="err" id="merr"></p><button class="addb" data-a="si:">Sign in</button> <button class="ghost" data-a="gsu:">Create account</button>`);const i=$("si-n");if(i)i.focus()}
async function doSignUp(){const n=$("su-n").value.trim(),p=$("su-p").value,c=$("su-c").value;
  if(n.length<2||n.length>24)return merr("Pick a username with 2 to 24 characters.");
  if(S.accts.some(a=>a.name.toLowerCase()===n.toLowerCase()))return merr("That username is already taken on this device.");
  if(p.length<6)return merr("Use a password with at least 6 characters.");if(p!==c)return merr("The passwords don't match.");
  const salt=rnd(),hash=await hashPw(p,salt);if(!hash)return merr("This browser can't secure passwords here.");
  const a={id:"u"+Date.now().toString(36),name:n,salt,hash,av:JSON.parse(JSON.stringify(S.guest.av))};
  LS.set("milo.v2."+a.id,LS.get("milo.v2",{}));S.accts.push(a);LS.set("milo.accts",S.accts);LS.set("milo.cur",a.id);S.acct=a;closeM();toast("Welcome, "+n);afterSwitch()}
async function doSignIn(){const n=$("si-n").value.trim(),p=$("si-p").value,a=S.accts.find(x=>x.name.toLowerCase()===n.toLowerCase());
  if(!a)return merr("No account with that username on this device.");
  const h=await hashPw(p,a.salt,a.hash[0]);if(!h||h!==a.hash)return merr("Wrong username or password.");
  LS.set("milo.cur",a.id);S.acct=a;closeM();toast("Signed in as "+a.name);afterSwitch()}
function openEdit(){const u=me();S.ed={name:u.name,av:JSON.parse(JSON.stringify(u.av))};editM()}
function editM(){const u=me(),e=S.ed,a=e.av;
  modal(`<h3>Edit profile</h3><div style="display:grid;place-items:center;margin-bottom:12px">${avEl({name:e.name||u.name,av:a},"xl")}</div>
  <input id="mi" value="${esc(e.name)}" maxlength="24" placeholder="Username" autocomplete="off">
  <b style="font-weight:500">Colour</b><div class="swr">${AVC.map(c=>`<button class="sw ${a.t!=="img"&&a.c===c?"on":""}" style="background:rgb(${c})" data-a="avc:${c}" aria-label="Colour"></button>`).join("")}</div>
  <b style="font-weight:500">Icon</b><div class="swr"><button class="em ${a.t==="color"?"on":""}" data-a="avi:">Aa</button>${AVE.map(x=>`<button class="em ${a.t==="emoji"&&a.e===x?"on":""}" data-a="ave:${x}">${x}</button>`).join("")}</div>
  <div class="acts"><label class="ghost">Upload photo<input type="file" accept="image/*" data-avpick></label>${a.t==="img"?'<button class="ghost" data-a="avi:">Remove photo</button>':""}</div>
  <p class="err" id="merr"></p><button class="addb" data-a="edsave:">Save</button>`)}
function edSave(){const n=$("mi").value.trim();
  if(n.length<2||n.length>24)return merr("Pick a username with 2 to 24 characters.");
  if(S.acct&&S.accts.some(a=>a!==S.acct&&a.name.toLowerCase()===n.toLowerCase()))return merr("That username is already taken on this device.");
  const u=me();u.name=n;u.av=S.ed.av;if(S.acct)LS.set("milo.accts",S.accts);else LS.set("milo.guest",S.guest);closeM();toast("Profile updated");save();render()}
$("mdl").onchange=e=>{if(e.target.dataset.avpick==null)return;const f=e.target.files[0];if(!f)return;S.ed.name=$("mi").value;
  const im=new Image(),u=URL.createObjectURL(f);
  im.onload=()=>{try{const c=document.createElement("canvas");c.width=c.height=192;const x=c.getContext("2d"),s=Math.min(im.width,im.height);x.drawImage(im,(im.width-s)/2,(im.height-s)/2,s,s,0,0,192,192);S.ed.av={t:"img",u:c.toDataURL("image/jpeg",.85)};editM()}catch(er){toast("Couldn't use that photo")}URL.revokeObjectURL(u)};
  im.onerror=()=>toast("Couldn't read that image");im.src=u};

