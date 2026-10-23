
const T=[
{t:"Low Tide",a:"Wren Sable",d:212,c:"84,120,140",n:[110,164.8,196,246.9]},
{t:"Amber Rooms",a:"Coast Choir",d:221,c:"201,151,90",n:[130.8,196,261.6,329.6]},
{t:"Half Light",a:"Junio",d:187,c:"150,100,130",n:[98,147,185,220]},
{t:"Salt & Static",a:"Wren Sable",d:198,c:"110,140,110",n:[123.5,185,220,277.2]},
{t:"Paper Moons",a:"Coast Choir",d:243,c:"170,110,90",n:[87.3,130.8,174.6,220]},
{t:"Slow Bloom",a:"Junio",d:205,c:"120,110,160",n:[116.5,174.6,233,293.7]}];
const LY=[
["The water pulls back from the pier","and leaves the shells to dry","I carry your name in my pocket","like a stone I found by the sea","nothing here is in a hurry","the light is going soft","stay till the tide turns over","and brings us back to shore"],
["Every window holds a lamp","every lamp a different year","I walk the hall in borrowed shoes","and the floorboards know my name","we were gold for a little while","in the rooms that smelled like rain","leave the door open a crack","I'll find my way to the light"],
["Halfway between the night and day","the streets forget their names","a bus goes by with no one on","and every sign says wait","I'm learning how to be unsure","and not be afraid of it","the sky is turning slowly","the color of your coat"],
["Radio hums in the kitchen","salt on the window glass","a song comes in half broken","I sing the parts I know","the static keeps the rhythm","when the signal fades away","turn it up a little louder","we'll fill in the rest ourselves"],
["We cut the moon from paper","and hung it on a string","it swung above the table","like it had somewhere to be","nobody said the ending","we just let the candle burn","if the morning finds us sleeping","let the paper moon stay up"],
["Nothing opens all at once","the garden takes its time","a green thing pushes through the dark","and doesn't ask for proof","I used to count the hours","now I just watch the light","be patient with the small things","they're the ones that bloom"]];
const lines=i=>T[i].lrc?T[i].lrc.map(x=>x[1]):(T[i].plain||LY[i]||[]);
const lt=(i,k)=>T[i].lrc?T[i].lrc[k][0]:10+k*(Math.max(T[i].d,60)-20)/lines(i).length;
const META=[["Saltwater Hours",2024,"Ambient"],["Coast Hours",2023,"Folk"],["Half Light",2025,"Indie"],["Saltwater Hours",2024,"Ambient"],["Coast Hours",2023,"Folk"],["Half Light",2025,"Indie"]];
T.forEach((t,i)=>{[t.al,t.y,t.g]=META[i];t.key="demo:"+t.t;t.demo=true;t.hid=true});
const $=id=>document.getElementById(id);
const esc=x=>String(x??"").replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));
const art=c=>`linear-gradient(145deg,rgba(${c},1),rgba(${c},.35) 60%,#1a1714)`;
const artOf=t=>t.img&&!(S.lite&&/^https?:/.test(t.img))?`center/cover url(${t.img})${t.rimg?","+art(t.c):""}`:t.logo?`center/58% no-repeat url(${t.logo}),${art(t.c)}`:art(t.c);
/* r,g,b (0-255) <-> h,s,l (0-1) */
const rgb2hsl=(r,g,b)=>{r/=255;g/=255;b/=255;const mx=Math.max(r,g,b),mn=Math.min(r,g,b),l=(mx+mn)/2,d=mx-mn;if(!d)return[0,0,l];const s=l>.5?d/(2-mx-mn):d/(mx+mn);let h=mx===r?(g-b)/d+(g<b?6:0):mx===g?(b-r)/d+2:(r-g)/d+4;return[h/6,s,l]};
const hsl2rgb=(h,s,l)=>{if(!s){const v=Math.round(l*255);return[v,v,v]}const q=l<.5?l*(1+s):l+s-l*s,p=2*l-q,f=t=>{t=(t+1)%1;return t<1/6?p+(q-p)*6*t:t<.5?q:t<2/3?p+(q-p)*(2/3-t)*6:p};return[f(h+1/3),f(h),f(h-1/3)].map(v=>Math.round(v*255))};
/* UI accent from the song's colour: SAME hue and saturation as the art, only the lightness is nudged so it stays readable (no more washing it out toward white) */
const acc=c=>{const [r,g,b]=String(c).split(",").map(Number),[h,s,l]=rgb2hsl(r,g,b);if(s<.1)return"226,226,226";return hsl2rgb(h,Math.min(1,s*1.04),Math.min(.7,Math.max(.56,l))).join(",")};
const fmt=s=>!isFinite(s)?"0:00":Math.floor(s/60)+":"+String(Math.floor(s%60)).padStart(2,"0");
let PLAY='<svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>',PAUSE='<svg viewBox="0 0 24 24"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>';
const svg=p=>`<svg viewBox="0 0 24 24">${p}</svg>`;
const I={shuf:'<path d="M3 7h3c4 0 5 10 9 10h4M3 17h3c1.5 0 2.5-1 3.5-2.5M14 9c.8-1 1.8-2 3.5-2H19M17 4l3 3-3 3M17 14l3 3-3 3"/>',
rep:'<path d="M17 3l3 3-3 3M4 11V9a3 3 0 013-3h13M7 21l-3-3 3-3M20 13v2a3 3 0 01-3 3H4"/>',
heart:'<path d="M12 20s-7-4.4-7-10a4 4 0 017-2.6A4 4 0 0119 10c0 5.6-7 10-7 10z"/>',
queue:'<path d="M4 6h12M4 12h12M4 18h8M18 14v6l4-3z"/>'};
const DOTS='<svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="19" cy="12" r="1.8"/></svg>';
const ICON={
Home:'<path d="M4 11l8-7 8 7M6 10v10h12V10"/>',
Search:'<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
Discover:'<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',
Library:'<path d="M6 4h12v17l-6-4-6 4z"/>',
Profile:'<circle cx="12" cy="8" r="4"/><path d="M4 21c1-4 4-6 8-6s7 2 8 6"/>'};
const EQF=[32,64,125,250,500,1000,2000,4000,8000,16000];
const PRESETS={Flat:{g:[0,0,0,0,0,0,0,0,0,0],wide:1,comp:0},
"Hi-Fi":{g:[2,1.5,.5,0,-.5,0,.5,1.5,2.5,3],wide:1.15,comp:0},
"Lo-Fi":{g:[1,2,2.5,2,0,-1,-3,-6,-10,-14],wide:.4,comp:1},
Dolby:{g:[2,2,1,0,0,1,2.5,3,2,1.5],wide:1.7,comp:1},
IMAX:{g:[7,6,3,0,-1,0,1.5,3,4,5],wide:2,comp:1},
"Bass boost":{g:[8,7,5,2,0,0,0,0,0,0],wide:1,comp:0},
"Treble boost":{g:[0,0,0,0,0,1,3,5,7,8],wide:1,comp:0},
Vocal:{g:[-3,-2,-1,1,3,4,3,2,0,-1],wide:1,comp:0},
Acoustic:{g:[3,3,2,1,1,1,2,2,3,2],wide:1.1,comp:0},
Electronic:{g:[5,4,1,0,-2,2,1,3,4,5],wide:1.3,comp:0},
"Hip-hop":{g:[6,5,2,3,-1,-1,1,0,2,3],wide:1,comp:1},
Rock:{g:[4,3,2,0,-1,-1,1,3,4,4],wide:1.2,comp:0},
Jazz:{g:[3,2,1,2,-1,-1,0,1,2,3],wide:1.1,comp:0},
Classical:{g:[2,1,0,0,0,0,0,1,2,3],wide:1.2,comp:0},
Night:{g:[-2,-1,0,1,2,2,1,0,-1,-2],wide:1,comp:1}};
const RA=new Audio(),RB=new Audio(),DK=[new Audio(),new Audio()];let A=DK[0];RB.crossOrigin="anonymous";RB.preload="none";
[...DK,RA].forEach(e=>{e.preload=e===RA?"none":"auto";e.onended=()=>{if(e===A)next(true)};e.onerror=()=>{if(e===A&&e.getAttribute("src")){toast("Couldn't play this file here");const t=T[S.cur];if(S.q.length>1&&t&&!t.radio&&(S.errc=(S.errc||0)+1)<=3)setTimeout(()=>{if(e===A)next(false,1)},500)}};e.onplaying=()=>{if(e===A)S.errc=0}});
RB.onended=()=>{if(A===RB)next(true)};RB.onplaying=()=>{if(A===RB)S.errc=0};
RB.onerror=()=>{if(A!==RB||!RB.getAttribute("src"))return;const t=T[S.cur];if(t&&t.url&&(t.stream||t.radio)&&!t.nocors){t.nocors=true;RB.removeAttribute("src");try{RB.load()}catch(e){}engine(S.cur)}else toast("Couldn't play this file here")};
const S={th:"dark",tab:"Home",lib:"Songs",nav:null,sq:"",cur:-1,playing:false,pos:0,q:[],qi:0,base:[],shuf:false,rep:0,rate:1,sleep:null,eq:{n:"Flat",g:PRESETS.Flat.g.slice(),wide:1,comp:0},eqOn:true,hc:false,ps:"milo",xf:0,gap:true,dk:0,pre:null,xfb:0,demoOn:[],likes:new Set(),rfav:[],rs:[],sfav:[],amb:1,mres:[],dm:"music",ms:"yt",mg:"Trending",pls:[],hist:[],lists:{},plv:null,ctx:null,gain:null,oscs:[]};
const LS={get(k,d){try{const v=JSON.parse(localStorage.getItem(k));return v==null?d:v}catch(e){return d}},set(k,v){try{localStorage.setItem(k,JSON.stringify(v))}catch(e){}}};
S.accts=LS.get("milo.accts",[]);S.guest=LS.get("milo.guest",{name:"Guest",av:{t:"color",c:"249,156,0"}});S.acct=S.accts.find(a=>a.id===LS.get("milo.cur",""))||null;
const dkey=()=>S.acct?"milo.v2."+S.acct.id:"milo.v2";
function save(){const o={look3:1,pls:S.pls,likes:[...S.likes],rfav:S.rfav,sfav:S.sfav,amb:S.amb,ps:S.ps,viz:S.viz,idle:S.idle,plays:S.plays,hist:S.hist,stat:S.stat,auto:S.auto,miNet:S.miNet,sb:S.sb,hc:S.hc,eq:S.eq,eqOn:S.eqOn,xf:S.xf,gap:S.gap,demoOn:S.demoOn,shuf:S.shuf,rep:S.rep,rate:S.rate,th:S.th};LS.set(dkey(),o);if(typeof sbPushSoon==="function")sbPushSoon(o)}
function loadData(){const d=LS.get(dkey(),{});S.pls=d.pls||[];S.likes=new Set(d.likes||[]);S.rfav=d.rfav||[];S.sfav=(d.sfav||[]).filter(x=>!(x&&/^(au|cc|ia)/.test(String(x.id))));S.amb=d.amb==null?1:d.amb;S.ps="spotify";S.viz=d.viz!==false;S.idle=d.idle!==false;S.plays=d.plays||{};S.hist=d.hist||[];S.stat=d.stat||{};S.auto=d.auto!==false;S.miNet=d.miNet!==false;S.sb=d.sb!==false;S.hc=d.hc==null?(()=>{try{return matchMedia("(prefers-contrast: more)").matches}catch(e){return false}})():!!d.hc;S.eq=d.eq||{n:"Flat",g:PRESETS.Flat.g.slice(),wide:1,comp:0};S.eqOn=d.eqOn!==false;S.xf=d.xf||0;S.gap=d.gap!==false;S.demoOn=d.demoOn||[];S.shuf=false;S.rep=d.rep||0;S.rate=d.rate||1;S.th=d.look3?(d.th||"dark"):"dark";
  T.forEach(t=>{if(t.demo)t.hid=!S.demoOn.includes(t.key)})}
loadData();
function toast(m){const e=$("toast");e.textContent=m;e.style.opacity=1;clearTimeout(S.tt);S.tt=setTimeout(()=>e.style.opacity=0,3200)}
const MUSIC="audio/*,.mp3,.m4a,.aac,.flac,.wav,.ogg,.opus,.weba";
const addBtn=(t,a,k)=>`<label class="addb">${t}<input type="file" data-pick="${k}" ${a?`accept="${a}"`:""} multiple></label>`;

