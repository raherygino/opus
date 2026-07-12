import { useState, useEffect, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPersonnelById,
  createPersonnel,
  updatePersonnel,
  getPersonnelAttachments,
  createPersonnelAttachment,
  updatePersonnelAttachmentTitle,
  deletePersonnelAttachment,
  getAttachmentDownloadUrl,
  uploadPersonnelPhoto,
  getPersonnelPhotoUrl,
  generateThumbnail,
  cropFileToSquare,
} from "@/lib/api/personnel";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Save,
  Loader2,
  UserPlus,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Camera,
  Smartphone,
} from "lucide-react";
import type { PersonnelAttachment } from "@/types";
import gradeData from "@/assets/json/grade.json";

const grades = gradeData.grade.map((g) => g.label);

const affectations = [
  "Service Général (SG)",
  "Police Judiciaire (PJ)",
  "Sédentaire",
  "Unité Spéciale",
  "Administration",
];

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

export function PersonnelForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState({
    im: "",
    grade: "",
    lastname: "",
    firstname: "",
    affectation: "",
    phone: "",
    address: "",
  });
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const [photoPadOpen, setPhotoPadOpen] = useState(false);
  const [photoUploading, setPhotoUploading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadPersonnel();
    }
  }, [id]);

  async function loadPersonnel() {
    setLoading(true);
    try {
      const p = await getPersonnelById(Number(id));
      setForm({
        im: p.im,
        grade: p.grade,
        lastname: p.lastname,
        firstname: p.firstname,
        affectation: p.affectation || "",
        phone: p.phone || "",
        address: p.address || "",
      });

      if (p.photo) {
        setPhotoPreview(getPersonnelPhotoUrl(p.id));
      }

      const atts = await getPersonnelAttachments(Number(id));
      setAttachments(
        atts.map((a: PersonnelAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Personnel introuvable");
      navigate("/personnel");
    } finally {
      setLoading(false);
    }
  }

  async function handlePhotoChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) {
      setPhotoUploading(true);
      try {
        const square = await cropFileToSquare(file);
        setPhotoFile(square);
        setPhotoPreview(URL.createObjectURL(square));
      } finally {
        setPhotoUploading(false);
      }
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

  const handlePhotoComplete = useCallback(async (photoData: string) => {
    const [meta, base64] = photoData.split(",");
    const mimeType = meta?.match(/:(.*?);/)?.[1] || "image/jpeg";
    const byteChars = atob(base64);
    const byteNumbers = new Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) {
      byteNumbers[i] = byteChars.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType });
    const file = new File([blob], "photo.jpg", { type: mimeType });
    const thumb = await generateThumbnail(file);

    if (isEdit) {
      setPhotoUploading(true);
      try {
        const updated = await uploadPersonnelPhoto(Number(id), file, thumb);
        setPhotoPreview(getPersonnelPhotoUrl(updated.id) + "?t=" + Date.now());
        addNotification("success", "Photo", "Photo enregistrée avec succès");
        setPhotoFile(null);
        setPhotoPadOpen(false);
      } catch {
        addNotification("error", "Erreur", "Impossible d'enregistrer la photo");
        throw new Error("Failed to save photo");
      } finally {
        setPhotoUploading(false);
      }
    } else {
      setPhotoFile(file);
      setPhotoPreview(URL.createObjectURL(file));
      setPhotoPadOpen(false);
    }
  }, [isEdit, id, addNotification]);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);

    try {
      let personnelId: number;

      if (isEdit) {
        personnelId = Number(id);
        await updatePersonnel(personnelId, form);
      } else {
        const created = await createPersonnel(form);
        personnelId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deletePersonnelAttachment(personnelId, a.id!);
      }

      if (photoFile) {
        const thumb = await generateThumbnail(photoFile);
        const updated = await uploadPersonnelPhoto(personnelId, photoFile, thumb);
        setPhotoPreview(getPersonnelPhotoUrl(updated.id) + "?t=" + Date.now());
        setPhotoFile(null);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deletePersonnelAttachment(personnelId, a.id);
            await createPersonnelAttachment(personnelId, a.title, a.file);
          } else if (a.title) {
            await updatePersonnelAttachmentTitle(personnelId, a.id, a.title);
          }
        } else if (a.file) {
          await createPersonnelAttachment(personnelId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifié" : "Créé",
        isEdit
          ? "Personnel mis à jour avec succès"
          : "Personnel créé avec succès",
      );
      navigate("/personnel");
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
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate("/personnel")}
        >
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier le personnel" : "Nouveau personnel"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de l'agent"
              : "Enregistrer un nouvel agent"}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserPlus className="h-4 w-4" />
            Informations personnelles
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
              <div className="flex items-start gap-6 mb-4">
                <div className="flex flex-col items-center gap-2 shrink-0">
                  <div className="relative h-24 w-24 rounded-full overflow-hidden border-2 border-border bg-muted flex items-center justify-center">
                    {photoUploading ? (
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    ) : photoPreview ? (
                      <img
                        src={photoPreview}
                        alt="Photo"
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <Camera className="h-8 w-8 text-muted-foreground" />
                    )}
                  </div>
                  <Label
                    htmlFor="photo-upload"
                    className="text-xs text-muted-foreground cursor-pointer hover:text-foreground"
                  >
                    {photoPreview ? "Changer" : "Ajouter une photo"}
                  </Label>
                  <input
                    id="photo-upload"
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    className="hidden"
                    onChange={handlePhotoChange}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="gap-1 text-xs h-7"
                    onClick={() => setPhotoPadOpen(true)}
                  >
                    <Smartphone className="h-3 w-3" />
                    Prendre une photo
                  </Button>
                </div>
              </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="im">IM (Indice Matricule) *</Label>
                <Input
                  id="im"
                  value={form.im}
                  onChange={(e) => setForm({ ...form, im: e.target.value })}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="grade">Grade *</Label>
                <Select
                  id="grade"
                  value={form.grade}
                  onChange={(e) =>
                    setForm({ ...form, grade: e.target.value })
                  }
                  options={grades.map((g) => ({ value: g, label: g }))}
                  placeholder="Sélectionner un grade"
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="lastname">Nom *</Label>
                <Input
                  id="lastname"
                  value={form.lastname}
                  onChange={(e) =>
                    setForm({ ...form, lastname: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="firstname">Prénoms *</Label>
                <Input
                  id="firstname"
                  value={form.firstname}
                  onChange={(e) =>
                    setForm({ ...form, firstname: e.target.value })
                  }
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="affectation">Affectation</Label>
                <Select
                  id="affectation"
                  value={form.affectation}
                  onChange={(e) =>
                    setForm({ ...form, affectation: e.target.value })
                  }
                  options={affectations.map((a) => ({ value: a, label: a }))}
                  placeholder="Sélectionner une affectation"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Téléphone</Label>
                <Input
                  id="phone"
                  value={form.phone}
                  onChange={(e) =>
                    setForm({ ...form, phone: e.target.value })
                  }
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="address">Adresse</Label>
              <Input
                id="address"
                value={form.address}
                onChange={(e) =>
                  setForm({ ...form, address: e.target.value })
                }
              />
            </div>

            <div className="flex items-center gap-3 pt-4">
              <Button type="submit" className="gap-2" disabled={saving}>
                {saving ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                {saving ? "Enregistrement..." : "Enregistrer"}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate("/personnel")}
              >
                Annuler
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {attachments.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Aucune pièce jointe
            </p>
          )}

          {attachments.map((att, index) =>
            att._delete ? null : (
              <div
                key={index}
                className="flex items-center gap-3 rounded-lg border border-border p-3"
              >
                <div className="flex-1 space-y-1">
                  <Input
                    placeholder="Titre de la pièce jointe"
                    value={att.title}
                    onChange={(e) =>
                      updateAttachment(index, { title: e.target.value })
                    }
                    className="h-8 text-sm"
                  />
                  {att.id && att.existingFile && id && (
                    <a
                      href={getAttachmentDownloadUrl(Number(id), att.id)}
                      download
                      className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                    >
                      <Download className="h-3 w-3" />
                      {att.existingFile}
                    </a>
                  )}
                </div>
                <div className="flex items-center gap-1">
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
            Ajouter une pièce jointe
          </Button>
        </CardContent>
      </Card>

      <PhotoCaptureDialog
        open={photoPadOpen}
        onClose={() => setPhotoPadOpen(false)}
        onPhotoComplete={handlePhotoComplete}
      />
    </motion.div>
  );
}
