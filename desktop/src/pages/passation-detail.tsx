import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getPassationById,
  getPassationAttachmentDownloadUrl,
} from "@/lib/api/passation";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  Handshake,
  UserCheck,
  ShieldCheck,
  Paperclip,
  Download,
  Eye,
  FileDown,
} from "lucide-react";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import logoPn from "@/assets/img/logo-pn.png";
import logoCsp from "@/assets/img/logo-csp.png";
import logoOpus from "@/assets/img/logo-opus.png";
import type { Passation, PassationAttachment } from "@/types";
import {
  PASSATION_MODULE,
  formatDate,
  formatHeure,
} from "@/pages/passation-list";

const LIST_PATH = "/sedentaire/poste/passation";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function PassationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, PASSATION_MODULE, "can_edit");

  const [passation, setPassation] = useState<Passation | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<PassationAttachment | null>(null);

  useEffect(() => {
    loadPassation();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadPassation() {
    setLoading(true);
    try {
      const data = await getPassationById(Number(id));
      setPassation(data);
    } catch {
      addNotification("error", "Erreur", "Passation introuvable");
      navigate(LIST_PATH);
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

  if (!passation) return null;

  const descendant =
    [passation.chef_descendant_grade, passation.chef_descendant_lastname]
      .filter(Boolean)
      .join(" ") || passation.chef_descendant_username || "—";
  const montant =
    [passation.chef_montant_grade, passation.chef_montant_lastname]
      .filter(Boolean)
      .join(" ") || passation.chef_montant_username || "—";

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto max-w-3xl space-y-6"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate(LIST_PATH)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-semibold tracking-tight flex items-center gap-2">
              Passation du {formatDate(passation.date_passation)}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {formatHeure(passation.heure_passation)} — {descendant} → {montant}
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
          {canEdit && (
            <Button
              variant="outline"
              className="gap-2"
              onClick={() => navigate(`${LIST_PATH}/${passation.id}/edit`)}
            >
              <Pencil className="h-4 w-4" />
              Modifier
            </Button>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Handshake className="h-4 w-4" />
            Passation
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de la passation" value={formatDate(passation.date_passation)} />
          <DetailRow label="Heure de la passation" value={formatHeure(passation.heure_passation)} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserCheck className="h-4 w-4" />
            Chef de poste descendant
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Grade" value={passation.chef_descendant_grade} />
          <DetailRow label="Nom complet" value={passation.chef_descendant_lastname} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <ShieldCheck className="h-4 w-4" />
            Chef de poste montant
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Grade" value={passation.chef_montant_grade} />
          <DetailRow label="Nom complet" value={passation.chef_montant_lastname} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Instructions & Incidents
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <DetailRow
            label="Instructions Autorité"
            value={
              <span className="whitespace-pre-wrap">{passation.instructions_autorite}</span>
            }
          />
          <DetailRow
            label="Incidents survenus"
            value={
              <span className="whitespace-pre-wrap">{passation.incidents_survenus}</span>
            }
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes ({passation.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(passation.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
          )}
          {(passation.attachments ?? []).map((att) => (
            <div
              key={att.id}
              className="flex items-center justify-between rounded-lg border border-border p-3"
            >
              <div>
                <p className="text-sm font-medium">{att.title}</p>
                <p className="text-xs text-muted-foreground">{att.original_filename}</p>
              </div>
              <div className="flex items-center gap-1">
                {isImageFile(att.mime_type, att.original_filename) && (
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8"
                    title="Aperçu"
                    onClick={() => setViewerTarget(att)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                )}
                <a
                  href={getPassationAttachmentDownloadUrl(passation.id, att.id)}
                  download
                >
                  <Button variant="ghost" size="icon" className="h-8 w-8" title="Télécharger">
                    <Download className="h-4 w-4" />
                  </Button>
                </a>
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget
            ? getPassationAttachmentDownloadUrl(passation.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );

  async function handleExportPDF() {
    if (!passation) return;

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

    // Header text
    doc.setFontSize(18);
    doc.setFont("helvetica", "bold");
    doc.text("Fiche de Passation", pageWidth / 2, logoY + 8, { align: "center" });

    doc.setFontSize(10);
    doc.setFont("helvetica", "normal");
    doc.text(
      `Généré le ${new Date().toLocaleDateString("fr-FR")}`,
      pageWidth / 2,
      logoY + 16,
      { align: "center" },
    );

    // Separator line: green | white | red (Senegal flag colors)
    const lineHeight = 2;
    const lineY = logoY + logoSize + 8;
    const lineXStart = 14;
    const lineXEnd = pageWidth - 14;
    const lineThird = (lineXEnd - lineXStart) / 3;

    doc.setFillColor(0, 0, 0);
    doc.rect(lineXStart, lineY, lineXEnd - lineXStart, lineHeight, "F");
    doc.setFillColor(0, 150, 80);
    doc.rect(lineXStart + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");
    doc.setFillColor(255, 255, 255);
    doc.rect(lineXStart + lineThird + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");
    doc.setFillColor(220, 40, 40);
    doc.rect(lineXStart + lineThird * 2 + 0.3, lineY + 0.3, lineThird - 0.6, lineHeight - 0.6, "F");

    // Passation info
    doc.setFontSize(13);
    doc.setFont("helvetica", "bold");
    const infoTitleY = lineY + lineHeight + 14;
    doc.text("Informations de la passation", 14, infoTitleY);

    const infoLines: [string, string][] = [
      ["Date", formatDate(passation.date_passation)],
      ["Heure", formatHeure(passation.heure_passation)],
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

    // Chef de poste descendant
    doc.setFontSize(13);
    doc.setFont("helvetica", "bold");
    yPos += 4;
    doc.text("Chef de poste descendant", 14, yPos);

    const descendantLines: [string, string][] = [
      ["Grade", passation.chef_descendant_grade || "—"],
      ["Nom complet", passation.chef_descendant_lastname || "—"],
    ];

    doc.setFontSize(10);
    yPos += 8;
    for (const [label, value] of descendantLines) {
      doc.setFont("helvetica", "bold");
      doc.text(`${label} :`, 14, yPos);
      doc.setFont("helvetica", "normal");
      doc.text(value, 60, yPos);
      yPos += 7;
    }

    // Chef de poste montant
    doc.setFontSize(13);
    doc.setFont("helvetica", "bold");
    yPos += 4;
    doc.text("Chef de poste montant", 14, yPos);

    const montantLines: [string, string][] = [
      ["Grade", passation.chef_montant_grade || "—"],
      ["Nom complet", passation.chef_montant_lastname || "—"],
    ];

    doc.setFontSize(10);
    yPos += 8;
    for (const [label, value] of montantLines) {
      doc.setFont("helvetica", "bold");
      doc.text(`${label} :`, 14, yPos);
      doc.setFont("helvetica", "normal");
      doc.text(value, 60, yPos);
      yPos += 7;
    }

    // Instructions & Incidents
    doc.setFontSize(13);
    doc.setFont("helvetica", "bold");
    yPos += 4;
    doc.text("Instructions & Incidents", 14, yPos);

    doc.setFontSize(10);
    yPos += 8;
    doc.setFont("helvetica", "bold");
    doc.text("Instructions Autorité :", 14, yPos);
    yPos += 5;
    doc.setFont("helvetica", "normal");
    const instructions = passation.instructions_autorite || "—";
    const instructionsLines = doc.splitTextToSize(instructions, pageWidth - 28);
    doc.text(instructionsLines, 14, yPos);
    yPos += instructionsLines.length * 5 + 4;

    doc.setFont("helvetica", "bold");
    doc.text("Incidents survenus :", 14, yPos);
    yPos += 5;
    doc.setFont("helvetica", "normal");
    const incidents = passation.incidents_survenus || "—";
    const incidentsLines = doc.splitTextToSize(incidents, pageWidth - 28);
    doc.text(incidentsLines, 14, yPos);
    yPos += incidentsLines.length * 5 + 6;

    // Pièces jointes
    const attachments = passation.attachments ?? [];
    if (attachments.length > 0) {
      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text("Pièces jointes", 14, yPos + 4);

      autoTable(doc, {
        startY: yPos + 8,
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

    doc.save(`passation_${passation.date_passation}_${passation.id}.pdf`);
  }
}
