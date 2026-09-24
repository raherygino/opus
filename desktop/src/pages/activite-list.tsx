import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getActiviteList,
  deleteActivite,
  PATROUILLE_TYPES,
  PATROUILLE_MODES,
  patrouilleItineraireField,
} from "@/lib/api/activite";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, File as FileIcon } from "lucide-react";
import type { Activite } from "@/types";

const SG_ACTIVITE_MODULE = "sg_activite";

/** Short patrol summary, e.g. "Diurne motorisée • Nocturne portée". */
function patrouilleSummary(item: Activite): string {
  return PATROUILLE_TYPES.flatMap((type) =>
    PATROUILLE_MODES.filter(
      (mode) => item[patrouilleItineraireField(type.key, mode.key)] !== null,
    ).map((mode) => `${type.label.replace("Patrouille ", "")} ${mode.label}`),
  ).join(" • ");
}

export function ActiviteList() {
  const [items, setItems] = useState<Activite[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Activite | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, SG_ACTIVITE_MODULE, "can_create");
  const canDelete = hasPermission(user, SG_ACTIVITE_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getActiviteList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les activités");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteActivite(deleteTarget.id);
      addNotification("success", "Supprimée", "Activité supprimée avec succès");
      setDeleteTarget(null);
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'activité");
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<Activite>[] = [
    {
      key: "date_activite",
      header: "Date",
      render: (item) => new Date(item.date_activite).toLocaleDateString("fr-FR"),
    },
    { key: "heure_activite", header: "Heure" },
    {
      key: "patrouille",
      header: "Patrouille",
      render: (item) => patrouilleSummary(item) || "—",
    },
    {
      key: "operation_ciblee",
      header: "Opération ciblée",
      render: (item) => item.operation_ciblee || "—",
    },
    {
      key: "nature_intervention",
      header: "Intervention",
      render: (item) => item.nature_intervention || "—",
    },
    {
      key: "agent",
      header: "Agent",
      render: (item) =>
        [item.agent_prenoms, item.agent_nom].filter(Boolean).join(" ") ||
        item.agent_username ||
        "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[130px]",
      render: (item) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/activites/${item.id}`)}
            title="Consulter"
          >
            <FileIcon className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/activites/${item.id}/edit`)}
            title="Modifier"
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => setDeleteTarget(item)}
              title="Supprimer"
            >
              <Trash2 className="h-3.5 w-3.5 text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Activité</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Patrouilles et interventions
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/sg/activites/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle activité
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Liste des activités</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/sg/activites/${r.id}`)}
            emptyMessage="Aucune activité enregistrée"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        onCancel={() => !deleting && setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Supprimer l'activité"
        message={`Voulez-vous vraiment supprimer l'activité du ${deleteTarget ? new Date(deleteTarget.date_activite).toLocaleDateString("fr-FR") : ""} ?`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}
