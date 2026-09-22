import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPlainteSortieById,
  getPlainteSortieAttachments,
  getPlainteSortieAttachmentDownloadUrl,
  PLAINTE_SORTIE_NATURE_LABELS,
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
} from "lucide-react";
import type { PlainteSortie, PlainteSortieAttachment } from "@/types";

const PJ_PLAINTE_MODULE = "pj_plainte";

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

export function PlainteSortieDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_PLAINTE_MODULE, "can_edit");

  const [sortie, setSortie] = useState<PlainteSortie | null>(null);
  const [attachments, setAttachments] = useState<PlainteSortieAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<PlainteSortieAttachment | null>(null);

  useEffect(() => {
    loadSortie();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSortie() {
    setLoading(true);
    try {
      const data = await getPlainteSortieById(Number(id));
      setSortie(data);
      try {
        const atts = await getPlainteSortieAttachments(Number(id));
        setAttachments(atts);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Sortie introuvable");
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

  if (!sortie) return null;

  const entreeOpjName = [sortie.entree_opj_prenoms, sortie.entree_opj_nom]
    .filter(Boolean)
    .join(" ") + (sortie.entree_opj_grade ? ` (${sortie.entree_opj_grade})` : "");

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/plainte")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">Détail de la sortie</h1>
              <p className="text-sm text-muted-foreground mt-1">Police Judiciaire — SORTIE</p>
            </div>
          </div>
          {canEdit && (
            <Button variant="outline" onClick={() => navigate(`/pj/plainte/sortie/${sortie.id}/edit`)} className="gap-2">
              <Pencil className="h-4 w-4" />
              Modifier
            </Button>
          )}
        </div>
      </div>

      {/* ENTRÉE associée */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Plainte ENTRÉE associée</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center gap-2">
            {sortie.entree_type && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                {PLAINTE_ENTREE_TYPE_LABELS[sortie.entree_type] ?? sortie.entree_type}
              </span>
            )}
            {sortie.entree_numero_dossier && (
              <span className="font-mono text-sm font-bold">{sortie.entree_numero_dossier}</span>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <DetailRow label="Date" value={formatDate(sortie.entree_date_plainte)} />
            <DetailRow label="Infraction" value={sortie.entree_infraction} />
            <DetailRow label="Mise en cause (MC)" value={sortie.entree_mise_en_cause} />
            {sortie.entree_partie_civile && <DetailRow label="Partie civile (PC)" value={sortie.entree_partie_civile} />}
            <DetailRow label="OPJ" value={entreeOpjName} />
          </div>
        </CardContent>
      </Card>

      {/* Sortie details */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Sortie</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2">
            <span className="text-xs px-2 py-0.5 rounded-full bg-secondary/10 text-secondary-foreground font-medium">
              {PLAINTE_SORTIE_NATURE_LABELS[sortie.nature] ?? sortie.nature}
            </span>
            <span className="font-mono text-sm font-bold">{sortie.numero}</span>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <DetailRow label="Date" value={formatDate(sortie.date_sortie)} />
            <DetailRow label="N° TTR" value={sortie.numero_ttr} />
            <DetailRow label="Nom du Substitut" value={sortie.nom_substitut} />
            {sortie.date_deferrement && <DetailRow label="Date du déferrement" value={formatDate(sortie.date_deferrement)} />}
          </div>
          {sortie.observation && (
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Observation</p>
              <p className="text-sm whitespace-pre-wrap">{sortie.observation}</p>
            </div>
          )}
          {(sortie.agent_prenoms || sortie.agent_nom || sortie.agent_username) && (
            <DetailRow
              label="Agent"
              value={[sortie.agent_prenoms, sortie.agent_nom].filter(Boolean).join(" ") || sortie.agent_username}
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
                    onClick={() => window.open(getPlainteSortieAttachmentDownloadUrl(sortie.id, att.id), "_blank")}
                  >
                    <Download className="h-3.5 w-3.5" />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={viewerTarget ? getPlainteSortieAttachmentDownloadUrl(sortie.id, viewerTarget.id) : ""}
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
