import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getDeclarationPerteById,
  getDeclarationPerteAttachmentDownloadUrl,
} from "@/lib/api/declaration-perte";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  FileWarning,
  PackageSearch,
  MapPin,
  FileBadge,
  Paperclip,
  Download,
  Eye,
} from "lucide-react";
import type { DeclarationPerte, DeclarationPerteAttachment } from "@/types";
import {
  DECLARATION_PERTE_MODULE,
  formatDate,
  formatHeure,
} from "@/pages/declaration-perte-list";

const LIST_PATH = "/sedentaire/secretariat/declaration-perte";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function DeclarationPerteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, DECLARATION_PERTE_MODULE, "can_edit");

  const [declaration, setDeclaration] = useState<DeclarationPerte | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<DeclarationPerteAttachment | null>(null);

  useEffect(() => {
    loadDeclaration();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadDeclaration() {
    setLoading(true);
    try {
      const data = await getDeclarationPerteById(Number(id));
      setDeclaration(data);
    } catch {
      addNotification("error", "Erreur", "Déclaration de perte introuvable");
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

  if (!declaration) return null;

  const agent =
    [declaration.agent_prenoms, declaration.agent_nom]
      .filter(Boolean)
      .join(" ") || declaration.agent_username;

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
              {declaration.numero_attestation}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {declaration.identite_declarant} — {declaration.nature_objet}
            </p>
          </div>
        </div>
        {canEdit && (
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => navigate(`${LIST_PATH}/${declaration.id}/edit`)}
          >
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <FileWarning className="h-4 w-4" />
            Déclaration
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de déclaration" value={formatDate(declaration.date_declaration)} />
          <DetailRow
            label="Heure de déclaration"
            value={formatHeure(declaration.heure_declaration)}
          />
          <DetailRow label="Identité du déclarant" value={declaration.identite_declarant} />
          <DetailRow label="Nom de l'agent" value={declaration.nom_agent} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <PackageSearch className="h-4 w-4" />
            Objet perdu
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <DetailRow label="Nature de l'objet" value={declaration.nature_objet} />
          <DetailRow
            label="Description de l'objet"
            value={
              <span className="whitespace-pre-wrap">{declaration.description_objet}</span>
            }
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <MapPin className="h-4 w-4" />
            Perte présumée
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de perte" value={formatDate(declaration.date_perte)} />
          <DetailRow label="Lieu de perte" value={declaration.lieu_perte} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <FileBadge className="h-4 w-4" />
            Attestation
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Numéro d'attestation" value={declaration.numero_attestation} />
          <DetailRow label="Enregistrée par" value={agent} />
          <DetailRow
            label="Enregistrée le"
            value={declaration.created_at
              ? new Date(declaration.created_at.replace(" ", "T")).toLocaleString("fr-FR")
              : "—"}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Fichiers joints ({declaration.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(declaration.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
          )}
          {(declaration.attachments ?? []).map((att) => (
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
                  href={getDeclarationPerteAttachmentDownloadUrl(declaration.id, att.id)}
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
            ? getDeclarationPerteAttachmentDownloadUrl(declaration.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
