import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getConvocationById,
  createConvocation,
  updateConvocation,
  getConvocationAttachments,
  createConvocationAttachment,
  updateConvocationAttachmentTitle,
  deleteConvocationAttachment,
  getConvocationAttachmentDownloadUrl,
  peekConvocationNumber,
  validateConvocationForm,
  CONVOCATION_TYPES,
  CONVOCATION_TYPE_LABELS,
  isConvocationImageAttachment,
} from "@/lib/api/convocation";
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
import type { Convocation, ConvocationInput } from "@/types";

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

export function ConvocationForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<ConvocationInput>({
    type: "ST_PARQUET",
    date_convocation: todayIso(),
    numero: "",
    nom: "",
    adresse: "",
    infraction: "",
    personne_accuse_recu: "",
    numero_dossier: "",
    observation: "",
  });
  const [numero, setNumero] = useState("");
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isEdit && id) {
      loadEntry(Number(id));
    } else {
      loadSuggestedNumber();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSuggestedNumber() {
    try {
      const num = await peekConvocationNumber(form.type);
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  function handleTypeChange(newType: ConvocationInput["type"]) {
    setForm((f) => ({ ...f, type: newType }));
    if (!isEdit) {
      peekConvocationNumber(newType)
        .then(setNumero)
        .catch(() => {});
    }
  }

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: Convocation = await getConvocationById(entryId);
      setForm({
        type: e.type,
        date_convocation: e.date_convocation.slice(0, 10),
        nom: e.nom,
        adresse: e.adresse ?? "",
        infraction: e.infraction ?? "",
        personne_accuse_recu: e.personne_accuse_recu ?? "",
        numero_dossier: e.numero_dossier ?? "",
        observation: e.observation ?? "",
      });
      setNumero(e.numero);
      try {
        const atts = await getConvocationAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger la convocation");
      navigate("/pj/convocation");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof ConvocationInput>(key: K, value: ConvocationInput[K]) {
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
    const validationErrors = validateConvocationForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: ConvocationInput = {
        ...form,
        numero: isEdit ? undefined : (numero.trim() || undefined),
        nom: form.nom.trim(),
        adresse: form.adresse?.trim() || null,
        infraction: form.infraction?.trim() || null,
        personne_accuse_recu: form.personne_accuse_recu?.trim() || null,
        numero_dossier: form.numero_dossier?.trim() || null,
        observation: form.observation?.trim() || null,
      };
      const saved: Convocation = isEdit && id
        ? await updateConvocation(Number(id), payload)
        : await createConvocation(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Convocation enregistrée avec succès");
      navigate(`/pj/convocation/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer la convocation";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteConvocationAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteConvocationAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createConvocationAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateConvocationAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createConvocationAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/convocation")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              {isEdit ? "Modifier la convocation" : "Nouvelle convocation"}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
          </div>
        </div>
        <Button onClick={handleSave} disabled={saving || loading} className="gap-2">
          {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
          {isEdit ? "Mettre à jour" : "Enregistrer"}
        </Button>
      </div>

      {loading && (
        <div className="flex justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && (
        <>
          {/* Type & Numéro */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Type & numéro</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Type de convocation *</Label>
                <Select
                  value={form.type}
                  onChange={(e) => handleTypeChange(e.target.value as ConvocationInput["type"])}
                  options={CONVOCATION_TYPES.map((t) => ({
                    value: t,
                    label: CONVOCATION_TYPE_LABELS[t],
                  }))}
                  required
                />
                {errors.type && <p className="text-sm text-destructive">{errors.type}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_convocation">Date *</Label>
                <Input
                  id="date_convocation"
                  type="date"
                  value={form.date_convocation}
                  onChange={(e) => update("date_convocation", e.target.value)}
                  required
                />
                {errors.date_convocation && <p className="text-sm text-destructive">{errors.date_convocation}</p>}
              </div>
              {isEdit ? (
                <div className="space-y-2">
                  <Label>Numéro</Label>
                  <Input value={numero} readOnly className="font-mono text-sm bg-muted/50" />
                </div>
              ) : (
                <div className="space-y-2">
                  <Label htmlFor="numero">Numéro (suggéré — modifiable)</Label>
                  <div className="flex gap-2">
                    <Input
                      id="numero"
                      value={numero}
                      onChange={(e) => setNumero(e.target.value)}
                      className="font-mono text-sm"
                      placeholder="Laissez vide pour auto-générer"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={loadSuggestedNumber}
                    >
                      Régénérer
                    </Button>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Le numéro suggéré est basé sur le type sélectionné.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Personne convoquée */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Personne convoquée</CardTitle>
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
              <div className="space-y-2">
                <Label htmlFor="infraction">Infraction *</Label>
                <Input
                  id="infraction"
                  value={form.infraction ?? ""}
                  onChange={(e) => update("infraction", e.target.value)}
                  required
                />
                {errors.infraction && <p className="text-sm text-destructive">{errors.infraction}</p>}
              </div>
            </CardContent>
          </Card>

          {/* Réception & dossier */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Réception & dossier</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="personne_accuse_recu">Nom de la personne ayant accusée réception</Label>
                <Input
                  id="personne_accuse_recu"
                  value={form.personne_accuse_recu ?? ""}
                  onChange={(e) => update("personne_accuse_recu", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="numero_dossier">Numéro du dossier</Label>
                <Input
                  id="numero_dossier"
                  value={form.numero_dossier ?? ""}
                  onChange={(e) => update("numero_dossier", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Observation */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Observation</CardTitle>
            </CardHeader>
            <CardContent>
              <textarea
                className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={form.observation ?? ""}
                onChange={(e) => update("observation", e.target.value)}
                placeholder="Remarques complémentaires"
              />
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
                            {isConvocationImageAttachment(null, a.existingFile) && (
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
                              href={getConvocationAttachmentDownloadUrl(Number(id), a.id)}
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
        </>
      )}

      <ImageViewerDialog
        src={viewerTarget ? getConvocationAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
