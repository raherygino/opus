import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { getUserList, deleteUser, updateUser } from "@/lib/api/users";
import { getRoleList } from "@/lib/api/roles";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, Shield, Circle, Loader2 } from "lucide-react";
import type { User, Role } from "@/types";

const roleColors: Record<string, string> = {
  SUPER_ADMIN: "text-red-500",
  CHIEF: "text-purple-500",
  STATION_ADMIN: "text-blue-500",
  HEAD_SG: "text-green-500",
  HEAD_SED: "text-yellow-500",
  HEAD_PJ: "text-orange-500",
  INVESTIGATOR: "text-cyan-500",
  OFFICER: "text-indigo-500",
  RECEPTION: "text-pink-500",
  CLERK: "text-slate-500",
  CUSTODY: "text-teal-500",
};

export function UsersList() {
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [changingRole, setChangingRole] = useState<number | null>(null);
  const navigate = useNavigate();
  const { user: currentUser } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const isSuperAdmin = currentUser?.role_code === "SUPER_ADMIN";

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const [usersData, rolesData] = await Promise.all([
        getUserList(),
        getRoleList(),
      ]);
      setUsers(usersData);
      setRoles(rolesData);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les données");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteUser(id);
      addNotification("success", "Supprimé", "Utilisateur supprimé avec succès");
      loadData();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer cet utilisateur");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  async function handleRoleChange(userId: number, roleId: string) {
    setChangingRole(userId);
    try {
      await updateUser(userId, { role_id: Number(roleId) });
      addNotification("success", "Rôle modifié", "Le rôle de l'utilisateur a été mis à jour");
      loadData();
    } catch {
      addNotification("error", "Erreur", "Impossible de modifier le rôle");
    } finally {
      setChangingRole(null);
    }
  }

  const columns: Column<User>[] = [
    { key: "username", header: "Nom d'utilisateur", sortable: true },
    { key: "lastname", header: "Nom", sortable: true },
    { key: "firstname", header: "Prénom", sortable: true },
    { key: "im", header: "Matricule", sortable: true },
    {
      key: "role_name",
      header: "Rôle",
      sortable: true,
      render: (u) => (
        <div className="flex items-center gap-2">
          {isSuperAdmin && u.id !== currentUser?.id ? (
            <div className="relative">
              <Select
                value={String(u.role_id)}
                onChange={(e) => handleRoleChange(u.id, e.target.value)}
                options={roles.map((r) => ({ value: String(r.id), label: r.name }))}
                disabled={changingRole === u.id}
              />
              {changingRole === u.id && (
                <Loader2 className="absolute right-8 top-1/2 -translate-y-1/2 h-3 w-3 animate-spin" />
              )}
            </div>
          ) : (
            <span className={`inline-flex items-center gap-1.5 text-sm ${roleColors[u.role_code] || ""}`}>
              <Circle className="h-2 w-2 fill-current" />
              {u.role_name}
            </span>
          )}
        </div>
      ),
    },
    {
      key: "is_active",
      header: "Statut",
      render: (u) => (
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          u.is_active
            ? "bg-green-500/10 text-green-500"
            : "bg-red-500/10 text-red-500"
        }`}>
          {u.is_active ? "Actif" : "Inactif"}
        </span>
      ),
    },
    {
      key: "last_login",
      header: "Dernière connexion",
      render: (u) => (
        <span className="text-xs text-muted-foreground">
          {u.last_login ? new Date(u.last_login).toLocaleString("fr-FR") : "Jamais"}
        </span>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (u) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => navigate(`/users/${u.id}/edit`)}>
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {isSuperAdmin && u.id !== currentUser?.id && (
            <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => setDeleteTarget(u)}>
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
          <h1 className="text-2xl font-semibold tracking-tight">Utilisateurs</h1>
          <p className="text-sm text-muted-foreground mt-1">Gestion des comptes utilisateurs du système</p>
        </div>
        {isSuperAdmin && (
          <Button onClick={() => navigate("/users/new")} className="gap-2">
            <Plus className="h-4 w-4" />
            Nouvel utilisateur
          </Button>
        )}
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Shield className="h-4 w-4" />
            Comptes utilisateurs ({users.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={columns}
            data={users}
            keyExtractor={(u) => u.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher un utilisateur..."
            onRowClick={(u) => navigate(`/users/${u.id}/edit`)}
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer l'utilisateur"
        message={`Êtes-vous sûr de vouloir supprimer l'utilisateur "${deleteTarget?.username}" ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
