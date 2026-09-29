import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getSituationGavById,
  getSituationGavAttachments,
  getSituationGavAttachmentDownloadUrl,
} from "@/lib/api/situation-gav";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Paperclip,
  Download,
  Eye,
} from "lucide-react";
import type { SituationGav, SituationGavAttachment } from "@/types";

const MODULE = "sedentaire_poste_situation_gav";
const LIST_PATH = "/sedentaire/poste/situation-gav";

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const datePart = date.slice(0, 10);
  const timePart = date.substring(11, 16);
  const [y, m, d] = datePart.split("-");
  const dateDisplay = y && m && d ? `${d}/${m}/${y}` : datePart;
  return `${dateDisplay} ${timePart}`.trim();
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value || "—"}</p>
    </div>
  );
}

export function SituationGavDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, MODULE, "can_edit");

  const [entry, setEntry] = useState<SituationGav | null>(null);
  const [attachments, setAttachments] = useState<SituationGavAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<SituationGavAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getSituationGavById(Number(id));
      setEntry(data);
      try {
        const atts = await getSituationGavAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Situation GAV introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!entry) return null;

  const personneName = [entry.personne_nom, entry.personne_prenoms]
    .filter(Boolean)
    .join(" ");
  const agentName = [entry.agent_controle_grade, entry.agent_controle_nom]
    .filter(Boolean)
    .join(" ");

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail de la situation GAV</h1>
            <p className="text-sm text-muted-foreground mt-1">Sédentaire — Poste</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`${LIST_PATH}/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      {/* Contrôle */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Contrôle</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Personne concernée" value={personneName} />
          <DetailRow label="Date et heure du contrôle" value={formatDateTime(entry.date_controle)} />
          <DetailRow label="Agent ayant effectué le contrôle" value={agentName} />
          <DetailRow label="État général de la personne" value={entry.etat_general} />
        </CardContent>
      </Card>

      {/* Observations & mesures */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Observations & mesures</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4">
          <DetailRow label="Observations" value={entry.observations} />
          <DetailRow label="Mesures prises" value={entry.mesures_prises} />
        </CardContent>
      </Card>

      {/* Pièces jointes */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes ({attachments.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {attachments.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe.</p>
          ) : (
            <div className="space-y-2">
              {attachments.map((att) => (
                <div
                  key={att.id}
                  className="flex items-center gap-3 rounded-lg border p-3"
                >
                  <Paperclip className="h-4 w-4 text-muted-foreground" />
                  <div className="flex-1">
                    <p className="text-sm font-medium">{att.title}</p>
                    <p className="text-xs text-muted-foreground">{att.original_filename}</p>
                  </div>
                  {isImageFile(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getSituationGavAttachmentDownloadUrl(entry.id, att.id)}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <Button variant="ghost" size="icon">
                      <Download className="h-4 w-4" />
                    </Button>
                  </a>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ImageViewerDialog
        src={viewerTarget ? getSituationGavAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
