import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getArmeById,
  getArmeConsommations,
  recordConsommation,
} from "@/lib/api/arme";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { DataTable, type Column } from "@/components/ui/data-table";
import {
  ArrowLeft,
  Pencil,
  Loader2,
  Crosshair,
  Plus,
  History,
} from "lucide-react";
import type { Arme, ArmeMunitionsConsommation } from "@/types";

const LIST_PATH = "/sedentaire/poste/armes";

function formatDateTime(s: string | null | undefined): string {
  if (!s) return "—";
  const parts = s.split(" ");
  const date = parts[0]?.split("-").reverse().join("/") ?? s;
  const time = parts[1]?.slice(0, 5) ?? "";
  return `${date}${time ? ` ${time}` : ""}`.trim();
}

function agentDisplay(c: ArmeMunitionsConsommation): string {
  const parts = [c.agent_grade, c.agent_im, [c.agent_firstname, c.agent_lastname].filter(Boolean).join(" ")].filter(Boolean);
  return parts.join(" — ") || "—";
}

export function ArmeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [arme, setArme] = useState<Arme | null>(null);
  const [consommations, setConsommations] = useState<ArmeMunitionsConsommation[]>([]);
  const [loading, setLoading] = useState(true);
  const [showConsoDialog, setShowConsoDialog] = useState(false);
  const [consoQuantite, setConsoQuantite] = useState("1");
  const [consoAgentId, setConsoAgentId] = useState("");
  const [savingConso, setSavingConso] = useState(false);

  useEffect(() => {
    if (id) {
      loadAll(Number(id));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadAll(armeId: number) {
    setLoading(true);
    try {
      const [a, c] = await Promise.all([
        getArmeById(armeId),
        getArmeConsommations(armeId),
      ]);
      setArme(a);
      setConsommations(c);
    } catch {
      addNotification("error", "Erreur", "Arme introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  async function handleConsommation(e: React.FormEvent) {
    e.preventDefault();
    if (!arme) return;
    const q = Number(consoQuantite);
    if (!Number.isInteger(q) || q <= 0) {
      addNotification("error", "Validation", "La quantité doit être un entier positif");
      return;
    }
    setSavingConso(true);
    try {
      await recordConsommation(arme.id, {
        quantite: q,
        agent_id: consoAgentId.trim() ? Number(consoAgentId) : null,
      });
      addNotification("success", "Consommation enregistrée", `${q} munition(s) déduites du stock`);
      setShowConsoDialog(false);
      setConsoQuantite("1");
      setConsoAgentId("");
      loadAll(arme.id);
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer la consommation";
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
      setSavingConso(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!arme) return null;

  const consoColumns: Column<ArmeMunitionsConsommation>[] = [
    {
      key: "date_consommation",
      header: "Date et heure",
      sortable: true,
      render: (c) => formatDateTime(c.date_consommation),
    },
    {
      key: "quantite",
      header: "Quantité",
      sortable: true,
      render: (c) => <Badge>{c.quantite}</Badge>,
    },
    {
      key: "agent",
      header: "Agent",
      sortable: false,
      render: (c) => agentDisplay(c),
    },
    {
      key: "armement_id",
      header: "Perception liée",
      sortable: false,
      render: (c) => (c.armement_id ? `#${c.armement_id}` : "—"),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              {arme.type_arme_nom} — {arme.matricule}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Détail de l'arme et historique des consommations
            </p>
          </div>
        </div>
        <Button variant="outline" onClick={() => navigate(`${LIST_PATH}/${arme.id}/edit`)} className="gap-2">
          <Pencil className="h-4 w-4" />
          Modifier
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm text-muted-foreground">Type d'arme</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <Crosshair className="h-4 w-4 text-muted-foreground" />
              <span className="text-lg font-medium">{arme.type_arme_nom || "—"}</span>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm text-muted-foreground">Matricule</CardTitle>
          </CardHeader>
          <CardContent>
            <span className="text-lg font-medium font-mono">{arme.matricule}</span>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm text-muted-foreground">Munitions disponibles (type)</CardTitle>
          </CardHeader>
          <CardContent>
            <Badge variant={arme.type_arme_munitions_stock > 0 ? "default" : "secondary"} className="text-base">
              {arme.type_arme_munitions_stock}
            </Badge>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base flex items-center gap-2">
              <History className="h-4 w-4" />
              Historique des consommations ({consommations.length})
            </CardTitle>
            <Button size="sm" onClick={() => setShowConsoDialog(true)} className="gap-2">
              <Plus className="h-4 w-4" />
              Enregistrer une consommation
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <DataTable
            columns={consoColumns}
            data={consommations}
            keyExtractor={(c) => c.id}
            loading={false}
            searchable={false}
            emptyMessage="Aucune consommation enregistrée"
          />
        </CardContent>
      </Card>

      {showConsoDialog && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={() => setShowConsoDialog(false)}
        >
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-base font-semibold mb-4">
              Enregistrer une consommation de munitions
            </h3>
            <form onSubmit={handleConsommation} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="conso_quantite">Quantité consommée *</Label>
                <Input
                  id="conso_quantite"
                  type="number"
                  min={1}
                  value={consoQuantite}
                  onChange={(e) => setConsoQuantite(e.target.value)}
                  required
                />
                <p className="text-xs text-muted-foreground">
                  Stock actuel du type : {arme.type_arme_munitions_stock}. La déduction est
                  atomique et rejetée si le stock est insuffisant.
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="conso_agent">ID Agent (optionnel)</Label>
                <Input
                  id="conso_agent"
                  type="number"
                  min={1}
                  value={consoAgentId}
                  onChange={(e) => setConsoAgentId(e.target.value)}
                  placeholder="Personnel ID"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setShowConsoDialog(false)}
                  disabled={savingConso}
                >
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={savingConso} className="gap-2">
                  {savingConso && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
                  Enregistrer
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}
    </motion.div>
  );
}
