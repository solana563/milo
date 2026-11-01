const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("miloDesktop", {
  isDesktop: true,
  platform: process.platform,
  minimize: () => ipcRenderer.send("win:min"),
  toggleMax: () => ipcRenderer.send("win:max"),
  close: () => ipcRenderer.send("win:close"),
});
