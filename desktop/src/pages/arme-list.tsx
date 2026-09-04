import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getArmeList, deleteArme } from "@/lib/api/arme";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Crosshair, Pencil, Trash2, Eye } from "lucide-react";
import type { Arme } from "@/types";

export const ARME_MODULE = "sedentaire_poste_arme";

const LIST_PATH = "/sedentaire/poste/armes";

export function ArmeList() {
  const [armes, setArmes] = useState<Arme[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Arme | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, ARME_MODULE, "can_create");
  const canEdit = hasPermission(user, ARME_MODULE, "can_edit");
  const canDelete = hasPermission(user, ARME_MODULE, "can_delete");

  useEffect(() => {
    loadArmes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadArmes() {
    setLoading(true);
    try {
      const data = await getArmeList();
      setArmes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les armes");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteArme(id);
      addNotification("success", "Supprimée", "Arme supprimée avec succès");
      loadArmes();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer cette arme";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Arme>[] = [
    {
      key: "type_arme_nom",
      header: "Type d'Arme",
      sortable: true,
      render: (a) => a.type_arme_nom || "—",
    },
    {
      key: "matricule",
      header: "Matricule",
      sortable: true,
      render: (a) => a.matricule,
    },
    {
      key: "munitions_stock",
      header: "Munitions (type)",
      sortable: true,
      render: (a) => (
        <Badge variant={a.type_arme_munitions_stock > 0 ? "default" : "secondary"}>
          {a.type_arme_munitions_stock}
        </Badge>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[120px]",
      render: (a) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            title="Voir le détail"
            onClick={() => navigate(`${LIST_PATH}/${a.id}`)}
          >
            <Eye className="h-3.5 w-3.5" />
          </Button>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => navigate(`${LIST_PATH}/${a.id}/edit`)}
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
          )}
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              title="Supprimer"
              onClick={() => setDeleteTarget(a)}
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
          <h1 className="text-2xl font-semibold tracking-tight">Armes</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Catalogue des armes et stock de munitions
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate(`${LIST_PATH}/new`)} className="gap-2">
            <Plus className="h-4 w-4" />
            Nouvelle arme
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Crosshair className="h-4 w-4" />
            Armes ({armes.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={armes}
            keyExtractor={(a) => a.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par type, matricule..."
            emptyMessage="Aucune arme enregistrée"
            onRowClick={(a) => navigate(`${LIST_PATH}/${a.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'arme"
        message={`Êtes-vous sûr de vouloir supprimer l'arme ${deleteTarget?.matricule} (${deleteTarget?.type_arme_nom}) ? Cette action est impossible si des perceptions ou consommations y sont liées.`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
