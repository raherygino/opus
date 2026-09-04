import { useState, useEffect } from "react";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ShieldCheck, Loader2 } from "lucide-react";
import type { Armement } from "@/types";
import type { ReintegrationPayload } from "@/lib/api/armement";

function nowTime(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function todayIso(): string {
  const d = new Date();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mm}-${dd}`;
}

interface ReintegrationDialogProps {
  open: boolean;
  armement: Armement | null;
  loading?: boolean;
  onConfirm: (payload: ReintegrationPayload) => void;
  onCancel: () => void;
}

/**
 * Dedicated réintégration form — only the three reintegration fields are
 * asked; all perception data is preserved server-side.
 */
export function ReintegrationDialog({
  open,
  armement,
  loading = false,
  onConfirm,
  onCancel,
}: ReintegrationDialogProps) {
  const [date, setDate] = useState("");
  const [heure, setHeure] = useState("");
  const [etat, setEtat] = useState("");
  const [munitionsConsommees, setMunitionsConsommees] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setDate(todayIso());
      setHeure(nowTime());
      setEtat("");
      setMunitionsConsommees("");
      setError(null);
    }
  }, [open, armement?.id]);

  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") onCancel();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onCancel]);

  if (!open || !armement) return null;

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!date) {
      setError("La date de la réintégration est requise");
      return;
    }
    if (!heure) {
      setError("L'heure de la réintégration est requise");
      return;
    }
    if (!etat.trim()) {
      setError("L'état à la réintégration est requis");
      return;
    }
    if (munitionsConsommees.trim() === "") {
      setError("Les munitions consommées sont requises");
      return;
    }
    const count = Number(munitionsConsommees);
    if (!Number.isInteger(count) || count < 0) {
      setError("Les munitions consommées doivent être un nombre entier positif");
      return;
    }
    if (armement!.munitions !== null && count > armement!.munitions) {
      setError(
        `Les munitions consommées ne peuvent pas dépasser les munitions perçues (${armement!.munitions})`,
      );
      return;
    }
    setError(null);
    onConfirm({
      date_reintegration: date,
      heure_reintegration: heure,
      etat_reintegration: etat.trim(),
      munitions_consommees: count,
    });
  }

  return (
    <div
      className="fixed inset-0 z-[1000] flex items-center justify-center"
      onClick={onCancel}
    >
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: -20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: -20 }}
        transition={{ duration: 0.15, ease: "easeOut" }}
        className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start gap-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div className="flex-1 space-y-1">
            <p className="text-sm font-semibold">Réintégration de l'arme</p>
            <p className="text-sm text-muted-foreground">
              {armement.type_arme} — {armement.matricule_arme}
              {armement.agent_preneur_nom
                ? ` · ${[armement.agent_preneur_grade, armement.agent_preneur_nom].filter(Boolean).join(" ")}`
                : ""}
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div className="space-y-2">
            <Label htmlFor="date_reintegration">Date de la réintégration *</Label>
            <Input
              id="date_reintegration"
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="heure_reintegration">Heure de la réintégration *</Label>
            <Input
              id="heure_reintegration"
              type="time"
              value={heure}
              onChange={(e) => setHeure(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="etat_reintegration">État à la réintégration *</Label>
            <textarea
              id="etat_reintegration"
              value={etat}
              onChange={(e) => setEtat(e.target.value)}
              rows={2}
              className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
              placeholder="État de l'arme au retour..."
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="munitions_consommees">
              Munitions consommées *
              {armement.munitions !== null && (
                <span className="text-xs text-muted-foreground">
                  {" "}(perçues : {armement.munitions})
                </span>
              )}
            </Label>
            <Input
              id="munitions_consommees"
              type="number"
              min={0}
              {...(armement.munitions !== null ? { max: armement.munitions } : {})}
              value={munitionsConsommees}
              onChange={(e) => setMunitionsConsommees(e.target.value)}
              required
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex items-center justify-end gap-2 pt-1">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onCancel}
              disabled={loading}
            >
              Annuler
            </Button>
            <Button type="submit" size="sm" disabled={loading} className="gap-2">
              {loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
              Réintégrer
            </Button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}
