/* ---------- player styles: milo., Apple Music, Spotify, YouTube Music ---------- */
const SK={p:'<path d="M6 5h2v14H6zM20 5v14L9 12z"/>',n:'<path d="M16 5h2v14h-2zM4 5v14l11-7z"/>'};
const PSTY={
 milo:{prev:SK.p,next:SK.n,play:'<path d="M8 5v14l11-7z"/>',pause:'<path d="M6 5h4v14H6zM14 5h4v14h-4z"/>'},
 apple:{prev:'<path d="M11 5.2v13.6c0 .7-.8 1.1-1.3.7L2.6 12.9c-.4-.3-.4-.9 0-1.2l7.1-6.6c.5-.4 1.3-.1 1.3.6zM21.4 5.2v13.6c0 .7-.8 1.1-1.3.7L13 12.9c-.4-.3-.4-.9 0-1.2l7.1-6.6c.5-.4 1.3-.1 1.3.6z"/>',next:'<path d="M13 5.2v13.6c0 .7.8 1.1 1.3.7l7.1-6.6c.4-.3.4-.9 0-1.2l-7.1-6.6c-.5-.4-1.3-.1-1.3.6zM2.6 5.2v13.6c0 .7.8 1.1 1.3.7L11 12.9c.4-.3.4-.9 0-1.2L3.900 5.100c-.5-.4-1.300-.1-1.300.6z"/>',
  play:'<path d="M7 4.9v14.2c0 .8.9 1.3 1.6.8l11-7.100c.6-.4.6-1.200 0-1.600l-11-7.100C7.900 3.600 7 4.100 7 4.900z"/>',pause:'<rect x="5.5" y="4" width="4.6" height="16" rx="1.6"/><rect x="13.9" y="4" width="4.6" height="16" rx="1.6"/>'},
 spotify:{prev:SK.p,next:SK.n,play:'<path d="M8 5.1v13.8c0 .8.9 1.3 1.6.9l10.6-6.9c.6-.4.6-1.3 0-1.7L9.6 4.2C8.9 3.8 8 4.3 8 5.1z"/>',pause:'<rect x="6" y="5" width="4" height="14" rx="1.2"/><rect x="14" y="5" width="4" height="14" rx="1.2"/>'},
 ytm:{prev:SK.p,next:SK.n,play:'<path d="M8 5v14l11-7z"/>',pause:'<path d="M6 5h4v14H6zM14 5h4v14h-4z"/>'}};
const PNAMES=[["milo","milo."],["apple","Apple Music"],["spotify","Spotify"],["ytm","YouTube Music"]];
function setStyle(){S.ps="spotify";const st=PSTY.spotify;$("app").dataset.ps="spotify";
  PLAY=`<svg viewBox="0 0 24 24">${st.play}</svg>`;PAUSE=`<svg viewBox="0 0 24 24">${st.pause}</svg>`;
  const pv=`<svg viewBox="0 0 24 24">${st.prev}</svg>`,nx=`<svg viewBox="0 0 24 24">${st.next}</svg>`;
  ["prev","dprev","imPv"].forEach(i=>$(i).innerHTML=pv);["next","dnext","imNx"].forEach(i=>$(i).innerHTML=nx);
  ["mP","fP","lyP","dP"].forEach(i=>{$(i).innerHTML=S.playing?PAUSE:PLAY})}
const styleSeg=(attr)=>PNAMES.map(([k,n])=>`<button ${attr}="${k}" class="${S.ps===k?"on":""}">${n}</button>`).join("");

