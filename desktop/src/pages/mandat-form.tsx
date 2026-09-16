import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getMandatById,
  createMandat,
  updateMandat,
  getMandatAttachments,
  createMandatAttachment,
  updateMandatAttachmentTitle,
  deleteMandatAttachment,
  getMandatAttachmentDownloadUrl,
  peekMandatNumber,
  validateMandatForm,
  isMandatImageAttachment,
} from "@/lib/api/mandat";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select } from "@/components/ui/select";
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
import { MANDAT_TYPES } from "@/types";
import type { Mandat, MandatInput } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

export function MandatForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<MandatInput>({
    type: "",
    autorite: "",
    personne_nom: "",
    date_lieu_naissance: "",
    motif: "",
    qualification_infraction: "",
    opj_execution: "",
    date_heure_execution: "",
    lieu_execution: "",
    observations: "",
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
      const num = await peekMandatNumber();
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: Mandat = await getMandatById(entryId);
      setForm({
        type: (e.type as MandatInput["type"]) || "",
        autorite: e.autorite ?? "",
        personne_nom: e.personne_nom,
        date_lieu_naissance: e.date_lieu_naissance ?? "",
        motif: e.motif ?? "",
        qualification_infraction: e.qualification_infraction ?? "",
        opj_execution: e.opj_execution ?? "",
        date_heure_execution: e.date_heure_execution?.slice(0, 16) ?? "",
        lieu_execution: e.lieu_execution ?? "",
        observations: e.observations ?? "",
      });
      setNumero(e.numero);
      try {
        const atts = await getMandatAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger le mandat");
      navigate("/pj/mandat");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof MandatInput>(key: K, value: MandatInput[K]) {
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
    const validationErrors = validateMandatForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: MandatInput = {
        ...form,
        numero: isEdit ? undefined : (numero.trim() || undefined),
        autorite: form.autorite?.trim() || null,
        personne_nom: form.personne_nom.trim(),
        date_lieu_naissance: form.date_lieu_naissance?.trim() || null,
        motif: form.motif?.trim() || null,
        qualification_infraction: form.qualification_infraction?.trim() || null,
        opj_execution: form.opj_execution?.trim() || null,
        date_heure_execution: form.date_heure_execution || null,
        lieu_execution: form.lieu_execution?.trim() || null,
        observations: form.observations?.trim() || null,
      };
      const saved: Mandat = isEdit && id
        ? await updateMandat(Number(id), payload)
        : await createMandat(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Mandat enregistré avec succès");
      navigate(`/pj/mandat/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer le mandat";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteMandatAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteMandatAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createMandatAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateMandatAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createMandatAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/mandat")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier le mandat" : "Nouveau mandat"}
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
              <CardTitle className="text-base">Mandat</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="type">Objet du mandat *</Label>
                <Select
                  id="type"
                  value={form.type}
                  onChange={(e) => update("type", e.target.value as MandatInput["type"])}
                  options={MANDAT_TYPES}
                  placeholder="— Choisir le type —"
                />
                {errors.type && <p className="text-sm text-destructive">{errors.type}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="numero">
                  Numéro du mandat {!isEdit && "(suggéré — modifiable)"}
                </Label>
                <Input
                  id="numero"
                  value={numero}
                  onChange={(e) => setNumero(e.target.value)}
                  readOnly={isEdit}
                  placeholder="N°…/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/…"
                />
                {!isEdit && (
                  <button
                    type="button"
                    className="text-xs text-primary hover:underline"
                    onClick={loadSuggestedNumber}
                  >
                    Régénérer le numéro suggéré
                  </button>
                )}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Autorité</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="autorite">Autorité ayant délivré le mandat</Label>
                <Input
                  id="autorite"
                  value={form.autorite ?? ""}
                  onChange={(e) => update("autorite", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Personne concernée</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="personne_nom">Nom et prénom *</Label>
                <Input
                  id="personne_nom"
                  value={form.personne_nom}
                  onChange={(e) => update("personne_nom", e.target.value)}
                  required
                />
                {errors.personne_nom && <p className="text-sm text-destructive">{errors.personne_nom}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_lieu_naissance">Date et lieu de naissance</Label>
                <Input
                  id="date_lieu_naissance"
                  value={form.date_lieu_naissance ?? ""}
                  onChange={(e) => update("date_lieu_naissance", e.target.value)}
                  placeholder="Ex: 15/03/1990, Antananarivo"
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Informations judiciaires</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="motif">Motif du mandat</Label>
                <textarea
                  id="motif"
                  className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.motif ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("motif", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="qualification_infraction">Qualification de l'infraction</Label>
                <Input
                  id="qualification_infraction"
                  value={form.qualification_infraction ?? ""}
                  onChange={(e) => update("qualification_infraction", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Exécution</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="opj_execution">Nom de l'OPJ chargé de l'exécution</Label>
                <Input
                  id="opj_execution"
                  value={form.opj_execution ?? ""}
                  onChange={(e) => update("opj_execution", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_heure_execution">Date et heure d'exécution</Label>
                <Input
                  id="date_heure_execution"
                  type="datetime-local"
                  value={form.date_heure_execution ?? ""}
                  onChange={(e) => update("date_heure_execution", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lieu_execution">Lieu d'exécution</Label>
                <Input
                  id="lieu_execution"
                  value={form.lieu_execution ?? ""}
                  onChange={(e) => update("lieu_execution", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Observations</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="observations">Observations</Label>
                <textarea
                  id="observations"
                  className="w-full min-h-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.observations ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("observations", e.target.value)}
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
                            {isMandatImageAttachment(null, a.existingFile) && (
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
                              href={getMandatAttachmentDownloadUrl(Number(id), a.id)}
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
        src={viewerTarget ? getMandatAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
