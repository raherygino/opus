import { WebSocketServer, WebSocket } from "ws";
import QRCode from "qrcode";
import os from "os";
import crypto from "crypto";

// ─── Types ───

export interface StrokePoint {
  x: number;
  y: number;
  timestamp: number;
  pressure: number;
}

export interface Stroke {
  points: StrokePoint[];
}

export interface PairingSession {
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

type DeviceEvent =
  | { type: "device-connected"; device: ConnectedDevice }
  | { type: "device-disconnected" }
  | { type: "stroke-event"; stroke: StrokePoint; eventType: "start" | "move" | "end" }
  | { type: "signature-complete"; strokes: Stroke[] }
  | { type: "signature-cancelled" }
  | { type: "signature-cleared" }
  | { type: "signature-undone" };

type DeviceEventListener = (event: DeviceEvent) => void;

// ─── SignaturePadServer ───

const DEFAULT_PORT = 9876;
const SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes
const HEARTBEAT_INTERVAL_MS = 15_000;
const HEARTBEAT_TIMEOUT_MS = 5_000;

export class SignaturePadServer {
  private wss: WebSocketServer | null = null;
  private port: number = DEFAULT_PORT;
  private session: PairingSession | null = null;
  private client: WebSocket | null = null;
  private device: ConnectedDevice | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private heartbeatTimeoutTimer: ReturnType<typeof setTimeout> | null = null;
  private sessionTimeoutTimer: ReturnType<typeof setTimeout> | null = null;
  private listeners: Set<DeviceEventListener> = new Set();

  /** Get the primary local IP address of this machine. */
  private getLocalIp(): string {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
      for (const iface of interfaces[name] ?? []) {
        if (iface.family === "IPv4" && !iface.internal) {
          return iface.address;
        }
      }
    }
    return "127.0.0.1";
  }

  /** Generate a random 6-digit pairing code. */
  private generatePairingCode(): string {
    return String(Math.floor(100000 + Math.random() * 900000));
  }

  /** Generate a secure random token. */
  private generateToken(): string {
    return crypto.randomBytes(16).toString("hex");
  }

  /** Generate a session ID. */
  private generateSessionId(): string {
    return crypto.randomBytes(8).toString("hex");
  }

  /** Add an event listener for device events. */
  addListener(listener: DeviceEventListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private emit(event: DeviceEvent): void {
    this.listeners.forEach((l) => l(event));
  }

  /** Start the WebSocket server and create a pairing session. */
  async startSession(): Promise<PairingSession> {
    // If a session already exists and is still valid, return it
    if (this.session && Date.now() < this.session.expiresAt) {
      return this.session;
    }

    // Start WebSocket server if not running
    if (!this.wss) {
      this.wss = new WebSocketServer({ port: this.port });
      this.wss.on("connection", (ws, req) => this.handleConnection(ws, req));
    }

    const sessionId = this.generateSessionId();
    const pairingToken = this.generateToken();
    const pairingCode = this.generatePairingCode();
    const ip = this.getLocalIp();

    // QR code payload: desktop IP, port, session ID, pairing token
    const qrPayload = JSON.stringify({
      ip,
      port: this.port,
      sessionId,
      token: pairingToken,
    });

    const qrDataUrl = await QRCode.toDataURL(qrPayload, {
      width: 300,
      margin: 2,
      color: { dark: "#0a0a0b", light: "#ffffff" },
    });

    this.session = {
      sessionId,
      pairingToken,
      pairingCode,
      qrDataUrl,
      port: this.port,
      ip,
      expiresAt: Date.now() + SESSION_TIMEOUT_MS,
    };

    // Set session expiry timer
    if (this.sessionTimeoutTimer) clearTimeout(this.sessionTimeoutTimer);
    this.sessionTimeoutTimer = setTimeout(() => {
      this.destroySession();
    }, SESSION_TIMEOUT_MS);

    return this.session;
  }

  /** Destroy the current pairing session and disconnect any client. */
  destroySession(): void {
    if (this.client) {
      try {
        this.client.close(1000, "session_destroyed");
      } catch {
        // ignore
      }
      this.client = null;
      this.device = null;
      this.emit({ type: "device-disconnected" });
    }

    this.session = null;
    if (this.sessionTimeoutTimer) {
      clearTimeout(this.sessionTimeoutTimer);
      this.sessionTimeoutTimer = null;
    }
    this.stopHeartbeat();
  }

  /** Stop the WebSocket server entirely. */
  stop(): void {
    this.destroySession();
    if (this.wss) {
      this.wss.close();
      this.wss = null;
    }
  }

  /** Get the current session info (or null). */
  getSession(): PairingSession | null {
    if (this.session && Date.now() < this.session.expiresAt) {
      return this.session;
    }
    return null;
  }

  /** Get the currently connected device (or null). */
  getDevice(): ConnectedDevice | null {
    return this.device;
  }

  /** Send a JSON message to the connected Android device. */
  sendToDevice(message: Record<string, unknown>): boolean {
    if (this.client && this.client.readyState === WebSocket.OPEN) {
      this.client.send(JSON.stringify(message));
      return true;
    }
    return false;
  }

  /** Handle a new WebSocket connection from an Android device. */
  private handleConnection(ws: WebSocket, req: { url?: string }): void {
    if (!req.url) return;
    // Only one device per session
    if (this.client) {
      ws.close(4000, "device_already_connected");
      return;
    }

    // Parse query params from URL
    const url = new URL(req.url, `http://localhost:${this.port}`);
    const sessionId = url.searchParams.get("sessionId");
    const token = url.searchParams.get("token");
    const pairingCode = url.searchParams.get("code");

    // Validate session
    if (!this.session) {
      ws.close(4001, "no_active_session");
      return;
    }

    // Validate session ID and token
    const tokenValid = sessionId === this.session.sessionId && token === this.session.pairingToken;
    const codeValid = pairingCode === this.session.pairingCode;

    if (!tokenValid && !codeValid) {
      ws.close(4002, "invalid_pairing");
      return;
    }

    // Check session expiry
    if (Date.now() >= this.session.expiresAt) {
      ws.close(4003, "session_expired");
      return;
    }

    this.client = ws;
    this.device = {
      deviceName: "Android Device",
      batteryLevel: null,
      connectedAt: Date.now(),
    };

    // Send PAIR_SUCCESS
    ws.send(
      JSON.stringify({
        type: "PAIR_SUCCESS",
        sessionId: this.session.sessionId,
        message: "Pairing successful",
      }),
    );

    // Notify renderer
    this.emit({ type: "device-connected", device: this.device });

    // Start heartbeat
    this.startHeartbeat(ws);

    // Handle messages
    ws.on("message", (data) => this.handleMessage(data.toString()));

    // Handle disconnect
    ws.on("close", () => {
      this.client = null;
      this.device = null;
      this.stopHeartbeat();
      this.emit({ type: "device-disconnected" });
    });

    ws.on("error", () => {
      // Silent — close handler will fire
    });
  }

  /** Handle incoming messages from the Android device. */
  private handleMessage(raw: string): void {
    try {
      const msg = JSON.parse(raw);
      switch (msg.type) {
        case "HELLO":
          if (this.device && msg.deviceName) {
            this.device = {
              ...this.device,
              deviceName: msg.deviceName as string,
              batteryLevel: msg.batteryLevel ?? null,
            };
            this.emit({ type: "device-connected", device: this.device });
          }
          break;

        case "HEARTBEAT":
          // Reset heartbeat timeout
          if (this.heartbeatTimeoutTimer) {
            clearTimeout(this.heartbeatTimeoutTimer);
            this.heartbeatTimeoutTimer = setTimeout(() => {
              // Heartbeat timeout — disconnect
              if (this.client) {
                this.client.close(4004, "heartbeat_timeout");
              }
            }, HEARTBEAT_TIMEOUT_MS * 3);
          }
          break;

        case "START_STROKE":
          this.emit({
            type: "stroke-event",
            eventType: "start",
            stroke: {
              x: msg.x,
              y: msg.y,
              timestamp: msg.timestamp,
              pressure: msg.pressure ?? 1,
            },
          });
          break;

        case "MOVE_STROKE":
          this.emit({
            type: "stroke-event",
            eventType: "move",
            stroke: {
              x: msg.x,
              y: msg.y,
              timestamp: msg.timestamp,
              pressure: msg.pressure ?? 1,
            },
          });
          break;

        case "END_STROKE":
          this.emit({
            type: "stroke-event",
            eventType: "end",
            stroke: {
              x: msg.x,
              y: msg.y,
              timestamp: msg.timestamp,
              pressure: msg.pressure ?? 1,
            },
          });
          break;

        case "CLEAR_SIGNATURE":
          this.emit({ type: "signature-cleared" });
          break;

        case "UNDO_STROKE":
          this.emit({ type: "signature-undone" });
          break;

        case "COMPLETE_SIGNATURE":
          this.emit({
            type: "signature-complete",
            strokes: msg.strokes as Stroke[],
          });
          break;

        case "CANCEL_SIGNATURE":
          this.emit({ type: "signature-cancelled" });
          break;

        case "DISCONNECT":
          if (this.client) {
            this.client.close(1000, "client_disconnect");
          }
          break;
      }
    } catch {
      // Ignore malformed messages
    }
  }

  /** Start the heartbeat mechanism. */
  private startHeartbeat(ws: WebSocket): void {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: "HEARTBEAT" }));
      }
    }, HEARTBEAT_INTERVAL_MS);

    this.heartbeatTimeoutTimer = setTimeout(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close(4004, "heartbeat_timeout");
      }
    }, HEARTBEAT_INTERVAL_MS + HEARTBEAT_TIMEOUT_MS);
  }

  /** Stop the heartbeat timers. */
  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
    if (this.heartbeatTimeoutTimer) {
      clearTimeout(this.heartbeatTimeoutTimer);
      this.heartbeatTimeoutTimer = null;
    }
  }
}
