import { useEffect } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useWindowStore } from "@/stores/window-store";
import { useCommandStore } from "@/stores/command-store";
import { useSidebarStore } from "@/stores/sidebar-store";
import { useThemeStore } from "@/stores/theme-store";
import { getTheme } from "@/themes";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { WindowControls } from "./window-controls";
import { TrafficLights } from "./traffic-lights";
import { cn } from "@/lib/utils";
import {
  PanelLeftClose,
  PanelLeft,
  Search,
  Command,
  Moon,
  Sun,
  LayoutDashboard,
  FileText,
  Settings,
  Map as MapIcon,
} from "lucide-react";

const navItems = [
  { label: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { label: "Cartographie", path: "/cartographie", icon: MapIcon },
  { label: "Notes", path: "/notes", icon: FileText },
  { label: "Settings", path: "/settings", icon: Settings },
];

export function CustomTitleBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { focused, platform, setMaximized, setFocused, setPlatform } =
    useWindowStore();
  const { open: openCommand } = useCommandStore();
  const { isOpen, toggle } = useSidebarStore();
  const { theme, cycleTheme } = useThemeStore();

  const isMac = platform === "darwin";

  useEffect(() => {
    setPlatform(
      (navigator.platform?.toLowerCase().includes("mac")
        ? "darwin"
        : navigator.platform?.toLowerCase().includes("win")
          ? "win32"
          : "linux") as "darwin" | "win32" | "linux",
    );
  }, [setPlatform]);

  useEffect(() => {
    if (!window.electronAPI) return;
    window.electronAPI.isMaximized().then(setMaximized);
    window.electronAPI.isFocused().then(setFocused);

    const unsubState = window.electronAPI.onWindowStateChanged(setMaximized);
    const unsubFocus = window.electronAPI.onFocusChanged(setFocused);

    return () => {
      unsubState();
      unsubFocus();
    };
  }, [setMaximized, setFocused]);

  const handleMinimize = () => window.electronAPI?.minimizeWindow();
  const handleMaximize = () => window.electronAPI?.maximizeWindow();
  const handleClose = () => window.electronAPI?.closeWindow();

  return (
    <header
      className={cn(
        "relative flex h-10 items-center border-b select-none draggable transition-colors duration-200",
        focused
          ? "border-border bg-background"
          : "border-border/50 bg-muted/30",
      )}
    >
      {isMac && (
        <TrafficLights
          onMinimize={handleMinimize}
          onMaximize={handleMaximize}
          onClose={handleClose}
        />
      )}

      <div className="flex items-center gap-1 px-2 no-drag">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-muted-foreground hover:text-foreground"
              onClick={toggle}
            >
              {isOpen ? (
                <PanelLeftClose className="h-4 w-4" />
              ) : (
                <PanelLeft className="h-4 w-4" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Toggle Sidebar</TooltipContent>
        </Tooltip>

        <div className="flex items-center gap-1 ml-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname.startsWith(item.path);
            return (
              <Button
                key={item.path}
                variant={isActive ? "secondary" : "ghost"}
                size="sm"
                className={cn(
                  "h-7 gap-1.5 px-2.5 text-xs font-medium",
                )}
                onClick={() => navigate(item.path)}
              >
                <Icon className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">{item.label}</span>
              </Button>
            );
          })}
        </div>
      </div>

      <div className="flex flex-1 items-center justify-center px-4">
        <div className="relative w-full max-w-sm  no-drag">
          <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search or run command..."
            className="h-7 pl-8 pr-10 text-xs bg-muted/50 border-muted focus-visible:bg-background cursor-pointer"
            onFocus={(e) => {
              e.target.blur();
              openCommand();
            }}
            readOnly
          />
          <div className="absolute right-1.5 top-1/2 -translate-y-1/2 flex items-center gap-0.5  no-drag rounded border border-border bg-muted px-1 py-0.5">
            <Command className="h-2.5 w-2.5 text-muted-foreground" />
            <span className="text-[9px] font-medium text-muted-foreground">P</span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-0.5 px-2 no-drag">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-muted-foreground hover:text-foreground"
              onClick={cycleTheme}
            >
              {getTheme(theme).type === "dark" ? (
                <Sun className="h-4 w-4" />
              ) : (
                <Moon className="h-4 w-4" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">Cycle theme ({getTheme(theme).name})</TooltipContent>
        </Tooltip>
      </div>

      {!isMac && (
        <WindowControls
          onMinimize={handleMinimize}
          onMaximize={handleMaximize}
          onClose={handleClose}
        />
      )}
    </header>
  );
}
