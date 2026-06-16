import { useThemeStore } from "@/stores/theme-store";
import { useNotificationStore } from "@/stores/notification-store";
import { cn } from "@/lib/utils";
import {
  Circle,
  Wifi,
  Database,
  GitBranch,
  Keyboard,
} from "lucide-react";

export function StatusBar() {
  const { theme } = useThemeStore();
  const { notifications } = useNotificationStore();

  return (
    <footer className="flex h-6 items-center justify-between border-t border-border bg-muted/50 px-4 text-[11px] text-muted-foreground select-none">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5">
          <Circle className="h-2 w-2 fill-green-500 text-green-500" />
          <span>Connected</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Wifi className="h-3 w-3" />
          <span>Online</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Database className="h-3 w-3" />
          <span>Local</span>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5">
          <GitBranch className="h-3 w-3" />
          <span>main</span>
        </div>
        <div className="flex items-center gap-1.5">
          <Keyboard className="h-3 w-3" />
          <span>Ctrl+P</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div
            className={cn(
              "h-2 w-2 rounded-full",
              theme === "dark" ? "bg-purple-400" : "bg-amber-400",
            )}
          />
          <span className="capitalize">{theme}</span>
        </div>
        {notifications.length > 0 && (
          <span className="rounded bg-primary/20 px-1.5 py-0.5 text-[10px] font-medium text-primary">
            {notifications.length}
          </span>
        )}
      </div>
    </footer>
  );
}
