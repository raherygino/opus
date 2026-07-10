import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getPersonnelList, deletePersonnel } from "@/lib/api/personnel";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Users, Pencil, Trash2 } from "lucide-react";
import type { Personnel } from "@/types";

export function PersonnelList() {
  const [personnel, setPersonnel] = useState<Personnel[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Personnel | null>(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, "personnel", "can_create");
  const canDelete = hasPermission(user, "personnel", "can_delete");

  useEffect(() => {
    loadPersonnel();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadPersonnel() {
    setLoading(true);
    try {
      const data = await getPersonnelList();
      setPersonnel(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger la liste du personnel");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deletePersonnel(id);
      addNotification("success", "Supprimé", "Personnel supprimé avec succès");
      loadPersonnel();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer ce personnel");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<Personnel>[] = [
    { key: "im", header: "IM", sortable: true },
    { key: "lastname", header: "Nom", sortable: true },
    { key: "firstname", header: "Prénoms", sortable: true },
    { key: "grade", header: "Grade", sortable: true },
    { key: "affectation", header: "Affectation", sortable: true },
    {
      key: "status",
      header: "Statut",
      render: (p) => (
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          p.status === "En service"
            ? "bg-green-500/10 text-green-500"
            : "bg-amber-500/10 text-amber-500"
        }`}>
          {p.status}
        </span>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (p) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => navigate(`/personnel/${p.id}/edit`)}>
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => setDeleteTarget(p)}>
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
          <h1 className="text-2xl font-semibold tracking-tight">Personnel</h1>
          <p className="text-sm text-muted-foreground mt-1">Gestion des effectifs du commissariat</p>
        </div>
        {canCreate && (
          <Button onClick={() => navigate("/personnel/new")} className="gap-2">
            <Plus className="h-4 w-4" />
            Nouveau personnel
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="h-4 w-4" />
            Liste du personnel ({personnel.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={personnel}
            keyExtractor={(p) => p.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher par nom, matricule..."
            onRowClick={(p) => navigate(`/personnel/${p.id}`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le personnel"
        message={`Êtes-vous sûr de vouloir supprimer ${deleteTarget?.firstname} ${deleteTarget?.lastname} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
