import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMaterielRoulantList,
  deleteMaterielRoulant,
} from "@/lib/api/materiel-roulant";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, Eye, Car } from "lucide-react";
import type { MaterielRoulant } from "@/types";
import { formatDate, formatHeure } from "@/pages/passation-list";

export const MATERIEL_ROULANT_MODULE = "sedentaire_poste_materiel_roulant";
const LIST_PATH = "/sedentaire/poste/materiel-roulant";

export function MaterielRoulantManagement() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, MATERIEL_ROULANT_MODULE, "can_create");
  const canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, "can_edit");
  const canDelete = hasPermission(user, MATERIEL_ROULANT_MODULE, "can_delete");

  const [items, setItems] = useState<MaterielRoulant[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<MaterielRoulant | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    loadItems();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadItems() {
    setLoading(true);
    try {
      const data = await getMaterielRoulantList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les matériels roulants");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteMaterielRoulant(id);
      addNotification("success", "Supprimé", "Matériel roulant supprimé avec succès");
      loadItems();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer ce matériel roulant";
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

  const columns: Column<MaterielRoulant>[] = [
    {
      key: "date_perception",
      header: "Perception",
      sortable: true,
      render: (m) =>
        `${formatDate(m.date_perception)} ${formatHeure(m.heure_perception)}`.trim(),
    },
    {
      key: "type_materiel",
      header: "Type",
      sortable: true,
      render: (m) => (
        <Badge variant="secondary">{m.type_materiel}</Badge>
      ),
    },
    {
      key: "numero_immatriculation",
      header: "Immatriculation",
      sortable: true,
      render: (m) => m.numero_immatriculation || "—",
    },
    {
      key: "description_vehicule",
      header: "Description",
      sortable: false,
      render: (m) => m.description_vehicule || "—",
    },
    {
      key: "agent_conducteur_nom",
      header: "Conducteur",
      sortable: true,
      render: (m) =>
        [m.agent_conducteur_im, [m.agent_conducteur_grade, m.agent_conducteur_nom].filter(Boolean).join(" ")]
          .filter(Boolean)
          .join(" — ") || "—",
    },
    {
      key: "chef_de_bord_nom",
      header: "Chef de bord",
      sortable: false,
      render: (m) =>
        [m.chef_de_bord_im, [m.chef_de_bord_grade, m.chef_de_bord_nom].filter(Boolean).join(" ")]
          .filter(Boolean)
          .join(" — ") || "—",
    },
    {
      key: "date_reintegration",
      header: "Réintégration",
      sortable: true,
      render: (m) =>
        m.heure_reintegration
          ? `${formatDate(m.date_reintegration)} ${formatHeure(m.heure_reintegration)}`.trim()
          : "—",
    },
    {
      key: "statut",
      header: "Statut",
      sortable: true,
      render: (m) =>
        m.statut === "Réintégré" ? (
          <Badge variant="secondary">Réintégré</Badge>
        ) : (
          <Badge>En service</Badge>
        ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[120px]",
      render: (m) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            title="Voir le détail"
            onClick={() => navigate(`${LIST_PATH}/${m.id}`)}
          >
            <Eye className="h-3.5 w-3.5" />
          </Button>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => navigate(`${LIST_PATH}/${m.id}/edit`)}
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
              onClick={() => setDeleteTarget(m)}
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
          <h1 className="text-2xl font-semibold tracking-tight">Matériel roulant</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Perception et réintégration des véhicules (VHL / Moto)
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {items.length} perception(s) enregistrée(s)
        </p>
        {canCreate && (
          <Button onClick={() => navigate(`${LIST_PATH}/new`)} className="gap-2">
            <Plus className="h-4 w-4" />
            Nouvelle perception
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Car className="h-4 w-4" />
            Matériel roulant ({items.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={items}
            keyExtractor={(m) => m.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par conducteur, chef de bord, observations..."
            emptyMessage="Aucune perception de matériel roulant enregistrée"
            onRowClick={(m) => navigate(`${LIST_PATH}/${m.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la perception"
        message={`Êtes-vous sûr de vouloir supprimer la perception du ${deleteTarget ? formatDate(deleteTarget.date_perception) : ""} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
