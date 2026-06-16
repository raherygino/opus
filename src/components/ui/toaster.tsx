import { useNotificationStore } from "@/stores/notification-store";
import {
  Toast,
  ToastProvider,
  ToastViewport,
  ToastTitle,
  ToastDescription,
  ToastClose,
} from "./toast";
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from "lucide-react";

const iconMap = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

export function Toaster() {
  const { notifications, removeNotification } = useNotificationStore();

  return (
    <ToastProvider>
      {notifications.map((notification) => {
        const Icon = iconMap[notification.type];
        return (
          <Toast
            key={notification.id}
            variant={notification.type}
            duration={notification.duration}
            onOpenChange={() => removeNotification(notification.id)}
          >
            <div className="flex items-start gap-3">
              <Icon className="h-5 w-5 shrink-0 mt-0.5" />
              <div className="flex flex-col gap-1">
                <ToastTitle>{notification.title}</ToastTitle>
                {notification.message && (
                  <ToastDescription>{notification.message}</ToastDescription>
                )}
              </div>
            </div>
            <ToastClose onClick={() => removeNotification(notification.id)}>
              <X className="h-4 w-4" />
            </ToastClose>
          </Toast>
        );
      })}
      <ToastViewport />
    </ToastProvider>
  );
}
