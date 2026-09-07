import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getTypeMaterielList,
  createTypeMateriel,
  updateTypeMateriel,
  deleteTypeMateriel,
  getAffectationMaterielList,
  deleteAffectationMateriel,
  type TypeMaterielPayload,
} from "@/lib/api/materiel";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Plus,
  Pencil,
  Trash2,
  Tags,
  Package,
  Eye,
  Loader2,
} from "lucide-react";
import type { TypeMateriel, AffectationMateriel } from "@/types";
import { formatDate, formatHeure } from "@/pages/passation-list";

export const MATERIELS_MODULE = "sedentaire_poste_materiels";
const LIST_PATH = "/sedentaire/poste/materiels";

const EMPTY_TYPE_FORM: TypeMaterielPayload = { nom: "", description: "" };

export function MaterielsManagement() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTab = searchParams.get("tab") === "types" ? "types" : "affectations";
  const [tab, setTab] = useState(initialTab);

  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, MATERIELS_MODULE, "can_create");
  const canEdit = hasPermission(user, MATERIELS_MODULE, "can_edit");
  const canDelete = hasPermission(user, MATERIELS_MODULE, "can_delete");

  // ---- Types de matériel state ----
  const [types, setTypes] = useState<TypeMateriel[]>([]);
  const [typesLoading, setTypesLoading] = useState(true);
  const [typeEditorOpen, setTypeEditorOpen] = useState(false);
  const [editingType, setEditingType] = useState<TypeMateriel | null>(null);
  const [typeForm, setTypeForm] = useState<TypeMaterielPayload>(EMPTY_TYPE_FORM);
  const [typeSaving, setTypeSaving] = useState(false);
  const [typeDeleteTarget, setTypeDeleteTarget] = useState<TypeMateriel | null>(null);
  const [typeDeleting, setTypeDeleting] = useState(false);

  // ---- Affectations state ----
  const [affectations, setAffectations] = useState<AffectationMateriel[]>([]);
  const [affectationsLoading, setAffectationsLoading] = useState(true);
  const [affDeleteTarget, setAffDeleteTarget] = useState<AffectationMateriel | null>(null);
  const [affDeleting, setAffDeleting] = useState(false);

  useEffect(() => {
    loadTypes();
    loadAffectations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleTabChange(value: string) {
    setTab(value);
    setSearchParams(value === "types" ? { tab: "types" } : {}, { replace: true });
  }

  // ---- Types de matériel CRUD ----
  async function loadTypes() {
    setTypesLoading(true);
    try {
      const data = await getTypeMaterielList();
      setTypes(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les types de matériel");
    } finally {
      setTypesLoading(false);
    }
  }

  function openCreateType() {
    setEditingType(null);
    setTypeForm(EMPTY_TYPE_FORM);
    setTypeEditorOpen(true);
  }

  function openEditType(t: TypeMateriel) {
    setEditingType(t);
    setTypeForm({
      nom: t.nom,
      description: t.description ?? "",
    });
    setTypeEditorOpen(true);
  }

  function closeTypeEditor() {
    setTypeEditorOpen(false);
    setEditingType(null);
    setTypeForm(EMPTY_TYPE_FORM);
  }

  async function handleSaveType(e: React.FormEvent) {
    e.preventDefault();
    if (!typeForm.nom.trim()) {
      addNotification("error", "Validation", "Le nom du type de matériel est requis");
      return;
    }
    setTypeSaving(true);
    try {
      const payload: TypeMaterielPayload = {
        nom: typeForm.nom.trim(),
        description: typeForm.description?.trim() || null,
      };
      if (editingType) {
        await updateTypeMateriel(editingType.id, payload);
        addNotification("success", "Modifié", "Type de matériel modifié avec succès");
      } else {
        await createTypeMateriel(payload);
        addNotification("success", "Créé", "Type de matériel créé avec succès");
      }
      closeTypeEditor();
      loadTypes();
    } catch (err: unknown) {
      let msg = "Impossible d'enregistrer le type de matériel";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string; errors?: Record<string, string> } } }).response;
        if (resp?.data?.errors) {
          msg = Object.values(resp.data.errors).join(", ");
        } else if (resp?.data?.message) {
          msg = resp.data.message;
        }
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setTypeSaving(false);
    }
  }

  async function handleDeleteType(id: number) {
    setTypeDeleting(true);
    try {
      await deleteTypeMateriel(id);
      addNotification("success", "Supprimé", "Type de matériel supprimé avec succès");
      loadTypes();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer ce type de matériel";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setTypeDeleting(false);
      setTypeDeleteTarget(null);
    }
  }

  // ---- Affectations CRUD ----
  async function loadAffectations() {
    setAffectationsLoading(true);
    try {
      const data = await getAffectationMaterielList();
      setAffectations(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les affectations");
    } finally {
      setAffectationsLoading(false);
    }
  }

  async function handleDeleteAff(id: number) {
    setAffDeleting(true);
    try {
      await deleteAffectationMateriel(id);
      addNotification("success", "Supprimé", "Affectation supprimée avec succès");
      loadAffectations();
    } catch (err: unknown) {
      let msg = "Impossible de supprimer cette affectation";
      if (err && typeof err === "object" && "response" in err) {
        const resp = (err as { response: { data: { message?: string } } }).response;
        if (resp?.data?.message) msg = resp.data.message;
      }
      addNotification("error", "Erreur", msg);
    } finally {
      setAffDeleting(false);
      setAffDeleteTarget(null);
    }
  }

  // ---- Type columns ----
  const typeColumns: Column<TypeMateriel>[] = [
    {
      key: "nom",
      header: "Nom",
      sortable: true,
      render: (t) => <span className="font-medium">{t.nom}</span>,
    },
    {
      key: "description",
      header: "Description",
      sortable: false,
      render: (t) => t.description || "—",
    },
    {
      key: "created_at",
      header: "Créé le",
      sortable: true,
      render: (t) =>
        t.created_at ? t.created_at.slice(0, 10).split("-").reverse().join("/") : "—",
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[110px]",
      render: (t) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => openEditType(t)}
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
          )}
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              title="Supprimer"
              onClick={() => setTypeDeleteTarget(t)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  // ---- Affectation columns ----
  const affColumns: Column<AffectationMateriel>[] = [
    {
      key: "date_perception",
      header: "Date Perception",
      sortable: true,
      render: (a) =>
        `${formatDate(a.date_perception)} ${formatHeure(a.heure_perception)}`.trim(),
    },
    {
      key: "agent_nom",
      header: "Agent",
      sortable: true,
      render: (a) =>
        [a.agent_im, [a.agent_grade, a.agent_nom].filter(Boolean).join(" ")]
          .filter(Boolean)
          .join(" — ") || "—",
    },
    {
      key: "lignes",
      header: "Matériels",
      sortable: false,
      render: (a) => {
        const lignes = a.lignes ?? [];
        if (lignes.length === 0) return "—";
        return (
          <div className="flex flex-wrap gap-1">
            {lignes.map((l) => (
              <Badge key={l.id} variant="secondary" className="gap-1">
                {l.type_materiel_nom} · {l.numero_materiel}
              </Badge>
            ))}
          </div>
        );
      },
    },
    {
      key: "date_reintegration",
      header: "Date Réintégration",
      sortable: true,
      render: (a) =>
        a.heure_reintegration
          ? `${formatDate(a.date_reintegration)} ${formatHeure(a.heure_reintegration)}`.trim()
          : "—",
    },
    {
      key: "statut",
      header: "Statut",
      sortable: true,
      render: (a) =>
        a.statut === "Réintégré" ? (
          <Badge variant="secondary">Réintégré</Badge>
        ) : (
          <Badge>Assigné</Badge>
        ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[120px]",
      render: (a) => (
        <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            title="Voir le détail"
            onClick={() => navigate(`${LIST_PATH}/${a.id}`)}
          >
            <Eye className="h-3.5 w-3.5" />
          </Button>
          {canEdit && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              title="Modifier"
              onClick={() => navigate(`${LIST_PATH}/${a.id}/edit`)}
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
          )}
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              title="Supprimer"
              onClick={() => setAffDeleteTarget(a)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Matériels</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion des types de matériel et des affectations aux agents
          </p>
        </div>
      </div>

      <Tabs value={tab} onValueChange={handleTabChange}>
        <TabsList>
          <TabsTrigger value="affectations" className="gap-2">
            <Package className="h-3.5 w-3.5" />
            Affectations
          </TabsTrigger>
          <TabsTrigger value="types" className="gap-2">
            <Tags className="h-3.5 w-3.5" />
            Types de matériel
          </TabsTrigger>
        </TabsList>

        {/* ── Tab: Affectations ─────────────────────────────────── */}
        <TabsContent value="affectations" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {affectations.length} affectation(s) enregistrée(s)
            </p>
            {canCreate && (
              <Button onClick={() => navigate(`${LIST_PATH}/new`)} className="gap-2">
                <Plus className="h-4 w-4" />
                Nouvelle affectation
              </Button>
            )}
          </div>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Package className="h-4 w-4" />
                Affectations ({affectations.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={affColumns}
                data={affectations}
                keyExtractor={(a) => a.id}
                loading={affectationsLoading}
                searchable
                searchPlaceholder="Rechercher par agent, matériel, observations..."
                emptyMessage="Aucune affectation enregistrée"
                onRowClick={(a) => navigate(`${LIST_PATH}/${a.id}`)}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Tab: Types de matériel ─────────────────────────────── */}
        <TabsContent value="types" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {types.length} type(s) de matériel enregistré(s)
            </p>
            {canCreate && (
              <Button onClick={openCreateType} className="gap-2">
                <Plus className="h-4 w-4" />
                Nouveau type
              </Button>
            )}
          </div>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Tags className="h-4 w-4" />
                Types de matériel ({types.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={typeColumns}
                data={types}
                keyExtractor={(t) => t.id}
                loading={typesLoading}
                searchable
                searchPlaceholder="Rechercher un type de matériel..."
                emptyMessage="Aucun type de matériel enregistré"
              />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* ── Type create/edit dialog ─────────────────────────────── */}
      {typeEditorOpen && (
        <div
          className="fixed inset-0 z-[1000] flex items-center justify-center"
          onClick={closeTypeEditor}
        >
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: -20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            className="relative z-50 w-full max-w-md rounded-xl border border-border bg-card p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-base font-semibold mb-4">
              {editingType ? "Modifier le type de matériel" : "Nouveau type de matériel"}
            </h3>
            <form onSubmit={handleSaveType} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="type_nom">Nom *</Label>
                <Input
                  id="type_nom"
                  value={typeForm.nom}
                  onChange={(e) => setTypeForm({ ...typeForm, nom: e.target.value })}
                  placeholder="Ex : Radio, Bâton, Gilet..."
                  required
                  autoFocus
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="type_description">Description (optionnelle)</Label>
                <Input
                  id="type_description"
                  value={typeForm.description ?? ""}
                  onChange={(e) => setTypeForm({ ...typeForm, description: e.target.value })}
                  placeholder="Courte description du type de matériel"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={closeTypeEditor}
                  disabled={typeSaving}
                >
                  Annuler
                </Button>
                <Button type="submit" size="sm" disabled={typeSaving} className="gap-2">
                  {typeSaving ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Plus className="h-3.5 w-3.5" />
                  )}
                  {editingType ? "Mettre à jour" : "Créer"}
                </Button>
              </div>
            </form>
          </motion.div>
        </div>
      )}

      {/* ── Confirm dialogs ─────────────────────────────────────── */}
      <ConfirmDialog
        open={typeDeleteTarget !== null}
        title="Supprimer le type de matériel"
        message={`Êtes-vous sûr de vouloir supprimer le type « ${typeDeleteTarget?.nom} » ? Cette action est impossible si des affectations l'utilisent.`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={typeDeleting}
        onConfirm={() => typeDeleteTarget && handleDeleteType(typeDeleteTarget.id)}
        onCancel={() => setTypeDeleteTarget(null)}
      />

      <ConfirmDialog
        open={affDeleteTarget !== null}
        title="Supprimer l'affectation"
        message={`Êtes-vous sûr de vouloir supprimer l'affectation du ${affDeleteTarget ? formatDate(affDeleteTarget.date_perception) : ""} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={affDeleting}
        onConfirm={() => affDeleteTarget && handleDeleteAff(affDeleteTarget.id)}
        onCancel={() => setAffDeleteTarget(null)}
      />
    </motion.div>
  );
}
