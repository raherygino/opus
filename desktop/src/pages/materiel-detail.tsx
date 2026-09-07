import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getAffectationMaterielById,
  reintegrateAffectationMateriel,
  type ReintegrationMaterielPayload,
} from "@/lib/api/materiel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Package,
  UserCheck,
  Calendar,
  ShieldCheck,
  CheckCircle2,
} from "lucide-react";
import type { AffectationMateriel } from "@/types";
import { formatDate, formatHeure } from "@/pages/passation-list";
import { MATERIELS_MODULE } from "@/pages/materiels-management";

const LIST_PATH = "/sedentaire/poste/materiels";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
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

export function MaterielDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, MATERIELS_MODULE, "can_edit");

  const [affectation, setAffectation] = useState<AffectationMateriel | null>(null);
  const [loading, setLoading] = useState(true);
  const [reintOpen, setReintOpen] = useState(false);
  const [reintegrating, setReintegrating] = useState(false);
  // Reintegration form state
  const [reintDate, setReintDate] = useState("");
  const [reintHeure, setReintHeure] = useState("");
  const [reintEtats, setReintEtats] = useState<Record<number, string>>({});
  const [reintError, setReintError] = useState<string | null>(null);

  useEffect(() => {
    loadAffectation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadAffectation() {
    setLoading(true);
    try {
      const data = await getAffectationMaterielById(Number(id));
      setAffectation(data);
    } catch {
      addNotification("error", "Erreur", "Affectation introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  function openReintegration() {
    setReintDate(todayIso());
    setReintHeure(nowTime());
    setReintEtats({});
    setReintError(null);
    setReintOpen(true);
  }

  async function handleReintegrate(e: React.FormEvent) {
    e.preventDefault();
    if (!affectation) return;

    if (!reintDate) {
      setReintError("La date de la réintégration est requise");
      return;
    }
    if (!reintHeure) {
      setReintError("L'heure de la réintégration est requise");
      return;
    }
    // Business rule 5: reintegration date must not be earlier than perception date
    if (reintDate < affectation.date_perception) {
      setReintError("La date de réintégration ne peut pas être antérieure à la date de perception");
      return;
    }
    // Business rule 6: etat_reintegration required per line item
    const lignes = affectation.lignes ?? [];
    for (const l of lignes) {
      if (!(reintEtats[l.id] ?? "").trim()) {
        setReintError("L'état à la réintégration est requis pour chaque matériel");
        return;
      }
    }

    setReintegrating(true);
    try {
      const payload: ReintegrationMaterielPayload = {
        date_reintegration: reintDate,
        heure_reintegration: reintHeure,
        ligne_etats: Object.fromEntries(
          Object.entries(reintEtats).map(([k, v]) => [k, v.trim()]),
        ),
      };
      const updated = await reintegrateAffectationMateriel(affectation.id, payload);
      addNotification("success", "Réintégré", "Matériel réintégré avec succès");
      setAffectation(updated);
      setReintOpen(false);
    } catch (err: unknown) {
      let msg = "Erreur lors de la réintégration";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          msg = Object.values(resp.data.errors).join(", ");
        } else if (resp?.data?.message) {
          msg = resp.data.message;
        }
      }
      setReintError(msg);
    } finally {
      setReintegrating(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!affectation) return null;

  const isReintegre = affectation.statut === "Réintégré";
  const lignes = affectation.lignes ?? [];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight flex items-center gap-2">
              Affectation du {formatDate(affectation.date_perception)}
              {isReintegre ? (
                <Badge variant="secondary">Réintégré</Badge>
              ) : (
                <Badge>Assigné</Badge>
              )}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {formatHeure(affectation.heure_perception)} —{" "}
              {[affectation.agent_grade, affectation.agent_nom].filter(Boolean).join(" ")}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {canEdit && !isReintegre && (
            <Button className="gap-2" onClick={openReintegration}>
              <ShieldCheck className="h-4 w-4" />
              Réintégration
            </Button>
          )}
          {canEdit && (
            <Button
              variant="outline"
              className="gap-2"
              onClick={() => navigate(`${LIST_PATH}/${affectation.id}/edit`)}
            >
              <Pencil className="h-4 w-4" />
              Modifier
            </Button>
          )}
        </div>
      </div>

      {/* Perception */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Calendar className="h-4 w-4" />
            Perception
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de la perception" value={formatDate(affectation.date_perception)} />
          <DetailRow label="Heure de la perception" value={formatHeure(affectation.heure_perception)} />
          <DetailRow label="Observations" value={<span className="whitespace-pre-wrap">{affectation.observations}</span>} />
        </CardContent>
      </Card>

      {/* Agent */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserCheck className="h-4 w-4" />
            Agent utilisateur
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="IM" value={affectation.agent_im} />
          <DetailRow label="Grade" value={affectation.agent_grade} />
          <DetailRow label="Nom complet" value={affectation.agent_nom} />
        </CardContent>
      </Card>

      {/* Vérification de l'identité */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4" />
            Vérification de l'identité
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center gap-2 text-sm">
            {affectation.agent_verifie ? (
              <>
                <CheckCircle2 className="h-4 w-4 text-primary" />
                <span className="text-primary font-medium">
                  Identité vérifiée
                  {affectation.agent_verifie_at && (
                    <span className="text-muted-foreground font-normal ml-2">
                      le {new Date(affectation.agent_verifie_at).toLocaleString("fr-FR")}
                    </span>
                  )}
                </span>
              </>
            ) : (
              <span className="text-muted-foreground">
                Identité non vérifiée (enregistrée avant la fonctionnalité de vérification)
              </span>
            )}
          </div>
          {affectation.signature_svg && (
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Signature</p>
              <div
                className="border border-border rounded-md bg-white p-2 h-24 flex items-center justify-center overflow-hidden [&>svg]:max-w-full [&>svg]:max-h-full [&>svg]:w-auto [&>svg]:h-auto"
                dangerouslySetInnerHTML={{ __html: affectation.signature_svg }}
              />
            </div>
          )}
        </CardContent>
      </Card>

      {/* Matériels (line items) */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Package className="h-4 w-4" />
            Matériels ({lignes.length})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          {lignes.length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun matériel</p>
          )}
          {lignes.map((l) => (
            <div
              key={l.id}
              className="rounded-lg border border-border p-4 space-y-2"
            >
              <div className="grid grid-cols-2 gap-4">
                <DetailRow label="Type de matériel" value={l.type_materiel_nom} />
                <DetailRow label="ID Matériel" value={l.numero_materiel} />
                <DetailRow label="État à l'emport" value={l.etat_emport} />
                <DetailRow
                  label="État à la réintégration"
                  value={l.etat_reintegration}
                />
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Réintégration */}
      {isReintegre && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="h-4 w-4" />
              Réintégration
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <DetailRow label="Date de la réintégration" value={formatDate(affectation.date_reintegration)} />
            <DetailRow label="Heure de la réintégration" value={formatHeure(affectation.heure_reintegration)} />
          </CardContent>
        </Card>
      )}

      {/* Reintegration dialog */}
      {reintOpen && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={() => !reintegrating && setReintOpen(false)}
        >
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="relative z-50 w-full max-w-lg max-h-[90vh] overflow-y-auto rounded-xl border border-border bg-card p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start gap-4">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
                <ShieldCheck className="h-5 w-5" />
              </div>
              <div className="flex-1 space-y-1">
                <p className="text-sm font-semibold">Réintégration du matériel</p>
                <p className="text-sm text-muted-foreground">
                  {[affectation.agent_grade, affectation.agent_nom].filter(Boolean).join(" ")} —{" "}
                  {lignes.length} matériel(s)
                </p>
              </div>
            </div>

            <form onSubmit={handleReintegrate} className="mt-5 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="reint_date">Date de la réintégration *</Label>
                  <Input
                    id="reint_date"
                    type="date"
                    value={reintDate}
                    onChange={(e) => setReintDate(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reint_heure">Heure de la réintégration *</Label>
                  <Input
                    id="reint_heure"
                    type="time"
                    value={reintHeure}
                    onChange={(e) => setReintHeure(e.target.value)}
                    required
                  />
                </div>
              </div>

              {/* Per-line etat_reintegration */}
              <div className="space-y-3">
                <Label>État à la réintégration (par matériel) *</Label>
                {lignes.map((l) => (
                  <div key={l.id} className="space-y-1">
                    <p className="text-xs text-muted-foreground">
                      {l.type_materiel_nom} · {l.numero_materiel}
                    </p>
                    <Input
                      value={reintEtats[l.id] ?? ""}
                      onChange={(e) =>
                        setReintEtats((prev) => ({ ...prev, [l.id]: e.target.value }))
                      }
                      placeholder="Ex : Bon, Endommagé..."
                      required
                    />
                  </div>
                ))}
              </div>

              {reintError && <p className="text-sm text-destructive">{reintError}</p>}

              <div className="flex items-center justify-end gap-2 pt-1">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setReintOpen(false)}
                  disabled={reintegrating}
                >
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={reintegrating} className="gap-2">
                  {reintegrating ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
                  Réintégrer
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
