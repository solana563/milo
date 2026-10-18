/* ----- start-up ----- */
(function boot(){const need=gateNeeded();
  if(need){if(S.cloud&&need!=="up"){profLoad().then(p=>{if(p){cloudAdopt(p);const n=gateNeeded();if(n){GATE.step=n;gateRender()}else soStart()}else{GATE.step="claim";gateRender()}}).catch(()=>{if(S.acct){GATE.step=termsOk()?"":"terms";if(GATE.step)gateRender()}})}else{GATE.step="up";gateRender()}}
  else{profLoad().then(()=>{soStart();setTimeout(()=>{plSig="";plPush()},3000)})}})();


