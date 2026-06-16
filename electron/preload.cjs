const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("electronAPI", {
  getAppVersion: () => ipcRenderer.invoke("get-app-version"),
  getPlatform: () => ipcRenderer.invoke("get-platform"),
  minimizeWindow: () => ipcRenderer.invoke("minimize-window"),
  maximizeWindow: () => ipcRenderer.invoke("maximize-window"),
  closeWindow: () => ipcRenderer.invoke("close-window"),
  isMaximized: () => ipcRenderer.invoke("is-maximized"),
  isFocused: () => ipcRenderer.invoke("is-focused"),

  onWindowStateChanged: (callback) => {
    const handler = (_event, maximized) => callback(maximized);
    ipcRenderer.on("window-state-changed", handler);
    return () => ipcRenderer.removeListener("window-state-changed", handler);
  },

  onFocusChanged: (callback) => {
    const handler = (_event, focused) => callback(focused);
    ipcRenderer.on("window-focus-changed", handler);
    return () => ipcRenderer.removeListener("window-focus-changed", handler);
  },
});
