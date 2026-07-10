import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import { getPersonnelList, deletePersonnel } from "@/lib/api/personnel";
import { DataTable, type Column } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Plus,
  Users,
  Pencil,
  Trash2,
  Search,
  Loader2,
  Undo2,
  Paperclip,
  X,
  Download,
} from "lucide-react";
import type { Personnel, Mouvement, MouvementAttachment } from "@/types";
import {
  getMouvementList,
  createMouvement,
  updateMouvement,
  deleteMouvement,
  getPersonnelByIm,
  getMouvementAttachments,
  createMouvementAttachment,
  deleteMouvementAttachment,
  getMouvementAttachmentDownloadUrl,
} from "@/lib/api/mouvement";

interface PendingFile {
  file: File;
  title: string;
}

const MOUVEMENT_TYPES = [
  "Congé",
  "Permission",
  "Mission",
  "Mutation",
  "Promotion",
  "Suspension",
  "Retraite",
  "Démission",
  "Détachement",
];

interface MouvementForm {
  personnel_id: number;
  im: string;
  matricule: string;
  grade: string;
  fonction: string;
  service: string;
  nom: string;
  prenoms: string;
  type_mouvement: string;
  date_depart: string;
  days: string;
  date_retour: string;
}

const defaultMouvementForm: MouvementForm = {
  personnel_id: 0,
  im: "",
  matricule: "",
  grade: "",
  fonction: "",
  service: "",
  nom: "",
  prenoms: "",
  type_mouvement: "",
  date_depart: "",
  days: "",
  date_retour: "",
};

export function PersonnelTabs() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canCreate = hasPermission(user, "personnel", "can_create");
  const canDelete = hasPermission(user, "personnel", "can_delete");

  // Liste tab
  const [personnel, setPersonnel] = useState<Personnel[]>([]);
  const [loadingPersonnel, setLoadingPersonnel] = useState(true);
  const [deleteTarget, setDeleteTarget] = useState<Personnel | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Mouvement tab
  const [mouvements, setMouvements] = useState<Mouvement[]>([]);
  const [loadingMouvements, setLoadingMouvements] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(defaultMouvementForm);
  const [searchIm, setSearchIm] = useState("");
  const [searchingPersonnel, setSearchingPersonnel] = useState(false);
  const [saving, setSaving] = useState(false);
  const [pendingFiles, setPendingFiles] = useState<PendingFile[]>([]);
  const [deleteMouvTarget, setDeleteMouvTarget] = useState<Mouvement | null>(
    null,
  );
  const [deletingMouv, setDeletingMouv] = useState(false);
  const [retourTarget, setRetourTarget] = useState<Mouvement | null>(null);
  const [retourDate, setRetourDate] = useState("");
  const [savingRetour, setSavingRetour] = useState(false);
  const [detailTarget, setDetailTarget] = useState<Mouvement | null>(null);
  const [mouvAttachments, setMouvAttachments] = useState<MouvementAttachment[]>([]);
  const [loadingAttachments, setLoadingAttachments] = useState(false);

  useEffect(() => {
    loadPersonnel();
    loadMouvements();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function loadPersonnel() {
    setLoadingPersonnel(true);
    try {
      const data = await getPersonnelList();
      setPersonnel(data);
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible de charger la liste du personnel",
      );
    } finally {
      setLoadingPersonnel(false);
    }
  }

  async function loadMouvements() {
    setLoadingMouvements(true);
    try {
      const data = await getMouvementList();
      setMouvements(data);
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible de charger les mouvements",
      );
    } finally {
      setLoadingMouvements(false);
    }
  }

  async function handleDeletePersonnel(id: number) {
    setDeleting(true);
    try {
      await deletePersonnel(id);
      addNotification("success", "Supprimé", "Personnel supprimé avec succès");
      loadPersonnel();
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible de supprimer ce personnel",
      );
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  }

  function resetForm() {
    setForm(defaultMouvementForm);
    setSearchIm("");
    setSearchingPersonnel(false);
    setPendingFiles([]);
  }

  function openNewMouvement() {
    resetForm();
    setFormOpen(true);
  }

  function computeDateRetour(dateDepart: string, days: string): string {
    if (!dateDepart || !days) return "";
    const d = new Date(dateDepart);
    d.setDate(d.getDate() + parseInt(days, 10));
    return d.toISOString().split("T")[0];
  }

  function handleDateDepartChange(value: string) {
    setForm((prev) => {
      const updated = { ...prev, date_depart: value };
      updated.date_retour = computeDateRetour(value, prev.days);
      return updated;
    });
  }

  function handleDaysChange(value: string) {
    setForm((prev) => {
      const updated = { ...prev, days: value };
      updated.date_retour = computeDateRetour(prev.date_depart, value);
      return updated;
    });
  }

  async function handleSearchIm() {
    if (!searchIm.trim()) return;
    setSearchingPersonnel(true);
    try {
      const person = await getPersonnelByIm(searchIm.trim());
      if (person) {
        setForm({
          personnel_id: person.id,
          im: person.im,
          matricule: person.matricule || "",
          grade: person.grade,
          fonction: person.fonction,
          service: person.service || "",
          nom: person.lastname,
          prenoms: person.firstname,
          type_mouvement: form.type_mouvement,
          date_depart: "",
          days: "",
          date_retour: "",
        });
        addNotification(
          "info",
          "Trouvé",
          `${person.firstname} ${person.lastname}`,
        );
      } else {
        addNotification("warning", "Non trouvé", "Aucun personnel avec cet IM");
      }
    } catch {
      addNotification("error", "Erreur", "Erreur lors de la recherche");
    } finally {
      setSearchingPersonnel(false);
    }
  }

  function addPendingFile(file: File, title: string) {
    setPendingFiles((prev) => [...prev, { file, title }]);
  }

  function removePendingFile(index: number) {
    setPendingFiles((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSaveMouvement() {
    if (!form.personnel_id) {
      addNotification("error", "Erreur", "Recherchez d'abord un IM");
      return;
    }
    if (!form.type_mouvement) {
      addNotification("error", "Erreur", "Sélectionnez un type de mouvement");
      return;
    }
    setSaving(true);
    try {
      const mouvement = await createMouvement({
        ...form,
        days: form.days ? parseInt(form.days, 10) : null,
        retour: "Non",
      });

      // Upload pending attachments
      for (const pf of pendingFiles) {
        try {
          await createMouvementAttachment(mouvement.id, pf.title, pf.file);
        } catch {
          addNotification("warning", "Attention", `Impossible d'ajouter la pièce jointe "${pf.title}"`);
        }
      }

      addNotification("success", "Ajouté", "Mouvement ajouté avec succès");
      resetForm();
      setFormOpen(false);
      loadMouvements();
    } catch {
      addNotification("error", "Erreur", "Impossible d'ajouter le mouvement");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteMouvement(id: number) {
    setDeletingMouv(true);
    try {
      await deleteMouvement(id);
      addNotification("success", "Supprimé", "Mouvement supprimé avec succès");
      loadMouvements();
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible de supprimer ce mouvement",
      );
    } finally {
      setDeletingMouv(false);
      setDeleteMouvTarget(null);
    }
  }

  function openRetourDialog(m: Mouvement) {
    setRetourTarget(m);
    setRetourDate(m.date_retour || "");
  }

  async function handleConfirmRetour() {
    if (!retourTarget) return;
    setSavingRetour(true);
    try {
      await updateMouvement(retourTarget.id, {
        date_retour: retourDate || null,
        retour: "Oui",
      });
      addNotification("success", "Retour", "Retour enregistré avec succès");
      setRetourTarget(null);
      loadMouvements();
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible d'enregistrer le retour",
      );
    } finally {
      setSavingRetour(false);
    }
  }

  async function loadMouvAttachments(mouvementId: number) {
    setLoadingAttachments(true);
    try {
      const data = await getMouvementAttachments(mouvementId);
      setMouvAttachments(data);
    } catch {
      addNotification("error", "Erreur", "Impossible de charger les pièces jointes");
    } finally {
      setLoadingAttachments(false);
    }
  }

  function openDetailDialog(m: Mouvement) {
    setDetailTarget(m);
    setMouvAttachments([]);
    loadMouvAttachments(m.id);
  }

  async function handleDeleteMouvAttachment(attachId: number) {
    if (!detailTarget) return;
    try {
      await deleteMouvementAttachment(detailTarget.id, attachId);
      addNotification("success", "Supprimé", "Pièce jointe supprimée");
      loadMouvAttachments(detailTarget.id);
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer la pièce jointe");
    }
  }

  function formatFileSize(bytes: number | null): string {
    if (!bytes) return "";
    if (bytes < 1024) return bytes + " o";
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + " Ko";
    return (bytes / 1048576).toFixed(1) + " Mo";
  }

  const personnelColumns: Column<Personnel>[] = [
    { key: "im", header: "IM", sortable: true },
    { key: "matricule", header: "Matricule", sortable: true },
    { key: "lastname", header: "Nom", sortable: true },
    { key: "firstname", header: "Prénom", sortable: true },
    { key: "grade", header: "Grade", sortable: true },
    { key: "fonction", header: "Fonction", sortable: true },
    {
      key: "status",
      header: "Statut",
      render: (p) => (
        <span
          className={`text-xs px-2 py-0.5 rounded-full ${
            p.status === "active"
              ? "bg-green-500/10 text-green-500"
              : "bg-muted text-muted-foreground"
          }`}
        >
          {p.status === "active" ? "Actif" : "Inactif"}
        </span>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[100px]",
      render: (p) => (
        <div
          className="flex items-center gap-1"
          onClick={(e) => e.stopPropagation()}
        >
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7"
            onClick={() => navigate(`/personnel/${p.id}/edit`)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          {canDelete && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive"
              onClick={() => setDeleteTarget(p)}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  const mouvementColumns: Column<Mouvement>[] = [
    { key: "im", header: "IM", sortable: true },
    { key: "matricule", header: "Matricule", sortable: true },
    { key: "nom", header: "Nom", sortable: true },
    { key: "prenoms", header: "Prénoms", sortable: true },
    { key: "grade", header: "Grade", sortable: true },
    { key: "fonction", header: "Fonction", sortable: true },
    { key: "service", header: "Service", sortable: true },
    {
      key: "type_mouvement",
      header: "Type",
      sortable: true,
      render: (m) => (
        <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary">
          {m.type_mouvement}
        </span>
      ),
    },
    { key: "date_depart", header: "Compté du", sortable: true },
    { key: "days", header: "Jours", sortable: true },
    { key: "date_retour", header: "Date retour", sortable: true },
    {
      key: "retour",
      header: "Retour",
      render: (m) => (
        <span className={`text-xs px-2 py-0.5 rounded-full ${
          m.retour === "Oui"
            ? "bg-green-500/10 text-green-500"
            : "bg-amber-500/10 text-amber-500"
        }`}>
          {m.retour}
        </span>
      ),
    },
    {
      key: "actions",
      header: "Actions",
      className: "w-[130px]",
      render: (m) => (
        <div
          className="flex items-center gap-1"
          onClick={(e) => e.stopPropagation()}
        >
          {m.retour === "Non" && (
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-green-500"
              onClick={() => openRetourDialog(m)}
              title="Enregistrer le retour"
            >
              <Undo2 className="h-3.5 w-3.5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 text-destructive"
            onClick={() => setDeleteMouvTarget(m)}
          >
            <Trash2 className="h-3.5 w-3.5" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Personnel</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Gestion des effectifs et mouvements
          </p>
        </div>
      </div>

      <Tabs defaultValue="liste">
        <TabsList>
          <TabsTrigger value="liste">Liste</TabsTrigger>
          <TabsTrigger value="mouvement">Mouvement</TabsTrigger>
          <TabsTrigger value="comportement">Comportement</TabsTrigger>
        </TabsList>

        {/* TAB: Liste */}
        <TabsContent value="liste" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {personnel.length} agent(s)
            </p>
            {canCreate && (
              <Button
                onClick={() => navigate("/personnel/new")}
                className="gap-2"
              >
                <Plus className="h-4 w-4" />
                Nouveau personnel
              </Button>
            )}
          </div>
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Users className="h-4 w-4" />
                Liste du personnel
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={personnelColumns}
                data={personnel}
                keyExtractor={(p) => p.id}
                loading={loadingPersonnel}
                searchable
                searchPlaceholder="Rechercher par nom, matricule..."
                onRowClick={(p) => navigate(`/personnel/${p.id}`)}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB: Mouvement */}
        <TabsContent value="mouvement" className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {mouvements.length} mouvement(s)
            </p>
            <Button onClick={openNewMouvement} className="gap-2">
              <Plus className="h-4 w-4" />
              Nouveau mouvement
            </Button>
          </div>

          {/* Add mouvement dialog */}
          {formOpen && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center"
              onClick={() => {
                resetForm();
                setFormOpen(false);
              }}
            >
              <div className="fixed inset-0 bg-black/60" />
              <div
                className="relative z-50 w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-xl border border-border bg-card p-6 shadow-lg"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="flex items-center justify-between mb-4">
                  <p className="text-base font-semibold">Ajouter un mouvement</p>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => {
                      resetForm();
                      setFormOpen(false);
                    }}
                  >
                    <X className="h-4 w-4" />
                  </Button>
                </div>

                <div className="space-y-4">
                  <div className="flex items-end gap-2">
                    <div className="flex-1 space-y-1">
                      <Label htmlFor="search-im">Rechercher par IM</Label>
                      <div className="flex gap-2">
                        <Input
                          id="search-im"
                          value={searchIm}
                          onChange={(e) => setSearchIm(e.target.value)}
                          placeholder="Saisir l'IM..."
                          onKeyDown={(e) => {
                            if (e.key === "Enter") handleSearchIm();
                          }}
                        />
                        <Button
                          variant="secondary"
                          onClick={handleSearchIm}
                          disabled={searchingPersonnel || !searchIm.trim()}
                        >
                          {searchingPersonnel ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Search className="h-4 w-4" />
                          )}
                        </Button>
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <Label>IM</Label>
                      <Input value={form.im} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Matricule</Label>
                      <Input value={form.matricule} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Grade</Label>
                      <Input value={form.grade} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Fonction</Label>
                      <Input value={form.fonction} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Service</Label>
                      <Input value={form.service} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Nom</Label>
                      <Input value={form.nom} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Prénoms</Label>
                      <Input value={form.prenoms} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor="type_mouvement">Type (Mouvement)</Label>
                      <select
                        id="type_mouvement"
                        value={form.type_mouvement}
                        onChange={(e) =>
                          setForm({ ...form, type_mouvement: e.target.value })
                        }
                        className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      >
                        <option value="">Sélectionner...</option>
                        {MOUVEMENT_TYPES.map((t) => (
                          <option key={t} value={t}>
                            {t}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor="date_depart">Compté du</Label>
                      <Input
                        id="date_depart"
                        type="date"
                        value={form.date_depart}
                        onChange={(e) => handleDateDepartChange(e.target.value)}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor="days">Nombre de jours</Label>
                      <Input
                        id="days"
                        type="number"
                        min="1"
                        value={form.days}
                        onChange={(e) => handleDaysChange(e.target.value)}
                        placeholder="0"
                      />
                    </div>
                    <div className="space-y-1">
                      <Label htmlFor="date_retour">Date retour</Label>
                      <Input
                        id="date_retour"
                        type="date"
                        value={form.date_retour}
                        onChange={(e) =>
                          setForm({ ...form, date_retour: e.target.value })
                        }
                      />
                    </div>
                  </div>

                  <Separator className="my-2" />

                  <div>
                    <p className="text-sm font-semibold mb-2 flex items-center gap-2">
                      <Paperclip className="h-4 w-4" />
                      Pièces jointes
                    </p>
                    <div className="flex items-end gap-2 mb-2">
                      <div className="flex-1 space-y-1">
                        <Label htmlFor="attach-title">Titre</Label>
                        <Input
                          id="attach-title"
                          placeholder="Titre du document..."
                        />
                      </div>
                      <div className="space-y-1">
                        <Label>Fichier</Label>
                        <Input
                          id="attach-file-form"
                          type="file"
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (!file) return;
                            const titleInput = document.getElementById("attach-title") as HTMLInputElement;
                            const title = titleInput.value.trim() || file.name;
                            addPendingFile(file, title);
                            titleInput.value = "";
                            e.target.value = "";
                          }}
                        />
                      </div>
                    </div>
                    {pendingFiles.length > 0 && (
                      <div className="space-y-1 mb-2">
                        {pendingFiles.map((pf, i) => (
                          <div key={i} className="flex items-center justify-between rounded-md border border-border px-3 py-1.5 text-sm">
                            <div className="flex-1 min-w-0">
                              <span className="truncate block">{pf.title}</span>
                              <span className="text-xs text-muted-foreground">{pf.file.name}</span>
                            </div>
                            <Button variant="ghost" size="icon" className="h-6 w-6 shrink-0" onClick={() => removePendingFile(i)}>
                              <X className="h-3 w-3" />
                            </Button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="flex justify-end gap-2">
                    <Button
                      variant="ghost"
                      onClick={() => {
                        resetForm();
                        setFormOpen(false);
                      }}
                    >
                      Annuler
                    </Button>
                    <Button
                      onClick={handleSaveMouvement}
                      disabled={saving || !form.personnel_id || !form.type_mouvement}
                    >
                      {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                      Enregistrer
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          )}

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Users className="h-4 w-4" />
                Liste des mouvements
              </CardTitle>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={mouvementColumns}
                data={mouvements}
                keyExtractor={(m) => m.id}
                loading={loadingMouvements}
                searchable
                searchPlaceholder="Rechercher par IM, matricule, nom..."
                onRowClick={(m) => openDetailDialog(m)}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB: Comportement */}
        <TabsContent value="comportement">
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              <p>Module Comportement - à venir</p>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Delete personnel confirm */}
      <ConfirmDialog
        open={deleteTarget !== null}
        title="Supprimer le personnel"
        message={`Êtes-vous sûr de vouloir supprimer ${deleteTarget?.firstname} ${deleteTarget?.lastname} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deleting}
        onConfirm={() => deleteTarget && handleDeletePersonnel(deleteTarget.id)}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* Delete mouvement confirm */}
      <ConfirmDialog
        open={deleteMouvTarget !== null}
        title="Supprimer le mouvement"
        message={`Êtes-vous sûr de vouloir supprimer ce mouvement de ${deleteMouvTarget?.nom} ${deleteMouvTarget?.prenoms} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deletingMouv}
        onConfirm={() =>
          deleteMouvTarget && handleDeleteMouvement(deleteMouvTarget.id)
        }
        onCancel={() => setDeleteMouvTarget(null)}
      />

      {/* Detail mouvement dialog */}
      {detailTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          onClick={() => setDetailTarget(null)}
        >
          <div className="fixed inset-0 bg-black/60" />
          <div
            className="relative z-50 w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-xl border border-border bg-card p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between mb-4">
              <div>
                <p className="text-base font-semibold">Détail du mouvement</p>
                <p className="text-sm text-muted-foreground">
                  {detailTarget.nom} {detailTarget.prenoms}
                </p>
              </div>
              <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setDetailTarget(null)}>
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="text-sm"><span className="text-muted-foreground">IM :</span> {detailTarget.im}</div>
              <div className="text-sm"><span className="text-muted-foreground">Matricule :</span> {detailTarget.matricule || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Grade :</span> {detailTarget.grade || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Fonction :</span> {detailTarget.fonction || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Service :</span> {detailTarget.service || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Type :</span> {detailTarget.type_mouvement}</div>
              <div className="text-sm"><span className="text-muted-foreground">Compté du :</span> {detailTarget.date_depart || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Jours :</span> {detailTarget.days ?? "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Date retour :</span> {detailTarget.date_retour || "—"}</div>
              <div className="text-sm"><span className="text-muted-foreground">Retour :</span> {detailTarget.retour}</div>
            </div>

            <Separator className="my-3" />

            <p className="text-sm font-semibold mb-2">Pièces jointes</p>

            {loadingAttachments ? (
              <div className="flex items-center justify-center py-4">
                <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
              </div>
            ) : mouvAttachments.length === 0 ? (
              <p className="text-sm text-muted-foreground py-2">Aucune pièce jointe</p>
            ) : (
              <div className="space-y-2 mb-4">
                {mouvAttachments.map((a) => (
                  <div key={a.id} className="flex items-center justify-between rounded-md border border-border px-3 py-2">
                    <div className="flex-1 min-w-0">
                      <p className="text-sm truncate">{a.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {a.original_filename} ({formatFileSize(a.file_size)})
                      </p>
                    </div>
                    <div className="flex items-center gap-1 shrink-0">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => window.open(getMouvementAttachmentDownloadUrl(detailTarget.id, a.id), "_blank")}
                        title="Télécharger"
                      >
                        <Download className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive"
                        onClick={() => handleDeleteMouvAttachment(a.id)}
                        title="Supprimer"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}


          </div>
        </div>
      )}

      {/* Retour dialog */}
      {retourTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          onClick={() => !savingRetour && setRetourTarget(null)}
        >
          <div className="fixed inset-0 bg-black/60" />
          <div
            className="relative z-50 w-full max-w-sm rounded-xl border border-border bg-card p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="space-y-1">
              <p className="text-sm font-semibold">Enregistrer le retour</p>
              <p className="text-sm text-muted-foreground">
                {retourTarget.nom} {retourTarget.prenoms} — {retourTarget.type_mouvement}
              </p>
            </div>
            <div className="mt-4 space-y-1">
              <Label htmlFor="retour-date">Date de retour</Label>
              <Input
                id="retour-date"
                type="date"
                value={retourDate}
                onChange={(e) => setRetourDate(e.target.value)}
              />
            </div>
            <div className="mt-6 flex items-center justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => setRetourTarget(null)}
                disabled={savingRetour}
              >
                Annuler
              </Button>
              <Button
                type="button"
                size="sm"
                onClick={handleConfirmRetour}
                disabled={savingRetour}
                className="gap-2"
              >
                {savingRetour ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
                Confirmer le retour
              </Button>
            </div>
          </div>
        </div>
      )}
    </motion.div>
  );
}
