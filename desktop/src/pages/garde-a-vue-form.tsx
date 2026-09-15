import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getGardeAVueById,
  createGardeAVue,
  updateGardeAVue,
  getGardeAVueAttachments,
  createGardeAVueAttachment,
  updateGardeAVueAttachmentTitle,
  deleteGardeAVueAttachment,
  getGardeAVueAttachmentDownloadUrl,
  validateGardeAVueForm,
  isGardeAVueImageAttachment,
} from "@/lib/api/garde-a-vue";
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
import type { GardeAVue, GardeAVueInput } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

function toDatetimeLocal(iso: string | null | undefined): string {
  if (!iso) return "";
  // "2026-09-15 08:00:00" → "2026-09-15T08:00"
  return iso.replace(" ", "T").substring(0, 16);
}

function fromDatetimeLocal(value: string): string | null {
  if (!value) return null;
  // "2026-09-15T08:00" → "2026-09-15 08:00:00"
  return `${value.replace("T", " ")}:00`;
}

export function GardeAVueForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<GardeAVueInput>({
    nom: "",
    prenoms: "",
    date_naissance: "",
    adresse: "",
    enqueteur_permance: "",
    opj_gav: "",
    motif: "",
    etat_sante: "",
    droits_notifies: "",
    personne_contacter: "",
    debut_gav: "",
    fin_gav: "",
    prolongation_gav: "",
  });
  const [debutLocal, setDebutLocal] = useState("");
  const [finLocal, setFinLocal] = useState("");
  const [prolongationLocal, setProlongationLocal] = useState("");
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
      const e: GardeAVue = await getGardeAVueById(entryId);
      setForm({
        nom: e.nom,
        prenoms: e.prenoms ?? "",
        date_naissance: e.date_naissance?.slice(0, 10) ?? "",
        adresse: e.adresse ?? "",
        enqueteur_permance: e.enqueteur_permance ?? "",
        opj_gav: e.opj_gav ?? "",
        motif: e.motif ?? "",
        etat_sante: e.etat_sante ?? "",
        droits_notifies: e.droits_notifies ?? "",
        personne_contacter: e.personne_contacter ?? "",
        debut_gav: e.debut_gav ?? "",
        fin_gav: e.fin_gav ?? "",
        prolongation_gav: e.prolongation_gav ?? "",
      });
      setDebutLocal(toDatetimeLocal(e.debut_gav));
      setFinLocal(toDatetimeLocal(e.fin_gav));
      setProlongationLocal(toDatetimeLocal(e.prolongation_gav));
      try {
        const atts = await getGardeAVueAttachments(entryId);
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
      addNotification("error", "Erreur", "Impossible de charger la garde à vue");
      navigate("/pj/gav");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof GardeAVueInput>(key: K, value: GardeAVueInput[K]) {
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
    const payload: GardeAVueInput = {
      ...form,
      nom: form.nom.trim(),
      prenoms: form.prenoms?.trim() || null,
      date_naissance: form.date_naissance?.trim() || null,
      adresse: form.adresse?.trim() || null,
      enqueteur_permance: form.enqueteur_permance?.trim() || null,
      opj_gav: form.opj_gav?.trim() || null,
      motif: form.motif?.trim() || null,
      etat_sante: form.etat_sante?.trim() || null,
      droits_notifies: form.droits_notifies?.trim() || null,
      personne_contacter: form.personne_contacter?.trim() || null,
      debut_gav: fromDatetimeLocal(debutLocal),
      fin_gav: fromDatetimeLocal(finLocal),
      prolongation_gav: fromDatetimeLocal(prolongationLocal),
    };
    const validationErrors = validateGardeAVueForm(payload);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const saved: GardeAVue = isEdit && id
        ? await updateGardeAVue(Number(id), payload)
        : await createGardeAVue(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Garde à vue enregistrée avec succès");
      navigate(`/pj/gav/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer la garde à vue";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteGardeAVueAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteGardeAVueAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createGardeAVueAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateGardeAVueAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createGardeAVueAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/gav")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier la garde à vue" : "Nouvelle garde à vue"}
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
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="nom">Nom *</Label>
                  <Input
                    id="nom"
                    value={form.nom ?? ""}
                    onChange={(e) => update("nom", e.target.value)}
                    required
                  />
                  {errors.nom && <p className="text-sm text-destructive">{errors.nom}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="prenoms">Prénoms</Label>
                  <Input
                    id="prenoms"
                    value={form.prenoms ?? ""}
                    onChange={(e) => update("prenoms", e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="date_naissance">Date de naissance</Label>
                  <Input
                    id="date_naissance"
                    type="date"
                    value={form.date_naissance ?? ""}
                    onChange={(e) => update("date_naissance", e.target.value)}
                  />
                  {errors.date_naissance && <p className="text-sm text-destructive">{errors.date_naissance}</p>}
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="adresse">Adresse</Label>
                <textarea
                  id="adresse"
                  className="w-full min-h-[60px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.adresse ?? ""}
                  onChange={(e) => update("adresse", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Enquête */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Enquête</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="enqueteur_permance">Enquêteur de permanence</Label>
                  <Input
                    id="enqueteur_permance"
                    value={form.enqueteur_permance ?? ""}
                    onChange={(e) => update("enqueteur_permance", e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="opj_gav">OPJ ayant décidé la garde à vue</Label>
                  <Input
                    id="opj_gav"
                    value={form.opj_gav ?? ""}
                    onChange={(e) => update("opj_gav", e.target.value)}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="motif">Motif</Label>
                <textarea
                  id="motif"
                  className="w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.motif ?? ""}
                  onChange={(e) => update("motif", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Santé & droits */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Santé & droits</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="etat_sante">État de santé</Label>
                <textarea
                  id="etat_sante"
                  className="w-full min-h-[60px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.etat_sante ?? ""}
                  onChange={(e) => update("etat_sante", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="droits_notifies">Droits notifiés</Label>
                <textarea
                  id="droits_notifies"
                  className="w-full min-h-[60px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.droits_notifies ?? ""}
                  onChange={(e) => update("droits_notifies", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="personne_contacter">Personne à contacter</Label>
                <Input
                  id="personne_contacter"
                  value={form.personne_contacter ?? ""}
                  onChange={(e) => update("personne_contacter", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Dates de la garde à vue */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Dates de la garde à vue</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="debut_gav">Début de GAV</Label>
                  <Input
                    id="debut_gav"
                    type="datetime-local"
                    value={debutLocal}
                    onChange={(e) => setDebutLocal(e.target.value)}
                  />
                  {errors.debut_gav && <p className="text-sm text-destructive">{errors.debut_gav}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="fin_gav">Fin de GAV</Label>
                  <Input
                    id="fin_gav"
                    type="datetime-local"
                    value={finLocal}
                    onChange={(e) => setFinLocal(e.target.value)}
                  />
                  {errors.fin_gav && <p className="text-sm text-destructive">{errors.fin_gav}</p>}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="prolongation_gav">Prolongation de GAV</Label>
                  <Input
                    id="prolongation_gav"
                    type="datetime-local"
                    value={prolongationLocal}
                    onChange={(e) => setProlongationLocal(e.target.value)}
                  />
                  {errors.prolongation_gav && <p className="text-sm text-destructive">{errors.prolongation_gav}</p>}
                </div>
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
                            {isGardeAVueImageAttachment(null, a.existingFile) && (
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
                              href={getGardeAVueAttachmentDownloadUrl(Number(id), a.id)}
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
        src={viewerTarget ? getGardeAVueAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
