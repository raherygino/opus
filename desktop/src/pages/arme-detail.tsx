import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getArmeById,
  getArmeConsommations,
} from "@/lib/api/arme";
import { Button } from "@/components/ui/button";
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
          <CardTitle className="text-base flex items-center gap-2">
            <History className="h-4 w-4" />
            Historique des consommations ({consommations.length})
          </CardTitle>
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

    </motion.div>
  );
}
