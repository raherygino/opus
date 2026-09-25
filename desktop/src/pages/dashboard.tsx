import { useCallback, useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import {
  Shield,
  Users,
  Building2,
  Car,
  Scale,
  Activity,
  UserCheck,
  ChevronRight,
  RefreshCw,
  TriangleAlert,
  Bell,
  ClipboardList,
  type LucideIcon,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";
import { getDashboardStats } from "@/lib/api/dashboard";
import { hasPermission } from "@/lib/permissions";
import type { DashboardStats } from "@/types";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

const REFRESH_INTERVAL_MS = 60_000;

interface KpiCard {
  label: string;
  value: number;
  desc: string;
  icon: LucideIcon;
  path: string | null;
  /** Amber accent when the metric requires attention (value > 0). */
  alert?: boolean;
}

function StatCard({ stat }: { stat: KpiCard }) {
  const navigate = useNavigate();
  const Icon = stat.icon;
  const clickable = stat.path !== null;

  const card = (
    <Card className="relative overflow-hidden transition-all duration-200 group-hover:-translate-y-0.5 group-hover:shadow-lg">
      <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {stat.label}
        </CardTitle>
        <div
          className={`rounded-full p-1.5 ${
            stat.alert && stat.value > 0
              ? "bg-amber-500/10"
              : "bg-primary/10"
          }`}
        >
          <Icon
            className={`h-4 w-4 ${
              stat.alert && stat.value > 0 ? "text-amber-500" : "text-primary"
            }`}
          />
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold tabular-nums">{stat.value}</span>
        </div>
        <p className="text-xs text-muted-foreground mt-1">{stat.desc}</p>
      </CardContent>
    </Card>
  );

  if (!clickable) return card;
  return (
    <button onClick={() => navigate(stat.path!)} className="group text-left">
      {card}
    </button>
  );
}

export function Dashboard() {
  const { user } = useAuthStore();
  const navigate = useNavigate();

  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const loadStats = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      setStats(await getDashboardStats());
      setError(false);
    } catch {
      // Silent refreshes keep the last good data; only the initial load
      // (or an explicit retry) shows the error state.
      if (!silent) setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial load + auto-refresh: poll every 60 s and refetch on window focus.
  useEffect(() => {
    loadStats();
    const interval = setInterval(() => loadStats(true), REFRESH_INTERVAL_MS);
    const onFocus = () => loadStats(true);
    window.addEventListener("focus", onFocus);
    return () => {
      clearInterval(interval);
      window.removeEventListener("focus", onFocus);
    };
  }, [loadStats]);

  const isCommand =
    user?.role_code === "SUPER_ADMIN" ||
    user?.role_code === "CHIEF" ||
    user?.role_code === "STATION_ADMIN";

  const quickLinks = isCommand
    ? [
        { icon: Building2, label: "Division Sédentaire", path: "/sedentaire/dashboard", desc: "Secrétariat & Chef de Poste" },
        { icon: Car, label: "Division Service Général", path: "/sg/dashboard", desc: "Patrouilles & Interventions" },
        { icon: Scale, label: "Division PJ", path: "/pj/dashboard", desc: "Enquêtes & Procédure" },
        { icon: Users, label: "Personnel", path: "/personnel", desc: "Gestion des effectifs" },
      ]
    : [];

  // Cards link to their module page only when the user can view that module.
  const kpiCards = useMemo<KpiCard[]>(() => {
    if (!stats) return [];
    const link = (module: string, path: string) =>
      hasPermission(user, module, "can_view") ? path : null;
    return [
      {
        label: "Personnel actif",
        value: stats.personnel_en_service,
        desc: `${stats.personnel_en_mouvement} en mouvement · ${stats.personnel_total} au total`,
        icon: UserCheck,
        path: link("personnel", "/personnel"),
      },
      {
        label: "Utilisateurs",
        value: stats.users_total,
        desc: `${stats.users_actifs} comptes actifs`,
        icon: Shield,
        path: link("users", "/users"),
      },
      {
        label: "Activités",
        value: stats.activites_7j,
        desc: `7 derniers jours · ${stats.activites_aujourdhui} aujourd'hui`,
        icon: Activity,
        path: link("sg_activite", "/sg/activites"),
      },
      {
        label: "GAV en cours",
        value: stats.gav_en_cours,
        desc: "Gardes à vue en cours",
        icon: Scale,
        path: link("pj_gav", "/pj/gav"),
        alert: true,
      },
      {
        label: "Armes perçues",
        value: stats.armes_en_service,
        desc: "Non réintégrées",
        icon: TriangleAlert,
        path: link("sedentaire_poste_armement", "/sedentaire/poste/armement"),
        alert: true,
      },
      {
        label: "Véhicules en service",
        value: stats.vehicules_en_service,
        desc: "Matériel roulant en mission",
        icon: Car,
        path: link("sedentaire_poste_materiel_roulant", "/sedentaire/poste/materiel-roulant"),
      },
    ];
  }, [stats, user]);

  const todayMetrics = useMemo(() => {
    if (!stats) return [];
    return [
      { icon: TriangleAlert, label: "Événements survenus", value: stats.evenements_aujourdhui, path: hasPermission(user, "sg_evenement_survenu", "can_view") ? "/sg/evenements-survenus" : null },
      { icon: ClipboardList, label: "Main courante", value: stats.main_courante_aujourdhui, path: hasPermission(user, "sedentaire_secretariat_main_courante", "can_view") || hasPermission(user, "sedentaire_poste_main_courante", "can_view") ? "/sedentaire/secretariat/main-courante" : null },
      { icon: Bell, label: "Notifications non lues", value: stats.notifications_non_lues, path: "/notifications" },
    ];
  }, [stats, user]);

  return (
    <div className="relative min-h-full">

      <motion.div
        variants={container}
        initial="hidden"
        animate="show"
        className="relative z-10 mx-auto max-w-5xl space-y-8"
      >
        {/* Header with logos */}
        <motion.div variants={item}>
          <div className="relative flex items-center gap-6 rounded-xl border border-border/50 bg-card/50 p-6 backdrop-blur-sm overflow-hidden">
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-green-500 via-white to-red-500" />
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoPnSrc} alt="PN" className="h-28 w-auto object-contain" />
            </div>

            <div className="flex-1 min-w-0">
              <span className="text-xs font-medium text-primary uppercase tracking-wider">Tableau de bord</span>
              <h1 className="text-2xl font-bold tracking-tight">
                Bienvenue, {user?.firstname} {user?.lastname}
              </h1>
              <p className="text-sm text-muted-foreground mt-1 flex items-center gap-1.5">
                <Shield className="h-3.5 w-3.5 text-primary/70" />
                {user?.role_name} — {user?.grade}
              </p>
            </div>

            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoCspSrc} alt="CSP" className="h-28 w-auto object-contain" />
            </div>
          </div>
        </motion.div>

        {/* Quick stats — live data from GET /api/dashboard/stats */}
        <motion.div variants={item} className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-medium text-muted-foreground uppercase tracking-wider">
              Vue d'ensemble
            </h2>
            <button
              onClick={() => loadStats()}
              title="Actualiser les statistiques"
              className="rounded-full p-1.5 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
            </button>
          </div>

          {error && !stats ? (
            <Card className="border-destructive/40">
              <CardContent className="flex items-center justify-between gap-4 py-5">
                <p className="text-sm text-muted-foreground">
                  Impossible de charger les statistiques. Vérifiez votre connexion.
                </p>
                <Button variant="outline" size="sm" onClick={() => loadStats()}>
                  <RefreshCw className="h-3.5 w-3.5 mr-1.5" />
                  Réessayer
                </Button>
              </CardContent>
            </Card>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {loading && !stats
                ? Array.from({ length: 6 }).map((_, i) => (
                    <Card key={i} className="relative overflow-hidden">
                      <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                      <CardHeader className="flex flex-row items-center justify-between pb-2">
                        <Skeleton variant="text" width="60%" />
                        <Skeleton variant="circular" width={20} height={20} />
                      </CardHeader>
                      <CardContent>
                        <Skeleton height={28} width="50%" />
                        <Skeleton variant="text" width="70%" className="mt-1" />
                      </CardContent>
                    </Card>
                  ))
                : kpiCards.map((stat) => <StatCard key={stat.label} stat={stat} />)}
            </div>
          )}

          {/* Aujourd'hui — slim counters row */}
          {!error || stats ? (
            <Card>
              <CardContent className="grid grid-cols-1 sm:grid-cols-3 gap-4 py-4">
                {loading && !stats
                  ? Array.from({ length: 3 }).map((_, i) => (
                      <div key={i} className="flex items-center gap-3">
                        <Skeleton variant="circular" width={32} height={32} />
                        <div className="flex-1 space-y-1.5">
                          <Skeleton variant="text" width="55%" />
                          <Skeleton variant="text" width="35%" />
                        </div>
                      </div>
                    ))
                  : todayMetrics.map((m) => {
                      const Icon = m.icon;
                      const inner = (
                        <>
                          <div className="rounded-lg bg-primary/10 p-2 shrink-0">
                            <Icon className="h-4 w-4 text-primary" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="text-xs text-muted-foreground truncate">{m.label}</p>
                            <p className="text-lg font-bold tabular-nums">{m.value}</p>
                          </div>
                          {m.path && (
                            <ChevronRight className="h-4 w-4 text-muted-foreground/40 group-hover:text-primary transition-colors" />
                          )}
                        </>
                      );
                      return m.path ? (
                        <button
                          key={m.label}
                          onClick={() => navigate(m.path!)}
                          className="group flex items-center gap-3 text-left rounded-lg p-1 -m-1 hover:bg-accent/50 transition-colors"
                        >
                          {inner}
                        </button>
                      ) : (
                        <div key={m.label} className="flex items-center gap-3">
                          {inner}
                        </div>
                      );
                    })}
              </CardContent>
            </Card>
          ) : null}
        </motion.div>

        {/* Quick links for command roles */}
        {isCommand && (
          <motion.div variants={item}>
            <Card className="overflow-hidden">
              <CardHeader>
                <CardTitle className="text-lg">Accès rapide</CardTitle>
              </CardHeader>
              <CardContent className="grid gap-3 sm:grid-cols-2">
                {quickLinks.map((link) => {
                  const Icon = link.icon;
                  return (
                    <button
                      key={link.path}
                      onClick={() => navigate(link.path)}
                      className="group relative flex items-start gap-3 rounded-lg border border-border bg-card p-4 text-left transition-all duration-200 hover:border-primary/30 hover:shadow-md hover:-translate-y-0.5"
                    >
                      <div className="rounded-lg bg-primary/10 p-2.5 transition-colors group-hover:bg-primary/15">
                        <Icon className="h-5 w-5 text-primary" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-medium">{link.label}</div>
                        <div className="text-xs text-muted-foreground mt-0.5">{link.desc}</div>
                      </div>
                      <ChevronRight className="h-4 w-4 text-muted-foreground/40 mt-1 transition-all group-hover:translate-x-0.5 group-hover:text-primary" />
                    </button>
                  );
                })}
              </CardContent>
            </Card>
          </motion.div>
        )}

        {/* Role info */}
        <motion.div variants={item}>
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Informations du compte</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-4 sm:grid-cols-2">
              {[
                { label: "Nom d'utilisateur", value: user?.username },
                { label: "IM", value: user?.im },
                { label: "Grade", value: user?.grade },
                { label: "Affectation", value: user?.affectation },
                { label: "Rôle système", value: user?.role_name },
                { label: "Dernière connexion", value: user?.last_login ? new Date(user.last_login).toLocaleString("fr-FR") : "Première connexion" },
              ].map((info) => (
                <div key={info.label} className="space-y-0.5">
                  <p className="text-xs text-muted-foreground">{info.label}</p>
                  <p className="text-sm font-medium">{info.value}</p>
                </div>
              ))}
            </CardContent>
          </Card>
        </motion.div>
      </motion.div>
    </div>
  );
}
