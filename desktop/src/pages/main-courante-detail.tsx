import { useState, useEffect } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getMainCouranteById,
  getMainCouranteAttachmentDownloadUrl,
} from "@/lib/api/main-courante";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  BookOpenText,
  Paperclip,
  Download,
  Eye,
} from "lucide-react";
import type { MainCourante, MainCouranteAttachment, MainCouranteOrigine } from "@/types";
import {
  listPathForOrigine,
  formatDate,
  formatHeure,
} from "@/pages/main-courante-list";

const CATEGORIE_COLORS: Record<string, string> = {
  "Entrée/Sortie de tiers": "bg-blue-500/10 text-blue-500",
  "Incident au poste": "bg-amber-500/10 text-amber-500",
  "Renseignement reçu": "bg-green-500/10 text-green-500",
};

function moduleForOrigine(origine: MainCouranteOrigine): string {
  return origine === "Poste"
    ? "sedentaire_poste_main_courante"
    : "sedentaire_secretariat_main_courante";
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function MainCouranteDetail() {
  const { id } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();

  const [entry, setEntry] = useState<MainCourante | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<MainCouranteAttachment | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getMainCouranteById(Number(id));
      setEntry(data);
    } catch {
      addNotification("error", "Erreur", "Main courante introuvable");
      navigate(location.pathname.includes("/poste/") ? "/sedentaire/poste/main-courante" : "/sedentaire/secretariat/main-courante");
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

  if (!entry) return null;

  const listPath = listPathForOrigine(entry.origine);
  const module = moduleForOrigine(entry.origine);
  const canEdit = hasPermission(user, module, "can_edit");
  const agent =
    [entry.agent_prenoms, entry.agent_nom]
      .filter(Boolean)
      .join(" ") || entry.agent_username;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(listPath)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight flex items-center gap-2">
              <span
                className={`text-xs px-2 py-0.5 rounded-full ${CATEGORIE_COLORS[entry.categorie] ?? "bg-muted text-muted-foreground"}`}
              >
                {entry.categorie}
              </span>
              {formatDate(entry.date_evenement)} {formatHeure(entry.heure_evenement)}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {entry.origine === "Poste" ? "Poste" : "Secrétariat"}
            </p>
          </div>
        </div>
        {canEdit && (
          <Button
            variant="outline"
            className="gap-2"
            onClick={() => navigate(`${listPath}/${entry.id}/edit`)}
          >
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <BookOpenText className="h-4 w-4" />
            Informations de l'événement
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <DetailRow label="Période (Date)" value={formatDate(entry.date_evenement)} />
            <DetailRow
              label="Heure précise"
              value={formatHeure(entry.heure_evenement)}
            />
            <DetailRow label="Catégorie" value={entry.categorie} />
            <DetailRow label="Agent" value={agent} />
          </div>
          <div className="space-y-1">
            <p className="text-xs text-muted-foreground">Description des faits</p>
            <p className="text-sm whitespace-pre-wrap">{entry.description}</p>
          </div>
          <DetailRow
            label="Enregistrée le"
            value={entry.created_at
              ? new Date(entry.created_at.replace(" ", "T")).toLocaleString("fr-FR")
              : "—"}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Fichiers joints ({entry.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(entry.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
          )}
          {(entry.attachments ?? []).map((att) => (
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
                  href={getMainCouranteAttachmentDownloadUrl(entry.id, att.id)}
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
            ? getMainCouranteAttachmentDownloadUrl(entry.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
