import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getRegistreEnqueteById,
  getRegistreEnqueteAttachments,
  getRegistreEnqueteAttachmentDownloadUrl,
  isRegistreEnqueteImageAttachment,
} from "@/lib/api/registre-enquete";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
import {
  REGISTRE_ENQUETE_STATUT_LABELS,
  type RegistreEnquete,
  type RegistreEnqueteAttachment,
} from "@/types";

const PJ_ENQUETE_MODULE = "pj_enquete";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const d = new Date(date.replace(" ", "T"));
  if (isNaN(d.getTime())) return date;
  return d.toLocaleDateString("fr-FR");
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value || "—"}</p>
    </div>
  );
}

export function RegistreEnqueteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_ENQUETE_MODULE, "can_edit");

  const [entry, setEntry] = useState<RegistreEnquete | null>(null);
  const [attachments, setAttachments] = useState<RegistreEnqueteAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<RegistreEnqueteAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getRegistreEnqueteById(Number(id));
      setEntry(data);
      try {
        const atts = await getRegistreEnqueteAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Entrée introuvable");
      navigate("/pj/registre-enquete");
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

  const enqueteurName = [entry.enqueteur_prenoms, entry.enqueteur_nom].filter(Boolean).join(" ");
  const enqueteurLabel = enqueteurName
    ? entry.enqueteur_grade ? `${enqueteurName} (${entry.enqueteur_grade})` : enqueteurName
    : "—";
  const opjName = [entry.opj_prenoms, entry.opj_nom].filter(Boolean).join(" ");
  const opjLabel = opjName
    ? entry.opj_grade ? `${opjName} (${entry.opj_grade})` : opjName
    : "—";

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/registre-enquete")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail de l'entrée</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — Registre d'enquête</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/registre-enquete/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Enregistrement</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Numéro" value={<span className="font-mono">{entry.numero}</span>} />
          <DetailRow label="Date d'ouverture" value={formatDate(entry.date_ouverture)} />
          <DetailRow label="N° du dossier rattaché" value={entry.numero_dossier} />
          <div className="space-y-1">
            <p className="text-xs text-muted-foreground">Statut</p>
            <Badge variant="secondary">
              {REGISTRE_ENQUETE_STATUT_LABELS[entry.statut] ?? entry.statut}
            </Badge>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Infraction</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Nature de l'infraction" value={entry.nature_infraction} />
          <DetailRow label="Date et lieu des faits" value={entry.date_lieu_faits} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Personnes</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Plaignant / Partie civile" value={entry.plaignant} />
          <DetailRow label="Mise en cause" value={entry.mise_en_cause} />
          <DetailRow label="Enquêteur" value={enqueteurLabel} />
          <DetailRow label="OPJ" value={opjLabel} />
        </CardContent>
      </Card>

      {entry.observations && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Observations</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm whitespace-pre-wrap">{entry.observations}</p>
          </CardContent>
        </Card>
      )}

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
                  {isRegistreEnqueteImageAttachment(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getRegistreEnqueteAttachmentDownloadUrl(entry.id, att.id)}
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
        src={viewerTarget ? getRegistreEnqueteAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
