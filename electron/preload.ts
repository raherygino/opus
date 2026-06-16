import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
  getAppVersion: () => ipcRenderer.invoke("get-app-version"),
  getPlatform: () => ipcRenderer.invoke("get-platform"),
  minimizeWindow: () => ipcRenderer.invoke("minimize-window"),
  maximizeWindow: () => ipcRenderer.invoke("maximize-window"),
  closeWindow: () => ipcRenderer.invoke("close-window"),
  isMaximized: () => ipcRenderer.invoke("is-maximized"),
  onMaximizeChange: (callback: (isMaximized: boolean) => void) => {
    ipcRenderer.on("maximize-change", (_event, isMaximized) => {
      callback(isMaximized);
    });
  },
});
