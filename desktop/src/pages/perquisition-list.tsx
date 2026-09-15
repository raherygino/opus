import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getPerquisitionList, deletePerquisition } from "@/lib/api/perquisition";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, Search } from "lucide-react";
import type { Perquisition } from "@/types";

const PJ_PERQUISITION_MODULE = "pj_perquisition";

export function PerquisitionList() {
  const [items, setItems] = useState<Perquisition[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Perquisition | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_PERQUISITION_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_PERQUISITION_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getPerquisitionList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les perquisitions");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deletePerquisition(deleteTarget.id);
      addNotification("success", "Supprimé", "Perquisition supprimée avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer la perquisition");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Perquisition>[] = [
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero}</span>,
    },
    {
      key: "affaire",
      header: "Affaire",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.affaire}</span>,
    },
    {
      key: "substitut",
      header: "Substitut",
      render: (r) => r.substitut ?? "—",
    },
    {
      key: "numero_ttr",
      header: "N° TTR",
      render: (r) => <span className="font-mono text-sm">{r.numero_ttr ?? "—"}</span>,
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
            onClick={() => navigate(`/pj/perquisition/${r.id}/edit`)}
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
            <Search className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Perquisitions</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Perquisitions
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/perquisition/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle perquisition
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des perquisitions</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/perquisition/${r.id}`)}
            emptyMessage="Aucune perquisition à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la perquisition"
        message="Voulez-vous vraiment supprimer cette perquisition ? Toutes les pièces jointes seront également supprimées."
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
