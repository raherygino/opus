import { app, BrowserWindow, ipcMain, shell } from "electron";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

let mainWindow: BrowserWindow | null = null;

const isDev = !app.isPackaged;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 900,
    minHeight: 600,
    show: false,
    frame: false,
    titleBarStyle: "hidden",
    backgroundColor: "#0a0a0b",
    webPreferences: {
      preload: join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  // Set CSP & CORS headers so the renderer can reach the local API
  mainWindow.webContents.session.webRequest.onHeadersReceived((details, callback) => {
    const isApi = details.url.startsWith("http://192.168.1.163:8080");
    callback({
      responseHeaders: {
        ...details.responseHeaders,
        "Content-Security-Policy": [
          "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data: https://api.mapbox.com; img-src 'self' data: http://127.0.0.1:8080 http://192.168.1.163:8080 https://api.mapbox.com https://*.tiles.mapbox.com; connect-src 'self' http://127.0.0.1:8080 http://192.168.1.163:8080 https://nominatim.openstreetmap.org https://api.mapbox.com https://events.mapbox.com; worker-src 'self' blob:;",
        ],
        ...(isApi && {
          "Access-Control-Allow-Origin": ["*"],
          "Access-Control-Allow-Methods": ["GET, POST, PUT, DELETE, PATCH, OPTIONS"],
          "Access-Control-Allow-Headers": ["Content-Type, Authorization"],
        }),
      },
    });
  });

  if (isDev) {
    mainWindow.loadURL("http://localhost:5173");
    mainWindow.webContents.openDevTools({ mode: "detach" });
  } else {
    mainWindow.loadFile(join(__dirname, "../dist/index.html"));
  }

  mainWindow.once("ready-to-show", () => {
    mainWindow?.show();
  });

  mainWindow.on("maximize", () => {
    mainWindow?.webContents.send("window-state-changed", true);
  });

  mainWindow.on("unmaximize", () => {
    mainWindow?.webContents.send("window-state-changed", false);
  });

  mainWindow.on("focus", () => {
    mainWindow?.webContents.send("window-focus-changed", true);
  });

  mainWindow.on("blur", () => {
    mainWindow?.webContents.send("window-focus-changed", false);
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

ipcMain.handle("get-app-version", () => {
  return app.getVersion();
});

ipcMain.handle("get-platform", () => {
  return process.platform;
});

ipcMain.handle("minimize-window", () => {
  mainWindow?.minimize();
});

ipcMain.handle("maximize-window", () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize();
  } else {
    mainWindow?.maximize();
  }
});

ipcMain.handle("close-window", () => {
  mainWindow?.close();
});

ipcMain.handle("is-maximized", () => {
  return mainWindow?.isMaximized() ?? false;
});

ipcMain.handle("is-focused", () => {
  return mainWindow?.isFocused() ?? false;
});
