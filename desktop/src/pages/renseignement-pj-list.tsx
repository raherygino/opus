import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRenseignementPjList, deleteRenseignementPj } from "@/lib/api/renseignement-pj";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, MessageCircleMore } from "lucide-react";
import type { RenseignementPj } from "@/types";

const PJ_RENSEIGNEMENT_MODULE = "pj_renseignement";

export function RenseignementPjList() {
  const [items, setItems] = useState<RenseignementPj[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<RenseignementPj | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_RENSEIGNEMENT_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getRenseignementPjList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les renseignements");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteRenseignementPj(deleteTarget.id);
      addNotification("success", "Supprimé", "Renseignement supprimé avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le renseignement");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<RenseignementPj>[] = [
    {
      key: "nature_infraction",
      header: "Nature d'infraction",
      sortable: true,
      render: (r) => <span className="font-medium line-clamp-1 max-w-xs">{r.nature_infraction}</span>,
    },
    {
      key: "date_lieu_faits",
      header: "Date et lieu des faits",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.date_lieu_faits ?? "—"}</span>,
    },
    {
      key: "circonstances",
      header: "Circonstances",
      render: (r) => <span className="line-clamp-1 max-w-xs text-muted-foreground">{r.circonstances ?? "—"}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (r) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/renseignement/${r.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(r)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-lg bg-primary/10">
            <MessageCircleMore className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Renseignements</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Renseignements judiciaires
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/renseignement/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau renseignement
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des renseignements</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/renseignement/${r.id}`)}
            emptyMessage="Aucun renseignement à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le renseignement"
        message="Voulez-vous vraiment supprimer ce renseignement ? Toutes les pièces jointes seront également supprimées."
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        variant="destructive"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
}
