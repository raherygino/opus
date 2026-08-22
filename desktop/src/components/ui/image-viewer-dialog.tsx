import { useState } from "react";
import { motion } from "framer-motion";
import { X, ZoomIn, ZoomOut } from "lucide-react";
import { Button } from "@/components/ui/button";

interface ImageViewerDialogProps {
  open: boolean;
  src: string;
  title?: string;
  onClose: () => void;
}

/**
 * Lightbox-style image viewer used to preview image attachments
 * in-app. Click the image (or the zoom button) to toggle between
 * fit-to-screen and full size.
 */
export function ImageViewerDialog({ open, src, title, onClose }: ImageViewerDialogProps) {
  const [zoomed, setZoomed] = useState(false);

  if (!open) return null;

  const handleClose = () => {
    setZoomed(false);
    onClose();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      onClick={handleClose}
    >
      <div className="fixed inset-0 bg-black/80" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative z-50 flex max-h-[90vh] max-w-[90vw] flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between gap-4 pb-2">
          <p className="text-sm text-white/80 truncate">{title ?? "Aperçu"}</p>
          <div className="flex items-center gap-1">
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-white hover:bg-white/10"
              title={zoomed ? "Ajuster à l'écran" : "Taille réelle"}
              onClick={() => setZoomed((z) => !z)}
            >
              {zoomed ? <ZoomOut className="h-4 w-4" /> : <ZoomIn className="h-4 w-4" />}
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-white hover:bg-white/10"
              onClick={handleClose}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
        </div>
        <div className="overflow-auto rounded-lg bg-black/40 flex items-center justify-center">
          <img
            src={src}
            alt={title ?? "Aperçu"}
            onClick={() => setZoomed((z) => !z)}
            className={
              zoomed
                ? "cursor-zoom-out max-w-none"
                : "cursor-zoom-in max-h-[80vh] max-w-[85vw] object-contain"
            }
            draggable={false}
          />
        </div>
      </motion.div>
    </div>
  );
}
