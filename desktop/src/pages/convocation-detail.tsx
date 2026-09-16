import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getConvocationById,
  getConvocationAttachments,
  getConvocationAttachmentDownloadUrl,
  CONVOCATION_TYPE_LABELS,
  isConvocationImageAttachment,
} from "@/lib/api/convocation";
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
import type { Convocation, ConvocationAttachment } from "@/types";

const PJ_CONVOCATION_MODULE = "pj_convocation";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function ConvocationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_CONVOCATION_MODULE, "can_edit");

  const [entry, setEntry] = useState<Convocation | null>(null);
  const [attachments, setAttachments] = useState<ConvocationAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<ConvocationAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getConvocationById(Number(id));
      setEntry(data);
      try {
        const atts = await getConvocationAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Convocation introuvable");
      navigate("/pj/convocation");
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

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/convocation")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail de la convocation</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/convocation/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      {/* Informations */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Informations</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Type" value={
            <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-primary/10 text-primary text-xs font-medium">
              {CONVOCATION_TYPE_LABELS[entry.type] ?? entry.type}
            </span>
          } />
          <DetailRow label="Date" value={formatDate(entry.date_convocation)} />
          <DetailRow label="Numéro" value={<span className="font-mono text-sm font-semibold">{entry.numero}</span>} />
          <DetailRow label="Nom" value={entry.nom} />
          <DetailRow label="Adresse" value={entry.adresse} />
          <DetailRow label="Infraction" value={entry.infraction} />
          <DetailRow label="Personne ayant accusée réception" value={entry.personne_accuse_recu} />
          <DetailRow label="Numéro du dossier" value={entry.numero_dossier} />
          {entry.observation && (
            <div className="md:col-span-2 space-y-1">
              <p className="text-xs text-muted-foreground">Observation</p>
              <p className="text-sm whitespace-pre-wrap">{entry.observation}</p>
            </div>
          )}
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
                  {isConvocationImageAttachment(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getConvocationAttachmentDownloadUrl(entry.id, att.id)}
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
        src={viewerTarget ? getConvocationAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
