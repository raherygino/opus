import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRenseignementPjById,
  createRenseignementPj,
  updateRenseignementPj,
  getRenseignementPjAttachments,
  createRenseignementPjAttachment,
  updateRenseignementPjAttachmentTitle,
  deleteRenseignementPjAttachment,
  getRenseignementPjAttachmentDownloadUrl,
  validateRenseignementPjForm,
  isRenseignementPjImageAttachment,
} from "@/lib/api/renseignement-pj";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
import type { RenseignementPj, RenseignementPjInput } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

export function RenseignementPjForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<RenseignementPjInput>({
    nature_infraction: "",
    date_lieu_faits: "",
    circonstances: "",
    prejudices: "",
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
      const e: RenseignementPj = await getRenseignementPjById(entryId);
      setForm({
        nature_infraction: e.nature_infraction,
        date_lieu_faits: e.date_lieu_faits ?? "",
        circonstances: e.circonstances ?? "",
        prejudices: e.prejudices ?? "",
      });
      try {
        const atts = await getRenseignementPjAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger le renseignement");
      navigate("/pj/renseignement");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof RenseignementPjInput>(key: K, value: RenseignementPjInput[K]) {
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
    const validationErrors = validateRenseignementPjForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: RenseignementPjInput = {
        nature_infraction: form.nature_infraction.trim(),
        date_lieu_faits: form.date_lieu_faits?.trim() || null,
        circonstances: form.circonstances?.trim() || null,
        prejudices: form.prejudices?.trim() || null,
      };
      const saved: RenseignementPj = isEdit && id
        ? await updateRenseignementPj(Number(id), payload)
        : await createRenseignementPj(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Renseignement enregistré avec succès");
      navigate(`/pj/renseignement/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer le renseignement";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteRenseignementPjAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteRenseignementPjAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createRenseignementPjAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateRenseignementPjAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createRenseignementPjAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/renseignement")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier le renseignement" : "Nouveau renseignement"}
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
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Renseignement</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nature_infraction">Nature d'infraction *</Label>
                <Input
                  id="nature_infraction"
                  value={form.nature_infraction}
                  onChange={(e) => update("nature_infraction", e.target.value)}
                  required
                />
                {errors.nature_infraction && <p className="text-sm text-destructive">{errors.nature_infraction}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_lieu_faits">Date et lieu des faits</Label>
                <Input
                  id="date_lieu_faits"
                  value={form.date_lieu_faits ?? ""}
                  onChange={(e) => update("date_lieu_faits", e.target.value)}
                  placeholder="Ex: 12/01/2026, Antananarivo"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="circonstances">Circonstances de l'affaire</Label>
                <textarea
                  id="circonstances"
                  className="w-full min-h-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.circonstances ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("circonstances", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="prejudices">Préjudices causés</Label>
                <textarea
                  id="prejudices"
                  className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.prejudices ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("prejudices", e.target.value)}
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
                            {isRenseignementPjImageAttachment(null, a.existingFile) && (
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
                              href={getRenseignementPjAttachmentDownloadUrl(Number(id), a.id)}
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
        src={viewerTarget ? getRenseignementPjAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
