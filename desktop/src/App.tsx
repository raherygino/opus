import { lazy, Suspense, useEffect, useRef } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { AnimatePresence } from "framer-motion";
import { useThemeStore } from "@/stores/theme-store";
import { getTheme } from "@/themes";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ErrorBoundary } from "@/components/error-boundary";
import { AppLayout } from "@/components/layout/app-layout";
import { CommandPalette } from "@/components/layout/command-palette";
import { ProtectedRoute } from "@/components/auth/protected-route";
import { DashboardSkeleton } from "@/components/skeletons/dashboard-skeleton";
import { FormSkeleton } from "@/components/skeletons/form-skeleton";
import { TableSkeleton } from "@/components/skeletons/table-skeleton";
import { MapSkeleton } from "@/components/skeletons/map-skeleton";

// Auth
const LoginPage = lazy(() =>
  import("@/pages/login").then((m) => ({ default: m.LoginPage })),
);

// Admin Dashboard
const Dashboard = lazy(() =>
  import("@/pages/dashboard").then((m) => ({ default: m.Dashboard })),
);

// Division Dashboards
const SedentaireDashboard = lazy(() =>
  import("@/pages/sedentaire-dashboard").then((m) => ({ default: m.SedentaireDashboard })),
);
const SgDashboard = lazy(() =>
  import("@/pages/sg-dashboard").then((m) => ({ default: m.SgDashboard })),
);
const PjDashboard = lazy(() =>
  import("@/pages/pj-dashboard").then((m) => ({ default: m.PjDashboard })),
);

// Personnel
const PersonnelTabs = lazy(() =>
  import("@/pages/personnel-tabs").then((m) => ({ default: m.PersonnelTabs })),
);
const PersonnelDetail = lazy(() =>
  import("@/pages/personnel-detail").then((m) => ({ default: m.PersonnelDetail })),
);
const PersonnelForm = lazy(() =>
  import("@/pages/personnel-form").then((m) => ({ default: m.PersonnelForm })),
);

// Profile
const ProfilePage = lazy(() =>
  import("@/pages/profile").then((m) => ({ default: m.ProfilePage })),
);

// Users
const UsersList = lazy(() =>
  import("@/pages/users-list").then((m) => ({ default: m.UsersList })),
);
const UserForm = lazy(() =>
  import("@/pages/users-form").then((m) => ({ default: m.UserForm })),
);

// Roles
const RolesList = lazy(() =>
  import("@/pages/roles-list").then((m) => ({ default: m.RolesList })),
);
const RoleForm = lazy(() =>
  import("@/pages/roles-form").then((m) => ({ default: m.RoleForm })),
);

// Cartographie
const Cartographie = lazy(() =>
  import("@/pages/cartographie").then((m) => ({ default: m.Cartographie })),
);

// Legacy notes (keep for now)
const Notes = lazy(() =>
  import("@/pages/notes").then((m) => ({ default: m.Notes })),
);
const Settings = lazy(() =>
  import("@/pages/settings").then((m) => ({ default: m.Settings })),
);

const ComingSoon = lazy(() =>
  import("@/pages/coming-soon").then((m) => ({ default: m.ComingSoon })),
);

// Notifications
const NotificationsPage = lazy(() =>
  import("@/pages/notifications").then((m) => ({ default: m.NotificationsPage })),
);

// Audit Logs
const AuditLogPage = lazy(() =>
  import("@/pages/audit-logs").then((m) => ({ default: m.AuditLogPage })),
);

export default function App() {
  const { theme } = useThemeStore();
  const transitionTimer = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    const root = document.documentElement;
    const themeDef = getTheme(theme);

    root.setAttribute("data-theme", theme);
    root.className = theme;

    Object.entries(themeDef.colors).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });

    if (transitionTimer.current) clearTimeout(transitionTimer.current);
    root.classList.add("theme-transition");
    transitionTimer.current = setTimeout(() => {
      root.classList.remove("theme-transition");
    }, 500);

    return () => {
      if (transitionTimer.current) clearTimeout(transitionTimer.current);
    };
  }, [theme]);

  return (
    <TooltipProvider delayDuration={200}>
      <div className="h-screen w-screen overflow-hidden bg-background text-foreground antialiased">
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />

          {/* Authenticated routes */}
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            {/* Redirects */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />

            {/* Admin Dashboard */}
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

            {/* Division: Sédentaire */}
            <Route path="/sedentaire">
              <Route
                index
                element={<Navigate to="/sedentaire/dashboard" replace />}
              />
              <Route
                path="dashboard"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<DashboardSkeleton />}>
                      <SedentaireDashboard />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route path="*" element={<ComingSoon />} />
            </Route>

            {/* Division: Service Général */}
            <Route path="/sg">
              <Route
                index
                element={<Navigate to="/sg/dashboard" replace />}
              />
              <Route
                path="dashboard"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<DashboardSkeleton />}>
                      <SgDashboard />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route path="*" element={<ComingSoon />} />
            </Route>

            {/* Division: Police Judiciaire */}
            <Route path="/pj">
              <Route
                index
                element={<Navigate to="/pj/dashboard" replace />}
              />
              <Route
                path="dashboard"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<DashboardSkeleton />}>
                      <PjDashboard />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route path="*" element={<ComingSoon />} />
            </Route>

            {/* Personnel Management */}
            <Route
              path="/personnel"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<TableSkeleton />}>
                    <PersonnelTabs />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/personnel/new"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <PersonnelForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/personnel/:id"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <PersonnelDetail />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/personnel/:id/edit"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <PersonnelForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Profile */}
            <Route
              path="/profile"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <ProfilePage />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* User Management */}
            <Route
              path="/users"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<TableSkeleton />}>
                    <UsersList />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/users/new"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <UserForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/users/:id/edit"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <UserForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Role Management */}
            <Route
              path="/roles"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<TableSkeleton />}>
                    <RolesList />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/roles/new"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <RoleForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/roles/:id/edit"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<FormSkeleton />}>
                    <RoleForm />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Cartographie */}
            <Route
              path="/cartographie"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<MapSkeleton />}>
                    <Cartographie />
                  </Suspense>
                </ErrorBoundary>
              }
            />

          {/* Legacy routes */}
            <Route
              path="/notes"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<DashboardSkeleton />}>
                    <Notes />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/notes/:id"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<DashboardSkeleton />}>
                    <Notes />
                  </Suspense>
                </ErrorBoundary>
              }
            />
            <Route
              path="/settings"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<DashboardSkeleton />}>
                    <Settings />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Notifications */}
            <Route
              path="/notifications"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<DashboardSkeleton />}>
                    <NotificationsPage />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Audit Logs (SUPER_ADMIN only) */}
            <Route
              path="/audit-logs"
              element={
                <ErrorBoundary>
                  <Suspense fallback={<TableSkeleton />}>
                    <AuditLogPage />
                  </Suspense>
                </ErrorBoundary>
              }
            />

            {/* Catch-all inside authenticated layout */}
            <Route path="*" element={<ComingSoon />} />
          </Route>

          {/* Outer catch-all: unauthenticated users go to login */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
        <AnimatePresence>
          <CommandPalette />
        </AnimatePresence>
        <Toaster />
      </div>
    </TooltipProvider>
  );
}
