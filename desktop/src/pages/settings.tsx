import { useState, useEffect, useCallback, useRef } from "react";
import { useThemeStore } from "@/stores/theme-store";
import { themes, getTheme } from "@/themes";
import type { CustomTheme } from "@/types";
import { COLOR_TOKENS } from "@/types";
import { motion } from "framer-motion";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import {
  Monitor,
  Moon,
  Sun,
  Type,
  Keyboard,
  Palette,
  Bell,
  Database,
  Users,
  Shield,
  Contrast,
  Sparkles,
  Terminal,
  Paintbrush,
  Plus,
  Trash2,
  Copy,
  Download,
  Upload,
  Save,
  X,
  Check,
  Undo2,
  AlertCircle,
} from "lucide-react";

const sections = [
  { id: "appearance", label: "Appearance", icon: Palette },
  { id: "editor", label: "Editor", icon: Type },
  { id: "shortcuts", label: "Keyboard Shortcuts", icon: Keyboard },
  { id: "theme-creator", label: "Theme Creator", icon: Paintbrush },
  { id: "notifications", label: "Notifications", icon: Bell },
  { id: "storage", label: "Storage", icon: Database },
  { id: "account", label: "Account", icon: Users },
  { id: "privacy", label: "Privacy & Security", icon: Shield },
];

const builtInThemeIcons: Record<string, React.ElementType> = {
  dark: Moon,
  light: Sun,
  "high-contrast": Contrast,
  ondark: Sparkles,
  matrix: Terminal,
  monokai: Paintbrush,
  "clean-light": Sun,
  "warm-light": Sun,
};

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
};

const sectionItem = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0 },
};

function generateCustomId(): `custom-${string}` {
  return `custom-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 6)}`;
}

function getDefaultCustomColors(type: "light" | "dark"): Record<string, string> {
  return type === "dark"
    ? {
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
      }
    : {
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
      };
}

interface ColorPickerProps {
  value: string;
  onChange: (value: string) => void;
}

function hexToRgb(hex: string): [number, number, number] | null {
  const m = hex.replace("#", "").trim();
  if (/^[0-9a-fA-F]{3}$/.test(m)) {
    return [parseInt(m[0] + m[0], 16), parseInt(m[1] + m[1], 16), parseInt(m[2] + m[2], 16)];
  }
  if (/^[0-9a-fA-F]{6}$/.test(m)) {
    return [parseInt(m.slice(0, 2), 16), parseInt(m.slice(2, 4), 16), parseInt(m.slice(4, 6), 16)];
  }
  return null;
}

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  r /= 255; g /= 255; b /= 255;
  const max = Math.max(r, g, b), min = Math.min(r, g, b);
  let h = 0, s = 0;
  const l = (max + min) / 2;
  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r: h = (g - b) / d + (g < b ? 6 : 0); break;
      case g: h = (b - r) / d + 2; break;
      case b: h = (r - g) / d + 4; break;
    }
    h *= 60;
  }
  return [Math.round(h), Math.round(s * 100), Math.round(l * 100)];
}

function hslToHex(h: number, s: number, l: number): string {
  s /= 100; l /= 100;
  const a = s * Math.min(l, 1 - l);
  const f = (n: number) => {
    const k = (n + h / 30) % 12;
    const c = l - a * Math.max(-1, Math.min(k - 3, 9 - k, 1));
    return Math.round(255 * c).toString(16).padStart(2, "0");
  };
  return `#${f(0)}${f(8)}${f(4)}`;
}

function parseColorToHsl(input: string): [number, number, number] | null {
  const trimmed = input.trim();
  if (!trimmed) return null;
  // hex
  if (/^#?[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(trimmed)) {
    const rgb = hexToRgb(trimmed.startsWith("#") ? trimmed : "#" + trimmed);
    if (rgb) return rgbToHsl(rgb[0], rgb[1], rgb[2]);
  }
  // rgb(r, g, b) or rgb(r g b)
  const rgbMatch = trimmed.match(/^rgba?\(\s*(\d{1,3})\s*[,\s]\s*(\d{1,3})\s*[,\s]\s*(\d{1,3})/i);
  if (rgbMatch) {
    return rgbToHsl(parseInt(rgbMatch[1]), parseInt(rgbMatch[2]), parseInt(rgbMatch[3]));
  }
  // hsl(h, s%, l%) or hsl(h s% l%)
  const hslMatch = trimmed.match(/^hsla?\(\s*(\d{1,3}(?:\.\d+)?)\s*[,\s]\s*(\d{1,3}(?:\.\d+)?)%\s*[,\s]\s*(\d{1,3}(?:\.\d+)?)%/i);
  if (hslMatch) {
    return [Math.round(parseFloat(hslMatch[1])), Math.round(parseFloat(hslMatch[2])), Math.round(parseFloat(hslMatch[3]))];
  }
  // already in internal "H S% L%" format
  const parts = trimmed.split(/\s+/);
  if (parts.length >= 3 && parts[1].endsWith("%") && parts[2].endsWith("%")) {
    return [Math.round(parseFloat(parts[0])), Math.round(parseFloat(parts[1])), Math.round(parseFloat(parts[2]))];
  }
  return null;
}

function ColorPicker({ value, onChange }: ColorPickerProps) {
  const [open, setOpen] = useState(false);
  const [textValue, setTextValue] = useState("");
  const svRef = useRef<HTMLDivElement>(null);
  const hueRef = useRef<HTMLDivElement>(null);
  const dragging = useRef<"sv" | "hue" | null>(null);

  const parts = value.split(/\s+/);
  const h = Math.round(parseFloat(parts[0] || "0"));
  const s = Math.round(parseFloat(parts[1] || "0"));
  const l = Math.round(parseFloat(parts[2] || "0"));

  const currentColor = `hsl(${h} ${s}% ${l}%)`;
  const hexValue = hslToHex(h, s, l);

  const updateFromPosition = useCallback(
    (clientX: number, clientY: number, area: "sv" | "hue") => {
      if (area === "sv" && svRef.current) {
        const rect = svRef.current.getBoundingClientRect();
        const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
        const y = Math.max(0, Math.min(1, (clientY - rect.top) / rect.height));
        const newSat = Math.round(x * 100);
        const newLit = Math.round((1 - y) * 100);
        onChange(`${h} ${newSat}% ${newLit}%`);
      } else if (area === "hue" && hueRef.current) {
        const rect = hueRef.current.getBoundingClientRect();
        const x = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
        const newHue = Math.round(x * 360);
        onChange(`${newHue} ${s}% ${l}%`);
      }
    },
    [h, s, l, onChange],
  );

  const handleSVMouseDown = useCallback(
    (e: React.MouseEvent) => {
      dragging.current = "sv";
      updateFromPosition(e.clientX, e.clientY, "sv");
    },
    [updateFromPosition],
  );

  const handleHueMouseDown = useCallback(
    (e: React.MouseEvent) => {
      dragging.current = "hue";
      updateFromPosition(e.clientX, e.clientY, "hue");
    },
    [updateFromPosition],
  );

  useEffect(() => {
    if (!open) return;
    const handleMouseMove = (e: MouseEvent) => {
      if (!dragging.current) return;
      e.preventDefault();
      updateFromPosition(e.clientX, e.clientY, dragging.current);
    };
    const handleMouseUp = () => {
      dragging.current = null;
    };
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [open, updateFromPosition]);

  useEffect(() => {
    if (!open) {
      dragging.current = null;
    }
  }, [open]);

  return (
    <div className="relative flex items-center gap-2">
      <button
        type="button"
        className="h-7 w-7 shrink-0 rounded-md border-2 border-border overflow-hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        style={{ background: currentColor }}
        onClick={() => setOpen(!open)}
        title="Open color picker"
      />
      <input
        type="number"
        value={h}
        min={0}
        max={360}
        className="h-7 w-14 rounded border border-border bg-transparent px-1.5 text-[11px] text-center tabular-nums"
        onChange={(e) => {
          const v = Math.max(0, Math.min(360, parseInt(e.target.value) || 0));
          onChange(`${v} ${s}% ${l}%`);
        }}
      />
      <input
        type="number"
        value={s}
        min={0}
        max={100}
        className="h-7 w-14 rounded border border-border bg-transparent px-1.5 text-[11px] text-center tabular-nums"
        onChange={(e) => {
          const v = Math.max(0, Math.min(100, parseInt(e.target.value) || 0));
          onChange(`${h} ${v}% ${l}%`);
        }}
      />
      <input
        type="number"
        value={l}
        min={0}
        max={100}
        className="h-7 w-14 rounded border border-border bg-transparent px-1.5 text-[11px] text-center tabular-nums"
        onChange={(e) => {
          const v = Math.max(0, Math.min(100, parseInt(e.target.value) || 0));
          onChange(`${h} ${s}% ${v}%`);
        }}
      />
      {open && (
        <div className="absolute top-full right-0 z-50 mt-1 w-56 rounded-xl border border-border bg-popover p-3 shadow-xl space-y-3">
          {/* Saturation-Lightness area */}
          <div
            ref={svRef}
            className="relative h-40 w-full rounded-lg cursor-crosshair"
            style={{
              background: `
                linear-gradient(to top, #000, transparent),
                linear-gradient(to right, #fff, transparent),
                hsl(${h}, 100%, 50%)
              `,
            }}
            onMouseDown={handleSVMouseDown}
          >
            <div
              className="absolute h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white shadow-md pointer-events-none"
              style={{
                left: `${s}%`,
                top: `${100 - l}%`,
                background: currentColor,
              }}
            />
          </div>

          {/* Hue slider */}
          <div
            ref={hueRef}
            className="relative h-3 w-full rounded-full cursor-pointer"
            style={{
              background:
                "linear-gradient(to right, " +
                Array.from({ length: 7 }, (_, i) => {
                  const deg = Math.round((i / 6) * 360);
                  return `hsl(${deg}, 100%, 50%)`;
                }).join(", ") +
                ")",
            }}
            onMouseDown={handleHueMouseDown}
          >
            <div
              className="absolute top-1/2 h-4 w-4 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white shadow-md pointer-events-none"
              style={{
                left: `${(h / 360) * 100}%`,
                background: `hsl(${h}, 100%, 50%)`,
              }}
            />
          </div>

          {/* Color text input (hex / rgb / hsl) */}
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <div
                className="h-5 w-5 shrink-0 rounded border"
                style={{ background: currentColor }}
              />
              <input
                type="text"
                value={textValue || hexValue}
                onChange={(e) => {
                  setTextValue(e.target.value);
                }}
                onBlur={(e) => {
                  const parsed = parseColorToHsl(e.target.value);
                  if (parsed) {
                    onChange(`${parsed[0]} ${parsed[1]}% ${parsed[2]}%`);
                  }
                  setTextValue("");
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    const parsed = parseColorToHsl((e.target as HTMLInputElement).value);
                    if (parsed) {
                      onChange(`${parsed[0]} ${parsed[1]}% ${parsed[2]}%`);
                    }
                    setTextValue("");
                    (e.target as HTMLInputElement).blur();
                  }
                }}
                placeholder="#hex, rgb(), hsl()"
                className="h-7 flex-1 rounded border border-border bg-transparent px-2 text-[11px] font-mono tabular-nums focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>
            <span className="text-[10px] font-mono text-muted-foreground uppercase">
              H:{h} S:{s}% L:{l}%
            </span>
          </div>
        </div>
      )}
    </div>
  );
}

export function Settings() {
  const { theme, setTheme, saveCustomTheme, deleteCustomTheme, reloadCustomThemes } = useThemeStore();
  const [activeSection, setActiveSection] = useState("appearance");
  const [fontSize, setFontSize] = useState("14");
  const [editingTheme, setEditingTheme] = useState<CustomTheme | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [showImportError, setShowImportError] = useState("");
  const [localCustomThemes, setLocalCustomThemes] = useState<CustomTheme[]>([]);

  useEffect(() => {
    const stored = localStorage.getItem("opus-custom-themes");
    if (stored) {
      try {
        setLocalCustomThemes(JSON.parse(stored));
      } catch {
        setLocalCustomThemes([]);
      }
    }
  }, [theme]);

  const reloadLocalCustomThemes = useCallback(() => {
    const stored = localStorage.getItem("opus-custom-themes");
    if (stored) {
      try {
        setLocalCustomThemes(JSON.parse(stored));
      } catch {
        setLocalCustomThemes([]);
      }
    }
  }, []);

  const applyPreview = useCallback((colors: Record<string, string>) => {
    const root = document.documentElement;
    Object.entries(colors).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
  }, []);

  const clearPreview = useCallback(() => {
    const themeDef = getTheme(theme);
    const root = document.documentElement;
    Object.entries(themeDef.colors).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
  }, [theme]);

  const startCreating = () => {
    setEditingTheme({
      id: generateCustomId(),
      name: "New Theme",
      type: "dark",
      colors: getDefaultCustomColors("dark"),
    });
    setIsCreating(true);
    applyPreview(getDefaultCustomColors("dark"));
  };

  const startEditing = (ct: CustomTheme) => {
    setEditingTheme({ ...ct, colors: { ...ct.colors } });
    setIsCreating(false);
    applyPreview(ct.colors);
  };

  const cancelEditing = () => {
    setEditingTheme(null);
    clearPreview();
    setIsCreating(false);
  };

  const updateEditingColor = (key: string, value: string) => {
    if (!editingTheme) return;
    const updated = { ...editingTheme, colors: { ...editingTheme.colors, [key]: value } };
    setEditingTheme(updated);
    applyPreview(updated.colors);
  };

  const handleSave = () => {
    if (!editingTheme) return;
    if (!editingTheme.name.trim()) return;
    const themeToSave: CustomTheme = {
      ...editingTheme,
      colors: { ...editingTheme.colors },
    };
    if (isCreating || !localCustomThemes.find((t) => t.id === editingTheme.id)) {
      saveCustomTheme(themeToSave);
    } else {
      saveCustomTheme(themeToSave);
    }
    cancelEditing();
    reloadCustomThemes();
    reloadLocalCustomThemes();
  };

  const handleDuplicate = (ct: CustomTheme) => {
    const dup: CustomTheme = {
      id: generateCustomId(),
      name: `${ct.name} (copy)`,
      type: ct.type,
      colors: { ...ct.colors },
    };
    saveCustomTheme(dup);
    reloadCustomThemes();
    cancelEditing();
    reloadLocalCustomThemes();
  };

  const handleDelete = (id: `custom-${string}`) => {
    deleteCustomTheme(id);
    if (editingTheme?.id === id) {
      cancelEditing();
    }
    reloadCustomThemes();
    reloadLocalCustomThemes();
  };

  const handleExportTheme = (ct: CustomTheme) => {
    const blob = new Blob([JSON.stringify(ct, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${ct.name.replace(/\s+/g, "-").toLowerCase()}-theme.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImport = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = ".json";
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (ev) => {
        try {
          const data = JSON.parse(ev.target?.result as string);
          if (!data.name || !data.colors || !data.type) {
            setShowImportError("Invalid theme file. Must contain name, type, and colors.");
            return;
          }
          const imported: CustomTheme = {
            id: generateCustomId(),
            name: data.name,
            type: data.type === "light" ? "light" : "dark",
            colors: data.colors,
          };
          saveCustomTheme(imported);
          reloadCustomThemes();
          cancelEditing();
    setShowImportError("");
                          reloadLocalCustomThemes();
        } catch {
          setShowImportError("Failed to parse theme file. Invalid JSON.");
        }
      };
      reader.readAsText(file);
    };
    input.click();
  };

  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      className="mx-auto flex max-w-5xl gap-8 h-full"
    >
      <motion.aside variants={sectionItem} className="w-48 shrink-0 space-y-1">
        {sections.map((section) => {
          const Icon = section.icon;
          return (
            <button
              key={section.id}
              onClick={() => setActiveSection(section.id)}
              className={cn(
                "flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-xs font-medium transition-colors",
                activeSection === section.id
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground hover:text-foreground hover:bg-accent/50",
              )}
            >
              <Icon className="h-4 w-4" />
              {section.label}
            </button>
          );
        })}
      </motion.aside>

      <motion.div variants={sectionItem} className="flex-1 space-y-6 pb-12 overflow-y-auto">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Settings</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Manage your workspace preferences.
          </p>
        </div>

        {activeSection === "appearance" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <Card>
              <CardHeader>
                <CardTitle>Color Theme</CardTitle>
                <CardDescription>
                  Choose a color theme for the entire application interface.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {themes.map((t) => {
                    const isBuiltIn = !t.id.startsWith("custom-");
                    const Icon = isBuiltIn
                      ? builtInThemeIcons[t.id] || Palette
                      : Palette;
                    const isActive = theme === t.id;
                    return (
                      <button
                        key={t.id}
                        onClick={() => setTheme(t.id)}
                        className={cn(
                          "relative flex flex-col items-center gap-2.5 rounded-xl border-2 p-4 transition-all",
                          isActive
                            ? "border-primary bg-primary/5 shadow-sm"
                            : "border-border hover:border-muted-foreground/30 hover:bg-accent/30",
                        )}
                      >
                        <div
                          className={cn(
                            "flex h-10 w-full items-center justify-center rounded-lg",
                            t.type === "dark" ? "bg-zinc-900" : "bg-white",
                          )}
                        >
                          <div
                            className="flex h-6 w-6 items-center justify-center rounded-full"
                            style={{
                              background: `hsl(${t.colors["--primary"]})`,
                            }}
                          >
                            <Icon className="h-3 w-3" style={{ color: `hsl(${t.colors["--primary-foreground"]})` }} />
                          </div>
                        </div>
                        <div className="text-center">
                          <div className="text-xs font-medium">{t.name}</div>
                          <div className="text-[10px] text-muted-foreground capitalize">
                            {t.type}{!isBuiltIn ? " \u00B7 Custom" : ""}
                          </div>
                        </div>
                        {isActive && (
                          <Badge variant="default" className="text-[10px] px-1.5 py-0 absolute top-2 right-2">
                            Active
                          </Badge>
                        )}
                      </button>
                    );
                  })}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Sidebar</CardTitle>
                <CardDescription>
                  Configure sidebar behavior and appearance.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Default width</div>
                    <div className="text-xs text-muted-foreground">
                      Set the default sidebar width in pixels
                    </div>
                  </div>
                  <Input
                    type="number"
                    value={260}
                    className="w-20 h-8 text-center text-sm"
                    readOnly
                  />
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {activeSection === "theme-creator" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {!editingTheme ? (
              <>
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-lg font-semibold">Custom Themes</h2>
                    <p className="text-sm text-muted-foreground">
                      Create, edit, and manage your own color themes.
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={handleImport}>
                      <Upload className="h-3.5 w-3.5 mr-1.5" />
                      Import
                    </Button>
                    <Button size="sm" onClick={startCreating}>
                      <Plus className="h-3.5 w-3.5 mr-1.5" />
                      Create Theme
                    </Button>
                  </div>
                </div>

                {showImportError && (
                  <div className="flex items-center gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">
                    <AlertCircle className="h-3.5 w-3.5" />
                    {showImportError}
                    <button
                      className="ml-auto"
                      onClick={() => setShowImportError("")}
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </div>
                )}

                {localCustomThemes.length === 0 ? (
                  <Card>
                    <CardContent className="flex flex-col items-center justify-center py-12">
                      <Paintbrush className="h-8 w-8 text-muted-foreground mb-3" />
                      <p className="text-sm text-muted-foreground">
                        No custom themes yet. Click "Create Theme" to get started.
                      </p>
                    </CardContent>
                  </Card>
                ) : (
                  <div className="space-y-3">
                    {localCustomThemes.map((ct) => (
                      <Card key={ct.id} className="hover:border-primary/30 transition-colors">
                        <CardContent className="flex items-center justify-between p-4">
                          <div className="flex items-center gap-4">
                            <div className="flex gap-1">
                              {[
                                ct.colors["--primary"],
                                ct.colors["--background"],
                                ct.colors["--accent"],
                                ct.colors["--sidebar"],
                              ].map((c, i) => (
                                <div
                                  key={i}
                                  className="h-6 w-6 rounded border"
                                  style={{ background: c ? `hsl(${c})` : "#888" }}
                                />
                              ))}
                            </div>
                            <div>
                              <div className="text-sm font-medium">{ct.name}</div>
                              <div className="text-[10px] text-muted-foreground capitalize">{ct.type}</div>
                            </div>
                          </div>
                          <div className="flex items-center gap-1">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              onClick={() => setTheme(ct.id)}
                              title="Apply"
                            >
                              <Check className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              onClick={() => startEditing(ct)}
                              title="Edit"
                            >
                              <Palette className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              onClick={() => handleDuplicate(ct)}
                              title="Duplicate"
                            >
                              <Copy className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7"
                              onClick={() => handleExportTheme(ct)}
                              title="Export"
                            >
                              <Download className="h-3.5 w-3.5" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-7 w-7 text-destructive hover:text-destructive"
                              onClick={() => handleDelete(ct.id)}
                              title="Delete"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                )}
              </>
            ) : (
              <div className="space-y-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <Button variant="ghost" size="icon" onClick={cancelEditing}>
                      <Undo2 className="h-4 w-4" />
                    </Button>
                    <h2 className="text-lg font-semibold">
                      {isCreating ? "Create Theme" : "Edit Theme"}
                    </h2>
                  </div>
                  <div className="flex gap-2">
                    <Button variant="outline" size="sm" onClick={cancelEditing}>
                      Cancel
                    </Button>
                    <Button size="sm" onClick={handleSave}>
                      <Save className="h-3.5 w-3.5 mr-1.5" />
                      Save Theme
                    </Button>
                  </div>
                </div>

                <Card>
                  <CardHeader>
                    <CardTitle>Theme Info</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex items-center gap-4">
                      <div className="flex-1">
                        <label className="text-xs font-medium">Name</label>
                        <Input
                          value={editingTheme.name}
                          onChange={(e) =>
                            setEditingTheme({ ...editingTheme, name: e.target.value })
                          }
                          placeholder="My Theme"
                          className="mt-1"
                        />
                      </div>
                      <div className="w-32">
                        <label className="text-xs font-medium">Type</label>
                        <select
                          value={editingTheme.type}
                          onChange={(e) => {
                            const newType = e.target.value as "light" | "dark";
                            const colors = getDefaultCustomColors(newType);
                            setEditingTheme({ ...editingTheme, type: newType, colors });
                            applyPreview(colors);
                          }}
                          className="mt-1 flex h-9 w-full rounded-lg border border-input bg-transparent px-3 py-1 text-sm"
                        >
                          <option value="dark">Dark</option>
                          <option value="light">Light</option>
                        </select>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Preview</CardTitle>
                    <CardDescription>Live preview of your theme colors</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div
                      className={cn(
                        "rounded-xl border p-4 space-y-3",
                        editingTheme.type === "dark" ? "bg-zinc-900" : "bg-white",
                      )}
                      style={{
                        background: `hsl(${editingTheme.colors["--background"]})`,
                        color: `hsl(${editingTheme.colors["--foreground"]})`,
                      }}
                    >
                      <div
                        className="rounded-lg border p-3"
                        style={{
                          background: `hsl(${editingTheme.colors["--card"]})`,
                          borderColor: `hsl(${editingTheme.colors["--border"]})`,
                        }}
                      >
                        <div className="text-sm font-medium mb-1" style={{ color: `hsl(${editingTheme.colors["--card-foreground"]})` }}>
                          Preview Card
                        </div>
                        <div className="text-xs mb-3" style={{ color: `hsl(${editingTheme.colors["--muted-foreground"]})` }}>
                          Sample text showing how colors interact.
                        </div>
                        <div className="flex gap-2">
                          <div
                            className="rounded-md px-3 py-1.5 text-xs font-medium"
                            style={{
                              background: `hsl(${editingTheme.colors["--primary"]})`,
                              color: `hsl(${editingTheme.colors["--primary-foreground"]})`,
                            }}
                          >
                            Primary
                          </div>
                          <div
                            className="rounded-md px-3 py-1.5 text-xs font-medium"
                            style={{
                              background: `hsl(${editingTheme.colors["--secondary"]})`,
                              color: `hsl(${editingTheme.colors["--secondary-foreground"]})`,
                            }}
                          >
                            Secondary
                          </div>
                          <div
                            className="rounded-md px-3 py-1.5 text-xs font-medium"
                            style={{
                              background: `hsl(${editingTheme.colors["--accent"]})`,
                              color: `hsl(${editingTheme.colors["--accent-foreground"]})`,
                            }}
                          >
                            Accent
                          </div>
                        </div>
                      </div>
                      <div
                        className="rounded-lg border p-3 text-xs"
                        style={{
                          background: `hsl(${editingTheme.colors["--sidebar"]})`,
                          borderColor: `hsl(${editingTheme.colors["--sidebar-border"]})`,
                          color: `hsl(${editingTheme.colors["--sidebar-foreground"]})`,
                        }}
                      >
                        Sidebar Preview — hover state
                        <div className="mt-1 flex gap-1">
                          <span
                            className="rounded px-2 py-0.5"
                            style={{
                              background: `hsl(${editingTheme.colors["--sidebar-accent"]})`,
                              color: `hsl(${editingTheme.colors["--sidebar-accent-foreground"]})`,
                            }}
                          >
                            Active Item
                          </span>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Color Tokens</CardTitle>
                    <CardDescription>Customize HSL values for each color token</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-6">
                    {(["Base", "Surface", "Accent", "Layout"] as const).map((category) => {
                      const tokens = COLOR_TOKENS.filter((t) => t.category === category);
                      return (
                        <div key={category}>
                          <h4 className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                            {category}
                          </h4>
                          <div className="space-y-2">
                            {tokens.map((token) => (
                              <div
                                key={token.key}
                                className="flex items-center justify-between gap-4"
                              >
                                <div className="flex-1 min-w-0">
                                  <div className="text-xs font-medium">{token.label}</div>
                                  <code className="text-[10px] text-muted-foreground">
                                    {token.key}
                                  </code>
                                </div>
                                <ColorPicker
                                  value={editingTheme.colors[token.key] || "0 0% 50%"}
                                  onChange={(v) => updateEditingColor(token.key, v)}
                                />
                              </div>
                            ))}
                          </div>
                        </div>
                      );
                    })}
                  </CardContent>
                </Card>
              </div>
            )}
          </motion.div>
        )}

        {activeSection === "editor" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card>
              <CardHeader>
                <CardTitle>Editor Preferences</CardTitle>
                <CardDescription>
                  Customize the note editing experience.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Font size</div>
                    <div className="text-xs text-muted-foreground">
                      Adjust the text size in the editor
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setFontSize((s) => String(Math.max(12, Number(s) - 1)))}
                    >
                      -
                    </Button>
                    <Input
                      type="number"
                      value={fontSize}
                      onChange={(e) => setFontSize(e.target.value)}
                      className="w-16 h-8 text-center text-sm"
                    />
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => setFontSize((s) => String(Math.min(24, Number(s) + 1)))}
                    >
                      +
                    </Button>
                  </div>
                </div>
                <Separator />
                <div className="flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">Font family</div>
                    <div className="text-xs text-muted-foreground">
                      Choose your preferred font
                    </div>
                  </div>
                  <Badge variant="secondary">Inter (System)</Badge>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {activeSection === "shortcuts" && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <Card>
              <CardHeader>
                <CardTitle>Keyboard Shortcuts</CardTitle>
                <CardDescription>
                  Available keyboard shortcuts for quick actions.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="divide-y divide-border">
                  {[
                    { keys: "⌘P / Ctrl+P", action: "Command palette" },
                    { keys: "⌘B / Ctrl+B", action: "Toggle sidebar" },
                    { keys: "⌘⇧L / Ctrl+Shift+L", action: "Cycle theme" },
                    { keys: "G D", action: "Go to Dashboard" },
                    { keys: "G N", action: "Go to Notes" },
                    { keys: "G S", action: "Open Settings" },
                    { keys: "ESC", action: "Close dialogs / cancel" },
                    { keys: "↑↓", action: "Navigate command list" },
                  ].map((shortcut) => (
                    <div
                      key={shortcut.keys}
                      className="flex items-center justify-between py-2.5"
                    >
                      <span className="text-sm">{shortcut.action}</span>
                      <Badge variant="outline" className="font-mono text-[10px]">
                        {shortcut.keys}
                      </Badge>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </motion.div>
        )}

        {!["appearance", "theme-creator", "editor", "shortcuts"].includes(activeSection) && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="flex flex-col items-center justify-center py-16 text-center"
          >
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted">
              <Monitor className="h-6 w-6 text-muted-foreground" />
            </div>
            <h3 className="mt-4 text-sm font-medium">Coming soon</h3>
            <p className="mt-1 text-xs text-muted-foreground max-w-sm">
              This section is under development and will be available in a future update.
            </p>
          </motion.div>
        )}
      </motion.div>
    </motion.div>
  );
}
