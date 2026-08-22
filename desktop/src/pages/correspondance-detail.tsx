import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getCorrespondanceById,
  getCorrespondanceAttachmentDownloadUrl,
} from "@/lib/api/correspondance";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Repeat,
  Paperclip,
  Download,
  ArrowDownLeft,
  ArrowUpRight,
} from "lucide-react";
import type { Correspondance } from "@/types";
import {
  CORRESPONDANCE_MODULE,
  formatDate,
  formatHeure,
} from "@/pages/correspondance-list";

const LIST_PATH = "/sedentaire/secretariat/correspondance";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function CorrespondanceDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, CORRESPONDANCE_MODULE, "can_edit");

  const [correspondance, setCorrespondance] = useState<Correspondance | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadCorrespondance();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadCorrespondance() {
    setLoading(true);
    try {
      const data = await getCorrespondanceById(Number(id));
      setCorrespondance(data);
    } catch {
      addNotification("error", "Erreur", "Correspondance introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!correspondance) return null;

  const agent =
    [correspondance.agent_prenoms, correspondance.agent_nom]
      .filter(Boolean)
      .join(" ") || correspondance.agent_username;

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
              <span
                className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full ${
                  correspondance.sens === "Entrant"
                    ? "bg-green-500/10 text-green-500"
                    : "bg-blue-500/10 text-blue-500"
                }`}
              >
                {correspondance.sens === "Entrant" ? (
                  <ArrowDownLeft className="h-3 w-3" />
                ) : (
                  <ArrowUpRight className="h-3 w-3" />
                )}
                {correspondance.sens}
              </span>
              {correspondance.reference}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {correspondance.objet}
            </p>
          </div>
        </div>
        {canEdit && (
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => navigate(`${LIST_PATH}/${correspondance.id}/edit`)}
          >
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Repeat className="h-4 w-4" />
            Informations de la correspondance
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date" value={formatDate(correspondance.date_correspondance)} />
          <DetailRow
            label="Heure d'enregistrement"
            value={formatHeure(correspondance.heure_enregistrement)}
          />
          <DetailRow label="N° d'ordre / Référence" value={correspondance.reference} />
          <DetailRow
            label={correspondance.sens === "Sortant" ? "Destinataire" : "Émetteur"}
            value={correspondance.emetteur_destinataire}
          />
          <DetailRow label="Objet" value={correspondance.objet} />
          <DetailRow label="Statut" value={correspondance.statut} />
          <DetailRow label="Agent secrétariat" value={agent} />
          <DetailRow
            label="Enregistrée le"
            value={correspondance.created_at
              ? new Date(correspondance.created_at.replace(" ", "T")).toLocaleString("fr-FR")
              : "—"}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Fichiers joints ({correspondance.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(correspondance.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
          )}
          {(correspondance.attachments ?? []).map((att) => (
            <div
              key={att.id}
              className="flex items-center justify-between rounded-lg border border-border p-3"
            >
              <div>
                <p className="text-sm font-medium">{att.title}</p>
                <p className="text-xs text-muted-foreground">{att.original_filename}</p>
              </div>
              <a
                href={getCorrespondanceAttachmentDownloadUrl(correspondance.id, att.id)}
                download
              >
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <Download className="h-4 w-4" />
                </Button>
              </a>
            </div>
          ))}
        </CardContent>
      </Card>
    </motion.div>
  );
}
