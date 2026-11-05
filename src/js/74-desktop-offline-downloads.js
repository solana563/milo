/* Windows desktop: save YouTube songs on the PC for offline listening. The file is fetched by the desktop app and played back from disk. */
const DLD={map:{},busy:{},real:ymStream};
const dlDk=()=>!!(window.miloDesktop&&miloDesktop.dlStart);
async function dlLoad(){if(!dlDk())return;try{DLD.map=(await miloDesktop.dlList())||{}}catch(e){}}
try{dlLoad();if(dlDk())miloDesktop.onDlProgress(p=>{const b=document.querySelector('[data-dlp="'+p.vid+'"]');if(b)b.textContent=Math.round(p.pct*100)+"%"})}catch(e){}
function dlB64(s){return btoa(unescape(encodeURIComponent(s))).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/,"")}
/* a saved song skips the lookup and plays from the file, so it works with no connection */
{const _s=ymStream;ymStream=async function(vid,force){const o=DLD.map[vid];
  if(o&&!force)return{url:"",off:o,mime:"",exp:Date.now()+3e9,d:o.d||0,ua:"",title:o.t||"",author:o.a||""};
  return _s(vid,force)}}
{const _y=ymSrc;ymSrc=function(s){return s&&s.off?"milofile://f/"+dlB64(s.off.file)+"/"+s.off.ext:_y(s)}}
async function dlSave(t){const vid=t.yt;if(!vid||DLD.busy[vid])return;DLD.busy[vid]=1;toast("Downloading “"+t.t+"”…");
  try{const s=await DLD.real(vid,true);if(!s.url)throw new Error("no stream");
    await miloDesktop.dlStart({vid,url:s.url,ua:s.ua,mime:s.mime,t:t.t,a:t.a,d:s.d});await dlLoad();toast("Saved for offline")}
  catch(e){toast("Download failed: "+String((e&&e.message)||e).slice(0,70))}
  delete DLD.busy[vid]}
async function dlDrop(vid){try{await miloDesktop.dlRemove(vid);delete DLD.map[vid];YM.sc.delete(vid);toast("Download removed")}catch(e){}}
{const _m=trackMenu;trackMenu=function(i){_m(i);try{const t=T[i];if(!dlDk()||!t||!t.yt)return;const p=$("mdl").querySelector(".pan"),have=DLD.map[t.yt];
  p.insertAdjacentHTML("beforeend",'<button class="mi" data-dl="'+(have?"rm":"add")+":"+i+'">'+(have?"Remove download":"Download for offline")+' <small data-dlp="'+t.yt+'"></small></button>')}catch(e){}}}
function dlCard(){if(!dlDk())return"";const n=Object.keys(DLD.map).length;
  return '<button class="sxb" data-dl="open"><b>Downloads</b><small>'+(n?n+" song"+(n>1?"s":"")+" saved on this PC":"Save YouTube songs to listen offline")+'</small></button>'}
{const _x=sxExtra;sxExtra=function(){return _x()+dlCard()}}
document.addEventListener("click",e=>{const b=e.target.closest("[data-dl]");if(!b)return;const [k,a]=b.dataset.dl.split(":");
  if(k==="add"){closeM();dlSave(T[+a])}
  else if(k==="rm"){const t=T[+a];closeM();if(t)dlDrop(t.yt)}
  else if(k==="open"){const ids=Object.keys(DLD.map);
    modal("<h3>Downloads</h3>"+(ids.length?ids.map(v=>{const o=DLD.map[v];return '<div class="li"><span><b>'+esc(o.t||v)+'</b><small>'+esc(o.a||"")+" · "+(o.size/1048576).toFixed(1)+' MB</small></span><button class="ghost" data-dl="del:'+v+'">Remove</button></div>'}).join(""):"<p>No saved songs yet. Open a YouTube song's menu and choose Download for offline.</p>"))}
  else if(k==="del"){dlDrop(a).then(closeM)}});
