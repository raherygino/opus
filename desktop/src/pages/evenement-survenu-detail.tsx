import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getEvenementSurvenuById,
  deleteEvenementSurvenu,
  getEvenementSurvenuAttachmentDownloadUrl,
  EVENEMENT_TYPE_LABELS,
} from "@/lib/api/evenement-survenu";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Pencil, Trash2, ArrowLeft, Paperclip, Download, Eye } from "lucide-react";
import type { EvenementSurvenu, EvenementSurvenuAttachment } from "@/types";

const SG_EVENEMENT_MODULE = "sg_evenement_survenu";

export function EvenementSurvenuDetail() {
  const { id } = useParams();
  const evenementId = id ? Number(id) : 0;
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, SG_EVENEMENT_MODULE, "can_edit");
  const canDelete = hasPermission(user, SG_EVENEMENT_MODULE, "can_delete");

  const [item, setItem] = useState<EvenementSurvenu | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [viewerTarget, setViewerTarget] = useState<EvenementSurvenuAttachment | null>(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    try {
      const data = await getEvenementSurvenuById(evenementId);
      setItem(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger l'évènement");
      navigate("/sg/evenements-survenus");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      await deleteEvenementSurvenu(evenementId);
      addNotification("success", "Supprimé", "Évènement supprimé avec succès");
      navigate("/sg/evenements-survenus");
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'évènement");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Chargement...</div>;
  }
  if (!item) return null;

  const agentName =
    [item.agent_prenoms, item.agent_nom].filter(Boolean).join(" ") ||
    item.agent_username ||
    null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/sg/evenements-survenus")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">
              Évènement du {new Date(item.date_evenement).toLocaleDateString("fr-FR")} à {item.heure_evenement}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              Évènements survenus sur la voie publique
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {canEdit && (
            <Button variant="outline" onClick={() => navigate(`/sg/evenements-survenus/${item.id}/edit`)}>
              <Pencil className="h-4 w-4 mr-2" />
              Modifier
            </Button>
          )}
          {canDelete && (
            <Button variant="outline" onClick={() => setDeleteOpen(true)}>
              <Trash2 className="h-4 w-4 mr-2 text-destructive" />
              Supprimer
            </Button>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Informations générales</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <DetailField label="Date" value={new Date(item.date_evenement).toLocaleDateString("fr-FR")} />
            <DetailField label="Heure" value={item.heure_evenement} />
            <DetailField
              label="Type d'événement"
              value={EVENEMENT_TYPE_LABELS[item.type_evenement] ?? item.type_evenement}
            />
          </div>
          <div className="border-t pt-4">
            <DetailField label="Lieu exact" value={item.lieu_exact} />
          </div>
          {item.latitude != null && item.longitude != null && (
            <div className="border-t pt-4">
              <DetailField
                label="Position GPS"
                value={`${Number(item.latitude).toFixed(6)}, ${Number(item.longitude).toFixed(6)}`}
              />
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Identités des parties impliquées</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <DetailField label="Auteur(s) présumé(s)" value={item.auteurs_presumes} multiline />
          <div className="border-t pt-4">
            <DetailField label="Victime(s)" value={item.victimes} multiline />
          </div>
          <div className="border-t pt-4">
            <DetailField label="Témoin(s)" value={item.temoins} multiline />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Mesures prises</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <DetailField label="Mesures prises" value={item.mesures_prises} multiline />
          {agentName && (
            <div className="border-t pt-4">
              <DetailField label="Agent" value={agentName} />
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Fichiers joints ({item.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(item.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucun fichier joint</p>
          )}
          {(item.attachments ?? []).map((att) => (
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
                  href={getEvenementSurvenuAttachmentDownloadUrl(item.id, att.id)}
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
            ? getEvenementSurvenuAttachmentDownloadUrl(item.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />

      <ConfirmDialog
        open={deleteOpen}
        onCancel={() => !deleting && setDeleteOpen(false)}
        onConfirm={handleDelete}
        title="Supprimer l'évènement"
        message="Voulez-vous vraiment supprimer cet évènement ?"
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}

function DetailField({ label, value, multiline }: { label: string; value: string | null; multiline?: boolean }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</p>
      <p className={`text-sm ${multiline ? "whitespace-pre-wrap" : ""}`}>{value || "—"}</p>
    </div>
  );
}
