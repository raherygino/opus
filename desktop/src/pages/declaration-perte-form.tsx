import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getDeclarationPerteById,
  createDeclarationPerte,
  updateDeclarationPerte,
  getDeclarationPerteAttachments,
  createDeclarationPerteAttachment,
  updateDeclarationPerteAttachmentTitle,
  deleteDeclarationPerteAttachment,
  getDeclarationPerteAttachmentDownloadUrl,
} from "@/lib/api/declaration-perte";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import {
  ArrowLeft,
  Save,
  Loader2,
  FileWarning,
  PackageSearch,
  MapPin,
  FileBadge,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
} from "lucide-react";
import type { DeclarationPerteAttachment } from "@/types";
import type { DeclarationPertePayload } from "@/lib/api/declaration-perte";

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

const LIST_PATH = "/sedentaire/secretariat/declaration-perte";

export function DeclarationPerteForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<DeclarationPertePayload>({
    date_declaration: todayIso(),
    heure_declaration: nowTime(),
    identite_declarant: "",
    nature_objet: "",
    description_objet: "",
    date_perte: "",
    lieu_perte: "",
    numero_attestation: "",
    nom_agent:
      [user?.firstname, user?.lastname].filter(Boolean).join(" ") ||
      user?.username ||
      "",
  });
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadDeclaration();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadDeclaration() {
    setLoading(true);
    try {
      const d = await getDeclarationPerteById(Number(id));
      setForm({
        date_declaration: d.date_declaration.slice(0, 10),
        heure_declaration: d.heure_declaration.slice(0, 5),
        identite_declarant: d.identite_declarant,
        nature_objet: d.nature_objet,
        description_objet: d.description_objet,
        date_perte: d.date_perte.slice(0, 10),
        lieu_perte: d.lieu_perte,
        numero_attestation: d.numero_attestation,
        nom_agent: d.nom_agent,
      });

      const atts = await getDeclarationPerteAttachments(Number(id));
      setAttachments(
        atts.map((a: DeclarationPerteAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Déclaration de perte introuvable");
      navigate(LIST_PATH);
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
      // Pre-fill the title when the row has none yet.
      ...(current && !current.title.trim() ? { title: file.name.replace(/\.[^.]+$/, "") } : {}),
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.date_declaration || !form.heure_declaration) {
      addNotification("error", "Erreur", "La date et l'heure de déclaration sont requises");
      return;
    }
    if (!form.identite_declarant.trim() || !form.nom_agent.trim()) {
      addNotification("error", "Erreur", "L'identité du déclarant et le nom de l'agent sont requis");
      return;
    }
    if (!form.nature_objet.trim() || !form.description_objet.trim()) {
      addNotification("error", "Erreur", "La nature et la description de l'objet sont requises");
      return;
    }
    if (!form.date_perte || !form.lieu_perte.trim()) {
      addNotification("error", "Erreur", "La date et le lieu de perte sont requis");
      return;
    }
    if (!form.numero_attestation.trim()) {
      addNotification("error", "Erreur", "Le numéro d'attestation est requis");
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
      let declarationId: number;

      if (isEdit) {
        declarationId = Number(id);
        await updateDeclarationPerte(declarationId, form);
      } else {
        const created = await createDeclarationPerte(form);
        declarationId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteDeclarationPerteAttachment(declarationId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteDeclarationPerteAttachment(declarationId, a.id);
            await createDeclarationPerteAttachment(declarationId, a.title, a.file);
          } else if (a.title) {
            await updateDeclarationPerteAttachmentTitle(declarationId, a.id, a.title);
          }
        } else if (a.file) {
          await createDeclarationPerteAttachment(declarationId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Déclaration de perte mise à jour avec succès"
          : "Déclaration de perte enregistrée avec succès",
      );
      navigate(LIST_PATH);
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
        <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier la déclaration de perte" : "Nouvelle déclaration de perte"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de la déclaration de perte"
              : "Enregistrer une déclaration de perte"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <FileWarning className="h-4 w-4" />
              Déclaration
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_declaration">Date de déclaration *</Label>
                <Input
                  id="date_declaration"
                  type="date"
                  value={form.date_declaration}
                  onChange={(e) =>
                    setForm({ ...form, date_declaration: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure_declaration">Heure de déclaration *</Label>
                <Input
                  id="heure_declaration"
                  type="time"
                  value={form.heure_declaration}
                  onChange={(e) =>
                    setForm({ ...form, heure_declaration: e.target.value })
                  }
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="identite_declarant">Identité du déclarant *</Label>
                <Input
                  id="identite_declarant"
                  value={form.identite_declarant}
                  onChange={(e) =>
                    setForm({ ...form, identite_declarant: e.target.value })
                  }
                  placeholder="Nom et prénom(s) du déclarant"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="nom_agent">Nom de l'agent *</Label>
                <Input
                  id="nom_agent"
                  value={form.nom_agent}
                  onChange={(e) => setForm({ ...form, nom_agent: e.target.value })}
                  placeholder="Agent ayant enregistré la déclaration"
                  required
                />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <PackageSearch className="h-4 w-4" />
              Objet perdu
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="nature_objet">Nature de l'objet *</Label>
              <Input
                id="nature_objet"
                value={form.nature_objet}
                onChange={(e) => setForm({ ...form, nature_objet: e.target.value })}
                placeholder="Ex. Carte d'identité, passeport, téléphone..."
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description_objet">Description de l'objet *</Label>
              <textarea
                id="description_objet"
                value={form.description_objet}
                onChange={(e) =>
                  setForm({ ...form, description_objet: e.target.value })
                }
                rows={4}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="Décrire l'objet perdu en détail..."
                required
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <MapPin className="h-4 w-4" />
              Perte présumée
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_perte">Date de perte *</Label>
                <Input
                  id="date_perte"
                  type="date"
                  value={form.date_perte}
                  onChange={(e) => setForm({ ...form, date_perte: e.target.value })}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lieu_perte">Lieu de perte *</Label>
                <Input
                  id="lieu_perte"
                  value={form.lieu_perte}
                  onChange={(e) => setForm({ ...form, lieu_perte: e.target.value })}
                  placeholder="Lieu présumé de la perte"
                  required
                />
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <FileBadge className="h-4 w-4" />
              Attestation
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="numero_attestation">Numéro d'attestation *</Label>
              <Input
                id="numero_attestation"
                value={form.numero_attestation}
                onChange={(e) =>
                  setForm({ ...form, numero_attestation: e.target.value })
                }
                placeholder="Numéro unique de l'attestation de perte"
                required
              />
            </div>
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
          <Button type="button" variant="outline" onClick={() => navigate(LIST_PATH)}>
            Annuler
          </Button>
        </div>
      </form>

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
                      href={getDeclarationPerteAttachmentDownloadUrl(Number(id), att.id)}
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
            ? getDeclarationPerteAttachmentDownloadUrl(Number(id), viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
