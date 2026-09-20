import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getRassemblementById, deleteRassemblement } from "@/lib/api/rassemblement-journalier";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Pencil, Trash2, ArrowLeft } from "lucide-react";
import {
  RepartitionSecteurTable,
  type EditableRepartitionRow,
} from "@/components/rassemblement-journalier/repartition-secteur-table";
import type { RassemblementJournalier } from "@/types";

const SG_RASSEMBLEMENT_MODULE = "sg_rassemblement_journalier";

export function RassemblementJournalierDetail() {
  const { id } = useParams();
  const rassemblementId = id ? Number(id) : 0;
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, SG_RASSEMBLEMENT_MODULE, "can_edit");
  const canDelete = hasPermission(user, SG_RASSEMBLEMENT_MODULE, "can_delete");

  const [item, setItem] = useState<RassemblementJournalier | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function load() {
    try {
      const data = await getRassemblementById(rassemblementId);
      setItem(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger le rassemblement");
      navigate("/sg/rassemblement-journalier");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      await deleteRassemblement(rassemblementId);
      addNotification("success", "Supprimé", "Rassemblement supprimé avec succès");
      navigate("/sg/rassemblement-journalier");
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le rassemblement");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return <div className="text-center py-8 text-muted-foreground">Chargement...</div>;
  }
  if (!item) return null;

  // Répartition par secteur — Diurne / Nocturne share the same structure.
  const repartitionRows: EditableRepartitionRow[] = (item.repartitions ?? []).map((r) => ({
    _key: String(r.id),
    type: r.type,
    secteur: r.secteur,
    effectif_engage: r.effectif_engage,
    chef_element_contact: r.chef_element_contact,
    controle_contact: r.controle_contact,
    materiels_armements: r.materiels_armements,
    missions: r.missions,
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate("/sg/rassemblement-journalier")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">
              Rassemblement du {new Date(item.date_rassemblement).toLocaleDateString("fr-FR")} à {item.heure_rassemblement}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">{item.brigade_service}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {canEdit && (
            <Button variant="outline" onClick={() => navigate(`/sg/rassemblement-journalier/${item.id}/edit`)}>
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

      {/* Main fields + situation de prise d'arme (part of the record) */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Informations générales</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <DetailField label="Date" value={new Date(item.date_rassemblement).toLocaleDateString("fr-FR")} />
            <DetailField label="Heure" value={item.heure_rassemblement} />
            <DetailField label="Brigade de service" value={item.brigade_service} />
            <DetailField label="Officier de permanence" value={item.officier_permanence} />
            <DetailField label="Inspecteur de permanence" value={item.inspecteur_permanence} />
            <DetailField label="Chef de poste" value={item.chef_poste} />
          </div>

          <div className="space-y-3 border-t pt-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Situation de prise d'arme
            </p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <DetailField label="Effectif théorique" value={String(item.effectif_theorique)} />
              <DetailField label="Présent" value={String(item.present)} />
              <DetailField label="Absent" value={String(item.absent)} />
              <DetailField label="Motif d'absence" value={item.motif_absence} multiline />
            </div>
          </div>

          <div className="border-t pt-4">
            <DetailField label="Instructions de l'autorité" value={item.instructions_autorite} multiline />
          </div>
        </CardContent>
      </Card>

      {/* Répartition par secteur — one unified table (Diurne / Nocturne) */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Répartition par secteur</CardTitle>
        </CardHeader>
        <CardContent>
          <RepartitionSecteurTable rows={repartitionRows} readOnly />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteOpen}
        onCancel={() => !deleting && setDeleteOpen(false)}
        onConfirm={handleDelete}
        title="Supprimer le rassemblement"
        message="Voulez-vous vraiment supprimer ce rassemblement ?"
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
