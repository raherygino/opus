import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getMandatList, deleteMandat } from "@/lib/api/mandat";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, File as FileIcon } from "lucide-react";
import { getMandatTypeLabel } from "@/types";
import type { Mandat } from "@/types";

const PJ_MANDAT_MODULE = "pj_mandat";

export function MandatList() {
  const [items, setItems] = useState<Mandat[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Mandat | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_MANDAT_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_MANDAT_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getMandatList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les mandats");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteMandat(deleteTarget.id);
      addNotification("success", "Supprimé", "Mandat supprimé avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le mandat");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Mandat>[] = [
    {
      key: "type",
      header: "Type",
      sortable: true,
      render: (r) => <span className="text-primary text-sm font-medium">{getMandatTypeLabel(r.type)}</span>,
    },
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero}</span>,
    },
    {
      key: "personne_nom",
      header: "Personne concernée",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.personne_nom}</span>,
    },
    {
      key: "autorite",
      header: "Autorité",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.autorite ?? "—"}</span>,
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
            onClick={() => navigate(`/pj/mandat/${r.id}/edit`)}
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
            <FileIcon className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Mandats</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Mandats judiciaires
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/mandat/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau mandat
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des mandats</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/mandat/${r.id}`)}
            emptyMessage="Aucun mandat à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le mandat"
        message="Voulez-vous vraiment supprimer ce mandat ? Toutes les pièces jointes seront également supprimées."
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
