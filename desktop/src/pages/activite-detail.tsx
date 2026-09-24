import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getActiviteById,
  deleteActivite,
  getActiviteAttachmentDownloadUrl,
  PATROUILLE_TYPES,
  PATROUILLE_MODES,
  patrouilleItineraireField,
} from "@/lib/api/activite";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Loader2,
  Download,
  Paperclip,
  Image as ImageIcon,
} from "lucide-react";
import type { Activite, ActiviteAttachment } from "@/types";

const SG_ACTIVITE_MODULE = "sg_activite";

function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <dt className="text-xs font-medium text-muted-foreground uppercase tracking-wide">
        {label}
      </dt>
      <dd className="mt-1 text-sm whitespace-pre-wrap">{value || "—"}</dd>
    </div>
  );
}

export function ActiviteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, SG_ACTIVITE_MODULE, "can_edit");
  const canDelete = hasPermission(user, SG_ACTIVITE_MODULE, "can_delete");

  const [entry, setEntry] = useState<Activite | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [viewerTarget, setViewerTarget] = useState<ActiviteAttachment | null>(null);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function load() {
    setLoading(true);
    try {
      const data = await getActiviteById(Number(id));
      setEntry(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger l'activité");
      navigate("/sg/activites");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      await deleteActivite(Number(id));
      addNotification("success", "Supprimée", "Activité supprimée avec succès");
      navigate("/sg/activites");
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer l'activité");
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!entry) return null;

  const agentName =
    [entry.agent_prenoms, entry.agent_nom].filter(Boolean).join(" ") ||
    entry.agent_username ||
    "—";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/sg/activites")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Activité</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Patrouilles et interventions — {new Date(entry.date_activite).toLocaleDateString("fr-FR")} à {entry.heure_activite}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {canEdit && (
            <Button variant="outline" onClick={() => navigate(`/sg/activites/${entry.id}/edit`)}>
              <Pencil className="h-4 w-4 mr-2" />
              Modifier
            </Button>
          )}
          {canDelete && (
            <Button variant="destructive" onClick={() => setConfirmDelete(true)}>
              <Trash2 className="h-4 w-4 mr-2" />
              Supprimer
            </Button>
          )}
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-6"
      >
        {/* ── Informations générales ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Informations générales</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <Field label="Date" value={new Date(entry.date_activite).toLocaleDateString("fr-FR")} />
              <Field label="Heure" value={entry.heure_activite} />
              <Field
                label="Position GPS"
                value={
                  entry.latitude != null && entry.longitude != null
                    ? `${Number(entry.latitude).toFixed(6)}, ${Number(entry.longitude).toFixed(6)}`
                    : null
                }
              />
            </dl>
          </CardContent>
        </Card>

        {/* ── Type de patrouille ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Type de patrouille</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              {PATROUILLE_TYPES.map((type) => {
                const selectedModes = PATROUILLE_MODES.filter(
                  (mode) => entry[patrouilleItineraireField(type.key, mode.key)] !== null,
                );
                return (
                  <div key={type.key} className="space-y-2">
                    <h3 className="font-semibold text-sm text-primary">{type.label}</h3>
                    {selectedModes.length === 0 ? (
                      <p className="text-sm text-muted-foreground">Aucune</p>
                    ) : (
                      <dl className="space-y-2">
                        {selectedModes.map((mode) => (
                          <Field
                            key={mode.key}
                            label={mode.label}
                            value={
                              entry[patrouilleItineraireField(type.key, mode.key)] ||
                              "Itinéraire non précisé"
                            }
                          />
                        ))}
                      </dl>
                    )}
                  </div>
                );
              })}
            </div>
          </CardContent>
        </Card>

        {/* ── Opération & faits ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Opération &amp; faits</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-4">
              <Field label="Opération ciblée" value={entry.operation_ciblee} />
              <Field label="Faits constatés" value={entry.faits_constates} />
            </dl>
          </CardContent>
        </Card>

        {/* ── Hiérarchie ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Hiérarchie</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-4">
              <Field label="Compte-rendu hiérarchie" value={entry.compte_rendu_hierarchie} />
              <Field label="Conduite à tenir" value={entry.conduite_a_tenir} />
            </dl>
          </CardContent>
        </Card>

        {/* ── Intervention ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Intervention</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="space-y-4">
              <Field label="Nature de l'intervention" value={entry.nature_intervention} />
              <Field label="Suites données" value={entry.suites_donnees} />
              <Field label="Agent" value={agentName} />
            </dl>
          </CardContent>
        </Card>

        {/* ── Pièces jointes ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">
              Pièces jointes ({entry.attachments?.length ?? 0})
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!entry.attachments || entry.attachments.length === 0 ? (
              <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
            ) : (
              <ul className="space-y-2">
                {entry.attachments.map((att) => {
                  const isImage = isImageFile(att.mime_type, att.original_filename);
                  return (
                    <li
                      key={att.id}
                      className="flex items-center gap-3 rounded-md border px-3 py-2"
                    >
                      <Paperclip className="h-4 w-4 shrink-0 text-muted-foreground" />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium truncate">{att.title}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {att.original_filename}
                          {att.file_size ? ` — ${(att.file_size / 1024).toFixed(1)} Ko` : ""}
                        </p>
                      </div>
                      {isImage && (
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          title="Aperçu"
                          onClick={() => setViewerTarget(att)}
                        >
                          <ImageIcon className="h-3.5 w-3.5" />
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        title="Télécharger"
                        onClick={() =>
                          window.open(
                            getActiviteAttachmentDownloadUrl(entry.id, att.id),
                            "_blank",
                          )
                        }
                      >
                        <Download className="h-3.5 w-3.5" />
                      </Button>
                    </li>
                  );
                })}
              </ul>
            )}
          </CardContent>
        </Card>
      </motion.div>

      <ConfirmDialog
        open={confirmDelete}
        onCancel={() => setConfirmDelete(false)}
        onConfirm={handleDelete}
        title="Supprimer l'activité"
        message="Voulez-vous vraiment supprimer cette activité ? Cette action est irréversible."
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget
            ? getActiviteAttachmentDownloadUrl(entry.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </div>
  );
}
