import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getPersonnelById,
  getPersonnelAttachments,
  getAttachmentDownloadUrl,
  getPersonnelPhotoUrl,
} from "@/lib/api/personnel";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  ArrowLeft,
  Pencil,
  Loader2,
  User as UserIcon,
  Briefcase,
  Paperclip,
  Download,
  Camera,
} from "lucide-react";
import type { Personnel, PersonnelAttachment } from "@/types";

function InfoRow({ label, value }: { label: string; value: string | null }) {
  if (!value) return null;
  return (
    <div className="flex items-start gap-2">
      <span className="text-sm text-muted-foreground min-w-[160px] shrink-0">
        {label}
      </span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}

export function PersonnelDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [person, setPerson] = useState<Personnel | null>(null);
  const [attachments, setAttachments] = useState<PersonnelAttachment[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, [id]);

  async function loadData() {
    setLoading(true);
    try {
      const p = await getPersonnelById(Number(id));
      setPerson(p);
      const atts = await getPersonnelAttachments(Number(id));
      setAttachments(atts);
    } catch {
      addNotification("error", "Erreur", "Personnel introuvable");
      navigate("/personnel");
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

  if (!person) return null;

  const photoUrl = person.photo ? getPersonnelPhotoUrl(person.id) : null;

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate("/personnel")}
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">
              {person.firstname} {person.lastname}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {person.grade} — {person.fonction}
            </p>
          </div>
        </div>
        <Button
          onClick={() => navigate(`/personnel/${person.id}/edit`)}
          className="gap-2"
        >
          <Pencil className="h-4 w-4" />
          Modifier
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="md:col-span-1">
          <CardContent className="pt-6 flex flex-col items-center gap-3">
            <div className="h-32 w-32 rounded-full overflow-hidden border-2 border-border bg-muted flex items-center justify-center">
              {photoUrl ? (
                <img
                  src={photoUrl}
                  alt={`${person.firstname} ${person.lastname}`}
                  className="h-full w-full object-cover"
                />
              ) : (
                <Camera className="h-10 w-10 text-muted-foreground" />
              )}
            </div>
            <div className="text-center">
              <p className="font-semibold text-lg">
                {person.firstname} {person.lastname}
              </p>
              <p className="text-sm text-muted-foreground">{person.grade}</p>
            </div>
            <Badge
              variant={person.status === "active" ? "default" : "secondary"}
              className="mt-1"
            >
              {person.status === "active" ? "Actif" : "Inactif"}
            </Badge>
          </CardContent>
        </Card>

        <div className="md:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <UserIcon className="h-4 w-4" />
                Informations personnelles
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <InfoRow label="IM (Indice Matricule)" value={person.im} />
              <InfoRow label="Matricule" value={person.matricule} />
              <InfoRow label="Nom" value={person.lastname} />
              <InfoRow label="Prénom" value={person.firstname} />
              <InfoRow label="CIN" value={person.cin} />
              <InfoRow label="Date de naissance" value={person.date_naissance} />
              <InfoRow label="Lieu de naissance" value={person.lieu_naissance} />
              <InfoRow label="Adresse" value={person.adresse} />
              <InfoRow label="Email" value={person.email} />
              <InfoRow label="Téléphone" value={person.phone} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Briefcase className="h-4 w-4" />
                Informations professionnelles
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <InfoRow label="Grade" value={person.grade} />
              <InfoRow label="Fonction" value={person.fonction} />
              <InfoRow label="Service" value={person.service} />
              <InfoRow
                label="Date de prise de service"
                value={person.date_prise_service}
              />
            </CardContent>
          </Card>

          {attachments.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-base flex items-center gap-2">
                  <Paperclip className="h-4 w-4" />
                  Pièces jointes ({attachments.length})
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {attachments.map((att) => (
                  <div
                    key={att.id}
                    className="flex items-center justify-between rounded-lg border border-border p-3"
                  >
                    <div>
                      <p className="text-sm font-medium">{att.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {att.original_filename}
                      </p>
                    </div>
                    <a
                      href={getAttachmentDownloadUrl(person.id, att.id)}
                      download
                      className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
                    >
                      <Download className="h-3.5 w-3.5" />
                      Télécharger
                    </a>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </motion.div>
  );
}
