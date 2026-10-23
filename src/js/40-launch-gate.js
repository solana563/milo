/* ----- launch gate ----- */
const GATE={step:"up",err:"",busy:0};
function gateEl(){let g=$("gate");if(!g){g=document.createElement("div");g.id="gate";$("app").appendChild(g)}return g}
function gateOpen(step){GATE.step=step==="in"?"in":step==="terms"?"terms":step==="claim"?"claim":step==="welcome"?"welcome":"up";GATE.err="";gateRender()}
function gateClose(){const g=$("gate");if(g)g.remove();document.documentElement.classList.remove("gated")}
function gateNeeded(){if(!S.cloud)return"up";if(!S.prof||!S.prof.username)return"claim";if(!termsOk())return"terms";return""}
const GT_MARK="data:image/svg+xml,"+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="190 346 966 644"><path d="M332.1,353.7 C324.3,355.2 314.7,357.8 306.4,361.3 C298.1,364.7 290.0,369.3 282.3,374.3 C274.6,379.3 267.2,385.2 260.3,391.3 C253.3,397.5 246.7,404.2 240.7,411.2 C234.6,418.2 229.1,425.7 224.0,433.4 C218.9,441.0 214.1,449.1 210.1,457.0 C206.1,465.0 202.3,473.4 200.1,481.1 C197.9,488.8 196.3,497.0 196.7,503.4 C197.1,509.9 199.2,516.0 202.7,519.8 C206.3,523.6 212.0,525.8 218.0,526.2 C224.1,526.6 231.8,524.7 239.2,522.0 C246.6,519.4 254.6,514.9 262.4,510.4 C270.1,505.9 277.9,500.4 285.7,495.1 C293.4,489.9 301.1,484.3 308.9,478.9 C316.7,473.6 324.5,468.2 332.4,463.2 C340.3,458.1 348.2,453.1 356.4,448.6 C364.5,444.1 372.7,439.7 381.1,436.3 C389.5,432.8 398.2,429.7 406.9,427.8 C415.6,425.8 424.6,424.7 433.5,424.7 C442.3,424.7 451.3,425.7 459.9,427.8 C468.5,429.8 477.1,433.1 485.2,437.0 C493.4,440.9 501.3,445.9 508.9,451.3 C516.4,456.7 523.7,462.9 530.6,469.3 C537.5,475.8 544.1,482.8 550.5,489.9 C556.9,497.0 562.9,504.5 568.8,512.0 C574.6,519.5 580.2,527.2 585.6,535.0 C591.1,542.7 596.2,550.6 601.4,558.5 C606.5,566.3 611.4,574.3 616.4,582.2 C621.4,590.2 626.3,598.2 631.2,606.1 C636.0,614.1 640.9,622.1 645.7,630.1 C650.6,638.1 655.3,646.2 660.3,654.1 C665.2,662.1 670.2,670.0 675.3,677.9 C680.5,685.8 685.8,693.6 691.2,701.3 C696.6,709.0 702.2,716.7 707.8,724.4 C713.4,732.0 719.1,739.6 724.8,747.1 C730.6,754.7 736.5,762.2 742.5,769.5 C748.4,776.9 754.4,784.3 760.6,791.5 C766.8,798.7 773.1,805.7 779.6,812.7 C786.0,819.7 792.7,826.5 799.4,833.3 C806.1,840.1 812.9,846.8 819.8,853.3 C826.7,859.9 833.6,866.4 840.8,872.5 C847.9,878.6 855.1,884.6 862.6,890.1 C870.0,895.7 877.6,900.9 885.4,906.0 C893.1,911.1 901.1,915.9 909.1,920.6 C917.1,925.3 925.2,929.9 933.3,934.3 C941.5,938.7 949.7,943.0 958.0,947.0 C966.4,951.1 974.7,955.0 983.2,958.6 C991.7,962.3 1000.2,965.8 1008.8,968.9 C1017.5,972.0 1026.2,975.0 1035.0,977.2 C1043.8,979.5 1052.9,981.5 1061.7,982.4 C1070.5,983.3 1079.5,983.8 1087.7,982.8 C1095.9,981.7 1104.0,979.6 1110.8,976.0 C1117.5,972.3 1123.5,967.0 1128.3,960.8 C1133.1,954.5 1136.6,946.6 1139.4,938.6 C1142.3,930.5 1144.0,921.3 1145.5,912.3 C1147.0,903.3 1147.7,893.8 1148.4,884.4 C1149.1,875.1 1149.5,865.5 1149.7,856.0 C1149.9,846.5 1149.9,836.9 1149.6,827.4 C1149.3,817.9 1148.8,808.3 1148.0,798.9 C1147.2,789.4 1146.1,779.9 1144.9,770.5 C1143.7,761.1 1142.3,751.7 1140.7,742.4 C1139.1,733.0 1137.4,723.7 1135.4,714.6 C1133.4,705.4 1131.2,696.3 1128.7,687.4 C1126.2,678.4 1123.3,669.6 1120.3,660.8 C1117.3,652.0 1114.0,643.4 1110.6,634.8 C1107.1,626.3 1103.4,617.8 1099.4,609.4 C1095.5,601.0 1091.3,592.8 1086.9,584.6 C1082.5,576.4 1077.8,568.3 1073.0,560.3 C1068.2,552.4 1063.2,544.4 1057.9,536.7 C1052.6,528.9 1047.1,521.2 1041.3,513.8 C1035.5,506.3 1029.5,499.0 1023.1,491.9 C1016.7,484.9 1010.0,478.0 1003.1,471.4 C996.2,464.8 989.0,458.4 981.7,452.2 C974.4,446.0 966.9,440.0 959.3,434.2 C951.8,428.4 944.1,422.8 936.3,417.4 C928.6,412.0 920.8,406.7 912.9,401.6 C904.9,396.6 897.0,391.6 888.9,387.1 C880.7,382.6 872.5,378.2 864.1,374.5 C855.6,370.8 847.0,367.4 838.2,364.7 C829.4,361.9 820.3,359.6 811.3,357.9 C802.2,356.2 793.0,354.9 783.9,354.2 C774.7,353.5 765.5,353.4 756.3,353.8 C747.1,354.2 737.9,355.3 728.7,356.6 C719.6,357.8 710.3,359.6 701.2,361.4 C692.0,363.1 682.8,365.2 673.7,367.3 C664.6,369.4 655.5,371.7 646.5,374.0 C637.5,376.3 628.5,378.8 619.4,381.1 C610.4,383.4 601.4,385.8 592.2,387.9 C583.1,390.0 574.0,392.1 564.8,393.7 C555.6,395.4 546.3,397.1 537.1,398.0 C527.8,398.9 518.5,399.6 509.4,399.3 C500.2,399.0 491.0,398.1 482.1,396.3 C473.2,394.5 464.4,391.7 455.9,388.6 C447.3,385.5 439.0,381.4 430.6,377.6 C422.2,373.8 414.0,369.4 405.5,365.9 C397.0,362.4 388.5,358.9 379.8,356.6 C371.1,354.4 361.3,352.9 353.4,352.4 C345.4,351.9 340.0,352.2 332.1,353.7 Z" fill="#fff"/></svg>');
const GT_BUB=[[5,8,30,-14,.42],[62,4,24,10,.26],[36,24,34,-6,.14],[-6,36,28,15,.26],[62,38,30,-11,.38],[8,60,24,-5,.2],[44,58,22,13,.16]];
const gtBubbles=()=>`<div class="gw-bub" aria-hidden="true">${GT_BUB.map((b,i)=>`<img alt="" src="${GT_MARK}" style="left:${b[0]}%;top:${b[1]}%;width:${b[2]}%;--r:${b[3]}deg;opacity:${b[4]};animation-delay:${-i*1.9}s;animation-duration:${8+i%3*2}s">`).join("")}</div>`;
const gtBack=()=>`<button class="gt-back" data-gt="tab:welcome" aria-label="Back"><svg viewBox="0 0 24 24"><path d="M15 5l-7 7 7 7"/></svg></button>`;
function gateRender(){const g=gateEl();document.documentElement.classList.add("gated");const st=GATE.step;let h="";
  const logo=$("logo")?$("logo").src:"";
  if(st==="welcome"){
    h=`<div class="gw">${gtBubbles()}<div class="gw-in"><img class="gw-logo" alt="milo." src="${logo}"><h1>Your music.<br>Your people.</h1><p class="gt-sub">Playlists, friends and chat, on every device.</p>
    <button class="gt-go gt-pri" data-gt="tab:up">Sign up free</button><button class="gt-go gt-ol" data-gt="tab:in">Log in</button>
    ${navigator.onLine===false?`<button class="gt-link" data-gt="skip">Continue without an account</button>`:""}
    <p class="gt-fine">By continuing you agree to the terms, shown on the next screens.</p></div></div>`}
  else if(st==="up"||st==="in"){const up=st==="up";
    h=`<div class="gt-in gt-form">${gtBack()}<img class="gt-logo" alt="milo." src="${logo}"><h1>${up?"Create your account":"Welcome back"}</h1><p class="gt-sub">${up?"Your playlists, friends and settings, on every device.":"Sign in to pick up where you left off."}</p>
    ${up?`<input id="gt-u" placeholder="Username (letters, numbers, _)" maxlength="20" autocomplete="username" autocapitalize="none" spellcheck="false">`:""}
    <input id="gt-e" type="email" placeholder="Email" autocomplete="email" autocapitalize="none">
    <input id="gt-p" type="password" placeholder="Password${up?" (6+ characters)":""}" autocomplete="${up?"new-password":"current-password"}">
    <p class="gt-err" id="gt-err">${esc(GATE.err)}</p>
    <button class="gt-go gt-pri" data-gt="go" ${GATE.busy?"disabled":""}>${GATE.busy?"Please wait…":up?"Create account":"Log in"}</button>
    ${navigator.onLine===false?`<button class="gt-link" data-gt="skip">Continue without an account</button>`:""}
    <p class="gt-fine">${up?"Next you will see your profile card and the terms.":""}</p>
    <button class="gt-link" data-gt="tab:${up?"in":"up"}">${up?"Already have an account? Log in":"New here? Sign up free"}</button></div>`}
  else if(st==="claim"){h=`<div class="gt-in"><img class="gt-logo" alt="milo." src="${logo}"><h1>Pick a username</h1><p class="gt-sub">Friends find you by this name. 3 to 20 letters, numbers or _.</p>
    <input id="gt-u" placeholder="Username" maxlength="20" autocapitalize="none" spellcheck="false"><p class="gt-err" id="gt-err">${esc(GATE.err)}</p>
    <button class="gt-go gt-pri" data-gt="claim" ${GATE.busy?"disabled":""}>Continue</button><button class="gt-go gt-ol" data-gt="out">Sign out</button></div>`}
  else{const u=me(),p=S.prof||{},un=p.username||u.un||"",hrs=Object.values(S.stat||{}).reduce((a,b)=>a+b,0)/3600;
    h=`<div class="gt-in gt-wide"><h1>Your profile</h1><p class="gt-sub">This is how friends see you.</p>
    <div class="pcard"><i class="pbg" style="background:${avCss(u.av)}"></i><i class="pfade"></i><div class="ptop">${avEl(u,"big")}<button class="dm" data-gt="edit">Edit profile</button></div>
    <h2>${esc(u.name)}</h2><div class="hd">@${esc(un)} · Signed in</div><div class="bio">Your music, your way.</div>
    <div class="ptr"><div><b>${S.likes.size}</b><span>Liked</span></div><div><b>${S.pls.length}</b><span>Playlists</span></div><div><b>${hrs>=10?Math.round(hrs):hrs.toFixed(1)}</b><span>Hours</span></div></div></div>
    <h3 class="gt-h3">Terms and how your information is used</h3><div class="gt-terms" tabindex="0">${TERMS_HTML}</div>
    <label class="gt-chk"><input type="checkbox" id="gt-ok"><span>I have read and agree to the terms, including how my synced playlists, shared listening and messages are used.</span></label>
    <p class="gt-err" id="gt-err">${esc(GATE.err)}</p><button class="gt-go gt-pri" id="gt-agree" data-gt="agree" disabled>Agree and continue</button><button class="gt-go gt-ol" data-gt="out">Sign out</button></div>`}
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
