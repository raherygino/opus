import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getArrestationList, deleteArrestation } from "@/lib/api/arrestation";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, ShieldBan } from "lucide-react";
import type { Arrestation } from "@/types";

const PJ_ARRESTATION_MODULE = "pj_arrestation";

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const d = new Date(date.replace(" ", "T"));
  if (isNaN(d.getTime())) return date;
  return d.toLocaleDateString("fr-FR") + " " + d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

export function ArrestationList() {
  const [items, setItems] = useState<Arrestation[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Arrestation | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_ARRESTATION_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_ARRESTATION_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getArrestationList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les arrestations");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteArrestation(deleteTarget.id);
      addNotification("success", "Supprimé", "Arrestation supprimée avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'arrestation");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Arrestation>[] = [
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero}</span>,
    },
    {
      key: "date_heure_arrestation",
      header: "Date et heure",
      sortable: true,
      render: (r) => <span className="text-sm">{formatDateTime(r.date_heure_arrestation)}</span>,
    },
    {
      key: "personne_nom",
      header: "Personne arrêtée",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.personne_nom}</span>,
    },
    {
      key: "lieu_arrestation",
      header: "Lieu",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.lieu_arrestation ?? "—"}</span>,
    },
    {
      key: "numero_dossier",
      header: "N° dossier",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.numero_dossier ?? "—"}</span>,
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
            onClick={() => navigate(`/pj/arrestation/${r.id}/edit`)}
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
            <ShieldBan className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Arrestations</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Registre des arrestations
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/arrestation/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle arrestation
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des arrestations</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/arrestation/${r.id}`)}
            emptyMessage="Aucune arrestation à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'arrestation"
        message="Voulez-vous vraiment supprimer cette arrestation ? Toutes les pièces jointes seront également supprimées."
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
