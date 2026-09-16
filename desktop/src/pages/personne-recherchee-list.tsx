import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPersonneRechercheeList,
  deletePersonneRecherchee,
} from "@/lib/api/personne-recherchee";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, UserSearch, ImageIcon } from "lucide-react";
import type { PersonneRecherchee } from "@/types";

const PJ_MODULE = "pj_personne_recherchee";

export function PersonneRechercheeList() {
  const [items, setItems] = useState<PersonneRecherchee[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<PersonneRecherchee | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_MODULE, "can_create");
  const canDelete = hasPermission(user, PJ_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getPersonneRechercheeList().catch(() => []);
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les personnes recherchées");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      if (deleteTarget) {
        await deletePersonneRecherchee(deleteTarget.id);
        addNotification("success", "Supprimée", "Personne recherchée supprimée avec succès");
      }
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<PersonneRecherchee>[] = [
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (r) => <span className="font-medium">{r.nom}</span>,
    },
    {
      key: "adresse",
      header: "Adresse",
      render: (r) => (
        <span className="line-clamp-1 max-w-xs">{r.adresse ?? "—"}</span>
      ),
    },
    {
      key: "motif",
      header: "Motif",
      render: (r) => (
        <span className="line-clamp-1 max-w-xs">{r.motif}</span>
      ),
    },
    {
      key: "photo_count",
      header: "Photos",
      render: (r) => (
        <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full bg-secondary text-secondary-foreground">
          <ImageIcon className="h-3 w-3" />
          {r.photo_count}
        </span>
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
            onClick={() => navigate(`/pj/personne-recherchee/${r.id}/edit`)}
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
            <UserSearch className="h-5 w-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Personne recherchée</h1>
            <p className="text-sm text-muted-foreground">
              Gestion des personnes recherchées (Police Judiciaire)
            </p>
          </div>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/pj/personne-recherchee/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouvelle personne
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Liste des personnes recherchées</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/pj/personne-recherchee/${r.id}`)}
            emptyMessage="Aucune personne recherchée à afficher"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer la personne recherchée"
        message={`Voulez-vous vraiment supprimer « ${deleteTarget?.nom} » ? Toutes les photos seront également supprimées.`}
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
