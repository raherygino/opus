import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getConvocationList,
  deleteConvocation,
  CONVOCATION_TYPES,
  CONVOCATION_TYPE_LABELS,
} from "@/lib/api/convocation";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Mail, Pencil, Trash2 } from "lucide-react";
import type { Convocation } from "@/types";

const PJ_CONVOCATION_MODULE = "pj_convocation";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

export function ConvocationList() {
  const [items, setItems] = useState<Convocation[]>([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<Convocation | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_CONVOCATION_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_CONVOCATION_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getConvocationList().catch(() => []);
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les convocations");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTarget) {
        await deleteConvocation(deleteTarget.id);
        addNotification("success", "Supprimée", "Convocation supprimée avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const filtered = items.filter((c) => {
    if (typeFilter && c.type !== typeFilter) return false;
    return true;
  });

  const columns: Column<Convocation>[] = [
    {
      key: "type",
      header: "Type",
      sortable: true,
      render: (c) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
          {CONVOCATION_TYPE_LABELS[c.type] ?? c.type}
        </span>
      ),
    },
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (c) => <span className="font-mono text-xs font-semibold">{c.numero}</span>,
    },
    {
      key: "date_convocation",
      header: "Date",
      sortable: true,
      render: (c) => formatDate(c.date_convocation),
    },
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (c) => c.nom,
    },
    {
      key: "infraction",
      header: "Infraction",
      render: (c) => <span className="line-clamp-1 max-w-xs">{c.infraction ?? "—"}</span>,
    },
    {
      key: "numero_dossier",
      header: "N° Dossier",
      render: (c) => c.numero_dossier ?? "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (c) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/pj/convocation/${c.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(c)}
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
            <Mail className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Convocation</h1>
            <p className="text-sm text-muted-foreground">
              Gestion des convocations (Police Judiciaire)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/convocation/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle convocation
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des convocations</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-3 mb-4">
            <Select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              options={[
                { value: "", label: "Tous les types" },
                ...CONVOCATION_TYPES.map((t) => ({
                  value: t,
                  label: CONVOCATION_TYPE_LABELS[t],
                })),
              ]}
              className="w-56"
            />
          </div>

          <DataTable
            data={filtered}
            columns={columns}
            loading={loading}
            keyExtractor={(c) => c.id}
            onRowClick={(c) => navigate(`/pj/convocation/${c.id}`)}
            emptyMessage="Aucune convocation à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la convocation"
        message={`Voulez-vous vraiment supprimer la convocation « ${deleteTarget?.numero} » ? Toutes les pièces jointes seront également supprimées.`}
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
