import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRegistreEnqueteList, deleteRegistreEnquete } from "@/lib/api/registre-enquete";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, FileSearch } from "lucide-react";
import {
  REGISTRE_ENQUETE_STATUT_LABELS,
  type RegistreEnquete,
  type RegistreEnqueteStatut,
} from "@/types";

const PJ_ENQUETE_MODULE = "pj_enquete";

const STATUT_VARIANTS: Record<RegistreEnqueteStatut, "default" | "secondary" | "destructive" | "outline"> = {
  EN_COURS: "default",
  SUSPENDUE: "secondary",
  TRANSMISE: "secondary",
  CLOTUREE: "outline",
};

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const d = new Date(date.replace(" ", "T"));
  if (isNaN(d.getTime())) return date;
  return d.toLocaleDateString("fr-FR");
}

function enqueteurLabel(e: RegistreEnquete): string {
  const name = [e.enqueteur_prenoms, e.enqueteur_nom].filter(Boolean).join(" ");
  if (!name) return "—";
  return e.enqueteur_grade ? `${name} (${e.enqueteur_grade})` : name;
}

export function RegistreEnqueteList() {
  const [items, setItems] = useState<RegistreEnquete[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<RegistreEnquete | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_ENQUETE_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_ENQUETE_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getRegistreEnqueteList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger le registre d'enquête");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteRegistreEnquete(deleteTarget.id);
      addNotification("success", "Supprimé", "Entrée supprimée avec succès");
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'entrée");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<RegistreEnquete>[] = [
    {
      key: "numero",
      header: "Numéro",
      sortable: true,
      render: (r) => <span className="font-mono text-sm">{r.numero}</span>,
    },
    {
      key: "date_ouverture",
      header: "Date d'ouverture",
      sortable: true,
      render: (r) => <span className="text-sm">{formatDate(r.date_ouverture)}</span>,
    },
    {
      key: "nature_infraction",
      header: "Nature de l'infraction",
      sortable: true,
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.nature_infraction}</span>,
    },
    {
      key: "mise_en_cause",
      header: "Mise en cause",
      render: (r) => <span className="line-clamp-1 max-w-xs">{r.mise_en_cause ?? "—"}</span>,
    },
    {
      key: "enqueteur",
      header: "Enquêteur",
      render: (r) => <span className="line-clamp-1 max-w-xs">{enqueteurLabel(r)}</span>,
    },
    {
      key: "statut",
      header: "Statut",
      render: (r) => (
        <Badge variant={STATUT_VARIANTS[r.statut] ?? "outline"}>
          {REGISTRE_ENQUETE_STATUT_LABELS[r.statut] ?? r.statut}
        </Badge>
      ),
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
            onClick={() => navigate(`/pj/registre-enquete/${r.id}/edit`)}
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
            <FileSearch className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Registre d'enquête</h1>
            <p className="text-sm text-muted-foreground">
              Police Judiciaire — Registre des dossiers d'enquête
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/registre-enquete/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle entrée
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des dossiers d'enquête</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/registre-enquete/${r.id}`)}
            emptyMessage="Aucune entrée à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'entrée"
        message="Voulez-vous vraiment supprimer cette entrée du registre d'enquête ? Toutes les pièces jointes seront également supprimées."
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
