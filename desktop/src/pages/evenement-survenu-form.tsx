import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getEvenementSurvenuById,
  createEvenementSurvenu,
  updateEvenementSurvenu,
  getEvenementSurvenuAttachments,
  createEvenementSurvenuAttachment,
  updateEvenementSurvenuAttachmentTitle,
  deleteEvenementSurvenuAttachment,
  getEvenementSurvenuAttachmentDownloadUrl,
  getEvenementSurvenuTypes,
} from "@/lib/api/evenement-survenu";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import { EvenementTypeDialog } from "@/components/evenement-survenu/evenement-type-dialog";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  Save,
  ArrowLeft,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
  Settings2,
} from "lucide-react";
import type {
  EvenementSurvenuAttachment,
  EvenementSurvenuInput,
  EvenementSurvenuTypeItem,
} from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

export function EvenementSurvenuForm() {
  const { id } = useParams();
  const editId = id ? Number(id) : 0;
  const isEdit = editId > 0;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  const [dateEvenement, setDateEvenement] = useState("");
  const [heureEvenement, setHeureEvenement] = useState("");
  const [typeEvenement, setTypeEvenement] = useState("");
  const [types, setTypes] = useState<EvenementSurvenuTypeItem[]>([]);
  const [showTypeDialog, setShowTypeDialog] = useState(false);
  const [lieuExact, setLieuExact] = useState("");
  const [auteursPresumes, setAuteursPresumes] = useState("");
  const [victimes, setVictimes] = useState("");
  const [temoins, setTemoins] = useState("");
  const [mesuresPrises, setMesuresPrises] = useState("");
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);

  useEffect(() => {
    loadTypes();
    if (!isEdit) return;
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadTypes() {
    try {
      const data = await getEvenementSurvenuTypes();
      setTypes(data);
      // Default the form's type to the first available if it's still empty.
      setTypeEvenement((prev) =>
        prev === "" && data.length > 0 ? data[0].label : prev,
      );
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types d'événement");
    }
  }

  async function load() {
    try {
      const data = await getEvenementSurvenuById(editId);
      setDateEvenement(data.date_evenement);
      setHeureEvenement(data.heure_evenement);
      setTypeEvenement(data.type_evenement);
      setLieuExact(data.lieu_exact);
      setAuteursPresumes(data.auteurs_presumes || "");
      setVictimes(data.victimes || "");
      setTemoins(data.temoins || "");
      setMesuresPrises(data.mesures_prises || "");

      const atts = await getEvenementSurvenuAttachments(editId);
      setAttachments(
        atts.map((a: EvenementSurvenuAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Impossible de charger l'évènement");
      navigate("/sg/evenements-survenus");
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
  // attachment's file, exactly like the other features' attachment flow.
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
    if (!dateEvenement || !heureEvenement || !typeEvenement || !lieuExact.trim()) {
      addNotification(
        "error",
        "Validation",
        "Date, heure, type d'événement et lieu exact sont requis",
      );
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
    const payload: EvenementSurvenuInput = {
      date_evenement: dateEvenement,
      heure_evenement: heureEvenement,
      type_evenement: typeEvenement,
      lieu_exact: lieuExact,
      auteurs_presumes: auteursPresumes || null,
      victimes: victimes || null,
      temoins: temoins || null,
      mesures_prises: mesuresPrises || null,
    };
    try {
      let evenementId: number;
      if (isEdit) {
        evenementId = editId;
        await updateEvenementSurvenu(evenementId, payload);
      } else {
        const created = await createEvenementSurvenu(payload);
        evenementId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteEvenementSurvenuAttachment(evenementId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteEvenementSurvenuAttachment(evenementId, a.id);
            await createEvenementSurvenuAttachment(evenementId, a.title, a.file);
          } else if (a.title) {
            await updateEvenementSurvenuAttachmentTitle(evenementId, a.id, a.title);
          }
        } else if (a.file) {
          await createEvenementSurvenuAttachment(evenementId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        "Enregistré",
        isEdit ? "Évènement modifié avec succès" : "Évènement créé avec succès",
      );
      navigate("/sg/evenements-survenus");
    } catch {
      addNotification("error", "Erreur", "Impossible d'enregistrer l'évènement");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Chargement...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/sg/evenements-survenus")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isEdit ? "Modifier l'évènement" : "Nouvel évènement"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Évènements survenus sur la voie publique
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="date">Date *</Label>
                <Input
                  id="date"
                  type="date"
                  value={dateEvenement}
                  onChange={(e) => setDateEvenement(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure">Heure *</Label>
                <Input
                  id="heure"
                  type="time"
                  value={heureEvenement}
                  onChange={(e) => setHeureEvenement(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="type">Type d'événement *</Label>
                  <button
                    type="button"
                    className="text-xs text-muted-foreground hover:text-foreground inline-flex items-center gap-1"
                    onClick={() => setShowTypeDialog(true)}
                  >
                    <Settings2 className="h-3 w-3" />
                    Gérer
                  </button>
                </div>
                <Select
                  id="type"
                  value={typeEvenement}
                  onChange={(e) => setTypeEvenement(e.target.value)}
                  options={types.map((t) => ({ value: t.label, label: t.label }))}
                  placeholder="Sélectionner un type"
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="lieu">Lieu exact *</Label>
              <Input
                id="lieu"
                value={lieuExact}
                onChange={(e) => setLieuExact(e.target.value)}
                required
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Identités des parties impliquées</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="auteurs">Auteur(s) présumé(s)</Label>
              <Textarea
                id="auteurs"
                value={auteursPresumes}
                onChange={(e) => setAuteursPresumes(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="victimes">Victime(s)</Label>
              <Textarea
                id="victimes"
                value={victimes}
                onChange={(e) => setVictimes(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="temoins">Témoin(s)</Label>
              <Textarea
                id="temoins"
                value={temoins}
                onChange={(e) => setTemoins(e.target.value)}
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Mesures prises</CardTitle>
          </CardHeader>
          <CardContent>
            <Textarea
              id="mesures"
              value={mesuresPrises}
              onChange={(e) => setMesuresPrises(e.target.value)}
              rows={4}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
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
                    {att.id && att.existingFile && editId > 0 && (
                      <a
                        href={getEvenementSurvenuAttachmentDownloadUrl(editId, att.id)}
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
                    {att.id && att.existingFile && editId > 0 && isImageFile(null, att.existingFile) && (
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

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={() => navigate("/sg/evenements-survenus")}>
            Annuler
          </Button>
          <Button type="submit" disabled={saving}>
            <Save className="h-4 w-4 mr-2" />
            {saving ? "Enregistrement..." : "Enregistrer"}
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
          viewerTarget && editId > 0
            ? getEvenementSurvenuAttachmentDownloadUrl(editId, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />

      <EvenementTypeDialog
        open={showTypeDialog}
        onClose={() => setShowTypeDialog(false)}
        onTypesChanged={(updated) => {
          setTypes(updated);
          // If the currently selected type was renamed, keep it selected.
          // If it was deleted, reset to the first available.
          setTypeEvenement((prev) => {
            const stillExists = updated.some((t) => t.label === prev);
            if (stillExists) return prev;
            return updated.length > 0 ? updated[0].label : "";
          });
        }}
      />
    </div>
  );
}
