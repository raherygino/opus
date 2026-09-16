import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRequisitionById,
  createRequisition,
  updateRequisition,
  getRequisitionAttachments,
  createRequisitionAttachment,
  updateRequisitionAttachmentTitle,
  deleteRequisitionAttachment,
  getRequisitionAttachmentDownloadUrl,
  peekRequisitionNumber,
  validateRequisitionForm,
  REQUISITION_TYPES,
  REQUISITION_TYPE_LABELS,
  isRequisitionImageAttachment,
} from "@/lib/api/requisition";
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
import type { Requisition, RequisitionInput } from "@/types";

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

export function RequisitionForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<RequisitionInput>({
    type: "TPH",
    date_requisition: todayIso(),
    numero: "",
    numero_ttr: "",
    nom_substitut: "",
    affaire: "",
    numero_dossier: "",
    opj: "",
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
      const num = await peekRequisitionNumber();
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  function handleTypeChange(newType: RequisitionInput["type"]) {
    setForm((f) => ({ ...f, type: newType }));
  }

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: Requisition = await getRequisitionById(entryId);
      setForm({
        type: e.type,
        date_requisition: e.date_requisition.slice(0, 10),
        numero_ttr: e.numero_ttr ?? "",
        nom_substitut: e.nom_substitut ?? "",
        affaire: e.affaire,
        numero_dossier: e.numero_dossier ?? "",
        opj: e.opj ?? "",
      });
      setNumero(e.numero);
      try {
        const atts = await getRequisitionAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger la réquisition");
      navigate("/pj/requisition");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof RequisitionInput>(key: K, value: RequisitionInput[K]) {
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
    const validationErrors = validateRequisitionForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: RequisitionInput = {
        ...form,
        numero: isEdit ? undefined : (numero.trim() || undefined),
        numero_ttr: form.numero_ttr?.trim() || null,
        nom_substitut: form.nom_substitut?.trim() || null,
        affaire: form.affaire.trim(),
        numero_dossier: form.numero_dossier?.trim() || null,
        opj: form.opj?.trim() || null,
      };
      const saved: Requisition = isEdit && id
        ? await updateRequisition(Number(id), payload)
        : await createRequisition(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Réquisition enregistrée avec succès");
      navigate(`/pj/requisition/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer la réquisition";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteRequisitionAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteRequisitionAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createRequisitionAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateRequisitionAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createRequisitionAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/requisition")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier la réquisition" : "Nouvelle réquisition"}
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
          {/* Type & Numéro */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Type & numéro</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Type de réquisition *</Label>
                <Select
                  value={form.type}
                  onChange={(e) => handleTypeChange(e.target.value as RequisitionInput["type"])}
                  options={REQUISITION_TYPES.map((t) => ({
                    value: t,
                    label: REQUISITION_TYPE_LABELS[t],
                  }))}
                  required
                />
                {errors.type && <p className="text-sm text-destructive">{errors.type}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_requisition">Date *</Label>
                <Input
                  id="date_requisition"
                  type="date"
                  value={form.date_requisition}
                  onChange={(e) => update("date_requisition", e.target.value)}
                  required
                />
                {errors.date_requisition && <p className="text-sm text-destructive">{errors.date_requisition}</p>}
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
                    Le numéro suggéré est basé sur la séquence REQ.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Informations judiciaires */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Informations judiciaires</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="numero_ttr">N° TTR</Label>
                <Input
                  id="numero_ttr"
                  value={form.numero_ttr ?? ""}
                  onChange={(e) => update("numero_ttr", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="nom_substitut">Nom du Substitut</Label>
                <Input
                  id="nom_substitut"
                  value={form.nom_substitut ?? ""}
                  onChange={(e) => update("nom_substitut", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="affaire">Affaire *</Label>
                <Input
                  id="affaire"
                  value={form.affaire ?? ""}
                  onChange={(e) => update("affaire", e.target.value)}
                  required
                />
                {errors.affaire && <p className="text-sm text-destructive">{errors.affaire}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="numero_dossier">Numéro du dossier concerné</Label>
                <Input
                  id="numero_dossier"
                  value={form.numero_dossier ?? ""}
                  onChange={(e) => update("numero_dossier", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="opj">OPJ</Label>
                <Input
                  id="opj"
                  value={form.opj ?? ""}
                  onChange={(e) => update("opj", e.target.value)}
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
                            {isRequisitionImageAttachment(null, a.existingFile) && (
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
                              href={getRequisitionAttachmentDownloadUrl(Number(id), a.id)}
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
        src={viewerTarget ? getRequisitionAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
