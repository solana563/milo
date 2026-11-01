const { app, BrowserWindow, Menu, Tray, shell, protocol, net, ipcMain, nativeImage } = require("electron");
const path = require("path");
const fs = require("fs");
const { pathToFileURL } = require("url");

// Serve the app from app://milo so it is a secure context with a stable origin
// (service worker, localStorage, IndexedDB and media APIs all work, and data survives updates).
protocol.registerSchemesAsPrivileged([
  { scheme: "app", privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true, bypassCSP: false } },
]);

const APP_DIR = path.join(__dirname, "app");
const ICON = path.join(__dirname, "build", "icon.png");
const stateFile = () => path.join(app.getPath("userData"), "window.json");
let win = null, tray = null, quitting = false;

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
    createWindow();
    createTray();
    ipcMain.on("win:min", () => win && win.minimize());
    ipcMain.on("win:max", () => win && (win.isMaximized() ? win.unmaximize() : win.maximize()));
    ipcMain.on("win:close", () => win && win.close());
    app.on("activate", showWin);
  });
  app.on("before-quit", () => { quitting = true; saveState(); });
  app.on("window-all-closed", () => { /* stay in tray */ });
}
