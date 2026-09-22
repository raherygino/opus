import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRassemblementById,
  createRassemblement,
  updateRassemblement,
} from "@/lib/api/rassemblement-journalier";
import { getPersonnelCount } from "@/lib/api/personnel";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Save, ArrowLeft } from "lucide-react";
import {
  RepartitionSecteurTable,
  newRepartitionKey,
  REPARTITION_TYPES,
  type EditableRepartitionRow,
} from "@/components/rassemblement-journalier/repartition-secteur-table";
import type { RassemblementJournalierInput } from "@/types";

export function RassemblementJournalierForm() {
  const { id } = useParams();
  const editId = id ? Number(id) : 0;
  const isEdit = editId > 0;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  const [dateRassemblement, setDateRassemblement] = useState("");
  const [heureRassemblement, setHeureRassemblement] = useState("");
  const [brigadeService, setBrigadeService] = useState("");
  const [officierPermanence, setOfficierPermanence] = useState("");
  const [inspecteurPermanence, setInspecteurPermanence] = useState("");
  const [chefPoste, setChefPoste] = useState("");
  const [instructionsAutorite, setInstructionsAutorite] = useState("");

  // Situation de prise d'arme — stored directly on the rassemblement record.
  const [effectifTheorique, setEffectifTheorique] = useState(0);
  const [presentCount, setPresentCount] = useState(0);
  const [absentCount, setAbsentCount] = useState(0);
  const [motifAbsence, setMotifAbsence] = useState("");

  // Répartition par secteur — one row list, Diurne and Nocturne only differ by type.
  const [repartitions, setRepartitions] = useState<EditableRepartitionRow[]>([]);

  useEffect(() => {
    if (!isEdit) {
      // Start with one blank line per section.
      setRepartitions(
        REPARTITION_TYPES.map(({ type }) => ({
          _key: newRepartitionKey(),
          type,
          secteur: "",
          effectif_engage: "",
          chef_element_contact: "",
          controle_contact: "",
          materiels_armements: "",
          missions: "",
        })),
      );
      // Prefill "Effectif théorique" with the current personnel count.
      getPersonnelCount()
        .then(setEffectifTheorique)
        .catch(() =>
          addNotification(
            "error",
            "Effectif théorique",
            "Impossible de charger l'effectif théorique — saisissez-le manuellement",
          ),
        );
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    try {
      const data = await getRassemblementById(editId);
      setDateRassemblement(data.date_rassemblement);
      setHeureRassemblement(data.heure_rassemblement);
      setBrigadeService(data.brigade_service);
      setOfficierPermanence(data.officier_permanence || "");
      setInspecteurPermanence(data.inspecteur_permanence || "");
      setChefPoste(data.chef_poste || "");
      setInstructionsAutorite(data.instructions_autorite || "");
      setEffectifTheorique(data.effectif_theorique ?? 0);
      setPresentCount(data.present ?? 0);
      setAbsentCount(data.absent ?? 0);
      setMotifAbsence(data.motif_absence || "");
      setRepartitions(
        (data.repartitions ?? []).map((r) => ({
          _key: newRepartitionKey(),
          type: r.type,
          secteur: r.secteur,
          effectif_engage: r.effectif_engage || "",
          chef_element_contact: r.chef_element_contact || "",
          controle_contact: r.controle_contact || "",
          materiels_armements: r.materiels_armements || "",
          missions: r.missions || "",
        })),
      );
    } catch {
      addNotification("error", "Erreur", "Impossible de charger le rassemblement");
      navigate("/sg/rassemblement-journalier");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!dateRassemblement || !heureRassemblement || !brigadeService) {
      addNotification("error", "Validation", "Date, heure et brigade de service sont requis");
      return;
    }
    setSaving(true);
    // Skip rows the user left entirely blank.
    const filledRows = repartitions.filter((r) =>
      [r.secteur, r.effectif_engage, r.chef_element_contact, r.controle_contact, r.materiels_armements, r.missions]
        .some((v) => (v ?? "").trim() !== ""),
    );
    const payload: RassemblementJournalierInput = {
      date_rassemblement: dateRassemblement,
      heure_rassemblement: heureRassemblement,
      brigade_service: brigadeService,
      officier_permanence: officierPermanence || null,
      inspecteur_permanence: inspecteurPermanence || null,
      chef_poste: chefPoste || null,
      instructions_autorite: instructionsAutorite || null,
      effectif_theorique: effectifTheorique,
      present: presentCount,
      absent: absentCount,
      motif_absence: motifAbsence || null,
      repartitions: filledRows.map(({ _key, ...row }) => row),
    };
    try {
      if (isEdit) {
        await updateRassemblement(editId, payload);
        addNotification("success", "Enregistré", "Rassemblement modifié avec succès");
      } else {
        await createRassemblement(payload);
        addNotification("success", "Enregistré", "Rassemblement créé avec succès");
      }
      navigate("/sg/rassemblement-journalier");
    } catch {
      addNotification("error", "Erreur", "Impossible d'enregistrer le rassemblement");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Chargement...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/sg/rassemblement-journalier")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isEdit ? "Modifier le rassemblement" : "Nouveau rassemblement journalier"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Renseignez les informations du rassemblement et la répartition par secteur
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Main fields + situation de prise d'arme (integrated, single form) */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div className="space-y-2">
                <Label htmlFor="date">Date *</Label>
                <Input
                  id="date"
                  type="date"
                  value={dateRassemblement}
                  onChange={(e) => setDateRassemblement(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="heure">Heure *</Label>
                <Input
                  id="heure"
                  type="time"
                  value={heureRassemblement}
                  onChange={(e) => setHeureRassemblement(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="brigade">Brigade de service *</Label>
                <Input
                  id="brigade"
                  value={brigadeService}
                  onChange={(e) => setBrigadeService(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="officier">Officier de permanence</Label>
                <Input
                  id="officier"
                  value={officierPermanence}
                  onChange={(e) => setOfficierPermanence(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="inspecteur">Inspecteur de permanence</Label>
                <Input
                  id="inspecteur"
                  value={inspecteurPermanence}
                  onChange={(e) => setInspecteurPermanence(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="chef_poste">Chef de poste</Label>
                <Input
                  id="chef_poste"
                  value={chefPoste}
                  onChange={(e) => setChefPoste(e.target.value)}
                />
              </div>
            </div>

            {/* Situation de prise d'arme — part of the rassemblement record itself */}
            <div className="space-y-3 border-t pt-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Situation de prise d'arme
              </p>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <div className="space-y-2">
                  <Label htmlFor="effectif_theorique">Effectif théorique</Label>
                  <Input
                    id="effectif_theorique"
                    type="number"
                    min={0}
                    value={effectifTheorique}
                    onChange={(e) => setEffectifTheorique(Number(e.target.value) || 0)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="present">Présent</Label>
                  <Input
                    id="present"
                    type="number"
                    min={0}
                    value={presentCount}
                    onChange={(e) => setPresentCount(Number(e.target.value) || 0)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="absent">Absent</Label>
                  <Input
                    id="absent"
                    type="number"
                    min={0}
                    value={absentCount}
                    onChange={(e) => setAbsentCount(Number(e.target.value) || 0)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="motif_absence">Motif d'absence</Label>
                  <Input
                    id="motif_absence"
                    value={motifAbsence}
                    onChange={(e) => setMotifAbsence(e.target.value)}
                  />
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="instructions">Instructions de l'autorité</Label>
              <Textarea
                id="instructions"
                value={instructionsAutorite}
                onChange={(e) => setInstructionsAutorite(e.target.value)}
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        {/* Répartition par secteur — one unified editable table (Diurne / Nocturne) */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Répartition par secteur</CardTitle>
          </CardHeader>
          <CardContent>
            <RepartitionSecteurTable rows={repartitions} onChange={setRepartitions} />
          </CardContent>
        </Card>

        <div className="flex justify-end gap-3">
          <Button type="button" variant="outline" onClick={() => navigate("/sg/rassemblement-journalier")}>
            Annuler
          </Button>
          <Button type="submit" disabled={saving}>
            <Save className="h-4 w-4 mr-2" />
            {saving ? "Enregistrement..." : "Enregistrer"}
          </Button>
        </div>
      </form>
    </div>
  );
}
