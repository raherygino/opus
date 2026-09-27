import { Fragment, useCallback, useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  ClipboardList,
  ChevronLeft,
  ChevronRight,
  FileDown,
  Inbox,
} from "lucide-react";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import { useNotificationStore } from "@/stores/notification-store";
import { getRapport } from "@/lib/api/rapport";
import type { Rapport, RapportType } from "@/types";

export const RAPPORT_MODULE = "sedentaire_secretariat_rapport";

const container = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.08 } },
};

const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0 },
};

/** Today's date in local time, formatted Y-m-d (the API anchor format). */
function todayLocal(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

/** Human-readable period label, derived from the server-computed bounds. */
function periodLabel(r: Rapport): string {
  if (r.type === "daily") return formatDate(r.period_start);
  if (r.type === "weekly") {
    return `Semaine du ${formatDate(r.period_start)} au ${formatDate(r.period_end)}`;
  }
  const d = new Date(`${r.period_start}T00:00:00`);
  const m = d.toLocaleDateString("fr-FR", { month: "long", year: "numeric" });
  return m.charAt(0).toUpperCase() + m.slice(1);
}

/** Shift the anchor date by one period (day / ISO week / month). */
function shiftDate(type: RapportType, date: string, delta: number): string {
  const [y, m, d] = date.split("-").map(Number);
  if (type === "daily") {
    const nd = new Date(y, m - 1, d + delta);
    return toAnchor(nd);
  }
  if (type === "weekly") {
    const nd = new Date(y, m - 1, d + delta * 7);
    return toAnchor(nd);
  }
  // Monthly: anchor on the 15th so month arithmetic never overflows.
  return toAnchor(new Date(y, m - 1 + delta, 15));
}

function toAnchor(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export function RapportPage() {
  const { addNotification } = useNotificationStore();

  const [type, setType] = useState<RapportType>("daily");
  const [date, setDate] = useState<string>(todayLocal());
  const [rapport, setRapport] = useState<Rapport | null>(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);

  const loadRapport = useCallback(async () => {
    setLoading(true);
    try {
      setRapport(await getRapport(type, date));
    } catch {
      setRapport(null);
      addNotification("error", "Erreur", "Impossible de charger le rapport");
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type, date]);

  useEffect(() => {
    loadRapport();
  }, [loadRapport]);

  function exportPdf() {
    if (!rapport) return;
    setExporting(true);
    try {
      const doc = new jsPDF({ orientation: "portrait", unit: "mm", format: "a4" });
      const pageWidth = doc.internal.pageSize.getWidth();
      const now = new Date().toLocaleString("fr-FR");

      // Header
      doc.setFontSize(16);
      doc.setFont("helvetica", "bold");
      doc.text(`OPUS — Rapport ${rapport.type_label.toLowerCase()}`, 14, 15);
      doc.setFontSize(10);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(100);
      doc.text(`Période : ${periodLabel(rapport)}`, 14, 22);
      doc.setFontSize(8);
      doc.text(`Généré le ${now}`, 14, 27);
      doc.setTextColor(0);

      // Body rows: a bold group header row per section group, then one row
      // per module — same data as the on-screen report.
      const body: ({ content: string; colSpan?: number; styles?: Record<string, unknown> } | string)[][] = [];
      rapport.groups.forEach((group) => {
        body.push([
          {
            content: group.label.toUpperCase(),
            colSpan: 4,
            styles: { fontStyle: "bold", fillColor: [238, 238, 245], textColor: [80, 80, 90] },
          },
        ]);
        group.sections.forEach((s) => {
          body.push([s.label, String(s.created), String(s.updated), String(s.total)]);
        });
      });

      autoTable(doc, {
        startY: 33,
        head: [["Module", "Créés", "Modifiés", "Total"]],
        body,
        foot: [[
          "Total général",
          String(rapport.totals.created),
          String(rapport.totals.updated),
          String(rapport.totals.total),
        ]],
        styles: { fontSize: 9, cellPadding: 2 },
        headStyles: { fillColor: [108, 99, 255], textColor: 255 },
        footStyles: { fillColor: [230, 230, 240], textColor: [20, 20, 30], fontStyle: "bold" },
        columnStyles: {
          1: { halign: "right", cellWidth: 25 },
          2: { halign: "right", cellWidth: 25 },
          3: { halign: "right", cellWidth: 25 },
        },
        didDrawPage: () => {
          const str = `Page ${doc.getNumberOfPages()}`;
          doc.setFontSize(8);
          doc.setTextColor(150);
          doc.text(str, pageWidth - 20, doc.internal.pageSize.getHeight() - 8);
          doc.setTextColor(0);
        },
      });

      doc.save(`rapport-${rapport.type}-${rapport.date}.pdf`);
      addNotification("success", "Export réussi", "Rapport exporté en PDF");
    } catch {
      addNotification("error", "Erreur", "Échec de l'export PDF");
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="relative min-h-full">
      <motion.div variants={container} initial="hidden" animate="show" className="relative z-10 space-y-6">
        {/* Header */}
        <motion.div variants={item}>
          <div className="relative flex items-center gap-6 rounded-xl border border-border/50 bg-card/50 p-6 backdrop-blur-sm overflow-hidden">
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-green-500 via-white to-red-500" />
            <div className="flex-1 min-w-0">
              <span className="text-xs font-medium text-primary uppercase tracking-wider">
                Sédentaire — Secrétariat
              </span>
              <h1 className="text-2xl font-bold tracking-tight">Rapport d'activité</h1>
              <p className="text-sm text-muted-foreground mt-1">
                {loading || !rapport ? "Chargement du rapport…" : periodLabel(rapport)}
              </p>
            </div>
            <Button
              onClick={exportPdf}
              disabled={!rapport || loading || exporting}
              className="shrink-0"
            >
              <FileDown className="h-4 w-4 mr-2" />
              {exporting ? "Export…" : "Exporter PDF"}
            </Button>
          </div>
        </motion.div>

        {/* Controls */}
        <motion.div variants={item}>
          <Card>
            <CardContent className="flex flex-wrap items-center gap-4 p-4">
              <Tabs value={type} onValueChange={(v) => setType(v as RapportType)}>
                <TabsList>
                  <TabsTrigger value="daily">Journalier</TabsTrigger>
                  <TabsTrigger value="weekly">Hebdomadaire</TabsTrigger>
                  <TabsTrigger value="monthly">Mensuel</TabsTrigger>
                </TabsList>
              </Tabs>
              <div className="flex items-center gap-2 ml-auto">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setDate((d) => shiftDate(type, d, -1))}
                  aria-label="Période précédente"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Input
                  type="date"
                  value={date}
                  onChange={(e) => e.target.value && setDate(e.target.value)}
                  className="w-[150px]"
                  aria-label="Date du rapport"
                />
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setDate((d) => shiftDate(type, d, 1))}
                  aria-label="Période suivante"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
                <Button variant="ghost" size="sm" onClick={() => setDate(todayLocal())}>
                  Aujourd'hui
                </Button>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Report table */}
        <motion.div variants={item}>
          <Card>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <ClipboardList className="h-4 w-4" />
                Récapitulatif par module
              </CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {loading ? (
                <div className="space-y-3 px-6 py-5">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <Skeleton key={i} variant="text" className="h-5 w-full" />
                  ))}
                </div>
              ) : !rapport ? (
                <div className="px-6 py-8 text-sm text-muted-foreground text-center">
                  Rapport indisponible pour cette période.
                </div>
              ) : (
                <>
                  {rapport.totals.total === 0 && (
                    <div className="mx-6 mt-2 mb-1 flex items-center gap-2 rounded-lg bg-muted/50 px-4 py-2.5 text-sm text-muted-foreground">
                      <Inbox className="h-4 w-4 shrink-0" />
                      Aucun enregistrement créé ou modifié sur cette période.
                    </div>
                  )}
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-muted-foreground">
                        <th className="text-left font-medium px-6 py-3">Module</th>
                        <th className="text-right font-medium px-4 py-3 w-[110px]">Créés</th>
                        <th className="text-right font-medium px-4 py-3 w-[110px]">Modifiés</th>
                        <th className="text-right font-medium px-6 py-3 w-[110px]">Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rapport.groups.map((group) => (
                        <Fragment key={group.key}>
                          <tr className="bg-muted/40">
                            <td
                              colSpan={4}
                              className="px-6 py-2 text-xs font-semibold uppercase tracking-wider text-muted-foreground"
                            >
                              {group.label}
                            </td>
                          </tr>
                          {group.sections.map((s) => (
                            <tr
                              key={s.key}
                              className="border-b border-border/50 last:border-0 hover:bg-accent/30 transition-colors"
                            >
                              <td className="px-6 py-2.5">{s.label}</td>
                              <td className="px-4 py-2.5 text-right tabular-nums">{s.created}</td>
                              <td className="px-4 py-2.5 text-right tabular-nums text-muted-foreground">
                                {s.updated}
                              </td>
                              <td className="px-6 py-2.5 text-right tabular-nums font-medium">
                                {s.total}
                              </td>
                            </tr>
                          ))}
                        </Fragment>
                      ))}
                      <tr className="bg-primary/5 font-semibold border-t border-border">
                        <td className="px-6 py-3">Total général</td>
                        <td className="px-4 py-3 text-right tabular-nums">{rapport.totals.created}</td>
                        <td className="px-4 py-3 text-right tabular-nums">{rapport.totals.updated}</td>
                        <td className="px-6 py-3 text-right tabular-nums">{rapport.totals.total}</td>
                      </tr>
                    </tbody>
                  </table>
                </>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </motion.div>
    </div>
  );
}
