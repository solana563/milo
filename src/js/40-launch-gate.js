/* ----- launch gate ----- */
const GATE={step:"up",err:"",busy:0};
function gateEl(){let g=$("gate");if(!g){g=document.createElement("div");g.id="gate";$("app").appendChild(g)}return g}
function gateOpen(step){GATE.step=step==="in"?"in":step==="terms"?"terms":step==="claim"?"claim":"up";GATE.err="";gateRender()}
function gateClose(){const g=$("gate");if(g)g.remove();document.documentElement.classList.remove("gated")}
function gateNeeded(){if(!S.cloud)return"up";if(!S.prof||!S.prof.username)return"claim";if(!termsOk())return"terms";return""}
function gateRender(){const g=gateEl();document.documentElement.classList.add("gated");const st=GATE.step;let h="";
  const logo=$("logo")?$("logo").src:"";
  if(st==="up"||st==="in"){const up=st==="up";
    h=`<div class="gt-in"><img class="gt-logo" alt="milo." src="${logo}"><h1>${up?"Create your account":"Welcome back"}</h1><p class="gt-sub">${up?"Your playlists, friends and settings, on every device.":"Sign in to pick up where you left off."}</p>
    <div class="gt-seg"><button class="${up?"on":""}" data-gt="tab:up">Sign up</button><button class="${up?"":"on"}" data-gt="tab:in">Sign in</button></div>
    ${up?`<input id="gt-u" placeholder="Username (letters, numbers, _)" maxlength="20" autocomplete="username" autocapitalize="none" spellcheck="false">`:""}
    <input id="gt-e" type="email" placeholder="Email" autocomplete="email" autocapitalize="none">
    <input id="gt-p" type="password" placeholder="Password${up?" (6+ characters)":""}" autocomplete="${up?"new-password":"current-password"}">
    <p class="gt-err" id="gt-err">${esc(GATE.err)}</p>
    <button class="addb gt-go" data-gt="go" ${GATE.busy?"disabled":""}>${GATE.busy?"Please wait…":up?"Create account":"Sign in"}</button>
    ${navigator.onLine===false?`<button class="ghost gt-skip" data-gt="skip">Continue without an account</button>`:""}
    <p class="gt-fine">${up?"Next you will see your profile card and the terms.":"New here? Choose Sign up."}</p></div>`}
  else if(st==="claim"){h=`<div class="gt-in"><img class="gt-logo" alt="milo." src="${logo}"><h1>Pick a username</h1><p class="gt-sub">Friends find you by this name. 3 to 20 letters, numbers or _.</p>
    <input id="gt-u" placeholder="Username" maxlength="20" autocapitalize="none" spellcheck="false"><p class="gt-err" id="gt-err">${esc(GATE.err)}</p>
    <button class="addb gt-go" data-gt="claim" ${GATE.busy?"disabled":""}>Continue</button><button class="ghost gt-skip" data-gt="out">Sign out</button></div>`}
  else{const u=me(),p=S.prof||{},un=p.username||u.un||"",hrs=Object.values(S.stat||{}).reduce((a,b)=>a+b,0)/3600;
    h=`<div class="gt-in gt-wide"><h1>Your profile</h1><p class="gt-sub">This is how friends see you.</p>
    <div class="pcard"><i class="pbg" style="background:${avCss(u.av)}"></i><i class="pfade"></i><div class="ptop">${avEl(u,"big")}<button class="dm" data-gt="edit">Edit profile</button></div>
    <h2>${esc(u.name)}</h2><div class="hd">@${esc(un)} · Signed in</div><div class="bio">Your music, your way.</div>
    <div class="ptr"><div><b>${S.likes.size}</b><span>Liked</span></div><div><b>${S.pls.length}</b><span>Playlists</span></div><div><b>${hrs>=10?Math.round(hrs):hrs.toFixed(1)}</b><span>Hours</span></div></div></div>
    <h3 class="gt-h3">Terms and how your information is used</h3><div class="gt-terms" tabindex="0">${TERMS_HTML}</div>
    <label class="gt-chk"><input type="checkbox" id="gt-ok"><span>I have read and agree to the terms, including how my synced playlists, shared listening and messages are used.</span></label>
    <p class="gt-err" id="gt-err">${esc(GATE.err)}</p><button class="addb gt-go" id="gt-agree" data-gt="agree" disabled>Agree and continue</button><button class="ghost gt-skip" data-gt="out">Sign out</button></div>`}
  g.innerHTML=h;const f=g.querySelector("input");if(f&&st!=="terms"&&matchMedia("(hover:hover)").matches)setTimeout(()=>{try{f.focus()}catch(e){}},50)}
const gtErr=m=>{GATE.err=m;const e=$("gt-err");if(e)e.textContent=m};
async function gtAuth(){const up=GATE.step==="up",e=($("gt-e").value||"").trim(),p=$("gt-p").value||"",un=up?($("gt-u").value||"").trim().toLowerCase().replace(/^@/,""):"";
  if(up&&!UN_RE.test(un))return gtErr("Username: 3 to 20 letters, numbers or _.");
  if(!/^\S+@\S+\.\S+$/.test(e))return gtErr("Enter a valid email address.");if(p.length<6)return gtErr("Use a password with at least 6 characters.");
  GATE.busy=1;gtErr("");gateRender();$("gt-e").value=e;if(up)$("gt-u").value=un;
  try{
    if(up){const fr=await sbReq("/rest/v1/rpc/username_free",{m:"POST",b:{u:un},anon:1});if(fr===false)throw new Error("USERNAME_TAKEN")}
    const j=await sbReq(up?"/auth/v1/signup":"/auth/v1/token?grant_type=password",{m:"POST",b:up?{email:e,password:p,data:{username:un,name:un}}:{email:e,password:p},anon:1});
    if(!j||!j.access_token){GATE.busy=0;GATE.step="in";GATE.err="Check your inbox to confirm your email, then sign in.";gateRender();return}
    await gtSession(j,up?un:"");
  }catch(er){GATE.busy=0;GATE.err=/USERNAME_TAKEN|duplicate|unique|username/i.test(String(er.message))&&up?"That username is taken. Try another.":sbMsg(er);gateRender()}}
async function gtSession(j,wantUn){S.cloud=null;sbStore(j);S.cloud.sync="";LS.set("milo.cloud",S.cloud);
  let pr=await profLoad();
  if(!pr&&wantUn){try{await sbReq("/rest/v1/profiles",{m:"POST",h:{Prefer:"return=minimal"},b:{id:S.cloud.id,username:wantUn,name:wantUn}});pr=await profLoad()}catch(e){}}
  GATE.busy=0;
  if(!pr){GATE.step="claim";gateRender();return}
  cloudAdopt(pr);await sbPull(false);afterSwitch();GATE.step=termsOk()?"":"terms";if(!GATE.step){gateClose();soStart();return}gateRender()}
async function profLoad(){try{const r=await sb("/rest/v1/profiles?select=*&id=eq."+encodeURIComponent(S.cloud.id));S.prof=r&&r[0]||null;LS.set("milo.prof",S.prof);return S.prof}catch(e){return null}}
function cloudAdopt(pr){S.cloud.un=pr.username;LS.set("milo.cloud",S.cloud);
  let a=S.accts.find(x=>x.cid===S.cloud.id);
  if(!a){const src=LS.get(dkey(),{});a={id:"c"+S.cloud.id.replace(/-/g,"").slice(0,10),cid:S.cloud.id,name:pr.name||pr.username,un:pr.username,av:pr.av&&pr.av.t?pr.av:JSON.parse(JSON.stringify(S.guest.av)),salt:"",hash:""};LS.set("milo.v2."+a.id,src);S.accts.push(a)}
  else{a.un=pr.username;if(pr.name)a.name=pr.name;if(pr.av&&pr.av.t)a.av=pr.av}
  LS.set("milo.accts",S.accts);LS.set("milo.cur",a.id);S.acct=a}
async function gtClaim(){const un=($("gt-u").value||"").trim().toLowerCase().replace(/^@/,"");if(!UN_RE.test(un))return gtErr("Username: 3 to 20 letters, numbers or _.");
  GATE.busy=1;gateRender();try{const fr=await sbReq("/rest/v1/rpc/username_free",{m:"POST",b:{u:un},anon:1});if(fr===false)throw new Error("USERNAME_TAKEN");
    await sb("/rest/v1/profiles",{m:"POST",h:{Prefer:"return=minimal"},b:{id:S.cloud.id,username:un,name:un}});const pr=await profLoad();GATE.busy=0;if(!pr)throw new Error("Couldn't create your profile.");cloudAdopt(pr);await sbPull(false);afterSwitch();GATE.step="terms";gateRender()}
  catch(er){GATE.busy=0;GATE.err=/USERNAME_TAKEN|duplicate|unique/i.test(String(er.message))?"That username is taken. Try another.":sbMsg(er);gateRender()}}
async function gtAgree(){const b=$("gt-agree");if(b)b.disabled=true;const at=new Date().toISOString();
  try{await sb("/rest/v1/profiles?id=eq."+encodeURIComponent(S.cloud.id),{m:"PATCH",h:{Prefer:"return=minimal"},b:{terms_v:TERMS_V,terms_at:at}})}catch(e){gtErr(sbMsg(e));if(b)b.disabled=false;return}
  S.termsAt={v:TERMS_V,at,uid:S.cloud.id};LS.set("milo.terms",S.termsAt);if(S.prof)S.prof.terms_v=TERMS_V;gateClose();toast("Welcome to milo., "+me().name);soStart();sbPush()}
function signOutAll(){soStop();const was=S.cloud;S.cloud=null;LS.set("milo.cloud",null);S.prof=null;LS.set("milo.prof",null);S.acct=null;LS.set("milo.cur","");S.termsAt=null;LS.set("milo.terms",null);afterSwitch();if(was)GATE.step="in";else GATE.step="up";gateRender()}
document.addEventListener("click",e=>{const b=e.target.closest("[data-gt]");if(!b)return;const k=b.dataset.gt;
  if(k.startsWith("tab:")){GATE.step=k.slice(4);GATE.err="";gateRender()}else if(k==="go")gtAuth();else if(k==="claim")gtClaim();else if(k==="agree")gtAgree();
  else if(k==="edit")openEdit();else if(k==="out")signOutAll();else if(k==="skip"){gateClose()}});
document.addEventListener("change",e=>{if(e.target.id==="gt-ok"){const b=$("gt-agree");if(b)b.disabled=!e.target.checked}});
document.addEventListener("keydown",e=>{if(e.key==="Enter"&&e.target.closest&&e.target.closest("#gate")&&e.target.tagName==="INPUT"&&e.target.type!=="checkbox"){const s=GATE.step;if(s==="up"||s==="in")gtAuth();else if(s==="claim")gtClaim()}});
