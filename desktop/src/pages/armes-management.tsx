import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getTypeArmeList,
  createTypeArme,
  updateTypeArme,
  deleteTypeArme,
  getArmeList,
  createArme,
  updateArme,
  deleteArme,
  type TypeArmePayload,
  type ArmePayload,
} from "@/lib/api/arme";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Plus,
  Pencil,
  Trash2,
  Tags,
  Crosshair,
  Eye,
  Loader2,
} from "lucide-react";
import type { TypeArme, Arme } from "@/types";

export const ARMES_MODULE = "sedentaire_poste_arme";
const LIST_PATH = "/sedentaire/poste/armes";

const EMPTY_TYPE_FORM: TypeArmePayload = { nom: "", description: "", munitions_stock: 0 };

export function ArmesManagement() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") === "types" ? "types" : "armes";
  const [tab, setTab] = useState(initialTab);

  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, ARMES_MODULE, "can_create");
  const canEdit = hasPermission(user, ARMES_MODULE, "can_edit");
  const canDelete = hasPermission(user, ARMES_MODULE, "can_delete");

  // ---- Types d'armes state ----
  const [types, setTypes] = useState<TypeArme[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typeEditorOpen, setTypeEditorOpen] = useState(false);
  const [editingType, setEditingType] = useState<TypeArme | null>(null);
  const [typeForm, setTypeForm] = useState<TypeArmePayload>(EMPTY_TYPE_FORM);
  const [typeSaving, setTypeSaving] = useState(false);
  const [typeDeleteTarget, setTypeDeleteTarget] = useState<TypeArme | null>(null);
  const [typeDeleting, setTypeDeleting] = useState(false);

  // ---- Armes state ----
  const [armes, setArmes] = useState<Arme[]>([]);
  const [armesLoading, setArmesLoading] = useState(true);
  const [armeEditorOpen, setArmeEditorOpen] = useState(false);
  const [editingArme, setEditingArme] = useState<Arme | null>(null);
  const [armeForm, setArmeForm] = useState<ArmePayload>({ type_arme_id: 0, matricule: "" });
  const [armeSaving, setArmeSaving] = useState(false);
  const [armeDeleteTarget, setArmeDeleteTarget] = useState<Arme | null>(null);
  const [armeDeleting, setArmeDeleting] = useState(false);

  useEffect(() => {
    loadTypes();
    loadArmes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleTabChange(value: string) {
    setTab(value);
    setSearchParams(value === "types" ? { tab: "types" } : {}, { replace: true });
  }

  // ---- Types d'armes CRUD ----
  async function loadTypes() {
    setTypesLoading(true);
    try {
      const data = await getTypeArmeList();
      setTypes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types d'armes");
    } finally {
      setTypesLoading(false);
    }
  }

  function openCreateType() {
    setEditingType(null);
    setTypeForm(EMPTY_TYPE_FORM);
    setTypeEditorOpen(true);
  }

  function openEditType(t: TypeArme) {
    setEditingType(t);
    setTypeForm({
      nom: t.nom,
      description: t.description ?? "",
      munitions_stock: t.munitions_stock,
    });
    setTypeEditorOpen(true);
  }

  function closeTypeEditor() {
    setTypeEditorOpen(false);
    setEditingType(null);
    setTypeForm(EMPTY_TYPE_FORM);
  }

  async function handleSaveType(e: React.FormEvent) {
    e.preventDefault();
    if (!typeForm.nom.trim()) {
      addNotification("error", "Validation", "Le nom du type d'arme est requis");
      return;
    }
    setTypeSaving(true);
    try {
      const payload: TypeArmePayload = {
        nom: typeForm.nom.trim(),
        description: typeForm.description?.trim() || null,
        munitions_stock: Math.max(0, Number(typeForm.munitions_stock ?? 0)),
      };
      if (editingType) {
        await updateTypeArme(editingType.id, payload);
        addNotification("success", "Modifié", "Type d'arme modifié avec succès");
      } else {
        await createTypeArme(payload);
        addNotification("success", "Créé", "Type d'arme créé avec succès");
      }
      closeTypeEditor();
      loadTypes();
      // Refresh armes too since they join type info.
      loadArmes();
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer le type d'arme";
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
      setTypeSaving(false);
    }
  }

  async function handleDeleteType(id: number) {
    setTypeDeleting(true);
    try {
      await deleteTypeArme(id);
      addNotification("success", "Supprimé", "Type d'arme supprimé avec succès");
      loadTypes();
      loadArmes();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer ce type d'arme";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setTypeDeleting(false);
      setTypeDeleteTarget(null);
    }
  }

  // ---- Armes CRUD ----
  async function loadArmes() {
    setArmesLoading(true);
    try {
      const data = await getArmeList();
      setArmes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les armes");
    } finally {
      setArmesLoading(false);
    }
  }

  async function handleDeleteArme(id: number) {
    setArmeDeleting(true);
    try {
      await deleteArme(id);
      addNotification("success", "Supprimée", "Arme supprimée avec succès");
      loadArmes();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer cette arme";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setArmeDeleting(false);
      setArmeDeleteTarget(null);
    }
  }

  function openCreateArme() {
    setEditingArme(null);
    setArmeForm({ type_arme_id: 0, matricule: "" });
    setArmeEditorOpen(true);
  }

  function openEditArme(a: Arme) {
    setEditingArme(a);
    setArmeForm({ type_arme_id: a.type_arme_id, matricule: a.matricule });
    setArmeEditorOpen(true);
  }

  function closeArmeEditor() {
    setArmeEditorOpen(false);
    setEditingArme(null);
    setArmeForm({ type_arme_id: 0, matricule: "" });
  }

  async function handleSaveArme(e: React.FormEvent) {
    e.preventDefault();
    if (armeForm.type_arme_id <= 0) {
      addNotification("error", "Validation", "Le type d'arme est requis");
      return;
    }
    if (!armeForm.matricule.trim()) {
      addNotification("error", "Validation", "Le matricule est requis");
      return;
    }
    setArmeSaving(true);
    try {
      const payload: ArmePayload = {
        type_arme_id: armeForm.type_arme_id,
        matricule: armeForm.matricule.trim(),
      };
      if (editingArme) {
        await updateArme(editingArme.id, payload);
        addNotification("success", "Modifiée", "Arme modifiée avec succès");
      } else {
        await createArme(payload);
        addNotification("success", "Créée", "Arme créée avec succès");
      }
      closeArmeEditor();
      loadArmes();
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer l'arme";
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
      setArmeSaving(false);
    }
  }

  // ---- Type columns ----
  const typeColumns: Column<TypeArme>[] = [
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (t) => <span className="font-medium">{t.nom}</span>,
    },
    {
      key: "description",
      header: "Description",
      sortable: false,
      render: (t) => t.description || "—",
    },
    {
      key: "munitions_stock",
      header: "Stock de munitions",
      sortable: true,
      render: (t) => (
        <Badge variant={t.munitions_stock > 0 ? "default" : "secondary"}>
          {t.munitions_stock}
        </Badge>
      ),
    },
    {
      key: "created_at",
      header: "Créé le",
      sortable: true,
      render: (t) =>
        t.created_at ? t.created_at.slice(0, 10).split("-").reverse().join("/") : "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[110px]",
      render: (t) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => openEditType(t)}
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
              onClick={() => setTypeDeleteTarget(t)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  // ---- Arme columns ----
  const armeColumns: Column<Arme>[] = [
    {
      key: "type_arme_nom",
      header: "Type d'Arme",
      sortable: true,
      render: (a) => a.type_arme_nom || "—",
    },
    {
      key: "matricule",
      header: "Matricule",
      sortable: true,
      render: (a) => <span className="font-medium">{a.matricule}</span>,
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[120px]",
      render: (a) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            title="Voir le détail"
            onClick={() => navigate(`${LIST_PATH}/${a.id}`)}
          >
            <Eye className="h-3.5 w-3.5" />
          </Button>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => openEditArme(a)}
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
              onClick={() => setArmeDeleteTarget(a)}
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
      {/* Page header — full width */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Armes & Munitions</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion des types d'armes, des armes et du stock de munitions
          </p>
        </div>
      </div>

      {/* Tabs — full width, no centered container */}
      <Tabs value={tab} onValueChange={handleTabChange}>
        <TabsList>
          <TabsTrigger value="armes" className="gap-2">
            <Crosshair className="h-3.5 w-3.5" />
            Armes
          </TabsTrigger>
          <TabsTrigger value="types" className="gap-2">
            <Tags className="h-3.5 w-3.5" />
            Types d'armes
          </TabsTrigger>
        </TabsList>

        {/* ── Tab: Armes ─────────────────────────────────────────── */}
        <TabsContent value="armes" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {armes.length} arme(s) enregistrée(s)
            </p>
            {canCreate && (
              <Button onClick={openCreateArme} className="gap-2">
                <Plus className="h-4 w-4" />
                Nouvelle arme
              </Button>
            )}
          </div>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Crosshair className="h-4 w-4" />
                Armes ({armes.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={armeColumns}
                data={armes}
                keyExtractor={(a) => a.id}
                loading={armesLoading}
                searchable
                searchPlaceholder="Rechercher par type, matricule..."
                emptyMessage="Aucune arme enregistrée"
                onRowClick={(a) => navigate(`${LIST_PATH}/${a.id}`)}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Tab: Types d'armes ─────────────────────────────────── */}
        <TabsContent value="types" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {types.length} type(s) d'arme enregistré(s)
            </p>
            {canCreate && (
              <Button onClick={openCreateType} className="gap-2">
                <Plus className="h-4 w-4" />
                Nouveau type
              </Button>
            )}
          </div>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Tags className="h-4 w-4" />
                Types d'armes ({types.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={typeColumns}
                data={types}
                keyExtractor={(t) => t.id}
                loading={typesLoading}
                searchable
                searchPlaceholder="Rechercher un type d'arme..."
                emptyMessage="Aucun type d'arme enregistré"
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ── Type create/edit dialog ─────────────────────────────── */}
      {typeEditorOpen && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={closeTypeEditor}
        >
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-base font-semibold mb-4">
              {editingType ? "Modifier le type d'arme" : "Nouveau type d'arme"}
            </h3>
            <form onSubmit={handleSaveType} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="type_nom">Nom *</Label>
                <Input
                  id="type_nom"
                  value={typeForm.nom}
                  onChange={(e) => setTypeForm({ ...typeForm, nom: e.target.value })}
                  placeholder="Ex : Pistolet PA 9mm"
                  required
                  autoFocus
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type_description">Description (optionnelle)</Label>
                <Input
                  id="type_description"
                  value={typeForm.description ?? ""}
                  onChange={(e) => setTypeForm({ ...typeForm, description: e.target.value })}
                  placeholder="Courte description du type d'arme"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type_munitions_stock">Stock de munitions</Label>
                <Input
                  id="type_munitions_stock"
                  type="number"
                  min={0}
                  value={typeForm.munitions_stock ?? 0}
                  onChange={(e) =>
                    setTypeForm({
                      ...typeForm,
                      munitions_stock: Math.max(0, Number(e.target.value)),
                    })
                  }
                  placeholder="0"
                />
                <p className="text-xs text-muted-foreground">
                  Stock partagé entre toutes les armes de ce type (même calibre)
                </p>
              </div>
              <div className="flex items-center justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={closeTypeEditor}
                  disabled={typeSaving}
                >
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={typeSaving} className="gap-2">
                  {typeSaving ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Plus className="h-3.5 w-3.5" />
                  )}
                  {editingType ? "Mettre à jour" : "Créer"}
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* ── Arme create/edit dialog ─────────────────────────────── */}
      {armeEditorOpen && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={closeArmeEditor}
        >
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-base font-semibold mb-4">
              {editingArme ? "Modifier l'arme" : "Nouvelle arme"}
            </h3>
            <form onSubmit={handleSaveArme} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="arme_type_arme_id">Type d'arme *</Label>
                <Select
                  id="arme_type_arme_id"
                  value={armeForm.type_arme_id === 0 ? "" : String(armeForm.type_arme_id)}
                  onChange={(e) =>
                    setArmeForm({ ...armeForm, type_arme_id: Number(e.target.value) })
                  }
                  placeholder="Sélectionner un type d'arme"
                  options={types.map((t) => ({ value: String(t.id), label: t.nom }))}
                />
                {types.length === 0 && (
                  <p className="text-xs text-muted-foreground">
                    Aucun type d'arme enregistré. Créez-en un depuis l'onglet « Types d'armes ».
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="arme_matricule">Matricule *</Label>
                <Input
                  id="arme_matricule"
                  value={armeForm.matricule}
                  onChange={(e) => setArmeForm({ ...armeForm, matricule: e.target.value })}
                  placeholder="Ex : PA-0001"
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Numéro de série unique de l'arme
                </p>
              </div>

              {armeForm.type_arme_id > 0 && (
                <div className="space-y-2">
                  <Label>Stock de munitions du type</Label>
                  <div className="rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">
                    {types.find((t) => t.id === armeForm.type_arme_id)?.munitions_stock ?? 0}
                    {" "}
                    <span className="text-muted-foreground">
                      (géré au niveau du type d'arme — toutes les armes de ce type partagent le même stock)
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Le stock de munitions est géré par type d'arme, pas par matricule.
                    Modifiez-le depuis l'onglet « Types d'armes ».
                  </p>
                </div>
              )}

              <div className="flex items-center justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={closeArmeEditor}
                  disabled={armeSaving}
                >
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={armeSaving} className="gap-2">
                  {armeSaving ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Plus className="h-3.5 w-3.5" />
                  )}
                  {editingArme ? "Mettre à jour" : "Créer"}
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* ── Confirm dialogs ─────────────────────────────────────── */}
      <ConfirmDialog
        open={typeDeleteTarget !== null}
        title="Supprimer le type d'arme"
        message={`Êtes-vous sûr de vouloir supprimer le type « ${typeDeleteTarget?.nom} » ? Cette action est impossible si des armes y sont liées.`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={typeDeleting}
        onConfirm={() => typeDeleteTarget && handleDeleteType(typeDeleteTarget.id)}
        onCancel={() => setTypeDeleteTarget(null)}
      />

      <ConfirmDialog
        open={armeDeleteTarget !== null}
        title="Supprimer l'arme"
        message={`Êtes-vous sûr de vouloir supprimer l'arme ${armeDeleteTarget?.matricule} (${armeDeleteTarget?.type_arme_nom}) ? Cette action est impossible si des perceptions ou consommations y sont liées.`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={armeDeleting}
        onConfirm={() => armeDeleteTarget && handleDeleteArme(armeDeleteTarget.id)}
        onCancel={() => setArmeDeleteTarget(null)}
      />
    </motion.div>
  );
}
