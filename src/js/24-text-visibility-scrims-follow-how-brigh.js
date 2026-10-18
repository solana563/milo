/* ---------- text visibility: scrims follow how bright the song's art is, plus a high-contrast option ---------- */
function lumOf(t){if(t.lum!=null)return t.lum;const [r,g,b]=String(t.c||"84,120,140").split(",").map(Number);return(.2126*r+.7152*g+.0722*b)/255}
function applyWash(){const ap=$("app");if(!ap)return;const t=T[S.cur],L=t?lumOf(t):.4,dark=ap.dataset.theme==="dark",hc=S.hc?.12:0;
  ap.style.setProperty("--wa",Math.min(.94,(dark?.5+.36*L:.5+.36*(1-L))+hc).toFixed(2));
  ap.style.setProperty("--lum",Math.min(1,L+(S.hc?.15:0)).toFixed(2));ap.dataset.hc=S.hc?"1":"0"}
const COLS=["84,120,140","150,100,130","110,140,110","170,110,90","120,110,160","201,151,90"];
const bn=n=>n.replace(/\.[^.]+$/,"");
const parseLrc=t=>{const o=[];t.split(/\r?\n/).forEach(l=>{const m=[...l.matchAll(/\[(\d+):(\d+(?:\.\d+)?)\]/g)],x=l.replace(/\[[^\]]*\]/g,"").trim();if(x)m.forEach(a=>o.push([+a[1]*60+ +a[2],x]))});return o.sort((a,b)=>a[0]-b[0])};
const u8=async(f,a,b)=>new Uint8Array(await f.slice(a,b).arrayBuffer());
const be=(d,o)=>((d[o]<<24)|(d[o+1]<<16)|(d[o+2]<<8)|d[o+3])>>>0;
const le=(d,o)=>((d[o+3]<<24)|(d[o+2]<<16)|(d[o+1]<<8)|d[o])>>>0;
const ss=(d,o)=>(d[o]<<21)|(d[o+1]<<14)|(d[o+2]<<7)|d[o+3];
const asc=(d,a,b)=>String.fromCharCode(...d.subarray(a,b));
function dec(d,e){try{const l=e===3?"utf-8":e===0?"windows-1252":((d[0]===254&&d[1]===255)||e===2)?"utf-16be":"utf-16le";return new TextDecoder(l).decode(d).replace(/^\uFEFF/,"")}catch(x){return""}}
const clean=x=>(x||"").split("\0").map(v=>v.trim()).filter(Boolean).join(", ");
const zt=(d,q,e)=>{if(e===1||e===2){while(q+1<d.length&&!(d[q]===0&&d[q+1]===0))q+=2;return q+2}while(q<d.length&&d[q])q++;return q+1};
const uns=d=>{const o=[];for(let i=0;i<d.length;i++){o.push(d[i]);if(d[i]===255&&d[i+1]===0)i++}return Uint8Array.from(o)};
const find=(d,s)=>{const n=s.length;outer:for(let i=0;i<d.length-n;i++){for(let j=0;j<n;j++)if(d[i+j]!==s.charCodeAt(j))continue outer;return i}return -1};
function img(d,mime){if(!d||d.length<8)return;const m=d[0]===137&&d[1]===80?"image/png":d[0]===255&&d[1]===216?"image/jpeg":(mime&&mime.includes("/")?mime:"image/jpeg");const b=new Blob([d],{type:m}),u=URL.createObjectURL(b);IMGB[u]=b;return u}
function lyr(text,r){text=(text||"").trim();if(!text)return;
  if(/\[\d+:\d+/.test(text)){const l=parseLrc(text);if(l.length){r.lrc=l;return}}
  const ls=text.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);if(ls.length)r.plain=ls}
const yr=x=>(String(x).match(/\d{4}/)||[""])[0];
async function id3(f,r){
  const h=await u8(f,0,10);if(asc(h,0,3)!=="ID3")return;
  const v=h[3],fl=h[5],sz=ss(h,6);let b=await u8(f,10,10+Math.min(sz,12e6));
  if(fl&128&&v<4)b=uns(b);
  let p=0;if(fl&64)p=v===4?ss(b,0):be(b,0)+4;
  const idl=v===2?3:4,hl=v===2?6:10;let best=0;
  while(p+hl<=b.length&&b[p]){
    const id=asc(b,p,p+idl);
    const n=v===2?(b[p+3]<<16)|(b[p+4]<<8)|b[p+5]:v===4?ss(b,p+4):be(b,p+4);
    const fg=v>2?b[p+9]:0,s0=p+hl;p=s0+n;if(n<=0)break;
    let d=b.subarray(s0,Math.min(p,b.length));
    if(v===4){if(fg&1)d=d.subarray(4);if(fg&2)d=uns(d)}
    const e=d[0],TX=()=>clean(dec(d.subarray(1),e));
    if(id==="TIT2"||id==="TT2")r.t=TX();
    else if(id==="TPE1"||id==="TP1")r.a=TX();
    else if(id==="TALB"||id==="TAL")r.al=TX();
    else if(id==="TPE2"||id==="TP2")r.aa=TX();
    else if(id==="TDRC"||id==="TYER"||id==="TYE"){r.y=r.y||yr(TX())}
    else if(id==="TRCK"||id==="TRK")r.n=parseInt(TX())||0;
    else if(id==="TCON"||id==="TCO"){const g=TX().replace(/\(\d+\)/g,"").trim();if(g&&!/^\d+$/.test(g))r.g=g}
    else if(id==="USLT"||id==="ULT"){if(!r.lrc&&!r.plain)lyr(dec(d.subarray(zt(d,4,e)),e),r)}
    else if(id==="SYLT"||id==="SLT"){
      if(d[4]===2){let q=zt(d,6,e);const out=[];
        while(q<d.length){const st=q;q=zt(d,q,e);const tx=dec(d.subarray(st,q),e).replace(/\0/g,"").trim();if(q+4>d.length)break;const tm=be(d,q);q+=4;if(tx)out.push([tm/1000,tx])}
        if(out.length)r.lrc=out}}
    else if(id==="APIC"||id==="PIC"){
      let q,mime,ty;
      if(id==="PIC"){mime="image/"+asc(d,1,4).toLowerCase();ty=d[4];q=zt(d,5,e)}
      else{let z=1;while(d[z])z++;mime=asc(d,1,z);ty=d[z+1];q=zt(d,z+2,e)}
      const sc=ty===3?2:1;if(sc>best){const u=img(d.subarray(q),mime);if(u){best=sc;r.img=u}}}
  }
}
async function id3v1(f,r){if(f.size<128)return;const d=await u8(f,f.size-128,f.size);if(asc(d,0,3)!=="TAG")return;
  const g=(a,b)=>dec(d.subarray(a,b),0).replace(/\0[\s\S]*$/,"").trim();
  r.t=r.t||g(3,33);r.a=r.a||g(33,63);r.al=r.al||g(63,93);r.y=r.y||yr(g(93,97));if(!r.n&&d[125]===0&&d[126])r.n=d[126]}
function flacPic(d,r){const ml=be(d,4),mime=asc(d,8,8+ml);let o=8+ml;o+=4+be(d,o);o+=16;const len=be(d,o);o+=4;if(!r.img)r.img=img(d.subarray(o,o+len),mime)}
function vc(d,o,r){o+=4+le(d,o);const c=le(d,o);o+=4;
  for(let i=0;i<c&&o+4<=d.length;i++){const l=le(d,o);o+=4;const s=dec(d.subarray(o,o+l),3);o+=l;const k=s.indexOf("=");if(k<1)continue;
    const K=s.slice(0,k).toUpperCase(),V=s.slice(k+1).trim();
    if(K==="TITLE")r.t=V;else if(K==="ARTIST")r.a=r.a?r.a+", "+V:V;else if(K==="ALBUM")r.al=V;else if(K==="ALBUMARTIST")r.aa=V;
    else if(K==="DATE"||K==="YEAR")r.y=r.y||yr(V);else if(K==="TRACKNUMBER")r.n=parseInt(V)||0;else if(K==="GENRE")r.g=r.g?r.g+", "+V:V;
    else if(K==="LYRICS"||K==="UNSYNCEDLYRICS"||K==="SYNCEDLYRICS")lyr(V,r);
    else if(K==="METADATA_BLOCK_PICTURE"&&!r.img){try{flacPic(Uint8Array.from(atob(V),c=>c.charCodeAt(0)),r)}catch(e){}}}}
async function flac(f,r){let o=4;for(let k=0;k<64;k++){const h=await u8(f,o,o+4);if(h.length<4)break;const last=h[0]&128,ty=h[0]&127,len=(h[1]<<16)|(h[2]<<8)|h[3];
  if((ty===4||ty===6)&&len<16e6){const d=await u8(f,o+4,o+4+len);if(ty===4)vc(d,0,r);else flacPic(d,r)}
  o+=4+len;if(last)break}}
async function ogg(f,r){const b=await u8(f,0,Math.min(f.size,6e6));const parts=[];let o=0,tot=0;
  while(o+27<b.length&&asc(b,o,o+4)==="OggS"){const ns=b[o+26];let len=0;for(let i=0;i<ns;i++)len+=b[o+27+i];const s=o+27+ns;parts.push(b.subarray(s,s+len));tot+=len;o=s+len}
  const c=new Uint8Array(tot);let q=0;parts.forEach(x=>{c.set(x,q);q+=x.length});
  let i=find(c,"OpusTags");if(i>=0){vc(c,i+8,r);return}i=find(c,"\x03vorbis");if(i>=0)vc(c,i+7,r)}
function item(d,c,x){while(c+8<=x){const sz=be(d,c);if(sz<16)break;if(asc(d,c+4,c+8)==="data"){const p=d.subarray(c+16,c+sz),f=be(d,c+8)&255;return{d:p,f,s:f===1?dec(p,3):""}}c+=sz}}
function walk(d,a,e,r,il){while(a+8<=e){const sz=be(d,a);if(sz<8||a+sz>e)break;const ty=asc(d,a+4,a+8),c=a+8,x=a+sz;
  if(il){const m=item(d,c,x);if(m){
    if(ty==="\xA9nam")r.t=m.s;else if(ty==="\xA9ART")r.a=m.s;else if(ty==="\xA9alb")r.al=m.s;else if(ty==="aART")r.aa=m.s;
    else if(ty==="\xA9day")r.y=yr(m.s);else if(ty==="\xA9gen")r.g=m.s;else if(ty==="\xA9lyr")lyr(m.s,r);
    else if(ty==="trkn")r.n=((m.d[2]<<8)|m.d[3])||0;else if(ty==="covr"&&!r.img)r.img=img(m.d,m.f===14?"image/png":"image/jpeg")}}
  else if(ty==="moov"||ty==="udta")walk(d,c,x,r,false);else if(ty==="meta")walk(d,c+4,x,r,false);else if(ty==="ilst")walk(d,c,x,r,true);
  a=x}}
async function mp4(f,r){let o=0;while(o<f.size){const h=await u8(f,o,o+16);let sz=be(h,0),hs=8;const ty=asc(h,4,8);
  if(sz===1){sz=be(h,8)*4294967296+be(h,12);hs=16}else if(sz===0)sz=f.size-o;if(sz<8)break;
  if(ty==="moov"&&sz<32e6){const d=await u8(f,o+hs,o+sz);walk(d,0,d.length,r,false);return}o+=sz}}
async function readTags(f){const r={};try{const h=await u8(f,0,12),m=asc(h,0,4);
  if(m==="fLaC")await flac(f,r);else if(m==="OggS")await ogg(f,r);else if(asc(h,4,8)==="ftyp")await mp4(f,r);else{await id3(f,r);await id3v1(f,r)}}catch(e){}return r}
function fromName(n){let s=bn(n).replace(/_/g," ").replace(/\s+/g," ").trim(),tn=0;const m=s.match(/^(\d{1,3})\s*[-.)]\s*(.+)$/);if(m){tn=+m[1];s=m[2]}
  const p=s.split(/\s+-\s+/);return p.length>1?{t:p.slice(1).join(" - "),a:p[0],n:tn}:{t:s,n:tn}}

S.lrc={};
const hs=s=>[...s].reduce((a,c)=>(a*31+c.charCodeAt(0))>>>0,7);
async function addFiles0(fl){
  for(const f of fl.filter(f=>/\.lrc$/i.test(f.name))){try{const bk=bn(f.name).toLowerCase(),ln=parseLrc(await f.text());S.lrc[bk]=ln;idbLrc(bk,ln)}catch(e){}}
  const au=fl.filter(f=>!/\.lrc$/i.test(f.name)&&(f.type.startsWith("audio/")||/\.(mp3|m4a|aac|flac|wav|ogg|opus|weba|webm|mp4)$/i.test(f.name)));
  const items=[];
  for(const f of au){const key=f.name+f.size;if(T.some(t=>t.key===key&&!t.gone))continue;
    const r=await readTags(f),nm=fromName(f.name);
    items.push({f,key,r,o:{t:r.t||nm.t,a:r.a||r.aa||nm.a||"On this device",al:r.al||"",y:r.y||"",g:r.g||"",n:r.n||nm.n||0}})}
  items.sort((x,y)=>(x.o.al||"\uFFFF").localeCompare(y.o.al||"\uFFFF")||(x.o.n-y.o.n)||x.f.name.localeCompare(y.f.name,undefined,{numeric:true}));
  for(const {f,key,r,o} of items){
    const t={...o,d:0,c:COLS[hs(o.al||o.t)%6],url:URL.createObjectURL(f),img:r.img,key,base:bn(f.name).toLowerCase(),lrc:r.lrc,plain:r.plain,fname:bn(f.name),lsrc:r.lrc?"Embedded · synced":r.plain?"Embedded":""};
    t.file=f;t.added=Date.now();T.push(t);idbPut(t);const idx=T.length-1,a=new Audio();a.preload="metadata";a.src=t.url;
    a.onloadedmetadata=()=>{if(isFinite(a.duration)){t.d=a.duration;const e=document.querySelector('.li[data-i="'+idx+'"] em');if(e)e.textContent=fmt(t.d);saveSoon(t)}};
  }
  T.forEach(t=>{if(t.url&&S.lrc[t.base]){t.lrc=S.lrc[t.base];t.lsrc=".lrc file";saveSoon(t)}});
  if(T[S.cur])lyrRender();
  render();
}
async function addFiles(fl){const n0=T.length;try{await addFiles0(fl)}catch(e){toast("Couldn't read those files");return}
  const n=T.length-n0;toast(n?`Added ${n} song${n>1?"s":""}`:fl.some(f=>/\.lrc$/i.test(f.name))?"Lyrics added":"No new audio files found")}
