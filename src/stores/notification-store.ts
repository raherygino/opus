import { create } from "zustand";
import type { Notification, NotificationType } from "@/types";
import { generateId } from "@/lib/utils";

interface NotificationState {
  notifications: Notification[];
  addNotification: (
    type: NotificationType,
    title: string,
    message?: string,
    duration?: number,
  ) => void;
  removeNotification: (id: string) => void;
  clearAll: () => void;
}

export const useNotificationStore = create<NotificationState>()((set) => ({
  notifications: [],
  addNotification: (type, title, message, duration = 4000) => {
    const id = generateId();
    set((state) => ({
      notifications: [...state.notifications, { id, type, title, message, duration }],
    }));
    if (duration > 0) {
      setTimeout(() => {
        set((state) => ({
          notifications: state.notifications.filter((n) => n.id !== id),
        }));
      }, duration);
    }
  },
  removeNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),
  clearAll: () => set({ notifications: [] }),
}));
