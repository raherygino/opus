import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getRequisitionList,
  deleteRequisition,
  REQUISITION_TYPES,
  REQUISITION_TYPE_LABELS,
} from "@/lib/api/requisition";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, FileText } from "lucide-react";
import type { Requisition } from "@/types";

const PJ_REQUISITION_MODULE = "pj_requisition";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

export function RequisitionList() {
  const [items, setItems] = useState<Requisition[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Requisition | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_REQUISITION_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_REQUISITION_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getRequisitionList().catch(() => []);
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les réquisitions");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTarget) {
        await deleteRequisition(deleteTarget.id);
        addNotification("success", "Supprimée", "Réquisition supprimée avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const filtered = items.filter((r) => {
    if (typeFilter && r.type !== typeFilter) return false;
    return true;
  });

  const columns: Column<Requisition>[] = [
    {
      key: "type",
      header: "Type",
      sortable: true,
      render: (r) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
          {REQUISITION_TYPE_LABELS[r.type] ?? r.type}
        </span>
      ),
    },
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-xs font-semibold">{r.numero}</span>,
    },
    {
      key: "date_requisition",
      header: "Date",
      sortable: true,
      render: (r) => formatDate(r.date_requisition),
    },
    {
      key: "affaire",
      header: "Affaire",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.affaire}</span>,
    },
    {
      key: "nom_substitut",
      header: "Substitut",
      render: (r) => r.nom_substitut ?? "—",
    },
    {
      key: "opj",
      header: "OPJ",
      render: (r) => r.opj ?? "—",
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
            onClick={() => navigate(`/pj/requisition/${r.id}/edit`)}
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
            <FileText className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Réquisition</h1>
            <p className="text-sm text-muted-foreground">
              Gestion des réquisitions (Police Judiciaire)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/requisition/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle réquisition
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des réquisitions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-3 mb-4">
            <Select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              options={[
                { value: "", label: "Tous les types" },
                ...REQUISITION_TYPES.map((t) => ({
                  value: t,
                  label: REQUISITION_TYPE_LABELS[t],
                })),
              ]}
              className="w-56"
            />
          </div>

          <DataTable
            data={filtered}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/requisition/${r.id}`)}
            emptyMessage="Aucune réquisition à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la réquisition"
        message={`Voulez-vous vraiment supprimer la réquisition « ${deleteTarget?.numero} » ? Toutes les pièces jointes seront également supprimées.`}
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
