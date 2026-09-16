import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getGardeAVueById,
  getGardeAVueAttachments,
  getGardeAVueAttachmentDownloadUrl,
  isGardeAVueImageAttachment,
} from "@/lib/api/garde-a-vue";
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
import type { GardeAVue, GardeAVueAttachment } from "@/types";

const PJ_GAV_MODULE = "pj_gav";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

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

export function GardeAVueDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_GAV_MODULE, "can_edit");

  const [entry, setEntry] = useState<GardeAVue | null>(null);
  const [attachments, setAttachments] = useState<GardeAVueAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<GardeAVueAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getGardeAVueById(Number(id));
      setEntry(data);
      try {
        const atts = await getGardeAVueAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Garde à vue introuvable");
      navigate("/pj/gav");
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
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/gav")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail de la garde à vue</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/gav/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      {/* Identité */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Identité</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Nom" value={entry.nom} />
          <DetailRow label="Prénoms" value={entry.prenoms} />
          <DetailRow label="Date de naissance" value={formatDate(entry.date_naissance)} />
          <DetailRow label="Adresse" value={entry.adresse} />
        </CardContent>
      </Card>

      {/* Enquête */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Enquête</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Enquêteur de permanence" value={entry.enqueteur_permance} />
          <DetailRow label="OPJ ayant décidé la garde à vue" value={entry.opj_gav} />
          <div className="md:col-span-2">
            <DetailRow label="Motif" value={entry.motif} />
          </div>
        </CardContent>
      </Card>

      {/* Santé & droits */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Santé & droits</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="État de santé" value={entry.etat_sante} />
          <DetailRow label="Personne à contacter" value={entry.personne_contacter} />
          <div className="md:col-span-2">
            <DetailRow label="Droits notifiés" value={entry.droits_notifies} />
          </div>
        </CardContent>
      </Card>

      {/* Dates de la garde à vue */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dates de la garde à vue</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <DetailRow label="Début de GAV" value={formatDateTime(entry.debut_gav)} />
          <DetailRow label="Fin de GAV" value={formatDateTime(entry.fin_gav)} />
          <DetailRow label="Prolongation de GAV" value={formatDateTime(entry.prolongation_gav)} />
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
                  {isGardeAVueImageAttachment(att.mime_type, att.original_filename) && (
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  )}
                  <a
                    href={getGardeAVueAttachmentDownloadUrl(entry.id, att.id)}
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
        src={viewerTarget ? getGardeAVueAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
