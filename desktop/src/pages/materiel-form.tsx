import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getAffectationMaterielById,
  createAffectationMateriel,
  updateAffectationMateriel,
  getTypeMaterielList,
  type AffectationMaterielPayload,
  type AffectationMaterielLignePayload,
} from "@/lib/api/materiel";
import { getPersonnelList, verifyPersonnelCodeSecret } from "@/lib/api/personnel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { SignaturePadDialog } from "@/components/signature/signature-pad-dialog";
import { strokesToSvg, type Stroke } from "@/stores/signature-pad-store";
import { ArrowLeft, Loader2, Save, Plus, Trash2, Package, UserCheck, Calendar, KeyRound, CheckCircle2, PenLine } from "lucide-react";
import type { TypeMateriel, Personnel } from "@/types";

const LIST_PATH = "/sedentaire/poste/materiels";

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

interface LigneForm {
  type_materiel_id: number;
  etat_emport: string;
}

export function MaterielForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const { user } = useAuthStore();

  const [form, setForm] = useState({
    agent_personnel_id: 0,
    date_perception: todayIso(),
    heure_perception: nowTime(),
    observations: "",
  });
  const [lignes, setLignes] = useState<LigneForm[]>([
    { type_materiel_id: 0, etat_emport: "" },
  ]);
  const [personnelList, setPersonnelList] = useState<Personnel[]>([]);
  const [typeList, setTypeList] = useState<TypeMateriel[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  // Agent verification (code secret) — create only.
  const [codeSecret, setCodeSecret] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [verified, setVerified] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  // Signature SVG (optional, captured after verification).
  const [signatureSvg, setSignatureSvg] = useState<string | null>(null);
  const [signatureFromPersonnel, setSignatureFromPersonnel] = useState(false);
  const [showSignaturePad, setShowSignaturePad] = useState(false);

  const canEdit = hasPermission(user, "sedentaire_poste_materiels", "can_edit");

  useEffect(() => {
    loadOptions();
    if (isEdit) loadAffectation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadOptions() {
    try {
      const [personnel, types] = await Promise.all([
        getPersonnelList(),
        getTypeMaterielList(),
      ]);
      setPersonnelList(personnel);
      setTypeList(types);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les options");
    }
  }

  async function loadAffectation() {
    setLoading(true);
    try {
      const data = await getAffectationMaterielById(Number(id));
      setForm({
        agent_personnel_id: data.agent_personnel_id,
        date_perception: data.date_perception,
        heure_perception: data.heure_perception.slice(0, 5),
        observations: data.observations ?? "",
      });
      setLignes(
        (data.lignes ?? []).map((l) => ({
          type_materiel_id: l.type_materiel_id,
          etat_emport: l.etat_emport ?? "",
        })),
      );
      setVerified(!!data.agent_verifie);
      setSignatureSvg(data.signature_svg);
    } catch {
      addNotification("error", "Erreur", "Affectation introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  // ─── Agent verification (code secret) ─────────────────────────────
  async function handleVerifyCode() {
    if (!form.agent_personnel_id) {
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
        form.agent_personnel_id,
        codeSecret.trim(),
      );
      if (result.verified) {
        setVerified(true);
        setVerifyError(null);
        // Pull the signature from the personnel's existing data.
        const personnel = personnelList.find(
          (p) => p.id === form.agent_personnel_id,
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
    setForm({ ...form, agent_personnel_id: personnelId });
    setVerified(false);
    setVerifyError(null);
    setCodeSecret("");
    setSignatureSvg(null);
    setSignatureFromPersonnel(false);
  }

  function updateLigne(index: number, field: keyof LigneForm, value: string | number) {
    setLignes((prev) =>
      prev.map((l, i) => (i === index ? { ...l, [field]: value } : l)),
    );
  }

  function addLigne() {
    setLignes((prev) => [
      ...prev,
      { type_materiel_id: 0, etat_emport: "" },
    ]);
  }

  function removeLigne(index: number) {
    setLignes((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    if (form.agent_personnel_id <= 0) {
      addNotification("error", "Erreur", "L'agent est requis");
      return;
    }
    if (!form.date_perception || !form.heure_perception) {
      addNotification("error", "Erreur", "La date et l'heure de perception sont requises");
      return;
    }
    if (lignes.length === 0) {
      addNotification("error", "Erreur", "Au moins un type de matériel est requis");
      return;
    }
    for (let i = 0; i < lignes.length; i++) {
      if (lignes[i].type_materiel_id <= 0) {
        addNotification("error", "Erreur", `Ligne ${i + 1} : le type de matériel est requis`);
        return;
      }
    }

    // On create, the agent must be verified via code secret.
    if (!isEdit && !verified) {
      addNotification("error", "Erreur", "L'identité de l'agent doit être vérifiée via le code secret avant d'enregistrer l'affectation");
      return;
    }

    setSaving(true);
    try {
      const payload: AffectationMaterielPayload = {
        agent_personnel_id: form.agent_personnel_id,
        date_perception: form.date_perception,
        heure_perception: form.heure_perception,
        observations: form.observations.trim() || null,
        lignes: lignes.map<AffectationMaterielLignePayload>((l) => ({
          type_materiel_id: l.type_materiel_id,
          etat_emport: l.etat_emport.trim() || null,
        })),
        // On create, include the code secret + optional signature.
        ...(isEdit
          ? {}
          : {
              code_secret: codeSecret.trim(),
              signature_svg: signatureSvg,
            }),
      };

      if (isEdit) {
        await updateAffectationMateriel(Number(id), payload);
        addNotification("success", "Modifiée", "Affectation modifiée avec succès");
      } else {
        await createAffectationMateriel(payload);
        addNotification("success", "Créée", "Affectation enregistrée avec succès");
      }
      navigate(LIST_PATH);
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer l'affectation";
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
            {isEdit ? "Modifier l'affectation" : "Nouvelle affectation de matériel"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Affecter un ou plusieurs matériels à un agent
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Agent information + verification + signature */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <UserCheck className="h-4 w-4" />
              Agent utilisateur
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="agent_personnel_id">Agent *</Label>
              <Select
                id="agent_personnel_id"
                value={form.agent_personnel_id === 0 ? "" : String(form.agent_personnel_id)}
                onChange={(e) => handleAgentChange(Number(e.target.value))}
                options={personnelList.map((p) => ({
                  value: String(p.id),
                  label: `${p.lastname} ${p.firstname} (${p.im}) — ${p.grade}`,
                }))}
                placeholder="Sélectionner un agent"
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
                  <p className="text-sm font-semibold">Vérification de l'identité</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    L'agent doit fournir son code secret pour confirmer son identité avant la remise du matériel.
                  </p>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="code_secret" className="text-sm font-medium">
                    Code secret de l'agent
                  </Label>
                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        id="code_secret"
                        type="password"
                        value={codeSecret}
                        onChange={(e) => setCodeSecret(e.target.value)}
                        disabled={verified || !form.agent_personnel_id}
                        className="pl-9"
                        placeholder="••••••"
                      />
                    </div>
                    {!verified && (
                      <Button
                        type="button"
                        onClick={handleVerifyCode}
                        disabled={verifying || !form.agent_personnel_id || !codeSecret.trim()}
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
                      Signature de l'agent (optionnel)
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {signatureFromPersonnel
                        ? "Signature récupérée depuis les données du personnel. Vous pouvez la garder ou en dessiner une nouvelle."
                        : signatureSvg
                          ? "Signature dessinée pour cette affectation."
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
          </CardContent>
        </Card>

        {/* Perception */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Calendar className="h-4 w-4" />
              Perception / Affectation
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

        {/* Material lines (multi-select) */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base flex items-center gap-2">
                <Package className="h-4 w-4" />
                Matériels ({lignes.length})
              </CardTitle>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addLigne}
                className="gap-2"
              >
                <Plus className="h-3.5 w-3.5" />
                Ajouter un matériel
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {lignes.length === 0 && (
              <p className="text-sm text-muted-foreground text-center py-4">
                Aucun matériel ajouté. Cliquez sur « Ajouter un matériel ».
              </p>
            )}
            {lignes.map((ligne, index) => (
              <div
                key={index}
                className="rounded-lg border border-border p-4 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Matériel #{index + 1}</span>
                  {lignes.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 text-destructive"
                      onClick={() => removeLigne(index)}
                      title="Retirer ce matériel"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="space-y-2">
                    <Label htmlFor={`ligne-${index}-type`}>Type de matériel *</Label>
                    <Select
                      id={`ligne-${index}-type`}
                      value={ligne.type_materiel_id === 0 ? "" : String(ligne.type_materiel_id)}
                      onChange={(e) =>
                        updateLigne(index, "type_materiel_id", Number(e.target.value))
                      }
                      options={typeList.map((t) => ({
                        value: String(t.id),
                        label: t.nom,
                      }))}
                      placeholder="Sélectionner un type"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor={`ligne-${index}-etat`}>État à l'emport (optionnel)</Label>
                    <Input
                      id={`ligne-${index}-etat`}
                      value={ligne.etat_emport}
                      onChange={(e) =>
                        updateLigne(index, "etat_emport", e.target.value)
                      }
                      placeholder="Ex : Bon, Neuf, Moyen..."
                    />
                  </div>
                </div>
              </div>
            ))}
            {typeList.length === 0 && (
              <p className="text-xs text-muted-foreground">
                Aucun type de matériel enregistré. Créez-en un depuis l'onglet « Types de matériel ».
              </p>
            )}
          </CardContent>
        </Card>

        {/* Observations */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Observations</CardTitle>
          </CardHeader>
          <CardContent>
            <textarea
              value={form.observations}
              onChange={(e) => setForm({ ...form, observations: e.target.value })}
              rows={3}
              className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              placeholder="Observations optionnelles..."
            />
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
