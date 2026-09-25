import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ClipboardList,
  Siren,
  Route,
  ShieldAlert,
  ChevronRight,
  Clock,
} from "lucide-react";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRassemblementList } from "@/lib/api/rassemblement-journalier";
import { getEvenementSurvenuList, EVENEMENT_TYPE_LABELS } from "@/lib/api/evenement-survenu";
import { getActiviteList } from "@/lib/api/activite";
import { getDispositifExceptionnelList } from "@/lib/api/dispositif-exceptionnel";
import type {
  RassemblementJournalier,
  EvenementSurvenu,
  Activite,
  DispositifExceptionnel,
} from "@/types";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

// Permission module codes for each SG feature.
const MODULE_RASSEMBLEMENT = "sg_rassemblement_journalier";
const MODULE_EVENEMENT = "sg_evenement_survenu";
const MODULE_ACTIVITE = "sg_activite";
const MODULE_DISPOSITIF = "sg_dispositif_exceptionnel";

type ActivityType = "rassemblement" | "evenement" | "activite" | "dispositif";

interface ActivityItem {
  id: string;
  action: string;
  createdAt: string;
  type: ActivityType;
  path: string;
}

/** Format an ISO timestamp as a French relative time string. */
function formatRelativeTime(iso: string): string {
  const then = new Date(iso.replace(" ", "T")).getTime();
  if (Number.isNaN(then)) return "—";
  const diffMs = Date.now() - then;
  const sec = Math.floor(diffMs / 1000);
  if (sec < 60) return "À l'instant";
  const min = Math.floor(sec / 60);
  if (min < 60) return `Il y a ${min} min`;
  const h = Math.floor(min / 60);
  if (h < 24) return `Il y a ${h} h`;
  const d = Math.floor(h / 24);
  if (d < 7) return `Il y a ${d} j`;
  const [y, m, day] = iso.slice(0, 10).split("-");
  return y && m && day ? `${day}/${m}/${y}` : iso;
}

const todayIso = () => new Date().toISOString().slice(0, 10);

export function SgDashboard() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [rassemblements, setRassemblements] = useState<RassemblementJournalier[]>([]);
  const [evenements, setEvenements] = useState<EvenementSurvenu[]>([]);
  const [activites, setActivites] = useState<Activite[]>([]);
  const [dispositifs, setDispositifs] = useState<DispositifExceptionnel[]>([]);
  const [loading, setLoading] = useState(true);

  const canViewRassemblement = hasPermission(user, MODULE_RASSEMBLEMENT, "can_view");
  const canViewEvenement = hasPermission(user, MODULE_EVENEMENT, "can_view");
  const canViewActivite = hasPermission(user, MODULE_ACTIVITE, "can_view");
  const canViewDispositif = hasPermission(user, MODULE_DISPOSITIF, "can_view");

  const canCreateRassemblement = hasPermission(user, MODULE_RASSEMBLEMENT, "can_create");
  const canCreateEvenement = hasPermission(user, MODULE_EVENEMENT, "can_create");
  const canCreateActivite = hasPermission(user, MODULE_ACTIVITE, "can_create");
  const canCreateDispositif = hasPermission(user, MODULE_DISPOSITIF, "can_create");

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadDashboard() {
    setLoading(true);
    const tasks: Promise<void>[] = [];
    if (canViewRassemblement) {
      tasks.push(
        getRassemblementList()
          .then(setRassemblements)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les rassemblements")),
      );
    }
    if (canViewEvenement) {
      tasks.push(
        getEvenementSurvenuList()
          .then(setEvenements)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les évènements")),
      );
    }
    if (canViewActivite) {
      tasks.push(
        getActiviteList()
          .then(setActivites)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les activités")),
      );
    }
    if (canViewDispositif) {
      tasks.push(
        getDispositifExceptionnelList()
          .then(setDispositifs)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les dispositifs")),
      );
    }
    await Promise.all(tasks);
    setLoading(false);
  }

  const today = todayIso();

  // Build the stat cards from real counts, gated by view permission.
  const stats = useMemo(() => {
    const cards: {
      label: string;
      value: number;
      change: string;
      icon: typeof ClipboardList;
      path: string;
    }[] = [];
    if (canViewRassemblement) {
      const latest = rassemblements[0];
      cards.push({
        label: "Rassemblements",
        value: rassemblements.length,
        change: latest
          ? `Dernier : ${latest.present} présents`
          : "Aucun enregistré",
        icon: ClipboardList,
        path: "/sg/rassemblement-journalier",
      });
    }
    if (canViewEvenement) {
      const todayCount = evenements.filter((e) => e.date_evenement === today).length;
      cards.push({
        label: "Événements survenus",
        value: evenements.length,
        change: `${todayCount} aujourd'hui`,
        icon: Siren,
        path: "/sg/evenements-survenus",
      });
    }
    if (canViewActivite) {
      const todayCount = activites.filter((a) => a.date_activite === today).length;
      cards.push({
        label: "Activités",
        value: activites.length,
        change: `${todayCount} aujourd'hui`,
        icon: Route,
        path: "/sg/activites",
      });
    }
    if (canViewDispositif) {
      const activeCount = dispositifs.filter(
        (d) => d.date_debut <= today && d.date_fin >= today,
      ).length;
      cards.push({
        label: "Dispositifs exceptionnels",
        value: dispositifs.length,
        change: `${activeCount} en cours`,
        icon: ShieldAlert,
        path: "/sg/dispositifs-exceptionnels",
      });
    }
    return cards;
  }, [
    canViewRassemblement, canViewEvenement, canViewActivite, canViewDispositif,
    rassemblements, evenements, activites, dispositifs, today,
  ]);

  // Merge recent items across all SG modules, sorted by created_at descending.
  const recentActivity = useMemo<ActivityItem[]>(() => {
    const items: ActivityItem[] = [];
    rassemblements.forEach((r) => {
      items.push({
        id: `rassemblement-${r.id}`,
        action: `Rassemblement ${r.date_rassemblement} — ${r.brigade_service}`,
        createdAt: r.created_at,
        type: "rassemblement",
        path: `/sg/rassemblement-journalier/${r.id}`,
      });
    });
    evenements.forEach((e) => {
      items.push({
        id: `evenement-${e.id}`,
        action: `Événement ${EVENEMENT_TYPE_LABELS[e.type_evenement] ?? e.type_evenement} — ${e.lieu_exact}`,
        createdAt: e.created_at,
        type: "evenement",
        path: `/sg/evenements-survenus/${e.id}`,
      });
    });
    activites.forEach((a) => {
      items.push({
        id: `activite-${a.id}`,
        action: `Activité ${a.date_activite} — ${a.nature_intervention || "Patrouille"}`,
        createdAt: a.created_at,
        type: "activite",
        path: `/sg/activites/${a.id}`,
      });
    });
    dispositifs.forEach((d) => {
      items.push({
        id: `dispositif-${d.id}`,
        action: `Dispositif exceptionnel — ${d.nature_evenement}`,
        createdAt: d.created_at,
        type: "dispositif",
        path: `/sg/dispositifs-exceptionnels/${d.id}`,
      });
    });
    return items
      .sort((a, b) => new Date(b.createdAt.replace(" ", "T")).getTime() - new Date(a.createdAt.replace(" ", "T")).getTime())
      .slice(0, 8);
  }, [rassemblements, evenements, activites, dispositifs]);

  // Quick actions: navigate to the new-form route, gated by can_create.
  const quickActions = useMemo(() => {
    const actions: { label: string; icon: typeof ClipboardList; path: string }[] = [];
    if (canCreateRassemblement) {
      actions.push({ label: "Nouveau rassemblement", icon: ClipboardList, path: "/sg/rassemblement-journalier/new" });
    }
    if (canCreateEvenement) {
      actions.push({ label: "Signaler un événement", icon: Siren, path: "/sg/evenements-survenus/new" });
    }
    if (canCreateActivite) {
      actions.push({ label: "Nouvelle activité", icon: Route, path: "/sg/activites/new" });
    }
    if (canCreateDispositif) {
      actions.push({ label: "Nouveau dispositif", icon: ShieldAlert, path: "/sg/dispositifs-exceptionnels/new" });
    }
    return actions;
  }, [canCreateRassemblement, canCreateEvenement, canCreateActivite, canCreateDispositif]);

  const activityDotColor: Record<ActivityType, string> = {
    rassemblement: "bg-emerald-500/70",
    evenement: "bg-red-500/70",
    activite: "bg-blue-500/70",
    dispositif: "bg-amber-500/70",
  };

  return (
    <div className="relative min-h-full">

      <motion.div variants={container} initial="hidden" animate="show" className="relative z-10 space-y-8">
        {/* Header */}
        <motion.div variants={item}>
          <div className="relative flex items-center gap-6 rounded-xl border border-border/50 bg-card/50 p-6 backdrop-blur-sm overflow-hidden">
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-green-500 via-white to-red-500" />
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoPnSrc} alt="PN" className="h-20 w-auto object-contain" />
            </div>
            <div className="flex-1 min-w-0">
              <span className="text-xs font-medium text-primary uppercase tracking-wider">Division Service Général</span>
              <h1 className="text-2xl font-bold tracking-tight">Patrouilles, interventions et sécurité</h1>
              <p className="text-sm text-muted-foreground mt-1">Vue d'ensemble des opérations</p>
            </div>
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoCspSrc} alt="CSP" className="h-20 w-auto object-contain" />
            </div>
          </div>
        </motion.div>

        {/* Stat cards */}
        {loading ? (
          <motion.div variants={item} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Card key={i} className="relative overflow-hidden">
                <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <Skeleton variant="text" width="60%" />
                  <Skeleton variant="circular" width={20} height={20} />
                </CardHeader>
                <CardContent>
                  <Skeleton height={28} width="40%" />
                  <Skeleton variant="text" width="70%" className="mt-1" />
                </CardContent>
              </Card>
            ))}
          </motion.div>
        ) : stats.length === 0 ? (
          <motion.div variants={item}>
            <Card>
              <CardContent className="py-8 text-center text-sm text-muted-foreground">
                Aucun module Service Général accessible pour votre rôle.
              </CardContent>
            </Card>
          </motion.div>
        ) : (
          <motion.div variants={item} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {stats.map((stat) => {
              const Icon = stat.icon;
              return (
                <button key={stat.label} onClick={() => navigate(stat.path)} className="group text-left">
                  <Card className="relative overflow-hidden transition-all duration-200 group-hover:-translate-y-0.5 group-hover:shadow-lg h-full">
                    <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                    <CardHeader className="flex flex-row items-center justify-between pb-2">
                      <CardTitle className="text-sm font-medium text-muted-foreground">{stat.label}</CardTitle>
                      <div className="rounded-full bg-primary/10 p-1.5">
                        <Icon className="h-4 w-4 text-primary" />
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="flex items-baseline gap-2">
                        <span className="text-2xl font-bold tabular-nums">{stat.value}</span>
                      </div>
                      <p className="text-xs text-muted-foreground mt-1">{stat.change}</p>
                    </CardContent>
                  </Card>
                </button>
              );
            })}
          </motion.div>
        )}

        <div className="grid gap-6 lg:grid-cols-2">
          {/* Recent activity */}
          <motion.div variants={item}>
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  Activité récente
                </CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                {loading ? (
                  <div className="divide-y divide-border">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <div key={i} className="flex items-center gap-3 px-6 py-3">
                        <Skeleton variant="circular" width={8} height={8} />
                        <Skeleton variant="text" width="70%" />
                      </div>
                    ))}
                  </div>
                ) : recentActivity.length === 0 ? (
                  <p className="px-6 py-6 text-sm text-muted-foreground">
                    Aucune activité récente à afficher.
                  </p>
                ) : (
                  <div className="divide-y divide-border">
                    {recentActivity.map((a) => (
                      <button
                        key={a.id}
                        onClick={() => navigate(a.path)}
                        className="w-full flex items-center gap-3 px-6 py-3 text-sm text-left hover:bg-accent/50 transition-colors"
                      >
                        <span className={`h-2 w-2 rounded-full shrink-0 ${activityDotColor[a.type]}`} />
                        <span className="flex-1 min-w-0 truncate">{a.action}</span>
                        <span className="text-xs text-muted-foreground shrink-0">
                          {formatRelativeTime(a.createdAt)}
                        </span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground/40 shrink-0" />
                      </button>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>

          {/* Quick actions */}
          <motion.div variants={item}>
            <Card className="h-full">
              <CardHeader>
                <CardTitle className="text-lg">Actions rapides</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {loading ? (
                  Array.from({ length: 4 }).map((_, i) => (
                    <Skeleton key={i} height={36} className="w-full" />
                  ))
                ) : quickActions.length === 0 ? (
                  <p className="text-sm text-muted-foreground py-2">
                    Aucune action disponible pour vos permissions.
                  </p>
                ) : (
                  quickActions.map((action) => {
                    const Icon = action.icon;
                    return (
                      <button
                        key={action.label}
                        onClick={() => navigate(action.path)}
                        className="group w-full flex items-center gap-3 rounded-lg px-4 py-2.5 text-sm hover:bg-accent transition-colors text-left"
                      >
                        <div className="rounded-md bg-primary/10 p-1.5 transition-colors group-hover:bg-primary/15">
                          <Icon className="h-4 w-4 text-primary" />
                        </div>
                        {action.label}
                        <ChevronRight className="h-4 w-4 text-muted-foreground/40 ml-auto" />
                      </button>
                    );
                  })
                )}
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
}
