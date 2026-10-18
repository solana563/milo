/* ---------- splash + installable-app bits ---------- */
const t0=performance.now();
function hideSplash(){const s=$("splash");if(!s)return;setTimeout(()=>{s.classList.add("go");setTimeout(()=>{if(s.parentNode)s.remove()},700)},Math.max(0,1200-(performance.now()-t0)))}
const isPwa=()=>!!document.querySelector('link[rel="manifest"]');
const isStandalone=()=>{try{return matchMedia("(display-mode: standalone)").matches||navigator.standalone===true}catch(e){return false}};
let ipEvt=null;
addEventListener("beforeinstallprompt",e=>{e.preventDefault();ipEvt=e;if(S.tab==="Profile"&&!S.nav)render()});
addEventListener("appinstalled",()=>{ipEvt=null;toast("milo. installed");if(S.tab==="Profile"&&!S.nav)render()});
function installHtml(){
  const emb=top!==self,sec=location.protocol==="https:"||location.hostname==="localhost"||location.hostname==="127.0.0.1",
    ios=/iphone|ipad|ipod/i.test(navigator.userAgent)||(navigator.platform==="MacIntel"&&navigator.maxTouchPoints>1),
    h=(t,b)=>`<div class="row" style="margin-top:24px"><h2>Install milo.</h2></div><p class="empty" style="padding-top:0">${t}</p>${b||""}`;
  if(isStandalone())return `<div class="row" style="margin-top:24px"><h2>App</h2></div><p class="empty" style="padding-top:0">milo. is installed on this device.</p>`;
  if(emb)return h("This preview is shown inside another page, so it can't be installed from here. Download the app folder from the chat, put it on a web address (README.txt has the steps), then install from that address.");
  if(!sec)return h(location.protocol==="file:"?"This copy was opened straight from a file, and browsers only allow installing from a web address that starts with https. Upload the folder to Netlify Drop (free, about a minute), or run python3 -m http.server 8080 in the folder and open http://localhost:8080.":"Installing needs an https address, and this one isn't secure.");
  if(ipEvt)return h("Get a full-screen app with its own icon that starts even when you're offline.",'<button class="addb" data-inst>Install app</button>');
  if(ios)return h("On iPhone or iPad, open this address in Safari, tap Share, then Add to Home Screen.");
  return h("Open your browser's menu and choose Install app, or Add to Home screen. If you don't see it yet, reload the page once.")}
function route(){const m={home:"Home",search:"Search",discover:"Discover",library:"Library",profile:"Profile"},h=(location.hash||"").slice(1).toLowerCase();if(m[h])go(m[h])}
if("launchQueue" in window){try{launchQueue.setConsumer(async p=>{if(!p||!p.files||!p.files.length)return;const fs=await Promise.all(p.files.map(h=>h.getFile()));await addFiles(fs);const i=idxOfKey(fs[0].name+fs[0].size);if(i>=0)play(i)})}catch(e){}}

