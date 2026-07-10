import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Scale,
  FileSearch,
  Lock,
  ScrollText,
  ArrowUpRight,
  Clock,
  Gavel,
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
  { label: "Enquêtes en cours", value: "8", change: "3 nouvelles cette semaine", icon: FileSearch, trend: "up" },
  { label: "GAV en cours", value: "2", change: "Limite légale respectée", icon: Lock, trend: "neutral" },
  { label: "Plaintes reçues", value: "15", change: "Ce mois", icon: ScrollText, trend: "up" },
  { label: "Mandats actifs", value: "4", change: "2 en attente d'exécution", icon: Scale, trend: "neutral" },
];

const pendingCases = [
  { ref: "PJ-2024-001", type: "Plainte directe", assigned: "I. Sow", status: "En cours" },
  { ref: "PJ-2024-002", type: "Saisine", assigned: "I. Sow", status: "Instruction" },
  { ref: "PJ-2024-003", type: "ST Parquet", assigned: "M. Kane", status: "Clôture" },
  { ref: "PJ-2024-004", type: "Plainte directe", assigned: "A. Ba", status: "Suspendue" },
];

export function PjDashboard() {
  return (
    <div className="relative min-h-full">

      <motion.div variants={container} initial="hidden" animate="show" className="relative z-10 mx-auto max-w-5xl space-y-8">
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
                  <Gavel className="h-4 w-4" />
                  Dossiers en cours
                </CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                <div className="divide-y divide-border">
                  {pendingCases.map((c, i) => (
                    <div key={i} className="flex items-center justify-between px-6 py-3 text-sm">
                      <div>
                        <span className="font-medium">{c.ref}</span>
                        <span className="text-muted-foreground ml-2">— {c.type}</span>
                        <div className="text-xs text-muted-foreground mt-0.5">{c.assigned}</div>
                      </div>
                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        c.status === "En cours" ? "bg-blue-500/10 text-blue-500" :
                        c.status === "Instruction" ? "bg-yellow-500/10 text-yellow-500" :
                        c.status === "Clôture" ? "bg-green-500/10 text-green-500" :
                        "bg-muted text-muted-foreground"
                      }`}>{c.status}</span>
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
                  <Clock className="h-4 w-4" />
                  Actions rapides
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {[
                  { label: "Nouvelle plainte", icon: ScrollText },
                  { label: "Enregistrer GAV", icon: Lock },
                  { label: "Créer mandat", icon: Scale },
                  { label: "Requérir expertise", icon: FileSearch },
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
