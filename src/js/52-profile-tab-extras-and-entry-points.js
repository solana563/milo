/* ----- profile tab extras and entry points ----- */
function sxHtml(){const p=S.prof||{},on=!!S.cloud;
  return `<div class="row" style="margin-top:6px"><h2>Friends &amp; sharing</h2></div><div class="sxbox">${on?`<button class="sxb" data-sx="friends"><b>Friends</b><small>Find people by username, share playlists, chat</small></button>
  <label class="sxs"><span><b>Share my playlists</b><small>Friends see your playlists as song DNA, never your files</small></span><input type="checkbox" data-sx="shpl" ${p.share_pl!==false?"checked":""}></label>
  <label class="sxs"><span><b>Share what I'm listening to</b><small>Friends can listen along by streaming it</small></span><input type="checkbox" data-sx="shnp" ${p.share_np!==false?"checked":""}></label>`:`<p>Sign up to add friends and keep your playlists on every device.</p><button class="addb" data-sx="signup">Sign up or sign in</button>`}
  ${sxExtra()}<button class="sxb" data-sx="terms"><b>Terms and privacy</b><small>How your information is used</small></button></div>`}
document.addEventListener("click",e=>{const b=e.target.closest("[data-sx]");if(!b||b.tagName==="INPUT")return;const k=b.dataset.sx;
  if(k==="friends")soOpen("home");else if(k==="signup")gateOpen("up");else if(k==="terms")modal(`<h3>Terms and privacy</h3><div class="gt-terms" style="max-height:48vh">${TERMS_HTML}</div>`)});
document.addEventListener("change",e=>{const b=e.target.closest("input[data-sx]");if(!b||!S.prof)return;const k=b.dataset.sx;
  if(k==="shpl"){profPush({share_pl:b.checked});if(b.checked){plSig="";plPush()}else{plSig="";sb("/rest/v1/shared?on_conflict=user_id",{m:"POST",h:{Prefer:"resolution=merge-duplicates,return=minimal"},b:{user_id:S.cloud.id,pl:null}}).catch(()=>{})}}
  else if(k==="shnp"){profPush({share_np:b.checked});npSig="";if(!b.checked)sb("/rest/v1/shared?on_conflict=user_id",{m:"POST",h:{Prefer:"resolution=merge-duplicates,return=minimal"},b:{user_id:S.cloud.id,np:null}}).catch(()=>{});else npTick()}});
async function delAcct(){const a=S.acct;try{if(a&&a.cid&&S.cloud)await sb("/rest/v1/rpc/delete_me",{m:"POST",b:{}})}catch(e){toast(sbMsg(e));return}
  if(a){S.accts=S.accts.filter(x=>x.id!==a.id);LS.set("milo.accts",S.accts);try{localStorage.removeItem("milo.v2."+a.id)}catch(e){}}
  S.cloud=null;LS.set("milo.cloud",null);S.prof=null;LS.set("milo.prof",null);S.acct=null;LS.set("milo.cur","");S.termsAt=null;LS.set("milo.terms",null);closeM();soStop();afterSwitch();GATE.step="up";gateRender()}
