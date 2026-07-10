import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import { getRoleById, createRole, updateRole, updateRolePermissions } from "@/lib/api/roles";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { ArrowLeft, Save, Loader2, ShieldCheck, Check, X } from "lucide-react";
import type { RolePermission } from "@/types";

const MODULES = [
  { key: "sedentaire_secretariat_correspondance", label: "Sédentaire > Secrétariat > Correspondance" },
  { key: "sedentaire_secretariat_declaration_perte", label: "Sédentaire > Secrétariat > Déclaration de perte" },
  { key: "sedentaire_secretariat_rapport", label: "Sédentaire > Secrétariat > Rapport" },
  { key: "sedentaire_secretariat_main_courante", label: "Sédentaire > Secrétariat > Main courante" },
  { key: "personnel", label: "Sédentaire > Secrétariat > Personnel" },
  { key: "sedentaire_poste_passation", label: "Sédentaire > Poste > Passation" },
  { key: "sedentaire_poste_armement", label: "Sédentaire > Poste > Armement" },
  { key: "sedentaire_poste_materiels", label: "Sédentaire > Poste > Matériels" },
  { key: "sedentaire_poste_situation_gav", label: "Sédentaire > Poste > Situation GAV" },
  { key: "sedentaire_poste_main_courante", label: "Sédentaire > Poste > Main courante" },
  { key: "sedentaire_poste_renseignement", label: "Sédentaire > Poste > Renseignement" },
  { key: "sg_spa", label: "Service Général > SPA" },
  { key: "sg_info_rassemblement", label: "Service Général > Info rassemblement" },
  { key: "sg_repartition", label: "Service Général > Répartition" },
  { key: "sg_patrouille", label: "Service Général > Patrouille" },
  { key: "sg_intervention", label: "Service Général > Intervention" },
  { key: "sg_dispositif_exceptionnel", label: "Service Général > Dispositif exceptionnel" },
  { key: "sg_instruction_autorite", label: "Service Général > Instruction autorité" },
  { key: "sg_compte_rendu", label: "Service Général > Compte rendu" },
  { key: "sg_recherche", label: "Service Général > Recherche" },
  { key: "sg_renseignement", label: "Service Général > Renseignement" },
  { key: "pj_plainte", label: "Police Judiciaire > Plainte reçue" },
  { key: "pj_enquete", label: "Police Judiciaire > Registre d'enquête" },
  { key: "pj_mandat", label: "Police Judiciaire > Mandat" },
  { key: "pj_convocation", label: "Police Judiciaire > Convocation" },
  { key: "pj_arrestation", label: "Police Judiciaire > Arrestation" },
  { key: "pj_gav", label: "Police Judiciaire > GAV" },
  { key: "pj_requisition", label: "Police Judiciaire > Réquisition" },
  { key: "pj_personne_recherchee", label: "Police Judiciaire > Personne recherchée" },
  { key: "pj_objets", label: "Police Judiciaire > Objets" },
  { key: "pj_deferrement", label: "Police Judiciaire > Registre de déferrement" },
  { key: "pj_renseignement", label: "Police Judiciaire > Renseignement" },
  { key: "cartographie", label: "Cartographie" },
  { key: "users", label: "Utilisateurs" },
];

const ACTIONS = [
  { key: "can_view", label: "View" },
  { key: "can_create", label: "Create" },
  { key: "can_edit", label: "Edit" },
  { key: "can_delete", label: "Delete" },
  { key: "can_export", label: "Export" },
] as const;

type ModulePerms = { can_view: number; can_create: number; can_edit: number; can_delete: number; can_export: number };
type PermissionsMap = Record<string, ModulePerms>;

function buildEmptyPermissions(): PermissionsMap {
  const map: PermissionsMap = {};
  for (const m of MODULES) {
    map[m.key] = { can_view: 0, can_create: 0, can_edit: 0, can_delete: 0, can_export: 0 };
  }
  return map;
}

function permissionsToMap(perms: RolePermission[]): PermissionsMap {
  const map = buildEmptyPermissions();
  for (const p of perms) {
    if (map[p.module]) {
      map[p.module] = {
        can_view: p.can_view,
        can_create: p.can_create,
        can_edit: p.can_edit,
        can_delete: p.can_delete,
        can_export: p.can_export,
      };
    }
  }
  return map;
}

export function RoleForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState({ name: "", description: "" });
  const [permissions, setPermissions] = useState<PermissionsMap>(buildEmptyPermissions);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadRole();
    }
  }, [id]);

  async function loadRole() {
    setLoading(true);
    try {
      const r = await getRoleById(Number(id));
      setForm({ name: r.name, description: r.description || "" });
      setPermissions(permissionsToMap(r.permissions || []));
    } catch {
      addNotification("error", "Erreur", "Erreur lors du chargement du rôle");
      navigate("/roles");
    } finally {
      setLoading(false);
    }
  }

  function togglePermission(module: string, action: keyof ModulePerms) {
    setPermissions((prev) => ({
      ...prev,
      [module]: {
        ...prev[module],
        [action]: prev[module][action] ? 0 : 1,
      },
    }));
  }

  function selectAll(module: string, value: number) {
    setPermissions((prev) => ({
      ...prev,
      [module]: {
        can_view: value,
        can_create: value,
        can_edit: value,
        can_delete: value,
        can_export: value,
      },
    }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      if (isEdit) {
        await updateRole(Number(id), form);
        await updateRolePermissions(Number(id), permissions);
        addNotification("success", "Modifié", "Rôle et permissions mis à jour avec succès");
      } else {
        const code = form.name.toUpperCase().replace(/\s+/g, "_").replace(/[^A-Z0-9_]/g, "");
        const created = await createRole({ code, name: form.name, description: form.description });
        await updateRolePermissions(created.id, permissions);
        addNotification("success", "Créé", "Rôle créé avec succès");
      }
      navigate("/roles");
    } catch (err: unknown) {
      const msg = err && typeof err === "object" && "response" in err
        ? (err as { response: { data: { message: string } } }).response?.data?.message || "Erreur"
        : "Erreur lors de l'enregistrement";
      addNotification("error", "Erreur", msg);
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/roles")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier le rôle" : "Nouveau rôle"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit ? "Modifier les informations et permissions du rôle" : "Créer un nouveau rôle système"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Role Info */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="h-4 w-4" />
              Informations du rôle
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Nom du rôle</Label>
              <Input
                id="name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Ex: Manager RH"
                required
                maxLength={100}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Input
                id="description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Description du rôle..."
                maxLength={255}
              />
            </div>
          </CardContent>
        </Card>

        {/* Permissions */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Check className="h-4 w-4" />
              Permissions
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="text-left py-3 pr-4 font-medium text-muted-foreground">Module</th>
                    {ACTIONS.map((a) => (
                      <th key={a.key} className="text-center py-3 px-2 font-medium text-muted-foreground text-xs uppercase tracking-wider">
                        {a.label}
                      </th>
                    ))}
                    <th className="text-center py-3 pl-2 font-medium text-muted-foreground text-xs uppercase tracking-wider">Tout</th>
                  </tr>
                </thead>
                <tbody>
                  {MODULES.map((mod) => {
                    const perm = permissions[mod.key];
                    const allChecked = ACTIONS.every((a) => perm[a.key]);
                    return (
                      <tr key={mod.key} className="border-b border-border/50 hover:bg-accent/30 transition-colors">
                        <td className="py-3 pr-4 font-medium">{mod.label}</td>
                        {ACTIONS.map((a) => (
                          <td key={a.key} className="text-center py-3 px-2">
                            <button
                              type="button"
                              onClick={() => togglePermission(mod.key, a.key)}
                              className={`inline-flex items-center justify-center h-7 w-7 rounded-md border transition-colors ${
                                perm[a.key]
                                  ? "bg-primary text-primary-foreground border-primary"
                                  : "border-input text-muted-foreground hover:border-muted-foreground"
                              }`}
                            >
                              {perm[a.key] ? <Check className="h-3.5 w-3.5" /> : <X className="h-3.5 w-3.5" />}
                            </button>
                          </td>
                        ))}
                        <td className="text-center py-3 pl-2">
                          <button
                            type="button"
                            onClick={() => selectAll(mod.key, allChecked ? 0 : 1)}
                            className="text-xs text-muted-foreground hover:text-foreground transition-colors underline underline-offset-2"
                          >
                            {allChecked ? "Aucun" : "Tous"}
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        <div className="flex items-center gap-3">
          <Button type="submit" className="gap-2 min-w-[140px]" disabled={saving}>
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {saving ? "Enregistrement..." : "Enregistrer"}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate("/roles")}>
            Annuler
          </Button>
        </div>
      </form>
    </motion.div>
  );
}
