import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPlainteEntreeById,
  getPlainteEntreeAttachments,
  getPlainteEntreeAttachmentDownloadUrl,
  PLAINTE_ENTREE_TYPE_LABELS,
} from "@/lib/api/plainte";
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
  ArrowRightLeft,
} from "lucide-react";
import type { PlainteEntree, PlainteEntreeAttachment } from "@/types";

const PJ_PLAINTE_MODULE = "pj_plainte";

function formatDate(date: string | null | undefined): string {
  if (!date) return "—";
  const [y, m, d] = date.slice(0, 10).split("-");
  return y && m && d ? `${d}/${m}/${y}` : date;
}

function formatHeure(heure: string | null | undefined): string {
  return heure ? heure.slice(0, 5) : "—";
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function PlainteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, PJ_PLAINTE_MODULE, "can_create");
  const canEdit = hasPermission(user, PJ_PLAINTE_MODULE, "can_edit");

  const [entry, setEntry] = useState<PlainteEntree | null>(null);
  const [attachments, setAttachments] = useState<PlainteEntreeAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<PlainteEntreeAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getPlainteEntreeById(Number(id));
      setEntry(data);
      try {
        const atts = await getPlainteEntreeAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Plainte introuvable");
      navigate("/pj/plainte");
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
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/plainte")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Détail de la plainte</h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — ENTRÉE</p>
          </div>
        </div>
        {canEdit && (
          <Button variant="outline" onClick={() => navigate(`/pj/plainte/${entry.id}/edit`)} className="gap-2">
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
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
              {PLAINTE_ENTREE_TYPE_LABELS[entry.type] ?? entry.type}
            </span>
            <span className="font-mono text-sm font-bold">{entry.numero_dossier}</span>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <DetailRow label="Date" value={formatDate(entry.date_plainte)} />
            {entry.numero_st && <DetailRow label="Numéro du ST" value={entry.numero_st} />}
            <DetailRow
              label="OPJ"
              value={[entry.opj_prenoms, entry.opj_nom].filter(Boolean).join(" ") + (entry.opj_grade ? ` (${entry.opj_grade})` : "")}
            />
            <DetailRow
              label="Enquêteur"
              value={[entry.enqueteur_prenoms, entry.enqueteur_nom].filter(Boolean).join(" ") + (entry.enqueteur_grade ? ` (${entry.enqueteur_grade})` : "")}
            />
            {entry.partie_civile && <DetailRow label="Partie civile (PC)" value={entry.partie_civile} />}
            <DetailRow label="Mise en cause (MC)" value={entry.mise_en_cause} />
            {entry.adresse_pc && <DetailRow label="Adresse du PC" value={entry.adresse_pc} />}
            <DetailRow label="Infraction" value={entry.infraction} />
            <DetailRow label="Préjudice" value={entry.prejudice} />
            <DetailRow label="Lieu de l'infraction" value={entry.lieu_infraction} />
            <DetailRow label="Heure de l'infraction" value={formatHeure(entry.heure_infraction)} />
          </div>
          {entry.observation && (
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Observation</p>
              <p className="text-sm whitespace-pre-wrap">{entry.observation}</p>
            </div>
          )}
          {(entry.agent_prenoms || entry.agent_nom || entry.agent_username) && (
            <DetailRow
              label="Agent"
              value={[entry.agent_prenoms, entry.agent_nom].filter(Boolean).join(" ") || entry.agent_username}
            />
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
            <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
          ) : (
            <div className="space-y-2">
              {attachments.map((att) => (
                <div key={att.id} className="flex items-center gap-2 rounded-md border p-3">
                  <div className="flex-1">
                    <p className="text-sm font-medium">{att.title}</p>
                    <p className="text-xs text-muted-foreground">{att.original_filename}</p>
                  </div>
                  {isImageFile(att.mime_type, att.original_filename) ? (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      onClick={() => setViewerTarget(att)}
                    >
                      <Eye className="h-3.5 w-3.5" />
                    </Button>
                  ) : null}
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => window.open(getPlainteEntreeAttachmentDownloadUrl(entry.id, att.id), "_blank")}
                  >
                    <Download className="h-3.5 w-3.5" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create SORTIE action */}
      {canCreate && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Sortie</CardTitle>
          </CardHeader>
          <CardContent>
            <Button onClick={() => navigate(`/pj/plainte/sortie/new?entreeId=${entry.id}`)} className="gap-2">
              <ArrowRightLeft className="h-4 w-4" />
              Créer une sortie pour cette plainte
            </Button>
          </CardContent>
        </Card>
      )}

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={viewerTarget ? getPlainteEntreeAttachmentDownloadUrl(entry.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
