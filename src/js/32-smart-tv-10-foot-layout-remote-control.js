/* ---------- smart TV: 10-foot layout, remote-control keys, D-pad focus ---------- */
var TVUA=/Tizen|Web0S|WebOS|SMART-TV|SmartTV|Android TV|AFTM|AFTT|AFTS|AFTB|BRAVIA|VIDAA|HbbTV|CrKey|GoogleTV|PhilipsTV|NetCast|Large Screen/i;
function tvOn(){const q=location.search.match(/[?&]tv=(\d)/);if(q)LS.set("milo.tv",q[1]==="1");const m=LS.get("milo.tv",null);return m!==null?!!m:TVUA.test(navigator.userAgent)}
function tvApply(){const on=tvOn(),a=$("app");a.toggleAttribute("data-tv",on);a.style.setProperty("--tvz",Math.max(1,Math.min(2,innerWidth/1280)).toFixed(2));S.tv=on;
  if(on){tvMark();try{if(window.tizen&&tizen.tvinputdevice)["MediaPlayPause","MediaPlay","MediaPause","MediaStop","MediaFastForward","MediaRewind","MediaTrackNext","MediaTrackPrevious","ColorF0Red","ColorF1Green"].forEach(k=>{try{tizen.tvinputdevice.registerKey(k)}catch(e){}})}catch(e){}}}
const TVSEL='button:not([disabled]),a[href],input:not([type=file]):not([type=hidden]),[tabindex="0"],label.addb';
function tvMark(){document.querySelectorAll("#view [data-i],#view [data-go],#view [data-mix],#view [data-hub],#view [data-nav],#view [data-lib],#view [data-np],#view [data-m],#view [data-acct],#view [data-si],#view [data-so],#view [data-back],.tile,.dc,.dc2").forEach(e=>{if(!e.hasAttribute("tabindex"))e.tabIndex=0})}
function tvRoot(){return !$("mdl").hidden?$("mdl"):$("lyr").classList.contains("open")?$("lyr"):IM.on?$("im"):$("full").classList.contains("open")?$("full"):$("app")}
function tvList(){const root=tvRoot();return [...root.querySelectorAll(TVSEL)].filter(e=>{if(e.closest("[hidden]"))return false;if(root===$("app")&&(e.closest("#full")||e.closest("#lyr")||e.closest("#im")||e.closest("#mdl")))return false;const r=e.getBoundingClientRect();if(r.width<4||r.height<4)return false;const cs=getComputedStyle(e);return cs.visibility!=="hidden"&&cs.display!=="none"&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth})}
function tvMove(k){const els=tvList(),cur=document.activeElement;if(!cur||!els.includes(cur)){if(els[0])els[0].focus({preventScroll:true});return}
  const c=cur.getBoundingClientRect(),cx=c.left+c.width/2,cy=c.top+c.height/2;let best=null,bs=1e9;
  els.forEach(e=>{if(e===cur)return;const r=e.getBoundingClientRect(),dx=r.left+r.width/2-cx,dy=r.top+r.height/2-cy;let ok,p,o;
    if(k==="ArrowRight"){ok=dx>4&&Math.abs(dy)<=Math.abs(dx)*1.6+c.height;p=dx;o=Math.abs(dy)}
    else if(k==="ArrowLeft"){ok=dx<-4&&Math.abs(dy)<=Math.abs(dx)*1.6+c.height;p=-dx;o=Math.abs(dy)}
    else if(k==="ArrowDown"){ok=dy>4&&Math.abs(dx)<=Math.abs(dy)*1.6+c.width;p=dy;o=Math.abs(dx)}
    else{ok=dy<-4&&Math.abs(dx)<=Math.abs(dy)*1.6+c.width;p=-dy;o=Math.abs(dx)}
    if(ok){const s=p+o*2;if(s<bs){bs=s;best=e}}});
  if(best){best.focus({preventScroll:true});try{best.scrollIntoView({block:"nearest",inline:"nearest",behavior:"smooth"})}catch(e){}}}
function tvBack(){backStep()}
addEventListener("keydown",e=>{if(!S.tv)return;const k=e.key,c=e.keyCode,tag=e.target&&e.target.tagName;
  if(c===10009||c===461||k==="BrowserBack"||k==="GoBack"||(k==="Backspace"&&tag!=="INPUT"&&tag!=="TEXTAREA")){e.preventDefault();e.stopImmediatePropagation();tvBack();return}
  if(k==="MediaPlayPause"||c===10252||c===415||c===19||k==="MediaPlay"||k==="MediaPause"){e.preventDefault();e.stopImmediatePropagation();if(S.cur>=0)toggle();return}
  if(k==="MediaTrackNext"||c===10233){e.preventDefault();next(false);return}
  if(k==="MediaTrackPrevious"||c===10232){e.preventDefault();prev();return}
  if(k==="MediaFastForward"||c===417){e.preventDefault();if(S.cur>=0&&!T[S.cur].radio)seekTo(Math.min(T[S.cur].d||1e9,S.pos+15));return}
  if(k==="MediaRewind"||c===412){e.preventDefault();if(S.cur>=0&&!T[S.cur].radio)seekTo(Math.max(0,S.pos-15));return}
  if(k==="MediaStop"||c===413){e.preventDefault();if(S.playing)toggle();return}
  if(e.target&&e.target.type==="range"&&(k==="ArrowLeft"||k==="ArrowRight"))return;
  if(tag==="INPUT"&&e.target.type!=="range"&&e.target.type!=="checkbox"&&(k==="ArrowLeft"||k==="ArrowRight"))return;
  if(k==="ArrowUp"||k==="ArrowDown"||k==="ArrowLeft"||k==="ArrowRight"){e.preventDefault();e.stopImmediatePropagation();if(IM.on||$("full").classList.contains("open")&&!document.activeElement.closest(".ctl,.fh,.ft,.sub"))return tvMove(k);tvMove(k);return}
  if(k==="Enter"){const a=document.activeElement;if(a&&a!==document.body&&!/^(BUTTON|A|INPUT|TEXTAREA|SELECT|LABEL)$/.test(a.tagName)){e.preventDefault();a.click()}}},true);
new MutationObserver(()=>{if(S.tv){clearTimeout(S.tvm);S.tvm=setTimeout(tvMark,80)}}).observe($("view"),{childList:true,subtree:true});
addEventListener("resize",()=>{if(S.tv)tvApply()});
$("view").addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-tool=tv]");if(!b)return;LS.set("milo.tv",!tvOn());tvApply();render();toast(tvOn()?"TV mode on. Use the arrow keys and OK on your remote.":"TV mode off")});
tvApply();

