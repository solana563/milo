const { app, BrowserWindow, Menu, Tray, shell, protocol, net, ipcMain, nativeImage, dialog } = require("electron");
const path = require("path");
const fs = require("fs");
const { pathToFileURL } = require("url");
const discord = require("./discord");

// Serve the app from app://milo so it is a secure context with a stable origin
// (service worker, localStorage, IndexedDB and media APIs all work, and data survives updates).
protocol.registerSchemesAsPrivileged([
  { scheme: "app", privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true, bypassCSP: false } },
  // milofile://f/<base64url path>/<name>: streams songs straight from linked folders (seekable, no copying)
  // milostream://s/<base64url googlevideo url>?ua=<base64url user agent>: YouTube audio, fetched in bounded pieces
  { scheme: "milostream", privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true } },
  { scheme: "milofile", privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true } },
]);

const APP_DIR = path.join(__dirname, "app");
const ICON = path.join(__dirname, "build", "icon.png");
const stateFile = () => path.join(app.getPath("userData"), "window.json");
let win = null, tray = null, quitting = false;

/* ---------------- settings: linked music folders + data folder ---------------- */
const cfgFile = () => path.join(app.getPath("userData"), "desktop.json");
let cfg = null;
function loadCfg() {
  if (cfg) return cfg;
  try { cfg = JSON.parse(fs.readFileSync(cfgFile(), "utf8")); } catch { cfg = {}; }
  cfg.folders = Array.isArray(cfg.folders) ? cfg.folders : [];
  if (!cfg.dataDir) cfg.dataDir = path.join(app.getPath("documents"), "milo");
  return cfg;
}
function saveCfg() { try { fs.writeFileSync(cfgFile(), JSON.stringify(cfg)); } catch {} }
const AUDIO = /\.(mp3|m4a|aac|flac|wav|ogg|opus|weba)$/i;
const sameP = (a, b) => process.platform === "win32" ? a.toLowerCase() === b.toLowerCase() : a === b;
const offDir = () => path.join(app.getPath("userData"), "offline");
const offIdx = () => path.join(offDir(), "index.json");
function readOff() { try { return JSON.parse(fs.readFileSync(offIdx(), "utf8")); } catch { return {}; } }
function writeOff(o) { fs.mkdirSync(offDir(), { recursive: true }); fs.writeFileSync(offIdx(), JSON.stringify(o)); }
function inside(file) {
  const f = path.resolve(file);
  const od = path.resolve(offDir()) + path.sep;
  if (process.platform === "win32" ? f.toLowerCase().startsWith(od.toLowerCase()) : f.startsWith(od)) return true; // saved songs
  return loadCfg().folders.some((d) => { const r = path.resolve(d); return sameP(f, r) || (process.platform === "win32" ? f.toLowerCase().startsWith((r + path.sep).toLowerCase()) : f.startsWith(r + path.sep)); });
}
async function walkDir(root, rel, out, lrc) {
  let ents = [];
  try { ents = await fs.promises.readdir(path.join(root, rel), { withFileTypes: true }); } catch { return; }
  for (const e of ents) {
    const r = rel ? rel + "/" + e.name : e.name;
    if (e.isDirectory()) { if (!e.name.startsWith(".") && e.name !== "$RECYCLE.BIN") await walkDir(root, r, out, lrc); }
    else if (AUDIO.test(e.name) || /\.lrc$/i.test(e.name)) {
      const abs = path.join(root, r);
      try {
        const st = await fs.promises.stat(abs);
        const row = { key: path.basename(root) + "/" + r, abs, name: e.name, size: st.size, mtime: st.mtimeMs };
        (AUDIO.test(e.name) ? out : lrc).push(row);
      } catch {}
    }
  }
}
const b64u = (s) => Buffer.from(s, "utf8").toString("base64url");

/* watch linked folders so new songs show up by themselves */
let watchers = [], watchT = null;
function rewatch() {
  watchers.forEach((w) => { try { w.close(); } catch {} });
  watchers = [];
  for (const d of loadCfg().folders) {
    try {
      watchers.push(fs.watch(d, { recursive: true }, (_ev, name) => {
        if (name && !AUDIO.test(name) && !/\.lrc$/i.test(name) && path.extname(name)) return;
        clearTimeout(watchT);
        watchT = setTimeout(() => { if (win && !win.isDestroyed()) win.webContents.send("dsk:changed"); }, 2500);
      }));
    } catch {}
  }
}

/* data folder: a readable copy of settings, playlists and likes, with daily backups */
const dataPath = () => path.join(loadCfg().dataDir, "milo-data.json");
function writeData(text) {
  const dir = loadCfg().dataDir;
  fs.mkdirSync(dir, { recursive: true });
  const tmp = dataPath() + ".tmp";
  fs.writeFileSync(tmp, text);
  fs.renameSync(tmp, dataPath());
  const bdir = path.join(dir, "backups");
  const day = new Date().toISOString().slice(0, 10);
  const bf = path.join(bdir, "milo-data-" + day + ".json");
  if (!fs.existsSync(bf)) {
    fs.mkdirSync(bdir, { recursive: true });
    fs.writeFileSync(bf, text);
    const all = fs.readdirSync(bdir).filter((n) => /^milo-data-.*\.json$/.test(n)).sort();
    all.slice(0, Math.max(0, all.length - 7)).forEach((n) => { try { fs.unlinkSync(path.join(bdir, n)); } catch {} });
  }
}

function loadState() {
  try { return JSON.parse(fs.readFileSync(stateFile(), "utf8")); } catch { return {}; }
}
function saveState() {
  if (!win || win.isDestroyed()) return;
  try {
    const b = win.isMaximized() ? (win._normal || win.getNormalBounds()) : win.getBounds();
    fs.writeFileSync(stateFile(), JSON.stringify({ ...b, max: win.isMaximized() }));
  } catch {}
}

function createWindow() {
  const st = loadState();
  win = new BrowserWindow({
    width: st.width || 1200, height: st.height || 800, x: st.x, y: st.y,
    minWidth: 380, minHeight: 560,
    backgroundColor: "#000000",
    title: "milo.",
    icon: ICON,
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true, nodeIntegration: false, sandbox: true,
      backgroundThrottling: false, // keep music, crossfade and timers running when minimised
      autoplayPolicy: "no-user-gesture-required",
    },
  });
  Menu.setApplicationMenu(null);
  if (st.max) win.maximize();
  win.once("ready-to-show", () => win.show());
  win.loadURL("app://milo/index.html");

  // Links to other sites open in the default browser.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/.test(url)) shell.openExternal(url);
    return { action: "deny" };
  });
  win.webContents.on("will-navigate", (e, url) => {
    if (!url.startsWith("app://")) { e.preventDefault(); if (/^https?:/.test(url)) shell.openExternal(url); }
  });

  // Close button hides to tray so music keeps playing; use the tray menu to quit.
  win.on("close", (e) => {
    saveState();
    if (!quitting) { e.preventDefault(); win.hide(); }
  });
  win.on("resize", saveState);
  win.on("move", saveState);
}

function showWin() {
  if (!win) return createWindow();
  if (win.isMinimized()) win.restore();
  win.show(); win.focus();
}

function createTray() {
  const img = nativeImage.createFromPath(ICON).resize({ width: 16, height: 16 });
  tray = new Tray(img);
  tray.setToolTip("milo.");
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: "Open milo.", click: showWin },
    { type: "separator" },
    { label: "Quit", click: () => { quitting = true; app.quit(); } },
  ]));
  tray.on("click", showWin);
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on("second-instance", showWin);
  app.whenReady().then(() => {
    protocol.handle("app", (req) => {
      const u = new URL(req.url);
      let p = decodeURIComponent(u.pathname);
      if (p === "/" || p === "") p = "/index.html";
      const file = path.normalize(path.join(APP_DIR, p));
      if (!file.startsWith(APP_DIR)) return new Response("Forbidden", { status: 403 });
      return net.fetch(pathToFileURL(file).toString());
    });
    protocol.handle("milofile", async (req) => {
      try {
        const seg = new URL(req.url).pathname.split("/").filter(Boolean)[0] || "";
        const file = path.resolve(Buffer.from(seg, "base64url").toString("utf8"));
        if (!inside(file)) return new Response("Forbidden", { status: 403 });
        const headers = {};
        const rg = req.headers.get("range");
        if (rg) headers.range = rg;
        const r = await net.fetch(pathToFileURL(file).toString(), { headers });
        const h = new Headers(r.headers);
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Accept-Ranges", "bytes");
        return new Response(r.body, { status: r.status, statusText: r.statusText, headers: h });
      } catch (e) { return new Response("Not found", { status: 404 }); }
    });

    /* YouTube Music: calls go from here, not from the page, so no browser rules (CORS, forbidden headers) get in the way */
    const YT_HOST = /(^|\.)(youtube\.com|googlevideo\.com|ytimg\.com|audioscrobbler\.com|listenbrainz\.org)$/i;
    ipcMain.handle("dsk:http", (_e, r) => new Promise((resolve, reject) => {
      let u; try { u = new URL(r.url); } catch { return reject(new Error("bad url")); }
      if (u.protocol !== "https:" || !YT_HOST.test(u.hostname)) return reject(new Error("blocked host"));
      const req = net.request({ url: r.url, method: r.method || "GET", useSessionCookies: false });
      for (const k of Object.keys(r.headers || {})) { try { req.setHeader(k, r.headers[k]); } catch {} }
      req.on("response", (res) => {
        const ch = [];
        res.on("data", (c) => ch.push(c));
        res.on("end", () => resolve({ status: res.statusCode, text: Buffer.concat(ch).toString("utf8") }));
        res.on("error", reject);
      });
      req.on("error", reject);
      if (r.body) req.write(r.body);
      req.end();
    }));
    protocol.handle("milostream", async (req) => {
      try {
        const u = new URL(req.url);
        const up = Buffer.from(u.pathname.split("/").filter(Boolean)[0] || "", "base64url").toString("utf8");
        if (!/^https:\/\/[a-z0-9.-]+\.googlevideo\.com\//i.test(up)) return new Response("Forbidden", { status: 403 });
        const ua = Buffer.from(u.searchParams.get("ua") || "", "base64url").toString("utf8");
        const CH = 4 * 1024 * 1024; // pieces, not one open-ended download: googlevideo throttles the latter to ~2x realtime
        let start = 0, end = null;
        const m = /bytes=(\d+)-(\d*)/.exec(req.headers.get("range") || "");
        if (m) { start = +m[1]; end = m[2] ? +m[2] : null; }
        const to = end == null ? start + CH - 1 : Math.min(end, start + CH - 1);
        const headers = { Range: "bytes=" + start + "-" + to };
        if (ua) headers["User-Agent"] = ua;
        const r = await net.fetch(up, { headers });
        const h = new Headers();
        for (const k of ["content-type", "content-range", "content-length", "accept-ranges"]) { const v = r.headers.get(k); if (v) h.set(k, v); }
        h.set("Accept-Ranges", "bytes");
        h.set("Access-Control-Allow-Origin", "*");
        return new Response(r.body, { status: r.status, headers: h });
      } catch (e) { return new Response("Bad gateway", { status: 502 }); }
    });
    /* Offline songs: the audio file is saved in bounded pieces (same reason as milostream) into <app data>/offline and played back through milofile */
    const dlBusy = new Set();
    ipcMain.handle("dsk:dl-list", () => { const o = readOff(), out = {}; for (const k of Object.keys(o)) { if (o[k] && fs.existsSync(o[k].file)) out[k] = o[k]; } return out; });
    ipcMain.handle("dsk:dl-remove", (_e, vid) => {
      const o = readOff(); const x = o[String(vid)];
      if (x) { try { fs.unlinkSync(x.file); } catch {} delete o[String(vid)]; writeOff(o); }
      return true;
    });
    ipcMain.handle("dsk:dl-start", async (e, r) => {
      const vid = String(r.vid || "");
      if (!/^[\w-]{6,20}$/.test(vid)) throw new Error("bad id");
      if (!/^https:\/\/[a-z0-9.-]+\.googlevideo\.com\//i.test(String(r.url))) throw new Error("blocked host");
      if (dlBusy.has(vid)) return false;
      dlBusy.add(vid);
      const ext = /webm|opus/i.test(String(r.mime || "")) ? "webm" : "m4a";
      const file = path.join(offDir(), vid + "." + ext), tmp = file + ".part";
      try {
        fs.mkdirSync(offDir(), { recursive: true });
        const fd = fs.openSync(tmp, "w");
        let pos = 0, total = 0;
        try {
          const CH = 4 * 1024 * 1024;
          while (!total || pos < total) {
            const headers = { Range: "bytes=" + pos + "-" + (pos + CH - 1) };
            if (r.ua) headers["User-Agent"] = String(r.ua);
            const res = await net.fetch(r.url, { headers });
            if (res.status !== 206 && res.status !== 200) throw new Error("HTTP " + res.status);
            const buf = Buffer.from(await res.arrayBuffer());
            const m = /\/(\d+)$/.exec(res.headers.get("content-range") || "");
            total = m ? +m[1] : buf.length;
            fs.writeSync(fd, buf, 0, buf.length, pos);
            pos += buf.length;
            try { e.sender.send("dsk:dl-progress", { vid, pct: total ? pos / total : 0 }); } catch {}
            if (!buf.length || res.status === 200) break;
          }
        } finally { fs.closeSync(fd); }
        fs.renameSync(tmp, file);
        const o = readOff();
        o[vid] = { file, ext, t: String(r.t || ""), a: String(r.a || ""), d: +r.d || 0, size: pos, at: Date.now() };
        writeOff(o);
        return true;
      } catch (err) { try { fs.unlinkSync(tmp); } catch {} throw err; }
      finally { dlBusy.delete(vid); }
    });
    ipcMain.handle("dsk:folders", () => loadCfg().folders.slice());
    ipcMain.handle("dsk:pick", async () => {
      const r = await dialog.showOpenDialog(win, { title: "Link a music folder", properties: ["openDirectory", "multiSelections"] });
      if (r.canceled) return null;
      const c = loadCfg();
      for (const d of r.filePaths) if (!c.folders.some((x) => sameP(path.resolve(x), path.resolve(d)))) c.folders.push(d);
      saveCfg(); rewatch();
      return c.folders.slice();
    });
    ipcMain.handle("dsk:remove", (_e, d) => {
      const c = loadCfg();
      c.folders = c.folders.filter((x) => !sameP(x, d));
      saveCfg(); rewatch();
      return c.folders.slice();
    });
    ipcMain.handle("dsk:scan", async () => {
      const files = [], lrc = [];
      for (const d of loadCfg().folders) await walkDir(d, "", files, lrc);
      files.forEach((f) => { f.url = "milofile://f/" + b64u(f.abs) + "/" + encodeURIComponent(f.name); });
      return { files, lrc, folders: loadCfg().folders.slice() };
    });
    ipcMain.handle("dsk:read", async (_e, abs, a, b) => {
      if (!inside(abs)) throw new Error("denied");
      const fh = await fs.promises.open(abs, "r");
      try {
        const n = Math.max(0, Math.min(b - a, 64 * 1024 * 1024));
        const buf = Buffer.alloc(n);
        const { bytesRead } = await fh.read(buf, 0, n, a);
        return buf.subarray(0, bytesRead);
      } finally { await fh.close(); }
    });
    ipcMain.handle("dsk:text", async (_e, abs) => {
      if (!inside(abs)) throw new Error("denied");
      return fs.promises.readFile(abs, "utf8");
    });
    ipcMain.on("dsk:data-load", (e) => { try { e.returnValue = fs.readFileSync(dataPath(), "utf8"); } catch { e.returnValue = null; } });
    ipcMain.handle("dsk:data-save", (_e, text) => { try { writeData(String(text)); return true; } catch { return false; } });
    ipcMain.handle("dsk:presence", (_e, cfg, info) => { try { discord.update(cfg, info); } catch {} return true; });
    ipcMain.handle("dsk:data-dir", () => loadCfg().dataDir);
    ipcMain.handle("dsk:data-open", () => { fs.mkdirSync(loadCfg().dataDir, { recursive: true }); return shell.openPath(loadCfg().dataDir); });
    ipcMain.handle("dsk:data-choose", async () => {
      const r = await dialog.showOpenDialog(win, { title: "Choose where milo. keeps its data", properties: ["openDirectory", "createDirectory"] });
      if (r.canceled) return null;
      const c = loadCfg();
      let old = null;
      try { old = fs.readFileSync(dataPath(), "utf8"); } catch {}
      c.dataDir = r.filePaths[0];
      saveCfg();
      if (old) { try { writeData(old); } catch {} }
      return c.dataDir;
    });
    loadCfg(); rewatch();
    createWindow();
    createTray();
    ipcMain.on("win:min", () => win && win.minimize());
    ipcMain.on("win:max", () => win && (win.isMaximized() ? win.unmaximize() : win.maximize()));
    ipcMain.on("win:close", () => win && win.close());
    app.on("activate", showWin);
  });
  app.on("before-quit", () => { quitting = true; saveState(); try { discord.close(); } catch {} });
  app.on("window-all-closed", () => { /* stay in tray */ });
}
