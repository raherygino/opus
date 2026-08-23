import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPassationById,
  getPassationAttachmentDownloadUrl,
} from "@/lib/api/passation";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Handshake,
  UserCheck,
  ShieldCheck,
  Paperclip,
  Download,
  Eye,
} from "lucide-react";
import type { Passation, PassationAttachment } from "@/types";
import {
  PASSATION_MODULE,
  formatDate,
  formatHeure,
} from "@/pages/passation-list";

const LIST_PATH = "/sedentaire/poste/passation";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function PassationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PASSATION_MODULE, "can_edit");

  const [passation, setPassation] = useState<Passation | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<PassationAttachment | null>(null);

  useEffect(() => {
    loadPassation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadPassation() {
    setLoading(true);
    try {
      const data = await getPassationById(Number(id));
      setPassation(data);
    } catch {
      addNotification("error", "Erreur", "Passation introuvable");
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

  if (!passation) return null;

  const descendant =
    [passation.chef_descendant_grade, passation.chef_descendant_lastname]
      .filter(Boolean)
      .join(" ") || passation.chef_descendant_username || "—";
  const montant =
    [passation.chef_montant_grade, passation.chef_montant_lastname]
      .filter(Boolean)
      .join(" ") || passation.chef_montant_username || "—";

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
              Passation du {formatDate(passation.date_passation)}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {formatHeure(passation.heure_passation)} — {descendant} → {montant}
            </p>
          </div>
        </div>
        {canEdit && (
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => navigate(`${LIST_PATH}/${passation.id}/edit`)}
          >
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Handshake className="h-4 w-4" />
            Passation
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de la passation" value={formatDate(passation.date_passation)} />
          <DetailRow label="Heure de la passation" value={formatHeure(passation.heure_passation)} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserCheck className="h-4 w-4" />
            Chef de poste descendant
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Grade" value={passation.chef_descendant_grade} />
          <DetailRow label="Nom complet" value={passation.chef_descendant_lastname} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <ShieldCheck className="h-4 w-4" />
            Chef de poste montant
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Grade" value={passation.chef_montant_grade} />
          <DetailRow label="Nom complet" value={passation.chef_montant_lastname} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Instructions & Incidents
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <DetailRow
            label="Instructions Autorité"
            value={
              <span className="whitespace-pre-wrap">{passation.instructions_autorite}</span>
            }
          />
          <DetailRow
            label="Incidents survenus"
            value={
              <span className="whitespace-pre-wrap">{passation.incidents_survenus}</span>
            }
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes ({passation.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(passation.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
          )}
          {(passation.attachments ?? []).map((att) => (
            <div
              key={att.id}
              className="flex items-center justify-between rounded-lg border border-border p-3"
            >
              <div>
                <p className="text-sm font-medium">{att.title}</p>
                <p className="text-xs text-muted-foreground">{att.original_filename}</p>
              </div>
              <div className="flex items-center gap-1">
                {isImageFile(att.mime_type, att.original_filename) && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    title="Aperçu"
                    onClick={() => setViewerTarget(att)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                )}
                <a
                  href={getPassationAttachmentDownloadUrl(passation.id, att.id)}
                  download
                >
                  <Button variant="ghost" size="icon" className="h-8 w-8" title="Télécharger">
                    <Download className="h-4 w-4" />
                  </Button>
                </a>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget
            ? getPassationAttachmentDownloadUrl(passation.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
