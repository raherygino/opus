import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getArmementById,
  createArmement,
  updateArmement,
  getArmementAttachments,
  createArmementAttachment,
  updateArmementAttachmentTitle,
  deleteArmementAttachment,
  getArmementAttachmentDownloadUrl,
} from "@/lib/api/armement";
import { getPersonnelList, verifyPersonnelCodeSecret } from "@/lib/api/personnel";
import { isImageFile } from "@/lib/utils/attachment";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PhotoCaptureDialog } from "@/components/photo/photo-capture-dialog";
import { SignaturePadDialog } from "@/components/signature/signature-pad-dialog";
import { strokesToSvg, type Stroke } from "@/stores/signature-pad-store";
import {
  ArrowLeft,
  Save,
  Loader2,
  ShieldEllipsis,
  UserCheck,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Smartphone,
  Eye,
  KeyRound,
  CheckCircle2,
  XCircle,
  PenTool,
} from "lucide-react";
import type { ArmementAttachment, Personnel } from "@/types";
import type { ArmementPayload } from "@/lib/api/armement";

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

const LIST_PATH = "/sedentaire/poste/armement";

export function ArmementForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<ArmementPayload>({
    date_perception: todayIso(),
    heure_perception: nowTime(),
    agent_preneur_personnel_id: 0,
    type_arme: "",
    matricule_arme: "",
    munitions: null,
    secteur_mission: "",
    etat_perception: "",
  });
  const [personnelList, setPersonnelList] = useState<Personnel[]>([]);
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [photoPadIndex, setPhotoPadIndex] = useState<number | null>(null);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // Agent verification state — only used on create (the perception workflow).
  // On edit, the verification was already done at perception time and cannot
  // be modified.
  const [codeSecret, setCodeSecret] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [verified, setVerified] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [signaturePadOpen, setSignaturePadOpen] = useState(false);
  // The signature SVG — either pulled from the personnel's existing data
  // after verification, or captured via the signature pad. The user can
  // always choose to draw a new one to override.
  const [signatureSvg, setSignatureSvg] = useState<string | null>(null);
  // Whether the current signature came from the personnel's data (vs drawn).
  const [signatureFromPersonnel, setSignatureFromPersonnel] = useState(false);

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadData() {
    setLoading(true);
    try {
      const personnel = await getPersonnelList();
      setPersonnelList(personnel);

      if (isEdit) {
        const a = await getArmementById(Number(id));
        setForm({
          date_perception: a.date_perception.slice(0, 10),
          heure_perception: a.heure_perception.slice(0, 5),
          agent_preneur_personnel_id: a.agent_preneur_personnel_id ?? 0,
          type_arme: a.type_arme ?? "",
          matricule_arme: a.matricule_arme ?? "",
          munitions: a.munitions,
          secteur_mission: a.secteur_mission ?? "",
          etat_perception: a.etat_perception ?? "",
        });

        const atts = await getArmementAttachments(Number(id));
        setAttachments(
          atts.map((x: ArmementAttachment) => ({
            id: x.id,
            title: x.title,
            existingFile: x.original_filename,
          })),
        );
      }
    } catch {
      addNotification("error", "Erreur", isEdit ? "Armement introuvable" : "Impossible de charger le personnel");
      if (isEdit) navigate(LIST_PATH);
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

  // ─── Agent verification (code secret) ─────────────────────────────
  // When the agent preneur is selected, the user must enter the agent's
  // code secret and verify it before the perception can be created. This
  // confirms the person receiving the weapon is the actual personnel.
  async function handleVerifyCode() {
    if (!form.agent_preneur_personnel_id) {
      setVerifyError("Sélectionnez d'abord un agent");
      return;
    }
    if (!codeSecret.trim()) {
      setVerifyError("Saisissez le code secret de l'agent");
      return;
    }
    setVerifying(true);
    setVerifyError(null);
    try {
      const result = await verifyPersonnelCodeSecret(
        form.agent_preneur_personnel_id,
        codeSecret.trim(),
      );
      if (result.verified) {
        setVerified(true);
        setVerifyError(null);
        // After successful verification, pull the signature directly from
        // the personnel's existing data. If they have one, it becomes the
        // default signature for this perception. The user can still choose
        // to draw a new one to override.
        const personnel = personnelList.find(
          (p) => p.id === form.agent_preneur_personnel_id,
        );
        if (personnel?.signature_svg) {
          setSignatureSvg(personnel.signature_svg);
          setSignatureFromPersonnel(true);
        } else {
          setSignatureSvg(null);
          setSignatureFromPersonnel(false);
        }
      } else {
        setVerified(false);
        setVerifyError("Code secret incorrect. L'identité de l'agent n'a pas pu être vérifiée.");
      }
    } catch {
      setVerified(false);
      setVerifyError("Erreur lors de la vérification du code secret");
    } finally {
      setVerifying(false);
    }
  }

  // When the agent changes, reset the verification state.
  function handleAgentChange(personnelId: number) {
    setForm({ ...form, agent_preneur_personnel_id: personnelId });
    setVerified(false);
    setVerifyError(null);
    setCodeSecret("");
    setSignatureSvg(null);
    setSignatureFromPersonnel(false);
  }

  function handleSignatureComplete(strokes: Stroke[]) {
    setSignatureSvg(strokesToSvg(strokes));
    setSignatureFromPersonnel(false);
    setSignaturePadOpen(false);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (!form.date_perception || !form.heure_perception) {
      addNotification("error", "Erreur", "La date et l'heure de la perception sont requises");
      return;
    }
    if (!form.agent_preneur_personnel_id) {
      addNotification("error", "Erreur", "L'agent preneur est requis");
      return;
    }
    // On create, the agent must be verified via code secret before the
    // perception can be created. On edit, verification was done at
    // perception time and cannot be modified.
    if (!isEdit && !verified) {
      addNotification("error", "Erreur", "L'identité de l'agent doit être vérifiée via le code secret avant d'enregistrer la perception");
      return;
    }
    if (!form.type_arme.trim() || !form.matricule_arme.trim()) {
      addNotification("error", "Erreur", "Le type et le matricule de l'arme sont requis");
      return;
    }
    if (form.munitions !== null && (!Number.isInteger(form.munitions) || form.munitions < 0)) {
      addNotification("error", "Erreur", "Les munitions doivent être un nombre entier positif");
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
      let armementId: number;

      if (isEdit) {
        armementId = Number(id);
        // On update, only the perception fields are sent — the reintegration
        // and verification/signature columns are one-way and cannot be modified.
        await updateArmement(armementId, form);
      } else {
        // On create, include the code secret (verified server-side) and the
        // signature SVG (captured after verification).
        const payload: ArmementPayload = {
          ...form,
          code_secret: codeSecret.trim(),
          signature_svg: signatureSvg,
        };
        const created = await createArmement(payload);
        armementId = created.id;
      }

      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteArmementAttachment(armementId, a.id!);
      }

      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteArmementAttachment(armementId, a.id);
            await createArmementAttachment(armementId, a.title, a.file);
          } else if (a.title) {
            await updateArmementAttachmentTitle(armementId, a.id, a.title);
          }
        } else if (a.file) {
          await createArmementAttachment(armementId, a.title, a.file);
        }
      }

      addNotification(
        "success",
        isEdit ? "Modifiée" : "Créée",
        isEdit
          ? "Perception mise à jour avec succès"
          : "Perception enregistrée avec succès",
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
            {isEdit ? "Modifier la perception" : "Nouvelle perception"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {isEdit
              ? "Modifier les informations de la perception"
              : "Enregistrer la perception d'une arme"}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldEllipsis className="h-4 w-4" />
              Perception
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="date_perception">Date de la perception *</Label>
                <Input
                  id="date_perception"
                  type="date"
                  value={form.date_perception}
                  onChange={(e) =>
                    setForm({ ...form, date_perception: e.target.value })
                  }
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure_perception">Heure de la perception *</Label>
                <Input
                  id="heure_perception"
                  type="time"
                  value={form.heure_perception}
                  onChange={(e) =>
                    setForm({ ...form, heure_perception: e.target.value })
                  }
                  required
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="type_arme">Type d'arme *</Label>
                <Input
                  id="type_arme"
                  value={form.type_arme}
                  onChange={(e) =>
                    setForm({ ...form, type_arme: e.target.value })
                  }
                  placeholder="Ex : Pistolet PA 9mm"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="matricule_arme">Matricule de l'arme *</Label>
                <Input
                  id="matricule_arme"
                  value={form.matricule_arme}
                  onChange={(e) =>
                    setForm({ ...form, matricule_arme: e.target.value })
                  }
                  placeholder="Ex : PA-0001"
                  required
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="munitions">Munitions</Label>
                <Input
                  id="munitions"
                  type="number"
                  min={0}
                  value={form.munitions ?? ""}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      munitions: e.target.value === "" ? null : Number(e.target.value),
                    })
                  }
                  placeholder="Nombre de munitions perçues"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="secteur_mission">Secteur / Mission</Label>
                <Input
                  id="secteur_mission"
                  value={form.secteur_mission}
                  onChange={(e) =>
                    setForm({ ...form, secteur_mission: e.target.value })
                  }
                  placeholder="Ex : Patrouille Centre-ville"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="etat_perception">État à la perception</Label>
              <textarea
                id="etat_perception"
                value={form.etat_perception}
                onChange={(e) =>
                  setForm({ ...form, etat_perception: e.target.value })
                }
                rows={2}
                className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                placeholder="État de l'arme lors de la perception..."
              />
            </div>
          </CardContent>
        </Card>

        {/* Agent preneur — selected from the personnel list, with code secret
            verification and signature capture (create only). On edit, the
            verification was done at perception time and is read-only. */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <UserCheck className="h-4 w-4" />
              Agent preneur
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="agent_preneur_personnel_id">Agent *</Label>
              <Select
                id="agent_preneur_personnel_id"
                value={String(form.agent_preneur_personnel_id || "")}
                onChange={(e) => handleAgentChange(Number(e.target.value))}
                options={personnelList.map((p) => ({
                  value: String(p.id),
                  label: `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`,
                }))}
                placeholder="Sélectionner un agent"
                required
                disabled={isEdit}
              />
              <p className="text-xs text-muted-foreground">
                L'identité de l'agent (IM, grade, nom) est enregistrée au moment de la perception.
              </p>
            </div>

            {/* Verification status (edit mode) */}
            {isEdit && (
              <div className="flex items-center gap-2 rounded-lg border border-border bg-muted/30 p-3">
                {verified ? (
                  <>
                    <CheckCircle2 className="h-4 w-4 text-green-600" />
                    <span className="text-sm">Identité de l'agent vérifiée au moment de la perception</span>
                  </>
                ) : (
                  <>
                    <XCircle className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm text-muted-foreground">
                      L'identité de l'agent n'a pas été vérifiée (enregistrée avant la fonctionnalité de vérification)
                    </span>
                  </>
                )}
              </div>
            )}

            {/* Verification step (create mode only) */}
            {!isEdit && (
              <>
                <div className="rounded-lg border border-border bg-muted/30 p-4 space-y-3">
                  <div className="flex items-center gap-2">
                    <KeyRound className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm font-medium">Vérification de l'identité</span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    L'agent doit fournir son code secret pour confirmer son identité avant la remise de l'arme.
                  </p>
                  <div className="flex items-center gap-2">
                    <Input
                      type="password"
                      value={codeSecret}
                      onChange={(e) => setCodeSecret(e.target.value)}
                      placeholder="Code secret de l'agent"
                      autoComplete="off"
                      disabled={verified || !form.agent_preneur_personnel_id}
                      className="flex-1"
                      onKeyDown={(e) => {
                        if (e.key === "Enter" && !verified && form.agent_preneur_personnel_id) {
                          e.preventDefault();
                          handleVerifyCode();
                        }
                      }}
                    />
                    {verified ? (
                      <div className="flex items-center gap-2 text-sm text-green-600 font-medium">
                        <CheckCircle2 className="h-4 w-4" />
                        Vérifié
                      </div>
                    ) : (
                      <Button
                        type="button"
                        onClick={handleVerifyCode}
                        disabled={verifying || !form.agent_preneur_personnel_id || !codeSecret.trim()}
                        className="gap-2"
                      >
                        {verifying ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <KeyRound className="h-4 w-4" />
                        )}
                        Vérifier
                      </Button>
                    )}
                  </div>
                  {verifyError && (
                    <div className="flex items-center gap-2 text-sm text-destructive">
                      <XCircle className="h-4 w-4" />
                      {verifyError}
                    </div>
                  )}
                </div>

                {/* Signature capture (after verification).
                    After verification, the signature is pulled from the
                    personnel's existing data. If they have none, the user
                    can capture one. The user can always choose to draw a
                    new one to override. */}
                {verified && (
                  <div className="rounded-lg border border-border bg-muted/30 p-4 space-y-3">
                    <div className="flex items-center gap-2">
                      <PenTool className="h-4 w-4 text-muted-foreground" />
                      <span className="text-sm font-medium">Signature de l'agent</span>
                    </div>
                    <p className="text-xs text-muted-foreground">
                      {signatureFromPersonnel
                        ? "Signature récupérée depuis les données du personnel. Vous pouvez la garder ou en dessiner une nouvelle."
                        : signatureSvg
                          ? "Signature dessinée pour cette perception."
                          : "Aucune signature dans les données du personnel. Vous pouvez en capturer une ou laisser vide."}
                    </p>
                    {signatureSvg ? (
                      <div className="space-y-2">
                        <div
                          className="rounded-lg border-2 border-dashed border-border bg-white p-2"
                          dangerouslySetInnerHTML={{ __html: signatureSvg }}
                        />
                        <div className="flex items-center gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setSignaturePadOpen(true)}
                            className="gap-2"
                          >
                            <PenTool className="h-3.5 w-3.5" />
                            Refaire la signature
                          </Button>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setSignatureSvg(null);
                              setSignatureFromPersonnel(false);
                            }}
                            className="text-destructive"
                          >
                            Supprimer
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setSignaturePadOpen(true)}
                        className="gap-2"
                      >
                        <PenTool className="h-4 w-4" />
                        Capturer la signature
                      </Button>
                    )}
                  </div>
                )}
              </>
            )}
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
                      href={getArmementAttachmentDownloadUrl(Number(id), att.id)}
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

      <SignaturePadDialog
        open={signaturePadOpen}
        onClose={() => setSignaturePadOpen(false)}
        onSignatureComplete={handleSignatureComplete}
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget && id
            ? getArmementAttachmentDownloadUrl(Number(id), viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
