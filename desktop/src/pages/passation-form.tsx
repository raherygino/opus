import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPassationById,
  createPassation,
  updatePassation,
  getPassationAttachments,
  createPassationAttachment,
  updatePassationAttachmentTitle,
  deletePassationAttachment,
  getPassationAttachmentDownloadUrl,
} from "@/lib/api/passation";
import { verifyIdentity } from "@/lib/api/auth";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import {
  ArrowLeft,
  Save,
  Loader2,
  Handshake,
  ShieldCheck,
  UserCheck,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
  Lock,
} from "lucide-react";
import type { PassationAttachment, VerifiedIdentity } from "@/types";
import type { PassationPayload } from "@/lib/api/passation";

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

const LIST_PATH = "/sedentaire/poste/passation";

export function PassationForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<PassationPayload>({
    date_passation: todayIso(),
    heure_passation: nowTime(),
    chef_montant_user_id: 0,
    chef_montant_grade: "",
    chef_montant_lastname: "",
    instructions_autorite: "",
    incidents_survenus: "",
  });
  const [montantIdentity, setMontantIdentity] = useState<VerifiedIdentity | null>(null);
  const [montantUsername, setMontantUsername] = useState("");
  const [montantPassword, setMontantPassword] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (isEdit) {
      loadPassation();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadPassation() {
    setLoading(true);
    try {
      const p = await getPassationById(Number(id));
      setForm({
        date_passation: p.date_passation.slice(0, 10),
        heure_passation: p.heure_passation.slice(0, 5),
        // Chef identities are snapshot/readonly on edit — keep them so the
        // payload round-trips, but the backend ignores chef_* on update.
        chef_montant_user_id: p.chef_montant_user_id ?? 0,
        chef_montant_grade: p.chef_montant_grade ?? "",
        chef_montant_lastname: p.chef_montant_lastname ?? "",
        instructions_autorite: p.instructions_autorite ?? "",
        incidents_survenus: p.incidents_survenus ?? "",
      });
      setMontantIdentity({
        id: p.chef_montant_user_id ?? 0,
        username: p.chef_montant_username ?? "",
        grade: p.chef_montant_grade,
        firstname: null,
        lastname: p.chef_montant_lastname,
      });

      const atts = await getPassationAttachments(Number(id));
      setAttachments(
        atts.map((a: PassationAttachment) => ({
          id: a.id,
          title: a.title,
          existingFile: a.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Passation introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  // Authenticate the chef de poste montant via /auth/verify — credentials are
  // checked against the backend, the password is never stored, and only the
  // identity (grade + lastname) is kept for the passation record.
  async function handleVerifyMontant(e: React.FormEvent) {
    e.preventDefault();
    if (!montantUsername.trim() || !montantPassword) {
      addNotification("error", "Erreur", "Identifiant et mot de passe requis");
      return;
    }
    setVerifying(true);
    try {
      const identity = await verifyIdentity(montantUsername.trim(), montantPassword);
      setMontantIdentity(identity);
      setForm((prev) => ({
        ...prev,
        chef_montant_user_id: identity.id,
        chef_montant_grade: identity.grade ?? "",
        // Store the full name (firstname + lastname) in the lastname field
        // so both chefs display consistently with a single name column.
        chef_montant_lastname: [identity.firstname, identity.lastname]
          .filter(Boolean)
          .join(" ")
          .trim(),
      }));
      // Clear the password immediately — never keep it in state after verify.
      setMontantPassword("");
      addNotification("success", "Vérifié", "Chef de poste montant authentifié");
    } catch (err: unknown) {
      let msg = "Identifiant ou mot de passe invalide";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
      setMontantIdentity(null);
    } finally {
      setVerifying(false);
    }
  }

  function resetMontant() {
    setMontantIdentity(null);
    setMontantUsername("");
    setMontantPassword("");
    setForm((prev) => ({
      ...prev,
      chef_montant_user_id: 0,
      chef_montant_grade: "",
      chef_montant_lastname: "",
    }));
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
      ...(current && !current.title.trim() ? { title: file.name.replace(/\.[^.]+$/, "") } : {}),
    });
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.date_passation || !form.heure_passation) {
      addNotification("error", "Erreur", "La date et l'heure de la passation sont requises");
      return;
    }
    // The chef montant MUST be authenticated before the passation can be
    // submitted — prevent submission with an unverified montant.
    if (!isEdit && (!montantIdentity || !form.chef_montant_user_id)) {
      addNotification("error", "Erreur", "Le chef de poste montant doit être authentifié");
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
      let passationId: number;

      if (isEdit) {
        passationId = Number(id);
        // On update, only the editable fields are sent (chef identities are
        // stripped server-side).
        await updatePassation(passationId, {
          date_passation: form.date_passation,
          heure_passation: form.heure_passation,
          instructions_autorite: form.instructions_autorite,
          incidents_survenus: form.incidents_survenus,
        } as Partial<PassationPayload>);
      } else {
        const created = await createPassation(form);
        passationId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deletePassationAttachment(passationId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deletePassationAttachment(passationId, a.id);
            await createPassationAttachment(passationId, a.title, a.file);
          } else if (a.title) {
            await updatePassationAttachmentTitle(passationId, a.id, a.title);
          }
        } else if (a.file) {
          await createPassationAttachment(passationId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Passation mise à jour avec succès"
          : "Passation enregistrée avec succès",
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

  const descendantDisplay =
    [user?.grade, user?.firstname, user?.lastname].filter(Boolean).join(" ") ||
    user?.username ||
    "—";

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
            {isEdit ? "Modifier la passation" : "Nouvelle passation"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de la passation"
              : "Enregistrer une passation de poste"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Handshake className="h-4 w-4" />
              Passation
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_passation">Date de la passation *</Label>
                <Input
                  id="date_passation"
                  type="date"
                  value={form.date_passation}
                  onChange={(e) =>
                    setForm({ ...form, date_passation: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure_passation">Heure de la passation *</Label>
                <Input
                  id="heure_passation"
                  type="time"
                  value={form.heure_passation}
                  onChange={(e) =>
                    setForm({ ...form, heure_passation: e.target.value })
                  }
                  required
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Chef de poste descendant — read-only, from the authenticated user */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <UserCheck className="h-4 w-4" />
              Chef de poste descendant
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="space-y-2">
              <Label>Grade + Nom complet</Label>
              <Input value={descendantDisplay} readOnly disabled />
              <p className="text-xs text-muted-foreground">
                Renseigné automatiquement à partir de l'utilisateur connecté. Non modifiable.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Chef de poste montant — authenticate via /auth/verify */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="h-4 w-4" />
              Chef de poste montant
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {montantIdentity ? (
              <div className="space-y-3">
                <div className="rounded-lg border border-border bg-muted/30 p-3 space-y-1">
                  <p className="text-xs text-muted-foreground">Identité vérifiée</p>
                  <p className="text-sm font-medium">
                    {[montantIdentity.grade, montantIdentity.firstname, montantIdentity.lastname]
                      .filter(Boolean)
                      .join(" ") || montantIdentity.username}
                  </p>
                  <p className="text-xs text-muted-foreground">@{montantIdentity.username}</p>
                </div>
                {!isEdit && (
                  <Button type="button" variant="outline" size="sm" onClick={resetMontant}>
                    Changer d'utilisateur
                  </Button>
                )}
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-sm text-muted-foreground">
                  Le chef de poste montant doit s'authentifier avec son identifiant et mot de passe.
                  Le mot de passe n'est jamais enregistré.
                </p>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-2">
                    <Label htmlFor="montant_username">Identifiant</Label>
                    <Input
                      id="montant_username"
                      value={montantUsername}
                      onChange={(e) => setMontantUsername(e.target.value)}
                      placeholder="Identifiant du chef montant"
                      autoComplete="off"
                      disabled={isEdit}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="montant_password">Mot de passe</Label>
                    <Input
                      id="montant_password"
                      type="password"
                      value={montantPassword}
                      onChange={(e) => setMontantPassword(e.target.value)}
                      placeholder="Mot de passe"
                      autoComplete="off"
                      disabled={isEdit}
                    />
                  </div>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  className="gap-2"
                  onClick={handleVerifyMontant}
                  disabled={verifying || isEdit}
                >
                  {verifying ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Lock className="h-4 w-4" />
                  )}
                  {verifying ? "Vérification..." : "Authentifier"}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              Instructions & Incidents
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="instructions_autorite">Instructions Autorité</Label>
              <textarea
                id="instructions_autorite"
                value={form.instructions_autorite}
                onChange={(e) =>
                  setForm({ ...form, instructions_autorite: e.target.value })
                }
                rows={3}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="Instructions laissées par l'autorité..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="incidents_survenus">Incidents survenus</Label>
              <textarea
                id="incidents_survenus"
                value={form.incidents_survenus}
                onChange={(e) =>
                  setForm({ ...form, incidents_survenus: e.target.value })
                }
                rows={3}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="Incidents survenus pendant le poste..."
              />
            </div>
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

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {attachments.filter((a) => !a._delete).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
          )}

          {attachments.map((att, index) =>
            att._delete ? null : (
              <div
                key={att.id || `attachment-${index}`}
                className="flex items-center gap-3 rounded-lg border border-border p-3"
              >
                <div className="flex-1 space-y-1">
                  <Input
                    placeholder="Titre de la pièce jointe"
                    value={att.title}
                    onChange={(e) => updateAttachment(index, { title: e.target.value })}
                    className="h-8 text-sm"
                  />
                  {att.id && att.existingFile && id && (
                    <a
                      href={getPassationAttachmentDownloadUrl(Number(id), att.id)}
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
            Ajouter une pièce jointe
          </Button>
        </CardContent>
      </Card>

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
            ? getPassationAttachmentDownloadUrl(Number(id), viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
