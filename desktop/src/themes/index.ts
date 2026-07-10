import type { ThemeId, CustomTheme } from "@/types";

export interface ThemeDefinition {
  id: ThemeId;
  name: string;
  type: "light" | "dark";
  colors: Record<string, string>;
}

const builtInThemes: ThemeDefinition[] = [
  {
    id: "dark",
    name: "Dark",
    type: "dark",
    colors: {
      "--background": "240 10% 3.9%",
      "--foreground": "0 0% 98%",
      "--card": "240 10% 5.9%",
      "--card-foreground": "0 0% 98%",
      "--popover": "240 10% 5.9%",
      "--popover-foreground": "0 0% 98%",
      "--primary": "217 91% 60%",
      "--primary-foreground": "0 0% 100%",
      "--secondary": "240 3.7% 15.9%",
      "--secondary-foreground": "0 0% 98%",
      "--muted": "240 3.7% 15.9%",
      "--muted-foreground": "240 5% 64.9%",
      "--accent": "240 3.7% 15.9%",
      "--accent-foreground": "0 0% 98%",
      "--destructive": "0 62.8% 30.6%",
      "--destructive-foreground": "0 0% 98%",
      "--border": "240 3.7% 15.9%",
      "--input": "240 3.7% 15.9%",
      "--ring": "224 76% 48%",
      "--radius": "0.75rem",
      "--sidebar": "240 10% 5.9%",
      "--sidebar-foreground": "240 5% 64.9%",
      "--sidebar-border": "240 3.7% 15.9%",
      "--sidebar-accent": "240 3.7% 15.9%",
      "--sidebar-accent-foreground": "0 0% 98%",
      "--command": "240 10% 8%",
      "--command-foreground": "0 0% 98%",
      "--command-border": "240 3.7% 20%",
      "--titlebar": "240 10% 5.9%",
      "--titlebar-foreground": "0 0% 98%",
      "--titlebar-border": "240 3.7% 15.9%",
      "--statusbar": "240 3.7% 15.9%",
      "--statusbar-foreground": "240 5% 64.9%",
      "--statusbar-border": "240 3.7% 15.9%",
      "--selection-background": "217 91% 60%",
      "--selection-foreground": "0 0% 100%",
    },
  },
  {
    id: "light",
    name: "Light",
    type: "light",
    colors: {
      "--background": "0 0% 100%",
      "--foreground": "0 0% 10%",
      "--card": "0 0% 100%",
      "--card-foreground": "0 0% 10%",
      "--popover": "0 0% 100%",
      "--popover-foreground": "0 0% 10%",
      "--primary": "0 0% 10%",
      "--primary-foreground": "0 0% 100%",
      "--secondary": "0 0% 95%",
      "--secondary-foreground": "0 0% 10%",
      "--muted": "0 0% 95%",
      "--muted-foreground": "0 0% 45%",
      "--accent": "0 0% 92%",
      "--accent-foreground": "0 0% 10%",
      "--destructive": "0 84% 56%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "0 0% 88%",
      "--input": "0 0% 88%",
      "--ring": "0 0% 10%",
      "--radius": "0.75rem",
      "--sidebar": "0 0% 96%",
      "--sidebar-foreground": "0 0% 26%",
      "--sidebar-border": "0 0% 88%",
      "--sidebar-accent": "0 0% 88%",
      "--sidebar-accent-foreground": "0 0% 10%",
      "--command": "0 0% 100%",
      "--command-foreground": "0 0% 10%",
      "--command-border": "0 0% 88%",
      "--titlebar": "0 0% 100%",
      "--titlebar-foreground": "0 0% 10%",
      "--titlebar-border": "0 0% 88%",
      "--statusbar": "0 0% 95%",
      "--statusbar-foreground": "0 0% 45%",
      "--statusbar-border": "0 0% 88%",
      "--selection-background": "0 0% 10%",
      "--selection-foreground": "0 0% 100%",
    },
  },
  {
    id: "clean-light",
    name: "Clean Light",
    type: "light",
    colors: {
      "--background": "220 25% 98%",
      "--foreground": "220 20% 14%",
      "--card": "0 0% 100%",
      "--card-foreground": "220 20% 14%",
      "--popover": "0 0% 100%",
      "--popover-foreground": "220 20% 14%",
      "--primary": "220 15% 40%",
      "--primary-foreground": "0 0% 100%",
      "--secondary": "220 20% 94%",
      "--secondary-foreground": "220 15% 30%",
      "--muted": "220 15% 94%",
      "--muted-foreground": "220 10% 50%",
      "--accent": "220 20% 90%",
      "--accent-foreground": "220 15% 25%",
      "--destructive": "0 84% 56%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "220 15% 86%",
      "--input": "220 15% 86%",
      "--ring": "220 15% 40%",
      "--radius": "0.75rem",
      "--sidebar": "220 20% 96%",
      "--sidebar-foreground": "220 10% 35%",
      "--sidebar-border": "220 15% 86%",
      "--sidebar-accent": "220 15% 30%",
      "--sidebar-accent-foreground": "0 0% 100%",
      "--command": "0 0% 100%",
      "--command-foreground": "220 20% 14%",
      "--command-border": "220 15% 86%",
      "--titlebar": "0 0% 100%",
      "--titlebar-foreground": "220 20% 14%",
      "--titlebar-border": "220 15% 86%",
      "--statusbar": "220 20% 94%",
      "--statusbar-foreground": "220 10% 50%",
      "--statusbar-border": "220 15% 86%",
      "--selection-background": "220 15% 40%",
      "--selection-foreground": "0 0% 100%",
    },
  },
  {
    id: "warm-light",
    name: "Warm Light",
    type: "light",
    colors: {
      "--background": "35 30% 96%",
      "--foreground": "25 40% 15%",
      "--card": "35 25% 98%",
      "--card-foreground": "25 40% 15%",
      "--popover": "35 25% 98%",
      "--popover-foreground": "25 40% 15%",
      "--primary": "25 80% 50%",
      "--primary-foreground": "0 0% 100%",
      "--secondary": "30 25% 90%",
      "--secondary-foreground": "25 40% 20%",
      "--muted": "30 20% 90%",
      "--muted-foreground": "25 20% 45%",
      "--accent": "30 30% 86%",
      "--accent-foreground": "25 40% 18%",
      "--destructive": "0 70% 50%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "30 25% 82%",
      "--input": "30 25% 82%",
      "--ring": "25 80% 50%",
      "--radius": "0.75rem",
      "--sidebar": "35 25% 94%",
      "--sidebar-foreground": "25 25% 30%",
      "--sidebar-border": "30 25% 82%",
      "--sidebar-accent": "25 60% 45%",
      "--sidebar-accent-foreground": "0 0% 100%",
      "--command": "35 25% 98%",
      "--command-foreground": "25 40% 15%",
      "--command-border": "30 25% 82%",
      "--titlebar": "35 25% 98%",
      "--titlebar-foreground": "25 40% 15%",
      "--titlebar-border": "30 25% 82%",
      "--statusbar": "30 25% 90%",
      "--statusbar-foreground": "25 20% 45%",
      "--statusbar-border": "30 25% 82%",
      "--selection-background": "25 80% 50%",
      "--selection-foreground": "0 0% 100%",
    },
  },
  {
    id: "high-contrast",
    name: "High Contrast",
    type: "dark",
    colors: {
      "--background": "0 0% 0%",
      "--foreground": "0 0% 100%",
      "--card": "0 0% 5%",
      "--card-foreground": "0 0% 100%",
      "--popover": "0 0% 5%",
      "--popover-foreground": "0 0% 100%",
      "--primary": "217 100% 70%",
      "--primary-foreground": "0 0% 0%",
      "--secondary": "0 0% 15%",
      "--secondary-foreground": "0 0% 100%",
      "--muted": "0 0% 12%",
      "--muted-foreground": "0 0% 80%",
      "--accent": "217 100% 70%",
      "--accent-foreground": "0 0% 0%",
      "--destructive": "0 100% 60%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "0 0% 40%",
      "--input": "0 0% 40%",
      "--ring": "217 100% 70%",
      "--radius": "0.75rem",
      "--sidebar": "0 0% 3%",
      "--sidebar-foreground": "0 0% 100%",
      "--sidebar-border": "0 0% 40%",
      "--sidebar-accent": "217 100% 70%",
      "--sidebar-accent-foreground": "0 0% 0%",
      "--command": "0 0% 5%",
      "--command-foreground": "0 0% 100%",
      "--command-border": "0 0% 40%",
      "--titlebar": "0 0% 3%",
      "--titlebar-foreground": "0 0% 100%",
      "--titlebar-border": "0 0% 40%",
      "--statusbar": "0 0% 8%",
      "--statusbar-foreground": "0 0% 100%",
      "--statusbar-border": "0 0% 40%",
      "--selection-background": "217 100% 70%",
      "--selection-foreground": "0 0% 0%",
    },
  },
  {
    id: "ondark",
    name: "Ondark",
    type: "dark",
    colors: {
      "--background": "190 60% 3%",
      "--foreground": "180 20% 92%",
      "--card": "190 50% 5%",
      "--card-foreground": "180 20% 92%",
      "--popover": "190 50% 5%",
      "--popover-foreground": "180 20% 92%",
      "--primary": "175 80% 42%",
      "--primary-foreground": "190 60% 3%",
      "--secondary": "190 30% 12%",
      "--secondary-foreground": "180 20% 92%",
      "--muted": "190 25% 10%",
      "--muted-foreground": "180 10% 60%",
      "--accent": "175 80% 42%",
      "--accent-foreground": "190 60% 3%",
      "--destructive": "0 80% 50%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "190 25% 18%",
      "--input": "190 25% 18%",
      "--ring": "175 80% 42%",
      "--radius": "0.75rem",
      "--sidebar": "190 60% 4%",
      "--sidebar-foreground": "180 10% 65%",
      "--sidebar-border": "190 25% 14%",
      "--sidebar-accent": "175 70% 36%",
      "--sidebar-accent-foreground": "0 0% 100%",
      "--command": "190 50% 6%",
      "--command-foreground": "180 20% 92%",
      "--command-border": "190 25% 18%",
      "--titlebar": "190 60% 4%",
      "--titlebar-foreground": "180 20% 92%",
      "--titlebar-border": "190 25% 14%",
      "--statusbar": "190 30% 8%",
      "--statusbar-foreground": "180 10% 60%",
      "--statusbar-border": "190 25% 14%",
      "--selection-background": "175 80% 42%",
      "--selection-foreground": "0 0% 100%",
    },
  },
  {
    id: "matrix",
    name: "Matrix",
    type: "dark",
    colors: {
      "--background": "120 100% 3%",
      "--foreground": "120 80% 65%",
      "--card": "120 80% 5%",
      "--card-foreground": "120 80% 65%",
      "--popover": "120 80% 5%",
      "--popover-foreground": "120 80% 65%",
      "--primary": "120 100% 50%",
      "--primary-foreground": "120 100% 3%",
      "--secondary": "120 50% 10%",
      "--secondary-foreground": "120 80% 65%",
      "--muted": "120 40% 10%",
      "--muted-foreground": "120 50% 40%",
      "--accent": "120 40% 16%",
      "--accent-foreground": "120 80% 60%",
      "--destructive": "0 90% 45%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "120 40% 16%",
      "--input": "120 40% 16%",
      "--ring": "120 100% 50%",
      "--radius": "0.75rem",
      "--sidebar": "120 100% 2.5%",
      "--sidebar-foreground": "120 50% 45%",
      "--sidebar-border": "120 40% 13%",
      "--sidebar-accent": "120 40% 16%",
      "--sidebar-accent-foreground": "120 80% 60%",
      "--command": "120 80% 5%",
      "--command-foreground": "120 80% 65%",
      "--command-border": "120 40% 16%",
      "--titlebar": "120 100% 2.5%",
      "--titlebar-foreground": "120 80% 65%",
      "--titlebar-border": "120 40% 13%",
      "--statusbar": "120 50% 7%",
      "--statusbar-foreground": "120 50% 40%",
      "--statusbar-border": "120 40% 13%",
      "--selection-background": "120 100% 50%",
      "--selection-foreground": "120 100% 3%",
    },
  },
  {
    id: "monokai",
    name: "Monokai",
    type: "dark",
    colors: {
      "--background": "270 12% 12%",
      "--foreground": "60 20% 90%",
      "--card": "270 10% 15%",
      "--card-foreground": "60 20% 90%",
      "--popover": "270 10% 15%",
      "--popover-foreground": "60 20% 90%",
      "--primary": "270 80% 60%",
      "--primary-foreground": "0 0% 100%",
      "--secondary": "270 8% 22%",
      "--secondary-foreground": "60 20% 90%",
      "--muted": "270 8% 20%",
      "--muted-foreground": "60 10% 60%",
      "--accent": "270 80% 60%",
      "--accent-foreground": "0 0% 100%",
      "--destructive": "0 80% 55%",
      "--destructive-foreground": "0 0% 100%",
      "--border": "270 8% 28%",
      "--input": "270 8% 28%",
      "--ring": "270 80% 60%",
      "--radius": "0.75rem",
      "--sidebar": "270 14% 10%",
      "--sidebar-foreground": "60 10% 65%",
      "--sidebar-border": "270 8% 24%",
      "--sidebar-accent": "270 70% 50%",
      "--sidebar-accent-foreground": "0 0% 100%",
      "--command": "270 10% 16%",
      "--command-foreground": "60 20% 90%",
      "--command-border": "270 8% 28%",
      "--titlebar": "270 14% 10%",
      "--titlebar-foreground": "60 20% 90%",
      "--titlebar-border": "270 8% 24%",
      "--statusbar": "270 8% 18%",
      "--statusbar-foreground": "60 10% 60%",
      "--statusbar-border": "270 8% 24%",
      "--selection-background": "270 80% 60%",
      "--selection-foreground": "0 0% 100%",
    },
  },
];

export const themes: ThemeDefinition[] = [...builtInThemes];

export const themeDefaults: Record<string, string> = {
  "--radius": "0.75rem",
};

export function getTheme(id: ThemeId): ThemeDefinition {
  const builtIn = builtInThemes.find((t) => t.id === id);
  if (builtIn) return builtIn;

  const custom = customThemes.find((t) => t.id === id);
  if (custom) return custom;

  return builtInThemes[0];
}

let customThemes: ThemeDefinition[] = [];

export function loadCustomThemes(): void {
  try {
    const stored = localStorage.getItem("opus-custom-themes");
    if (stored) {
      const parsed: CustomTheme[] = JSON.parse(stored);
      customThemes = parsed.map((ct) => ({
        id: ct.id,
        name: ct.name,
        type: ct.type,
        colors: ct.colors,
      }));
      rebuildThemes();
    }
  } catch {
    customThemes = [];
  }
}

export function getCustomThemes(): ThemeDefinition[] {
  return [...customThemes];
}

export function saveCustomTheme(theme: CustomTheme): void {
  const idx = customThemes.findIndex((t) => t.id === theme.id);
  if (idx >= 0) {
    customThemes[idx] = { ...theme, id: theme.id, type: theme.type };
  } else {
    customThemes.push({ ...theme, id: theme.id, type: theme.type });
  }
  persistCustomThemes();
  rebuildThemes();
}

export function deleteCustomTheme(id: `custom-${string}`): void {
  customThemes = customThemes.filter((t) => t.id !== id);
  persistCustomThemes();
  rebuildThemes();
}

function persistCustomThemes(): void {
  localStorage.setItem("opus-custom-themes", JSON.stringify(customThemes));
}

function rebuildThemes(): void {
  themes.length = 0;
  themes.push(...builtInThemes, ...customThemes);
}

loadCustomThemes();
