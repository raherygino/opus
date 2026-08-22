import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getCorrespondanceById,
  createCorrespondance,
  updateCorrespondance,
  getCorrespondanceAttachments,
  createCorrespondanceAttachment,
  updateCorrespondanceAttachmentTitle,
  deleteCorrespondanceAttachment,
  getCorrespondanceAttachmentDownloadUrl,
} from "@/lib/api/correspondance";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Save,
  Loader2,
  Repeat,
  Paperclip,
  Trash2,
  Download,
  Plus,
} from "lucide-react";
import type { Correspondance, CorrespondanceAttachment } from "@/types";
import type { CorrespondancePayload } from "@/lib/api/correspondance";

const SENS_OPTIONS = [
  { value: "Entrant", label: "Entrant" },
  { value: "Sortant", label: "Sortant" },
];

const STATUT_OPTIONS = [
  { value: "Enregistré", label: "Enregistré" },
  { value: "En traitement", label: "En traitement" },
  { value: "Traité", label: "Traité" },
  { value: "Archivé", label: "Archivé" },
];

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

function nowTime(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

const LIST_PATH = "/sedentaire/secretariat/correspondance";

export function CorrespondanceForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<Required<CorrespondancePayload>>({
    date_correspondance: todayIso(),
    heure_enregistrement: nowTime(),
    sens: "Entrant",
    reference: "",
    emetteur_destinataire: "",
    objet: "",
    statut: "Enregistré",
  });
  const [agent, setAgent] = useState<string | null>(null);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadCorrespondance();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadCorrespondance() {
    setLoading(true);
    try {
      const c = await getCorrespondanceById(Number(id));
      setForm({
        date_correspondance: c.date_correspondance.slice(0, 10),
        heure_enregistrement: c.heure_enregistrement.slice(0, 5),
        sens: c.sens,
        reference: c.reference,
        emetteur_destinataire: c.emetteur_destinataire,
        objet: c.objet,
        statut: c.statut,
      });
      setAgent(
        [c.agent_prenoms, c.agent_nom].filter(Boolean).join(" ") ||
          c.agent_username ||
          null,
      );

      const atts = await getCorrespondanceAttachments(Number(id));
      setAttachments(
        atts.map((a: CorrespondanceAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Correspondance introuvable");
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

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.date_correspondance || !form.heure_enregistrement) {
      addNotification("error", "Erreur", "La date et l'heure d'enregistrement sont requises");
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
      let correspondanceId: number;

      if (isEdit) {
        correspondanceId = Number(id);
        await updateCorrespondance(correspondanceId, form);
      } else {
        const created = await createCorrespondance(form);
        correspondanceId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteCorrespondanceAttachment(correspondanceId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteCorrespondanceAttachment(correspondanceId, a.id);
            await createCorrespondanceAttachment(correspondanceId, a.title, a.file);
          } else if (a.title) {
            await updateCorrespondanceAttachmentTitle(correspondanceId, a.id, a.title);
          }
        } else if (a.file) {
          await createCorrespondanceAttachment(correspondanceId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Correspondance mise à jour avec succès"
          : "Correspondance enregistrée avec succès",
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
            {isEdit ? "Modifier la correspondance" : "Nouvelle correspondance"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de la correspondance"
              : "Enregistrer un courrier entrant ou sortant"}
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Repeat className="h-4 w-4" />
            Informations de la correspondance
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_correspondance">Date *</Label>
                <Input
                  id="date_correspondance"
                  type="date"
                  value={form.date_correspondance}
                  onChange={(e) =>
                    setForm({ ...form, date_correspondance: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure_enregistrement">Heure d'enregistrement *</Label>
                <Input
                  id="heure_enregistrement"
                  type="time"
                  value={form.heure_enregistrement}
                  onChange={(e) =>
                    setForm({ ...form, heure_enregistrement: e.target.value })
                  }
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="sens">Sens *</Label>
                <Select
                  id="sens"
                  value={form.sens}
                  onChange={(e) =>
                    setForm({ ...form, sens: e.target.value as Correspondance["sens"] })
                  }
                  options={SENS_OPTIONS}
                  placeholder="Sélectionner le sens"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="reference">Numéro d'ordre / Référence *</Label>
                <Input
                  id="reference"
                  value={form.reference}
                  onChange={(e) => setForm({ ...form, reference: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="emetteur_destinataire">
                {form.sens === "Sortant" ? "Destinataire *" : "Émetteur *"}
              </Label>
              <Input
                id="emetteur_destinataire"
                value={form.emetteur_destinataire}
                onChange={(e) =>
                  setForm({ ...form, emetteur_destinataire: e.target.value })
                }
                placeholder={
                  form.sens === "Sortant"
                    ? "Personne, organisation ou service destinataire"
                    : "Personne, organisation ou service émetteur"
                }
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="objet">Objet *</Label>
              <Input
                id="objet"
                value={form.objet}
                onChange={(e) => setForm({ ...form, objet: e.target.value })}
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="statut">Statut</Label>
                <Select
                  id="statut"
                  value={form.statut}
                  onChange={(e) =>
                    setForm({ ...form, statut: e.target.value as Correspondance["statut"] })
                  }
                  options={STATUT_OPTIONS}
                />
              </div>
              {isEdit && agent && (
                <div className="space-y-2">
                  <Label>Agent secrétariat</Label>
                  <Input value={agent} disabled />
                </div>
              )}
            </div>

            <div className="flex items-center gap-3 pt-4">
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
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Fichiers joints
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
                      href={getCorrespondanceAttachmentDownloadUrl(Number(id), att.id)}
                      download
                      className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
                    >
                      <Download className="h-3 w-3" />
                      {att.existingFile}
                    </a>
                  )}
                </div>
                <div className="flex items-center gap-1">
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
    </motion.div>
  );
}
