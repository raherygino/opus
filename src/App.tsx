import { lazy, Suspense, useEffect } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { AnimatePresence } from "framer-motion";
import { useThemeStore } from "@/stores/theme-store";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ErrorBoundary } from "@/components/error-boundary";
import { AppLayout } from "@/components/layout/app-layout";
import { CommandPalette } from "@/components/layout/command-palette";
import { DashboardSkeleton } from "@/components/skeletons/dashboard-skeleton";
import { NotesSkeleton } from "@/components/skeletons/notes-skeleton";
import { SettingsSkeleton } from "@/components/skeletons/settings-skeleton";

const Dashboard = lazy(() =>
  import("@/pages/dashboard").then((m) => ({ default: m.Dashboard })),
);

const Notes = lazy(() =>
  import("@/pages/notes").then((m) => ({ default: m.Notes })),
);

const Settings = lazy(() =>
  import("@/pages/settings").then((m) => ({ default: m.Settings })),
);

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
            <Route
              path="/dashboard"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<DashboardSkeleton />}>
                    <Dashboard />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/notes"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<NotesSkeleton />}>
                    <Notes />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/notes/:id"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<NotesSkeleton />}>
                    <Notes />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/settings"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<SettingsSkeleton />}>
                    <Settings />
                  </Suspense>
                </ErrorBoundary>
              }
            />
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
