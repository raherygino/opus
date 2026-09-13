import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPlainteEntreeById,
  createPlainteEntree,
  updatePlainteEntree,
  getPlainteEntreeAttachments,
  createPlainteEntreeAttachment,
  updatePlainteEntreeAttachmentTitle,
  deletePlainteEntreeAttachment,
  getPlainteEntreeAttachmentDownloadUrl,
  validatePlainteEntreeForm,
  PLAINTE_ENTREE_TYPES,
  PLAINTE_ENTREE_TYPE_LABELS,
} from "@/lib/api/plainte";
import { getPersonnelList } from "@/lib/api/personnel";
import { isImageFile } from "@/lib/utils/attachment";
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
import type { PlainteEntree, PlainteEntreeAttachment, PlainteEntreeInput, Personnel } from "@/types";

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

export function PlainteForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<PlainteEntreeInput>({
    type: "ST_PARQUET",
    date_plainte: todayIso(),
    numero_st: "",
    opj_personnel_id: 0,
    enqueteur_personnel_id: 0,
    partie_civile: "",
    mise_en_cause: "",
    adresse_pc: "",
    infraction: "",
    prejudice: "",
    lieu_infraction: "",
    heure_infraction: "",
    observation: "",
  });
  const [numeroDossier, setNumeroDossier] = useState("");
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
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

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
      const e: PlainteEntree = await getPlainteEntreeById(entryId);
      setForm({
        type: e.type,
        date_plainte: e.date_plainte.slice(0, 10),
        numero_st: e.numero_st ?? "",
        opj_personnel_id: e.opj_personnel_id ?? 0,
        enqueteur_personnel_id: e.enqueteur_personnel_id ?? 0,
        partie_civile: e.partie_civile ?? "",
        mise_en_cause: e.mise_en_cause ?? "",
        adresse_pc: e.adresse_pc ?? "",
        infraction: e.infraction ?? "",
        prejudice: e.prejudice ?? "",
        lieu_infraction: e.lieu_infraction ?? "",
        heure_infraction: e.heure_infraction?.slice(0, 5) ?? "",
        observation: e.observation ?? "",
      });
      setNumeroDossier(e.numero_dossier);
      try {
        const atts = await getPlainteEntreeAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger la plainte");
      navigate("/pj/plainte");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof PlainteEntreeInput>(key: K, value: PlainteEntreeInput[K]) {
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
    const validationErrors = validatePlainteEntreeForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: PlainteEntreeInput = {
        ...form,
        numero_st: form.type === "ST_PARQUET" ? form.numero_st?.trim() || null : null,
        partie_civile: form.partie_civile?.trim() || null,
        mise_en_cause: form.mise_en_cause?.trim() || null,
        adresse_pc: form.adresse_pc?.trim() || null,
        infraction: form.infraction?.trim() || null,
        prejudice: form.prejudice?.trim() || null,
        lieu_infraction: form.lieu_infraction?.trim() || null,
        heure_infraction: form.heure_infraction?.trim() || null,
        observation: form.observation?.trim() || null,
      };
      const saved: PlainteEntree = isEdit && id
        ? await updatePlainteEntree(Number(id), payload)
        : await createPlainteEntree(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Plainte enregistrée avec succès");
      navigate(`/pj/plainte/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer la plainte";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deletePlainteEntreeAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        // Replace: delete then re-create
        await deletePlainteEntreeAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createPlainteEntreeAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updatePlainteEntreeAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createPlainteEntreeAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  const showNumeroSt = form.type === "ST_PARQUET";
  const showPartieCivile = form.type === "ST_PARQUET" || form.type === "PLAINTE_DIRECTE";
  const showAdressePc = form.type === "ST_PARQUET" || form.type === "PLAINTE_DIRECTE";

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/plainte")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              {isEdit ? "Modifier la plainte" : "Nouvelle plainte ENTRÉE"}
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
          {/* Type & Dossier */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Type de plainte & dossier</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Type de plainte *</Label>
                <Select
                  value={form.type}
                  onChange={(e) => update("type", e.target.value as PlainteEntreeInput["type"])}
                  options={PLAINTE_ENTREE_TYPES.map((t) => ({
                    value: t,
                    label: PLAINTE_ENTREE_TYPE_LABELS[t],
                  }))}
                  required
                />
                {errors.type && <p className="text-sm text-destructive">{errors.type}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_plainte">Date *</Label>
                <Input
                  id="date_plainte"
                  type="date"
                  value={form.date_plainte}
                  onChange={(e) => update("date_plainte", e.target.value)}
                  required
                />
                {errors.date_plainte && <p className="text-sm text-destructive">{errors.date_plainte}</p>}
              </div>
              {isEdit && numeroDossier && (
                <div className="space-y-2">
                  <Label>Numéro du dossier</Label>
                  <Input value={numeroDossier} readOnly className="font-mono text-sm bg-muted/50" />
                </div>
              )}
              {showNumeroSt && (
                <div className="space-y-2">
                  <Label htmlFor="numero_st">Numéro du ST *</Label>
                  <Input
                    id="numero_st"
                    value={form.numero_st ?? ""}
                    onChange={(e) => update("numero_st", e.target.value)}
                    required
                  />
                  {errors.numero_st && <p className="text-sm text-destructive">{errors.numero_st}</p>}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Personnes */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Personnes</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="opj_personnel_id">OPJ *</Label>
                  <Select
                    id="opj_personnel_id"
                    value={String(form.opj_personnel_id ?? 0)}
                    onChange={(e) => update("opj_personnel_id", Number(e.target.value))}
                    options={[
                      { value: "0", label: "Sélectionner un OPJ" },
                      ...personnelList.map((p) => ({ value: String(p.id), label: personnelLabel(p) })),
                    ]}
                    required
                  />
                  {errors.opj_personnel_id && <p className="text-sm text-destructive">{errors.opj_personnel_id}</p>}
                </div>
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
                  {errors.enqueteur_personnel_id && <p className="text-sm text-destructive">{errors.enqueteur_personnel_id}</p>}
                </div>
              </div>
              {showPartieCivile && (
                <div className="space-y-2">
                  <Label htmlFor="partie_civile">Partie civile (PC) *</Label>
                  <Input
                    id="partie_civile"
                    value={form.partie_civile ?? ""}
                    onChange={(e) => update("partie_civile", e.target.value)}
                    required
                  />
                  {errors.partie_civile && <p className="text-sm text-destructive">{errors.partie_civile}</p>}
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="mise_en_cause">Mise en cause (MC) *</Label>
                <Input
                  id="mise_en_cause"
                  value={form.mise_en_cause ?? ""}
                  onChange={(e) => update("mise_en_cause", e.target.value)}
                  required
                />
                {errors.mise_en_cause && <p className="text-sm text-destructive">{errors.mise_en_cause}</p>}
              </div>
              {showAdressePc && (
                <div className="space-y-2">
                  <Label htmlFor="adresse_pc">Adresse du PC *</Label>
                  <Input
                    id="adresse_pc"
                    value={form.adresse_pc ?? ""}
                    onChange={(e) => update("adresse_pc", e.target.value)}
                    required
                  />
                  {errors.adresse_pc && <p className="text-sm text-destructive">{errors.adresse_pc}</p>}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Infraction */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Infraction</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
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
              <div className="space-y-2">
                <Label htmlFor="prejudice">Préjudice *</Label>
                <Input
                  id="prejudice"
                  value={form.prejudice ?? ""}
                  onChange={(e) => update("prejudice", e.target.value)}
                  required
                />
                {errors.prejudice && <p className="text-sm text-destructive">{errors.prejudice}</p>}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="lieu_infraction">Lieu de l'infraction *</Label>
                  <Input
                    id="lieu_infraction"
                    value={form.lieu_infraction ?? ""}
                    onChange={(e) => update("lieu_infraction", e.target.value)}
                    required
                  />
                  {errors.lieu_infraction && <p className="text-sm text-destructive">{errors.lieu_infraction}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="heure_infraction">Heure de l'infraction</Label>
                  <Input
                    id="heure_infraction"
                    type="time"
                    value={form.heure_infraction ?? ""}
                    onChange={(e) => update("heure_infraction", e.target.value)}
                  />
                  {errors.heure_infraction && <p className="text-sm text-destructive">{errors.heure_infraction}</p>}
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Observation */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Observation</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <Label htmlFor="observation">Observation</Label>
                <textarea
                  id="observation"
                  className="flex min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  value={form.observation ?? ""}
                  onChange={(e) => update("observation", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Pièces jointes */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Paperclip className="h-4 w-4" />
                Pièces jointes
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              {attachments.filter((a) => !a._delete).map((a, i) => (
                <div key={i} className="flex items-center gap-2 rounded-md border p-3">
                  <Input
                    placeholder="Titre"
                    value={a.title}
                    onChange={(e) => updateAttachmentTitle(i, e.target.value)}
                    className="flex-1"
                  />
                  <input
                    type="file"
                    onChange={(e) => e.target.files?.[0] && setAttachmentFile(i, e.target.files[0])}
                    className="hidden"
                    id={`file-${i}`}
                  />
                  <Label htmlFor={`file-${i}`} className="cursor-pointer">
                    <span className="text-sm text-primary underline">
                      {a.file?.name ?? a.existingFile ?? "Choisir un fichier"}
                    </span>
                  </Label>
                  {a.id && a.existingFile && !a.file && (
                    <>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => {
                          const att: PlainteEntreeAttachment = { id: a.id!, title: a.title, filename: "", original_filename: a.existingFile ?? "", mime_type: null, file_size: null, created_at: "", updated_at: "", plainte_entree_id: 0 };
                          if (isImageFile(att.mime_type, att.original_filename)) {
                            setViewerTarget({ id: a.id!, title: a.title });
                          } else {
                            window.open(getPlainteEntreeAttachmentDownloadUrl(Number(id), a.id!), "_blank");
                          }
                        }}
                      >
                        {isImageFile(null, a.existingFile) ? <Eye className="h-3.5 w-3.5" /> : <Download className="h-3.5 w-3.5" />}
                      </Button>
                    </>
                  )}
                  <Button variant="ghost" size="icon" className="h-7 w-7 text-destructive" onClick={() => removeAttachment(i)}>
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              ))}
              <Button variant="outline" size="sm" onClick={addAttachment} className="gap-2">
                <Plus className="h-4 w-4" />
                Ajouter une pièce jointe
              </Button>
            </CardContent>
          </Card>
        </>
      )}

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={viewerTarget ? getPlainteEntreeAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
