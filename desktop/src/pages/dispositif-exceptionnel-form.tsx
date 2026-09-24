import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import { useAuthStore } from "@/stores/auth-store";
import { hasPermission } from "@/lib/permissions";
import {
  createDispositifExceptionnel,
  updateDispositifExceptionnel,
  getDispositifExceptionnelById,
} from "@/lib/api/dispositif-exceptionnel";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { DateRangePicker } from "@/components/ui/date-range-picker";
import { ArrowLeft, Save, Loader2 } from "lucide-react";
import {
  EffectifEngageTable,
  blankEffectifRow,
  type EditableEffectifRow,
} from "@/components/dispositif-exceptionnel/effectif-engage-table";
import type { DispositifExceptionnelInput } from "@/types";

const SG_DISPOSITIF_MODULE = "sg_dispositif_exceptionnel";

export function DispositifExceptionnelForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();
  const { user } = useAuthStore();
  const canEdit = hasPermission(user, SG_DISPOSITIF_MODULE, "can_edit");

  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(isEdit);
  const [natureEvenement, setNatureEvenement] = useState("");
  const [dateDebut, setDateDebut] = useState("");
  const [dateFin, setDateFin] = useState("");
  const [effectifs, setEffectifs] = useState<EditableEffectifRow[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [invalidSecteurKeys, setInvalidSecteurKeys] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!isEdit) {
      // Start with one blank effectif line.
      setEffectifs([blankEffectifRow()]);
      return;
    }
    (async () => {
      try {
        const e = await getDispositifExceptionnelById(Number(id));
        setNatureEvenement(e.nature_evenement ?? "");
        setDateDebut(e.date_debut?.substring(0, 10) ?? "");
        setDateFin(e.date_fin?.substring(0, 10) ?? "");
        setEffectifs(
          (e.effectifs ?? []).map((r) => ({
            _key: String(r.id),
            secteur: r.secteur,
            chef_element_contact: r.chef_element_contact ?? "",
            controle_contact: r.controle_contact ?? "",
            materiels_armements: r.materiels_armements ?? "",
            missions: r.missions ?? "",
          })),
        );
      } catch {
        addNotification("error", "Erreur", "Impossible de charger le dispositif exceptionnel");
        navigate("/sg/dispositifs-exceptionnels");
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!natureEvenement.trim()) errs.nature_evenement = "La nature de l'évènement est requise";
    if (!dateDebut) errs.date_debut = "La date de début est requise";
    if (!dateFin) errs.date_fin = "La date de fin est requise";
    if (dateDebut && dateFin && dateFin < dateDebut) {
      errs.date_fin = "La date de fin doit être postérieure ou égale à la date de début";
    }
    // Table entries: a partially filled row must have a secteur.
    const invalid = new Set<string>();
    effectifs.forEach((r) => {
      const hasContent = [
        r.secteur,
        r.chef_element_contact,
        r.controle_contact,
        r.materiels_armements,
        r.missions,
      ].some((v) => (v ?? "").trim() !== "");
      if (hasContent && r.secteur.trim() === "") {
        invalid.add(r._key);
      }
    });
    setInvalidSecteurKeys(invalid);
    if (invalid.size > 0) {
      errs.effectifs = "Chaque ligne d'effectif renseignée doit avoir un secteur";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(ev: React.FormEvent) {
    ev.preventDefault();
    if (!validate()) return;
    setSaving(true);

    // Skip rows the user left entirely blank.
    const filledRows = effectifs.filter((r) =>
      [r.secteur, r.chef_element_contact, r.controle_contact, r.materiels_armements, r.missions]
        .some((v) => (v ?? "").trim() !== ""),
    );
    const payload: DispositifExceptionnelInput = {
      nature_evenement: natureEvenement.trim(),
      date_debut: dateDebut,
      date_fin: dateFin,
      effectifs: filledRows.map(({ _key, ...row }) => ({
        ...row,
        secteur: row.secteur.trim(),
      })),
    };

    try {
      if (isEdit) {
        await updateDispositifExceptionnel(Number(id), payload);
      } else {
        await createDispositifExceptionnel(payload);
      }
      addNotification(
        "success",
        isEdit ? "Modifié" : "Enregistré",
        `Le dispositif exceptionnel a été ${isEdit ? "modifié" : "enregistré"} avec succès`,
      );
      navigate("/sg/dispositifs-exceptionnels");
    } catch (err) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      addNotification("error", "Erreur", axiosErr?.response?.data?.message || "Erreur lors de l'enregistrement");
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate("/sg/dispositifs-exceptionnels")}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {isEdit ? "Modifier le dispositif" : "Nouveau dispositif exceptionnel"}
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Opération de sécurité exceptionnelle
          </p>
        </div>
      </div>

      <motion.form
        onSubmit={handleSubmit}
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        {/* ── Informations générales ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="nature_evenement">Nature de l'évènement *</Label>
              <Input
                id="nature_evenement"
                value={natureEvenement}
                onChange={(e) => setNatureEvenement(e.target.value)}
                placeholder="Ex : Visite VIP, manifestation, match de football"
                aria-invalid={!!errors.nature_evenement}
              />
              {errors.nature_evenement && (
                <p className="text-sm text-destructive">{errors.nature_evenement}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="periode">Période *</Label>
              <DateRangePicker
                id="periode"
                start={dateDebut}
                end={dateFin}
                onChange={({ start, end }) => {
                  setDateDebut(start);
                  setDateFin(end);
                }}
                startInvalid={!!errors.date_debut}
                endInvalid={!!errors.date_fin}
              />
              {(errors.date_debut || errors.date_fin) && (
                <p className="text-sm text-destructive">
                  {errors.date_debut || errors.date_fin}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* ── Effectif engagé ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Effectif engagé</CardTitle>
            <p className="text-sm text-muted-foreground">
              Répartition des effectifs par secteur
            </p>
          </CardHeader>
          <CardContent className="space-y-2">
            <EffectifEngageTable
              rows={effectifs}
              onChange={setEffectifs}
              invalidSecteurKeys={invalidSecteurKeys}
            />
            {errors.effectifs && (
              <p className="text-sm text-destructive">{errors.effectifs}</p>
            )}
          </CardContent>
        </Card>

        <div className="flex gap-3">
          <Button type="submit" disabled={saving || (isEdit && !canEdit)}>
            {saving ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate("/sg/dispositifs-exceptionnels")}>
            Annuler
          </Button>
        </div>
      </motion.form>
    </div>
  );
}
