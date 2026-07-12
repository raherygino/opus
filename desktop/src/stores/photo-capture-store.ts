import { create } from "zustand";

export interface PhotoPairingSession {
  sessionId: string;
  pairingToken: string;
  pairingCode: string;
  qrDataUrl: string;
  port: number;
  ip: string;
  expiresAt: number;
}

export interface PhotoDeviceInfo {
  deviceName: string;
  batteryLevel: number | null;
  connectedAt: number;
}

type PhotoConnectionStatus = "idle" | "waiting" | "connected" | "disconnected";

interface PhotoCaptureState {
  session: PhotoPairingSession | null;
  device: PhotoDeviceInfo | null;
  status: PhotoConnectionStatus;
  photoData: string | null;

  startSession: () => Promise<void>;
  destroySession: () => Promise<void>;
  setDevice: (device: PhotoDeviceInfo | null) => void;
  setStatus: (status: PhotoConnectionStatus) => void;
  setPhotoData: (data: string | null) => void;
}

export const usePhotoCaptureStore = create<PhotoCaptureState>((set) => ({
  session: null,
  device: null,
  status: "idle",
  photoData: null,

  startSession: async () => {
    const api = window.electronAPI;
    if (!api) return;

    const result = await api.photoStartSession();
    if (result.success && result.session) {
      set({
        session: result.session,
        status: "waiting",
        device: null,
        photoData: null,
      });
    }
  },

  destroySession: async () => {
    const api = window.electronAPI;
    if (!api) return;

    await api.photoDestroySession();
    set({
      session: null,
      device: null,
      status: "idle",
      photoData: null,
    });
  },

  setDevice: (device) => {
    set({
      device,
      status: device ? "connected" : "disconnected",
    });
  },

  setStatus: (status) => {
    set({ status });
  },

  setPhotoData: (data) => {
    set({ photoData: data });
  },
}));
