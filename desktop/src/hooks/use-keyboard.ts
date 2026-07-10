import { useEffect } from "react";
import { useCommandStore } from "@/stores/command-store";
import { useThemeStore } from "@/stores/theme-store";
import { useSidebarStore } from "@/stores/sidebar-store";

export function useKeyboardShortcuts() {
  const { toggle: toggleCommand } = useCommandStore();
  const { cycleTheme } = useThemeStore();
  const { toggle: toggleSidebar } = useSidebarStore();

  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      const mod = e.metaKey || e.ctrlKey;

      if (mod && e.key === "p") {
        e.preventDefault();
        toggleCommand();
      }

      if (mod && e.key === "b") {
        e.preventDefault();
        toggleSidebar();
      }

      if (mod && e.shiftKey && e.key === "L") {
        e.preventDefault();
        cycleTheme();
      }

      if (e.key === "Escape") {
        const commandPalette = document.querySelector("[data-command-palette]");
        if (commandPalette) {
          const event = new KeyboardEvent("keydown", { key: "Escape" });
          commandPalette.dispatchEvent(event);
        }
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [toggleCommand, cycleTheme, toggleSidebar]);
}
