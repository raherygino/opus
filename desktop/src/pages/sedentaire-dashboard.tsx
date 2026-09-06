import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Mail,
  Users,
  FileText,
  ArrowRightLeft,
  ArrowUpRight,
  Clock,
  Inbox,
  ChevronRight,
} from "lucide-react";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getCorrespondanceList } from "@/lib/api/correspondance";
import { getDeclarationPerteList } from "@/lib/api/declaration-perte";
import { getPassationList } from "@/lib/api/passation";
import { getPersonnelList } from "@/lib/api/personnel";
import type {
  Correspondance,
  DeclarationPerte,
  Passation,
  Personnel,
} from "@/types";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

const MODULE_CORRESPONDANCE = "sedentaire_secretariat_correspondance";
const MODULE_DECLARATION_PERTE = "sedentaire_secretariat_declaration_perte";
const MODULE_PASSATION = "sedentaire_poste_passation";
const MODULE_PERSONNEL = "personnel";

interface ActivityItem {
  id: string;
  action: string;
  createdAt: string;
  type: "correspondance" | "declaration" | "passation" | "personnel";
  path: string;
}

/** Format an ISO timestamp as a French relative time string. */
function formatRelativeTime(iso: string): string {
  const then = new Date(iso).getTime();
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

export function SedentaireDashboard() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [correspondances, setCorrespondances] = useState<Correspondance[]>([]);
  const [declarations, setDeclarations] = useState<DeclarationPerte[]>([]);
  const [passations, setPassations] = useState<Passation[]>([]);
  const [personnel, setPersonnel] = useState<Personnel[]>([]);
  const [loading, setLoading] = useState(true);

  const canViewCorrespondance = hasPermission(user, MODULE_CORRESPONDANCE, "can_view");
  const canViewDeclaration = hasPermission(user, MODULE_DECLARATION_PERTE, "can_view");
  const canViewPassation = hasPermission(user, MODULE_PASSATION, "can_view");
  const canViewPersonnel = hasPermission(user, MODULE_PERSONNEL, "can_view");

  const canCreateCorrespondance = hasPermission(user, MODULE_CORRESPONDANCE, "can_create");
  const canCreateDeclaration = hasPermission(user, MODULE_DECLARATION_PERTE, "can_create");
  const canCreatePassation = hasPermission(user, MODULE_PASSATION, "can_create");
  const canCreatePersonnel = hasPermission(user, MODULE_PERSONNEL, "can_create");

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadDashboard() {
    setLoading(true);
    // Fetch each module in parallel; a failure in one doesn't block the others.
    const tasks: Promise<void>[] = [];
    if (canViewCorrespondance) {
      tasks.push(
        getCorrespondanceList()
          .then(setCorrespondances)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les correspondances")),
      );
    }
    if (canViewDeclaration) {
      tasks.push(
        getDeclarationPerteList()
          .then(setDeclarations)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les déclarations de perte")),
      );
    }
    if (canViewPassation) {
      tasks.push(
        getPassationList()
          .then(setPassations)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les passations")),
      );
    }
    if (canViewPersonnel) {
      tasks.push(
        getPersonnelList()
          .then(setPersonnel)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger le personnel")),
      );
    }
    await Promise.all(tasks);
    setLoading(false);
  }

  // Build the stat cards from real counts, gated by view permission.
  const stats = useMemo(() => {
    const cards: {
      label: string;
      value: number;
      change: string;
      icon: typeof Mail;
      path: string;
    }[] = [];
    if (canViewCorrespondance) {
      cards.push({
        label: "Correspondances",
        value: correspondances.length,
        change: `${correspondances.filter((c) => c.sens === "Entrant").length} entrants`,
        icon: Mail,
        path: "/sedentaire/secretariat/correspondance",
      });
    }
    if (canViewPersonnel) {
      cards.push({
        label: "Personnel actif",
        value: personnel.length,
        change: `${personnel.filter((p) => p.status === "Actif").length} en service`,
        icon: Users,
        path: "/personnel",
      });
    }
    if (canViewDeclaration) {
      cards.push({
        label: "Déclarations de perte",
        value: declarations.length,
        change: "Total enregistré",
        icon: FileText,
        path: "/sedentaire/secretariat/declaration-perte",
      });
    }
    if (canViewPassation) {
      cards.push({
        label: "Passations",
        value: passations.length,
        change: "Total enregistré",
        icon: ArrowRightLeft,
        path: "/sedentaire/poste/passation",
      });
    }
    return cards;
  }, [
    canViewCorrespondance,
    canViewPersonnel,
    canViewDeclaration,
    canViewPassation,
    correspondances,
    personnel,
    declarations,
    passations,
  ]);

  // Merge recent items across all modules, sorted by created_at descending.
  const recentActivity = useMemo<ActivityItem[]>(() => {
    const items: ActivityItem[] = [];
    correspondances.forEach((c) => {
      items.push({
        id: `corr-${c.id}`,
        action: `Correspondance ${c.sens.toLowerCase()} #${c.id} — ${c.objet || c.reference || "sans objet"}`,
        createdAt: c.created_at,
        type: "correspondance",
        path: `/sedentaire/secretariat/correspondance/${c.id}`,
      });
    });
    declarations.forEach((d) => {
      items.push({
        id: `decl-${d.id}`,
        action: `Déclaration de perte #${d.id} — ${d.nature_objet || d.identite_declarant || ""}`,
        createdAt: d.created_at,
        type: "declaration",
        path: `/sedentaire/secretariat/declaration-perte/${d.id}`,
      });
    });
    passations.forEach((p) => {
      items.push({
        id: `pass-${p.id}`,
        action: `Passation #${p.id} — ${p.chef_descendant_lastname || ""} → ${p.chef_montant_lastname || ""}`.trim(),
        createdAt: p.created_at,
        type: "passation",
        path: `/sedentaire/poste/passation/${p.id}`,
      });
    });
    personnel.forEach((p) => {
      items.push({
        id: `pers-${p.id}`,
        action: `Personnel enregistré — ${p.grade || ""} ${p.lastname || ""} ${p.firstname || ""}`.trim(),
        createdAt: p.created_at,
        type: "personnel",
        path: `/personnel/${p.id}`,
      });
    });
    return items
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 8);
  }, [correspondances, declarations, passations, personnel]);

  // Quick actions: navigate to the new-form route, gated by can_create.
  const quickActions = useMemo(() => {
    const actions: { label: string; icon: typeof Mail; path: string }[] = [];
    if (canCreateCorrespondance) {
      actions.push({
        label: "Enregistrer un courrier",
        icon: Mail,
        path: "/sedentaire/secretariat/correspondance/new",
      });
    }
    if (canCreateDeclaration) {
      actions.push({
        label: "Déclaration de perte",
        icon: FileText,
        path: "/sedentaire/secretariat/declaration-perte/new",
      });
    }
    if (canCreatePassation) {
      actions.push({
        label: "Nouvelle passation",
        icon: ArrowRightLeft,
        path: "/sedentaire/poste/passation/new",
      });
    }
    if (canCreatePersonnel) {
      actions.push({
        label: "Nouveau personnel",
        icon: Users,
        path: "/personnel/new",
      });
    }
    return actions;
  }, [
    canCreateCorrespondance,
    canCreateDeclaration,
    canCreatePassation,
    canCreatePersonnel,
  ]);

  const activityDotColor: Record<ActivityItem["type"], string> = {
    correspondance: "bg-blue-500/70",
    declaration: "bg-amber-500/70",
    passation: "bg-purple-500/70",
    personnel: "bg-green-500/70",
  };

  return (
    <div className="relative min-h-full">

      <motion.div variants={container} initial="hidden" animate="show" className="relative z-10 space-y-8">
        <motion.div variants={item}>
          <div className="relative flex items-center gap-6 rounded-xl border border-border/50 bg-card/50 p-6 backdrop-blur-sm overflow-hidden">
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-green-500 via-white to-red-500" />
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoPnSrc} alt="PN" className="h-20 w-auto object-contain" />
            </div>
            <div className="flex-1 min-w-0">
              <span className="text-xs font-medium text-primary uppercase tracking-wider">Division Sédentaire</span>
              <h1 className="text-2xl font-bold tracking-tight">Secrétariat et Chef de Poste</h1>
              <p className="text-sm text-muted-foreground mt-1">Vue d'ensemble des activités</p>
            </div>
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoCspSrc} alt="CSP" className="h-20 w-auto object-contain" />
            </div>
          </div>
        </motion.div>

        <motion.div variants={item} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {loading
            ? Array.from({ length: stats.length || 4 }).map((_, i) => (
                <Card key={i} className="relative overflow-hidden">
                  <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                  <CardHeader className="flex flex-row items-center justify-between pb-2">
                    <Skeleton variant="text" width="60%" />
                    <Skeleton variant="circular" width={20} height={20} />
                  </CardHeader>
                  <CardContent>
                    <Skeleton height={28} width="50%" />
                  </CardContent>
                </Card>
              ))
            : stats.map((stat) => {
                const Icon = stat.icon;
                return (
                  <button
                    key={stat.label}
                    onClick={() => navigate(stat.path)}
                    className="text-left"
                  >
                    <Card className="relative overflow-hidden transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg">
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
                          <span className="inline-flex items-center text-xs font-medium text-muted-foreground">
                            <ArrowUpRight className="h-3 w-3 mr-0.5" />
                            {stat.change}
                          </span>
                        </div>
                      </CardContent>
                    </Card>
                  </button>
                );
              })}
        </motion.div>

        <div className="grid gap-6 lg:grid-cols-2">
          <motion.div variants={item}>
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  Activité récente
                </CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                {loading ? (
                  <div className="space-y-3 px-6 py-4">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <div key={i} className="flex items-center gap-3">
                        <Skeleton variant="circular" width={8} height={8} />
                        <Skeleton variant="text" className="flex-1" />
                        <Skeleton variant="text" width={60} />
                      </div>
                    ))}
                  </div>
                ) : recentActivity.length === 0 ? (
                  <div className="px-6 py-8 text-sm text-muted-foreground text-center">
                    Aucune activité récente à afficher.
                  </div>
                ) : (
                  <div className="divide-y divide-border">
                    {recentActivity.map((act) => (
                      <button
                        key={act.id}
                        onClick={() => navigate(act.path)}
                        className="group flex w-full items-center gap-3 px-6 py-3 text-sm text-left hover:bg-accent/50 transition-colors"
                      >
                        <div className={`h-2 w-2 rounded-full shrink-0 ${activityDotColor[act.type]}`} />
                        <span className="flex-1 truncate">{act.action}</span>
                        <span className="text-xs text-muted-foreground shrink-0">{formatRelativeTime(act.createdAt)}</span>
                        <ChevronRight className="h-4 w-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity shrink-0" />
                      </button>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </motion.div>

          <motion.div variants={item}>
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Inbox className="h-4 w-4" />
                  Actions rapides
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {quickActions.length === 0 ? (
                  <div className="py-8 text-sm text-muted-foreground text-center">
                    Aucune action disponible pour vos permissions.
                  </div>
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
                        <ChevronRight className="h-4 w-4 text-muted-foreground ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
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
