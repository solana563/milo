/* ---------- insights, autoplay, share, backup, shortcuts ---------- */
const dayKey=(d=new Date())=>d.getFullYear()+"-"+String(d.getMonth()+1).padStart(2,"0")+"-"+String(d.getDate()).padStart(2,"0");
function statTick(){S.stat=S.stat||{};const k=dayKey();S.stat[k]=(S.stat[k]||0)+.25}
const durTxt=s=>{s=Math.round(s);const h=Math.floor(s/3600),m=Math.floor(s%3600/60);return h?h+"h "+m+"m":m?m+" min":s+" s"};
function insightsM(){const st=S.stat||{},pl=S.plays||{};
  const tot=Object.values(st).reduce((a,b)=>a+b,0),days=[];
  for(let i=6;i>=0;i--){const d=new Date();d.setDate(d.getDate()-i);days.push([d.toLocaleDateString(undefined,{weekday:"short"}).slice(0,1),st[dayKey(d)]||0])}
  const mx=Math.max(60,...days.map(x=>x[1]));let streak=0;
  for(let i=0;i<400;i++){const d=new Date();d.setDate(d.getDate()-i);if((st[dayKey(d)]||0)>=30)streak++;else if(i>0)break}
  const rows=[],ar={},gn={};let np=0;
  Object.keys(pl).forEach(k=>{const i=idxOfKey(k);if(i<0)return;const t=T[i],n=pl[k];np+=n;rows.push([i,n]);if(t.a)ar[t.a]=(ar[t.a]||0)+n;if(t.g)gn[t.g]=(gn[t.g]||0)+n});
  rows.sort((a,b)=>b[1]-a[1]);const top=o=>Object.entries(o).sort((a,b)=>b[1]-a[1]).slice(0,5);
  const list=(a,fn)=>a.map((x,j)=>`<div class="insr"><em>${j+1}</em>${fn(x)}</div>`).join("");
  modal(`<h3>Your insights</h3><div class="ins"><div><b>${durTxt(tot)}</b><small>listened</small></div><div><b>${np}</b><small>plays</small></div><div><b>${streak}</b><small>day streak</small></div></div>
  <div class="inb">${days.map(([l,v])=>`<div class="c" title="${durTxt(v)}"><span class="u" style="--h:${Math.round(v/mx*100)*.7}px"></span><small>${l}</small></div>`).join("")}</div>
  ${rows.length?`<h4>Top songs</h4>${list(rows.slice(0,5),([i,n])=>`<i style="background:${artOf(T[i])}"></i><span>${esc(T[i].t)}<small>${esc(T[i].a||"")}</small></span><small>${n} play${n>1?"s":""}</small>`)}
  <h4>Top artists</h4>${list(top(ar),([a,n])=>`<span>${esc(a)}</span><small>${n} plays</small>`)}
  ${Object.keys(gn).length?`<h4>Top genres</h4>${list(top(gn),([a,n])=>`<span>${esc(a)}</span><small>${n} plays</small>`)}`:""}`:`<p>Listen to a song for 30 seconds and your top songs, artists and genres show up here.</p>`}`)}
function shareTrack(i){const t=T[i],txt=t.t+(t.a?" — "+t.a:"")+" · on milo.",url=(t.stream&&t.raw&&!t.ph&&/^https?:/.test(location.protocol)?dnaLink(dnaOf(t)):"")||t.page||(/^https?:/.test(t.url||"")?t.url:"");
  if(navigator.share){navigator.share({title:t.t,text:txt,url:url||undefined}).catch(()=>{});return}
  const s=url?txt+"\n"+url:txt;try{navigator.clipboard.writeText(s).then(()=>toast("Copied to clipboard"),()=>toast(s))}catch(e){toast(s)}}
function backupOut(){try{const o=LS.get(dkey(),{}),b=new Blob([JSON.stringify({milo:2,at:Date.now(),data:o},null,1)],{type:"application/json"}),a=document.createElement("a");a.href=URL.createObjectURL(b);a.download="milo-backup-"+dayKey()+".json";document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(a.href),4000);toast("Backup saved. Songs you uploaded are not included.")}catch(e){toast("Couldn't create the backup")}}
function backupIn(f){const r=new FileReader();r.onload=()=>{try{const j=JSON.parse(r.result);if(!j||j.milo!==2||!j.data)throw 0;LS.set(dkey(),j.data);loadData();save();render();toast("Backup restored")}catch(e){toast("That isn't a milo. backup file")}};r.readAsText(f)}
function keysM(){const R=[["Space","Play / pause"],["← →","Seek 5 s"],["Shift + ← →","Previous / next"],["N / P","Next / previous"],["S","Shuffle"],["R","Repeat"],["L","Lyrics"],["F","Full-screen now playing"],["M","Mute"],["+ / −","Volume"],["/","Search"],["?","This list"],["TV remote","Arrows move, OK selects, Back goes back"]];
  modal(`<h3>Keyboard shortcuts</h3><div class="kbd">${R.map(([k,v])=>`<kbd>${k}</kbd><span>${v}</span>`).join("")}</div>`)}
$("view").addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-tool]");if(!b)return;const k=b.dataset.tool;if(k==="ins")insightsM();else if(k==="bkx")backupOut();else if(k==="keys")keysM()});
document.addEventListener("change",e=>{const t=e.target;if(t&&t.dataset&&t.dataset.bk&&t.files&&t.files[0]){backupIn(t.files[0]);t.value=""}});

