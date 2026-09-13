import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMaterielRoulantById,
  reintegrateMaterielRoulant,
  getMaterielRoulantAttachmentDownloadUrl,
  type ReintegrationMaterielRoulantPayload,
} from "@/lib/api/materiel-roulant";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Car,
  UserCheck,
  Gauge,
  Fuel,
  ShieldCheck,
  Wrench,
  AlertTriangle,
  CheckCircle2,
  Paperclip,
  Download,
  Eye,
} from "lucide-react";
import type { MaterielRoulant, MaterielRoulantAttachment } from "@/types";
import { formatDate, formatHeure } from "@/pages/passation-list";
import { MATERIEL_ROULANT_MODULE } from "@/pages/materiel-roulant-management";

const LIST_PATH = "/sedentaire/poste/materiel-roulant";

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

export function MaterielRoulantDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, MATERIEL_ROULANT_MODULE, "can_edit");

  const [item, setItem] = useState<MaterielRoulant | null>(null);
  const [loading, setLoading] = useState(true);
  const [reintOpen, setReintOpen] = useState(false);
  const [reintegrating, setReintegrating] = useState(false);
  const [reintForm, setReintForm] = useState({
    date_reintegration: "",
    heure_reintegration: "",
    kilometrage_retour: "",
    niveau_carburant_retour: "",
    observations_techniques: "",
    defaillances: "",
  });
  const [reintError, setReintError] = useState<string | null>(null);
  const [viewerTarget, setViewerTarget] = useState<MaterielRoulantAttachment | null>(null);

  useEffect(() => {
    loadItem();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadItem() {
    setLoading(true);
    try {
      const data = await getMaterielRoulantById(Number(id));
      setItem(data);
    } catch {
      addNotification("error", "Erreur", "Matériel roulant introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  function openReintegration() {
    setReintForm({
      date_reintegration: todayIso(),
      heure_reintegration: nowTime(),
      kilometrage_retour: item?.kilometrage_depart ?? "",
      niveau_carburant_retour: "",
      observations_techniques: "",
      defaillances: "",
    });
    setReintError(null);
    setReintOpen(true);
  }

  async function handleReintegrate(e: React.FormEvent) {
    e.preventDefault();
    if (!item) return;

    if (!reintForm.date_reintegration) {
      setReintError("La date de la réintégration est requise");
      return;
    }
    if (!reintForm.heure_reintegration) {
      setReintError("L'heure de la réintégration est requise");
      return;
    }
    if (reintForm.date_reintegration < item.date_perception) {
      setReintError("La date de réintégration ne peut pas être antérieure à la date de perception");
      return;
    }
    if (!reintForm.kilometrage_retour.trim()) {
      setReintError("Le kilométrage de retour est requis");
      return;
    }
    if (
      item.kilometrage_depart &&
      Number(reintForm.kilometrage_retour) < Number(item.kilometrage_depart)
    ) {
      setReintError("Le kilométrage de retour ne peut pas être inférieur au kilométrage de départ");
      return;
    }
    if (
      reintForm.observations_techniques.trim() &&
      reintForm.observations_techniques.trim() === reintForm.defaillances.trim()
    ) {
      setReintError("Les observations techniques et les défaillances doivent être distinctes");
      return;
    }

    setReintegrating(true);
    try {
      const payload: ReintegrationMaterielRoulantPayload = {
        date_reintegration: reintForm.date_reintegration,
        heure_reintegration: reintForm.heure_reintegration,
        kilometrage_retour: reintForm.kilometrage_retour.trim(),
        niveau_carburant_retour: reintForm.niveau_carburant_retour.trim() || null,
        observations_techniques: reintForm.observations_techniques.trim() || null,
        defaillances: reintForm.defaillances.trim() || null,
      };
      const updated = await reintegrateMaterielRoulant(item.id, payload);
      addNotification("success", "Réintégré", "Matériel roulant réintégré avec succès");
      setItem(updated);
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

  if (!item) return null;

  const isReintegre = item.statut === "Réintégré";
  const hasDefaillances = !!(item.defaillances ?? "").trim();

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
              {item.type_materiel} — {formatDate(item.date_perception)}
              {isReintegre ? (
                <Badge variant="secondary">Réintégré</Badge>
              ) : (
                <Badge>En service</Badge>
              )}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {formatHeure(item.heure_perception)} —{" "}
              {[item.agent_conducteur_grade, item.agent_conducteur_nom].filter(Boolean).join(" ")}
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
              onClick={() => navigate(`${LIST_PATH}/${item.id}/edit`)}
            >
              <Pencil className="h-4 w-4" />
              Modifier
            </Button>
          )}
        </div>
      </div>

      {/* Véhicule + Perception */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Car className="h-4 w-4" />
            Véhicule & Perception
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Type de véhicule" value={<Badge variant="secondary">{item.type_materiel}</Badge>} />
          <DetailRow label="Immatriculation" value={item.numero_immatriculation} />
          <DetailRow label="Description" value={item.description_vehicule} />
          <DetailRow label="Date de la perception" value={formatDate(item.date_perception)} />
          <DetailRow label="Heure de la perception" value={formatHeure(item.heure_perception)} />
        </CardContent>
      </Card>

      {/* Conducteur */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserCheck className="h-4 w-4" />
            Agent conducteur
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <DetailRow label="IM" value={item.agent_conducteur_im} />
            <DetailRow label="Grade" value={item.agent_conducteur_grade} />
            <DetailRow label="Nom complet" value={item.agent_conducteur_nom} />
          </div>
          <div className="flex items-center gap-2 text-sm">
            {item.agent_verifie ? (
              <>
                <CheckCircle2 className="h-4 w-4 text-primary" />
                <span className="text-foreground">
                  Identité vérifiée via code secret
                  {item.agent_verifie_at ? ` le ${formatDate(item.agent_verifie_at)} à ${formatHeure(item.agent_verifie_at)}` : ""}
                </span>
              </>
            ) : (
              <span className="text-muted-foreground">
                Identité non vérifiée (enregistrée avant la fonctionnalité de vérification)
              </span>
            )}
          </div>
          {item.signature_svg && (
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Signature du conducteur</p>
              <div
                className="border border-border rounded-md bg-white p-2 h-28 flex items-center justify-center overflow-hidden [&>svg]:max-w-full [&>svg]:max-h-full [&>svg]:w-auto [&>svg]:h-auto"
                dangerouslySetInnerHTML={{ __html: item.signature_svg }}
              />
            </div>
          )}
        </CardContent>
      </Card>

      {/* Chef de bord */}
      {item.chef_de_bord_personnel_id && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <UserCheck className="h-4 w-4" />
              Chef de bord
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <DetailRow label="IM" value={item.chef_de_bord_im} />
            <DetailRow label="Grade" value={item.chef_de_bord_grade} />
            <DetailRow label="Nom complet" value={item.chef_de_bord_nom} />
          </CardContent>
        </Card>
      )}

      {/* Compteurs de départ */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Gauge className="h-4 w-4" />
            Compteurs de départ
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Kilométrage de départ (km)" value={item.kilometrage_depart} />
          <DetailRow label="Carburant de départ (%)" value={item.niveau_carburant_depart} />
        </CardContent>
      </Card>

      {/* Réintégration */}
      {isReintegre && (
        <>
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <ShieldCheck className="h-4 w-4" />
                Réintégration
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-2 gap-4">
              <DetailRow label="Date de la réintégration" value={formatDate(item.date_reintegration)} />
              <DetailRow label="Heure de la réintégration" value={formatHeure(item.heure_reintegration)} />
              <DetailRow label="Kilométrage de retour (km)" value={item.kilometrage_retour} />
              <DetailRow label="Carburant de retour (%)" value={item.niveau_carburant_retour} />
            </CardContent>
          </Card>

          {/* Observations techniques */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Wrench className="h-4 w-4" />
                Observations techniques
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm whitespace-pre-wrap">
                {item.observations_techniques || "Aucune observation technique"}
              </p>
            </CardContent>
          </Card>

          {/* Défaillances */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <AlertTriangle className="h-4 w-4" />
                Défaillances signalées
              </CardTitle>
            </CardHeader>
            <CardContent>
              {hasDefaillances ? (
                <p className="text-sm whitespace-pre-wrap text-destructive">
                  {item.defaillances}
                </p>
              ) : (
                <p className="text-sm text-muted-foreground">
                  Aucune défaillance signalée
                </p>
              )}
            </CardContent>
          </Card>
        </>
      )}

      {/* Pièces jointes */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes ({item.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(item.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
          )}
          {(item.attachments ?? []).map((att: MaterielRoulantAttachment) => (
            <div
              key={att.id}
              className="flex items-center justify-between rounded-lg border border-border p-3"
            >
              <div>
                <p className="text-sm font-medium">{att.title}</p>
                <p className="text-xs text-muted-foreground">{att.original_filename}</p>
              </div>
              <div className="flex items-center gap-1">
                {isImageFile(att.mime_type, att.original_filename) && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    title="Aperçu"
                    onClick={() => setViewerTarget(att)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                )}
                <a
                  href={getMaterielRoulantAttachmentDownloadUrl(item.id, att.id)}
                  download
                >
                  <Button variant="ghost" size="icon" className="h-8 w-8" title="Télécharger">
                    <Download className="h-4 w-4" />
                  </Button>
                </a>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

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
                <p className="text-sm font-semibold">Réintégration du véhicule</p>
                <p className="text-sm text-muted-foreground">
                  {item.type_materiel} —{" "}
                  {[item.agent_conducteur_grade, item.agent_conducteur_nom].filter(Boolean).join(" ")}
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
                    value={reintForm.date_reintegration}
                    onChange={(e) =>
                      setReintForm({ ...reintForm, date_reintegration: e.target.value })
                    }
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reint_heure">Heure de la réintégration *</Label>
                  <Input
                    id="reint_heure"
                    type="time"
                    value={reintForm.heure_reintegration}
                    onChange={(e) =>
                      setReintForm({ ...reintForm, heure_reintegration: e.target.value })
                    }
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-2">
                  <Label htmlFor="reint_km">Kilométrage de retour (km) *</Label>
                  <Input
                    id="reint_km"
                    type="number"
                    step="0.1"
                    min="0"
                    value={reintForm.kilometrage_retour}
                    onChange={(e) =>
                      setReintForm({ ...reintForm, kilometrage_retour: e.target.value })
                    }
                    placeholder="Ex : 13100.0"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="reint_carburant" className="flex items-center gap-1.5">
                    <Fuel className="h-3.5 w-3.5" />
                    Carburant de retour (0–100%)
                  </Label>
                  <Input
                    id="reint_carburant"
                    type="number"
                    step="0.01"
                    min="0"
                    max="100"
                    value={reintForm.niveau_carburant_retour}
                    onChange={(e) =>
                      setReintForm({ ...reintForm, niveau_carburant_retour: e.target.value })
                    }
                    placeholder="Ex : 40.0"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="reint_obs">Observations techniques (optionnel)</Label>
                <textarea
                  value={reintForm.observations_techniques}
                  onChange={(e) =>
                    setReintForm({ ...reintForm, observations_techniques: e.target.value })
                  }
                  rows={2}
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Observations générales sur l'état du véhicule..."
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="reint_def">Défaillances signalées (optionnel)</Label>
                <textarea
                  value={reintForm.defaillances}
                  onChange={(e) =>
                    setReintForm({ ...reintForm, defaillances: e.target.value })
                  }
                  rows={2}
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Pannes, dommages ou anomalies constatés..."
                />
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

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget
            ? getMaterielRoulantAttachmentDownloadUrl(item.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
