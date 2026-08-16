import { useCallback, useEffect, useRef, useState } from "react";
import QRCode from "qrcode";
import { motion, AnimatePresence } from "framer-motion";
import {
  CheckCircle2,
  Clock,
  Loader2,
  QrCode,
  RefreshCw,
  Smartphone,
  XCircle,
  X,
  AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import {
  createQrAuthRequest,
  getQrAuthStatus,
  cancelQrAuth,
} from "@/lib/api/qr-auth";
import type { User } from "@/types";

type PanelStatus =
  | "idle"
  | "generating"
  | "waiting"
  | "scanned"
  | "approved"
  | "rejected"
  | "expired"
  | "cancelled"
  | "error";

interface QrLoginPanelProps {
  /**
   * "desktop" — the desktop is requesting auth (forward flow, login page).
   * "android" — the phone is requesting auth (reverse flow, desktop is
   *   already logged in and shows the QR so the phone can scan to log in).
   */
  deviceType: "desktop" | "android";
  /** Called when authentication succeeds (tokens received). */
  onSuccess: (user: User, accessToken: string, refreshToken: string) => void;
  /** Called when the user cancels / wants to return. */
  onCancel: () => void;
  /** Optional device name override. Defaults to a platform-derived name. */
  deviceName?: string;
}

const POLL_INTERVAL_MS = 2000;

const STATUS_LABELS: Record<PanelStatus, string> = {
  idle: "Prêt",
  generating: "Génération du QR code…",
  waiting: "En attente du scan…",
  scanned: "QR code scanné — en attente d'approbation",
  approved: "Authentification approuvée",
  rejected: "Authentification refusée",
  expired: "QR code expiré",
  cancelled: "Demande annulée",
  error: "Une erreur est survenue",
};

function osLabel(): string {
  const p = navigator.platform?.toLowerCase() ?? "";
  if (p.includes("mac")) return "macOS";
  if (p.includes("win")) return "Windows";
  if (p.includes("linux")) return "Linux";
  return "Desktop";
}

export function QrLoginPanel({
  deviceType,
  onSuccess,
  onCancel,
  deviceName,
}: QrLoginPanelProps) {
  const [status, setStatus] = useState<PanelStatus>("idle");
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string>("qr");
  const [requestCode, setRequestCode] = useState<string | null>(null);
  const [countdown, setCountdown] = useState<number>(0);

  const pollTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const expiryTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const cancelledRef = useRef(false);
  const consumedRef = useRef(false);

  const { login } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const finalDeviceName = deviceName ?? `OPUS Desktop — ${osLabel()}`;

  const cleanup = useCallback(() => {
    if (pollTimer.current) {
      clearTimeout(pollTimer.current);
      pollTimer.current = null;
    }
    if (expiryTimer.current) {
      clearInterval(expiryTimer.current);
      expiryTimer.current = null;
    }
  }, []);

  const cancelRequest = useCallback(
    async (code: string | null) => {
      cleanup();
      if (code) {
        try {
          await cancelQrAuth(code);
        } catch {
          // Best-effort — the request will expire on its own
        }
      }
    },
    [cleanup],
  );

  const generateQr = useCallback(async () => {
    cleanup();
    cancelledRef.current = false;
    consumedRef.current = false;
    setStatus("generating");
    setQrDataUrl(null);
    setRequestCode(null);

    try {
      const resp = await createQrAuthRequest(deviceType, finalDeviceName);
      if (cancelledRef.current) return;

      const code = resp.request_code;
      // The server stores expires_at in UTC but returns it as a MySQL
      // TIMESTAMP string (e.g. "2026-08-16 12:30:00") without a timezone
      // suffix. JavaScript's Date constructor treats such strings as local
      // time, which would make the countdown wrong. Append "Z" so it's
      // parsed as UTC.
      const expiresRaw = resp.expires_at;
      const expires = new Date(
        expiresRaw.endsWith("Z") || /[+-]\d{2}:?\d{2}$/.test(expiresRaw)
          ? expiresRaw
          : expiresRaw.replace(" ", "T") + "Z",
      ).getTime();

      // QR payload — only the temporary code, never credentials
      const qrPayload = JSON.stringify({
        opus: "qr_auth",
        code,
        v: 1,
      });

      const dataUrl = await QRCode.toDataURL(qrPayload, {
        width: 280,
        margin: 2,
        color: { dark: "#0a0a0b", light: "#ffffff" },
        errorCorrectionLevel: "M",
      });

      if (cancelledRef.current) return;

      setRequestCode(code);
      setQrDataUrl(dataUrl);
      setCountdown(Math.max(0, Math.floor((expires - Date.now()) / 1000)));
      setStatus("waiting");

      // Countdown timer
      expiryTimer.current = setInterval(() => {
        const remaining = Math.max(0, Math.floor((expires - Date.now()) / 1000));
        setCountdown(remaining);
        if (remaining <= 0) {
          if (expiryTimer.current) clearInterval(expiryTimer.current);
          setStatus("expired");
        }
      }, 1000);
    } catch (err: unknown) {
      if (cancelledRef.current) return;
      const msg =
        err && typeof err === "object" && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data
              ?.message ?? "Erreur lors de la génération du QR code"
          : "Erreur lors de la génération du QR code";
      setErrorCode(msg);
      setStatus("error");
    }
  }, [cleanup, deviceType, finalDeviceName]);

  // Poll status
  useEffect(() => {
    if (status !== "waiting" && status !== "scanned") return;
    if (!requestCode) return;

    let active = true;

    const poll = async () => {
      if (!active || !requestCode || consumedRef.current) return;
      try {
        const resp = await getQrAuthStatus(requestCode);
        if (!active || consumedRef.current) return;

        switch (resp.status) {
          case "pending":
            setStatus("waiting");
            break;
          case "scanned":
            setStatus("scanned");
            break;
          case "approved":
            consumedRef.current = true;
            cleanup();
            setStatus("approved");
            if (deviceType === "android") {
              // Reverse flow: phone received the tokens directly. Desktop just confirms success.
              onSuccess(null as unknown as User, "", "");
            } else if (resp.access_token && resp.refresh_token && resp.user) {
              login(resp.user, resp.access_token, resp.refresh_token);
              addNotification(
                "success",
                "Connexion réussie",
                `Bienvenue ${resp.user.firstname} ${resp.user.lastname}`,
              );
              onSuccess(resp.user, resp.access_token, resp.refresh_token);
            } else {
              setStatus("error");
              setErrorCode("Tokens manquants dans la réponse d'approbation");
            }
            return; // Stop polling
          case "rejected":
            cleanup();
            setStatus("rejected");
            return;
          case "expired":
            cleanup();
            setStatus("expired");
            return;
          case "cancelled":
            cleanup();
            setStatus("cancelled");
            return;
          case "consumed":
            // If reverse flow (phone already approved and consumed tokens), desktop sees "consumed"
            if (deviceType === "android") {
              consumedRef.current = true;
              cleanup();
              setStatus("approved");
              onSuccess(null as unknown as User, "", "");
              return;
            }
            cleanup();
            return;
        }

        // Schedule next poll
        pollTimer.current = setTimeout(poll, POLL_INTERVAL_MS);
      } catch {
        if (!active) return;
        // Network error — keep polling (transient)
        pollTimer.current = setTimeout(poll, POLL_INTERVAL_MS * 2);
      }
    };

    pollTimer.current = setTimeout(poll, POLL_INTERVAL_MS);

    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, requestCode]);

  // Generate on mount
  useEffect(() => {
    generateQr();
    return () => {
      cancelledRef.current = true;
      cleanup();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCancel = async () => {
    cancelledRef.current = true;
    await cancelRequest(requestCode);
    setStatus("cancelled");
    onCancel();
  };

  const handleRegenerate = () => {
    generateQr();
  };

  const isTerminal =
    status === "approved" ||
    status === "rejected" ||
    status === "expired" ||
    status === "cancelled" ||
    status === "error";

  return (
    <div className="flex flex-col items-center space-y-5">
      <div className="text-center">
        <h2 className="text-lg font-semibold text-foreground">
          {deviceType === "desktop"
            ? "Se connecter avec le téléphone"
            : "Connecter un téléphone"}
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {deviceType === "desktop"
            ? "Scannez ce QR code avec votre téléphone pour vous connecter."
            : "Affichez ce QR code pour qu'un téléphone se connecte."}
        </p>
      </div>

      {/* QR code area */}
      <div className="relative flex items-center justify-center">
        <div className="relative rounded-2xl border-2 border-border bg-white p-4 shadow-sm">
          <AnimatePresence mode="wait">
            {qrDataUrl && status === "waiting" && (
              <motion.img
                key={requestCode ?? "qr"}
                src={qrDataUrl}
                alt="QR code de connexion"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ duration: 0.25 }}
                className="h-[280px] w-[280px]"
              />
            )}
            {status === "generating" && (
              <motion.div
                key="generating"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex h-[280px] w-[280px] items-center justify-center"
              >
                <Loader2 className="h-10 w-10 animate-spin text-muted-foreground" />
              </motion.div>
            )}
            {status === "scanned" && (
              <motion.div
                key="scanned"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="flex h-[280px] w-[280px] flex-col items-center justify-center gap-3"
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-primary/10">
                  <Smartphone className="h-8 w-8 text-primary" />
                </div>
                <p className="text-center text-sm font-medium text-foreground">
                  Téléphone connecté
                </p>
                <p className="text-center text-xs text-muted-foreground">
                  Autorisez la connexion depuis votre téléphone
                </p>
              </motion.div>
            )}
            {status === "approved" && (
              <motion.div
                key="approved"
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="flex h-[280px] w-[280px] flex-col items-center justify-center gap-3"
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-500/10">
                  <CheckCircle2 className="h-8 w-8 text-green-500" />
                </div>
                <p className="text-center text-sm font-medium text-foreground">
                  Connexion réussie
                </p>
              </motion.div>
            )}
            {(status === "rejected" ||
              status === "expired" ||
              status === "cancelled" ||
              status === "error") && (
              <motion.div
                key={status}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0 }}
                className="flex h-[280px] w-[280px] flex-col items-center justify-center gap-3"
              >
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
                  {status === "error" ? (
                    <AlertCircle className="h-8 w-8 text-destructive" />
                  ) : (
                    <XCircle className="h-8 w-8 text-destructive" />
                  )}
                </div>
                <p className="text-center text-sm font-medium text-foreground">
                  {STATUS_LABELS[status]}
                </p>
                {status === "error" && (
                  <p className="text-center text-xs text-muted-foreground max-w-[220px]">
                    {errorCode}
                  </p>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {/* Status + countdown */}
      <div className="flex flex-col items-center gap-1.5">
        <StatusBadge status={status} />
        {status === "waiting" && countdown > 0 && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Clock className="h-3.5 w-3.5" />
            <span>Expire dans {countdown}s</span>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex w-full flex-col gap-2">
        {(status === "expired" ||
          status === "rejected" ||
          status === "cancelled" ||
          status === "error") && (
          <Button
            onClick={handleRegenerate}
            className="w-full h-11 rounded-lg gap-2"
          >
            <RefreshCw className="h-4 w-4" />
            Regénérer le QR code
          </Button>
        )}
        {!isTerminal && (
          <Button
            variant="outline"
            onClick={handleCancel}
            className="w-full h-11 rounded-lg gap-2"
          >
            <X className="h-4 w-4" />
            Annuler
          </Button>
        )}
        {isTerminal && status !== "approved" && (
          <Button
            variant="outline"
            onClick={onCancel}
            className="w-full h-11 rounded-lg gap-2"
          >
            <QrCode className="h-4 w-4" />
            Retour à la connexion
          </Button>
        )}
      </div>
    </div>
  );
}

function StatusBadge({ status }: { status: PanelStatus }) {
  const config: Record<
    PanelStatus,
    { icon: React.ReactNode; className: string }
  > = {
    idle: {
      icon: <Clock className="h-4 w-4" />,
      className: "bg-muted text-muted-foreground",
    },
    generating: {
      icon: <Loader2 className="h-4 w-4 animate-spin" />,
      className: "bg-muted text-muted-foreground",
    },
    waiting: {
      icon: <Clock className="h-4 w-4" />,
      className: "bg-primary/10 text-primary",
    },
    scanned: {
      icon: <Smartphone className="h-4 w-4" />,
      className: "bg-blue-500/10 text-blue-500",
    },
    approved: {
      icon: <CheckCircle2 className="h-4 w-4" />,
      className: "bg-green-500/10 text-green-500",
    },
    rejected: {
      icon: <XCircle className="h-4 w-4" />,
      className: "bg-destructive/10 text-destructive",
    },
    expired: {
      icon: <Clock className="h-4 w-4" />,
      className: "bg-destructive/10 text-destructive",
    },
    cancelled: {
      icon: <XCircle className="h-4 w-4" />,
      className: "bg-muted text-muted-foreground",
    },
    error: {
      icon: <AlertCircle className="h-4 w-4" />,
      className: "bg-destructive/10 text-destructive",
    },
  };

  const { icon, className } = config[status];

  return (
    <div
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-xs font-medium ${className}`}
    >
      {icon}
      {STATUS_LABELS[status]}
    </div>
  );
}
