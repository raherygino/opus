import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getDispositifExceptionnelById,
  deleteDispositifExceptionnel,
} from "@/lib/api/dispositif-exceptionnel";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Pencil, Trash2, ArrowLeft, Loader2 } from "lucide-react";
import {
  EffectifEngageTable,
  type EditableEffectifRow,
} from "@/components/dispositif-exceptionnel/effectif-engage-table";
import type { DispositifExceptionnel } from "@/types";

const SG_DISPOSITIF_MODULE = "sg_dispositif_exceptionnel";

export function DispositifExceptionnelDetail() {
  const { id } = useParams();
  const dispositifId = id ? Number(id) : 0;
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, SG_DISPOSITIF_MODULE, "can_edit");
  const canDelete = hasPermission(user, SG_DISPOSITIF_MODULE, "can_delete");

  const [item, setItem] = useState<DispositifExceptionnel | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function load() {
    try {
      const data = await getDispositifExceptionnelById(dispositifId);
      setItem(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger le dispositif exceptionnel");
      navigate("/sg/dispositifs-exceptionnels");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setDeleting(true);
    try {
      await deleteDispositifExceptionnel(dispositifId);
      addNotification("success", "Supprimé", "Dispositif exceptionnel supprimé avec succès");
      navigate("/sg/dispositifs-exceptionnels");
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer le dispositif exceptionnel");
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }
  if (!item) return null;

  const agentName =
    [item.agent_prenoms, item.agent_nom].filter(Boolean).join(" ") ||
    item.agent_username ||
    "—";

  const effectifRows: EditableEffectifRow[] = (item.effectifs ?? []).map((r) => ({
    _key: String(r.id),
    secteur: r.secteur,
    chef_element_contact: r.chef_element_contact,
    controle_contact: r.controle_contact,
    materiels_armements: r.materiels_armements,
    missions: r.missions,
  }));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="icon" onClick={() => navigate("/sg/dispositifs-exceptionnels")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">{item.nature_evenement}</h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Dispositif exceptionnel — du {new Date(item.date_debut).toLocaleDateString("fr-FR")} au{" "}
              {new Date(item.date_fin).toLocaleDateString("fr-FR")}
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {canEdit && (
            <Button variant="outline" onClick={() => navigate(`/sg/dispositifs-exceptionnels/${item.id}/edit`)}>
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
              <Field label="Nature de l'évènement" value={item.nature_evenement} />
              <Field
                label="Période"
                value={`Du ${new Date(item.date_debut).toLocaleDateString("fr-FR")} au ${new Date(item.date_fin).toLocaleDateString("fr-FR")}`}
              />
              <Field label="Agent" value={agentName} />
            </dl>
          </CardContent>
        </Card>

        {/* ── Effectif engagé ── */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Effectif engagé</CardTitle>
          </CardHeader>
          <CardContent>
            <EffectifEngageTable rows={effectifRows} readOnly />
          </CardContent>
        </Card>
      </motion.div>

      <ConfirmDialog
        open={deleteOpen}
        onCancel={() => !deleting && setDeleteOpen(false)}
        onConfirm={handleDelete}
        title="Supprimer le dispositif exceptionnel"
        message="Voulez-vous vraiment supprimer ce dispositif exceptionnel ?"
        confirmLabel="Supprimer"
        cancelLabel="Annuler"
        loading={deleting}
        variant="destructive"
      />
    </div>
  );
}

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
