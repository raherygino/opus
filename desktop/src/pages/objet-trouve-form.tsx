import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getObjetTrouveById,
  createObjetTrouve,
  updateObjetTrouve,
  getObjetTrouveAttachments,
  createObjetTrouveAttachment,
  updateObjetTrouveAttachmentTitle,
  deleteObjetTrouveAttachment,
  getObjetTrouveAttachmentDownloadUrl,
  validateObjetTrouveForm,
  isObjetImageAttachment,
} from "@/lib/api/objet";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Save,
  Loader2,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Eye,
} from "lucide-react";
import { OBJET_TROUVE_MOTIF_LABELS, type ObjetTrouve, type ObjetTrouveInput } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

export function ObjetTrouveForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<ObjetTrouveInput>({
    affaire: "",
    motif_decouverte: "",
    restitution: false,
  });
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isEdit && id) {
      loadEntry(Number(id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: ObjetTrouve = await getObjetTrouveById(entryId);
      setForm({
        affaire: e.affaire,
        motif_decouverte: e.motif_decouverte,
        restitution: e.restitution === 1,
      });
      try {
        const atts = await getObjetTrouveAttachments(entryId);
        setAttachments(
          atts.map((a) => ({
            id: a.id,
            title: a.title,
            existingFile: a.original_filename,
          })),
        );
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Impossible de charger l'objet trouvé");
      navigate("/pj/objets");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof ObjetTrouveInput>(key: K, value: ObjetTrouveInput[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function addAttachment() {
    setAttachments([...attachments, { title: "" }]);
  }

  function updateAttachmentTitle(index: number, title: string) {
    setAttachments(attachments.map((a, i) => (i === index ? { ...a, title } : a)));
  }

  function setAttachmentFile(index: number, file: File) {
    setAttachments(attachments.map((a, i) => (i === index ? { ...a, file } : a)));
  }

  function removeAttachment(index: number) {
    setAttachments(
      attachments.map((a, i) => {
        if (i !== index) return a;
        if (a.id) return { ...a, _delete: true };
        return a;
      }),
    );
  }

  async function handleSave() {
    const validationErrors = validateObjetTrouveForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: ObjetTrouveInput = {
        affaire: form.affaire.trim(),
        motif_decouverte: form.motif_decouverte,
        restitution: form.restitution,
      };
      const saved: ObjetTrouve = isEdit && id
        ? await updateObjetTrouve(Number(id), payload)
        : await createObjetTrouve(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Objet trouvé enregistré avec succès");
      navigate(`/pj/objets/trouve/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteObjetTrouveAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteObjetTrouveAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createObjetTrouveAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateObjetTrouveAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createObjetTrouveAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/objets")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier l'objet trouvé" : "Nouvel objet trouvé"}
              </h1>
              <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — Objet trouvé</p>
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
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Objet trouvé</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="affaire">Affaire *</Label>
                <Input
                  id="affaire"
                  value={form.affaire}
                  onChange={(e) => update("affaire", e.target.value)}
                  required
                />
                {errors.affaire && <p className="text-sm text-destructive">{errors.affaire}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="motif_decouverte">Motif de découverte *</Label>
                <Select
                  id="motif_decouverte"
                  value={form.motif_decouverte}
                  onChange={(e) => update("motif_decouverte", e.target.value as ObjetTrouveInput["motif_decouverte"])}
                  options={Object.entries(OBJET_TROUVE_MOTIF_LABELS).map(([key, label]) => ({ value: key, label }))}
                  placeholder="Sélectionner…"
                />
                {errors.motif_decouverte && <p className="text-sm text-destructive">{errors.motif_decouverte}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="restitution">Restitution</Label>
                <Select
                  id="restitution"
                  value={form.restitution ? "1" : "0"}
                  onChange={(e) => update("restitution", e.target.value === "1")}
                  options={[
                    { value: "0", label: "Non restitué" },
                    { value: "1", label: "Restitué" },
                  ]}
                />
              </div>
            </CardContent>
          </Card>

          {/* Pièces jointes */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">Pièces jointes</CardTitle>
                <Button type="button" variant="outline" size="sm" onClick={addAttachment} className="gap-2">
                  <Plus className="h-4 w-4" />
                  Ajouter
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {attachments.filter((a) => !a._delete).length === 0 && (
                <p className="text-sm text-muted-foreground">Aucune pièce jointe.</p>
              )}
              {attachments.map((a, i) =>
                a._delete ? null : (
                  <div key={i} className="flex items-start gap-3 rounded-lg border p-3">
                    <div className="flex-1 space-y-2">
                      <Input
                        placeholder="Titre"
                        value={a.title}
                        onChange={(e) => updateAttachmentTitle(i, e.target.value)}
                      />
                      <div className="flex items-center gap-2">
                        <label className="flex-1 cursor-pointer">
                          <span className="flex items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm text-muted-foreground hover:bg-accent">
                            <Paperclip className="h-4 w-4" />
                            {a.file?.name ?? a.existingFile ?? "Choisir un fichier"}
                          </span>
                          <input
                            type="file"
                            className="hidden"
                            onChange={(e) => {
                              const f = e.target.files?.[0];
                              if (f) setAttachmentFile(i, f);
                            }}
                          />
                        </label>
                        {a.existingFile && a.id && (
                          <>
                            {isObjetImageAttachment(null, a.existingFile) && (
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => setViewerTarget({ id: a.id!, title: a.title })}
                              >
                                <Eye className="h-4 w-4" />
                              </Button>
                            )}
                            <a
                              href={getObjetTrouveAttachmentDownloadUrl(Number(id), a.id)}
                              target="_blank"
                              rel="noreferrer"
                            >
                              <Button type="button" variant="ghost" size="icon">
                                <Download className="h-4 w-4" />
                              </Button>
                            </a>
                          </>
                        )}
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="text-destructive"
                      onClick={() => removeAttachment(i)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ),
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <ImageViewerDialog
        src={viewerTarget ? getObjetTrouveAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
