import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRegistreDeferrementList, deleteRegistreDeferrement } from "@/lib/api/registre-deferrement";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, Gavel } from "lucide-react";
import type { RegistreDeferrement } from "@/types";

const PJ_DEFERREMENT_MODULE = "pj_deferrement";

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const d = new Date(date.replace(" ", "T"));
  if (isNaN(d.getTime())) return date;
  return d.toLocaleDateString("fr-FR") + " " + d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

export function RegistreDeferrementList() {
  const [items, setItems] = useState<RegistreDeferrement[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<RegistreDeferrement | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_DEFERREMENT_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_DEFERREMENT_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getRegistreDeferrementList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger le registre de déferrement");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteRegistreDeferrement(deleteTarget.id);
      addNotification("success", "Supprimé", "Entrée supprimée avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'entrée");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<RegistreDeferrement>[] = [
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero}</span>,
    },
    {
      key: "date_heure_deferrement",
      header: "Date et heure",
      sortable: true,
      render: (r) => <span className="text-sm">{formatDateTime(r.date_heure_deferrement)}</span>,
    },
    {
      key: "personne_nom",
      header: "Personne déférée",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.personne_nom}</span>,
    },
    {
      key: "infraction",
      header: "Infraction",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.infraction ?? "—"}</span>,
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
            onClick={() => navigate(`/pj/registre-deferrement/${r.id}/edit`)}
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
            <Gavel className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Registre de déferrement</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Registre des déferrements
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/registre-deferrement/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle entrée
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des déferrements</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/registre-deferrement/${r.id}`)}
            emptyMessage="Aucune entrée à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'entrée"
        message="Voulez-vous vraiment supprimer cette entrée du registre de déferrement ? Toutes les pièces jointes seront également supprimées."
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
