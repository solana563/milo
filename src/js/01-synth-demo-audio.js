/* ---------- synth demo audio ---------- */
function audioStart(i){audioStop();try{const C=ctxGet();if(!C)return;if(C.state==="suspended")C.resume();
  const g=C.createGain();g.gain.value=0;g.connect(S.eqIn);g.gain.linearRampToValueAtTime(.12,C.currentTime+1.5);
  T[i].n.forEach((f,k)=>{const o=C.createOscillator(),lfo=C.createOscillator(),lg=C.createGain();o.type=k%2?"triangle":"sine";o.frequency.value=f;lfo.frequency.value=.15+k*.07;lg.gain.value=3;lfo.connect(lg);lg.connect(o.detune);o.connect(g);o.start();lfo.start();S.oscs.push(o,lfo)});S.gain=g}catch(e){}}
function audioStop(){try{if(S.gain){const g=S.gain,o=S.oscs;g.gain.cancelScheduledValues(S.ctx.currentTime);g.gain.linearRampToValueAtTime(0,S.ctx.currentTime+.25);setTimeout(()=>o.forEach(x=>{try{x.stop()}catch(e){}}),300)}}catch(e){}S.gain=null;S.oscs=[]}

