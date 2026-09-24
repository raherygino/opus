import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getDispositifExceptionnelList,
  deleteDispositifExceptionnel,
} from "@/lib/api/dispositif-exceptionnel";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, File as FileIcon } from "lucide-react";
import type { DispositifExceptionnel } from "@/types";

const SG_DISPOSITIF_MODULE = "sg_dispositif_exceptionnel";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("fr-FR");
}

export function DispositifExceptionnelList() {
  const [items, setItems] = useState<DispositifExceptionnel[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<DispositifExceptionnel | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, SG_DISPOSITIF_MODULE, "can_create");
  const canDelete = hasPermission(user, SG_DISPOSITIF_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getDispositifExceptionnelList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les dispositifs exceptionnels");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteDispositifExceptionnel(deleteTarget.id);
      addNotification("success", "Supprimé", "Dispositif exceptionnel supprimé avec succès");
      setDeleteTarget(null);
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le dispositif exceptionnel");
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<DispositifExceptionnel>[] = [
    {
      key: "nature_evenement",
      header: "Nature de l'évènement",
      render: (item) => item.nature_evenement || "—",
    },
    {
      key: "date_debut",
      header: "Période",
      render: (item) =>
        `Du ${formatDate(item.date_debut)} au ${formatDate(item.date_fin)}`,
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
            onClick={() => navigate(`/sg/dispositifs-exceptionnels/${item.id}`)}
            title="Consulter"
          >
            <FileIcon className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/dispositifs-exceptionnels/${item.id}/edit`)}
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
          <h1 className="text-2xl font-bold tracking-tight">Dispositif exceptionnel</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Opérations de sécurité exceptionnelles
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/sg/dispositifs-exceptionnels/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau dispositif
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Liste des dispositifs exceptionnels</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/sg/dispositifs-exceptionnels/${r.id}`)}
            emptyMessage="Aucun dispositif exceptionnel enregistré"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        onCancel={() => !deleting && setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Supprimer le dispositif exceptionnel"
        message={`Voulez-vous vraiment supprimer le dispositif « ${deleteTarget?.nature_evenement ?? ""} » ?`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}
