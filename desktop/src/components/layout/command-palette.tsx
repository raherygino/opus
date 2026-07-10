import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useCommandStore } from "@/stores/command-store";
import { useThemeStore } from "@/stores/theme-store";
import { useSidebarStore } from "@/stores/sidebar-store";
import { themes } from "@/themes";
import { cn } from "@/lib/utils";
import { motion } from "framer-motion";
import {
  Search,
  LayoutDashboard,
  FileText,
  Settings,
  Moon,
  Sun,
  PanelLeft,
  Command,
  ArrowRight,
  Palette,
  Map as MapIcon,
} from "lucide-react";

interface CommandItem {
  id: string;
  label: string;
  description?: string;
  shortcut?: string;
  icon: React.ElementType;
  action: () => void;
}

export function CommandPalette() {
  const navigate = useNavigate();
  const { isOpen, close } = useCommandStore();
  const { cycleTheme } = useThemeStore();
  const { toggle: toggleSidebar } = useSidebarStore();

  const [query, setQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const commands: CommandItem[] = [
    {
      id: "go-dashboard",
      label: "Go to Dashboard",
      description: "Navigate to the dashboard page",
      shortcut: "G D",
      icon: LayoutDashboard,
      action: () => {
        navigate("/dashboard");
        close();
      },
    },
    {
      id: "go-notes",
      label: "Go to Notes",
      description: "Navigate to the notes page",
      shortcut: "G N",
      icon: FileText,
      action: () => {
        navigate("/notes");
        close();
      },
    },
    {
      id: "go-cartographie",
      label: "Go to Cartographie",
      description: "Navigate to the cartography page",
      shortcut: "G C",
      icon: MapIcon,
      action: () => {
        navigate("/cartographie");
        close();
      },
    },
    {
      id: "go-settings",
      label: "Open Settings",
      description: "Navigate to the settings page",
      shortcut: "G S",
      icon: Settings,
      action: () => {
        navigate("/settings");
        close();
      },
    },
    {
      id: "cycle-theme",
      label: "Cycle Theme",
      description: "Switch to the next available theme",
      shortcut: "⇧L",
      icon: Palette,
      action: () => {
        cycleTheme();
        close();
      },
    },
    {
      id: "toggle-sidebar",
      label: "Toggle Sidebar",
      description: "Show or hide the sidebar",
      shortcut: "⌘B",
      icon: PanelLeft,
      action: () => {
        toggleSidebar();
        close();
      },
    },
  ];

  const themeCommands: CommandItem[] = themes.map((t) => ({
    id: `theme-${t.id}`,
    label: `Theme: ${t.name}`,
    description: `Switch to the ${t.name} theme`,
    icon: t.type === "dark" ? Moon : Sun,
    action: () => {
      useThemeStore.getState().setTheme(t.id);
      close();
    },
  }));

  const allCommands = [...commands, ...themeCommands];

  const filteredCommands = query
    ? allCommands.filter(
        (cmd) =>
          cmd.label.toLowerCase().includes(query.toLowerCase()) ||
          cmd.description?.toLowerCase().includes(query.toLowerCase()),
      )
    : allCommands;

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === "Escape") {
        close();
      }
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setSelectedIndex((prev) => (prev + 1) % filteredCommands.length);
      }
      if (e.key === "ArrowUp") {
        e.preventDefault();
        setSelectedIndex((prev) =>
          prev - 1 < 0 ? filteredCommands.length - 1 : prev - 1,
        );
      }
      if (e.key === "Enter" && filteredCommands[selectedIndex]) {
        filteredCommands[selectedIndex].action();
      }
    },
    [close, filteredCommands, selectedIndex],
  );

  useEffect(() => {
    if (isOpen) {
      setQuery("");
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.15 }}
      className="fixed inset-0 z-[1000] flex items-start justify-center pt-[15vh]"
      data-command-palette
    >
      <div
        className="absolute inset-0 bg-black/50 backdrop-blur-sm"
        onClick={close}
      />
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: -20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: -20 }}
        transition={{ duration: 0.15, ease: "easeOut" }}
        className="relative w-full max-w-lg overflow-hidden rounded-xl border border-command-border bg-command shadow-2xl"
      >
        <div className="flex items-center border-b border-command-border px-4">
          <Search className="h-4 w-4 shrink-0 text-muted-foreground" />
          <input
            ref={inputRef}
            type="text"
            placeholder="Search commands..."
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setSelectedIndex(0);
            }}
            onKeyDown={handleKeyDown}
            className="flex-1 border-0 bg-transparent px-3 py-3.5 text-sm outline-none placeholder:text-muted-foreground"
          />
          <div className="flex items-center gap-1.5 rounded-md bg-muted px-2 py-1">
            <Command className="h-3 w-3 text-muted-foreground" />
            <span className="text-[10px] font-medium text-muted-foreground">K</span>
          </div>
        </div>

        <div className="max-h-72 overflow-y-auto p-2">
          {filteredCommands.length === 0 ? (
            <div className="px-3 py-8 text-center text-sm text-muted-foreground">
              No results found for "{query}"
            </div>
          ) : (
            filteredCommands.map((cmd, index) => {
              const Icon = cmd.icon;
              return (
                <button
                  key={cmd.id}
                  className={cn(
                    "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition-colors",
                    index === selectedIndex
                      ? "bg-accent text-accent-foreground"
                      : "text-command-foreground hover:bg-accent/50",
                  )}
                  onClick={cmd.action}
                  onMouseEnter={() => setSelectedIndex(index)}
                >
                  <Icon className="h-4 w-4 shrink-0 text-muted-foreground" />
                  <div className="flex-1 min-w-0">
                    <div className="font-medium">{cmd.label}</div>
                    {cmd.description && (
                      <div className="text-xs text-muted-foreground truncate">
                        {cmd.description}
                      </div>
                    )}
                  </div>
                  <div className="flex items-center gap-1.5">
                    {cmd.shortcut && (
                      <span className="rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                        {cmd.shortcut}
                      </span>
                    )}
                    <ArrowRight className="h-3.5 w-3.5 text-muted-foreground/50" />
                  </div>
                </button>
              );
            })
          )}
        </div>

        <div className="border-t border-command-border px-4 py-2">
          <div className="flex items-center gap-4 text-[11px] text-muted-foreground">
            <div className="flex items-center gap-1">
              <ArrowRight className="h-3 w-3" />
              <span>Select</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="rounded border border-border bg-muted px-1 text-[9px] font-medium">
                ESC
              </span>
              <span>Close</span>
            </div>
            <div className="flex items-center gap-1">
              <span className="rounded border border-border bg-muted px-1 text-[9px] font-medium">
                ↑↓
              </span>
              <span>Navigate</span>
            </div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
