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

// Plainte (Police Judiciaire)
const PlainteList = lazy(() =>
  import("@/pages/plainte-list").then((m) => ({ default: m.PlainteList })),
);
const PlainteForm = lazy(() =>
  import("@/pages/plainte-form").then((m) => ({ default: m.PlainteForm })),
);
const PlainteDetail = lazy(() =>
  import("@/pages/plainte-detail").then((m) => ({ default: m.PlainteDetail })),
);
const PlainteSortieForm = lazy(() =>
  import("@/pages/plainte-sortie-form").then((m) => ({ default: m.PlainteSortieForm })),
);
const PlainteSortieDetail = lazy(() =>
  import("@/pages/plainte-sortie-detail").then((m) => ({ default: m.PlainteSortieDetail })),
);

// Convocation (Police Judiciaire)
const ConvocationList = lazy(() =>
  import("@/pages/convocation-list").then((m) => ({ default: m.ConvocationList })),
);
const ConvocationForm = lazy(() =>
  import("@/pages/convocation-form").then((m) => ({ default: m.ConvocationForm })),
);
const ConvocationDetail = lazy(() =>
  import("@/pages/convocation-detail").then((m) => ({ default: m.ConvocationDetail })),
);

// Garde à Vue (Police Judiciaire)
const GardeAVueList = lazy(() =>
  import("@/pages/garde-a-vue-list").then((m) => ({ default: m.GardeAVueList })),
);
const GardeAVueForm = lazy(() =>
  import("@/pages/garde-a-vue-form").then((m) => ({ default: m.GardeAVueForm })),
);
const GardeAVueDetail = lazy(() =>
  import("@/pages/garde-a-vue-detail").then((m) => ({ default: m.GardeAVueDetail })),
);

// Requisition (Police Judiciaire)
const RequisitionList = lazy(() =>
  import("@/pages/requisition-list").then((m) => ({ default: m.RequisitionList })),
);
const RequisitionForm = lazy(() =>
  import("@/pages/requisition-form").then((m) => ({ default: m.RequisitionForm })),
);
const RequisitionDetail = lazy(() =>
  import("@/pages/requisition-detail").then((m) => ({ default: m.RequisitionDetail })),
);

// Personne Recherchée (Police Judiciaire)
const PersonneRechercheeList = lazy(() =>
  import("@/pages/personne-recherchee-list").then((m) => ({ default: m.PersonneRechercheeList })),
);
const PersonneRechercheeForm = lazy(() =>
  import("@/pages/personne-recherchee-form").then((m) => ({ default: m.PersonneRechercheeForm })),
);
const PersonneRechercheeDetail = lazy(() =>
  import("@/pages/personne-recherchee-detail").then((m) => ({ default: m.PersonneRechercheeDetail })),
);

// Objet (Police Judiciaire — OBJET SAISI / OBJET TROUVÉ tabs)
const ObjetList = lazy(() =>
  import("@/pages/objet-list").then((m) => ({ default: m.ObjetList })),
);
const ObjetSaisiForm = lazy(() =>
  import("@/pages/objet-saisi-form").then((m) => ({ default: m.ObjetSaisiForm })),
);
const ObjetSaisiDetail = lazy(() =>
  import("@/pages/objet-saisi-detail").then((m) => ({ default: m.ObjetSaisiDetail })),
);
const ObjetTrouveForm = lazy(() =>
  import("@/pages/objet-trouve-form").then((m) => ({ default: m.ObjetTrouveForm })),
);
const ObjetTrouveDetail = lazy(() =>
  import("@/pages/objet-trouve-detail").then((m) => ({ default: m.ObjetTrouveDetail })),
);

// Perquisition (Police Judiciaire)
const PerquisitionList = lazy(() =>
  import("@/pages/perquisition-list").then((m) => ({ default: m.PerquisitionList })),
);
const PerquisitionForm = lazy(() =>
  import("@/pages/perquisition-form").then((m) => ({ default: m.PerquisitionForm })),
);
const PerquisitionDetail = lazy(() =>
  import("@/pages/perquisition-detail").then((m) => ({ default: m.PerquisitionDetail })),
);

// Renseignement (Police Judiciaire)
const RenseignementPjList = lazy(() =>
  import("@/pages/renseignement-pj-list").then((m) => ({ default: m.RenseignementPjList })),
);
const RenseignementPjForm = lazy(() =>
  import("@/pages/renseignement-pj-form").then((m) => ({ default: m.RenseignementPjForm })),
);
const RenseignementPjDetail = lazy(() =>
  import("@/pages/renseignement-pj-detail").then((m) => ({ default: m.RenseignementPjDetail })),
);

// Mandat (Police Judiciaire)
const MandatList = lazy(() =>
  import("@/pages/mandat-list").then((m) => ({ default: m.MandatList })),
);
const MandatForm = lazy(() =>
  import("@/pages/mandat-form").then((m) => ({ default: m.MandatForm })),
);
const MandatDetail = lazy(() =>
  import("@/pages/mandat-detail").then((m) => ({ default: m.MandatDetail })),
);

// Arrestation (Police Judiciaire)
const ArrestationList = lazy(() =>
  import("@/pages/arrestation-list").then((m) => ({ default: m.ArrestationList })),
);
const ArrestationForm = lazy(() =>
  import("@/pages/arrestation-form").then((m) => ({ default: m.ArrestationForm })),
);
const ArrestationDetail = lazy(() =>
  import("@/pages/arrestation-detail").then((m) => ({ default: m.ArrestationDetail })),
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

// Correspondance (Sédentaire > Secrétariat)
const CorrespondanceList = lazy(() =>
  import("@/pages/correspondance-list").then((m) => ({ default: m.CorrespondanceList })),
);
const CorrespondanceForm = lazy(() =>
  import("@/pages/correspondance-form").then((m) => ({ default: m.CorrespondanceForm })),
);
const CorrespondanceDetail = lazy(() =>
  import("@/pages/correspondance-detail").then((m) => ({ default: m.CorrespondanceDetail })),
);

// Déclaration de perte (Sédentaire > Secrétariat)
const DeclarationPerteList = lazy(() =>
  import("@/pages/declaration-perte-list").then((m) => ({ default: m.DeclarationPerteList })),
);
const DeclarationPerteForm = lazy(() =>
  import("@/pages/declaration-perte-form").then((m) => ({ default: m.DeclarationPerteForm })),
);
const DeclarationPerteDetail = lazy(() =>
  import("@/pages/declaration-perte-detail").then((m) => ({ default: m.DeclarationPerteDetail })),
);

// Passation (Sédentaire > Poste)
const PassationList = lazy(() =>
  import("@/pages/passation-list").then((m) => ({ default: m.PassationList })),
);
const PassationForm = lazy(() =>
  import("@/pages/passation-form").then((m) => ({ default: m.PassationForm })),
);
const PassationDetail = lazy(() =>
  import("@/pages/passation-detail").then((m) => ({ default: m.PassationDetail })),
);

// Armement (Sédentaire > Poste)
const ArmementList = lazy(() =>
  import("@/pages/armement-list").then((m) => ({ default: m.ArmementList })),
);
const ArmementForm = lazy(() =>
  import("@/pages/armement-form").then((m) => ({ default: m.ArmementForm })),
);
const ArmementDetail = lazy(() =>
  import("@/pages/armement-detail").then((m) => ({ default: m.ArmementDetail })),
);

// Armes (Sédentaire > Poste — weapon catalog + ammunition stock)
const ArmesManagement = lazy(() =>
  import("@/pages/armes-management").then((m) => ({ default: m.ArmesManagement })),
);
const ArmeForm = lazy(() =>
  import("@/pages/arme-form").then((m) => ({ default: m.ArmeForm })),
);
const ArmeDetail = lazy(() =>
  import("@/pages/arme-detail").then((m) => ({ default: m.ArmeDetail })),
);

// Matériels (Sédentaire > Poste — equipment assignment & return)
const MaterielsManagement = lazy(() =>
  import("@/pages/materiels-management").then((m) => ({ default: m.MaterielsManagement })),
);
const MaterielForm = lazy(() =>
  import("@/pages/materiel-form").then((m) => ({ default: m.MaterielForm })),
);
const MaterielDetail = lazy(() =>
  import("@/pages/materiel-detail").then((m) => ({ default: m.MaterielDetail })),
);

// Matériel Roulant (Sédentaire > Poste — vehicle perception & reintegration)
const MaterielRoulantManagement = lazy(() =>
  import("@/pages/materiel-roulant-management").then((m) => ({ default: m.MaterielRoulantManagement })),
);
const MaterielRoulantForm = lazy(() =>
  import("@/pages/materiel-roulant-form").then((m) => ({ default: m.MaterielRoulantForm })),
);
const MaterielRoulantDetail = lazy(() =>
  import("@/pages/materiel-roulant-detail").then((m) => ({ default: m.MaterielRoulantDetail })),
);

// Main courante (Sédentaire > Secrétariat & Poste — event logbook)
const MainCouranteList = lazy(() =>
  import("@/pages/main-courante-list").then((m) => ({ default: m.MainCouranteList })),
);
const MainCouranteForm = lazy(() =>
  import("@/pages/main-courante-form").then((m) => ({ default: m.MainCouranteForm })),
);
const MainCouranteDetail = lazy(() =>
  import("@/pages/main-courante-detail").then((m) => ({ default: m.MainCouranteDetail })),
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
          <Route
            path="/login"
            element={
              <ErrorBoundary>
                <Suspense fallback={<DashboardSkeleton />}>
                  <LoginPage />
                </Suspense>
              </ErrorBoundary>
            }
          />

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
              <Route
                path="secretariat/correspondance"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <CorrespondanceList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/correspondance/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <CorrespondanceForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/correspondance/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <CorrespondanceDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/correspondance/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <CorrespondanceForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/declaration-perte"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <DeclarationPerteList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/declaration-perte/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <DeclarationPerteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/declaration-perte/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <DeclarationPerteDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/declaration-perte/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <DeclarationPerteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/main-courante"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MainCouranteList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/main-courante/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/main-courante/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="secretariat/main-courante/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/passation"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PassationList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/passation/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <PassationForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/passation/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <PassationDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/passation/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <PassationForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armement"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArmementList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armement/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmementForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armement/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmementDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armement/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmementForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armes"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArmesManagement />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armes/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmeForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armes/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmeDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/armes/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <ArmeForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiels"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MaterielsManagement />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiels/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiels/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiels/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiel-roulant"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MaterielRoulantManagement />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiel-roulant/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielRoulantForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiel-roulant/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielRoulantDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/materiel-roulant/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MaterielRoulantForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/main-courante"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MainCouranteList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/main-courante/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/main-courante/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="poste/main-courante/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<FormSkeleton />}>
                      <MainCouranteForm />
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
              {/* Plainte ENTRÉE + SORTIE */}
              <Route
                path="plainte"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/sortie/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteSortieForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/sortie/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteSortieDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="plainte/sortie/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PlainteSortieForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Convocation */}
              <Route
                path="convocation"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ConvocationList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="convocation/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ConvocationForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="convocation/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ConvocationDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="convocation/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ConvocationForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Garde à Vue */}
              <Route
                path="gav"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <GardeAVueList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="gav/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <GardeAVueForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="gav/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <GardeAVueDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="gav/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <GardeAVueForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Requisition */}
              <Route
                path="requisition"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RequisitionList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="requisition/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RequisitionForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="requisition/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RequisitionDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="requisition/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RequisitionForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Personne Recherchée */}
              <Route
                path="personne-recherchee"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PersonneRechercheeList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="personne-recherchee/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PersonneRechercheeForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="personne-recherchee/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PersonneRechercheeDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="personne-recherchee/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PersonneRechercheeForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Objets (tabbed: saisi / trouvé) */}
              <Route
                path="objets"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/saisi/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetSaisiForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/saisi/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetSaisiDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/saisi/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetSaisiForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/trouve/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetTrouveForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/trouve/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetTrouveDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="objets/trouve/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ObjetTrouveForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Perquisition */}
              <Route
                path="perquisition"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PerquisitionList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="perquisition/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PerquisitionForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="perquisition/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PerquisitionDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="perquisition/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <PerquisitionForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Renseignement */}
              <Route
                path="renseignement"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RenseignementPjList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="renseignement/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RenseignementPjForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="renseignement/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RenseignementPjDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="renseignement/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <RenseignementPjForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Mandat */}
              <Route
                path="mandat"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MandatList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="mandat/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MandatForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="mandat/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MandatDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="mandat/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <MandatForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              {/* Arrestation */}
              <Route
                path="arrestation"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArrestationList />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="arrestation/new"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArrestationForm />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="arrestation/:id"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArrestationDetail />
                    </Suspense>
                  </ErrorBoundary>
                }
              />
              <Route
                path="arrestation/:id/edit"
                element={
                  <ErrorBoundary>
                    <Suspense fallback={<TableSkeleton />}>
                      <ArrestationForm />
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
