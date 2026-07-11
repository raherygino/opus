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
  Loader2,
  Undo2,
  Paperclip,
  X,
  Download,
} from "lucide-react";
import type { Personnel, Mouvement, MouvementAttachment, Comportement } from "@/types";
import {
  getMouvementList,
  createMouvement,
  updateMouvement,
  deleteMouvement,
  getMouvementAttachments,
  createMouvementAttachment,
  deleteMouvementAttachment,
  getMouvementAttachmentDownloadUrl,
} from "@/lib/api/mouvement";
import {
  getComportementList,
  createComportement,
  deleteComportement,
} from "@/lib/api/comportement";

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
  "Repos",
  "Repos médical",
  "Absent non motivé",
];

interface MouvementForm {
  personnel_id: number;
  im: string;
  grade: string;
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
  grade: "",
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
  const [searchName, setSearchName] = useState("");
  const [autocompleteResults, setAutocompleteResults] = useState<Personnel[]>([]);
  const [showAutocomplete, setShowAutocomplete] = useState(false);
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

  // Comportement tab
  const [comportements, setComportements] = useState<Comportement[]>([]);
  const [loadingComportements, setLoadingComportements] = useState(true);
  const [comportFormOpen, setComportFormOpen] = useState(false);
  const [comportForm, setComportForm] = useState({
    personnel_id: 0,
    im: "",
    grade: "",
    service: "",
    nom: "",
    prenoms: "",
    type: "" as "" | "Positive" | "Negative",
    date_comportement: "",
    motif: "",
    decision: "",
  });
  const [comportSearchName, setComportSearchName] = useState("");
  const [comportAutocomplete, setComportAutocomplete] = useState<Personnel[]>([]);
  const [showComportAutocomplete, setShowComportAutocomplete] = useState(false);
  const [savingComport, setSavingComport] = useState(false);
  const [deleteComportTarget, setDeleteComportTarget] = useState<Comportement | null>(null);
  const [deletingComport, setDeletingComport] = useState(false);
  const [comportDetailTarget, setComportDetailTarget] = useState<Comportement | null>(null);

  useEffect(() => {
    loadPersonnel();
    loadMouvements();
    loadComportements();
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

  async function loadComportements() {
    setLoadingComportements(true);
    try {
      const data = await getComportementList();
      setComportements(data);
    } catch {
      addNotification(
        "error",
        "Erreur",
        "Impossible de charger les comportements",
      );
    } finally {
      setLoadingComportements(false);
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
    setSearchName("");
    setAutocompleteResults([]);
    setShowAutocomplete(false);
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

  function handleSearchName(value: string) {
    setSearchName(value);
    if (value.trim().length < 1) {
      setAutocompleteResults([]);
      setShowAutocomplete(false);
      return;
    }
    const query = value.toLowerCase().trim();
    const results = personnel.filter(
      (p) =>
        p.lastname.toLowerCase().includes(query) ||
        p.firstname.toLowerCase().includes(query) ||
        `${p.firstname} ${p.lastname}`.toLowerCase().includes(query) ||
        p.im.toLowerCase().includes(query),
    );
    setAutocompleteResults(results);
    setShowAutocomplete(true);
  }

  function selectPersonnel(person: Personnel) {
    setForm({
      personnel_id: person.id,
      im: person.im,
      grade: person.grade,
      service: person.affectation || "",
      nom: person.lastname,
      prenoms: person.firstname,
      type_mouvement: form.type_mouvement,
      date_depart: "",
      days: "",
      date_retour: "",
    });
    setSearchName(`${person.firstname} ${person.lastname}`);
    setShowAutocomplete(false);
    addNotification(
      "info",
      "Trouvé",
      `${person.firstname} ${person.lastname} (IM: ${person.im})`,
    );
  }

  function addPendingFile(file: File, title: string) {
    setPendingFiles((prev) => [...prev, { file, title }]);
  }

  function removePendingFile(index: number) {
    setPendingFiles((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSaveMouvement() {
    if (!form.personnel_id) {
      addNotification("error", "Erreur", "Recherchez d'abord un personnel");
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
      loadPersonnel();
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
      loadPersonnel();
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
      loadPersonnel();
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

  // --- Comportement handlers ---

  function resetComportForm() {
    setComportForm({
      personnel_id: 0,
      im: "",
      grade: "",
      service: "",
      nom: "",
      prenoms: "",
      type: "",
      date_comportement: "",
      motif: "",
      decision: "",
    });
    setComportSearchName("");
    setComportAutocomplete([]);
    setShowComportAutocomplete(false);
  }

  function handleComportSearchName(value: string) {
    setComportSearchName(value);
    if (value.trim().length < 1) {
      setComportAutocomplete([]);
      setShowComportAutocomplete(false);
      return;
    }
    const query = value.toLowerCase().trim();
    const results = personnel.filter(
      (p) =>
        p.lastname.toLowerCase().includes(query) ||
        p.firstname.toLowerCase().includes(query) ||
        `${p.firstname} ${p.lastname}`.toLowerCase().includes(query) ||
        p.im.toLowerCase().includes(query),
    );
    setComportAutocomplete(results);
    setShowComportAutocomplete(true);
  }

  function selectComportPersonnel(person: Personnel) {
    setComportForm((prev) => ({
      ...prev,
      personnel_id: person.id,
      im: person.im,
      grade: person.grade,
      service: person.affectation || "",
      nom: person.lastname,
      prenoms: person.firstname,
    }));
    setComportSearchName(`${person.firstname} ${person.lastname}`);
    setShowComportAutocomplete(false);
  }

  async function handleSaveComportement() {
    if (!comportForm.personnel_id) {
      addNotification("error", "Erreur", "Recherchez d'abord un personnel");
      return;
    }
    if (!comportForm.type) {
      addNotification("error", "Erreur", "Sélectionnez un type");
      return;
    }
    if (!comportForm.date_comportement) {
      addNotification("error", "Erreur", "La date est requise");
      return;
    }
    if (!comportForm.motif.trim()) {
      addNotification("error", "Erreur", "Le motif est requis");
      return;
    }
    setSavingComport(true);
    try {
      await createComportement({
        ...comportForm,
        type: comportForm.type as "Positive" | "Negative",
        decision: comportForm.decision || null,
      });
      addNotification("success", "Ajouté", "Comportement enregistré avec succès");
      resetComportForm();
      setComportFormOpen(false);
      loadComportements();
    } catch {
      addNotification("error", "Erreur", "Impossible d'ajouter le comportement");
    } finally {
      setSavingComport(false);
    }
  }

  async function handleDeleteComportement(id: number) {
    setDeletingComport(true);
    try {
      await deleteComportement(id);
      addNotification("success", "Supprimé", "Comportement supprimé avec succès");
      loadComportements();
    } catch {
      addNotification("error", "Erreur", "Impossible de supprimer ce comportement");
    } finally {
      setDeletingComport(false);
      setDeleteComportTarget(null);
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
    { key: "lastname", header: "Nom", sortable: true },
    { key: "firstname", header: "Prénoms", sortable: true },
    { key: "grade", header: "Grade", sortable: true },
    { key: "affectation", header: "Affectation", sortable: true },
    {
      key: "status",
      header: "Statut",
      render: (p) => (
        <span
          className={`text-xs px-2 py-0.5 rounded-full ${
            p.status === "En service"
              ? "bg-green-500/10 text-green-500"
              : "bg-amber-500/10 text-amber-500"
          }`}
        >
          {p.status}
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
    { key: "nom", header: "Nom", sortable: true },
    { key: "prenoms", header: "Prénoms", sortable: true },
    { key: "grade", header: "Grade", sortable: true },
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
                  <div className="space-y-1">
                      <Label htmlFor="search-name">Rechercher par nom ou prénoms</Label>
                      <div className="relative">
                        <Input
                          id="search-name"
                          value={searchName}
                          onChange={(e) => handleSearchName(e.target.value)}
                          placeholder="Saisir le nom ou prénoms..."
                          onFocus={() => searchName && setShowAutocomplete(true)}
                          onBlur={() => setTimeout(() => setShowAutocomplete(false), 200)}
                        />
                        {searchingPersonnel && (
                          <Loader2 className="absolute right-3 top-3 h-4 w-4 animate-spin text-muted-foreground" />
                        )}
                        {showAutocomplete && autocompleteResults.length > 0 && (
                          <div className="absolute z-50 mt-1 w-full max-h-48 overflow-y-auto rounded-md border border-border bg-popover shadow-md">
                            {autocompleteResults.map((p) => (
                              <button
                                key={p.id}
                                type="button"
                                onMouseDown={() => selectPersonnel(p)}
                                className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-accent"
                              >
                                <span className="font-medium">{p.firstname} {p.lastname}</span>
                                <span className="text-xs text-muted-foreground">— {p.im}</span>
                              </button>
                            ))}
                          </div>
                        )}
                        {showAutocomplete && autocompleteResults.length === 0 && searchName.trim() && (
                          <div className="absolute z-50 mt-1 w-full rounded-md border border-border bg-popover shadow-md px-3 py-2 text-sm text-muted-foreground">
                            Aucun personnel trouvé
                          </div>
                        )}
                      </div>
                    </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <Label>IM</Label>
                      <Input value={form.im} readOnly />
                    </div>
                    <div className="space-y-1">
                      <Label>Grade</Label>
                      <Input value={form.grade} readOnly />
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
                searchPlaceholder="Rechercher par IM, nom..."
                onRowClick={(m) => openDetailDialog(m)}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* TAB: Comportement */}
        <TabsContent value="comportement">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">Liste des comportements</CardTitle>
                {canCreate && (
                  <Button
                    size="sm"
                    className="gap-2"
                    onClick={() => {
                      resetComportForm();
                      setComportFormOpen(true);
                    }}
                  >
                    <Plus className="h-4 w-4" />
                    Nouveau comportement
                  </Button>
                )}
              </div>
            </CardHeader>
            <CardContent>
              <DataTable
                columns={[
                  { key: "im", header: "IM", sortable: true },
                  { key: "nom", header: "Last Name", sortable: true },
                  { key: "prenoms", header: "First Name", sortable: true },
                  { key: "grade", header: "Grade", sortable: true },
                  {
                    key: "type",
                    header: "Type",
                    sortable: true,
                    render: (c) => (
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          c.type === "Positive"
                            ? "bg-green-500/10 text-green-500"
                            : "bg-red-500/10 text-red-500"
                        }`}
                      >
                        {c.type}
                      </span>
                    ),
                  },
                  { key: "date_comportement", header: "Date", sortable: true },
                  {
                    key: "actions",
                    header: "Actions",
                    className: "w-[100px]",
                    render: (c) => (
                      <div
                        className="flex items-center gap-1"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          onClick={() => setComportDetailTarget(c)}
                        >
                          <Users className="h-3.5 w-3.5" />
                        </Button>
                        {canDelete && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-destructive"
                            onClick={() => setDeleteComportTarget(c)}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        )}
                      </div>
                    ),
                  },
                ] as Column<Comportement>[]}
                data={comportements}
                keyExtractor={(c) => c.id}
                loading={loadingComportements}
                searchable
                searchPlaceholder="Search by IM, name, motif..."
                onRowClick={(c) => setComportDetailTarget(c)}
              />
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
              <div className="text-sm"><span className="text-muted-foreground">Grade :</span> {detailTarget.grade || "—"}</div>
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

      {/* Comportement form dialog */}
      {comportFormOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          onClick={() => !savingComport && setComportFormOpen(false)}
        >
          <div className="fixed inset-0 bg-black/60" />
          <div
            className="relative z-50 w-full max-w-2xl max-h-[85vh] overflow-y-auto rounded-xl border border-border bg-card p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between mb-4">
              <div>
                <p className="text-base font-semibold">Nouveau comportement</p>
                <p className="text-sm text-muted-foreground">
                  Enregistrer un comportement
                </p>
              </div>
              <Button
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                onClick={() => {
                  resetComportForm();
                  setComportFormOpen(false);
                }}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-4">
              <div className="space-y-1">
                <Label htmlFor="comport-search">Rechercher par nom ou prénoms</Label>
                <div className="relative">
                  <Input
                    id="comport-search"
                    value={comportSearchName}
                    onChange={(e) => handleComportSearchName(e.target.value)}
                    placeholder="Saisir le nom ou prénoms..."
                    onFocus={() => comportSearchName && setShowComportAutocomplete(true)}
                    onBlur={() => setTimeout(() => setShowComportAutocomplete(false), 200)}
                  />
                  {showComportAutocomplete && comportAutocomplete.length > 0 && (
                    <div className="absolute z-50 mt-1 w-full max-h-48 overflow-y-auto rounded-md border border-border bg-popover shadow-md">
                      {comportAutocomplete.map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          onMouseDown={() => selectComportPersonnel(p)}
                          className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-accent"
                        >
                          <span className="font-medium">{p.firstname} {p.lastname}</span>
                          <span className="text-xs text-muted-foreground">— {p.im}</span>
                        </button>
                      ))}
                    </div>
                  )}
                  {showComportAutocomplete && comportAutocomplete.length === 0 && comportSearchName.trim() && (
                    <div className="absolute z-50 mt-1 w-full rounded-md border border-border bg-popover shadow-md px-3 py-2 text-sm text-muted-foreground">
                      Aucun personnel trouvé
                    </div>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-1">
                  <Label>IM</Label>
                  <Input value={comportForm.im} readOnly />
                </div>
                <div className="space-y-1">
                  <Label>Grade</Label>
                  <Input value={comportForm.grade} readOnly />
                </div>
                <div className="space-y-1">
                  <Label>Service</Label>
                  <Input value={comportForm.service} readOnly />
                </div>
                <div className="space-y-1">
                  <Label>Nom</Label>
                  <Input value={comportForm.nom} readOnly />
                </div>
                <div className="space-y-1">
                  <Label>Prénoms</Label>
                  <Input value={comportForm.prenoms} readOnly />
                </div>
                <div className="space-y-1">
                  <Label htmlFor="comport-type">Type</Label>
                  <select
                    id="comport-type"
                    value={comportForm.type}
                    onChange={(e) =>
                      setComportForm({ ...comportForm, type: e.target.value as "Positive" | "Negative" })
                    }
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    <option value="">Sélectionner...</option>
                    <option value="Positive">Positive</option>
                    <option value="Negative">Negative</option>
                  </select>
                </div>
                <div className="space-y-1">
                  <Label htmlFor="comport-date">Date</Label>
                  <Input
                    id="comport-date"
                    type="date"
                    value={comportForm.date_comportement}
                    onChange={(e) =>
                      setComportForm({ ...comportForm, date_comportement: e.target.value })
                    }
                  />
                </div>
              </div>

              <div className="space-y-1">
                <Label htmlFor="comport-motif">Motif</Label>
                <textarea
                  id="comport-motif"
                  value={comportForm.motif}
                  onChange={(e) =>
                    setComportForm({ ...comportForm, motif: e.target.value })
                  }
                  rows={3}
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Décrire le motif..."
                />
              </div>

              <div className="space-y-1">
                <Label htmlFor="comport-decision">Décision de la hiérarchie</Label>
                <textarea
                  id="comport-decision"
                  value={comportForm.decision}
                  onChange={(e) =>
                    setComportForm({ ...comportForm, decision: e.target.value })
                  }
                  rows={2}
                  className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  placeholder="Décision prise par la hiérarchie..."
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    resetComportForm();
                    setComportFormOpen(false);
                  }}
                >
                  Annuler
                </Button>
                <Button
                  type="button"
                  size="sm"
                  onClick={handleSaveComportement}
                  disabled={savingComport}
                  className="gap-2"
                >
                  {savingComport ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
                  Enregistrer
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Delete comportement confirm */}
      <ConfirmDialog
        open={deleteComportTarget !== null}
        title="Supprimer le comportement"
        message={`Êtes-vous sûr de vouloir supprimer ce comportement de ${deleteComportTarget?.nom} ${deleteComportTarget?.prenoms} ?`}
        confirmLabel="Supprimer"
        variant="destructive"
        loading={deletingComport}
        onConfirm={() =>
          deleteComportTarget && handleDeleteComportement(deleteComportTarget.id)
        }
        onCancel={() => setDeleteComportTarget(null)}
      />

      {/* Detail comportement dialog */}
      {comportDetailTarget && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          onClick={() => setComportDetailTarget(null)}
        >
          <div className="fixed inset-0 bg-black/60" />
          <div
            className="relative z-50 w-full max-w-lg rounded-xl border border-border bg-card p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between mb-4">
              <div>
                <p className="text-base font-semibold">Détail du comportement</p>
                <p className="text-sm text-muted-foreground">
                  {comportDetailTarget.nom} {comportDetailTarget.prenoms}
                </p>
              </div>
              <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setComportDetailTarget(null)}>
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="text-sm"><span className="text-muted-foreground">IM :</span> {comportDetailTarget.im}</div>
                <div className="text-sm"><span className="text-muted-foreground">Grade :</span> {comportDetailTarget.grade || "—"}</div>
                <div className="text-sm"><span className="text-muted-foreground">Service :</span> {comportDetailTarget.service || "—"}</div>
                <div className="text-sm">
                  <span className="text-muted-foreground">Type :</span>{" "}
                  <span className={`text-xs px-2 py-0.5 rounded-full ${comportDetailTarget.type === "Positive" ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"}`}>
                    {comportDetailTarget.type}
                  </span>
                </div>
                <div className="text-sm"><span className="text-muted-foreground">Date :</span> {comportDetailTarget.date_comportement}</div>
              </div>

              <Separator className="my-2" />

              <div>
                <p className="text-sm font-semibold mb-1">Motif</p>
                <p className="text-sm text-muted-foreground">{comportDetailTarget.motif}</p>
              </div>

              {comportDetailTarget.decision && (
                <div>
                  <p className="text-sm font-semibold mb-1">Décision de la hiérarchie</p>
                  <p className="text-sm text-muted-foreground">{comportDetailTarget.decision}</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </motion.div>
  );
}
