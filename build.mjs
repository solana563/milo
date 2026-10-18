// Builds index.html from src/ (concatenation, in the order listed in src/parts.json).
// Usage: node build.mjs          -> writes index.html (+ milo-tv copies if ../milo-tv exists)
//        node build.mjs --check  -> exits 1 if index.html differs from what src/ produces
import fs from "node:fs";import path from "node:path";import {fileURLToPath} from "node:url";
const root=path.dirname(fileURLToPath(import.meta.url));
const {parts}=JSON.parse(fs.readFileSync(path.join(root,"src/parts.json"),"utf8"));
const out=parts.map(p=>fs.readFileSync(path.join(root,"src",p),"utf8")).join("");
const target=path.join(root,"index.html");
if(process.argv.includes("--check")){const cur=fs.existsSync(target)?fs.readFileSync(target,"utf8"):"";
  if(cur!==out){console.error("index.html is out of date - run: node build.mjs");process.exit(1)}console.log("index.html matches src/");process.exit(0)}
fs.writeFileSync(target,out);console.log("index.html built:",out.length,"bytes from",parts.length,"parts");
for(const t of ["../milo-tv/tizen","../milo-tv/webos"]){const d=path.join(root,t);if(fs.existsSync(d)){fs.writeFileSync(path.join(d,"index.html"),out);console.log("  copied to",t)}}
