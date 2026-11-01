// Copies the built web app (../index.html etc.) into desktop/app so Electron can serve it.
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..", "..");
const out = path.resolve(here, "..", "app");
fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });
for (const f of ["index.html", "manifest.webmanifest", "sw.js"]) fs.copyFileSync(path.join(root, f), path.join(out, f));
for (const d of ["icons", "splash", "screenshots"]) {
  if (fs.existsSync(path.join(root, d))) fs.cpSync(path.join(root, d), path.join(out, d), { recursive: true });
}
console.log("web app copied to", out);
