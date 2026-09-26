import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useNotificationStore } from "@/stores/notification-store";
import {
  getRegistreDeferrementById,
  createRegistreDeferrement,
  updateRegistreDeferrement,
  getRegistreDeferrementAttachments,
  createRegistreDeferrementAttachment,
  updateRegistreDeferrementAttachmentTitle,
  deleteRegistreDeferrementAttachment,
  getRegistreDeferrementAttachmentDownloadUrl,
  peekRegistreDeferrementNumber,
  validateRegistreDeferrementForm,
  isRegistreDeferrementImageAttachment,
} from "@/lib/api/registre-deferrement";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  Save,
  Loader2,
  Paperclip,
  Trash2,
  Download,
  Plus,
  Eye,
} from "lucide-react";
import type { RegistreDeferrement, RegistreDeferrementInput } from "@/types";

interface AttachmentItem {
  id?: number;
  title: string;
  file?: File;
  existingFile?: string;
  _delete?: boolean;
}

function nowLocalDateTime(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function RegistreDeferrementForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { addNotification } = useNotificationStore();

  const [form, setForm] = useState<RegistreDeferrementInput>({
    date_heure_deferrement: nowLocalDateTime(),
    personne_nom: "",
    date_lieu_naissance: "",
    infraction: "",
    numero_dossier: "",
    autorite: "",
    destination: "",
    escorte: "",
    suite_donnee: "",
    observations: "",
  });
  const [numero, setNumero] = useState("");
  const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
  const [viewerTarget, setViewerTarget] = useState<{ id: number; title: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (isEdit && id) {
      loadEntry(Number(id));
    } else {
      loadSuggestedNumber();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadSuggestedNumber() {
    try {
      const num = await peekRegistreDeferrementNumber();
      setNumero(num);
    } catch {
      // Non-blocking
    }
  }

  async function loadEntry(entryId: number) {
    setLoading(true);
    try {
      const e: RegistreDeferrement = await getRegistreDeferrementById(entryId);
      setForm({
        date_heure_deferrement: e.date_heure_deferrement?.slice(0, 16) ?? "",
        personne_nom: e.personne_nom,
        date_lieu_naissance: e.date_lieu_naissance ?? "",
        infraction: e.infraction ?? "",
        numero_dossier: e.numero_dossier ?? "",
        autorite: e.autorite ?? "",
        destination: e.destination ?? "",
        escorte: e.escorte ?? "",
        suite_donnee: e.suite_donnee ?? "",
        observations: e.observations ?? "",
      });
      setNumero(e.numero);
      try {
        const atts = await getRegistreDeferrementAttachments(entryId);
        setAttachments(
          atts.map((a) => ({
            id: a.id,
            title: a.title,
            existingFile: a.original_filename,
          })),
        );
      } catch {
        // Non-blocking
      }
    } catch {
      addNotification("error", "Erreur", "Impossible de charger l'entrée");
      navigate("/pj/registre-deferrement");
    } finally {
      setLoading(false);
    }
  }

  function update<K extends keyof RegistreDeferrementInput>(key: K, value: RegistreDeferrementInput[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function addAttachment() {
    setAttachments([...attachments, { title: "" }]);
  }

  function updateAttachmentTitle(index: number, title: string) {
    setAttachments(attachments.map((a, i) => (i === index ? { ...a, title } : a)));
  }

  function setAttachmentFile(index: number, file: File) {
    setAttachments(attachments.map((a, i) => (i === index ? { ...a, file } : a)));
  }

  function removeAttachment(index: number) {
    setAttachments(
      attachments.map((a, i) => {
        if (i !== index) return a;
        if (a.id) return { ...a, _delete: true };
        return a;
      }),
    );
  }

  async function handleSave() {
    const validationErrors = validateRegistreDeferrementForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      addNotification("error", "Erreur", Object.values(validationErrors)[0]);
      return;
    }
    setErrors({});
    setSaving(true);
    try {
      const payload: RegistreDeferrementInput = {
        ...form,
        numero: numero.trim() || undefined,
        personne_nom: form.personne_nom.trim(),
        date_lieu_naissance: form.date_lieu_naissance?.trim() || null,
        infraction: form.infraction?.trim() || null,
        numero_dossier: form.numero_dossier?.trim() || null,
        autorite: form.autorite?.trim() || null,
        destination: form.destination?.trim() || null,
        escorte: form.escorte?.trim() || null,
        suite_donnee: form.suite_donnee?.trim() || null,
        observations: form.observations?.trim() || null,
      };
      const saved: RegistreDeferrement = isEdit && id
        ? await updateRegistreDeferrement(Number(id), payload)
        : await createRegistreDeferrement(payload);
      await handleAttachments(saved.id);
      addNotification("success", "Enregistré", "Entrée au registre de déferrement enregistrée avec succès");
      navigate(`/pj/registre-deferrement/${saved.id}`);
    } catch (err: unknown) {
      const message = (err as { message?: string })?.message ?? "Impossible d'enregistrer l'entrée";
      addNotification("error", "Erreur", message);
    } finally {
      setSaving(false);
    }
  }

  async function handleAttachments(savedId: number) {
    for (const a of attachments) {
      if (a._delete && a.id) {
        await deleteRegistreDeferrementAttachment(savedId, a.id);
      } else if (!a._delete && a.id && a.file) {
        await deleteRegistreDeferrementAttachment(savedId, a.id);
        if (a.title.trim()) {
          await createRegistreDeferrementAttachment(savedId, a.title.trim(), a.file);
        }
      } else if (!a._delete && a.id && a.title.trim()) {
        await updateRegistreDeferrementAttachmentTitle(savedId, a.id, a.title);
      } else if (!a._delete && a.file && a.title.trim()) {
        await createRegistreDeferrementAttachment(savedId, a.title.trim(), a.file);
      }
    }
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
      <div className="sticky top-0 z-10 -mx-6 px-6 py-4 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b border-border">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" onClick={() => navigate("/pj/registre-deferrement")}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">
                {isEdit ? "Modifier l'entrée" : "Nouvelle entrée au registre de déferrement"}
              </h1>
              <p className="text-sm text-muted-foreground mt-1">Police Judiciaire</p>
            </div>
          </div>
          <Button onClick={handleSave} disabled={saving || loading} className="gap-2">
            {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            {isEdit ? "Mettre à jour" : "Enregistrer"}
          </Button>
        </div>
      </div>

      {loading && (
        <div className="flex justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {!loading && (
        <div className="space-y-6 pb-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Numéro & date</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="numero">
                  Numéro d'enregistrement {!isEdit && "(suggéré — modifiable)"}
                </Label>
                <Input
                  id="numero"
                  value={numero}
                  onChange={(e) => setNumero(e.target.value)}
                  readOnly={isEdit}
                  placeholder="N°…/MSP/SG/DGPN/DGA/DRSP.1/DEF/CSP/TRIMO/…"
                />
                {!isEdit && (
                  <button
                    type="button"
                    className="text-xs text-primary hover:underline"
                    onClick={loadSuggestedNumber}
                  >
                    Régénérer le numéro suggéré
                  </button>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="date_heure_deferrement">Date et heure du déferrement *</Label>
                <Input
                  id="date_heure_deferrement"
                  type="datetime-local"
                  value={form.date_heure_deferrement ?? ""}
                  onChange={(e) => update("date_heure_deferrement", e.target.value)}
                />
                {errors.date_heure_deferrement && (
                  <p className="text-sm text-destructive">{errors.date_heure_deferrement}</p>
                )}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Personne déférée</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="personne_nom">Nom et prénom *</Label>
                <Input
                  id="personne_nom"
                  value={form.personne_nom}
                  onChange={(e) => update("personne_nom", e.target.value)}
                  required
                />
                {errors.personne_nom && <p className="text-sm text-destructive">{errors.personne_nom}</p>}
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="date_lieu_naissance">Date et lieu de naissance</Label>
                  <Input
                    id="date_lieu_naissance"
                    value={form.date_lieu_naissance ?? ""}
                    onChange={(e) => update("date_lieu_naissance", e.target.value)}
                    placeholder="Né(e) le … à …"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="infraction">Infraction reprochée</Label>
                  <Input
                    id="infraction"
                    value={form.infraction ?? ""}
                    onChange={(e) => update("infraction", e.target.value)}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Autorité & escorte</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="autorite">Autorité judiciaire</Label>
                  <Input
                    id="autorite"
                    value={form.autorite ?? ""}
                    onChange={(e) => update("autorite", e.target.value)}
                    placeholder="Magistrat / parquet saisi"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="destination">Destination</Label>
                  <Input
                    id="destination"
                    value={form.destination ?? ""}
                    onChange={(e) => update("destination", e.target.value)}
                    placeholder="Juridiction ou lieu de destination"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="escorte">Éléments d'escorte</Label>
                <textarea
                  id="escorte"
                  className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.escorte ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("escorte", e.target.value)}
                  placeholder="Un policier par ligne (ex: Cne RAKOTO Jean — OPJ)"
                />
                <p className="text-xs text-muted-foreground">
                  Saisissez un élément par ligne pour permettre l'ajout de plusieurs policiers.
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Suite & dossier rattaché</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="suite_donnee">Suite donnée</Label>
                <textarea
                  id="suite_donnee"
                  className="w-full min-h-[80px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.suite_donnee ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("suite_donnee", e.target.value)}
                  placeholder="Décision du magistrat (mandat de dépôt, convocation, remise en liberté…)"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="numero_dossier">N° du dossier rattaché</Label>
                <Input
                  id="numero_dossier"
                  value={form.numero_dossier ?? ""}
                  onChange={(e) => update("numero_dossier", e.target.value)}
                  placeholder="Référence au dossier associé"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="observations">Observations</Label>
                <textarea
                  id="observations"
                  className="w-full min-h-[120px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={form.observations ?? ""}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => update("observations", e.target.value)}
                />
              </div>
            </CardContent>
          </Card>

          {/* Pièces jointes */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-base">Pièces jointes</CardTitle>
                <Button type="button" variant="outline" size="sm" onClick={addAttachment} className="gap-2">
                  <Plus className="h-4 w-4" />
                  Ajouter
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {attachments.filter((a) => !a._delete).length === 0 && (
                <p className="text-sm text-muted-foreground">Aucune pièce jointe.</p>
              )}
              {attachments.map((a, i) =>
                a._delete ? null : (
                  <div key={i} className="flex items-start gap-3 rounded-lg border p-3">
                    <div className="flex-1 space-y-2">
                      <Input
                        placeholder="Titre"
                        value={a.title}
                        onChange={(e) => updateAttachmentTitle(i, e.target.value)}
                      />
                      <div className="flex items-center gap-2">
                        <label className="flex-1 cursor-pointer">
                          <span className="flex items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm text-muted-foreground hover:bg-accent">
                            <Paperclip className="h-4 w-4" />
                            {a.file?.name ?? a.existingFile ?? "Choisir un fichier"}
                          </span>
                          <input
                            type="file"
                            className="hidden"
                            onChange={(e) => {
                              const f = e.target.files?.[0];
                              if (f) setAttachmentFile(i, f);
                            }}
                          />
                        </label>
                        {a.existingFile && a.id && (
                          <>
                            {isRegistreDeferrementImageAttachment(null, a.existingFile) && (
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon"
                                onClick={() => setViewerTarget({ id: a.id!, title: a.title })}
                              >
                                <Eye className="h-4 w-4" />
                              </Button>
                            )}
                            <a
                              href={getRegistreDeferrementAttachmentDownloadUrl(Number(id), a.id)}
                              target="_blank"
                              rel="noreferrer"
                            >
                              <Button type="button" variant="ghost" size="icon">
                                <Download className="h-4 w-4" />
                              </Button>
                            </a>
                          </>
                        )}
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="text-destructive"
                      onClick={() => removeAttachment(i)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                ),
              )}
            </CardContent>
          </Card>
        </div>
      )}

      <ImageViewerDialog
        src={viewerTarget ? getRegistreDeferrementAttachmentDownloadUrl(Number(id), viewerTarget.id) : ""}
        title={viewerTarget?.title}
        open={viewerTarget !== null}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );
}
