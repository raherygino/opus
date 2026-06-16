import { useNavigate, useLocation } from "react-router-dom";
import { useCommandStore } from "@/stores/command-store";
import { useThemeStore } from "@/stores/theme-store";
import { useSidebarStore } from "@/stores/sidebar-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import {
  Search,
  Moon,
  Sun,
  PanelLeftClose,
  PanelLeft,
  Command,
  Github,
} from "lucide-react";

const navItems = [
  { label: "Dashboard", path: "/dashboard" },
  { label: "Notes", path: "/notes" },
  { label: "Settings", path: "/settings" },
];

export function TopBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { open: openCommand } = useCommandStore();
  const { theme, toggleTheme } = useThemeStore();
  const { isOpen, toggle } = useSidebarStore();

  return (
    <header className="flex h-12 items-center gap-2 border-b border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 px-4 draggable">
      <div className="flex items-center gap-1 no-drag">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              onClick={toggle}
            >
              {isOpen ? (
                <PanelLeftClose className="h-4 w-4" />
              ) : (
                <PanelLeft className="h-4 w-4" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">
            Toggle Sidebar ({navigator.platform.includes("Mac") ? "⌘" : "Ctrl"}B)
          </TooltipContent>
        </Tooltip>

        <nav className="ml-4 flex items-center gap-1">
          {navItems.map((item) => (
            <Button
              key={item.path}
              variant={location.pathname.startsWith(item.path) ? "secondary" : "ghost"}
              size="sm"
              className="h-7 px-3 text-xs font-medium"
              onClick={() => navigate(item.path)}
            >
              {item.label}
            </Button>
          ))}
        </nav>
      </div>

      <div className="flex flex-1 items-center justify-center no-drag">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search or run command..."
            className="h-8 pl-9 pr-12 text-sm bg-muted/50 border-muted focus-visible:bg-background"
            onFocus={(e) => {
              e.target.blur();
              openCommand();
            }}
            readOnly
          />
          <div className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1 rounded border border-border bg-muted px-1.5 py-0.5">
            <Command className="h-3 w-3 text-muted-foreground" />
            <span className="text-[10px] font-medium text-muted-foreground">P</span>
          </div>
        </div>
      </div>

      <div className="flex items-center gap-1 no-drag">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              onClick={toggleTheme}
            >
              {theme === "dark" ? (
                <Sun className="h-4 w-4" />
              ) : (
                <Moon className="h-4 w-4" />
              )}
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">
            Toggle theme ({(navigator.platform.includes("Mac") ? "⌘" : "Ctrl")}⇧L)
          </TooltipContent>
        </Tooltip>

        <Tooltip>
          <TooltipTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8 text-muted-foreground hover:text-foreground"
              asChild
            >
              <a
                href="https://github.com"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Github className="h-4 w-4" />
              </a>
            </Button>
          </TooltipTrigger>
          <TooltipContent side="bottom">View on GitHub</TooltipContent>
        </Tooltip>
      </div>
    </header>
  );
}
