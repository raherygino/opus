import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRegistreEnqueteById,
  createRegistreEnquete,
  updateRegistreEnquete,
  getRegistreEnqueteAttachments,
  createRegistreEnqueteAttachment,
  updateRegistreEnqueteAttachmentTitle,
  deleteRegistreEnqueteAttachment,
  getRegistreEnqueteAttachmentDownloadUrl,
  peekRegistreEnqueteNumber,
  validateRegistreEnqueteForm,
  isRegistreEnqueteImageAttachment,
} from "@/lib/api/registre-enquete";
import { getPersonnelList } from "@/lib/api/personnel";
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
import {
  REGISTRE_ENQUETE_STATUTS,
  REGISTRE_ENQUETE_STATUT_LABELS,
  type Personnel,
  type RegistreEnquete,
  type RegistreEnqueteInput,
  type RegistreEnqueteStatut,
} from "@/types";

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

function personnelLabel(p: Personnel): string {
  return `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`;
}

export function RegistreEnqueteForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<RegistreEnqueteInput>({
    date_ouverture: todayIso(),
    numero_dossier: "",
    nature_infraction: "",
    date_lieu_faits: "",
    plaignant: "",
    mise_en_cause: "",
    enqueteur_personnel_id: 0,
    opj_personnel_id: 0,
    statut: "EN_COURS",
    observations: "",
  });
  const [numero, setNumero] = useState("");
  const [personnelList, setPersonnelList] = useState<Personnel[]>([]);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadPersonnel();
    if (isEdit && id) {
      loadEntry(Number(id));
    } else {
      loadSuggestedNumber();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSuggestedNumber() {
    try {
      const num = await peekRegistreEnqueteNumber();
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  async function loadPersonnel() {
    try {
      const data = await getPersonnelList();
      setPersonnelList(data);
    } catch {
      // Non-blocking
    }
  }

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: RegistreEnquete = await getRegistreEnqueteById(entryId);
      setForm({
        date_ouverture: e.date_ouverture?.slice(0, 10) ?? "",
        numero_dossier: e.numero_dossier ?? "",
        nature_infraction: e.nature_infraction,
        date_lieu_faits: e.date_lieu_faits ?? "",
        plaignant: e.plaignant ?? "",
        mise_en_cause: e.mise_en_cause ?? "",
        enqueteur_personnel_id: e.enqueteur_personnel_id ?? 0,
        opj_personnel_id: e.opj_personnel_id ?? 0,
        statut: e.statut,
        observations: e.observations ?? "",
      });
      setNumero(e.numero);
      try {
        const atts = await getRegistreEnqueteAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger l'entrée");
      navigate("/pj/registre-enquete");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof RegistreEnqueteInput>(key: K, value: RegistreEnqueteInput[K]) {
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
    const validationErrors = validateRegistreEnqueteForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: RegistreEnqueteInput = {
        ...form,
        numero: numero.trim() || undefined,
        numero_dossier: form.numero_dossier?.trim() || null,
        nature_infraction: form.nature_infraction.trim(),
        date_lieu_faits: form.date_lieu_faits?.trim() || null,
        plaignant: form.plaignant?.trim() || null,
        mise_en_cause: form.mise_en_cause?.trim() || null,
        enqueteur_personnel_id: form.enqueteur_personnel_id || null,
        opj_personnel_id: form.opj_personnel_id || null,
        statut: form.statut ?? "EN_COURS",
        observations: form.observations?.trim() || null,
      };
      const saved: RegistreEnquete = isEdit && id
        ? await updateRegistreEnquete(Number(id), payload)
        : await createRegistreEnquete(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Entrée au registre d'enquête enregistrée avec succès");
      navigate(`/pj/registre-enquete/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer l'entrée";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteRegistreEnqueteAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteRegistreEnqueteAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createRegistreEnqueteAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateRegistreEnqueteAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createRegistreEnqueteAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/registre-enquete")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier l'entrée" : "Nouvelle entrée au registre d'enquête"}
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
              <CardTitle className="text-base">Numéro & date</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="numero">
                  Numéro d'enregistrement {!isEdit && "(suggéré — modifiable)"}
                </Label>
                <Input
                  id="numero"
                  value={numero}
                  onChange={(e) => setNumero(e.target.value)}
                  readOnly={isEdit}
                  placeholder="N°…/MSP/SG/DGPN/DGA/DRSP.1/ENQ/CSP/TRIMO/…"
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
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="date_ouverture">Date d'ouverture *</Label>
                  <Input
                    id="date_ouverture"
                    type="date"
                    value={form.date_ouverture ?? ""}
                    onChange={(e) => update("date_ouverture", e.target.value)}
                  />
                  {errors.date_ouverture && (
                    <p className="text-sm text-destructive">{errors.date_ouverture}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="numero_dossier">N° du dossier rattaché</Label>
                  <Input
                    id="numero_dossier"
                    value={form.numero_dossier ?? ""}
                    onChange={(e) => update("numero_dossier", e.target.value)}
                    placeholder="Référence à la plainte ou au dossier"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="statut">Statut de l'enquête</Label>
                <Select
                  id="statut"
                  value={form.statut ?? "EN_COURS"}
                  onChange={(e) => update("statut", e.target.value as RegistreEnqueteStatut)}
                  options={REGISTRE_ENQUETE_STATUTS.map((s) => ({
                    value: s,
                    label: REGISTRE_ENQUETE_STATUT_LABELS[s],
                  }))}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Infraction</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="nature_infraction">Nature de l'infraction *</Label>
                <Input
                  id="nature_infraction"
                  value={form.nature_infraction}
                  onChange={(e) => update("nature_infraction", e.target.value)}
                  required
                />
                {errors.nature_infraction && (
                  <p className="text-sm text-destructive">{errors.nature_infraction}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_lieu_faits">Date et lieu des faits</Label>
                <textarea
                  id="date_lieu_faits"
                  className="w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.date_lieu_faits ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("date_lieu_faits", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Personnes</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="plaignant">Plaignant / Partie civile</Label>
                  <Input
                    id="plaignant"
                    value={form.plaignant ?? ""}
                    onChange={(e) => update("plaignant", e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="mise_en_cause">Mise en cause</Label>
                  <Input
                    id="mise_en_cause"
                    value={form.mise_en_cause ?? ""}
                    onChange={(e) => update("mise_en_cause", e.target.value)}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="enqueteur_personnel_id">Enquêteur *</Label>
                  <Select
                    id="enqueteur_personnel_id"
                    value={String(form.enqueteur_personnel_id ?? 0)}
                    onChange={(e) => update("enqueteur_personnel_id", Number(e.target.value))}
                    options={[
                      { value: "0", label: "Sélectionner un enquêteur" },
                      ...personnelList.map((p) => ({ value: String(p.id), label: personnelLabel(p) })),
                    ]}
                    required
                  />
                  {errors.enqueteur_personnel_id && (
                    <p className="text-sm text-destructive">{errors.enqueteur_personnel_id}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="opj_personnel_id">OPJ</Label>
                  <Select
                    id="opj_personnel_id"
                    value={String(form.opj_personnel_id ?? 0)}
                    onChange={(e) => update("opj_personnel_id", Number(e.target.value))}
                    options={[
                      { value: "0", label: "Sélectionner un OPJ" },
                      ...personnelList.map((p) => ({ value: String(p.id), label: personnelLabel(p) })),
                    ]}
                  />
                </div>
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
                            {isRegistreEnqueteImageAttachment(null, a.existingFile) && (
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
                              href={getRegistreEnqueteAttachmentDownloadUrl(Number(id), a.id)}
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
        src={viewerTarget ? getRegistreEnqueteAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
