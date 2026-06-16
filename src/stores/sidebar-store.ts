import { create } from "zustand";
import { persist } from "zustand/middleware";

interface SidebarState {
  isOpen: boolean;
  width: number;
  toggle: () => void;
  setOpen: (open: boolean) => void;
  setWidth: (width: number) => void;
}

export const useSidebarStore = create<SidebarState>()(
  persist(
    (set) => ({
      isOpen: true,
      width: 260,
      toggle: () => set((state) => ({ isOpen: !state.isOpen })),
      setOpen: (open) => set({ isOpen: open }),
      setWidth: (width) => set({ width: Math.max(200, Math.min(400, width)) }),
    }),
    { name: "gescom-sidebar" },
  ),
);
