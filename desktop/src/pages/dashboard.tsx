import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Shield,
  Users,
  Building2,
  Car,
  Scale,
  Activity,
  UserCheck,
  ChevronRight,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import logoPnSrc from "@/assets/img/logo-pn.png";
import logoCspSrc from "@/assets/img/logo-csp.png";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

export function Dashboard() {
  const { user } = useAuthStore();
  const navigate = useNavigate();

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

        {/* Quick stats */}
        <motion.div
          variants={item}
          className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
        >
          {[
            { label: "Personnel actif", value: "—", icon: UserCheck, desc: "Total effectif" },
            { label: "Divisions", value: "3", icon: Building2, desc: "Sédentaire, SG, PJ" },
            { label: "Utilisateurs", value: "—", icon: Shield, desc: "Comptes système" },
            { label: "Activité", value: "—", icon: Activity, desc: "Aujourd'hui" },
          ].map((stat) => {
            const Icon = stat.icon;
            return (
              <Card
                key={stat.label}
                className="relative overflow-hidden transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    {stat.label}
                  </CardTitle>
                  <div className="rounded-full bg-primary/10 p-1.5">
                    <Icon className="h-4 w-4 text-primary" />
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-baseline gap-2">
                    <span className="text-2xl font-bold">{stat.value}</span>
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">{stat.desc}</p>
                </CardContent>
              </Card>
            );
          })}
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
                { label: "Matricule (IM)", value: user?.im },
                { label: "Grade", value: user?.grade },
                { label: "Fonction", value: user?.fonction },
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
