import { useEffect } from "react";
import { X } from "lucide-react";
import { QrLoginPanel } from "@/components/auth/qr-login-panel";
import { useNotificationStore } from "@/stores/notification-store";
import type { User } from "@/types";

interface PhonePairingDialogProps {
  open: boolean;
  onClose: () => void;
}

/**
 * Dialog shown from the desktop (when already logged in) to let a phone
 * scan a QR code and log in. The desktop displays the QR; the phone scans
 * it, sees the desktop's identity, and approves. After approval the phone
 * receives its own tokens directly from the approve endpoint.
 */
export function PhonePairingDialog({ open, onClose }: PhonePairingDialogProps) {
  const { addNotification } = useNotificationStore();

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const handleSuccess = (_user: User, _accessToken: string, _refreshToken: string) => {
    // The phone receives tokens directly via the approve endpoint (device_type=android).
    // The desktop only needs to close the dialog and notify the user.
    addNotification(
      "success",
      "Téléphone connecté",
      "Le téléphone a été authentifié avec succès.",
    );
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={onClose}>
      <div className="fixed inset-0 bg-black/60" />
      <div
        className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-lg"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 text-muted-foreground hover:text-foreground transition-colors"
        >
          <X className="h-4 w-4" />
        </button>

        <QrLoginPanel
          deviceType="android"
          onSuccess={handleSuccess}
          onCancel={onClose}
        />
      </div>
    </div>
  );
}
