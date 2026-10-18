/* ===== web+milo: links (protocol handler): web+milo:search/<words>, web+milo:library, web+milo:discover ... ===== */
(function(){const u=new URLSearchParams(location.search).get("uri");if(!u)return;
  let s=u;try{s=decodeURIComponent(u)}catch(e){}s=s.replace(/^web\+milo:\/*/i,"");const a=s.split("/")[0].toLowerCase(),q=s.split("/").slice(1).join("/");
  const tabs={home:"Home",search:"Search",discover:"Discover",library:"Library",profile:"Profile"};
  setTimeout(()=>{try{const t=tabs[a];if(t){go(t);if(t==="Search"&&q){S.sq=q;render()}}}catch(e){}try{history.replaceState(null,"",location.pathname)}catch(e){}},1500)})();
