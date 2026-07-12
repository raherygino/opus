import { useEffect, useState, useRef, useCallback } from "react";
import { motion } from "framer-motion";
import { X, Wifi, WifiOff, Battery, CheckCircle2, Loader2, RefreshCw, Camera, Crop } from "lucide-react";
import { Button } from "@/components/ui/button";
import { usePhotoCaptureStore } from "@/stores/photo-capture-store";

interface PhotoCaptureDialogProps {
  open: boolean;
  onClose: () => void;
  onPhotoComplete: (photoData: string) => void;
}

async function cropToSquareDataUrl(
  dataUrl: string,
  offsetX: number,
  offsetY: number,
  sizeFraction: number,
): Promise<string> {
  const img = await loadImage(dataUrl);
  const minSide = Math.min(img.width, img.height);
  const side = Math.round(sizeFraction * minSide);
  const sx = Math.round(offsetX * img.width);
  const sy = Math.round(offsetY * img.height);
  const canvas = document.createElement("canvas");
  canvas.width = side;
  canvas.height = side;
  const ctx = canvas.getContext("2d")!;
  ctx.drawImage(img, sx, sy, side, side, 0, 0, side, side);
  return new Promise((resolve) => {
    canvas.toBlob(
      (blob) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.readAsDataURL(blob!);
      },
      "image/jpeg",
      90,
    );
  });
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

export function PhotoCaptureDialog({ open, onClose, onPhotoComplete }: PhotoCaptureDialogProps) {
  const {
    session,
    device,
    status,
    photoData,
    startSession,
    destroySession,
    setDevice,
    setStatus,
    setPhotoData,
  } = usePhotoCaptureStore();

  const [starting, setStarting] = useState(false);
  const [timeLeft, setTimeLeft] = useState(0);
  const [photoReceived, setPhotoReceived] = useState(false);
  const photoReceivedRef = useRef(false);

  // Crop state — all fractions of the displayed image
  // cropX/cropY: top-left corner position (0..1 of image width/height)
  // cropSize: square side as fraction of min(imgW, imgH), range 0.2..1
  const [cropX, setCropX] = useState(0);
  const [cropY, setCropY] = useState(0);
  const [cropSize, setCropSize] = useState(1);
  const [imgDims, setImgDims] = useState<{ w: number; h: number } | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{ mode: "move" | "resize"; startX: number; startY: number; startCropX: number; startCropY: number; startSize: number } | null>(null);

  // Compute maximum crop offsets so the square stays inside the image
  // maxCropX (in image-width fraction) = (imgW - side) / imgW
  // where side = cropSize * min(imgW, imgH)

  // reset crop when new photo arrives
  useEffect(() => {
    if (photoReceived && photoData) {
      loadImage(photoData).then((img) => {
        setImgDims({ w: img.width, h: img.height });
        const minSide = Math.min(img.width, img.height);
        // center the square, default to full min size
        setCropSize(1);
        setCropX((img.width - minSide) / (2 * img.width));
        setCropY((img.height - minSide) / (2 * img.height));
      });
    }
  }, [photoReceived, photoData]);

  const handleDragStart = useCallback(
    (mode: "move" | "resize") => (e: React.PointerEvent) => {
      e.preventDefault();
      e.stopPropagation();
      (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
      dragRef.current = {
        mode,
        startX: e.clientX,
        startY: e.clientY,
        startCropX: cropX,
        startCropY: cropY,
        startSize: cropSize,
      };
    },
    [cropX, cropY, cropSize],
  );

  const handleDragMove = useCallback(
    (e: React.PointerEvent) => {
      if (!dragRef.current || !containerRef.current || !imgDims) return;
      const rect = containerRef.current.getBoundingClientRect();
      const dx = (e.clientX - dragRef.current.startX) / rect.width;
      const dy = (e.clientY - dragRef.current.startY) / rect.height;
      const minSide = Math.min(imgDims.w, imgDims.h);

      if (dragRef.current.mode === "move") {
        const side = dragRef.current.startSize * minSide;
        const maxX = (imgDims.w - side) / imgDims.w;
        const maxY = (imgDims.h - side) / imgDims.h;
        setCropX(Math.max(0, Math.min(maxX, dragRef.current.startCropX + dx)));
        setCropY(Math.max(0, Math.min(maxY, dragRef.current.startCropY + dy)));
      } else {
        // resize — use the larger of dx/dy in image-pixel space to keep square
        const maxIdx = Math.max(
          Math.abs(dx) * imgDims.w,
          Math.abs(dy) * imgDims.h,
        );
        const sign = Math.sign(
          (dx * imgDims.w + dy * imgDims.h) > 0 ? 1 : -1,
        );
        const deltaPx = sign * maxIdx;
        const newSidePx = Math.max(
          minSide * 0.2,
          Math.min(minSide, dragRef.current.startSize * minSide + deltaPx),
        );
        const newSize = newSidePx / minSide;
        // keep top-left position but clamp so the square stays inside the image
        const maxX = (imgDims.w - newSidePx) / imgDims.w;
        const maxY = (imgDims.h - newSidePx) / imgDims.h;
        setCropSize(newSize);
        setCropX(Math.max(0, Math.min(maxX, dragRef.current.startCropX)));
        setCropY(Math.max(0, Math.min(maxY, dragRef.current.startCropY)));
      }
    },
    [imgDims],
  );

  const handleDragEnd = useCallback((e: React.PointerEvent) => {
    dragRef.current = null;
    (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
  }, []);

  useEffect(() => {
    if (open && !session) {
      setStarting(true);
      setPhotoReceived(false);
      photoReceivedRef.current = false;
      startSession().finally(() => setStarting(false));
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const api = window.electronAPI;
    if (!api) return;

    const unsub = api.onPhotoDeviceEvent((event) => {
      switch (event.type) {
        case "device-connected":
          setDevice(event.device);
          break;
        case "device-disconnected":
          if (!photoReceivedRef.current) {
            setDevice(null);
            setStatus("disconnected");
          }
          break;
        case "photo-received":
          setPhotoData(event.photoData);
          photoReceivedRef.current = true;
          setPhotoReceived(true);
          break;
      }
    });

    return unsub;
  }, [open]);

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

  const [saving, setSaving] = useState(false);

  const handleClose = () => {
    destroySession();
    onClose();
  };

  const handleSave = async () => {
    if (!photoData) return;
    setSaving(true);
    try {
      const cropped = await cropToSquareDataUrl(photoData, cropX, cropY, cropSize);
      await onPhotoComplete(cropped);
      destroySession();
      onClose();
    } catch {
      setSaving(false);
    }
  };

  const handleRestart = () => {
    setStarting(true);
    setPhotoReceived(false);
    photoReceivedRef.current = false;
    setPhotoData(null);
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
            <Camera className="h-5 w-5 text-primary" />
            <h2 className="text-base font-semibold">Capture photo</h2>
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

            <div className="flex flex-col items-center gap-3 py-2">
              <div className="rounded-lg border border-border bg-white p-3">
                <img src={session.qrDataUrl} alt="QR Code" className="w-48 h-48" />
              </div>
              <p className="text-xs text-muted-foreground text-center">
                Scannez ce QR code avec l'application Android
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="h-px flex-1 bg-border" />
              <span className="text-xs text-muted-foreground">OU</span>
              <div className="h-px flex-1 bg-border" />
            </div>

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

            {photoReceived && photoData ? (
              <>
                <div className="flex items-center gap-2 rounded-lg border border-green-500/30 bg-green-500/10 p-3">
                  <Crop className="h-4 w-4 text-green-600" />
                  <p className="text-sm font-medium text-green-700 dark:text-green-400">
                    Ajustez le cadrage carré, puis cliquez sur "Enregistrer"
                  </p>
                </div>

                <div
                  ref={containerRef}
                  className="relative rounded-lg overflow-hidden border-2 border-border bg-black select-none"
                  onPointerMove={handleDragMove}
                >
                  <img
                    src={photoData}
                    alt="Photo capturée"
                    className="w-full h-auto pointer-events-none"
                    draggable={false}
                  />
                  {(() => {
                    if (!imgDims) return null;
                    const minSide = Math.min(imgDims.w, imgDims.h);
                    const sidePx = cropSize * minSide;
                    const boxWPct = (sidePx / imgDims.w) * 100;
                    const boxHPct = (sidePx / imgDims.h) * 100;
                    const leftPct = cropX * 100;
                    const topPct = cropY * 100;

                    return (
                      <>
                        {/* Dark mask using four divs around the square */}
                        {/* top band */}
                        <div className="absolute left-0 top-0 w-full pointer-events-none bg-black/55" style={{ height: `${topPct}%` }} />
                        {/* bottom band */}
                        <div className="absolute left-0 pointer-events-none bg-black/55" style={{ top: `${topPct + boxHPct}%`, bottom: 0, width: "100%" }} />
                        {/* left band */}
                        <div className="absolute top-0 left-0 pointer-events-none bg-black/55" style={{ width: `${leftPct}%`, top: `${topPct}%`, height: `${boxHPct}%` }} />
                        {/* right band */}
                        <div className="absolute pointer-events-none bg-black/55" style={{ left: `${leftPct + boxWPct}%`, top: `${topPct}%`, height: `${boxHPct}%`, right: 0 }} />

                        {/* Crop square */}
                        <div
                          className="absolute border-2 border-white cursor-move"
                          style={{
                            left: `${leftPct}%`,
                            top: `${topPct}%`,
                            width: `${boxWPct}%`,
                            height: `${boxHPct}%`,
                          }}
                          onPointerDown={handleDragStart("move")}
                          onPointerUp={handleDragEnd}
                        >
                          {/* corner grid lines */}
                          <div className="absolute left-1/3 top-0 bottom-0 w-px bg-white/30" />
                          <div className="absolute left-2/3 top-0 bottom-0 w-px bg-white/30" />
                          <div className="absolute top-1/3 left-0 right-0 h-px bg-white/30" />
                          <div className="absolute top-2/3 left-0 right-0 h-px bg-white/30" />

                          {/* center crosshair */}
                          <div className="absolute inset-0 flex items-center justify-center">
                            <div className="w-0.5 h-6 border-l border-white/50" />
                          </div>
                          <div className="absolute inset-0 flex items-center justify-center">
                            <div className="h-0.5 w-6 border-t border-white/50" />
                          </div>

                          {/* resize handle — bottom-right corner */}
                          <div
                            className="absolute -bottom-1.5 -right-1.5 h-4 w-4 rounded-full border-2 border-white bg-primary cursor-se-resize"
                            onPointerDown={handleDragStart("resize")}
                            onPointerUp={handleDragEnd}
                          />
                        </div>
                      </>
                    );
                  })()}
                </div>

                <p className="text-xs text-muted-foreground text-center">
                  Glissez le carré pour déplacer • Glissez le coin pour redimensionner
                </p>
              </>
            ) : (
              <div className="rounded-lg border-2 border-dashed border-border p-8 text-center min-h-[200px] flex flex-col items-center justify-center">
                <Camera className="h-12 w-12 text-muted-foreground mb-3" />
                <p className="text-sm text-muted-foreground">
                  En attente de la capture photo sur l'appareil Android...
                </p>
              </div>
            )}

            <p className="text-xs text-muted-foreground text-center">
              {photoReceived
                ? "Cadrage carré appliqué automatiquement lors de l'enregistrement."
                : "L'utilisateur prend une photo sur l'appareil Android. L'image apparaîtra ici après validation."}
            </p>

            <div className="flex justify-between">
              <Button onClick={handleRestart} variant="ghost" size="sm" className="gap-1 text-xs">
                <RefreshCw className="h-3 w-3" />
                Nouvelle session
              </Button>
              {photoReceived && (
                <Button onClick={handleSave} size="sm" className="gap-1" disabled={saving}>
                  {saving ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <CheckCircle2 className="h-4 w-4" />
                  )}
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
