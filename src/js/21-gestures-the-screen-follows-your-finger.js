/* ---------- gestures: the screen follows your finger; distance or flick speed decides ---------- */
const G={sup:0};
document.addEventListener("click",e=>{if(Date.now()<G.sup){e.stopPropagation();e.preventDefault()}},true);
function buzz(n){try{navigator.vibrate&&navigator.vibrate(n||8)}catch(e){}}
function drag(el,o){let x0=0,y0=0,px=0,py=0,pt=0,vx=0,vy=0,ax=null,on=false;
  const reset=()=>{on=false;ax=null;if(o.cancel)o.cancel()};
  el.addEventListener("touchstart",e=>{if(e.touches.length!==1){on=false;return}
    if(o.skip&&e.target.closest&&e.target.closest(o.skip)){on=false;return}
    if(o.start&&!o.start(e)){on=false;return}
    const t=e.touches[0];on=true;ax=null;x0=px=t.clientX;y0=py=t.clientY;pt=Date.now();vx=vy=0},{passive:true});
  el.addEventListener("touchmove",e=>{if(!on)return;const t=e.touches[0],dx=t.clientX-x0,dy=t.clientY-y0,n=Date.now(),dt=Math.max(1,n-pt);
    vx=.75*vx+.25*((t.clientX-px)/dt);vy=.75*vy+.25*((t.clientY-py)/dt);px=t.clientX;py=t.clientY;pt=n;
    if(!ax){if(Math.abs(dx)<10&&Math.abs(dy)<10)return;ax=Math.abs(dx)>Math.abs(dy)*1.2?"x":Math.abs(dy)>Math.abs(dx)*1.2?"y":null;if(!ax)return;
      if(o.axes&&!o.axes.includes(ax)){on=false;return}}
    if(o.move)o.move(dx,dy,ax,e)},{passive:true});
  el.addEventListener("touchend",e=>{if(!on)return;on=false;if(!ax)return;const t=e.changedTouches[0];G.sup=Date.now()+350;o.end&&o.end(t.clientX-x0,t.clientY-y0,vx,vy,ax)});
  el.addEventListener("touchcancel",reset)}
function artSwap(dir){const a=$("big"),W=innerWidth;buzz();
  a.style.transition="transform .18s ease-in,opacity .18s";a.style.transform=`translateX(${-dir*W*.7}px)`;a.style.opacity=0;
  setTimeout(()=>{dir>0?next(false):prev();a.style.transition="none";a.style.transform=`translateX(${dir*W*.5}px)`;
    requestAnimationFrame(()=>{a.style.transition="transform .32s cubic-bezier(.2,.8,.2,1),opacity .32s";a.style.transform="";a.style.opacity=1;setTimeout(()=>{a.style.transition="";a.style.opacity=""},340)})},170)}
const flick=(dx,vx,W)=>Math.abs(dx)>W||(Math.abs(vx)>.45&&Math.abs(dx)>40);
/* mini player: drag up to open Now Playing, swipe sideways to skip */
drag($("mini"),{start:()=>S.cur>=0&&!mqDesk.matches,
  move:(dx,dy,ax)=>{if(ax==="y"){if(dy<0){const f=$("full");f.style.transition="none";f.style.transform=`translateY(${Math.max(0,innerHeight+dy)}px)`}}else{const m=$("mini");m.style.transition="none";m.style.transform=`translateX(${dx*.6}px)`}},
  end:(dx,dy,vx,vy,ax)=>{const f=$("full"),m=$("mini");
    if(ax==="y"){f.style.transition="";f.style.transform="";if(dy<-60||(vy<-.45&&dy<-25)){openFull();buzz()}}
    else{m.style.transition="transform .25s cubic-bezier(.2,.8,.2,1)";m.style.transform="";setTimeout(()=>{m.style.transition=""},260);if(flick(dx,vx,70)){buzz();dx<0?next(false):prev()}}},
  cancel:()=>{const f=$("full"),m=$("mini");f.style.transition="";f.style.transform="";m.style.transition="";m.style.transform=""}});
/* Now Playing: drag down to dismiss, swipe the cover to change song, flick up for lyrics */
drag($("full"),{skip:"input,#lyPeek",
  move:(dx,dy,ax)=>{const f=$("full");if(ax==="y"){if(dy>0){f.style.transition="none";f.style.transform=`translateY(${dy}px)`;f.style.borderRadius=Math.min(32,dy/6)+"px"}}
    else{const a=$("big");a.style.transition="none";a.style.transform=`translateX(${dx}px) rotate(${dx/55}deg)`;a.style.opacity=1-Math.min(.5,Math.abs(dx)/(innerWidth*1.2))}},
  end:(dx,dy,vx,vy,ax)=>{const f=$("full"),a=$("big");
    if(ax==="y"){f.style.transition="";f.style.transform="";f.style.borderRadius="";
      if(dy>110||(vy>.55&&dy>40)){f.classList.remove("open");buzz(6)}else if(dy<-70&&vy<-.35){openLyrics();buzz(6)}}
    else if(flick(dx,vx,innerWidth*.22))artSwap(dx<0?1:-1);
    else{a.style.transition="transform .25s cubic-bezier(.2,.8,.2,1),opacity .25s";a.style.transform="";a.style.opacity="";setTimeout(()=>{a.style.transition=""},260)}},
  cancel:()=>{const f=$("full"),a=$("big");f.style.transition="";f.style.transform="";f.style.borderRadius="";a.style.transition="";a.style.transform="";a.style.opacity=""}});
/* lyrics: drag down from the header to dismiss, swipe sideways to skip; the lyric list itself just scrolls */
(function(){let inL=false;
  drag($("lyr"),{skip:"input,#lyF",start:e=>{inL=!!(e.target.closest&&e.target.closest("#lyL"));return true},
    move:(dx,dy,ax)=>{const l=$("lyr");if(ax==="y"){if(inL||dy<0)return;l.style.transition="none";l.style.transform=`translateY(${dy}px)`}else{const a=$("lyArt");a.style.transition="none";a.style.transform=`translateX(${dx*.4}px)`}},
    end:(dx,dy,vx,vy,ax)=>{const l=$("lyr");
      if(ax==="y"){l.style.transition="";l.style.transform="";if(!inL&&(dy>110||(vy>.55&&dy>40))){l.classList.remove("open");buzz(6)}}
      else{const a=$("lyArt");a.style.transition="transform .25s";a.style.transform="";setTimeout(()=>{a.style.transition=""},260);if(flick(dx,vx,70)){buzz();dx<0?next(false):prev()}}},
    cancel:()=>{const l=$("lyr"),a=$("lyArt");l.style.transition="";l.style.transform="";a.style.transition="";a.style.transform=""}})})();
/* lyrics slide at the bottom of Now Playing */
drag($("lyPeek"),{axes:["y"],start:()=>S.cur>=0,
  move:(dx,dy)=>{if(dy<0){const l=$("lyr");l.style.transition="none";l.style.transform=`translateY(${Math.max(0,innerHeight+dy)}px)`}},
  end:(dx,dy,vx,vy)=>{const l=$("lyr");l.style.transition="";l.style.transform="";if(dy<-innerHeight*.15||(vy<-.45&&dy<-25)){openLyrics();buzz(6)}},
  cancel:()=>{const l=$("lyr");l.style.transition="";l.style.transform=""}});
$("lyPeek").addEventListener("click",()=>{if(S.cur>=0)openLyrics()});
$("lyPeek").addEventListener("keydown",e=>{if((e.key==="Enter"||e.key===" ")&&S.cur>=0){e.preventDefault();openLyrics()}});
/* main screens: swipe to change tab (or go back inside an album/playlist) */
drag($("view"),{skip:".pls,.chips,.seg,input,.bandw",axes:["x"],
  end:(dx,dy,vx)=>{if(!flick(dx,vx,innerWidth*.26))return;
    if(S.nav){if(dx>0){S.nav=null;render();buzz()}return}
    const k=Object.keys(ICON),n=k.indexOf(S.tab)+(dx<0?1:-1);if(n>=0&&n<k.length){buzz();go(k[n])}}});
/* double-tap the cover: left third -10s, right third +10s, middle play/pause */
(function(){let lt=0,lx=0;$("big").addEventListener("click",e=>{const n=Date.now(),r=e.currentTarget.getBoundingClientRect();
  if(n-lt<300&&Math.abs(e.clientX-lx)<60&&S.cur>=0){lt=0;const z=(e.clientX-r.left)/Math.max(1,r.width),t=T[S.cur];let msg;
    if(z<.35){if(!t.radio)seekTo(Math.max(0,S.pos-10));msg="−10s"}else if(z>.65){if(!t.radio)seekTo(Math.min(t.d||1e9,S.pos+10));msg="+10s"}else{toggle();msg=S.playing?"Play":"Pause"}
    buzz(6);const f=document.createElement("div");f.className="flash";f.textContent=msg;$("big").parentNode.appendChild(f);setTimeout(()=>f.remove(),650)}else{lt=n;lx=e.clientX}})})();


