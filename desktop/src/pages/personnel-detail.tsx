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
import { getMouvementList } from "@/lib/api/mouvement";
import { getComportementList } from "@/lib/api/comportement";
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
  FileDown,
  History,
  Award,
} from "lucide-react";
import type { Personnel, PersonnelAttachment, Mouvement, Comportement } from "@/types";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import logoPn from "@/assets/img/logo-pn.png";
import logoCsp from "@/assets/img/logo-csp.png";
import logoOpus from "@/assets/img/logo-opus.png";

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
  const [mouvements, setMouvements] = useState<Mouvement[]>([]);
  const [comportements, setComportements] = useState<Comportement[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, [id]);

  async function loadData() {
    setLoading(true);
    try {
      const p = await getPersonnelById(Number(id));
      setPerson(p);
      const [atts, mouvs, comps] = await Promise.all([
        getPersonnelAttachments(Number(id)),
        getMouvementList({ personnel_id: String(id) }),
        getComportementList({ personnel_id: String(id) }),
      ]);
      setAttachments(atts);
      setMouvements(mouvs);
      setComportements(comps);
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
              {person.grade}{person.affectation ? ` — ${person.affectation}` : ""}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={handleExportPDF}
            className="gap-2"
          >
            <FileDown className="h-4 w-4" />
            Export PDF
          </Button>
          <Button
            onClick={() => navigate(`/personnel/${person.id}/edit`)}
            className="gap-2"
          >
            <Pencil className="h-4 w-4" />
            Modifier
          </Button>
        </div>
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
              variant={person.status === "En service" ? "default" : "secondary"}
              className="mt-1"
            >
              {person.status}
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
              <InfoRow label="Nom" value={person.lastname} />
              <InfoRow label="Prénoms" value={person.firstname} />
              <InfoRow label="Téléphone" value={person.phone} />
              <InfoRow label="Adresse" value={person.address} />
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
              <InfoRow label="Affectation" value={person.affectation} />
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

          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <History className="h-4 w-4" />
                Historique des mouvements ({mouvements.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              {mouvements.length === 0 ? (
                <p className="text-sm text-muted-foreground py-2">
                  Aucun mouvement enregistré
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-left text-xs text-muted-foreground">
                        <th className="pb-2 pr-4 font-medium">Type</th>
                        <th className="pb-2 pr-4 font-medium">Compté du</th>
                        <th className="pb-2 pr-4 font-medium">Jours</th>
                        <th className="pb-2 pr-4 font-medium">Date retour</th>
                        <th className="pb-2 font-medium">Retour</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mouvements.map((m) => (
                        <tr key={m.id} className="border-b border-border/50">
                          <td className="py-2 pr-4">
                            <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                              {m.type_mouvement}
                            </span>
                          </td>
                          <td className="py-2 pr-4 text-muted-foreground">
                            {m.date_depart || "—"}
                          </td>
                          <td className="py-2 pr-4 text-muted-foreground">
                            {m.days ?? "—"}
                          </td>
                          <td className="py-2 pr-4 text-muted-foreground">
                            {m.date_retour || "—"}
                          </td>
                          <td className="py-2">
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full ${
                                m.retour === "Oui"
                                  ? "bg-green-500/10 text-green-500"
                                  : "bg-amber-500/10 text-amber-500"
                              }`}
                            >
                              {m.retour}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Award className="h-4 w-4" />
                Historique des comportements ({comportements.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              {comportements.length === 0 ? (
                <p className="text-sm text-muted-foreground py-2">
                  Aucun comportement enregistré
                </p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b border-border text-left text-xs text-muted-foreground">
                        <th className="pb-2 pr-4 font-medium">Type</th>
                        <th className="pb-2 pr-4 font-medium">Date</th>
                        <th className="pb-2 pr-4 font-medium">Motif</th>
                        <th className="pb-2 font-medium">Décision</th>
                      </tr>
                    </thead>
                    <tbody>
                      {comportements.map((c) => (
                        <tr key={c.id} className="border-b border-border/50">
                          <td className="py-2 pr-4">
                            <span
                              className={`text-xs px-2 py-0.5 rounded-full ${
                                c.type === "Positive"
                                  ? "bg-green-500/10 text-green-500"
                                  : "bg-red-500/10 text-red-500"
                              }`}
                            >
                              {c.type}
                            </span>
                          </td>
                          <td className="py-2 pr-4 text-muted-foreground">
                            {c.date_comportement}
                          </td>
                          <td className="py-2 pr-4 text-muted-foreground max-w-[200px] truncate">
                            {c.motif}
                          </td>
                          <td className="py-2 text-muted-foreground max-w-[200px] truncate">
                            {c.decision || "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </motion.div>
  );

  async function handleExportPDF() {
    if (!person) return;

    const doc = new jsPDF();
    const pageWidth = (doc as unknown as { getPageWidth: () => number }).getPageWidth();
    const pageHeight = (doc as unknown as { getPageHeight: () => number }).getPageHeight();

    // Logos - top left (PN) and top right (CSP)
    const logoSize = 22;
    const logoY = 8;
    try {
      doc.addImage(logoPn, "PNG", 14, logoY, logoSize, logoSize);
      doc.addImage(logoCsp, "PNG", pageWidth - 14 - logoSize, logoY, logoSize, logoSize);
    } catch {
      // Images may fail to load in some environments
    }

    // Header text (same level as logos)
    doc.setFontSize(18);
    doc.setFont("helvetica", "bold");
    doc.text("Fiche Personnel", pageWidth / 2, logoY + 8, { align: "center" });

    // Date (same level as logos, below the title)
    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.text(
      `Généré le ${new Date().toLocaleDateString("fr-FR")}`,
      pageWidth / 2,
      logoY + 16,
      { align: "center" },
    );

    // Separator line: green | white | red (Senegal flag colors) with thin black border
    const lineHeight = 2;
    const lineY = logoY + logoSize + 8;
    const lineXStart = 14;
    const lineXEnd = pageWidth - 14;
    const lineThird = (lineXEnd - lineXStart) / 3;

    // Black border
    doc.setFillColor(0, 0, 0);
    doc.rect(lineXStart, lineY, lineXEnd - lineXStart, lineHeight, "F");

    // Green segment
    doc.setFillColor(0, 150, 80);
    doc.rect(lineXStart + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");
    // White segment
    doc.setFillColor(255, 255, 255);
    doc.rect(lineXStart + lineThird + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");
    // Red segment
    doc.setFillColor(220, 40, 40);
    doc.rect(lineXStart + lineThird * 2 + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");

    // Personnel info
    doc.setFontSize(13);
    doc.setFont("helvetica", "bold");
    const infoTitleY = lineY + lineHeight + 14;
    doc.text("Informations personnelles", 14, infoTitleY);

    const infoLines: [string, string][] = [
      ["IM", person.im],
      ["Nom", person.lastname],
      ["Prénoms", person.firstname],
      ["Grade", person.grade],
      ["Affectation", person.affectation || "—"],
      ["Téléphone", person.phone || "—"],
      ["Adresse", person.address || "—"],
      ["Statut", person.status],
    ];

    doc.setFontSize(10);
    let yPos = lineY + lineHeight + 22;
    for (const [label, value] of infoLines) {
      doc.setFont("helvetica", "bold");
      doc.text(`${label} :`, 14, yPos);
      doc.setFont("helvetica", "normal");
      doc.text(value, 60, yPos);
      yPos += 7;
    }

    // Photo (right of "Informations personnelles")
    const photoBoxSize = 30;
    const photoBoxX = pageWidth - 14 - photoBoxSize;
    const photoBoxY = infoTitleY - 4;

    if (person.photo) {
      try {
        const photoUrl = getPersonnelPhotoUrl(person.id);
        const response = await fetch(photoUrl);
        if (response.ok) {
          const blob = await response.blob();
          const reader = new FileReader();
          const dataUrl = await new Promise<string>((resolve, reject) => {
            reader.onload = () => resolve(reader.result as string);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
          });
          doc.addImage(dataUrl, "JPEG", photoBoxX, photoBoxY, photoBoxSize, photoBoxSize);
        } else {
          throw new Error("Photo not found");
        }
      } catch {
        // Draw placeholder if photo fails to load
        doc.setDrawColor(0, 0, 0);
        doc.setLineWidth(0.5);
        doc.rect(photoBoxX, photoBoxY, photoBoxSize, photoBoxSize);
      }
    } else {
      // No photo: draw bordered square
      doc.setDrawColor(0, 0, 0);
      doc.setLineWidth(0.5);
      doc.rect(photoBoxX, photoBoxY, photoBoxSize, photoBoxSize);
    }

    // Mouvements history
    if (mouvements.length > 0) {
      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text("Historique des mouvements", 14, yPos + 6);

      autoTable(doc, {
        startY: yPos + 10,
        theme: "striped",
        head: [["Type", "Compté du", "Jours", "Date retour", "Retour"]],
        body: mouvements.map((m) => [
          m.type_mouvement,
          m.date_depart || "—",
          m.days != null ? String(m.days) : "—",
          m.date_retour || "—",
          m.retour,
        ]),
        headStyles: { fillColor: [100, 100, 100], textColor: [255, 255, 255] },
        styles: { fontSize: 9 },
      });
    }

    // Comportements history
    if (comportements.length > 0) {
      const afterMouvements = (doc as unknown as { lastAutoTable: { finalY: number } }).lastAutoTable?.finalY ?? yPos;

      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text("Historique des comportements", 14, afterMouvements + 14);

      autoTable(doc, {
        startY: afterMouvements + 18,
        theme: "striped",
        head: [["Type", "Date", "Motif", "Décision"]],
        body: comportements.map((c) => [
          c.type,
          c.date_comportement,
          c.motif,
          c.decision || "—",
        ]),
        headStyles: { fillColor: [100, 100, 100], textColor: [255, 255, 255] },
        styles: { fontSize: 9 },
      });
    }

    // Attachments
    if (attachments.length > 0) {
      const finalY = (doc as unknown as { lastAutoTable: { finalY: number } }).lastAutoTable.finalY;

      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text("Pièces jointes", 14, finalY + 14);

      autoTable(doc, {
        startY: finalY + 18,
        theme: "striped",
        head: [["Titre", "Fichier"]],
        body: attachments.map((a) => [a.title, a.original_filename]),
        headStyles: { fillColor: [100, 100, 100], textColor: [255, 255, 255] },
        styles: { fontSize: 10 },
      });
    }

    // Footer with OPUS logo
    const opusLogoSize = 14;
    const footerTextY = pageHeight - 6;
    const footerLogoY = footerTextY - opusLogoSize - 4;
    const pageCount = doc.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      doc.setPage(i);
      try {
        doc.addImage(
          logoOpus,
          "PNG",
          pageWidth / 2 - opusLogoSize / 2,
          footerLogoY,
          opusLogoSize,
          opusLogoSize,
        );
      } catch {
        // Images may fail to load
      }
      doc.setFontSize(8);
      doc.setFont("helvetica", "normal");
      doc.setTextColor(150);
      doc.text(
        `OPUS — Page ${i}/${pageCount}`,
        pageWidth / 2,
        footerTextY,
        { align: "center" },
      );
    }

    doc.save(
      `personnel_${person.im}_${person.lastname}.pdf`,
    );
  }
}
