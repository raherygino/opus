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
import type { RassemblementJournalier, RepartitionSecteur } from "@/types";

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

      {/* Main fields */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Informations générales</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <DetailField label="Date" value={new Date(item.date_rassemblement).toLocaleDateString("fr-FR")} />
          <DetailField label="Heure" value={item.heure_rassemblement} />
          <DetailField label="Brigade de service" value={item.brigade_service} />
          <DetailField label="Officier de permanence" value={item.officier_permanence} />
          <DetailField label="Inspecteur de permanence" value={item.inspecteur_permanence} />
          <DetailField label="Chef de poste" value={item.chef_poste} />
          <div className="sm:col-span-2 lg:col-span-3">
            <DetailField label="Instructions de l'autorité" value={item.instructions_autorite} multiline />
          </div>
        </CardContent>
      </Card>

      {/* Situation de prise d'arme */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Situation de prise d'arme</CardTitle>
        </CardHeader>
        <CardContent>
          {(item.situations_prise_arme ?? []).length === 0 ? (
            <p className="text-sm text-muted-foreground">Aucune situation enregistrée</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-2 px-3 font-medium">EFFECTIF THÉORIQUE</th>
                    <th className="text-left py-2 px-3 font-medium">PRÉSENT</th>
                    <th className="text-left py-2 px-3 font-medium">ABSENT</th>
                    <th className="text-left py-2 px-3 font-medium">MOTIF D'ABSENCE</th>
                  </tr>
                </thead>
                <tbody>
                  {(item.situations_prise_arme ?? []).map((s) => (
                    <tr key={s.id} className="border-b last:border-0">
                      <td className="py-2 px-3">{s.effectif_theorique}</td>
                      <td className="py-2 px-3">{s.present}</td>
                      <td className="py-2 px-3">{s.absent}</td>
                      <td className="py-2 px-3">{s.motif_absence || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Répartition par secteur – Diurne */}
      <RepartitionTable title="Répartition par secteur – Diurne" rows={item.repartitions_diurne ?? []} />

      {/* Répartition par secteur – Nocturne */}
      <RepartitionTable title="Répartition par secteur – Nocturne" rows={item.repartitions_nocturne ?? []} />

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

function RepartitionTable({
  title,
  rows,
}: {
  title: string;
  rows: RepartitionSecteur[];
}) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {rows.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucune répartition enregistrée</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2 px-3 font-medium">SECTEUR</th>
                  <th className="text-left py-2 px-3 font-medium">EFFECTIF ENGAGÉ</th>
                  <th className="text-left py-2 px-3 font-medium">CHEF D'ÉLÉMENT AVEC CONTACT</th>
                  <th className="text-left py-2 px-3 font-medium">CONTRÔLE AVEC CONTACT</th>
                  <th className="text-left py-2 px-3 font-medium">MATÉRIELS ET ARMEMENTS</th>
                  <th className="text-left py-2 px-3 font-medium">MISSIONS</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id} className="border-b last:border-0 align-top">
                    <td className="py-2 px-3">{r.secteur}</td>
                    <td className="py-2 px-3">{r.effectif_engage || "—"}</td>
                    <td className="py-2 px-3">{r.chef_element_contact || "—"}</td>
                    <td className="py-2 px-3">{r.controle_contact || "—"}</td>
                    <td className="py-2 px-3 whitespace-pre-wrap">{r.materiels_armements || "—"}</td>
                    <td className="py-2 px-3 whitespace-pre-wrap">{r.missions || "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
