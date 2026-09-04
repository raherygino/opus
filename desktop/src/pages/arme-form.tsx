import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getArmeById,
  createArme,
  updateArme,
  getTypeArmeList,
} from "@/lib/api/arme";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { ArrowLeft, Save, Loader2, Crosshair } from "lucide-react";
import type { Arme, TypeArme } from "@/types";
import type { ArmePayload } from "@/lib/api/arme";

const LIST_PATH = "/sedentaire/poste/armes";

export function ArmeForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<ArmePayload>({
    type_arme_id: 0,
    matricule: "",
  });
  const [typesArmes, setTypesArmes] = useState<TypeArme[]>([]);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadTypes();
    if (isEdit && id) {
      loadArme(Number(id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadTypes() {
    try {
      const data = await getTypeArmeList();
      setTypesArmes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types d'armes");
    }
  }

  async function loadArme(armeId: number) {
    setLoading(true);
    try {
      const arme: Arme = await getArmeById(armeId);
      setForm({
        type_arme_id: arme.type_arme_id,
        matricule: arme.matricule,
      });
    } catch {
      addNotification("error", "Erreur", "Arme introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (form.type_arme_id <= 0) {
      addNotification("error", "Validation", "Le type d'arme est requis");
      return;
    }
    if (!form.matricule.trim()) {
      addNotification("error", "Validation", "Le matricule est requis");
      return;
    }
    setSaving(true);
    try {
      if (isEdit && id) {
        await updateArme(Number(id), form);
        addNotification("success", "Modifiée", "Arme modifiée avec succès");
      } else {
        await createArme(form);
        addNotification("success", "Créée", "Arme créée avec succès");
      }
      navigate(LIST_PATH);
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer l'arme";
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
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6 max-w-2xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            {isEdit ? "Modifier l'arme" : "Nouvelle arme"}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Identifiez l'arme physique par son matricule unique et son type
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Crosshair className="h-4 w-4" />
              Identification de l'arme
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="type_arme_id">Type d'arme *</Label>
              <Select
                id="type_arme_id"
                value={form.type_arme_id === 0 ? "" : String(form.type_arme_id)}
                onChange={(e) => setForm({ ...form, type_arme_id: Number(e.target.value) })}
                placeholder="Sélectionner un type d'arme"
                options={typesArmes.map((t) => ({ value: String(t.id), label: t.nom }))}
              />
              {typesArmes.length === 0 && (
                <p className="text-xs text-muted-foreground">
                  Aucun type d'arme enregistré.{" "}
                  <button
                    type="button"
                    className="text-primary underline"
                    onClick={() => navigate("/sedentaire/poste/armes?tab=types")}
                  >
                    Créez-en un d'abord.
                  </button>
                </p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="matricule">Matricule *</Label>
              <Input
                id="matricule"
                value={form.matricule}
                onChange={(e) => setForm({ ...form, matricule: e.target.value })}
                placeholder="Ex : PA-0001"
                required
              />
              <p className="text-xs text-muted-foreground">
                Numéro de série unique de l'arme
              </p>
            </div>

            {form.type_arme_id > 0 && (
              <div className="space-y-2">
                <Label>Stock de munitions du type</Label>
                <div className="rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">
                  {typesArmes.find((t) => t.id === form.type_arme_id)?.munitions_stock ?? 0}
                  {" "}
                  <span className="text-muted-foreground">
                    (géré au niveau du type d'arme — toutes les armes de ce type partagent le même stock)
                  </span>
                </div>
                <p className="text-xs text-muted-foreground">
                  Le stock de munitions est géré par type d'arme, pas par matricule.
                  Modifiez-le depuis la page « Types d'armes ».
                </p>
              </div>
            )}
          </CardContent>
        </Card>

        <div className="flex items-center justify-end gap-2 pt-4">
          <Button type="button" variant="outline" onClick={() => navigate(LIST_PATH)}>
            Annuler
          </Button>
          <Button type="submit" disabled={saving} className="gap-2">
            {saving ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Save className="h-4 w-4" />
            )}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
        </div>
      </form>
    </motion.div>
  );
}
