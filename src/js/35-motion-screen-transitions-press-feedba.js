/* ---------- motion: screen transitions, press feedback, art swaps ---------- */
(function(){const red=()=>{try{return matchMedia("(prefers-reduced-motion: reduce)").matches}catch(e){return false}};
  const once=(els,cls,ms)=>{if(red())return;els.forEach(e=>{if(!e)return;e.classList.remove(cls);void e.offsetWidth;e.classList.add(cls);setTimeout(()=>e.classList.remove(cls),ms)})};
  const _render=render;render=function(){const k=S.tab+"|"+(S.nav||""),ch=k!==S._vk;S._vk=k;_render.apply(this,arguments);if(ch)once([$("view")],"vin",1300)};
  S._vk=S.tab+"|"+(S.nav||"");
  const _toggle=toggle;toggle=function(){const r=_toggle.apply(this,arguments);once([$("fP"),$("mP"),$("dP"),$("lyP")],"pop",520);return r};
  const _like=toggleLike;toggleLike=function(i){const t=T[i],r=_like.apply(this,arguments);if(t&&S.likes.has(t.key))once([$("fL"),$("imLk"),$("mLk")],"hpop",650);return r};
  const _bg=bgArt;bgArt=function(t){_bg.apply(this,arguments);once([$("big"),$("mArt"),$("imA")],"swap",800)};
})();

;(function(){const o=updCtl;updCtl=function(){const r=o.apply(this,arguments);try{dpUpd()}catch(e){}return r}})();

/* keyboard / visual viewport: keep sheets above the keyboard and stop the page jumping */
(function(){const vv=window.visualViewport,A=()=>document.getElementById("app");let t=0;
function fix(){const a=A();if(!a)return;let kb=0,h=innerHeight;if(vv){h=vv.height;kb=Math.max(0,Math.round(innerHeight-vv.height-vv.offsetTop))}
 const open=kb>80||(vv&&innerHeight-vv.height>120);a.classList.toggle("kb",open);a.style.setProperty("--kb",(open?kb:0)+"px");a.style.setProperty("--vvh",h+"px");
 a.scrollTop=0;a.scrollLeft=0;const se=document.scrollingElement;if(se){se.scrollTop=0;se.scrollLeft=0}["sheet","mdl"].forEach(i=>{const e=document.getElementById(i);if(e){e.scrollTop=0}})}
if(vv){vv.addEventListener("resize",fix);vv.addEventListener("scroll",fix)}
addEventListener("focusin",e=>{if(e.target.matches&&e.target.matches("input,textarea,select")){clearTimeout(t);[60,250,600].forEach(ms=>setTimeout(()=>{fix();try{if(e.target.closest("#view"))e.target.scrollIntoView({block:"center"})}catch(_){}},ms))}});
addEventListener("focusout",()=>setTimeout(fix,200));
})();

/* now playing sheet: fill the extra fields */
var FP='<path d="M8 5v14l11-7z"/>',FPA='<path d="M7 5h4v14H7zM13 5h4v14h-4z"/>';
function fSheet(){const t=T[S.cur],f=$("full");if(!t||!f)return;
 f.classList.toggle("isyt",!!t.yt);
 $("hT").textContent=t.t;$("hS").textContent=t.radio?"Live radio":(t.a||"");
 $("fCirc").style.background=artOf(t);$("fCirc").style.backgroundSize="cover";
 const st=$("fSt");st.textContent=t.radio?"LIVE":S.playing?"Now playing":"Paused";st.classList.toggle("off",!S.playing&&!t.radio);
 const nx=(S.q||[]).slice(S.qi+1,S.qi+4).filter(i=>T[i]),rest=Math.max(0,(S.q||[]).length-S.qi-1-nx.length);
 $("fNx").innerHTML=nx.length?nx.map(i=>`<i style="background:${artOf(T[i])}"></i>`).join("")+(rest?`<em>+${rest}</em>`:""):'<small style="margin:0">Nothing queued</small>';
 $("fSrc").textContent=t.radio?"Radio":t.yt?"YouTube":(t.al||t.a||"Library");
 $("fTm").textContent=t.radio?"Live":$("fC").textContent+" / "+$("fD").textContent;
 const b=$("fBig");b.innerHTML=`<svg viewBox="0 0 24 24">${S.playing?FPA:FP}</svg>${S.playing?"Pause":"Play"}`}
$("fBig").onclick=()=>{if(S.cur>=0)toggle();setTimeout(fSheet,60)};
$("fNx").onclick=()=>$("fQ").click();
setInterval(()=>{if($("full").classList.contains("open"))fSheet()},500);
;(function(){const o=updCtl;updCtl=function(){const r=o.apply(this,arguments);try{fSheet()}catch(e){}return r}})();

$("mMo").innerHTML=DOTS;$("mMo").onclick=e=>{e.stopPropagation();if(S.cur>=0)trackMenu(S.cur)};

/* keep detail-screen chrome out of the way while a full screen is open */
(function(){const a=$("app");const f=()=>{const o=$("full").classList.contains("open")||$("lyr").classList.contains("open")||(typeof IM!=="undefined"&&IM.on)||!$("mdl").hidden;a.classList.toggle("np-open",o)};
["full","lyr","im","mdl"].forEach(i=>{const e=$(i);if(e)new MutationObserver(f).observe(e,{attributes:true,attributeFilter:["class","hidden"]})});f()})();
