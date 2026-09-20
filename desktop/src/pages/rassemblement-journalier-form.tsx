import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRassemblementById,
  createRassemblement,
  updateRassemblement,
} from "@/lib/api/rassemblement-journalier";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Trash2, Save, ArrowLeft } from "lucide-react";
import type {
  RassemblementJournalierInput,
  SituationPriseArmeInput,
  RepartitionSecteurInput,
} from "@/types";

const emptySituation: SituationPriseArmeInput = {
  effectif_theorique: 0,
  present: 0,
  absent: 0,
  motif_absence: "",
};

const emptyRepartition: RepartitionSecteurInput = {
  secteur: "",
  effectif_engage: "",
  chef_element_contact: "",
  controle_contact: "",
  materiels_armements: "",
  missions: "",
};

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

  const [situations, setSituations] = useState<SituationPriseArmeInput[]>([{ ...emptySituation }]);
  const [diurne, setDiurne] = useState<RepartitionSecteurInput[]>([{ ...emptyRepartition }]);
  const [nocturne, setNocturne] = useState<RepartitionSecteurInput[]>([{ ...emptyRepartition }]);

  useEffect(() => {
    if (!isEdit) return;
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
      setSituations(
        (data.situations_prise_arme ?? []).length > 0
          ? (data.situations_prise_arme ?? []).map((s) => ({
              effectif_theorique: s.effectif_theorique,
              present: s.present,
              absent: s.absent,
              motif_absence: s.motif_absence || "",
            }))
          : [{ ...emptySituation }],
      );
      setDiurne(
        (data.repartitions_diurne ?? []).length > 0
          ? (data.repartitions_diurne ?? []).map((r) => ({
              secteur: r.secteur,
              effectif_engage: r.effectif_engage || "",
              chef_element_contact: r.chef_element_contact || "",
              controle_contact: r.controle_contact || "",
              materiels_armements: r.materiels_armements || "",
              missions: r.missions || "",
            }))
          : [{ ...emptyRepartition }],
      );
      setNocturne(
        (data.repartitions_nocturne ?? []).length > 0
          ? (data.repartitions_nocturne ?? []).map((r) => ({
              secteur: r.secteur,
              effectif_engage: r.effectif_engage || "",
              chef_element_contact: r.chef_element_contact || "",
              controle_contact: r.controle_contact || "",
              materiels_armements: r.materiels_armements || "",
              missions: r.missions || "",
            }))
          : [{ ...emptyRepartition }],
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
    const payload: RassemblementJournalierInput = {
      date_rassemblement: dateRassemblement,
      heure_rassemblement: heureRassemblement,
      brigade_service: brigadeService,
      officier_permanence: officierPermanence || null,
      inspecteur_permanence: inspecteurPermanence || null,
      chef_poste: chefPoste || null,
      instructions_autorite: instructionsAutorite || null,
      situations_prise_arme: situations,
      repartitions_diurne: diurne,
      repartitions_nocturne: nocturne,
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
            Renseignez les informations du rassemblement et les tables associées
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Main fields */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
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
            <div className="space-y-2 sm:col-span-2 lg:col-span-3">
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

        {/* Situation de prise d'arme */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">Situation de prise d'arme</CardTitle>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setSituations([...situations, { ...emptySituation }])}
              >
                <Plus className="h-4 w-4 mr-1" />
                Ajouter
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-3">
            {situations.map((s, idx) => (
              <div key={idx} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5 items-end rounded-lg border p-3">
                <div className="space-y-1">
                  <Label>Effectif théorique</Label>
                  <Input
                    type="number"
                    min={0}
                    value={s.effectif_theorique}
                    onChange={(e) => updateSituation(idx, "effectif_theorique", Number(e.target.value))}
                  />
                </div>
                <div className="space-y-1">
                  <Label>Présent</Label>
                  <Input
                    type="number"
                    min={0}
                    value={s.present}
                    onChange={(e) => updateSituation(idx, "present", Number(e.target.value))}
                  />
                </div>
                <div className="space-y-1">
                  <Label>Absent</Label>
                  <Input
                    type="number"
                    min={0}
                    value={s.absent}
                    onChange={(e) => updateSituation(idx, "absent", Number(e.target.value))}
                  />
                </div>
                <div className="space-y-1 sm:col-span-2">
                  <Label>Motif d'absence</Label>
                  <Input
                    value={s.motif_absence || ""}
                    onChange={(e) => updateSituation(idx, "motif_absence", e.target.value)}
                  />
                </div>
                <div className="flex justify-end">
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => removeRow(situations, setSituations, idx)}
                    disabled={situations.length === 1}
                  >
                    <Trash2 className="h-4 w-4 text-destructive" />
                  </Button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Répartition par secteur – Diurne */}
        <RepartitionEditor
          title="Répartition par secteur – Diurne"
          rows={diurne}
          onChange={setDiurne}
        />

        {/* Répartition par secteur – Nocturne */}
        <RepartitionEditor
          title="Répartition par secteur – Nocturne"
          rows={nocturne}
          onChange={setNocturne}
        />

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

  function updateSituation(idx: number, field: keyof SituationPriseArmeInput, value: string | number) {
    setSituations((prev) => prev.map((s, i) => (i === idx ? { ...s, [field]: value } : s)));
  }
}

function removeRow<T>(arr: T[], setter: (rows: T[]) => void, idx: number) {
  setter(arr.filter((_, i) => i !== idx));
}

function RepartitionEditor({
  title,
  rows,
  onChange,
}: {
  title: string;
  rows: RepartitionSecteurInput[];
  onChange: (rows: RepartitionSecteurInput[]) => void;
}) {
  const fields: { key: keyof RepartitionSecteurInput; label: string; multiline?: boolean }[] = [
    { key: "secteur", label: "Secteur" },
    { key: "effectif_engage", label: "Effectif engagé" },
    { key: "chef_element_contact", label: "Chef d'élément avec contact" },
    { key: "controle_contact", label: "Contrôle avec contact" },
    { key: "materiels_armements", label: "Matériels et armements", multiline: true },
    { key: "missions", label: "Missions", multiline: true },
  ];

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="text-lg">{title}</CardTitle>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => onChange([...rows, { ...emptyRepartition }])}
          >
            <Plus className="h-4 w-4 mr-1" />
            Ajouter
          </Button>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {rows.map((row, idx) => (
          <div key={idx} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 items-start rounded-lg border p-3">
            {fields.map((f) => (
              <div key={f.key} className="space-y-1">
                <Label>{f.label}</Label>
                {f.multiline ? (
                  <Textarea
                    value={(row[f.key] as string) || ""}
                    onChange={(e) =>
                      onChange(rows.map((r, i) => (i === idx ? { ...r, [f.key]: e.target.value } : r)))
                    }
                    rows={2}
                  />
                ) : (
                  <Input
                    value={(row[f.key] as string) || ""}
                    onChange={(e) =>
                      onChange(rows.map((r, i) => (i === idx ? { ...r, [f.key]: e.target.value } : r)))
                    }
                  />
                )}
              </div>
            ))}
            <div className="flex justify-end sm:col-span-2 lg:col-span-3">
              <Button
                type="button"
                variant="ghost"
                size="icon"
                onClick={() => removeRow(rows, onChange, idx)}
                disabled={rows.length === 1}
              >
                <Trash2 className="h-4 w-4 text-destructive" />
              </Button>
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}
