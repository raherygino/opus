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

export interface ElectronAPI {
  getAppVersion: () => Promise<string>;
  getPlatform: () => Promise<string>;
  minimizeWindow: () => Promise<void>;
  maximizeWindow: () => Promise<void>;
  closeWindow: () => Promise<void>;
  isMaximized: () => Promise<boolean>;
  isFocused: () => Promise<boolean>;
  onWindowStateChanged: (callback: (maximized: boolean) => void) => () => void;
  onFocusChanged: (callback: (focused: boolean) => void) => () => void;

  // Signature Pad
  signatureStartSession: () => Promise<{ success: boolean; session?: SignaturePadSession; error?: string }>;
  signatureDestroySession: () => Promise<{ success: boolean }>;
  signatureGetSession: () => Promise<{ session: SignaturePadSession | null; device: ConnectedDevice | null }>;
  signatureSendToDevice: (message: Record<string, unknown>) => Promise<boolean>;
  onSignatureDeviceEvent: (callback: (event: SignatureDeviceEvent) => void) => () => void;

  // Photo Capture (reuses same WebSocket server)
  photoStartSession: () => Promise<{ success: boolean; session?: SignaturePadSession; error?: string }>;
  photoDestroySession: () => Promise<{ success: boolean }>;
  onPhotoDeviceEvent: (callback: (event: SignatureDeviceEvent) => void) => () => void;
}

declare global {
  interface Window {
    electronAPI?: ElectronAPI;
  }
}
