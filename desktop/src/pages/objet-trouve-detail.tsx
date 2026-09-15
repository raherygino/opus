import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getObjetTrouveById,
  getObjetTrouveAttachments,
  getObjetTrouveAttachmentDownloadUrl,
  isObjetImageAttachment,
} from "@/lib/api/objet";
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
  CheckCircle2,
} from "lucide-react";
import type { ObjetTrouve, ObjetTrouveAttachment } from "@/types";
import { OBJET_TROUVE_MOTIF_LABELS } from "@/types";

const PJ_MODULE = "pj_objets";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value || "—"}</p>
    </div>
  );
}

export function ObjetTrouveDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_MODULE, "can_edit");

  const [entry, setEntry] = useState<ObjetTrouve | null>(null);
  const [attachments, setAttachments] = useState<ObjetTrouveAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<ObjetTrouveAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getObjetTrouveById(Number(id));
      setEntry(data);
      try {
        const atts = await getObjetTrouveAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Objet trouvé introuvable");
      navigate("/pj/objets");
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
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/objets")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail — Objet trouvé</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — Objet trouvé</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/objets/trouve/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Objet trouvé</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Affaire" value={entry.affaire} />
          <DetailRow label="Motif de découverte" value={OBJET_TROUVE_MOTIF_LABELS[entry.motif_decouverte] ?? entry.motif_decouverte} />
          <DetailRow
            label="Restitution"
            value={
              entry.restitution === 1 ? (
                <span className="inline-flex items-center gap-1 text-primary">
                  <CheckCircle2 className="h-4 w-4" />
                  Restitué
                </span>
              ) : (
                "Non restitué"
              )
            }
          />
        </CardContent>
      </Card>

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
                  {isObjetImageAttachment(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getObjetTrouveAttachmentDownloadUrl(entry.id, att.id)}
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
        src={viewerTarget ? getObjetTrouveAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
