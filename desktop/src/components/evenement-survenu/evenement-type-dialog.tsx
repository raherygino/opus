import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { motion } from "framer-motion";
import {
  X,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  Check,
  Tag,
} from "lucide-react";
import {
  getEvenementSurvenuTypes,
  createEvenementSurvenuType,
  updateEvenementSurvenuType,
  deleteEvenementSurvenuType,
} from "@/lib/api/evenement-survenu";
import { useNotificationStore } from "@/stores/notification-store";
import type { EvenementSurvenuTypeItem } from "@/types";

interface EvenementTypeDialogProps {
  open: boolean;
  onClose: () => void;
  /** Called when the catalog changes so the parent can refresh its dropdown. */
  onTypesChanged?: (types: EvenementSurvenuTypeItem[]) => void;
}

export function EvenementTypeDialog({
  open,
  onClose,
  onTypesChanged,
}: EvenementTypeDialogProps) {
  const { addNotification } = useNotificationStore();
  const [types, setTypes] = useState<EvenementSurvenuTypeItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [newLabel, setNewLabel] = useState("");
  const [creating, setCreating] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingLabel, setEditingLabel] = useState("");
  const [savingEdit, setSavingEdit] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<EvenementSurvenuTypeItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!open) return;
    loadTypes();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  async function loadTypes() {
    setLoading(true);
    try {
      const data = await getEvenementSurvenuTypes();
      setTypes(data);
      onTypesChanged?.(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types d'événement");
    } finally {
      setLoading(false);
    }
  }

  async function handleCreate() {
    const label = newLabel.trim();
    if (!label) return;
    setCreating(true);
    try {
      const created = await createEvenementSurvenuType(label);
      const next = [...types, created].sort((a, b) =>
        a.label.localeCompare(b.label),
      );
      setTypes(next);
      onTypesChanged?.(next);
      setNewLabel("");
      addNotification("success", "Créé", "Type d'événement ajouté avec succès");
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? ((err as { response: { data?: { message?: string; errors?: Record<string, string> } } }).response?.data?.errors?.label ??
            (err as { response: { data?: { message?: string } } }).response?.data?.message ??
            "Impossible de créer le type d'événement")
          : "Impossible de créer le type d'événement";
      addNotification("error", "Erreur", msg);
    } finally {
      setCreating(false);
    }
  }

  function startEdit(cat: EvenementSurvenuTypeItem) {
    setEditingId(cat.id);
    setEditingLabel(cat.label);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditingLabel("");
  }

  async function handleSaveEdit(id: number) {
    const label = editingLabel.trim();
    if (!label) return;
    setSavingEdit(id);
    try {
      const updated = await updateEvenementSurvenuType(id, label);
      const next = types
        .map((c) => (c.id === id ? updated : c))
        .sort((a, b) => a.label.localeCompare(b.label));
      setTypes(next);
      onTypesChanged?.(next);
      setEditingId(null);
      setEditingLabel("");
      addNotification("success", "Modifié", "Type d'événement renommé avec succès");
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? ((err as { response: { data?: { message?: string; errors?: Record<string, string> } } }).response?.data?.errors?.label ??
            (err as { response: { data?: { message?: string } } }).response?.data?.message ??
            "Impossible de modifier le type d'événement")
          : "Impossible de modifier le type d'événement";
      addNotification("error", "Erreur", msg);
    } finally {
      setSavingEdit(null);
    }
  }

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    setDeleting(true);
    try {
      await deleteEvenementSurvenuType(deleteTarget.id);
      const next = types.filter((c) => c.id !== deleteTarget.id);
      setTypes(next);
      onTypesChanged?.(next);
      addNotification("success", "Supprimé", "Type d'événement supprimé avec succès");
      setDeleteTarget(null);
    } catch (err: unknown) {
      const msg =
        err && typeof err === "object" && "response" in err
          ? ((err as { response: { data?: { message?: string } } }).response?.data?.message ??
            "Impossible de supprimer le type d'événement")
          : "Impossible de supprimer le type d'événement";
      addNotification("error", "Erreur", msg);
    } finally {
      setDeleting(false);
    }
  }

  if (!open) return null;

  return (
    <>
      <div
        className="fixed inset-0 z-[1000] flex items-center justify-center"
        onClick={onClose}
      >
        <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: -20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: -20 }}
          transition={{ duration: 0.15, ease: "easeOut" }}
          className="relative z-50 w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-2xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-start justify-between mb-4">
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
                <Tag className="h-5 w-5" />
              </div>
              <div>
                <h2 className="text-base font-semibold">
                  Gérer les types d'événement
                </h2>
                <p className="text-xs text-muted-foreground">
                  Types des évènements survenus
                </p>
              </div>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={onClose}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Create new type */}
          <div className="space-y-2 mb-4">
            <Label htmlFor="new-type">Nouveau type</Label>
            <div className="flex gap-2">
              <Input
                id="new-type"
                placeholder="Libellé du type d'événement"
                value={newLabel}
                onChange={(e) => setNewLabel(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleCreate();
                  }
                }}
                maxLength={100}
              />
              <Button
                type="button"
                size="sm"
                className="gap-2"
                onClick={handleCreate}
                disabled={creating || !newLabel.trim()}
              >
                {creating ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                Ajouter
              </Button>
            </div>
          </div>

          {/* List */}
          <div className="max-h-80 space-y-2 overflow-y-auto pr-1">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : types.length === 0 ? (
              <p className="py-8 text-center text-sm text-muted-foreground">
                Aucun type. Ajoutez-en un ci-dessus.
              </p>
            ) : (
              types.map((cat) => {
                const isEditing = editingId === cat.id;
                return (
                  <div
                    key={cat.id}
                    className="flex items-center gap-2 rounded-lg border border-border p-2.5"
                  >
                    {isEditing ? (
                      <>
                        <Input
                          value={editingLabel}
                          onChange={(e) => setEditingLabel(e.target.value)}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              e.preventDefault();
                              handleSaveEdit(cat.id);
                            } else if (e.key === "Escape") {
                              cancelEdit();
                            }
                          }}
                          maxLength={100}
                          autoFocus
                          className="h-8 text-sm"
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-emerald-600"
                          onClick={() => handleSaveEdit(cat.id)}
                          disabled={savingEdit === cat.id || !editingLabel.trim()}
                        >
                          {savingEdit === cat.id ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Check className="h-4 w-4" />
                          )}
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={cancelEdit}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </>
                    ) : (
                      <>
                        <span className="flex-1 text-sm">{cat.label}</span>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => startEdit(cat)}
                        >
                          <Pencil className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-destructive"
                          onClick={() => setDeleteTarget(cat)}
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </>
                    )}
                  </div>
                );
              })
            )}
          </div>

          <div className="mt-6 flex justify-end">
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              Fermer
            </Button>
          </div>
        </motion.div>
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le type"
        message={`Voulez-vous vraiment supprimer « ${deleteTarget?.label} » ? Les évènements existants conservent leur libellé.`}
        confirmLabel={deleting ? "..." : "Supprimer"}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </>
  );
}
