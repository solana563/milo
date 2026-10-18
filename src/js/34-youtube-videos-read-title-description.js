/* ---------- YouTube videos: read title, description and tags to find lyrics ---------- */
async function ytInfo(t){if(!t||!t.yt||t.ytInfo)return;t.ytInfo=1;
  try{const d=await jget(`https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=${t.yt}&key=${YTK}`),v=(d.items||[])[0];if(!v)return;
    const sn=v.snippet||{};t.tags=(sn.tags||[]).slice(0,30);t.ytc=sn.channelTitle||"";
    const m=String(sn.description||"").match(/Provided to YouTube by[^\n]*\n+\s*([^\n·]+?)\s*·\s*([^\n]+)/);
    if(m){t.lt=m[1].trim();t.la=m[2].split("·")[0].trim()}
    const dm=((v.contentDetails&&v.contentDetails.duration)||"").match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
    if(dm){const s=(+dm[1]||0)*3600+(+dm[2]||0)*60+(+dm[3]||0);if(s&&!t.d)t.d=s}
  }catch(e){t.ytInfo=0}rydGet(t)}
function ytVariants(t){const raw=lclean(t.t);
  if(t.lt&&t.la)return{t:lclean(t.lt),a:lclean(t.la),base:raw};
  const p=raw.split(/\s+[-–—|:]\s+/);
  if(p.length>1)return{t:p.slice(1).join(" "),a:p[0],base:raw};
  return{t:raw,a:lclean(t.a||""),base:raw}}

