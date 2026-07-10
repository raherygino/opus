import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ThemeId, CustomTheme } from "@/types";
import { themes, getTheme, getCustomThemes, saveCustomTheme, deleteCustomTheme, loadCustomThemes } from "@/themes";

interface ThemeState {
  theme: ThemeId;
  setTheme: (theme: ThemeId) => void;
  cycleTheme: () => void;
  getCurrentTheme: () => (typeof themes)[number];
  getCustomThemes: () => (typeof themes)[number][];
  saveCustomTheme: (theme: CustomTheme) => void;
  deleteCustomTheme: (id: `custom-${string}`) => void;
  reloadCustomThemes: () => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set, get) => ({
      theme: "dark",
      setTheme: (theme) => set({ theme }),
      cycleTheme: () => {
        const current = get().theme;
        const ids = themes.map((t) => t.id);
        const idx = ids.indexOf(current);
        const next = ids[(idx + 1) % ids.length];
        set({ theme: next });
      },
      getCurrentTheme: () => getTheme(get().theme),
      getCustomThemes: () => getCustomThemes(),
      saveCustomTheme: (theme: CustomTheme) => {
        saveCustomTheme(theme);
      },
      deleteCustomTheme: (id: `custom-${string}`) => {
        deleteCustomTheme(id);
        const current = get().theme;
        if (current === id) {
          set({ theme: "dark" });
        }
      },
      reloadCustomThemes: () => {
        loadCustomThemes();
      },
    }),
    { name: "opus-theme" },
  ),
);
