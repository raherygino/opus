import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import { useAuthStore } from "@/stores/auth-store";
import { hasPermission } from "@/lib/permissions";
import {
  createActivite,
  updateActivite,
  getActiviteById,
  getActiviteAttachments,
  createActiviteAttachment,
  deleteActiviteAttachment,
  updateActiviteAttachmentTitle,
  getActiviteAttachmentDownloadUrl,
  PATROUILLE_TYPES,
  PATROUILLE_MODES,
  patrouilleItineraireField,
} from "@/lib/api/activite";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft, Save, Loader2, Plus, Trash2, Paperclip, Camera, Image as ImageIcon } from "lucide-react";
import type { ActiviteInput } from "@/types";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";

const SG_ACTIVITE_MODULE = "sg_activite";

interface PendingFile {
  file: File;
  title: string;
  /** Object URL for local image preview before upload. */
  previewUrl?: string;
}

/** Selection state for one patrol mode (e.g. diurne × motorisée). */
interface PatrouilleRow {
  selected: boolean;
  itineraire: string;
}

function emptyPatrouilles(): Record<string, PatrouilleRow> {
  const map: Record<string, PatrouilleRow> = {};
  for (const type of PATROUILLE_TYPES) {
    for (const mode of PATROUILLE_MODES) {
      map[`${type.key}_${mode.key}`] = { selected: false, itineraire: "" };
    }
  }
  return map;
}

export function ActiviteForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const { user } = useAuthStore();
  const canEdit = hasPermission(user, SG_ACTIVITE_MODULE, "can_edit");

  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(isEdit);
  const [form, setForm] = useState<ActiviteInput>({
    date_activite: "",
    heure_activite: "",
    operation_ciblee: "",
    faits_constates: "",
    compte_rendu_hierarchie: "",
    conduite_a_tenir: "",
    nature_intervention: "",
    suites_donnees: "",
  });
  const [patrouilles, setPatrouilles] = useState<Record<string, PatrouilleRow>>(emptyPatrouilles);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [existingAttachments, setExistingAttachments] = useState<
    { id: number; title: string; original_filename: string; mime_type?: string | null }[]
  >([]);
  const [attachmentsToDelete, setAttachmentsToDelete] = useState<number[]>([]);
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [isCaptureOpen, setIsCaptureOpen] = useState(false);
  const [viewerTarget, setViewerTarget] = useState<{ url: string; title: string } | null>(null);

  useEffect(() => {
    if (!isEdit) return;
    (async () => {
      try {
        const e = await getActiviteById(Number(id));
        setForm({
          date_activite: e.date_activite?.substring(0, 10) ?? "",
          heure_activite: e.heure_activite ?? "",
          operation_ciblee: e.operation_ciblee ?? "",
          faits_constates: e.faits_constates ?? "",
          compte_rendu_hierarchie: e.compte_rendu_hierarchie ?? "",
          conduite_a_tenir: e.conduite_a_tenir ?? "",
          nature_intervention: e.nature_intervention ?? "",
          suites_donnees: e.suites_donnees ?? "",
        });
        // Restore patrol selections from stored itineraries
        // (null = mode not selected).
        const rows = emptyPatrouilles();
        for (const type of PATROUILLE_TYPES) {
          for (const mode of PATROUILLE_MODES) {
            const value = e[patrouilleItineraireField(type.key, mode.key)];
            if (value !== null) {
              rows[`${type.key}_${mode.key}`] = { selected: true, itineraire: value };
            }
          }
        }
        setPatrouilles(rows);
        const atts = await getActiviteAttachments(Number(id));
        setExistingAttachments(atts.map((a) => ({ id: a.id, title: a.title, original_filename: a.original_filename, mime_type: a.mime_type })));
      } catch {
        addNotification("error", "Erreur", "Impossible de charger l'activité");
        navigate("/sg/activites");
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!form.date_activite) errs.date_activite = "La date est requise";
    if (!form.heure_activite) errs.heure_activite = "L'heure est requise";
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  function setPatrouilleRow(key: string, patch: Partial<PatrouilleRow>) {
    setPatrouilles((prev) => ({ ...prev, [key]: { ...prev[key], ...patch } }));
  }

  async function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    if (!validate()) return;
    setSaving(true);

    // Map patrol selections to itinerary columns — null when the mode
    // is not selected, itinerary text (possibly empty) when selected.
    const payload: ActiviteInput = { ...form };
    for (const type of PATROUILLE_TYPES) {
      for (const mode of PATROUILLE_MODES) {
        const row = patrouilles[`${type.key}_${mode.key}`];
        payload[patrouilleItineraireField(type.key, mode.key)] = row?.selected ? row.itineraire : null;
      }
    }

    try {
      let savedId: number;
      if (isEdit) {
        const updated = await updateActivite(Number(id), payload);
        savedId = updated.id;
      } else {
        const created = await createActivite(payload);
        savedId = created.id;
      }

      // Delete marked attachments
      for (const attId of attachmentsToDelete) {
        try {
          await deleteActiviteAttachment(savedId, attId);
        } catch {
          /* best-effort */
        }
      }

      // Update titles of existing attachments
      for (const att of existingAttachments) {
        if (!attachmentsToDelete.includes(att.id)) {
          try {
            await updateActiviteAttachmentTitle(savedId, att.id, att.title);
          } catch {
            /* best-effort */
          }
        }
      }

      // Upload new attachments
      for (const pf of pendingFiles) {
        try {
          await createActiviteAttachment(savedId, pf.title || pf.file.name, pf.file);
        } catch {
          addNotification("error", "Upload", `Impossible d'uploader ${pf.file.name}`);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Enregistrée",
        `L'activité a été ${isEdit ? "modifiée" : "enregistrée"} avec succès`,
      );
      navigate("/sg/activites");
    } catch (err) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      addNotification("error", "Erreur", axiosErr?.response?.data?.message || "Erreur lors de l'enregistrement");
    } finally {
      setSaving(false);
    }
  }

  function removePendingFile(index: number) {
    setPendingFiles((prev) => {
      const target = prev[index];
      if (target?.previewUrl) URL.revokeObjectURL(target.previewUrl);
      return prev.filter((_, i) => i !== index);
    });
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/sg/activites")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isEdit ? "Modifier l'activité" : "Nouvelle activité"}
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Patrouilles et interventions
          </p>
        </div>
      </div>

      <motion.form
        onSubmit={handleSubmit}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        {/* ── Informations générales ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="date_activite">Date *</Label>
              <Input
                id="date_activite"
                type="date"
                value={form.date_activite}
                onChange={(e) => setForm({ ...form, date_activite: e.target.value })}
                aria-invalid={!!errors.date_activite}
              />
              {errors.date_activite && (
                <p className="text-sm text-destructive">{errors.date_activite}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="heure_activite">Heure *</Label>
              <Input
                id="heure_activite"
                type="time"
                value={form.heure_activite}
                onChange={(e) => setForm({ ...form, heure_activite: e.target.value })}
                aria-invalid={!!errors.heure_activite}
              />
              {errors.heure_activite && (
                <p className="text-sm text-destructive">{errors.heure_activite}</p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* ── Type de patrouille ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Type de patrouille</CardTitle>
            <p className="text-sm text-muted-foreground">
              Sélectionnez les modes de patrouille et renseignez leur itinéraire
            </p>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              {PATROUILLE_TYPES.map((type) => (
                <div key={type.key} className="space-y-3">
                  <h3 className="font-semibold text-sm text-primary">{type.label}</h3>
                  {PATROUILLE_MODES.map((mode) => {
                    const key = `${type.key}_${mode.key}`;
                    const row = patrouilles[key];
                    return (
                      <div key={mode.key} className="space-y-2">
                        <label className="flex items-center gap-2 cursor-pointer select-none">
                          <input
                            type="checkbox"
                            className="h-4 w-4 rounded border-input accent-primary"
                            checked={row.selected}
                            onChange={(e) => setPatrouilleRow(key, { selected: e.target.checked })}
                          />
                          <span className="text-sm font-medium">{mode.label}</span>
                        </label>
                        {row.selected && (
                          <div className="pl-6">
                            <Input
                              value={row.itineraire}
                              onChange={(e) => setPatrouilleRow(key, { itineraire: e.target.value })}
                              placeholder="Itinéraire"
                              aria-label={`Itinéraire ${type.label} ${mode.label}`}
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* ── Opération & faits ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Opération &amp; faits</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="operation_ciblee">Opération ciblée</Label>
              <Input
                id="operation_ciblee"
                value={form.operation_ciblee ?? ""}
                onChange={(e) => setForm({ ...form, operation_ciblee: e.target.value })}
                placeholder="Ex : Contrôle CIN, Contrôle débit de boissons"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="faits_constates">Faits constatés</Label>
              <Textarea
                id="faits_constates"
                value={form.faits_constates ?? ""}
                onChange={(e) => setForm({ ...form, faits_constates: e.target.value })}
                rows={4}
                placeholder="Faits observés durant la patrouille ou l'opération"
              />
            </div>
          </CardContent>
        </Card>

        {/* ── Hiérarchie ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Hiérarchie</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="compte_rendu_hierarchie">Compte-rendu hiérarchie</Label>
              <Textarea
                id="compte_rendu_hierarchie"
                value={form.compte_rendu_hierarchie ?? ""}
                onChange={(e) => setForm({ ...form, compte_rendu_hierarchie: e.target.value })}
                rows={4}
                placeholder="Compte-rendu temps réel à l'Autorité et/ou au second"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="conduite_a_tenir">Conduite à tenir</Label>
              <Textarea
                id="conduite_a_tenir"
                value={form.conduite_a_tenir ?? ""}
                onChange={(e) => setForm({ ...form, conduite_a_tenir: e.target.value })}
                rows={4}
                placeholder="Instructions données par l'Autorité et/ou le second"
              />
            </div>
          </CardContent>
        </Card>

        {/* ── Intervention ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Intervention</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="nature_intervention">Nature de l'intervention</Label>
              <Input
                id="nature_intervention"
                value={form.nature_intervention ?? ""}
                onChange={(e) => setForm({ ...form, nature_intervention: e.target.value })}
                placeholder="Ex : Tapage nocturne, Braquage en cours, Intervention police-secours"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="suites_donnees">Suites données</Label>
              <Textarea
                id="suites_donnees"
                value={form.suites_donnees ?? ""}
                onChange={(e) => setForm({ ...form, suites_donnees: e.target.value })}
                rows={4}
                placeholder="Ex : Conduite au Poste pour examen de situation, Interpellation, RAS"
              />
            </div>
          </CardContent>
        </Card>

        {/* ── Pièces jointes ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Pièces jointes</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {existingAttachments
              .filter((a) => !attachmentsToDelete.includes(a.id))
              .map((att) => {
                const isImage = att.mime_type?.startsWith("image/");
                return (
                  <div key={att.id} className="flex items-center gap-2">
                    <Paperclip className="h-4 w-4 shrink-0 text-muted-foreground" />
                    <Input
                      value={att.title}
                      onChange={(e) =>
                        setExistingAttachments((prev) =>
                          prev.map((a) => (a.id === att.id ? { ...a, title: e.target.value } : a)),
                        )
                      }
                      className="flex-1"
                    />
                    <span className="text-xs text-muted-foreground shrink-0">
                      {att.original_filename}
                    </span>
                    {isImage && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 shrink-0"
                        title="Aperçu"
                        onClick={() =>
                          setViewerTarget({
                            url: getActiviteAttachmentDownloadUrl(Number(id), att.id),
                            title: att.title || att.original_filename,
                          })
                        }
                      >
                        <ImageIcon className="h-3.5 w-3.5" />
                      </Button>
                    )}
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 shrink-0"
                      onClick={() => setAttachmentsToDelete((prev) => [...prev, att.id])}
                    >
                      <Trash2 className="h-3.5 w-3.5 text-destructive" />
                    </Button>
                  </div>
                );
              })}

            {pendingFiles.map((pf, i) => {
              const isImage = pf.file.type.startsWith("image/");
              return (
                <div key={i} className="flex items-center gap-2">
                  {isImage && pf.previewUrl ? (
                    <button
                      type="button"
                      className="h-9 w-9 shrink-0 rounded-md border overflow-hidden"
                      title="Aperçu"
                      onClick={() => setViewerTarget({ url: pf.previewUrl!, title: pf.title || pf.file.name })}
                    >
                      <img src={pf.previewUrl} alt="" className="h-full w-full object-cover" />
                    </button>
                  ) : (
                    <Paperclip className="h-4 w-4 shrink-0 text-muted-foreground" />
                  )}
                  <Input
                    value={pf.title}
                    onChange={(e) =>
                      setPendingFiles((prev) =>
                        prev.map((p, j) => (j === i ? { ...p, title: e.target.value } : p)),
                      )
                    }
                    placeholder="Titre"
                    className="flex-1"
                  />
                  <span className="text-xs text-muted-foreground shrink-0">{pf.file.name}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 shrink-0"
                    onClick={() => removePendingFile(i)}
                  >
                    <Trash2 className="h-3.5 w-3.5 text-destructive" />
                  </Button>
                </div>
              );
            })}

            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  const input = document.createElement("input");
                  input.type = "file";
                  input.onchange = () => {
                    const f = input.files?.[0];
                    if (f) {
                      const previewUrl = f.type.startsWith("image/") ? URL.createObjectURL(f) : undefined;
                      setPendingFiles((prev) => [...prev, { file: f, title: f.name, previewUrl }]);
                    }
                  };
                  input.click();
                }}
              >
                <Plus className="h-4 w-4 mr-2" />
                Ajouter un fichier
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsCaptureOpen(true)}
              >
                <Camera className="h-4 w-4 mr-2" />
                Prendre une photo
              </Button>
            </div>
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="submit" disabled={saving || (isEdit && !canEdit)}>
            {saving ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate("/sg/activites")}>
            Annuler
          </Button>
        </div>
      </motion.form>

      <PhotoCaptureDialog
        open={isCaptureOpen}
        onClose={() => setIsCaptureOpen(false)}
        onPhotoComplete={(photoData) => {
          setIsCaptureOpen(false);
          const byteString = atob(photoData.split(",")[1]);
          const bytes = new Uint8Array(byteString.length);
          for (let i = 0; i < byteString.length; i++) bytes[i] = byteString.charCodeAt(i);
          const blob = new Blob([bytes], { type: "image/jpeg" });
          const file = new File([blob], `photo_${Date.now()}.jpg`, { type: "image/jpeg" });
          setPendingFiles((prev) => [...prev, { file, title: "Photo activité", previewUrl: photoData }]);
        }}
        squareCrop={false}
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={viewerTarget?.url ?? ""}
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </div>
  );
}
