import { Suspense } from "react";
import { Outlet } from "react-router-dom";
import { useKeyboardShortcuts } from "@/hooks/use-keyboard";
import { CustomTitleBar } from "@/components/title-bar/custom-title-bar";
import { ErrorBoundary } from "@/components/error-boundary";
import { Sidebar } from "./sidebar";
import { StatusBar } from "./status-bar";
import { DashboardSkeleton } from "@/components/skeletons/dashboard-skeleton";

export function AppLayout() {
  useKeyboardShortcuts();

  return (
    <div className="flex h-screen flex-col">
      <CustomTitleBar />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-auto">
          <div className="h-full p-6">
            <ErrorBoundary>
              <Suspense fallback={<DashboardSkeleton />}>
                <Outlet />
              </Suspense>
            </ErrorBoundary>
          </div>
        </main>
      </div>
      <StatusBar />
    </div>
  );
}
