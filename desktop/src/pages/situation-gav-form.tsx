import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getSituationGavById,
  createSituationGav,
  updateSituationGav,
  getSituationGavAttachments,
  createSituationGavAttachment,
  updateSituationGavAttachmentTitle,
  deleteSituationGavAttachment,
  getSituationGavAttachmentDownloadUrl,
} from "@/lib/api/situation-gav";
import { getGardeAVueList } from "@/lib/api/garde-a-vue";
import { getPersonnelList } from "@/lib/api/personnel";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import {
  ArrowLeft,
  Save,
  Loader2,
  ClipboardCheck,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
} from "lucide-react";
import type { GardeAVue, Personnel, SituationGavAttachment } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

function nowDateTimeLocal(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}T${hh}:${mi}`;
}

/** "YYYY-MM-DD HH:MM:SS" (server) → "YYYY-MM-DDTHH:MM" (datetime-local input). */
function toDateTimeLocal(value: string | null | undefined): string {
  if (!value) return "";
  return value.slice(0, 16).replace(" ", "T");
}

function formatDate(date: string | null | undefined): string {
  if (!date) return "";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : "";
}

function gavLabel(g: GardeAVue): string {
  const name = [g.nom, g.prenoms].filter(Boolean).join(" ");
  const debut = formatDate(g.debut_gav);
  return debut ? `${name} — GAV du ${debut}` : name;
}

/** Agent display: grade + lastname. */
function agentLabel(p: Personnel): string {
  return `${p.grade} ${p.lastname}`.trim();
}

const LIST_PATH = "/sedentaire/poste/situation-gav";

export function SituationGavForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [gardeAVueId, setGardeAVueId] = useState("");
  const [dateControle, setDateControle] = useState(nowDateTimeLocal());
  const [agentControleId, setAgentControleId] = useState("");
  const [etatGeneral, setEtatGeneral] = useState("");
  const [observations, setObservations] = useState("");
  const [mesuresPrises, setMesuresPrises] = useState("");
  const [agent, setAgent] = useState<string | null>(null);

  const [gavOptions, setGavOptions] = useState<GardeAVue[]>([]);
  const [personnelOptions, setPersonnelOptions] = useState<Personnel[]>([]);

  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    getGardeAVueList().then(setGavOptions).catch(() => setGavOptions([]));
    getPersonnelList().then(setPersonnelOptions).catch(() => setPersonnelOptions([]));
    if (isEdit) {
      loadSituation();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSituation() {
    setLoading(true);
    try {
      const s = await getSituationGavById(Number(id));
      setGardeAVueId(String(s.garde_a_vue_id));
      setDateControle(toDateTimeLocal(s.date_controle));
      setAgentControleId(s.agent_controle_id ? String(s.agent_controle_id) : "");
      setEtatGeneral(s.etat_general ?? "");
      setObservations(s.observations ?? "");
      setMesuresPrises(s.mesures_prises ?? "");
      setAgent(
        [s.agent_prenoms, s.agent_nom].filter(Boolean).join(" ") ||
          s.agent_username ||
          null,
      );

      const atts = await getSituationGavAttachments(Number(id));
      setAttachments(
        atts.map((a: SituationGavAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Situation GAV introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  function addAttachment() {
    setAttachments((prev) => [...prev, { title: "", file: undefined }]);
  }

  function removeAttachment(index: number) {
    setAttachments((prev) => {
      const updated = [...prev];
      if (updated[index].id) {
        updated[index] = { ...updated[index], _delete: true };
      } else {
        updated.splice(index, 1);
      }
      return updated;
    });
  }

  function updateAttachment(index: number, data: Partial<AttachmentItem>) {
    setAttachments((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], ...data };
      return updated;
    });
  }

  // Phone photo capture (QR pairing) → the captured photo becomes the
  // attachment's file, exactly like the correspondance/personnel flow.
  async function handleAttachmentPhotoComplete(photoData: string) {
    const index = photoPadIndex;
    setPhotoPadIndex(null);
    if (index === null) return;

    const [meta, base64] = photoData.split(",");
    const mimeType = meta?.match(/:(.*?);/)?.[1] || "image/jpeg";
    const byteChars = atob(base64);
    const byteNumbers = new Array(byteChars.length);
    for (let i = 0; i < byteChars.length; i++) {
      byteNumbers[i] = byteChars.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: mimeType });
    const extension = mimeType === "image/png" ? "png" : "jpg";
    const file = new File([blob], `photo.${extension}`, { type: mimeType });

    const current = attachments[index];
    updateAttachment(index, {
      file,
      // Pre-fill the title when the row has none yet.
      ...(current && !current.title.trim() ? { title: file.name.replace(/\.[^.]+$/, "") } : {}),
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!gardeAVueId) {
      addNotification("error", "Erreur", "La personne concernée est requise");
      return;
    }
    if (!dateControle) {
      addNotification("error", "Erreur", "La date et l'heure du contrôle sont requises");
      return;
    }
    const incompleteAttachment = attachments.some(
      (a) => !a._delete && !a.id && (!a.title.trim() || !a.file),
    );
    if (incompleteAttachment) {
      addNotification("error", "Erreur", "Chaque pièce jointe doit avoir un titre et un fichier");
      return;
    }

    setSaving(true);

    try {
      const payload = {
        garde_a_vue_id: Number(gardeAVueId),
        date_controle: dateControle,
        agent_controle_id: agentControleId ? Number(agentControleId) : null,
        etat_general: etatGeneral.trim() || null,
        observations: observations.trim() || null,
        mesures_prises: mesuresPrises.trim() || null,
      };

      let situationId: number;
      if (isEdit) {
        situationId = Number(id);
        await updateSituationGav(situationId, payload);
      } else {
        const created = await createSituationGav(payload);
        situationId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteSituationGavAttachment(situationId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteSituationGavAttachment(situationId, a.id);
            await createSituationGavAttachment(situationId, a.title, a.file);
          } else if (a.title) {
            await updateSituationGavAttachmentTitle(situationId, a.id, a.title);
          }
        } else if (a.file) {
          await createSituationGavAttachment(situationId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Situation GAV mise à jour avec succès"
          : "Situation GAV enregistrée avec succès",
      );
      navigate(LIST_PATH);
    } catch (err: unknown) {
      let msg = "Erreur lors de l'enregistrement";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          const fieldErrors = Object.entries(resp.data.errors)
            .map(([field, error]) => `${field}: ${error}`)
            .join(", ");
          msg = fieldErrors;
        } else if (resp?.data?.message) {
          msg = resp.data.message;
        }
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier la situation GAV" : "Nouvelle situation GAV"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations du contrôle"
              : "Enregistrer un contrôle d'une personne en garde à vue"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ClipboardCheck className="h-4 w-4" />
              Contrôle
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="garde_a_vue_id">Personne concernée *</Label>
              <Select
                id="garde_a_vue_id"
                value={gardeAVueId}
                onChange={(e) => setGardeAVueId(e.target.value)}
                options={gavOptions.map((g) => ({ value: String(g.id), label: gavLabel(g) }))}
                placeholder="Sélectionner une personne en garde à vue"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_controle">Date et heure du contrôle *</Label>
                <Input
                  id="date_controle"
                  type="datetime-local"
                  value={dateControle}
                  onChange={(e) => setDateControle(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="agent_controle_id">Agent ayant effectué le contrôle</Label>
                <Select
                  id="agent_controle_id"
                  value={agentControleId}
                  onChange={(e) => setAgentControleId(e.target.value)}
                  options={[
                    { value: "", label: "—" },
                    ...personnelOptions.map((p) => ({ value: String(p.id), label: agentLabel(p) })),
                  ]}
                  placeholder="Sélectionner un agent"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="etat_general">État général de la personne</Label>
              <Input
                id="etat_general"
                value={etatGeneral}
                onChange={(e) => setEtatGeneral(e.target.value)}
                placeholder="Ex. Calme, bon état général, agité..."
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">Observations & mesures</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="observations">Observations</Label>
              <Textarea
                id="observations"
                value={observations}
                onChange={(e) => setObservations(e.target.value)}
                rows={3}
                placeholder="Observations ou informations complémentaires"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="mesures_prises">Mesures prises</Label>
              <Textarea
                id="mesures_prises"
                value={mesuresPrises}
                onChange={(e) => setMesuresPrises(e.target.value)}
                rows={3}
                placeholder="Mesures ou actions prises suite au contrôle"
              />
            </div>
            {isEdit && agent && (
              <div className="space-y-2">
                <Label>Enregistré par</Label>
                <Input value={agent} disabled />
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              Fichiers joints / Photos
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {attachments.filter((a) => !a._delete).length === 0 && (
              <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
            )}

            {attachments.map((att, index) =>
              att._delete ? null : (
                <div
                  key={att.id || `attachment-${index}`}
                  className="flex items-center gap-3 rounded-lg border border-border p-3"
                >
                  <div className="flex-1 space-y-1">
                    <Input
                      placeholder="Titre du fichier joint"
                      value={att.title}
                      onChange={(e) => updateAttachment(index, { title: e.target.value })}
                      className="h-8 text-sm"
                    />
                    {att.id && att.existingFile && id && (
                      <a
                        href={getSituationGavAttachmentDownloadUrl(Number(id), att.id)}
                        download
                        className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                      >
                        <Download className="h-3 w-3" />
                        {att.existingFile}
                      </a>
                    )}
                    {att.file && (
                      <p className="text-xs text-muted-foreground flex items-center gap-1">
                        <Paperclip className="h-3 w-3" />
                        {att.file.name}
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-1">
                    {att.id && att.existingFile && id && isImageFile(null, att.existingFile) && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        title="Aperçu"
                        onClick={() => setViewerTarget({ id: att.id!, title: att.title || att.existingFile! })}
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </Button>
                    )}
                    <Input
                      type="file"
                      className="w-40 h-8 text-xs"
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) updateAttachment(index, { file });
                      }}
                    />
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      title="Prendre une photo avec le téléphone"
                      onClick={() => setPhotoPadIndex(index)}
                    >
                      <Smartphone className="h-3.5 w-3.5" />
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-destructive"
                      onClick={() => removeAttachment(index)}
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                </div>
              ),
            )}

            <Button
              type="button"
              variant="outline"
              size="sm"
              className="gap-2"
              onClick={addAttachment}
            >
              <Plus className="h-4 w-4" />
              Ajouter un fichier joint
            </Button>
          </CardContent>
        </Card>

        <div className="flex items-center gap-3">
          <Button type="submit" className="gap-2" disabled={saving}>
            {saving ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {saving ? "Enregistrement..." : "Enregistrer"}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate(LIST_PATH)}>
            Annuler
          </Button>
        </div>
      </form>

      <PhotoCaptureDialog
        open={photoPadIndex !== null}
        onClose={() => setPhotoPadIndex(null)}
        onPhotoComplete={handleAttachmentPhotoComplete}
        squareCrop={false}
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget && id
            ? getSituationGavAttachmentDownloadUrl(Number(id), viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
