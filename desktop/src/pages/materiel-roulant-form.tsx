import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMaterielRoulantById,
  createMaterielRoulant,
  updateMaterielRoulant,
  getMaterielRoulantAttachments,
  createMaterielRoulantAttachment,
  updateMaterielRoulantAttachmentTitle,
  deleteMaterielRoulantAttachment,
  getMaterielRoulantAttachmentDownloadUrl,
  type MaterielRoulantPayload,
} from "@/lib/api/materiel-roulant";
import { getPersonnelList, verifyPersonnelCodeSecret } from "@/lib/api/personnel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { SignaturePadDialog } from "@/components/signature/signature-pad-dialog";
import { strokesToSvg, type Stroke } from "@/stores/signature-pad-store";
import { ArrowLeft, Loader2, Save, Car, UserCheck, Calendar, Gauge, Fuel, KeyRound, CheckCircle2, PenLine, Paperclip, Plus, Trash2, Download } from "lucide-react";
import type { Personnel, MaterielRoulantType, MaterielRoulantAttachment } from "@/types";
import { MATERIEL_ROULANT_MODULE } from "@/pages/materiel-roulant-management";

const LIST_PATH = "/sedentaire/poste/materiel-roulant";

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

export function MaterielRoulantForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const { user } = useAuthStore();

  const [form, setForm] = useState({
    date_perception: todayIso(),
    heure_perception: nowTime(),
    type_materiel: "VHL" as MaterielRoulantType,
    numero_immatriculation: "",
    description_vehicule: "",
    agent_conducteur_personnel_id: 0,
    chef_de_bord_personnel_id: 0,
    kilometrage_depart: "",
    niveau_carburant_depart: "",
  });
  const [personnelList, setPersonnelList] = useState<Personnel[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  // Conducteur verification (code secret) — create only.
  const [codeSecret, setCodeSecret] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [verified, setVerified] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  // Signature SVG (optional, captured after verification).
  const [signatureSvg, setSignatureSvg] = useState<string | null>(null);
  const [signatureFromPersonnel, setSignatureFromPersonnel] = useState(false);
  const [showSignaturePad, setShowSignaturePad] = useState(false);
  // Attachments (files associated with the perception).
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);

  const canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, "can_edit");

  useEffect(() => {
    loadOptions();
    if (isEdit) loadItem();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadOptions() {
    try {
      const personnel = await getPersonnelList();
      setPersonnelList(personnel);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger la liste du personnel");
    }
  }

  async function loadItem() {
    setLoading(true);
    try {
      const data = await getMaterielRoulantById(Number(id));
      setForm({
        date_perception: data.date_perception,
        heure_perception: data.heure_perception.slice(0, 5),
        type_materiel: data.type_materiel,
        numero_immatriculation: data.numero_immatriculation ?? "",
        description_vehicule: data.description_vehicule ?? "",
        agent_conducteur_personnel_id: data.agent_conducteur_personnel_id ?? 0,
        chef_de_bord_personnel_id: data.chef_de_bord_personnel_id ?? 0,
        kilometrage_depart: data.kilometrage_depart ?? "",
        niveau_carburant_depart: data.niveau_carburant_depart ?? "",
      });
      setVerified(!!data.agent_verifie);
      setSignatureSvg(data.signature_svg ?? null);

      const atts = await getMaterielRoulantAttachments(Number(id));
      setAttachments(
        atts.map((x: MaterielRoulantAttachment) => ({
          id: x.id,
          title: x.title,
          existingFile: x.original_filename,
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Matériel roulant introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  // ─── Conducteur verification (code secret) ─────────────────────────────
  async function handleVerifyCode() {
    if (!form.agent_conducteur_personnel_id) {
      setVerifyError("Sélectionnez d'abord un agent conducteur");
      return;
    }
    if (!codeSecret.trim()) {
      setVerifyError("Saisissez le code secret du conducteur");
      return;
    }
    setVerifying(true);
    setVerifyError(null);
    try {
      const result = await verifyPersonnelCodeSecret(
        form.agent_conducteur_personnel_id,
        codeSecret.trim(),
      );
      if (result.verified) {
        setVerified(true);
        setVerifyError(null);
        // Pull the signature from the personnel's existing data.
        const personnel = personnelList.find(
          (p) => p.id === form.agent_conducteur_personnel_id,
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
        setVerifyError("Code secret incorrect. L'identité du conducteur n'a pas pu être vérifiée.");
      }
    } catch {
      setVerified(false);
      setVerifyError("Erreur lors de la vérification du code secret");
    } finally {
      setVerifying(false);
    }
  }

  // When the conducteur changes, reset the verification state.
  function handleConducteurChange(personnelId: number) {
    setForm({ ...form, agent_conducteur_personnel_id: personnelId });
    setVerified(false);
    setVerifyError(null);
    setCodeSecret("");
    setSignatureSvg(null);
    setSignatureFromPersonnel(false);
  }

  // ─── Attachments ─────────────────────────────────────────────────────
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

    if (form.agent_conducteur_personnel_id <= 0) {
      addNotification("error", "Erreur", "L'agent conducteur est requis");
      return;
    }
    if (!form.date_perception || !form.heure_perception) {
      addNotification("error", "Erreur", "La date et l'heure de perception sont requises");
      return;
    }
    // On create, the conducteur must be verified via code secret before the
    // perception can be created. On edit, verification was done at perception
    // time and cannot be modified.
    if (!isEdit && !verified) {
      addNotification("error", "Erreur", "L'identité du conducteur doit être vérifiée via le code secret avant d'enregistrer la perception");
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
      const payload: MaterielRoulantPayload = {
        date_perception: form.date_perception,
        heure_perception: form.heure_perception,
        type_materiel: form.type_materiel,
        numero_immatriculation: form.numero_immatriculation.trim() || null,
        description_vehicule: form.description_vehicule.trim() || null,
        agent_conducteur_personnel_id: form.agent_conducteur_personnel_id,
        chef_de_bord_personnel_id: form.chef_de_bord_personnel_id > 0
          ? form.chef_de_bord_personnel_id
          : null,
        kilometrage_depart: form.kilometrage_depart.trim() || null,
        niveau_carburant_depart: form.niveau_carburant_depart.trim() || null,
        // On create, include the code secret (verified server-side) and the
        // optional signature SVG. On edit, these are read-only.
        ...(isEdit
          ? {}
          : {
              code_secret: codeSecret.trim(),
              signature_svg: signatureSvg,
            }),
      };

      let materielRoulantId: number;
      if (isEdit) {
        materielRoulantId = Number(id);
        await updateMaterielRoulant(materielRoulantId, payload);
        addNotification("success", "Modifiée", "Perception modifiée avec succès");
      } else {
        const created = await createMaterielRoulant(payload);
        materielRoulantId = created.id;
        addNotification("success", "Créée", "Perception enregistrée avec succès");
      }

      // Persist attachment changes (create / update title / replace file / delete).
      for (const a of attachments.filter((x) => x._delete && x.id)) {
        await deleteMaterielRoulantAttachment(materielRoulantId, a.id!);
      }
      for (const a of attachments.filter((x) => !x._delete)) {
        if (a.id) {
          if (a.file) {
            await deleteMaterielRoulantAttachment(materielRoulantId, a.id);
            await createMaterielRoulantAttachment(materielRoulantId, a.title, a.file);
          } else if (a.title) {
            await updateMaterielRoulantAttachmentTitle(materielRoulantId, a.id, a.title);
          }
        } else if (a.file) {
          await createMaterielRoulantAttachment(materielRoulantId, a.title, a.file);
        }
      }

      navigate(LIST_PATH);
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer la perception";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          msg = Object.values(resp.data.errors).join(", ");
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
            {isEdit ? "Modifier la perception" : "Nouvelle perception de matériel roulant"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Enregistrer la remise d'un véhicule (VHL ou Moto) à un conducteur
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Véhicule */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Car className="h-4 w-4" />
              Véhicule
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="type_materiel">Type de véhicule *</Label>
              <Select
                id="type_materiel"
                value={form.type_materiel}
                onChange={(e) =>
                  setForm({ ...form, type_materiel: e.target.value as MaterielRoulantType })
                }
                options={[
                  { value: "VHL", label: "VHL" },
                  { value: "Moto", label: "Moto" },
                ]}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="numero_immatriculation">Numéro d'immatriculation</Label>
              <Input
                id="numero_immatriculation"
                type="text"
                maxLength={50}
                placeholder="Ex. 1234 AB 75"
                value={form.numero_immatriculation}
                onChange={(e) => setForm({ ...form, numero_immatriculation: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description_vehicule">Description du véhicule</Label>
              <Input
                id="description_vehicule"
                type="text"
                maxLength={255}
                placeholder="Ex. S.U.V 4x4, Berline, Nissan, Toyota..."
                value={form.description_vehicule}
                onChange={(e) => setForm({ ...form, description_vehicule: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                Type de carrosserie, marque et modèle du véhicule
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Conducteur + Chef de bord */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <UserCheck className="h-4 w-4" />
              Équipage
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="agent_conducteur_personnel_id">Agent conducteur *</Label>
              <Select
                id="agent_conducteur_personnel_id"
                value={form.agent_conducteur_personnel_id === 0 ? "" : String(form.agent_conducteur_personnel_id)}
                onChange={(e) =>
                  isEdit
                    ? setForm({ ...form, agent_conducteur_personnel_id: Number(e.target.value) })
                    : handleConducteurChange(Number(e.target.value))
                }
                options={personnelList.map((p) => ({
                  value: String(p.id),
                  label: `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`,
                }))}
                placeholder="Sélectionner un agent conducteur"
                required
              />
            </div>

            {/* Verification status (edit mode) — read-only */}
            {isEdit && (
              <div className="flex items-center gap-2 text-sm">
                {verified ? (
                  <>
                    <CheckCircle2 className="h-4 w-4 text-primary" />
                    <span className="text-foreground">
                      Identité vérifiée au moment de la perception
                    </span>
                  </>
                ) : (
                  <span className="text-muted-foreground">
                    Identité non vérifiée (enregistrée avant la fonctionnalité de vérification)
                  </span>
                )}
              </div>
            )}

            {/* Verification step (create mode only) */}
            {!isEdit && (
              <div className="space-y-3 rounded-lg border border-border p-4 bg-muted/30">
                <div>
                  <p className="text-sm font-semibold">Vérification de l'identité du conducteur</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Le conducteur doit fournir son code secret pour confirmer son identité avant la remise du véhicule.
                  </p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="code_secret" className="text-sm font-medium">
                    Code secret du conducteur
                  </Label>
                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="code_secret"
                        type="password"
                        value={codeSecret}
                        onChange={(e) => setCodeSecret(e.target.value)}
                        disabled={verified || !form.agent_conducteur_personnel_id}
                        className="pl-9"
                        placeholder="••••••"
                      />
                    </div>
                    {!verified && (
                      <Button
                        type="button"
                        onClick={handleVerifyCode}
                        disabled={verifying || !form.agent_conducteur_personnel_id || !codeSecret.trim()}
                        variant="default"
                        size="sm"
                      >
                        {verifying ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          "Vérifier"
                        )}
                      </Button>
                    )}
                  </div>
                </div>
                {verified && (
                  <div className="flex items-center gap-2 text-sm text-primary">
                    <CheckCircle2 className="h-4 w-4" />
                    <span className="font-medium">Vérifié</span>
                  </div>
                )}
                {verifyError && (
                  <p className="text-sm text-destructive">{verifyError}</p>
                )}

                {/* Signature capture (after verification, optional) */}
                {verified && (
                  <div className="space-y-2 pt-2 border-t border-border">
                    <p className="text-sm font-semibold">
                      Signature du conducteur (optionnel)
                    </p>
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
                          className="border border-border rounded-md bg-white p-2 h-28 flex items-center justify-center overflow-hidden [&>svg]:max-w-full [&>svg]:max-h-full [&>svg]:w-auto [&>svg]:h-auto"
                          dangerouslySetInnerHTML={{ __html: signatureSvg }}
                        />
                        <div className="flex gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => setShowSignaturePad(true)}
                            className="gap-2"
                          >
                            <PenLine className="h-3.5 w-3.5" />
                            Refaire
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
                        size="sm"
                        onClick={() => setShowSignaturePad(true)}
                        className="gap-2"
                      >
                        <PenLine className="h-3.5 w-3.5" />
                        Capturer la signature
                      </Button>
                    )}
                  </div>
                )}
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="chef_de_bord_personnel_id">Chef de bord (optionnel)</Label>
              <Select
                id="chef_de_bord_personnel_id"
                value={form.chef_de_bord_personnel_id === 0 ? "" : String(form.chef_de_bord_personnel_id)}
                onChange={(e) =>
                  setForm({ ...form, chef_de_bord_personnel_id: Number(e.target.value) })
                }
                options={personnelList.map((p) => ({
                  value: String(p.id),
                  label: `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`,
                }))}
                placeholder="Aucun chef de bord"
              />
            </div>
          </CardContent>
        </Card>

        {/* Perception */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Perception
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="date_perception">Date de la perception *</Label>
              <Input
                id="date_perception"
                type="date"
                value={form.date_perception}
                onChange={(e) => setForm({ ...form, date_perception: e.target.value })}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="heure_perception">Heure de la perception *</Label>
              <Input
                id="heure_perception"
                type="time"
                value={form.heure_perception}
                onChange={(e) => setForm({ ...form, heure_perception: e.target.value })}
                required
              />
            </div>
          </CardContent>
        </Card>

        {/* Départ (kilométrage + carburant) */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Gauge className="h-4 w-4" />
              Compteurs de départ (optionnel)
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="kilometrage_depart" className="flex items-center gap-1.5">
                <Gauge className="h-3.5 w-3.5" />
                Kilométrage de départ (km)
              </Label>
              <Input
                id="kilometrage_depart"
                type="number"
                step="0.1"
                min="0"
                value={form.kilometrage_depart}
                onChange={(e) => setForm({ ...form, kilometrage_depart: e.target.value })}
                placeholder="Ex : 12345.0"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="niveau_carburant_depart" className="flex items-center gap-1.5">
                <Fuel className="h-3.5 w-3.5" />
                Carburant de départ (0–100%)
              </Label>
              <Input
                id="niveau_carburant_depart"
                type="number"
                step="0.01"
                min="0"
                max="100"
                value={form.niveau_carburant_depart}
                onChange={(e) => setForm({ ...form, niveau_carburant_depart: e.target.value })}
                placeholder="Ex : 80.0"
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
                        href={getMaterielRoulantAttachmentDownloadUrl(Number(id), att.id)}
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
              Ajouter une pièce jointe
            </Button>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => navigate(LIST_PATH)}
            disabled={saving}
          >
            Annuler
          </Button>
          <Button type="submit" disabled={saving || (isEdit && !canEdit)} className="gap-2">
            {saving ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
        </div>
      </form>

      {showSignaturePad && (
        <SignaturePadDialog
          open={showSignaturePad}
          onClose={() => setShowSignaturePad(false)}
          onSignatureComplete={(strokes: Stroke[]) => {
            setSignatureSvg(strokesToSvg(strokes));
            setSignatureFromPersonnel(false);
            setShowSignaturePad(false);
          }}
        />
      )}
    </motion.div>
  );
}
