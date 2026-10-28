/* cards follow the song: hand the song's colour to the CSS as --cc.
   Light path (no blur filter at all): the cover is shrunk once to a 48px bitmap; stretched back up it is already soft.
   Fallback when the browser won't let us read a web cover: blur the real image, but ONLY on the cards you can see. */
(function(){
  /* cards are a SOLID colour that follows the song (--cc = the song's colour, the CSS mixes it into the card colour).
     The art behind the logo is separate and stays crisp. */
  function paint(){try{const a=$("app"),t=T[S.cur];if(!a)return;a.classList.remove("cardlive");a.style.setProperty("--cardart","none");
    if(!t){a.style.removeProperty("--cc");return}
    a.style.setProperty("--cc",t.c||"120,110,100")}catch(e){}}
  const o=glowSet;glowSet=function(t){const r=o.apply(this,arguments);if(T[S.cur]===t)paint();return r};
  /* only cards on screen get the live-blur fallback */
  const io=window.IntersectionObserver?new IntersectionObserver(es=>es.forEach(e=>e.target.classList.toggle("av",e.isIntersecting)),{rootMargin:"60px"}):null;
  const watch=()=>{if(!io)return;document.querySelectorAll("#view .mcard").forEach(c=>{if(!c._w){c._w=1;io.observe(c)}})};
  const _r=render;render=function(){const r=_r.apply(this,arguments);watch();return r};
  watch();paint()})();
