import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPersonneRechercheeById,
  getPersonneRechercheePhotos,
  getPersonneRechercheePhotoDownloadUrl,
} from "@/lib/api/personne-recherchee";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Camera,
  ImageIcon,
} from "lucide-react";
import type {
  PersonneRecherchee,
  PersonneRechercheePhoto,
} from "@/types";

const PJ_MODULE = "pj_personne_recherchee";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm whitespace-pre-wrap">{value || "—"}</p>
    </div>
  );
}

export function PersonneRechercheeDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PJ_MODULE, "can_edit");

  const [entry, setEntry] = useState<PersonneRecherchee | null>(null);
  const [photos, setPhotos] = useState<PersonneRechercheePhoto[]>([]);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<PersonneRechercheePhoto | null>(null);

  useEffect(() => {
    loadEntry();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadEntry() {
    setLoading(true);
    try {
      const data = await getPersonneRechercheeById(Number(id));
      setEntry(data);
      try {
        const list = await getPersonneRechercheePhotos(Number(id));
        setPhotos(list);
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Personne recherchée introuvable");
      navigate("/pj/personne-recherchee");
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
          <Button variant="ghost" size="icon" onClick={() => navigate("/pj/personne-recherchee")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              Détail — Personne recherchée
            </h1>
            <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
          </div>
        </div>
        {canEdit && (
          <Button onClick={() => navigate(`/pj/personne-recherchee/${entry.id}/edit`)} className="gap-2">
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Identité</CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <DetailRow label="Nom" value={entry.nom} />
          <DetailRow label="Adresse" value={entry.adresse} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Motif</CardTitle>
        </CardHeader>
        <CardContent>
          <DetailRow label="Motif de la recherche" value={entry.motif} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Camera className="h-4 w-4" />
            Photos ({photos.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {photos.length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune photo.</p>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {photos.map((photo) => (
                <div key={photo.id} className="space-y-1">
                  <div
                    className="relative aspect-square rounded-lg border overflow-hidden bg-muted cursor-pointer"
                    onClick={() => setViewerTarget(photo)}
                  >
                    <img
                      src={getPersonneRechercheePhotoDownloadUrl(entry.id, photo.id)}
                      alt={photo.caption ?? photo.original_filename}
                      className="h-full w-full object-cover"
                      loading="lazy"
                    />
                    {photo.capture_source && (
                      <span className="absolute top-1 left-1 text-[10px] px-1.5 py-0.5 rounded bg-black/60 text-white">
                        {photo.capture_source === "CAMERA" ? "Caméra" : "Galerie"}
                      </span>
                    )}
                  </div>
                  {photo.caption ? (
                    <p className="text-xs text-muted-foreground truncate">{photo.caption}</p>
                  ) : (
                    <p className="text-xs text-muted-foreground/60 flex items-center gap-1">
                      <ImageIcon className="h-3 w-3" />
                      {photo.original_filename}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ImageViewerDialog
        src={
          viewerTarget
            ? getPersonneRechercheePhotoDownloadUrl(entry.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.caption ?? viewerTarget?.original_filename}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
