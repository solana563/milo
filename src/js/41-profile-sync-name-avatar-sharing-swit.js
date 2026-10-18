/* ----- profile sync (name, avatar, sharing switches) ----- */
async function profPush(extra){if(!S.cloud||!S.prof)return;const u=me(),b=Object.assign({name:u.name},extra||{});if(u.av&&(u.av.t!=="img"||(u.av.u||"").length<60000))b.av=u.av;
  try{await sb("/rest/v1/profiles?id=eq."+encodeURIComponent(S.cloud.id),{m:"PATCH",h:{Prefer:"return=minimal"},b});Object.assign(S.prof,b)}catch(e){}}
const _edSave=edSave;edSave=function(){_edSave();if(!$("merr")||!$("merr").textContent){profPush();if($("gate")&&GATE.step==="terms")gateRender()}};
