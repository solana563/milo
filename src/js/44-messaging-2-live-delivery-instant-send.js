/* ----- messaging 2: live delivery, instant send, ticks, replies, reactions, notification pills ----- */
var RT={ws:null,open:0,live:0,ref:0,hb:0,rc:0,chat:"",typ:{},ts:0,joined:{},stop:0};
SO.tab=null;SO.last={};SO.tn=0;SO.reply=null;SO.act=null;SO.newc=0;SO.view=null;SO.at=0;
const MC_RX=["❤️","😂","👍","🔥","😮","😢"];
const mcTime=d=>d.toLocaleTimeString(LOC(),{hour:"2-digit",minute:"2-digit"});
const mcDay=d=>{const n=new Date(),y=new Date(n.getTime()-864e5);return d.toDateString()===n.toDateString()?"Today":d.toDateString()===y.toDateString()?"Yesterday":d.toLocaleDateString(LOC(),{day:"numeric",month:"short"})};
const mcStamp=iso=>{const d=new Date(iso);return d.toDateString()===new Date().toDateString()?mcTime(d):mcDay(d)};
function mcPrev(m,noyou){if(!m)return"";const pre=m.sender===soUid()&&!noyou?"You: ":"";
  if(m.kind==="img")return pre+"📷 Photo";
  if(m.kind==="voice")return pre+"🎤 Voice message";
  if(m.kind==="song"){const x=/"t":"((?:[^"\\]|\\.)*)/.exec(m.pv||m.body||"");let t="Song";try{if(x)t=JSON.parse('"'+x[1]+'"')}catch(e){t=x?x[1]:"Song"}return pre+"♪ "+t}
  return pre+(m.pv!=null?m.pv:String(m.body||"").slice(0,80))}
const mcTick=m=>m.st==="sending"?'<svg class="tk" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M12 7v5l3 2"/></svg>':m.read_at?'<svg class="tk rd" viewBox="0 0 24 24"><path d="M2 13l4 4 8-9M10 16l1 1 9-10"/></svg>':'<svg class="tk" viewBox="0 0 24 24"><path d="M5 13l4 4 10-10"/></svg>';
