import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRassemblementList, deleteRassemblement } from "@/lib/api/rassemblement-journalier";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, File as FileIcon } from "lucide-react";
import type { RassemblementJournalier } from "@/types";

const SG_RASSEMBLEMENT_MODULE = "sg_rassemblement_journalier";

export function RassemblementJournalierList() {
  const [items, setItems] = useState<RassemblementJournalier[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<RassemblementJournalier | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, SG_RASSEMBLEMENT_MODULE, "can_create");
  const canDelete = hasPermission(user, SG_RASSEMBLEMENT_MODULE, "can_delete");

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    setLoading(true);
    try {
      const data = await getRassemblementList();
      setItems(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les rassemblements");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteRassemblement(deleteTarget.id);
      addNotification("success", "Supprimé", "Rassemblement supprimé avec succès");
      setDeleteTarget(null);
      load();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le rassemblement");
    } finally {
      setDeleting(false);
    }
  }

  const columns: Column<RassemblementJournalier>[] = [
    {
      key: "date_rassemblement",
      header: "Date",
      render: (item) => new Date(item.date_rassemblement).toLocaleDateString("fr-FR"),
    },
    { key: "heure_rassemblement", header: "Heure" },
    { key: "brigade_service", header: "Brigade de service" },
    { key: "officier_permanence", header: "Officier de permanence", render: (i) => i.officier_permanence || "—" },
    { key: "inspecteur_permanence", header: "Inspecteur de permanence", render: (i) => i.inspecteur_permanence || "—" },
    { key: "chef_poste", header: "Chef de poste", render: (i) => i.chef_poste || "—" },
    {
      key: "actions",
      header: "Actions",
      className: "w-[130px]",
      render: (item) => (
        <div className="flex items-center gap-1" onClick={(ev) => ev.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/rassemblement-journalier/${item.id}`)}
            title="Consulter"
          >
            <FileIcon className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/sg/rassemblement-journalier/${item.id}/edit`)}
            title="Modifier"
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => setDeleteTarget(item)}
              title="Supprimer"
            >
              <Trash2 className="h-3.5 w-3.5 text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Rassemblement Journalier</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion des rassemblements journaliers
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/sg/rassemblement-journalier/new")}>
            <Plus className="h-4 w-4 mr-2" />
            Nouveau rassemblement
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Liste des rassemblements</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            data={items}
            columns={columns}
            loading={loading}
            keyExtractor={(r) => r.id}
            onRowClick={(r) => navigate(`/sg/rassemblement-journalier/${r.id}`)}
            emptyMessage="Aucun rassemblement enregistré"
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={!!deleteTarget}
        onCancel={() => !deleting && setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Supprimer le rassemblement"
        message={`Voulez-vous vraiment supprimer le rassemblement du ${deleteTarget ? new Date(deleteTarget.date_rassemblement).toLocaleDateString("fr-FR") : ""} ?`}
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}
