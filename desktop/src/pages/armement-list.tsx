import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getArmementList,
  deleteArmement,
  reintegrateArmement,
} from "@/lib/api/armement";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ReintegrationDialog } from "@/pages/armement-reintegration-dialog";
import { Plus, ShieldEllipsis, Pencil, Trash2, ShieldCheck } from "lucide-react";
import type { Armement } from "@/types";
import type { ReintegrationPayload } from "@/lib/api/armement";
import { formatDate, formatHeure } from "@/pages/passation-list";

export const ARMEMENT_MODULE = "sedentaire_poste_armement";

export function isReintegree(a: Armement): boolean {
  return a.heure_reintegration !== null && a.heure_reintegration !== undefined;
}

export function agentPreneurDisplay(a: Armement): string {
  return (
    [a.agent_preneur_grade, a.agent_preneur_nom].filter(Boolean).join(" ") ||
    "—"
  );
}

export function armeDisplay(a: Armement): string {
  return [a.type_arme, a.matricule_arme].filter(Boolean).join(" — ") || "—";
}

const LIST_PATH = "/sedentaire/poste/armement";

export function ArmementList() {
  const [armements, setArmements] = useState<Armement[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Armement | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [reintTarget, setReintTarget] = useState<Armement | null>(null);
  const [reintegrating, setReintegrating] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, ARMEMENT_MODULE, "can_create");
  const canEdit = hasPermission(user, ARMEMENT_MODULE, "can_edit");
  const canDelete = hasPermission(user, ARMEMENT_MODULE, "can_delete");

  useEffect(() => {
    loadArmements();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadArmements() {
    setLoading(true);
    try {
      const data = await getArmementList();
      setArmements(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les armements");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteArmement(id);
      addNotification("success", "Supprimé", "Armement supprimé avec succès");
      loadArmements();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cet armement");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  async function handleReintegrate(payload: ReintegrationPayload) {
    if (!reintTarget) return;
    setReintegrating(true);
    try {
      await reintegrateArmement(reintTarget.id, payload);
      addNotification("success", "Réintégrée", "Arme réintégrée avec succès");
      loadArmements();
    } catch (err: unknown) {
      let msg = "Erreur lors de la réintégration";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          msg = Object.values(resp.data.errors).join(", ");
        } else if (resp?.data?.message) {
          msg = resp.data.message;
        }
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setReintegrating(false);
      setReintTarget(null);
    }
  }

  const columns: Column<Armement>[] = [
    {
      key: "date_perception",
      header: "Date",
      sortable: true,
      render: (a) => formatDate(a.date_perception),
    },
    {
      key: "heure_perception",
      header: "Heure",
      sortable: true,
      render: (a) => formatHeure(a.heure_perception),
    },
    {
      key: "agent_preneur",
      header: "Agent preneur",
      sortable: true,
      render: (a) => agentPreneurDisplay(a),
    },
    {
      key: "arme",
      header: "Arme",
      sortable: true,
      render: (a) => armeDisplay(a),
    },
    {
      key: "secteur_mission",
      header: "Secteur / Mission",
      sortable: true,
      render: (a) => a.secteur_mission || "—",
    },
    {
      key: "statut",
      header: "Statut",
      sortable: true,
      render: (a) =>
        isReintegree(a) ? (
          <Badge variant="secondary">Réintégrée</Badge>
        ) : (
          <Badge>En cours</Badge>
        ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[130px]",
      render: (a) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {canEdit && !isReintegree(a) && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-primary"
              title="Réintégrer l'arme"
              onClick={() => setReintTarget(a)}
            >
              <ShieldCheck className="h-3.5 w-3.5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            title="Modifier la perception"
            onClick={() => navigate(`${LIST_PATH}/${a.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
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
          <h1 className="text-2xl font-semibold tracking-tight">Armement</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Registre des perceptions et réintégrations d'armes
          </p>
        </div>
        {canCreate && (
          <Button
            onClick={() => navigate(`${LIST_PATH}/new`)}
            className="gap-2"
          >
            <Plus className="h-4 w-4" />
            Nouvelle perception
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between gap-4 flex-wrap">
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldEllipsis className="h-4 w-4" />
              Armements ({armements.length})
            </CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={armements}
            keyExtractor={(a) => a.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par agent, arme, matricule, secteur..."
            emptyMessage="Aucun armement"
            onRowClick={(a) => navigate(`${LIST_PATH}/${a.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'armement"
        message={`Êtes-vous sûr de vouloir supprimer la perception du ${deleteTarget ? formatDate(deleteTarget.date_perception) : ""} (${deleteTarget ? armeDisplay(deleteTarget) : ""}) ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />

      <ReintegrationDialog
        open={reintTarget !== null}
        armement={reintTarget}
        loading={reintegrating}
        onConfirm={handleReintegrate}
        onCancel={() => setReintTarget(null)}
      />
    </motion.div>
  );
}
