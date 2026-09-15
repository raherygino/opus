import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMandatById,
  getMandatAttachments,
  getMandatAttachmentDownloadUrl,
  isMandatImageAttachment,
} from "@/lib/api/mandat";
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
import { getMandatTypeLabel } from "@/types";
import type { Mandat, MandatAttachment } from "@/types";

const PJ_MANDAT_MODULE = "pj_mandat";

function formatDateTime(date: string | null | undefined): string {
  if (!date) return "—";
  const d = new Date(date.replace(" ", "T"));
  if (isNaN(d.getTime())) return date;
  return d.toLocaleDateString("fr-FR") + " " + d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value || "—"}</p>
    </div>
  );
}

export function MandatDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_MANDAT_MODULE, "can_edit");

  const [entry, setEntry] = useState<Mandat | null>(null);
  const [attachments, setAttachments] = useState<MandatAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<MandatAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getMandatById(Number(id));
      setEntry(data);
      try {
        const atts = await getMandatAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Mandat introuvable");
      navigate("/pj/mandat");
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
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/mandat")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail du mandat</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/mandat/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Mandat</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Objet du mandat" value={getMandatTypeLabel(entry.type)} />
          <DetailRow label="Numéro" value={<span className="font-mono">{entry.numero}</span>} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Autorité</CardTitle>
        </CardHeader>
        <CardContent>
          <DetailRow label="Autorité ayant délivré le mandat" value={entry.autorite} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Personne concernée</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Nom et prénom" value={entry.personne_nom} />
          <DetailRow label="Date et lieu de naissance" value={entry.date_lieu_naissance} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Informations judiciaires</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Motif du mandat" value={entry.motif} />
          <DetailRow label="Qualification de l'infraction" value={entry.qualification_infraction} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Exécution</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Nom de l'OPJ chargé de l'exécution" value={entry.opj_execution} />
          <DetailRow label="Date et heure d'exécution" value={formatDateTime(entry.date_heure_execution)} />
          <DetailRow label="Lieu d'exécution" value={entry.lieu_execution} />
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
                  {isMandatImageAttachment(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getMandatAttachmentDownloadUrl(entry.id, att.id)}
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
        src={viewerTarget ? getMandatAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
