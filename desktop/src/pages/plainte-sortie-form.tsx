import { useState, useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPlainteSortieById,
  createPlainteSortie,
  updatePlainteSortie,
  getPlainteSortieAttachments,
  createPlainteSortieAttachment,
  updatePlainteSortieAttachmentTitle,
  deletePlainteSortieAttachment,
  getPlainteSortieAttachmentDownloadUrl,
  getPlaintesEntreeWithoutSortie,
  peekPlainteSortieNumber,
  validatePlainteSortieForm,
  PLAINTE_SORTIE_NATURES,
  PLAINTE_SORTIE_NATURE_LABELS,
  PLAINTE_ENTREE_TYPE_LABELS,
} from "@/lib/api/plainte";
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
  ArrowRightLeft,
} from "lucide-react";
import type {
  PlainteSortie,
  PlainteSortieInput,
  PlainteEntreeSummary,
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

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

export function PlainteSortieForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const [searchParams] = useSearchParams();
  const preselectedEntreeId = Number(searchParams.get("entreeId") ?? 0);
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<PlainteSortieInput>({
    plainte_entree_id: preselectedEntreeId,
    nature: "DAT",
    date_sortie: todayIso(),
    numero: "",
    numero_ttr: "",
    nom_substitut: "",
    date_deferrement: "",
    observation: "",
  });
  const [numero, setNumero] = useState("");
  const [entreeSummary, setEntreeSummary] = useState<PlainteEntreeSummary | null>(null);
  const [availableEntrees, setAvailableEntrees] = useState<PlainteEntreeSummary[]>([]);
  const [loadingEntrees, setLoadingEntrees] = useState(false);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isEdit && id) {
      loadSortie(Number(id));
    } else if (preselectedEntreeId > 0) {
      loadEntreeSummary(preselectedEntreeId);
    } else {
      loadAvailableEntrees();
    }
    if (!isEdit) {
      loadSuggestedNumber();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSuggestedNumber() {
    try {
      const num = await peekPlainteSortieNumber();
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  async function loadAvailableEntrees() {
    setLoadingEntrees(true);
    try {
      const data = await getPlaintesEntreeWithoutSortie();
      setAvailableEntrees(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les plaintes ENTRÉE");
    } finally {
      setLoadingEntrees(false);
    }
  }

  async function loadEntreeSummary(entreeId: number) {
    try {
      const data = await getPlaintesEntreeWithoutSortie();
      const summary = data.find((e) => e.id === entreeId);
      if (summary) {
        setEntreeSummary(summary);
        setForm((f) => ({ ...f, plainte_entree_id: entreeId }));
      }
    } catch {
      // Non-blocking
    }
  }

  async function loadSortie(sortieId: number) {
    setLoading(true);
    try {
      const s: PlainteSortie = await getPlainteSortieById(sortieId);
      setForm({
        plainte_entree_id: s.plainte_entree_id,
        nature: s.nature,
        date_sortie: s.date_sortie.slice(0, 10),
        numero_ttr: s.numero_ttr ?? "",
        nom_substitut: s.nom_substitut ?? "",
        date_deferrement: s.date_deferrement?.slice(0, 10) ?? "",
        observation: s.observation ?? "",
      });
      setNumero(s.numero);
      // Build a summary from the joined entree data
      if (s.entree_numero_dossier) {
        setEntreeSummary({
          id: s.plainte_entree_id,
          type: s.entree_type ?? "ST_PARQUET",
          numero_dossier: s.entree_numero_dossier,
          date_plainte: s.entree_date_plainte ?? "",
          partie_civile: s.entree_partie_civile ?? null,
          mise_en_cause: s.entree_mise_en_cause ?? null,
          infraction: s.entree_infraction ?? null,
          opj_prenoms: s.entree_opj_prenoms ?? null,
          opj_nom: s.entree_opj_nom ?? null,
          opj_grade: s.entree_opj_grade ?? null,
        });
      }
      try {
        const atts = await getPlainteSortieAttachments(sortieId);
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
      addNotification("error", "Erreur", "Impossible de charger la sortie");
      navigate("/pj/plainte");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof PlainteSortieInput>(key: K, value: PlainteSortieInput[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function selectEntree(entreeId: number) {
    const summary = availableEntrees.find((e) => e.id === entreeId);
    setEntreeSummary(summary ?? null);
    setForm((f) => ({ ...f, plainte_entree_id: entreeId }));
  }

  function changeEntree() {
    setEntreeSummary(null);
    setForm((f) => ({ ...f, plainte_entree_id: 0 }));
    if (availableEntrees.length === 0) {
      loadAvailableEntrees();
    }
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
    if (!form.plainte_entree_id || form.plainte_entree_id <= 0) {
      addNotification("error", "Erreur", "Aucune plainte ENTRÉE associée");
      return;
    }
    const validationErrors = validatePlainteSortieForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: PlainteSortieInput = {
        ...form,
        numero: isEdit ? undefined : (numero.trim() || undefined),
        numero_ttr: form.numero_ttr.trim(),
        nom_substitut: form.nom_substitut.trim(),
        date_deferrement: form.nature === "DEFERREMENT" ? form.date_deferrement?.trim() || null : null,
        observation: form.observation?.trim() || null,
      };
      const saved: PlainteSortie = isEdit && id
        ? await updatePlainteSortie(Number(id), payload)
        : await createPlainteSortie(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Sortie enregistrée avec succès");
      navigate(`/pj/plainte/sortie/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer la sortie";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deletePlainteSortieAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deletePlainteSortieAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createPlainteSortieAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updatePlainteSortieAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createPlainteSortieAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  const showDateDeferrement = form.nature === "DEFERREMENT";

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/plainte")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier la sortie" : "Nouvelle sortie"}
              </h1>
              <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — SORTIE</p>
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
        <>
          {/* ENTRÉE selection / summary */}
          {entreeSummary ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-base flex items-center gap-2">
                  <ArrowRightLeft className="h-4 w-4" />
                  Plainte ENTRÉE associée
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                  {PLAINTE_ENTREE_TYPE_LABELS[entreeSummary.type] ?? entreeSummary.type}
                </span>
                <p className="font-mono text-sm font-bold">{entreeSummary.numero_dossier}</p>
                <p className="text-sm text-muted-foreground">Date: {formatDate(entreeSummary.date_plainte)}</p>
                {entreeSummary.infraction && (
                  <p className="text-sm text-muted-foreground">Infraction: {entreeSummary.infraction}</p>
                )}
                {entreeSummary.mise_en_cause && (
                  <p className="text-sm text-muted-foreground">MC: {entreeSummary.mise_en_cause}</p>
                )}
                {!isEdit && (
                  <Button variant="link" size="sm" onClick={changeEntree} className="p-0 h-auto">
                    Changer de plainte ENTRÉE
                  </Button>
                )}
              </CardContent>
            </Card>
          ) : !isEdit && form.plainte_entree_id === 0 ? (
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Sélectionner une plainte ENTRÉE</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {loadingEntrees ? (
                  <div className="flex items-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span className="text-sm text-muted-foreground">Chargement des plaintes...</span>
                  </div>
                ) : availableEntrees.length === 0 ? (
                  <p className="text-sm text-muted-foreground">
                    Aucune plainte ENTRÉE sans sortie disponible. Créez d'abord une plainte ENTRÉE.
                  </p>
                ) : (
                  availableEntrees.map((entree) => (
                    <div
                      key={entree.id}
                      onClick={() => selectEntree(entree.id)}
                      className="flex items-center gap-3 rounded-md border p-3 cursor-pointer hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex-1">
                        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                          {PLAINTE_ENTREE_TYPE_LABELS[entree.type] ?? entree.type}
                        </span>
                        <p className="font-mono text-sm font-bold mt-1">{entree.numero_dossier}</p>
                        <p className="text-sm text-muted-foreground">
                          {formatDate(entree.date_plainte)} — MC: {entree.mise_en_cause ?? "—"}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>
          ) : null}

          {/* Sortie details */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Sortie</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label>Nature *</Label>
                <Select
                  value={form.nature}
                  onChange={(e) => update("nature", e.target.value as PlainteSortieInput["nature"])}
                  options={PLAINTE_SORTIE_NATURES.map((n) => ({
                    value: n,
                    label: PLAINTE_SORTIE_NATURE_LABELS[n],
                  }))}
                  required
                />
                {errors.nature && <p className="text-sm text-destructive">{errors.nature}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_sortie">Date *</Label>
                <Input
                  id="date_sortie"
                  type="date"
                  value={form.date_sortie}
                  onChange={(e) => update("date_sortie", e.target.value)}
                  required
                />
                {errors.date_sortie && <p className="text-sm text-destructive">{errors.date_sortie}</p>}
              </div>
              {isEdit ? (
                numero && (
                  <div className="space-y-2">
                    <Label>Numéro</Label>
                    <Input value={numero} readOnly className="font-mono text-sm bg-muted/50" />
                  </div>
                )
              ) : (
                <div className="space-y-2">
                  <Label htmlFor="numero">Numéro (suggéré — modifiable)</Label>
                  <Input
                    id="numero"
                    value={numero}
                    onChange={(e) => setNumero(e.target.value)}
                    className="font-mono text-sm"
                  />
                  <Button
                    variant="link"
                    size="sm"
                    onClick={loadSuggestedNumber}
                    className="p-0 h-auto text-xs"
                  >
                    Régénérer le numéro suggéré
                  </Button>
                </div>
              )}
              <div className="space-y-2">
                <Label htmlFor="numero_ttr">N° TTR *</Label>
                <Input
                  id="numero_ttr"
                  value={form.numero_ttr}
                  onChange={(e) => update("numero_ttr", e.target.value)}
                  required
                />
                {errors.numero_ttr && <p className="text-sm text-destructive">{errors.numero_ttr}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="nom_substitut">Nom du Substitut *</Label>
                <Input
                  id="nom_substitut"
                  value={form.nom_substitut}
                  onChange={(e) => update("nom_substitut", e.target.value)}
                  required
                />
                {errors.nom_substitut && <p className="text-sm text-destructive">{errors.nom_substitut}</p>}
              </div>
              {showDateDeferrement && (
                <div className="space-y-2">
                  <Label htmlFor="date_deferrement">Date du déferrement *</Label>
                  <Input
                    id="date_deferrement"
                    type="date"
                    value={form.date_deferrement ?? ""}
                    onChange={(e) => update("date_deferrement", e.target.value)}
                    required
                  />
                  {errors.date_deferrement && <p className="text-sm text-destructive">{errors.date_deferrement}</p>}
                </div>
              )}
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
                    id={`sortie-file-${i}`}
                  />
                  <Label htmlFor={`sortie-file-${i}`} className="cursor-pointer">
                    <span className="text-sm text-primary underline">
                      {a.file?.name ?? a.existingFile ?? "Choisir un fichier"}
                    </span>
                  </Label>
                  {a.id && a.existingFile && !a.file && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      onClick={() => {
                        if (isImageFile(null, a.existingFile)) {
                          setViewerTarget({ id: a.id!, title: a.title });
                        } else {
                          window.open(getPlainteSortieAttachmentDownloadUrl(Number(id), a.id!), "_blank");
                        }
                      }}
                    >
                      {isImageFile(null, a.existingFile) ? <Eye className="h-3.5 w-3.5" /> : <Download className="h-3.5 w-3.5" />}
                    </Button>
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
        src={viewerTarget ? getPlainteSortieAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
