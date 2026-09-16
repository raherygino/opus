import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Scale,
  FileSearch,
  Lock,
  ScrollText,
  ArrowUpRight,
  Clock,
  Gavel,
  ShieldBan,
  FileInput,
  FileUp,
  UserRoundSearch,
  Box,
  Search,
  MessageCircleMore,
  ChevronRight,
  Inbox,
} from "lucide-react";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getPlainteEntreeList, getPlaintesEntreeWithoutSortie } from "@/lib/api/plainte";
import { getMandatList } from "@/lib/api/mandat";
import { getConvocationList } from "@/lib/api/convocation";
import { getArrestationList } from "@/lib/api/arrestation";
import { getGardeAVueList } from "@/lib/api/garde-a-vue";
import { getRequisitionList } from "@/lib/api/requisition";
import { getPersonneRechercheeList } from "@/lib/api/personne-recherchee";
import { getObjetSaisiList, getObjetTrouveList } from "@/lib/api/objet";
import { getPerquisitionList } from "@/lib/api/perquisition";
import { getRenseignementPjList } from "@/lib/api/renseignement-pj";
import type {
  PlainteEntree,
  PlainteEntreeSummary,
  Mandat,
  Convocation,
  Arrestation,
  GardeAVue,
  Requisition,
  PersonneRecherchee,
  ObjetSaisi,
  ObjetTrouve,
  Perquisition,
  RenseignementPj,
} from "@/types";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

// Permission module codes for each PJ feature.
const MODULE_PLAINTE = "pj_plainte";
const MODULE_ENQUETE = "pj_enquete";
const MODULE_MANDAT = "pj_mandat";
const MODULE_CONVOCATION = "pj_convocation";
const MODULE_ARRESTATION = "pj_arrestation";
const MODULE_GAV = "pj_gav";
const MODULE_REQUISITION = "pj_requisition";
const MODULE_PERSONNE_RECHERCHEE = "pj_personne_recherchee";
const MODULE_OBJETS = "pj_objets";
const MODULE_PERQUISITION = "pj_perquisition";
const MODULE_DEFERREMENT = "pj_deferrement";
const MODULE_RENSEIGNEMENT = "pj_renseignement";

interface ActivityItem {
  id: string;
  action: string;
  createdAt: string;
  type: ActivityType;
  path: string;
}

type ActivityType =
  | "plainte"
  | "mandat"
  | "convocation"
  | "arrestation"
  | "gav"
  | "requisition"
  | "personne_recherchee"
  | "objet_saisi"
  | "objet_trouve"
  | "perquisition"
  | "renseignement";

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

const PLAINTE_TYPE_LABELS: Record<string, string> = {
  ST_PARQUET: "ST Parquet",
  PLAINTE_DIRECTE: "Plainte directe",
  RAPPORT_POLICE: "Rapport de police",
};

export function PjDashboard() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [plaintes, setPlaintes] = useState<PlainteEntree[]>([]);
  const [plaintesPending, setPlaintesPending] = useState<PlainteEntreeSummary[]>([]);
  const [mandats, setMandats] = useState<Mandat[]>([]);
  const [convocations, setConvocations] = useState<Convocation[]>([]);
  const [arrestations, setArrestations] = useState<Arrestation[]>([]);
  const [gavs, setGavs] = useState<GardeAVue[]>([]);
  const [requisitions, setRequisitions] = useState<Requisition[]>([]);
  const [personnesRecherchees, setPersonnesRecherchees] = useState<PersonneRecherchee[]>([]);
  const [objetsSaisis, setObjetsSaisis] = useState<ObjetSaisi[]>([]);
  const [objetsTrouves, setObjetsTrouves] = useState<ObjetTrouve[]>([]);
  const [perquisitions, setPerquisitions] = useState<Perquisition[]>([]);
  const [renseignements, setRenseignements] = useState<RenseignementPj[]>([]);
  const [loading, setLoading] = useState(true);

  const canViewPlainte = hasPermission(user, MODULE_PLAINTE, "can_view");
  const canViewEnquete = hasPermission(user, MODULE_ENQUETE, "can_view");
  const canViewMandat = hasPermission(user, MODULE_MANDAT, "can_view");
  const canViewConvocation = hasPermission(user, MODULE_CONVOCATION, "can_view");
  const canViewArrestation = hasPermission(user, MODULE_ARRESTATION, "can_view");
  const canViewGav = hasPermission(user, MODULE_GAV, "can_view");
  const canViewRequisition = hasPermission(user, MODULE_REQUISITION, "can_view");
  const canViewPersonneRecherchee = hasPermission(user, MODULE_PERSONNE_RECHERCHEE, "can_view");
  const canViewObjets = hasPermission(user, MODULE_OBJETS, "can_view");
  const canViewPerquisition = hasPermission(user, MODULE_PERQUISITION, "can_view");
  const canViewDeferrement = hasPermission(user, MODULE_DEFERREMENT, "can_view");
  const canViewRenseignement = hasPermission(user, MODULE_RENSEIGNEMENT, "can_view");

  const canCreatePlainte = hasPermission(user, MODULE_PLAINTE, "can_create");
  const canCreateEnquete = hasPermission(user, MODULE_ENQUETE, "can_create");
  const canCreateMandat = hasPermission(user, MODULE_MANDAT, "can_create");
  const canCreateConvocation = hasPermission(user, MODULE_CONVOCATION, "can_create");
  const canCreateArrestation = hasPermission(user, MODULE_ARRESTATION, "can_create");
  const canCreateGav = hasPermission(user, MODULE_GAV, "can_create");
  const canCreateRequisition = hasPermission(user, MODULE_REQUISITION, "can_create");
  const canCreatePersonneRecherchee = hasPermission(user, MODULE_PERSONNE_RECHERCHEE, "can_create");
  const canCreateObjets = hasPermission(user, MODULE_OBJETS, "can_create");
  const canCreatePerquisition = hasPermission(user, MODULE_PERQUISITION, "can_create");
  const canCreateDeferrement = hasPermission(user, MODULE_DEFERREMENT, "can_create");
  const canCreateRenseignement = hasPermission(user, MODULE_RENSEIGNEMENT, "can_create");

  useEffect(() => {
    loadDashboard();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadDashboard() {
    setLoading(true);
    const tasks: Promise<void>[] = [];
    if (canViewPlainte) {
      tasks.push(
        getPlainteEntreeList()
          .then(setPlaintes)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les plaintes")),
      );
      tasks.push(
        getPlaintesEntreeWithoutSortie()
          .then(setPlaintesPending)
          .catch(() => {}),
      );
    }
    if (canViewMandat) {
      tasks.push(
        getMandatList()
          .then(setMandats)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les mandats")),
      );
    }
    if (canViewConvocation) {
      tasks.push(
        getConvocationList()
          .then(setConvocations)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les convocations")),
      );
    }
    if (canViewArrestation) {
      tasks.push(
        getArrestationList()
          .then(setArrestations)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les arrestations")),
      );
    }
    if (canViewGav) {
      tasks.push(
        getGardeAVueList()
          .then(setGavs)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les GAV")),
      );
    }
    if (canViewRequisition) {
      tasks.push(
        getRequisitionList()
          .then(setRequisitions)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les réquisitions")),
      );
    }
    if (canViewPersonneRecherchee) {
      tasks.push(
        getPersonneRechercheeList()
          .then(setPersonnesRecherchees)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les personnes recherchées")),
      );
    }
    if (canViewObjets) {
      tasks.push(
        getObjetSaisiList()
          .then(setObjetsSaisis)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les objets saisis")),
      );
      tasks.push(
        getObjetTrouveList()
          .then(setObjetsTrouves)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les objets trouvés")),
      );
    }
    if (canViewPerquisition) {
      tasks.push(
        getPerquisitionList()
          .then(setPerquisitions)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les perquisitions")),
      );
    }
    if (canViewRenseignement) {
      tasks.push(
        getRenseignementPjList()
          .then(setRenseignements)
          .catch(() => addNotification("error", "Erreur", "Impossible de charger les renseignements")),
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
      icon: typeof Scale;
      path: string;
      comingSoon?: boolean;
    }[] = [];
    if (canViewPlainte) {
      cards.push({
        label: "Plaintes reçues",
        value: plaintes.length,
        change: `${plaintesPending.length} sans sortie`,
        icon: ScrollText,
        path: "/pj/plainte",
      });
    }
    if (canViewEnquete) {
      cards.push({
        label: "Registre d'enquête",
        value: 0,
        change: "Bientôt disponible",
        icon: FileSearch,
        path: "/pj/registre-enquete",
        comingSoon: true,
      });
    }
    if (canViewMandat) {
      cards.push({
        label: "Mandats",
        value: mandats.length,
        change: "Total enregistré",
        icon: Scale,
        path: "/pj/mandat",
      });
    }
    if (canViewConvocation) {
      cards.push({
        label: "Convocations",
        value: convocations.length,
        change: "Total enregistré",
        icon: FileInput,
        path: "/pj/convocation",
      });
    }
    if (canViewArrestation) {
      cards.push({
        label: "Arrestations",
        value: arrestations.length,
        change: "Total enregistré",
        icon: ShieldBan,
        path: "/pj/arrestation",
      });
    }
    if (canViewGav) {
      cards.push({
        label: "Gardes à vue",
        value: gavs.length,
        change: "Total enregistré",
        icon: Lock,
        path: "/pj/gav",
      });
    }
    if (canViewRequisition) {
      cards.push({
        label: "Réquisitions",
        value: requisitions.length,
        change: "Total enregistré",
        icon: FileUp,
        path: "/pj/requisition",
      });
    }
    if (canViewPersonneRecherchee) {
      cards.push({
        label: "Personnes recherchées",
        value: personnesRecherchees.length,
        change: "Total enregistré",
        icon: UserRoundSearch,
        path: "/pj/personne-recherchee",
      });
    }
    if (canViewObjets) {
      cards.push({
        label: "Objets",
        value: objetsSaisis.length + objetsTrouves.length,
        change: `${objetsSaisis.length} saisis · ${objetsTrouves.length} trouvés`,
        icon: Box,
        path: "/pj/objets",
      });
    }
    if (canViewPerquisition) {
      cards.push({
        label: "Perquisitions",
        value: perquisitions.length,
        change: "Total enregistré",
        icon: Search,
        path: "/pj/perquisition",
      });
    }
    if (canViewDeferrement) {
      cards.push({
        label: "Registre de déferrement",
        value: 0,
        change: "Bientôt disponible",
        icon: Gavel,
        path: "/pj/registre-deferrement",
        comingSoon: true,
      });
    }
    if (canViewRenseignement) {
      cards.push({
        label: "Renseignements",
        value: renseignements.length,
        change: "Total enregistré",
        icon: MessageCircleMore,
        path: "/pj/renseignement",
      });
    }
    return cards;
  }, [
    canViewPlainte, canViewEnquete, canViewMandat, canViewConvocation, canViewArrestation,
    canViewGav, canViewRequisition, canViewPersonneRecherchee, canViewObjets,
    canViewPerquisition, canViewDeferrement, canViewRenseignement,
    plaintes, plaintesPending, mandats, convocations, arrestations, gavs,
    requisitions, personnesRecherchees, objetsSaisis, objetsTrouves,
    perquisitions, renseignements,
  ]);

  // Merge recent items across all PJ modules, sorted by created_at descending.
  const recentActivity = useMemo<ActivityItem[]>(() => {
    const items: ActivityItem[] = [];
    plaintes.forEach((p) => {
      items.push({
        id: `plainte-${p.id}`,
        action: `Plainte ${PLAINTE_TYPE_LABELS[p.type] ?? p.type} — ${p.numero_dossier} · ${p.infraction || p.mise_en_cause || ""}`.trim(),
        createdAt: p.created_at,
        type: "plainte",
        path: `/pj/plainte/${p.id}`,
      });
    });
    mandats.forEach((m) => {
      items.push({
        id: `mandat-${m.id}`,
        action: `Mandat ${m.numero} — ${m.personne_nom}`,
        createdAt: m.created_at,
        type: "mandat",
        path: `/pj/mandat/${m.id}`,
      });
    });
    convocations.forEach((c) => {
      items.push({
        id: `convocation-${c.id}`,
        action: `Convocation ${c.numero} — ${c.nom}`,
        createdAt: c.created_at,
        type: "convocation",
        path: `/pj/convocation/${c.id}`,
      });
    });
    arrestations.forEach((a) => {
      items.push({
        id: `arrestation-${a.id}`,
        action: `Arrestation ${a.numero} — ${a.personne_nom}`,
        createdAt: a.created_at,
        type: "arrestation",
        path: `/pj/arrestation/${a.id}`,
      });
    });
    gavs.forEach((g) => {
      items.push({
        id: `gav-${g.id}`,
        action: `GAV — ${g.nom}${g.prenoms ? " " + g.prenoms : ""}`.trim(),
        createdAt: g.created_at,
        type: "gav",
        path: `/pj/gav/${g.id}`,
      });
    });
    requisitions.forEach((r) => {
      items.push({
        id: `requisition-${r.id}`,
        action: `Réquisition ${r.numero} — ${r.affaire}`,
        createdAt: r.created_at,
        type: "requisition",
        path: `/pj/requisition/${r.id}`,
      });
    });
    personnesRecherchees.forEach((p) => {
      items.push({
        id: `recherchee-${p.id}`,
        action: `Personne recherchée — ${p.nom}`,
        createdAt: p.created_at,
        type: "personne_recherchee",
        path: `/pj/personne-recherchee/${p.id}`,
      });
    });
    objetsSaisis.forEach((o) => {
      items.push({
        id: `objet-saisi-${o.id}`,
        action: `Objet saisi — ${o.motif}${o.proprietaire ? " · " + o.proprietaire : ""}`.trim(),
        createdAt: o.created_at,
        type: "objet_saisi",
        path: `/pj/objets/saisi/${o.id}`,
      });
    });
    objetsTrouves.forEach((o) => {
      items.push({
        id: `objet-trouve-${o.id}`,
        action: `Objet trouvé — ${o.affaire}`,
        createdAt: o.created_at,
        type: "objet_trouve",
        path: `/pj/objets/trouve/${o.id}`,
      });
    });
    perquisitions.forEach((p) => {
      items.push({
        id: `perquisition-${p.id}`,
        action: `Perquisition ${p.numero} — ${p.affaire}`,
        createdAt: p.created_at,
        type: "perquisition",
        path: `/pj/perquisition/${p.id}`,
      });
    });
    renseignements.forEach((r) => {
      items.push({
        id: `renseignement-${r.id}`,
        action: `Renseignement PJ — ${r.nature_infraction}`,
        createdAt: r.created_at,
        type: "renseignement",
        path: `/pj/renseignement/${r.id}`,
      });
    });
    return items
      .sort((a, b) => new Date(b.createdAt.replace(" ", "T")).getTime() - new Date(a.createdAt.replace(" ", "T")).getTime())
      .slice(0, 8);
  }, [
    plaintes, mandats, convocations, arrestations, gavs, requisitions,
    personnesRecherchees, objetsSaisis, objetsTrouves, perquisitions, renseignements,
  ]);

  // Quick actions: navigate to the new-form route, gated by can_create.
  const quickActions = useMemo(() => {
    const actions: { label: string; icon: typeof Scale; path: string }[] = [];
    if (canCreatePlainte) {
      actions.push({ label: "Nouvelle plainte", icon: ScrollText, path: "/pj/plainte/new" });
    }
    if (canCreateEnquete) {
      actions.push({ label: "Registre d'enquête", icon: FileSearch, path: "/pj/registre-enquete" });
    }
    if (canCreateMandat) {
      actions.push({ label: "Créer un mandat", icon: Scale, path: "/pj/mandat/new" });
    }
    if (canCreateConvocation) {
      actions.push({ label: "Nouvelle convocation", icon: FileInput, path: "/pj/convocation/new" });
    }
    if (canCreateArrestation) {
      actions.push({ label: "Nouvelle arrestation", icon: ShieldBan, path: "/pj/arrestation/new" });
    }
    if (canCreateGav) {
      actions.push({ label: "Enregistrer une GAV", icon: Lock, path: "/pj/gav/new" });
    }
    if (canCreateRequisition) {
      actions.push({ label: "Nouvelle réquisition", icon: FileUp, path: "/pj/requisition/new" });
    }
    if (canCreatePersonneRecherchee) {
      actions.push({ label: "Personne recherchée", icon: UserRoundSearch, path: "/pj/personne-recherchee/new" });
    }
    if (canCreateObjets) {
      actions.push({ label: "Objet saisi", icon: Box, path: "/pj/objets/saisi/new" });
    }
    if (canCreatePerquisition) {
      actions.push({ label: "Nouvelle perquisition", icon: Search, path: "/pj/perquisition/new" });
    }
    if (canCreateDeferrement) {
      actions.push({ label: "Registre de déferrement", icon: Gavel, path: "/pj/registre-deferrement" });
    }
    if (canCreateRenseignement) {
      actions.push({ label: "Nouveau renseignement", icon: MessageCircleMore, path: "/pj/renseignement/new" });
    }
    return actions;
  }, [
    canCreatePlainte, canCreateEnquete, canCreateMandat, canCreateConvocation, canCreateArrestation,
    canCreateGav, canCreateRequisition, canCreatePersonneRecherchee, canCreateObjets,
    canCreatePerquisition, canCreateDeferrement, canCreateRenseignement,
  ]);

  const activityDotColor: Record<ActivityType, string> = {
    plainte: "bg-blue-500/70",
    mandat: "bg-purple-500/70",
    convocation: "bg-indigo-500/70",
    arrestation: "bg-red-500/70",
    gav: "bg-amber-500/70",
    requisition: "bg-teal-500/70",
    personne_recherchee: "bg-orange-500/70",
    objet_saisi: "bg-cyan-500/70",
    objet_trouve: "bg-emerald-500/70",
    perquisition: "bg-pink-500/70",
    renseignement: "bg-violet-500/70",
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
              <span className="text-xs font-medium text-primary uppercase tracking-wider">Division Police Judiciaire</span>
              <h1 className="text-2xl font-bold tracking-tight">Enquêtes, plaintes et procédure judiciaire</h1>
              <p className="text-sm text-muted-foreground mt-1">Vue d'ensemble des dossiers</p>
            </div>
            <div className="hidden lg:flex items-center shrink-0">
              <img src={logoCspSrc} alt="CSP" className="h-20 w-auto object-contain" />
            </div>
          </div>
        </motion.div>

        <motion.div variants={item} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {loading
            ? Array.from({ length: stats.length || 8 }).map((_, i) => (
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
                          {stat.comingSoon ? (
                            <span className="text-sm font-semibold text-amber-500">Bientôt</span>
                          ) : (
                            <span className="text-2xl font-bold tabular-nums">{stat.value}</span>
                          )}
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
