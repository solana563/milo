/* ----- one-tap error report: the last things that went wrong, ready to send. Nothing leaves the device unless you send it. ----- */
const ER={log:[]};
function erAdd(tag,msg){try{ER.log.push(new Date().toTimeString().slice(0,8)+"  ["+tag+"] "+String(msg==null?"":msg).slice(0,600));if(ER.log.length>40)ER.log.shift()}catch(e){}}
window.addEventListener("error",e=>erAdd("error",(e.message||"")+(e.filename?" @ "+String(e.filename).split("/").pop()+":"+e.lineno:"")));
window.addEventListener("unhandledrejection",e=>erAdd("promise",e.reason&&(e.reason.message||e.reason)));
{const _f=ymFallback;ymFallback=function(i,tog,t,e){erAdd("stream",(t&&t.yt||"")+" "+(e&&(e.message||e)));return _f.apply(this,arguments)}}
{const _y=ytErr;ytErr=function(e){erAdd("youtube",JSON.stringify(e&&e.data!==undefined?e.data:e));return _y.apply(this,arguments)}}
function erText(){const d=window.miloDesktop?"desktop "+(miloDesktop.platform||""):(NV&&NV.cap?"android wrapper":"web");
  return"milo. error report\n"+d+" · "+navigator.userAgent+"\n-----\n"+(ER.log.length?ER.log.join("\n"):"Nothing has gone wrong since milo. opened.")}
{const _x=sxExtra;sxExtra=function(){return _x()+'<button class="sxb" data-er="send"><b>Something not working?</b><small>Send the last errors with your device details</small></button>'}}
document.addEventListener("click",async e=>{if(!e.target.closest("[data-er]"))return;const t=erText();
  try{if(navigator.share){await navigator.share({title:"milo. error report",text:t});return}}catch(x){if(x&&x.name==="AbortError")return}
  try{await navigator.clipboard.writeText(t);toast("Error report copied. Paste it where you want to send it.")}catch(x){modal(`<h3>Error report</h3><textarea readonly style="width:100%;height:180px">${esc(t)}</textarea><button class="ghost" data-x>Close</button>`)}});
