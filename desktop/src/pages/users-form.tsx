import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import { getUserById, createUser, updateUser } from "@/lib/api/users";
import { getAvailablePersonnel } from "@/lib/api/personnel";
import { getRoleList } from "@/lib/api/roles";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft, Save, Loader2, Shield, Eye, EyeOff } from "lucide-react";
import type { Personnel, Role } from "@/types";

export function UserForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState({
    personnel_id: "",
    username: "",
    password: "",
    role_id: "",
    is_active: "1",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [availablePersonnel, setAvailablePersonnel] = useState<Personnel[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadData() {
    setLoading(true);
    try {
      const [roleList] = await Promise.all([getRoleList()]);
      setRoles(roleList);
      if (!isEdit) {
        const avail = await getAvailablePersonnel();
        setAvailablePersonnel(avail);
      }
      if (isEdit) {
        const u = await getUserById(Number(id));
        setForm({
          personnel_id: String(u.personnel_id),
          username: u.username,
          password: "",
          role_id: String(u.role_id),
          is_active: String(u.is_active),
        });
      }
    } catch {
      addNotification("error", "Erreur", "Erreur lors du chargement des données");
      navigate("/users");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      if (isEdit) {
        const payload: Record<string, unknown> = {
          username: form.username,
          role_id: Number(form.role_id),
          is_active: Number(form.is_active),
        };
        if (form.password) {
          payload.password = form.password;
        }
        await updateUser(Number(id), payload as Parameters<typeof updateUser>[1]);
        addNotification("success", "Modifié", "Utilisateur mis à jour avec succès");
      } else {
        await createUser({
          personnel_id: Number(form.personnel_id),
          username: form.username,
          password: form.password,
          role_id: Number(form.role_id),
        });
        addNotification("success", "Créé", "Utilisateur créé avec succès");
      }
      navigate("/users");
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
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mx-auto max-w-2xl space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/users")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier l'utilisateur" : "Nouvel utilisateur"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit ? "Modifier les informations du compte" : "Créer un compte pour un agent"}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Shield className="h-4 w-4" />
            Informations du compte
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {!isEdit && (
              <div className="space-y-2">
                <Label htmlFor="personnel_id">Agent *</Label>
                <Select
                  id="personnel_id"
                  value={form.personnel_id}
                  onChange={(e) => setForm({ ...form, personnel_id: e.target.value })}
                  options={availablePersonnel.map((p) => ({
                    value: String(p.id),
                    label: `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`,
                  }))}
                  placeholder="Sélectionner un agent"
                  required
                />
                {availablePersonnel.length === 0 && (
                  <p className="text-xs text-muted-foreground">Tous les agents ont déjà un compte utilisateur.</p>
                )}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="username">Nom d'utilisateur *</Label>
              <Input
                id="username"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
                minLength={3}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">
                Mot de passe {isEdit ? "(laisser vide pour conserver l'actuel)" : "*"}
              </Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  required={!isEdit}
                  minLength={6}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="role_id">Rôle *</Label>
              <Select
                id="role_id"
                value={form.role_id}
                onChange={(e) => setForm({ ...form, role_id: e.target.value })}
                options={roles.map((r) => ({ value: String(r.id), label: r.name }))}
                placeholder="Sélectionner un rôle"
                required
              />
            </div>

            {isEdit && (
              <div className="space-y-2">
                <Label htmlFor="is_active">Statut</Label>
                <Select
                  id="is_active"
                  value={form.is_active}
                  onChange={(e) => setForm({ ...form, is_active: e.target.value })}
                  options={[
                    { value: "1", label: "Actif" },
                    { value: "0", label: "Inactif" },
                  ]}
                />
              </div>
            )}

            <div className="flex items-center gap-3 pt-4">
              <Button type="submit" className="gap-2" disabled={saving}>
                {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                {saving ? "Enregistrement..." : "Enregistrer"}
              </Button>
              <Button type="button" variant="outline" onClick={() => navigate("/users")}>
                Annuler
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </motion.div>
  );
}
