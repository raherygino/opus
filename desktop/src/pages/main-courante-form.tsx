import { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getMainCouranteById,
  createMainCourante,
  updateMainCourante,
  getMainCouranteAttachments,
  createMainCouranteAttachment,
  updateMainCouranteAttachmentTitle,
  deleteMainCouranteAttachment,
  getMainCouranteAttachmentDownloadUrl,
  getMainCouranteCategories,
} from "@/lib/api/main-courante";
import { MainCouranteCategorieDialog } from "@/components/main-courante/main-courante-categorie-dialog";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import {
  ArrowLeft,
  Save,
  Loader2,
  BookOpenText,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
  Settings2,
} from "lucide-react";
import type { MainCourante, MainCouranteAttachment, MainCouranteCategorieItem, MainCouranteOrigine } from "@/types";
import type { MainCourantePayload } from "@/lib/api/main-courante";
import { listPathForOrigine } from "@/pages/main-courante-list";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

function todayIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

function nowTime(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

/** Resolve the origine (Secretariat / Poste) from the current URL path. */
function useOrigine(): MainCouranteOrigine {
  const location = useLocation();
  return location.pathname.includes("/poste/") ? "Poste" : "Secretariat";
}

export function MainCouranteForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const origine = useOrigine();
  const listPath = listPathForOrigine(origine);
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<MainCourantePayload>({
    date_evenement: todayIso(),
    heure_evenement: nowTime(),
    categorie: "",
    description: "",
    origine,
  });
  const [agent, setAgent] = useState<string | null>(null);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [categories, setCategories] = useState<MainCouranteCategorieItem[]>([]);
  const [showCategorieDialog, setShowCategorieDialog] = useState(false);

  useEffect(() => {
    loadCategories();
    if (isEdit) {
      loadEntry();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadCategories() {
    try {
      const data = await getMainCouranteCategories();
      setCategories(data);
      // Default the form's categorie to the first available if it's still empty.
      setForm((prev) =>
        prev.categorie === "" && data.length > 0
          ? { ...prev, categorie: data[0].label }
          : prev,
      );
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les catégories");
    }
  }

  async function loadEntry() {
    setLoading(true);
    try {
      const e = await getMainCouranteById(Number(id));
      setForm({
        date_evenement: e.date_evenement.slice(0, 10),
        heure_evenement: e.heure_evenement.slice(0, 5),
        categorie: e.categorie,
        description: e.description,
        origine: e.origine,
      });
      setAgent(
        [e.agent_prenoms, e.agent_nom].filter(Boolean).join(" ") ||
          e.agent_username ||
          null,
      );

      const atts = await getMainCouranteAttachments(Number(id));
      setAttachments(
        atts.map((a: MainCouranteAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Main courante introuvable");
      navigate(listPath);
    } finally {
      setLoading(false);
    }
  }

  function addAttachment() {
    setAttachments((prev) => [...prev, { title: "", file: undefined }]);
  }

  function removeAttachment(index: number) {
    setAttachments((prev) => {
      const updated = [...prev];
      if (updated[index].id) {
        updated[index] = { ...updated[index], _delete: true };
      } else {
        updated.splice(index, 1);
      }
      return updated;
    });
  }

  function updateAttachment(index: number, data: Partial<AttachmentItem>) {
    setAttachments((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], ...data };
      return updated;
    });
  }

  // Phone photo capture (QR pairing) → the captured photo becomes the
  // attachment's file, exactly like the personnel photo flow.
  async function handleAttachmentPhotoComplete(photoData: string) {
    const index = photoPadIndex;
    setPhotoPadIndex(null);
    if (index === null) return;

    const [meta, base64] = photoData.split(",");
    const mimeType = meta?.match(/:(.*?);/)?.[1] || "image/jpeg";
    const byteChars = atob(base64);
    const byteNumbers = new Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) {
      byteNumbers[i] = byteChars.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType });
    const extension = mimeType === "image/png" ? "png" : "jpg";
    const file = new File([blob], `photo.${extension}`, { type: mimeType });

    const current = attachments[index];
    updateAttachment(index, {
      file,
      ...(current && !current.title.trim() ? { title: file.name.replace(/\.[^.]+$/, "") } : {}),
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.date_evenement || !form.heure_evenement) {
      addNotification("error", "Erreur", "La période et l'heure précise sont requises");
      return;
    }
    if (!form.categorie) {
      addNotification("error", "Erreur", "La catégorie de l'événement est requise");
      return;
    }
    if (!form.description.trim()) {
      addNotification("error", "Erreur", "La description des faits est requise");
      return;
    }
    const incompleteAttachment = attachments.some(
      (a) => !a._delete && !a.id && (!a.title.trim() || !a.file),
    );
    if (incompleteAttachment) {
      addNotification("error", "Erreur", "Chaque pièce jointe doit avoir un titre et un fichier");
      return;
    }

    setSaving(true);

    try {
      let entryId: number;

      if (isEdit) {
        entryId = Number(id);
        await updateMainCourante(entryId, form);
      } else {
        const created = await createMainCourante(form);
        entryId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteMainCouranteAttachment(entryId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteMainCouranteAttachment(entryId, a.id);
            await createMainCouranteAttachment(entryId, a.title, a.file);
          } else if (a.title) {
            await updateMainCouranteAttachmentTitle(entryId, a.id, a.title);
          }
        } else if (a.file) {
          await createMainCouranteAttachment(entryId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Main courante mise à jour avec succès"
          : "Main courante enregistrée avec succès",
      );
      navigate(listPath);
    } catch (err: unknown) {
      let msg = "Erreur lors de l'enregistrement";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          const fieldErrors = Object.entries(resp.data.errors)
            .map(([field, error]) => `${field}: ${error}`)
            .join(", ");
          msg = fieldErrors;
        } else if (resp?.data?.message) {
          msg = resp.data.message;
        }
      }
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
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate(listPath)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier l'entrée" : "Nouvelle entrée"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de la main courante"
              : "Enregistrer un événement dans la main courante"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <BookOpenText className="h-4 w-4" />
              Informations de l'événement
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_evenement">Période (Date) *</Label>
                <Input
                  id="date_evenement"
                  type="date"
                  value={form.date_evenement}
                  onChange={(e) =>
                    setForm({ ...form, date_evenement: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure_evenement">Heure précise *</Label>
                <Input
                  id="heure_evenement"
                  type="time"
                  value={form.heure_evenement}
                  onChange={(e) =>
                    setForm({ ...form, heure_evenement: e.target.value })
                  }
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="categorie">Catégorie de l'événement *</Label>
                <button
                  type="button"
                  className="text-xs text-muted-foreground hover:text-foreground inline-flex items-center gap-1"
                  onClick={() => setShowCategorieDialog(true)}
                >
                  <Settings2 className="h-3 w-3" />
                  Gérer
                </button>
              </div>
              <Select
                id="categorie"
                value={form.categorie}
                onChange={(e) =>
                  setForm({ ...form, categorie: e.target.value as MainCourante["categorie"] })
                }
                options={categories.map((c) => ({ value: c.label, label: c.label }))}
                placeholder="Sélectionner la catégorie"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description des faits *</Label>
              <textarea
                id="description"
                className="flex min-h-[120px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-primary disabled:cursor-not-allowed disabled:opacity-50"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Décrivez les faits survenus..."
                required
              />
            </div>

            {isEdit && agent && (
              <div className="space-y-2">
                <Label>Agent</Label>
                <Input value={agent} disabled />
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              Fichiers joints
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {attachments.filter((a) => !a._delete).length === 0 && (
              <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
            )}

            {attachments.map((att, index) =>
              att._delete ? null : (
                <div
                  key={att.id || `attachment-${index}`}
                  className="flex items-center gap-3 rounded-lg border border-border p-3"
                >
                  <div className="flex-1 space-y-1">
                    <Input
                      placeholder="Titre du fichier joint"
                      value={att.title}
                      onChange={(e) => updateAttachment(index, { title: e.target.value })}
                      className="h-8 text-sm"
                    />
                    {att.id && att.existingFile && id && (
                      <a
                        href={getMainCouranteAttachmentDownloadUrl(Number(id), att.id)}
                        download
                        className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                      >
                        <Download className="h-3 w-3" />
                        {att.existingFile}
                      </a>
                    )}
                    {att.file && (
                      <p className="text-xs text-muted-foreground flex items-center gap-1">
                        <Paperclip className="h-3 w-3" />
                        {att.file.name}
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-1">
                    {att.id && att.existingFile && id && isImageFile(null, att.existingFile) && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        title="Aperçu"
                        onClick={() => setViewerTarget({ id: att.id!, title: att.title || att.existingFile! })}
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </Button>
                    )}
                    <Input
                      type="file"
                      className="w-40 h-8 text-xs"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) updateAttachment(index, { file });
                      }}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      title="Prendre une photo avec le téléphone"
                      onClick={() => setPhotoPadIndex(index)}
                    >
                      <Smartphone className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive"
                      onClick={() => removeAttachment(index)}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              ),
            )}

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="gap-2"
              onClick={addAttachment}
            >
              <Plus className="h-4 w-4" />
              Ajouter un fichier joint
            </Button>
          </CardContent>
        </Card>

        <div className="flex items-center gap-3">
          <Button type="submit" className="gap-2" disabled={saving}>
            {saving ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {saving ? "Enregistrement..." : "Enregistrer"}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate(listPath)}>
            Annuler
          </Button>
        </div>
      </form>

      <PhotoCaptureDialog
        open={photoPadIndex !== null}
        onClose={() => setPhotoPadIndex(null)}
        onPhotoComplete={handleAttachmentPhotoComplete}
        squareCrop={false}
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget && id
            ? getMainCouranteAttachmentDownloadUrl(Number(id), viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />

      <MainCouranteCategorieDialog
        open={showCategorieDialog}
        onClose={() => setShowCategorieDialog(false)}
        onCategoriesChanged={(updated) => {
          setCategories(updated);
          // If the currently selected categorie was renamed, keep it selected.
          // If it was deleted, reset to the first available.
          setForm((prev) => {
            const stillExists = updated.some((c) => c.label === prev.categorie);
            if (stillExists) return prev;
            return {
              ...prev,
              categorie: updated.length > 0 ? updated[0].label : "",
            };
          });
        }}
      />
    </motion.div>
  );
}
