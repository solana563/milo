/* ---------- TV: play songs straight from a USB stick or drive (or any storage the TV browser can open) ----------
   Nothing is copied: each file is played from where it is. The browser only gives us access for this visit
   (a TV browser cannot remember a USB stick), so pick the folder again next time. */
var USBN=0;
const USBX=/\.(mp3|m4a|aac|flac|wav|ogg|opus|weba|webm|mp4)$/i;
function usbBar(){
  return `<label class="addb">Play from USB / storage<input type="file" data-usb="dir" webkitdirectory multiple></label>`
    +`<label class="addb">Choose songs<input type="file" data-usb="files" accept="audio/*" multiple></label>`
    +(USBN?`<button class="ghost" data-usb-x="1">Remove USB songs (${USBN})</button>`:"")}
async function usbLoad(list){
  const fl=[...list].filter(f=>USBX.test(f.name)||/^audio\//.test(f.type));
  if(!fl.length){toast("No music found there");return}
  toast(`Reading ${fl.length} song${fl.length>1?"s":""}…`);
  const rows=[];let i=0;
  const w=async()=>{while(i<fl.length){const f=fl[i++],key="usb:"+(f.webkitRelativePath||f.name)+":"+f.size;
    if(T.some(t=>t.key===key&&!t.gone))continue;
    try{const r=await readTags(f),nm=fromName(f.name),al=r.al||"";
      rows.push({key,o:{t:r.t||nm.t,a:r.a||r.aa||nm.a||"On this device",al,y:r.y||"",n:r.n||nm.n||0,d:0,c:COLS[hs(al||r.t||nm.t)%6],url:URL.createObjectURL(f),usb:1,img:r.img,base:bn(f.name).toLowerCase(),fname:f.name,lrc:r.lrc,plain:r.plain,lsrc:r.lrc?"Embedded · synced":r.plain?"Embedded":""}});
      if(rows.length%25===0){dvSync(rows.splice(0),1);render()}}catch(e){}}};
  await Promise.all([w(),w(),w(),w()]);
  const n=rows.length;dvSync(rows,1);
  USBN=T.filter(t=>t.usb&&!t.gone).length;DV.on=1;render();if(window.dvDur)dvDur();
  toast(USBN?`${USBN} songs ready from USB`:"No new songs found")}
document.addEventListener("change",e=>{const el=e.target;if(el&&el.matches&&el.matches("input[data-usb]")){const f=el.files;if(f&&f.length)usbLoad(f);el.value=""}});
document.addEventListener("click",e=>{const b=e.target.closest&&e.target.closest("[data-usb-x]");if(!b)return;
  T.forEach(t=>{if(t.usb&&!t.gone){t.gone=true;t.hid=true;try{URL.revokeObjectURL(t.url)}catch(_){}}});USBN=0;toast("USB songs removed");render()});
