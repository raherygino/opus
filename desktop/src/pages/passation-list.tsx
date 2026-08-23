import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPassationList,
  deletePassation,
} from "@/lib/api/passation";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Handshake, Pencil, Trash2 } from "lucide-react";
import type { Passation } from "@/types";

export const PASSATION_MODULE = "sedentaire_poste_passation";

export function formatHeure(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : "—";
}

export function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

const LIST_PATH = "/sedentaire/poste/passation";

export function PassationList() {
  const [passations, setPassations] = useState<Passation[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Passation | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PASSATION_MODULE, "can_create");
  const canDelete = hasPermission(user, PASSATION_MODULE, "can_delete");

  useEffect(() => {
    loadPassations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadPassations() {
    setLoading(true);
    try {
      const data = await getPassationList();
      setPassations(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les passations");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deletePassation(id);
      addNotification("success", "Supprimée", "Passation supprimée avec succès");
      loadPassations();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cette passation");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Passation>[] = [
    {
      key: "date_passation",
      header: "Date",
      sortable: true,
      render: (p) => formatDate(p.date_passation),
    },
    {
      key: "heure_passation",
      header: "Heure",
      sortable: true,
      render: (p) => formatHeure(p.heure_passation),
    },
    {
      key: "chef_descendant",
      header: "Chef descendant",
      sortable: true,
      render: (p) =>
        [p.chef_descendant_grade, p.chef_descendant_lastname]
          .filter(Boolean)
          .join(" ") || "—",
    },
    {
      key: "chef_montant",
      header: "Chef montant",
      sortable: true,
      render: (p) =>
        [p.chef_montant_grade, p.chef_montant_lastname]
          .filter(Boolean)
          .join(" ") || "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (p) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`${LIST_PATH}/${p.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(p)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Passations</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Registre des passations de poste entre chefs de poste
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={() => navigate(`${LIST_PATH}/new`)}
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            Nouvelle passation
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <Handshake className="h-4 w-4" />
              Passations ({passations.length})
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={passations}
            keyExtractor={(p) => p.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par chef, instructions, incidents..."
            emptyMessage="Aucune passation"
            onRowClick={(p) => navigate(`${LIST_PATH}/${p.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la passation"
        message={`Êtes-vous sûr de vouloir supprimer la passation du ${deleteTarget ? formatDate(deleteTarget.date_passation) : ""} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
