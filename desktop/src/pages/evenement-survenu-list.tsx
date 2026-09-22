import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getEvenementSurvenuList,
  deleteEvenementSurvenu,
  EVENEMENT_TYPE_LABELS,
} from "@/lib/api/evenement-survenu";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, File as FileIcon } from "lucide-react";
import type { EvenementSurvenu } from "@/types";

const SG_EVENEMENT_MODULE = "sg_evenement_survenu";

export function EvenementSurvenuList() {
  const [items, setItems] = useState<EvenementSurvenu[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<EvenementSurvenu | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, SG_EVENEMENT_MODULE, "can_create");
  const canDelete = hasPermission(user, SG_EVENEMENT_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getEvenementSurvenuList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les évènements");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteEvenementSurvenu(deleteTarget.id);
      addNotification("success", "Supprimé", "Évènement supprimé avec succès");
      setDeleteTarget(null);
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'évènement");
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<EvenementSurvenu>[] = [
    {
      key: "date_evenement",
      header: "Date",
      render: (item) => new Date(item.date_evenement).toLocaleDateString("fr-FR"),
    },
    { key: "heure_evenement", header: "Heure" },
    {
      key: "type_evenement",
      header: "Type d'événement",
      render: (item) => EVENEMENT_TYPE_LABELS[item.type_evenement] ?? item.type_evenement,
    },
    { key: "lieu_exact", header: "Lieu exact" },
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
            onClick={() => navigate(`/sg/evenements-survenus/${item.id}`)}
            title="Consulter"
          >
            <FileIcon className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/evenements-survenus/${item.id}/edit`)}
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
          <h1 className="text-2xl font-bold tracking-tight">Évènements survenus sur la voie publique</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion des évènements survenus sur la voie publique
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/sg/evenements-survenus/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvel évènement
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Liste des évènements</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/sg/evenements-survenus/${r.id}`)}
            emptyMessage="Aucun évènement enregistré"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        onCancel={() => !deleting && setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Supprimer l'évènement"
        message={`Voulez-vous vraiment supprimer l'évènement du ${deleteTarget ? new Date(deleteTarget.date_evenement).toLocaleDateString("fr-FR") : ""} ?`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}
