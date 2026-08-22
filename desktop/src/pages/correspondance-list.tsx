import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getCorrespondanceList,
  deleteCorrespondance,
} from "@/lib/api/correspondance";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Repeat, Pencil, Trash2, ArrowDownLeft, ArrowUpRight } from "lucide-react";
import type { Correspondance } from "@/types";

export const CORRESPONDANCE_MODULE = "sedentaire_secretariat_correspondance";

const STATUT_COLORS: Record<string, string> = {
  "Enregistré": "bg-blue-500/10 text-blue-500",
  "En traitement": "bg-amber-500/10 text-amber-500",
  "Traité": "bg-green-500/10 text-green-500",
  "Archivé": "bg-muted text-muted-foreground",
};

export function formatHeure(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : "—";
}

export function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

export function CorrespondanceList() {
  const [correspondances, setCorrespondances] = useState<Correspondance[]>([]);
  const [loading, setLoading] = useState(true);
  const [sensFilter, setSensFilter] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Correspondance | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, CORRESPONDANCE_MODULE, "can_create");
  const canDelete = hasPermission(user, CORRESPONDANCE_MODULE, "can_delete");

  useEffect(() => {
    loadCorrespondances();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sensFilter]);

  async function loadCorrespondances() {
    setLoading(true);
    try {
      const filters: Record<string, string> = {};
      if (sensFilter) filters.sens = sensFilter;
      const data = await getCorrespondanceList(filters);
      setCorrespondances(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les correspondances");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteCorrespondance(id);
      addNotification("success", "Supprimée", "Correspondance supprimée avec succès");
      loadCorrespondances();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cette correspondance");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Correspondance>[] = [
    {
      key: "sens",
      header: "Sens",
      sortable: true,
      render: (c) => (
        <span className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full ${
          c.sens === "Entrant"
            ? "bg-green-500/10 text-green-500"
            : "bg-blue-500/10 text-blue-500"
        }`}>
          {c.sens === "Entrant" ? (
            <ArrowDownLeft className="h-3 w-3" />
          ) : (
            <ArrowUpRight className="h-3 w-3" />
          )}
          {c.sens}
        </span>
      ),
    },
    { key: "reference", header: "N° d'ordre / Référence", sortable: true },
    {
      key: "date_correspondance",
      header: "Date",
      sortable: true,
      render: (c) => `${formatDate(c.date_correspondance)} ${formatHeure(c.heure_enregistrement)}`,
    },
    { key: "emetteur_destinataire", header: "Émetteur / Destinataire", sortable: true },
    { key: "objet", header: "Objet", sortable: true },
    {
      key: "statut",
      header: "Statut",
      sortable: true,
      render: (c) => (
        <span className={`text-xs px-2 py-0.5 rounded-full ${STATUT_COLORS[c.statut] ?? "bg-muted text-muted-foreground"}`}>
          {c.statut}
        </span>
      ),
    },
    {
      key: "agent",
      header: "Agent secrétariat",
      render: (c) =>
        [c.agent_prenoms, c.agent_nom].filter(Boolean).join(" ") ||
        c.agent_username ||
        "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (c) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sedentaire/secretariat/correspondance/${c.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(c)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Correspondance</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Registre des courriers entrants et sortants
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={() => navigate("/sedentaire/secretariat/correspondance/new")}
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            Nouvelle correspondance
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <Repeat className="h-4 w-4" />
              Correspondances ({correspondances.length})
            </CardTitle>
            <div className="flex items-center gap-2">
              <Select
                value={sensFilter}
                onChange={(e) => setSensFilter(e.target.value)}
                options={[
                  { value: "", label: "Tous les sens" },
                  { value: "Entrant", label: "Entrant" },
                  { value: "Sortant", label: "Sortant" },
                ]}
                className="w-40"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={correspondances}
            keyExtractor={(c) => c.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par référence, objet, émetteur..."
            emptyMessage="Aucune correspondance"
            onRowClick={(c) => navigate(`/sedentaire/secretariat/correspondance/${c.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la correspondance"
        message={`Êtes-vous sûr de vouloir supprimer la correspondance ${deleteTarget?.reference} (${deleteTarget?.sens}) ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
