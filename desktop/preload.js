const { contextBridge, ipcRenderer } = require("electron");
let data = null;
try { data = ipcRenderer.sendSync("dsk:data-load"); } catch (e) {}
contextBridge.exposeInMainWorld("miloDesktop", {
  isDesktop: true,
  platform: process.platform,
  data, // saved settings text from the data folder (read once at start-up)
  minimize: () => ipcRenderer.send("win:min"),
  toggleMax: () => ipcRenderer.send("win:max"),
  close: () => ipcRenderer.send("win:close"),
  folders: () => ipcRenderer.invoke("dsk:folders"),
  pickFolders: () => ipcRenderer.invoke("dsk:pick"),
  removeFolder: (d) => ipcRenderer.invoke("dsk:remove", d),
  scan: () => ipcRenderer.invoke("dsk:scan"),
  read: (abs, a, b) => ipcRenderer.invoke("dsk:read", abs, a, b),
  text: (abs) => ipcRenderer.invoke("dsk:text", abs),
  saveData: (t) => ipcRenderer.invoke("dsk:data-save", t),
  dataDir: () => ipcRenderer.invoke("dsk:data-dir"),
  openDataDir: () => ipcRenderer.invoke("dsk:data-open"),
  chooseDataDir: () => ipcRenderer.invoke("dsk:data-choose"),
  http: (r) => ipcRenderer.invoke("dsk:http", r),
  streamUrl: (url, ua) => {
    const b = (x) => btoa(unescape(encodeURIComponent(x))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    return "milostream://s/" + b(url) + "?ua=" + b(ua || "");
  },
  onFoldersChanged: (cb) => ipcRenderer.on("dsk:changed", () => cb()),
});
