import { useEffect, useState, useRef } from "react";
import { motion } from "framer-motion";
import { X, Smartphone, Wifi, WifiOff, Battery, CheckCircle2, Loader2, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useSignaturePadStore } from "@/stores/signature-pad-store";

interface SignaturePadDialogProps {
  open: boolean;
  onClose: () => void;
  onSignatureComplete: (strokes: import("@/stores/signature-pad-store").Stroke[]) => void;
}

export function SignaturePadDialog({ open, onClose, onSignatureComplete }: SignaturePadDialogProps) {
  const {
    session,
    device,
    status,
    strokes,
    startSession,
    destroySession,
    clearStrokes,
    undoStroke,
    addStrokePoint,
    completeSignature,
    cancelSignature,
    setDevice,
    setStatus,
  } = useSignaturePadStore();

  const [starting, setStarting] = useState(false);
  const [timeLeft, setTimeLeft] = useState(0);
  const [signatureReceived, setSignatureReceived] = useState(false);
  const signatureReceivedRef = useRef(false);

  // Start session when dialog opens
  useEffect(() => {
    if (open && !session) {
      setStarting(true);
      setSignatureReceived(false);
      signatureReceivedRef.current = false;
      startSession().finally(() => setStarting(false));
    }
  }, [open]);

  // Listen for device events from Electron
  useEffect(() => {
    if (!open) return;
    const api = window.electronAPI;
    if (!api) return;

    const unsub = api.onSignatureDeviceEvent((event) => {
      switch (event.type) {
        case "device-connected":
          setDevice(event.device);
          break;
        case "device-disconnected":
          // Don't clear the view if we already received the signature
          if (!signatureReceivedRef.current) {
            setDevice(null);
            setStatus("disconnected");
          }
          break;
        case "stroke-event":
          addStrokePoint(event.stroke, event.eventType);
          break;
        case "signature-complete":
          completeSignature(event.strokes);
          signatureReceivedRef.current = true;
          setSignatureReceived(true);
          break;
        case "signature-cancelled":
          cancelSignature();
          break;
        case "signature-cleared":
          clearStrokes();
          break;
        case "signature-undone":
          undoStroke();
          break;
      }
    });

    return unsub;
  }, [open]);

  // Session expiry countdown
  useEffect(() => {
    if (!session) return;
    const interval = setInterval(() => {
      const remaining = Math.max(0, session.expiresAt - Date.now());
      setTimeLeft(remaining);
      if (remaining === 0) {
        destroySession();
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [session]);

  // Cleanup on close — save signature if one was received
  const handleClose = () => {
    if (signatureReceived && strokes.length > 0) {
      onSignatureComplete(strokes);
    }
    destroySession();
    onClose();
  };

  const handleRestart = () => {
    setStarting(true);
    setSignatureReceived(false);
    signatureReceivedRef.current = false;
    destroySession().then(() => {
      startSession().finally(() => setStarting(false));
    });
  };

  if (!open) return null;

  const formatTime = (ms: number) => {
    const s = Math.floor(ms / 1000);
    const m = Math.floor(s / 60);
    return `${m}:${String(s % 60).padStart(2, "0")}`;
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={handleClose}>
      <div className="fixed inset-0 bg-black/60" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        className="relative z-50 w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Smartphone className="h-5 w-5 text-primary" />
            <h2 className="text-base font-semibold">Tablette de signature</h2>
          </div>
          <Button variant="ghost" size="icon" className="h-7 w-7" onClick={handleClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        {starting ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : !session ? (
          <div className="py-8 text-center">
            <p className="text-sm text-muted-foreground mb-4">
              Impossible de démarrer la session
            </p>
            <Button onClick={handleRestart} variant="outline" className="gap-2">
              <RefreshCw className="h-4 w-4" />
              Réessayer
            </Button>
          </div>
        ) : status !== "connected" ? (
          /* Pairing view */
          <div className="space-y-4">
            {/* Connection status */}
            <div className="flex items-center gap-2 text-sm">
              {status === "waiting" ? (
                <>
                  <Wifi className="h-4 w-4 text-amber-500" />
                  <span className="text-muted-foreground">En attente de connexion...</span>
                </>
              ) : (
                <>
                  <WifiOff className="h-4 w-4 text-red-500" />
                  <span className="text-muted-foreground">Déconnecté</span>
                </>
              )}
              <span className="ml-auto text-xs text-muted-foreground">
                Expire dans {formatTime(timeLeft)}
              </span>
            </div>

            {/* QR Code */}
            <div className="flex flex-col items-center gap-3 py-2">
              <div className="rounded-lg border border-border bg-white p-3">
                <img src={session.qrDataUrl} alt="QR Code" className="w-48 h-48" />
              </div>
              <p className="text-xs text-muted-foreground text-center">
                Scannez ce QR code avec l'application Android
              </p>
            </div>

            {/* Divider */}
            <div className="flex items-center gap-3">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs text-muted-foreground">OU</span>
              <div className="h-px flex-1 bg-border" />
            </div>

            {/* Pairing code */}
            <div className="flex flex-col items-center gap-2">
              <p className="text-xs text-muted-foreground">Code de couplage</p>
              <div className="flex gap-2">
                {session.pairingCode.split("").map((digit, i) => (
                  <div
                    key={i}
                    className="flex h-10 w-8 items-center justify-center rounded-md border border-border bg-muted text-lg font-bold tracking-wider"
                  >
                    {digit}
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted-foreground text-center mt-1">
                Saisissez ce code dans l'application Android
              </p>
            </div>

            <div className="flex items-center justify-between pt-2">
              <p className="text-xs text-muted-foreground">
                IP: {session.ip}:{session.port}
              </p>
              <Button onClick={handleRestart} variant="ghost" size="sm" className="gap-1 text-xs">
                <RefreshCw className="h-3 w-3" />
                Régénérer
              </Button>
            </div>
          </div>
        ) : (
          /* Connected view */
          <div className="space-y-4">
            {/* Device info */}
            <div className="flex items-center justify-between rounded-lg border border-border bg-muted/50 p-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-green-500" />
                <div>
                  <p className="text-sm font-medium">{device?.deviceName ?? "Android"}</p>
                  <p className="text-xs text-muted-foreground">Connecté</p>
                </div>
              </div>
              {device?.batteryLevel != null && (
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Battery className="h-3.5 w-3.5" />
                  {Math.round(device.batteryLevel * 100)}%
                </div>
              )}
            </div>

            {signatureReceived && (
              <div className="flex items-center gap-2 rounded-lg border border-green-500/30 bg-green-500/10 p-3">
                <CheckCircle2 className="h-4 w-4 text-green-600" />
                <p className="text-sm font-medium text-green-700 dark:text-green-400">
                  Signature reçue — cliquez sur "Enregistrer" pour valider
                </p>
              </div>
            )}

            {/* Signature preview */}
            <div className="rounded-lg border-2 border-dashed border-border bg-white p-2 min-h-[160px] flex items-center justify-center">
              {strokes.length > 0 ? (
                <svg
                  width="100%"
                  height="160"
                  viewBox="0 0 400 200"
                  preserveAspectRatio="xMidYMid meet"
                >
                  <rect width="400" height="200" fill="white" />
                  {strokes.map((stroke, i) => {
                    if (stroke.points.length === 0) return null;
                    const d = stroke.points
                      .map((p, j) =>
                        j === 0
                          ? `M ${p.x.toFixed(2)} ${p.y.toFixed(2)}`
                          : `L ${p.x.toFixed(2)} ${p.y.toFixed(2)}`,
                      )
                      .join(" ");
                    return (
                      <path
                        key={i}
                        d={d}
                        stroke="#1a1a2e"
                        strokeWidth={2}
                        fill="none"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    );
                  })}
                </svg>
              ) : (
                <span className="text-sm text-muted-foreground">
                  En attente de signature sur l'appareil...
                </span>
              )}
            </div>

            <p className="text-xs text-muted-foreground text-center">
              {signatureReceived
                ? "Signature reçue de l'appareil Android."
                : "L'utilisateur dessine sur l'appareil Android. La signature apparaîtra ici après validation."}
            </p>

            <div className="flex justify-between">
              <Button onClick={handleRestart} variant="ghost" size="sm" className="gap-1 text-xs">
                <RefreshCw className="h-3 w-3" />
                Nouvelle session
              </Button>
              {signatureReceived && (
                <Button onClick={handleClose} size="sm" className="gap-1">
                  <CheckCircle2 className="h-4 w-4" />
                  Enregistrer
                </Button>
              )}
            </div>
          </div>
        )}
      </motion.div>
    </div>
  );
}
