import { useState, useEffect, useCallback } from "react";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { getAuditLogs } from "@/lib/api/audit-logs";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  ScrollText,
  LogIn,
  LogOut,
  Plus,
  Pencil,
  Trash2,
  KeyRound,
  Image,
  RotateCcw,
  ShieldCheck,
  Filter,
  X,
  FileDown,
} from "lucide-react";
import type { AuditLog, AuditLogFilters } from "@/types";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

// ========================
// JSON Diff Viewer
// ========================
type DiffEntry = {
  key: string;
  oldValue: unknown;
  newValue: unknown;
  status: "unchanged" | "added" | "removed" | "modified";
};

function getDiffEntries(
  oldObj: Record<string, unknown> | null,
  newObj: Record<string, unknown> | null,
): DiffEntry[] {
  const oldV = oldObj ?? {};
  const newV = newObj ?? {};
  const allKeys = Array.from(
    new Set([...Object.keys(oldV), ...Object.keys(newV)]),
  ).sort();
  return allKeys.map((key) => {
    const ov = oldV[key];
    const nv = newV[key];
    const inOld = key in oldV;
    const inNew = key in newV;
    if (inOld && !inNew)
      return { key, oldValue: ov, newValue: undefined, status: "removed" };
    if (!inOld && inNew)
      return { key, oldValue: undefined, newValue: nv, status: "added" };
    if (JSON.stringify(ov) !== JSON.stringify(nv))
      return { key, oldValue: ov, newValue: nv, status: "modified" };
    return { key, oldValue: ov, newValue: nv, status: "unchanged" };
  });
}

function formatValue(val: unknown): string {
  if (val === undefined) return "—";
  if (val === null) return "null";
  if (typeof val === "object") return JSON.stringify(val, null, 2);
  return String(val);
}

function DiffRow({ entry }: { entry: DiffEntry }) {
  const { status, key, oldValue, newValue } = entry;
  const isChanged = status !== "unchanged";

  const oldBg =
    status === "removed" || status === "modified"
      ? "bg-red-500/10"
      : "";
  const newBg =
    status === "added" || status === "modified"
      ? "bg-green-500/10"
      : "";
  const oldText =
    status === "removed" || status === "modified"
      ? "text-red-600 dark:text-red-400"
      : "text-muted-foreground";
  const newText =
    status === "added" || status === "modified"
      ? "text-green-600 dark:text-green-400"
      : "text-muted-foreground";

  return (
    <div className="grid grid-cols-[minmax(100px,1fr)_1fr_1fr] border-b border-border/50 last:border-0">
      <div
        className={`px-2 py-1.5 text-xs font-medium border-r border-border/50 ${isChanged ? "font-semibold text-foreground" : "text-muted-foreground"}`}
      >
        {key}
      </div>
      <div
        className={`px-2 py-1.5 text-xs font-mono break-all whitespace-pre-wrap border-r border-border/50 ${oldBg} ${oldText}`}
      >
        {formatValue(oldValue)}
      </div>
      <div
        className={`px-2 py-1.5 text-xs font-mono break-all whitespace-pre-wrap ${newBg} ${newText}`}
      >
        {formatValue(newValue)}
      </div>
    </div>
  );
}

function JsonDiffView({
  oldValues,
  newValues,
}: {
  oldValues: string | null;
  newValues: string | null;
}) {
  let oldObj: Record<string, unknown> | null = null;
  let newObj: Record<string, unknown> | null = null;

  try {
    oldObj = oldValues ? JSON.parse(oldValues) : null;
  } catch {
    oldObj = null;
  }
  try {
    newObj = newValues ? JSON.parse(newValues) : null;
  } catch {
    newObj = null;
  }

  const entries = getDiffEntries(oldObj, newObj);
  const hasChanges = entries.some((e) => e.status !== "unchanged");

  if (!oldValues && !newValues) return null;

  // For non-object JSON (e.g. plain strings), fall back to simple display
  if (
    (oldObj && typeof oldObj !== "object") ||
    (newObj && typeof newObj !== "object")
  ) {
    return (
      <div className="grid grid-cols-2 gap-3">
        {oldValues && (
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">
              Anciennes valeurs
            </p>
            <pre className="text-xs bg-muted rounded-lg p-3 overflow-auto max-h-40 text-red-600 dark:text-red-400">
              {oldValues}
            </pre>
          </div>
        )}
        {newValues && (
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-1">
              Nouvelles valeurs
            </p>
            <pre className="text-xs bg-muted rounded-lg p-3 overflow-auto max-h-40 text-green-600 dark:text-green-400">
              {newValues}
            </pre>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-2">
        <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">
          Comparaison des valeurs
        </span>
        {hasChanges && (
          <span className="text-[10px] text-muted-foreground">
            ({entries.filter((e) => e.status !== "unchanged").length} champ(s) modifié(s))
          </span>
        )}
      </div>
      <div className="rounded-lg border border-border overflow-auto max-h-[300px]">
        <div className="sticky top-0 z-10 grid grid-cols-[minmax(100px,1fr)_1fr_1fr] bg-muted/80 backdrop-blur-sm border-b border-border">
          <div className="px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground border-r border-border/50">
            Champ
          </div>
          <div className="px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-red-600 dark:text-red-400 border-r border-border/50">
            Ancien
          </div>
          <div className="px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-green-600 dark:text-green-400">
            Nouveau
          </div>
        </div>
        {entries.map((entry) => (
          <DiffRow key={entry.key} entry={entry} />
        ))}
      </div>
    </div>
  );
}

const ACTION_LABELS: Record<string, string> = {
  login: "Connexion",
  logout: "Déconnexion",
  password_change: "Changement mot de passe",
  photo_upload: "Upload photo",
  create: "Création",
  update: "Modification",
  delete: "Suppression",
  retour: "Retour mouvement",
  update_permissions: "Permissions modifiées",
};

const MODULE_LABELS: Record<string, string> = {
  auth: "Authentification",
  personnel: "Personnel",
  users: "Utilisateurs",
  roles: "Rôles",
  mouvements: "Mouvements",
};

const ACTION_ICONS: Record<string, React.ElementType> = {
  login: LogIn,
  logout: LogOut,
  password_change: KeyRound,
  photo_upload: Image,
  create: Plus,
  update: Pencil,
  delete: Trash2,
  retour: RotateCcw,
  update_permissions: ShieldCheck,
};

const ACTION_COLORS: Record<string, string> = {
  login: "bg-blue-500/10 text-blue-500 border-blue-500/20",
  logout: "bg-slate-500/10 text-slate-500 border-slate-500/20",
  password_change: "bg-amber-500/10 text-amber-500 border-amber-500/20",
  photo_upload: "bg-purple-500/10 text-purple-500 border-purple-500/20",
  create: "bg-green-500/10 text-green-500 border-green-500/20",
  update: "bg-cyan-500/10 text-cyan-500 border-cyan-500/20",
  delete: "bg-red-500/10 text-red-500 border-red-500/20",
  retour: "bg-indigo-500/10 text-indigo-500 border-indigo-500/20",
  update_permissions: "bg-orange-500/10 text-orange-500 border-orange-500/20",
};

const MODULE_COLORS: Record<string, string> = {
  auth: "bg-blue-500/10 text-blue-500",
  personnel: "bg-green-500/10 text-green-500",
  users: "bg-purple-500/10 text-purple-500",
  roles: "bg-amber-500/10 text-amber-500",
  mouvements: "bg-cyan-500/10 text-cyan-500",
};

export function AuditLogPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);
  const [filters, setFilters] = useState<AuditLogFilters>({});
  const [showFilters, setShowFilters] = useState(false);
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const isSuperAdmin = user?.role_code === "SUPER_ADMIN";
  const [exporting, setExporting] = useState(false);

  const loadLogs = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAuditLogs(filters);
      setLogs(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les journaux d'audit");
    } finally {
      setLoading(false);
    }
  }, [filters, addNotification]);

  useEffect(() => {
    if (isSuperAdmin) {
      loadLogs();
    }
  }, [isSuperAdmin, loadLogs]);

  function applyFilters(newFilters: AuditLogFilters) {
    setFilters(newFilters);
  }

  function clearFilters() {
    setFilters({});
  }

  const hasActiveFilters = Object.values(filters).some((v) => v);

  async function exportPdf() {
    if (logs.length === 0) return;
    setExporting(true);
    try {
      const doc = new jsPDF({ orientation: "landscape", unit: "mm", format: "a4" });
      const pageWidth = doc.internal.pageSize.getWidth();
      const now = new Date().toLocaleString("fr-FR");

      // Header
      doc.setFontSize(16);
      doc.setFont("helvetica", "bold");
      doc.text("OPUS — Journal d'audit", 14, 15);
      doc.setFontSize(9);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(100);
      doc.text(`Exporté le ${now}`, 14, 21);
      doc.text(`Nombre d'entrées : ${logs.length}`, 14, 26);
      if (hasActiveFilters) {
        const activeFilters = Object.entries(filters)
          .filter(([, v]) => v)
          .map(([k, v]) => `${k}=${v}`)
          .join(", ");
        doc.text(`Filtres : ${activeFilters}`, 14, 31);
      }
      doc.setTextColor(0);

      autoTable(doc, {
        startY: 36,
        head: [["Date", "Utilisateur", "Action", "Module", "Description", "IP"]],
        body: logs.map((log) => [
          new Date(log.created_at).toLocaleString("fr-FR"),
          `${log.prenoms || ""} ${log.nom || ""}`.trim() || log.username || "—",
          ACTION_LABELS[log.action] || log.action,
          MODULE_LABELS[log.module] || log.module,
          log.description || "—",
          log.ip_address || "—",
        ]),
        styles: { fontSize: 7, cellPadding: 1.5, overflow: "linebreak" },
        headStyles: { fillColor: [108, 99, 255], textColor: 255, fontSize: 8 },
        columnStyles: {
          0: { cellWidth: 35 },
          1: { cellWidth: 35 },
          2: { cellWidth: 30 },
          3: { cellWidth: 25 },
          4: { cellWidth: "auto" },
          5: { cellWidth: 25 },
        },
        didDrawPage: (data) => {
          const str = `Page ${doc.getNumberOfPages()}`;
          doc.setFontSize(8);
          doc.setTextColor(150);
          doc.text(str, pageWidth - 20, doc.internal.pageSize.getHeight() - 8);
          doc.setTextColor(0);
        },
      });

      doc.save(`audit-logs-${new Date().toISOString().slice(0, 10)}.pdf`);
      addNotification("success", "Export réussi", `${logs.length} entrées exportées en PDF`);
    } catch {
      addNotification("error", "Erreur", "Échec de l'export PDF");
    } finally {
      setExporting(false);
    }
  }

  function exportSinglePdf(log: AuditLog) {
    try {
      const doc = new jsPDF({ orientation: "portrait", unit: "mm", format: "a4" });
      const pageWidth = doc.internal.pageSize.getWidth();
      const now = new Date().toLocaleString("fr-FR");

      // Header
      doc.setFontSize(16);
      doc.setFont("helvetica", "bold");
      doc.text("OPUS — Détail du journal d'audit", 14, 15);
      doc.setFontSize(9);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(100);
      doc.text(`Exporté le ${now}`, 14, 21);
      doc.setTextColor(0);

      // Info table
      autoTable(doc, {
        startY: 27,
        head: [["Champ", "Valeur"]],
        body: [
          ["Date", new Date(log.created_at).toLocaleString("fr-FR")],
          ["Utilisateur", `${log.prenoms || ""} ${log.nom || ""}`.trim() || log.username || "—"],
          ["Nom d'utilisateur", log.username || "—"],
          ["Action", ACTION_LABELS[log.action] || log.action],
          ["Module", MODULE_LABELS[log.module] || log.module],
          ["Entité ID", String(log.entity_id || "—")],
          ["Description", log.description || "—"],
          ["Adresse IP", log.ip_address || "—"],
          ["User-Agent", log.user_agent || "—"],
        ],
        styles: { fontSize: 9, cellPadding: 2, overflow: "linebreak" },
        headStyles: { fillColor: [108, 99, 255], textColor: 255, fontSize: 10 },
        columnStyles: { 0: { cellWidth: 50 }, 1: { cellWidth: "auto" } },
      });

      // Values comparison table (single table with old & new side by side)
      if (log.old_values || log.new_values) {
        let oldObj: Record<string, unknown> | null = null;
        let newObj: Record<string, unknown> | null = null;
        try { oldObj = log.old_values ? JSON.parse(log.old_values) : null; } catch { oldObj = null; }
        try { newObj = log.new_values ? JSON.parse(log.new_values) : null; } catch { newObj = null; }

        if (
          (oldObj && typeof oldObj === "object") ||
          (newObj && typeof newObj === "object")
        ) {
          const diffEntries = getDiffEntries(oldObj, newObj);
          autoTable(doc, {
            head: [["Champ", "Ancien", "Nouveau"]],
            body: diffEntries.map((e) => [
              e.key,
              formatValue(e.oldValue),
              formatValue(e.newValue),
            ]),
            styles: { fontSize: 8, cellPadding: 2, overflow: "linebreak", font: "courier" },
            headStyles: { fillColor: [108, 99, 255], textColor: 255, fontSize: 9 },
            columnStyles: {
              0: { cellWidth: 40, fontStyle: "bold" },
              1: { cellWidth: "auto", textColor: [220, 38, 38] },
              2: { cellWidth: "auto", textColor: [22, 163, 74] },
            },
            margin: { top: 6 },
            didParseCell: (data) => {
              if (data.section === "body" && data.column.index > 0) {
                const entry = diffEntries[data.row.index];
                if (entry && entry.status !== "unchanged") {
                  data.cell.styles.fillColor = data.column.index === 1
                    ? [254, 226, 226]
                    : [226, 254, 226];
                }
              }
            },
          });
        } else {
          autoTable(doc, {
            head: [["Anciennes valeurs", "Nouvelles valeurs"]],
            body: [[log.old_values || "—", log.new_values || "—"]],
            styles: { fontSize: 8, cellPadding: 2, overflow: "linebreak", font: "courier" },
            headStyles: { fillColor: [108, 99, 255], textColor: 255, fontSize: 9 },
            columnStyles: {
              0: { cellWidth: "auto", textColor: [220, 38, 38] },
              1: { cellWidth: "auto", textColor: [22, 163, 74] },
            },
            margin: { top: 6 },
          });
        }
      }

      // Page numbers
      const pageCount = doc.getNumberOfPages();
      for (let i = 1; i <= pageCount; i++) {
        doc.setPage(i);
        doc.setFontSize(8);
        doc.setTextColor(150);
        doc.text(`Page ${i}/${pageCount}`, pageWidth - 25, doc.internal.pageSize.getHeight() - 8);
        doc.setTextColor(0);
      }

      doc.save(`audit-log-${log.id}-${new Date().toISOString().slice(0, 10)}.pdf`);
      addNotification("success", "Export réussi", "Entrée exportée en PDF");
    } catch {
      addNotification("error", "Erreur", "Échec de l'export PDF");
    }
  }

  const columns: Column<AuditLog>[] = [
    {
      key: "created_at",
      header: "Date",
      sortable: true,
      render: (log) => (
        <span className="text-xs text-muted-foreground whitespace-nowrap">
          {new Date(log.created_at).toLocaleString("fr-FR", {
            day: "2-digit",
            month: "2-digit",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
          })}
        </span>
      ),
    },
    {
      key: "username",
      header: "Utilisateur",
      sortable: true,
      render: (log) => (
        <div className="flex flex-col">
          <span className="text-sm font-medium">
            {log.prenoms} {log.nom}
          </span>
          <span className="text-xs text-muted-foreground">@{log.username || "—"}</span>
        </div>
      ),
    },
    {
      key: "action",
      header: "Action",
      sortable: true,
      render: (log) => {
        const Icon = ACTION_ICONS[log.action] || ScrollText;
        const colorClass = ACTION_COLORS[log.action] || "bg-muted text-muted-foreground border-border";
        return (
          <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-full border ${colorClass}`}>
            <Icon className="h-3 w-3" />
            {ACTION_LABELS[log.action] || log.action}
          </span>
        );
      },
    },
    {
      key: "module",
      header: "Module",
      sortable: true,
      render: (log) => (
        <span className={`inline-flex items-center text-xs font-medium px-2 py-0.5 rounded-full ${MODULE_COLORS[log.module] || "bg-muted text-muted-foreground"}`}>
          {MODULE_LABELS[log.module] || log.module}
        </span>
      ),
    },
    {
      key: "description",
      header: "Description",
      render: (log) => (
        <span className="text-sm text-muted-foreground line-clamp-2 max-w-md">
          {log.description || "—"}
        </span>
      ),
    },
    {
      key: "ip_address",
      header: "Adresse IP",
      render: (log) => (
        <span className="text-xs text-muted-foreground font-mono">
          {log.ip_address || "—"}
        </span>
      ),
    },
  ];

  if (!isSuperAdmin) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center space-y-2">
          <ShieldCheck className="h-12 w-12 text-muted-foreground mx-auto" />
          <p className="text-lg font-semibold">Accès restreint</p>
          <p className="text-sm text-muted-foreground">
            Seuls les Super Administrateurs peuvent consulter les journaux d'audit.
          </p>
        </div>
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Journal d'audit</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Traçabilité de toutes les actions du système
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            className="gap-2"
            onClick={exportPdf}
            disabled={exporting || logs.length === 0}
          >
            <FileDown className="h-4 w-4" />
            Exporter PDF
          </Button>
          <Button
            variant={showFilters ? "default" : "outline"}
            className="gap-2"
            onClick={() => setShowFilters(!showFilters)}
          >
            <Filter className="h-4 w-4" />
            Filtres
            {hasActiveFilters && (
              <span className="ml-1 inline-flex items-center justify-center min-w-[16px] h-[16px] px-1 text-[10px] font-semibold rounded-full bg-primary-foreground text-primary" />
            )}
          </Button>
        </div>
      </div>

      {showFilters && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          exit={{ opacity: 0, height: 0 }}
        >
          <Card>
            <CardContent className="pt-4">
              <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-3">
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted-foreground">Action</label>
                  <Select
                    value={filters.action || ""}
                    onChange={(e) => applyFilters({ ...filters, action: e.target.value || undefined })}
                    placeholder="Toutes les actions"
                    options={[
                      { value: "", label: "Toutes les actions" },
                      ...Object.entries(ACTION_LABELS).map(([value, label]) => ({ value, label })),
                    ]}
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted-foreground">Module</label>
                  <Select
                    value={filters.module || ""}
                    onChange={(e) => applyFilters({ ...filters, module: e.target.value || undefined })}
                    placeholder="Tous les modules"
                    options={[
                      { value: "", label: "Tous les modules" },
                      ...Object.entries(MODULE_LABELS).map(([value, label]) => ({ value, label })),
                    ]}
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted-foreground">Date début</label>
                  <Input
                    type="date"
                    value={filters.date_from || ""}
                    onChange={(e) => applyFilters({ ...filters, date_from: e.target.value || undefined })}
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-muted-foreground">Date fin</label>
                  <Input
                    type="date"
                    value={filters.date_to || ""}
                    onChange={(e) => applyFilters({ ...filters, date_to: e.target.value || undefined })}
                  />
                </div>
                <div className="space-y-1.5 lg:col-span-2">
                  <label className="text-xs font-medium text-muted-foreground">Recherche</label>
                  <Input
                    placeholder="Rechercher dans les descriptions..."
                    value={filters.search || ""}
                    onChange={(e) => applyFilters({ ...filters, search: e.target.value || undefined })}
                  />
                </div>
                <div className="flex items-end gap-2">
                  <Button variant="outline" className="gap-2" onClick={clearFilters}>
                    <X className="h-4 w-4" />
                    Effacer
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>
      )}

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <ScrollText className="h-4 w-4" />
            Journaux d'audit ({logs.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={logs}
            keyExtractor={(log) => log.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher un log..."
            onRowClick={(log) => setSelectedLog(log)}
            pageSize={20}
            emptyMessage="Aucun journal d'audit trouvé"
          />
        </CardContent>
      </Card>

      {selectedLog && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          onClick={() => setSelectedLog(null)}
        >
          <div className="fixed inset-0 bg-black/40" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="relative z-50 bg-card border border-border rounded-xl shadow-xl w-full max-w-2xl max-h-[85vh] flex flex-col mx-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between p-4 border-b border-border shrink-0">
              <div className="flex items-center gap-2">
                <ScrollText className="h-5 w-5 text-primary" />
                <h3 className="text-lg font-semibold">Détail du journal</h3>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  className="gap-2"
                  onClick={() => exportSinglePdf(selectedLog)}
                >
                  <FileDown className="h-4 w-4" />
                  Exporter PDF
                </Button>
                <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setSelectedLog(null)}>
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
            <div className="overflow-y-auto flex-1 p-4 space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Date</p>
                    <p className="text-sm mt-1">
                      {new Date(selectedLog.created_at).toLocaleString("fr-FR")}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Utilisateur</p>
                    <p className="text-sm mt-1">
                      {selectedLog.prenoms} {selectedLog.nom}{" "}
                      <span className="text-muted-foreground">@{selectedLog.username}</span>
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Action</p>
                    <div className="mt-1">
                      <Badge variant="outline" className={ACTION_COLORS[selectedLog.action] || ""}>
                        {ACTION_LABELS[selectedLog.action] || selectedLog.action}
                      </Badge>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Module</p>
                    <div className="mt-1">
                      <Badge variant="outline" className={MODULE_COLORS[selectedLog.module] || ""}>
                        {MODULE_LABELS[selectedLog.module] || selectedLog.module}
                      </Badge>
                    </div>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Entité ID</p>
                    <p className="text-sm mt-1 font-mono">{selectedLog.entity_id || "—"}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Adresse IP</p>
                    <p className="text-sm mt-1 font-mono">{selectedLog.ip_address || "—"}</p>
                  </div>
                </div>

                <div>
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Description</p>
                  <p className="text-sm mt-1">{selectedLog.description || "—"}</p>
                </div>

                <div>
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">User-Agent</p>
                  <p className="text-xs mt-1 text-muted-foreground break-all">{selectedLog.user_agent || "—"}</p>
                </div>

                {(selectedLog.old_values || selectedLog.new_values) && (
                  <JsonDiffView
                    oldValues={selectedLog.old_values}
                    newValues={selectedLog.new_values}
                  />
                )}
              </div>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
