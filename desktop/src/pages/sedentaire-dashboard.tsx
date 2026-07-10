import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Mail,
  Users,
  FileText,
  Activity,
  ArrowUpRight,
  Clock,
  Inbox,
} from "lucide-react";
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

const stats = [
  { label: "Courriers traités", value: "24", change: "+3 cette semaine", icon: Mail, trend: "up" },
  { label: "Personnel actif", value: "12", change: "1 en congé", icon: Users, trend: "neutral" },
  { label: "Événements enregistrés", value: "48", change: "+8% vs hier", icon: Activity, trend: "up" },
  { label: "Rapports du jour", value: "3", change: "2 en attente", icon: FileText, trend: "neutral" },
];

const recentActivity = [
  { action: "Courrier entrant #024 enregistré", time: "Il y a 10 min", type: "mail" },
  { action: "Déclaration de perte #008 créée", time: "Il y a 25 min", type: "declaration" },
  { action: "Rapport journalier soumis", time: "Il y a 1h", type: "report" },
  { action: "Entrée visiteuse enregistrée", time: "Il y a 2h", type: "event" },
  { action: "Mouvement personnel enregistré", time: "Il y a 3h", type: "personnel" },
];

export function SedentaireDashboard() {
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
          {stats.map((stat) => {
            const Icon = stat.icon;
            return (
              <Card
                key={stat.label}
                className="relative overflow-hidden transition-all duration-200 hover:-translate-y-0.5 hover:shadow-lg"
              >
                <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-primary/20 via-primary/60 to-primary/20" />
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">{stat.label}</CardTitle>
                  <div className="rounded-full bg-primary/10 p-1.5">
                    <Icon className="h-4 w-4 text-primary" />
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-baseline gap-2">
                    <span className="text-2xl font-bold">{stat.value}</span>
                    <span className="inline-flex items-center text-xs font-medium text-green-500">
                      <ArrowUpRight className="h-3 w-3 mr-0.5" />
                      {stat.change}
                    </span>
                  </div>
                </CardContent>
              </Card>
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
                <div className="divide-y divide-border">
                  {recentActivity.map((act, i) => (
                    <div key={i} className="flex items-center gap-3 px-6 py-3 text-sm">
                      <div className="h-2 w-2 rounded-full bg-primary/60 shrink-0" />
                      <span className="flex-1">{act.action}</span>
                      <span className="text-xs text-muted-foreground shrink-0">{act.time}</span>
                    </div>
                  ))}
                </div>
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
                {[
                  { label: "Nouvel événement", icon: Activity },
                  { label: "Enregistrer courrier", icon: Mail },
                  { label: "Déclaration de perte", icon: FileText },
                  { label: "Rapport journalier", icon: FileText },
                ].map((action) => {
                  const Icon = action.icon;
                  return (
                    <button
                      key={action.label}
                      className="group w-full flex items-center gap-3 rounded-lg px-4 py-2.5 text-sm hover:bg-accent transition-colors text-left"
                    >
                      <div className="rounded-md bg-primary/10 p-1.5 transition-colors group-hover:bg-primary/15">
                        <Icon className="h-4 w-4 text-primary" />
                      </div>
                      {action.label}
                    </button>
                  );
                })}
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </motion.div>
    </div>
  );
}
