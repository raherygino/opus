import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getSituationGavList,
  deleteSituationGav,
} from "@/lib/api/situation-gav";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, ClipboardCheck } from "lucide-react";
import type { SituationGav } from "@/types";

const MODULE = "sedentaire_poste_situation_gav";
const LIST_PATH = "/sedentaire/poste/situation-gav";

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const datePart = date.slice(0, 10);
  const timePart = date.substring(11, 16);
  const [y, m, d] = datePart.split("-");
  const dateDisplay = y && m && d ? `${d}/${m}/${y}` : datePart;
  return `${dateDisplay} ${timePart}`.trim();
}

function personneLabel(s: SituationGav): string {
  return [s.personne_nom, s.personne_prenoms].filter(Boolean).join(" ") || "—";
}

function agentLabel(s: SituationGav): string {
  return [s.agent_controle_grade, s.agent_controle_nom].filter(Boolean).join(" ") || "—";
}

export function SituationGavList() {
  const [items, setItems] = useState<SituationGav[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<SituationGav | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, MODULE, "can_create");
  const canDelete = hasPermission(user, MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getSituationGavList().catch(() => []);
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les situations GAV");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTarget) {
        await deleteSituationGav(deleteTarget.id);
        addNotification("success", "Supprimée", "Situation GAV supprimée avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<SituationGav>[] = [
    {
      key: "personne",
      header: "Personne concernée",
      sortable: true,
      render: (s) => (
        <div>
          <span className="font-medium">{s.personne_nom ?? "—"}</span>
          {s.personne_prenoms && (
            <span className="text-muted-foreground"> {s.personne_prenoms}</span>
          )}
        </div>
      ),
    },
    {
      key: "date_controle",
      header: "Date du contrôle",
      sortable: true,
      render: (s) => formatDateTime(s.date_controle),
    },
    {
      key: "agent",
      header: "Agent",
      render: (s) => agentLabel(s),
    },
    {
      key: "etat_general",
      header: "État général",
      render: (s) => (
        <span className="line-clamp-1 max-w-xs">{s.etat_general ?? "—"}</span>
      ),
    },
    {
      key: "observations",
      header: "Observations",
      render: (s) => (
        <span className="line-clamp-1 max-w-xs">{s.observations ?? "—"}</span>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (s) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`${LIST_PATH}/${s.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(s)}
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
            <ClipboardCheck className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Situation GAV</h1>
            <p className="text-sm text-muted-foreground">
              Contrôles des personnes en garde à vue (Sédentaire — Poste)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate(`${LIST_PATH}/new`)}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle situation
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des situations GAV</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(s) => s.id}
            onRowClick={(s) => navigate(`${LIST_PATH}/${s.id}`)}
            emptyMessage="Aucune situation GAV à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la situation GAV"
        message={`Voulez-vous vraiment supprimer la situation GAV de « ${personneLabel(deleteTarget ?? ({} as SituationGav))} » ? Toutes les pièces jointes seront également supprimées.`}
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
