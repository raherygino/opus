import { useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPersonneRechercheeById,
  createPersonneRecherchee,
  updatePersonneRecherchee,
  getPersonneRechercheePhotos,
  createPersonneRechercheePhoto,
  updatePersonneRechercheePhotoCaption,
  deletePersonneRechercheePhoto,
  getPersonneRechercheePhotoDownloadUrl,
  validatePersonneRechercheeForm,
  dataUrlToFile,
} from "@/lib/api/personne-recherchee";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Save,
  Loader2,
  Camera,
  Trash2,
  Plus,
  ImageIcon,
} from "lucide-react";
import type { PersonneRecherchee, PersonneRechercheeInput } from "@/types";

interface PhotoItem {
  id?: number;
  caption: string;
  file?: File;
  previewUrl?: string;
  existingUrl?: string;
  captureSource?: "CAMERA" | "GALLERY" | null;
  _delete?: boolean;
}

export function PersonneRechercheeForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<PersonneRechercheeInput>({
    nom: "",
    adresse: "",
    motif: "",
  });
  const [photos, setPhotos] = useState<PhotoItem[]>([]);
  const [photoPadOpen, setPhotoPadOpen] = useState(false);
  const [viewerUrl, setViewerUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isEdit && id) {
      loadEntry(Number(id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: PersonneRecherchee = await getPersonneRechercheeById(entryId);
      setForm({
        nom: e.nom,
        adresse: e.adresse ?? "",
        motif: e.motif,
      });
      try {
        const list = await getPersonneRechercheePhotos(entryId);
        setPhotos(
          list.map((p) => ({
            id: p.id,
            caption: p.caption ?? "",
            existingUrl: getPersonneRechercheePhotoDownloadUrl(entryId, p.id),
            captureSource: p.capture_source,
          })),
        );
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Impossible de charger la personne recherchée");
      navigate("/pj/personne-recherchee");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof PersonneRechercheeInput>(key: K, value: PersonneRechercheeInput[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  // ─── Photo handling ──────────────────────────────────────────────

  function addGalleryPhoto(file: File) {
    setPhotos((p) => [
      ...p,
      {
        caption: "",
        file,
        previewUrl: URL.createObjectURL(file),
        captureSource: "GALLERY",
      },
    ]);
  }

  function handlePhotoComplete(photoData: string) {
    const file = dataUrlToFile(photoData, `photo_${Date.now()}.jpg`);
    setPhotos((p) => [
      ...p,
      {
        caption: "",
        file,
        previewUrl: URL.createObjectURL(file),
        captureSource: "CAMERA",
      },
    ]);
    setPhotoPadOpen(false);
  }

  function updateCaption(index: number, caption: string) {
    setPhotos(photos.map((p, i) => (i === index ? { ...p, caption } : p)));
  }

  function removePhoto(index: number) {
    setPhotos(
      photos.map((p, i) => {
        if (i !== index) return p;
        if (p.id) return { ...p, _delete: true };
        return p;
      }),
    );
  }

  // ─── Save ─────────────────────────────────────────────────────────

  async function handleSave() {
    const validationErrors = validatePersonneRechercheeForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: PersonneRechercheeInput = {
        nom: form.nom.trim(),
        adresse: form.adresse?.trim() || null,
        motif: form.motif.trim(),
      };
      const saved: PersonneRecherchee = isEdit && id
        ? await updatePersonneRecherchee(Number(id), payload)
        : await createPersonneRecherchee(payload);
      await handlePhotos(saved.id);
      addNotification("success", "Enregistré", "Personne recherchée enregistrée avec succès");
      navigate(`/pj/personne-recherchee/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handlePhotos(savedId: number) {
    for (const p of photos) {
      if (p._delete && p.id) {
        await deletePersonneRechercheePhoto(savedId, p.id);
      } else if (!p._delete && p.file) {
        await createPersonneRechercheePhoto(
          savedId,
          p.file,
          p.caption.trim() || null,
          p.captureSource ?? null,
        );
      } else if (!p._delete && p.id) {
        await updatePersonneRechercheePhotoCaption(savedId, p.id, p.caption.trim() || null);
      }
    }
  }

  const visiblePhotos = photos.filter((p) => !p._delete);

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/personne-recherchee")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier la personne recherchée" : "Nouvelle personne recherchée"}
              </h1>
              <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
            </div>
          </div>
          <Button onClick={handleSave} disabled={saving || loading} className="gap-2">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
        </div>
      </div>

      {loading && (
        <div className="flex justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && (
        <div className="space-y-6 pb-6">
          {/* Identité */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Identité</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nom">Nom *</Label>
                <Input
                  id="nom"
                  value={form.nom}
                  onChange={(e) => update("nom", e.target.value)}
                  required
                />
                {errors.nom && <p className="text-sm text-destructive">{errors.nom}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="adresse">Adresse</Label>
                <Input
                  id="adresse"
                  value={form.adresse ?? ""}
                  onChange={(e) => update("adresse", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Motif */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Motif</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="motif">Motif de la recherche *</Label>
                <textarea
                  id="motif"
                  className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.motif}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("motif", e.target.value)}
                  required
                />
                {errors.motif && <p className="text-sm text-destructive">{errors.motif}</p>}
              </div>
            </CardContent>
          </Card>

          {/* Photos — dedicated multi-image (NOT generic attachments) */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">
                  Photos ({visiblePhotos.length})
                </CardTitle>
                <div className="flex gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setPhotoPadOpen(true)}
                    className="gap-2"
                  >
                    <Camera className="h-4 w-4" />
                    Prendre une photo
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => fileInputRef.current?.click()}
                    className="gap-2"
                  >
                    <Plus className="h-4 w-4" />
                    Choisir un fichier
                  </Button>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    multiple
                    className="hidden"
                    onChange={(e) => {
                      const files = Array.from(e.target.files ?? []);
                      files.forEach(addGalleryPhoto);
                      e.target.value = "";
                    }}
                  />
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {visiblePhotos.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  Aucune photo. Utilisez la caméra ou sélectionnez un fichier image.
                </p>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                  {photos.map((p, i) =>
                    p._delete ? null : (
                      <div key={i} className="space-y-2">
                        <div
                          className="relative aspect-square rounded-lg border overflow-hidden bg-muted cursor-pointer"
                          onClick={() =>
                            setViewerUrl(p.previewUrl ?? p.existingUrl ?? null)
                          }
                        >
                          {p.previewUrl || p.existingUrl ? (
                            <img
                              src={p.previewUrl ?? p.existingUrl}
                              alt={p.caption || "Photo"}
                              className="h-full w-full object-cover"
                            />
                          ) : (
                            <div className="flex h-full w-full items-center justify-center">
                              <ImageIcon className="h-8 w-8 text-muted-foreground" />
                            </div>
                          )}
                          {p.captureSource && (
                            <span className="absolute top-1 left-1 text-[10px] px-1.5 py-0.5 rounded bg-black/60 text-white">
                              {p.captureSource === "CAMERA" ? "Caméra" : "Galerie"}
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-1">
                          <Input
                            placeholder="Légende"
                            value={p.caption}
                            onChange={(e) => updateCaption(i, e.target.value)}
                            className="h-8 text-xs"
                          />
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive shrink-0"
                            onClick={() => removePhoto(i)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </div>
                    ),
                  )}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <PhotoCaptureDialog
        open={photoPadOpen}
        onClose={() => setPhotoPadOpen(false)}
        onPhotoComplete={handlePhotoComplete}
        squareCrop={false}
      />

      <ImageViewerDialog
        src={viewerUrl ?? ""}
        title="Photo"
        open={viewerUrl !== null}
        onClose={() => setViewerUrl(null)}
      />
    </motion.div>
  );
}
