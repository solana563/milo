/* ----- sharing: now playing + playlists, as song DNA only ----- */
const shNP=()=>!S.prof||S.prof.share_np!==false,shPL=()=>!S.prof||S.prof.share_pl!==false;
let npSig="",npAt=0,plSig="",plT=0;
async function npTick(){if(!S.cloud||!S.prof||!termsOk())return;const t=T[S.cur],on=!!(t&&S.playing&&shNP());let np=null;if(on){const d=dnaOf(t);np=d?Object.assign({p:1},/^\d{1,3},\d{1,3},\d{1,3}$/.test(String(t.c))?{c:t.c}:{},d):null}
  const sig=np?np.id||np.t+"|"+np.a:"off";if(sig===npSig&&(!np||Date.now()-npAt<90000))return;npSig=sig;npAt=Date.now();
  try{await sb("/rest/v1/shared?on_conflict=user_id",{m:"POST",h:{Prefer:"resolution=merge-duplicates,return=minimal"},b:{user_id:S.cloud.id,np,np_at:new Date().toISOString()}})}catch(e){npSig=""}}
setInterval(npTick,4000);
function plBuild(){if(!shPL())return null;const out=[];const lk=[...S.likes].map(dnaOfKey).filter(Boolean).slice(0,150);if(lk.length)out.push({id:"liked",name:"Liked songs",t:lk});
  S.pls.slice(0,40).forEach(p=>{const t=(p.keys||[]).map(dnaOfKey).filter(Boolean).slice(0,150);out.push({id:p.id,name:p.name,t})});return out}
async function plPush(){if(!S.cloud||!S.prof||!termsOk())return;const pl=plBuild(),s=JSON.stringify(pl);if(s===plSig)return;if(s&&s.length>380000)return;plSig=s;
  try{await sb("/rest/v1/shared?on_conflict=user_id",{m:"POST",h:{Prefer:"resolution=merge-duplicates,return=minimal"},b:{user_id:S.cloud.id,pl,pl_at:new Date().toISOString()}})}catch(e){plSig=""}}
const _save=save;save=function(){const r=_save.apply(this,arguments);clearTimeout(plT);plT=setTimeout(plPush,1500);return r};
