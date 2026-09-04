import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getTypeArmeList,
  createTypeArme,
  updateTypeArme,
  deleteTypeArme,
  type TypeArmePayload,
} from "@/lib/api/arme";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Plus, Pencil, Trash2, Tags, Loader2 } from "lucide-react";
import type { TypeArme } from "@/types";

export const TYPE_ARME_MODULE = "sedentaire_poste_arme";

const EMPTY_FORM: TypeArmePayload = { nom: "", description: "", munitions_stock: 0 };

export function TypeArmeList() {
  const [types, setTypes] = useState<TypeArme[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<TypeArme | null>(null);
  const [deleting, setDeleting] = useState(false);
  // Editor dialog (create + edit share the same dialog)
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<TypeArme | null>(null);
  const [form, setForm] = useState<TypeArmePayload>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, TYPE_ARME_MODULE, "can_create");
  const canEdit = hasPermission(user, TYPE_ARME_MODULE, "can_edit");
  const canDelete = hasPermission(user, TYPE_ARME_MODULE, "can_delete");

  useEffect(() => {
    loadTypes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadTypes() {
    setLoading(true);
    try {
      const data = await getTypeArmeList();
      setTypes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types d'armes");
    } finally {
      setLoading(false);
    }
  }

  function openCreate() {
    setEditing(null);
    setForm(EMPTY_FORM);
    setEditorOpen(true);
  }

  function openEdit(t: TypeArme) {
    setEditing(t);
    setForm({ nom: t.nom, description: t.description ?? "", munitions_stock: t.munitions_stock });
    setEditorOpen(true);
  }

  function closeEditor() {
    setEditorOpen(false);
    setEditing(null);
    setForm(EMPTY_FORM);
  }

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!form.nom.trim()) {
      addNotification("error", "Validation", "Le nom du type d'arme est requis");
      return;
    }
    setSaving(true);
    try {
      const payload: TypeArmePayload = {
        nom: form.nom.trim(),
        description: form.description?.trim() || null,
        munitions_stock: Math.max(0, Number(form.munitions_stock ?? 0)),
      };
      if (editing) {
        await updateTypeArme(editing.id, payload);
        addNotification("success", "Modifié", "Type d'arme modifié avec succès");
      } else {
        await createTypeArme(payload);
        addNotification("success", "Créé", "Type d'arme créé avec succès");
      }
      closeEditor();
      loadTypes();
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
      setSaving(false);
    }
  }

  async function handleDelete(id: number) {
    setDeleting(true);
    try {
      await deleteTypeArme(id);
      addNotification("success", "Supprimé", "Type d'arme supprimé avec succès");
      loadTypes();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer ce type d'arme";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  const columns: Column<TypeArme>[] = [
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (t) => t.nom,
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
        <span className={t.munitions_stock > 0 ? "font-medium" : "text-muted-foreground"}>
          {t.munitions_stock}
        </span>
      ),
    },
    {
      key: "created_at",
      header: "Créé le",
      sortable: true,
      render: (t) => (t.created_at ? t.created_at.slice(0, 10).split("-").reverse().join("/") : "—"),
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
              onClick={() => openEdit(t)}
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
              onClick={() => setDeleteTarget(t)}
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
          <h1 className="text-2xl font-semibold tracking-tight">Types d'armes</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Catalogue des types d'armes utilisés lors de la création d'une arme
          </p>
        </div>
        {canCreate && (
          <Button onClick={openCreate} className="gap-2">
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
            columns={columns}
            data={types}
            keyExtractor={(t) => t.id}
            loading={loading}
            searchable
            searchPlaceholder="Rechercher un type d'arme..."
            emptyMessage="Aucun type d'arme enregistré"
          />
        </CardContent>
      </Card>

      {/* Create / Edit dialog */}
      {editorOpen && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={closeEditor}
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
              {editing ? "Modifier le type d'arme" : "Nouveau type d'arme"}
            </h3>
            <form onSubmit={handleSave} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="type_nom">Nom *</Label>
                <Input
                  id="type_nom"
                  value={form.nom}
                  onChange={(e) => setForm({ ...form, nom: e.target.value })}
                  placeholder="Ex : Pistolet PA 9mm"
                  required
                  autoFocus
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type_description">Description (optionnelle)</Label>
                <Input
                  id="type_description"
                  value={form.description ?? ""}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Courte description du type d'arme"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type_munitions_stock">Stock de munitions</Label>
                <Input
                  id="type_munitions_stock"
                  type="number"
                  min={0}
                  value={form.munitions_stock ?? 0}
                  onChange={(e) =>
                    setForm({ ...form, munitions_stock: Math.max(0, Number(e.target.value)) })
                  }
                  placeholder="0"
                />
                <p className="text-xs text-muted-foreground">
                  Stock partagé entre toutes les armes de ce type (même calibre)
                </p>
              </div>
              <div className="flex items-center justify-end gap-2 pt-2">
                <Button type="button" variant="outline" size="sm" onClick={closeEditor} disabled={saving}>
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={saving} className="gap-2">
                  {saving ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Plus className="h-3.5 w-3.5" />
                  )}
                  {editing ? "Mettre à jour" : "Créer"}
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le type d'arme"
        message={`Êtes-vous sûr de vouloir supprimer le type « ${deleteTarget?.nom} » ? Cette action est impossible si des armes y sont liées.`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDelete(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />
    </motion.div>
  );
}
