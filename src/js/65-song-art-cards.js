/* cards follow the song: hand the cover to the CSS as --cardart.
   Light path (no blur filter at all): the cover is shrunk once to a 48px bitmap; stretched back up it is already soft.
   Fallback when the browser won't let us read a web cover: blur the real image, but ONLY on the cards you can see. */
(function(){
  /* the cover is shrunk, then blurred by hand on a 64px canvas (works everywhere, no filter support needed);
     stretched back up it is smooth, never blocky. The art behind the logo is NOT touched: that one stays crisp. */
  function soften(x,w){const d=x.getImageData(0,0,w,w),p=d.data,t=new Uint8ClampedArray(p.length);
    for(let pass=0;pass<3;pass++){for(let y=0;y<w;y++)for(let c=0;c<w;c++){let r=0,g=0,b=0,n=0;
      for(let dy=-2;dy<=2;dy++)for(let dx=-2;dx<=2;dx++){const yy=y+dy,xx=c+dx;if(yy<0||xx<0||yy>=w||xx>=w)continue;const i=(yy*w+xx)*4;r+=p[i];g+=p[i+1];b+=p[i+2];n++}
      const o=(y*w+c)*4;t[o]=r/n;t[o+1]=g/n;t[o+2]=b/n;t[o+3]=255}
      p.set(t)}
    x.putImageData(d,0,0)}
  function small(t){if(t.cardFor===t.img)return;t.cardFor=t.img;t.cardSm=null;if(!t.img)return;
    const im=new Image();im.crossOrigin="anonymous";
    im.onload=()=>{try{const a=document.createElement("canvas");a.width=a.height=96;const x=a.getContext("2d");x.imageSmoothingQuality="high";x.drawImage(im,0,0,96,96);
      const b=document.createElement("canvas");b.width=b.height=48;const y=b.getContext("2d");y.imageSmoothingQuality="high";try{y.filter="saturate(1.35)"}catch(e){}y.drawImage(a,0,0,48,48);
      soften(y,48);
      t.cardSm=b.toDataURL("image/png");if(T[S.cur]===t)paint()}catch(e){}};
    im.src=t.img}
  function paint(){try{const a=$("app"),t=T[S.cur];if(!a)return;
    if(!t){a.style.setProperty("--cardart","none");a.classList.remove("cardlive");return}
    small(t);
    if(t.cardSm){a.style.setProperty("--cardart",`url(${t.cardSm})`);a.classList.remove("cardlive")}
    else if(t.img){a.style.setProperty("--cardart",`url("${String(t.img).replace(/"/g,"%22")}")`);a.classList.add("cardlive")}
    else{const c=t.c||"120,110,100";a.style.setProperty("--cardart",`radial-gradient(120% 70% at 18% 0%,rgba(${c},.95),rgba(${c},.38) 58%,rgba(${c},.14))`);a.classList.remove("cardlive")}}catch(e){}}
  const o=glowSet;glowSet=function(t){const r=o.apply(this,arguments);if(T[S.cur]===t)paint();return r};
  /* only cards on screen get the live-blur fallback */
  const io=window.IntersectionObserver?new IntersectionObserver(es=>es.forEach(e=>e.target.classList.toggle("av",e.isIntersecting)),{rootMargin:"60px"}):null;
  const watch=()=>{if(!io)return;document.querySelectorAll("#view .mcard").forEach(c=>{if(!c._w){c._w=1;io.observe(c)}})};
  const _r=render;render=function(){const r=_r.apply(this,arguments);watch();return r};
  watch();paint()})();
