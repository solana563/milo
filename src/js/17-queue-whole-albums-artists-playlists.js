/* ---------- queue whole albums, artists, playlists ---------- */
const cmo=r=>`<button class="mo cmo" data-cm="${r}" aria-label="More">${DOTS}</button>`;
function collIdx(k,id){if(k==="album"){const o=albums().find(x=>x.id===id);return o?o.idx:[]}
  if(k==="artist"){const o=artists().find(x=>x.id===id);return o?o.idx:[]}
  if(id==="liked")return likedIdx();const p=S.pls.find(x=>x.id===id);return p?plIdx(p):[]}
function collMenu(ref){const sr=decodeURIComponent(ref),c=sr.indexOf(":"),k=sr.slice(0,c),id=sr.slice(c+1),idx=collIdx(k,id);S.cidx=idx;
  const nm=k==="album"?(albums().find(x=>x.id===id)||{}).name:k==="artist"?(artists().find(x=>x.id===id)||{}).name:id==="liked"?"Liked songs":(S.pls.find(x=>x.id===id)||{}).name;
  const bg=k==="pl"?plArt(id,idx):idx.length?artOf(T[rep(idx)]):"var(--card)";
  modal(`<div class="mh"><i class="art" style="background:${bg}"></i><div><b>${esc(nm||"")}</b><br><small>${idx.length} song${idx.length===1?"":"s"}</small></div></div>
  <button class="mi" data-a="cpl:">Play</button><button class="mi" data-a="csh:">Shuffle</button><button class="mi" data-a="cpn:">Play next</button><button class="mi" data-a="cqu:">Add to queue</button><button class="mi" data-a="cap:">Add all to a playlist</button><button class="mi" data-a="cop:${ref}">Open</button>`)}
function qAddList(idx,nx){idx=(idx||[]).filter(i=>T[i]&&!T[i].ph);const n=idx.length,pl=n===1?"":"s";
  if(!n){toast("Nothing to add");return}
  if(S.cur<0){playList(idx,false);toast(`Playing ${n} song${pl}`);return}
  if(nx)S.q.splice(S.qi+1,0,...idx);else{S.q.push(...idx);idx.forEach(i=>{if(!S.base.includes(i))S.base.push(i)})}
  S.pre=null;toast(nx?`${n} song${pl} will play next`:`${n} song${pl} added to the queue`)}
function addKeys(p,idx){idx.forEach(i=>{const k=T[i].key;if(!p.keys.includes(k)){p.keys.push(k);rememberStream(i)}});save()}
function addListPlM(){modal(`<h3>Add all to playlist</h3>${S.pls.map(p=>`<button class="mi" data-a="tpl:${p.id}">${esc(p.name)}</button>`).join("")}<button class="mi" data-a="npl:">+ New playlist</button>`)}
