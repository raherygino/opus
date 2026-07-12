import { contextBridge, ipcRenderer } from "electron";

export interface SignaturePadSession {
  sessionId: string;
  pairingToken: string;
  pairingCode: string;
  qrDataUrl: string;
  port: number;
  ip: string;
  expiresAt: number;
}

export interface ConnectedDevice {
  deviceName: string;
  batteryLevel: number | null;
  connectedAt: number;
}

export type SignatureDeviceEvent =
  | { type: "device-connected"; device: ConnectedDevice }
  | { type: "device-disconnected" }
  | { type: "stroke-event"; stroke: { x: number; y: number; timestamp: number; pressure: number }; eventType: "start" | "move" | "end" }
  | { type: "signature-complete"; strokes: { points: { x: number; y: number; timestamp: number; pressure: number }[] }[] }
  | { type: "signature-cancelled" }
  | { type: "signature-cleared" }
  | { type: "signature-undone" }
  | { type: "photo-received"; photoData: string };

contextBridge.exposeInMainWorld("electronAPI", {
  getAppVersion: () => ipcRenderer.invoke("get-app-version"),
  getPlatform: () => ipcRenderer.invoke("get-platform"),
  minimizeWindow: () => ipcRenderer.invoke("minimize-window"),
  maximizeWindow: () => ipcRenderer.invoke("maximize-window"),
  closeWindow: () => ipcRenderer.invoke("close-window"),
  isMaximized: () => ipcRenderer.invoke("is-maximized"),
  isFocused: () => ipcRenderer.invoke("is-focused"),

  onWindowStateChanged: (callback: (maximized: boolean) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, maximized: boolean) => callback(maximized);
    ipcRenderer.on("window-state-changed", handler);
    return () => ipcRenderer.removeListener("window-state-changed", handler);
  },

  onFocusChanged: (callback: (focused: boolean) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, focused: boolean) => callback(focused);
    ipcRenderer.on("window-focus-changed", handler);
    return () => ipcRenderer.removeListener("window-focus-changed", handler);
  },

  // ─── Signature Pad ───
  signatureStartSession: (): Promise<{ success: boolean; session?: SignaturePadSession; error?: string }> =>
    ipcRenderer.invoke("signature:start-session"),

  signatureDestroySession: (): Promise<{ success: boolean }> =>
    ipcRenderer.invoke("signature:destroy-session"),

  signatureGetSession: (): Promise<{ session: SignaturePadSession | null; device: ConnectedDevice | null }> =>
    ipcRenderer.invoke("signature:get-session"),

  signatureSendToDevice: (message: Record<string, unknown>): Promise<boolean> =>
    ipcRenderer.invoke("signature:send-to-device", message),

  onSignatureDeviceEvent: (callback: (event: SignatureDeviceEvent) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, data: SignatureDeviceEvent) => callback(data);
    ipcRenderer.on("signature-device-event", handler);
    return () => ipcRenderer.removeListener("signature-device-event", handler);
  },

  // ─── Photo Capture (reuses same WebSocket server) ───
  photoStartSession: (): Promise<{ success: boolean; session?: SignaturePadSession; error?: string }> =>
    ipcRenderer.invoke("signature:start-session"),

  photoDestroySession: (): Promise<{ success: boolean }> =>
    ipcRenderer.invoke("signature:destroy-session"),

  onPhotoDeviceEvent: (callback: (event: SignatureDeviceEvent) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, data: SignatureDeviceEvent) => callback(data);
    ipcRenderer.on("signature-device-event", handler);
    return () => ipcRenderer.removeListener("signature-device-event", handler);
  },
});
