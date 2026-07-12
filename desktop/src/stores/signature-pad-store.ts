import { create } from "zustand";

export interface StrokePoint {
  x: number;
  y: number;
  timestamp: number;
  pressure: number;
}

export interface Stroke {
  points: StrokePoint[];
}

export interface PairingSessionInfo {
  sessionId: string;
  pairingToken: string;
  pairingCode: string;
  qrDataUrl: string;
  port: number;
  ip: string;
  expiresAt: number;
}

export interface DeviceInfo {
  deviceName: string;
  batteryLevel: number | null;
  connectedAt: number;
}

type ConnectionStatus = "idle" | "waiting" | "connected" | "disconnected";

/**
 * Normalize stroke coordinates to fit within a target width/height.
 * Finds the bounding box of all stroke points and scales them proportionally
 * with a small padding margin.
 */
function normalizeStrokes(strokes: Stroke[], targetWidth = 400, targetHeight = 200): Stroke[] {
  if (strokes.length === 0) return strokes;

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const stroke of strokes) {
    for (const p of stroke.points) {
      if (p.x < minX) minX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.x > maxX) maxX = p.x;
      if (p.y > maxY) maxY = p.y;
    }
  }

  const padding = 10;
  const sourceWidth = maxX - minX || 1;
  const sourceHeight = maxY - minY || 1;
  const scale = Math.min(
    (targetWidth - padding * 2) / sourceWidth,
    (targetHeight - padding * 2) / sourceHeight,
  );
  const offsetX = (targetWidth - sourceWidth * scale) / 2 - minX * scale;
  const offsetY = (targetHeight - sourceHeight * scale) / 2 - minY * scale;

  return strokes.map((stroke) => ({
    points: stroke.points.map((p) => ({
      x: p.x * scale + offsetX,
      y: p.y * scale + offsetY,
      timestamp: p.timestamp,
      pressure: p.pressure,
    })),
  }));
}

interface SignaturePadState {
  // Session
  session: PairingSessionInfo | null;
  device: DeviceInfo | null;
  status: ConnectionStatus;

  // Strokes
  strokes: Stroke[];
  currentStroke: StrokePoint[];

  // Actions
  startSession: () => Promise<void>;
  destroySession: () => Promise<void>;
  clearStrokes: () => void;
  undoStroke: () => void;
  addStrokePoint: (point: StrokePoint, eventType: "start" | "move" | "end") => void;
  completeSignature: (strokes: Stroke[]) => void;
  cancelSignature: () => void;
  setDevice: (device: DeviceInfo | null) => void;
  setStatus: (status: ConnectionStatus) => void;
}

export const useSignaturePadStore = create<SignaturePadState>((set, get) => ({
  session: null,
  device: null,
  status: "idle",
  strokes: [],
  currentStroke: [],

  startSession: async () => {
    const api = window.electronAPI;
    if (!api) return;

    const result = await api.signatureStartSession();
    if (result.success && result.session) {
      set({
        session: result.session,
        status: "waiting",
        strokes: [],
        currentStroke: [],
        device: null,
      });
    }
  },

  destroySession: async () => {
    const api = window.electronAPI;
    if (!api) return;

    await api.signatureDestroySession();
    set({
      session: null,
      device: null,
      status: "idle",
      strokes: [],
      currentStroke: [],
    });
  },

  clearStrokes: () => {
    set({ strokes: [], currentStroke: [] });
  },

  undoStroke: () => {
    set((state) => ({
      strokes: state.strokes.slice(0, -1),
      currentStroke: [],
    }));
  },

  addStrokePoint: (point, eventType) => {
    if (eventType === "start") {
      set({ currentStroke: [point] });
    } else if (eventType === "move") {
      set((state) => ({
        currentStroke: [...state.currentStroke, point],
      }));
    } else if (eventType === "end") {
      const { currentStroke, strokes } = get();
      const finalStroke = [...currentStroke, point];
      if (finalStroke.length > 1) {
        set({
          strokes: [...strokes, { points: finalStroke }],
          currentStroke: [],
        });
      } else {
        set({ currentStroke: [] });
      }
    }
  },

  completeSignature: (strokes) => {
    const normalized = normalizeStrokes(strokes, 400, 200);
    set({ strokes: normalized, currentStroke: [] });
  },

  cancelSignature: () => {
    set({ strokes: [], currentStroke: [] });
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
}));

/**
 * Convert strokes to an SVG string.
 * Each stroke becomes a <path> with a polyline through all its points.
 */
export function strokesToSvg(strokes: Stroke[], width = 400, height = 200): string {
  const normalized = normalizeStrokes(strokes, width, height);
  const paths = normalized.map((stroke) => {
    if (stroke.points.length === 0) return "";
    const d = stroke.points
      .map((p, i) => {
        if (i === 0) return `M ${p.x.toFixed(2)} ${p.y.toFixed(2)}`;
        return `L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`;
      })
      .join(" ");
    return `<path d="${d}" stroke="black" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round" />`;
  });

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${paths.join("")}</svg>`;
}
