import { Routes, Route, Navigate } from "react-router-dom";
import { AnimatePresence } from "framer-motion";
import { useThemeStore } from "@/stores/theme-store";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AppLayout } from "@/components/layout/app-layout";
import { CommandPalette } from "@/components/layout/command-palette";
import { Dashboard } from "@/pages/dashboard";
import { Notes } from "@/pages/notes";
import { Settings } from "@/pages/settings";
import { useEffect } from "react";

export default function App() {
  const { theme } = useThemeStore();

  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove("light", "dark");
    root.classList.add(theme);
  }, [theme]);

  return (
    <TooltipProvider delayDuration={200}>
      <div className="h-screen w-screen overflow-hidden bg-background text-foreground antialiased">
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/notes" element={<Notes />} />
            <Route path="/notes/:id" element={<Notes />} />
            <Route path="/settings" element={<Settings />} />
          </Route>
        </Routes>
        <AnimatePresence>
          <CommandPalette />
        </AnimatePresence>
        <Toaster />
      </div>
    </TooltipProvider>
  );
}
