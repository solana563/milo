// Copies the web app into native/www (without the service worker, docs and native folder).
import {cpSync,rmSync,mkdirSync,existsSync} from "node:fs";
import {dirname,join} from "node:path";
import {fileURLToPath} from "node:url";
const here=dirname(fileURLToPath(import.meta.url)),root=join(here,"..",".."),www=join(here,"..","www");
rmSync(www,{recursive:true,force:true});mkdirSync(www,{recursive:true});
for(const f of ["index.html","manifest.webmanifest","icons","splash"]){const s=join(root,f);if(existsSync(s))cpSync(s,join(www,f),{recursive:true})}
console.log("web files copied to native/www");
