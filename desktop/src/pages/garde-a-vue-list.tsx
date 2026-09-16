import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getGardeAVueList,
  deleteGardeAVue,
} from "@/lib/api/garde-a-vue";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, ShieldAlert } from "lucide-react";
import type { GardeAVue } from "@/types";

const PJ_GAV_MODULE = "pj_gav";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const datePart = date.slice(0, 10);
  const timePart = date.substring(11, 16);
  const [y, m, d] = datePart.split("-");
  const dateDisplay = y && m && d ? `${d}/${m}/${y}` : datePart;
  return `${dateDisplay} ${timePart}`.trim();
}

export function GardeAVueList() {
  const [items, setItems] = useState<GardeAVue[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<GardeAVue | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_GAV_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_GAV_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getGardeAVueList().catch(() => []);
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les gardes à vue");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTarget) {
        await deleteGardeAVue(deleteTarget.id);
        addNotification("success", "Supprimée", "Garde à vue supprimée avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<GardeAVue>[] = [
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (g) => (
        <div>
          <span className="font-medium">{g.nom}</span>
          {g.prenoms && (
            <span className="text-muted-foreground"> {g.prenoms}</span>
          )}
        </div>
      ),
    },
    {
      key: "date_naissance",
      header: "Naissance",
      sortable: true,
      render: (g) => formatDate(g.date_naissance),
    },
    {
      key: "motif",
      header: "Motif",
      render: (g) => <span className="line-clamp-1 max-w-xs">{g.motif ?? "—"}</span>,
    },
    {
      key: "opj_gav",
      header: "OPJ",
      render: (g) => g.opj_gav ?? "—",
    },
    {
      key: "debut_gav",
      header: "Début GAV",
      sortable: true,
      render: (g) => formatDateTime(g.debut_gav),
    },
    {
      key: "fin_gav",
      header: "Fin GAV",
      sortable: true,
      render: (g) => formatDateTime(g.fin_gav),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (g) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/gav/${g.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(g)}
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
            <ShieldAlert className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Garde à vue</h1>
            <p className="text-sm text-muted-foreground">
              Gestion des gardes à vue (Police Judiciaire)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/gav/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle garde à vue
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des gardes à vue</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(g) => g.id}
            onRowClick={(g) => navigate(`/pj/gav/${g.id}`)}
            emptyMessage="Aucune garde à vue à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la garde à vue"
        message={`Voulez-vous vraiment supprimer la garde à vue de « ${deleteTarget?.nom} » ? Toutes les pièces jointes seront également supprimées.`}
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
