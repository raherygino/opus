import { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuthStore } from "@/stores/auth-store";
import { useNotificationStore } from "@/stores/notification-store";
import { hasPermission } from "@/lib/permissions";
import {
  getArmementById,
  getArmementAttachmentDownloadUrl,
  reintegrateArmement,
} from "@/lib/api/armement";
import { isImageFile } from "@/lib/utils/attachment";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ImageViewerDialog } from "@/components/ui/image-viewer-dialog";
import { ReintegrationDialog } from "@/pages/armement-reintegration-dialog";
import {
  ArrowLeft,
  Loader2,
  Pencil,
  ShieldEllipsis,
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
import type { Armement, ArmementAttachment } from "@/types";
import type { ReintegrationPayload } from "@/lib/api/armement";
import { formatDate, formatHeure } from "@/pages/passation-list";
import {
  ARMEMENT_MODULE,
  agentPreneurDisplay,
  armeDisplay,
  isReintegree,
} from "@/pages/armement-list";

const LIST_PATH = "/sedentaire/poste/armement";

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm">{value || "—"}</p>
    </div>
  );
}

export function ArmementDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const { addNotification } = useNotificationStore();
  const canEdit = hasPermission(user, ARMEMENT_MODULE, "can_edit");

  const [armement, setArmement] = useState<Armement | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewerTarget, setViewerTarget] = useState<ArmementAttachment | null>(null);
  const [reintOpen, setReintOpen] = useState(false);
  const [reintegrating, setReintegrating] = useState(false);

  useEffect(() => {
    loadArmement();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  async function loadArmement() {
    setLoading(true);
    try {
      const data = await getArmementById(Number(id));
      setArmement(data);
    } catch {
      addNotification("error", "Erreur", "Armement introuvable");
      navigate(LIST_PATH);
    } finally {
      setLoading(false);
    }
  }

  async function handleReintegrate(payload: ReintegrationPayload) {
    if (!armement) return;
    setReintegrating(true);
    try {
      const updated = await reintegrateArmement(armement.id, payload);
      addNotification("success", "Réintégrée", "Arme réintégrée avec succès");
      setReintOpen(false);
      setArmement({ ...updated, attachments: armement.attachments });
    } catch (err: unknown) {
      let msg = "Erreur lors de la réintégration";
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
      setReintegrating(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!armement) return null;

  const reintegree = isReintegree(armement);
  const munitionsRestantes =
    armement.munitions !== null && armement.munitions_consommees !== null
      ? armement.munitions - armement.munitions_consommees
      : null;

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
              Perception du {formatDate(armement.date_perception)}
              {reintegree ? (
                <Badge variant="secondary">Réintégrée</Badge>
              ) : (
                <Badge>En cours</Badge>
              )}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">
              {formatHeure(armement.heure_perception)} — {armeDisplay(armement)} — {agentPreneurDisplay(armement)}
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
          {canEdit && !reintegree && (
            <Button className="gap-2" onClick={() => setReintOpen(true)}>
              <ShieldCheck className="h-4 w-4" />
              Réintégration
            </Button>
          )}
          {canEdit && (
            <Button
              variant="outline"
              className="gap-2"
              onClick={() => navigate(`${LIST_PATH}/${armement.id}/edit`)}
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
            <ShieldEllipsis className="h-4 w-4" />
            Perception
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="Date de la perception" value={formatDate(armement.date_perception)} />
          <DetailRow label="Heure de la perception" value={formatHeure(armement.heure_perception)} />
          <DetailRow label="Type d'arme" value={armement.type_arme} />
          <DetailRow label="Matricule de l'arme" value={armement.matricule_arme} />
          <DetailRow label="Munitions" value={armement.munitions !== null ? String(armement.munitions) : "—"} />
          <DetailRow label="Secteur / Mission" value={armement.secteur_mission} />
          <DetailRow
            label="État à la perception"
            value={<span className="whitespace-pre-wrap">{armement.etat_perception}</span>}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <UserCheck className="h-4 w-4" />
            Agent preneur
          </CardTitle>
        </CardHeader>
        <CardContent className="grid grid-cols-2 gap-4">
          <DetailRow label="IM" value={armement.agent_preneur_im} />
          <DetailRow label="Grade" value={armement.agent_preneur_grade} />
          <DetailRow label="Nom complet" value={armement.agent_preneur_nom} />
        </CardContent>
      </Card>

      {reintegree && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <ShieldCheck className="h-4 w-4" />
              Réintégration
            </CardTitle>
          </CardHeader>
          <CardContent className="grid grid-cols-2 gap-4">
            <DetailRow label="Heure de la réintégration" value={formatHeure(armement.heure_reintegration)} />
            <DetailRow
              label="Munitions consommées"
              value={
                armement.munitions_consommees !== null
                  ? String(armement.munitions_consommees) +
                    (munitionsRestantes !== null ? ` (restantes : ${munitionsRestantes})` : "")
                  : "—"
              }
            />
            <DetailRow
              label="État à la réintégration"
              value={<span className="whitespace-pre-wrap">{armement.etat_reintegration}</span>}
            />
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Paperclip className="h-4 w-4" />
            Pièces jointes ({armement.attachments?.length ?? 0})
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {(armement.attachments ?? []).length === 0 && (
            <p className="text-sm text-muted-foreground">Aucune pièce jointe</p>
          )}
          {(armement.attachments ?? []).map((att) => (
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
                  href={getArmementAttachmentDownloadUrl(armement.id, att.id)}
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

      <ReintegrationDialog
        open={reintOpen}
        armement={armement}
        loading={reintegrating}
        onConfirm={handleReintegrate}
        onCancel={() => setReintOpen(false)}
      />

      <ImageViewerDialog
        open={viewerTarget !== null}
        src={
          viewerTarget
            ? getArmementAttachmentDownloadUrl(armement.id, viewerTarget.id)
            : ""
        }
        title={viewerTarget?.title}
        onClose={() => setViewerTarget(null)}
      />
    </motion.div>
  );

  async function handleExportPDF() {
    if (!armement) return;

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
    doc.text("Fiche d'Armement", pageWidth / 2, logoY + 8, { align: "center" });

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

    const printSection = (title: string, lines: [string, string][], startY: number): number => {
      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text(title, 14, startY);
      doc.setFontSize(10);
      let y = startY + 8;
      for (const [label, value] of lines) {
        doc.setFont("helvetica", "bold");
        doc.text(`${label} :`, 14, y);
        doc.setFont("helvetica", "normal");
        doc.text(value, 70, y);
        y += 7;
      }
      return y + 4;
    };

    let yPos = printSection(
      "Informations de la perception",
      [
        ["Date", formatDate(armement.date_perception)],
        ["Heure", formatHeure(armement.heure_perception)],
        ["Type d'arme", armement.type_arme || "—"],
        ["Matricule", armement.matricule_arme || "—"],
        ["Munitions", armement.munitions !== null ? String(armement.munitions) : "—"],
        ["Secteur / Mission", armement.secteur_mission || "—"],
        ["État à la perception", armement.etat_perception || "—"],
      ],
      lineY + lineHeight + 14,
    );

    yPos = printSection(
      "Agent preneur",
      [
        ["IM", armement.agent_preneur_im || "—"],
        ["Grade", armement.agent_preneur_grade || "—"],
        ["Nom complet", armement.agent_preneur_nom || "—"],
      ],
      yPos,
    );

    if (reintegree) {
      yPos = printSection(
        "Réintégration",
        [
          ["Heure", formatHeure(armement.heure_reintegration)],
          ["État à la réintégration", armement.etat_reintegration || "—"],
          [
            "Munitions consommées",
            armement.munitions_consommees !== null
              ? String(armement.munitions_consommees) +
                (munitionsRestantes !== null ? ` (restantes : ${munitionsRestantes})` : "")
              : "—",
          ],
        ],
        yPos,
      );
    } else {
      doc.setFontSize(13);
      doc.setFont("helvetica", "bold");
      doc.text("Réintégration", 14, yPos);
      doc.setFontSize(10);
      doc.setFont("helvetica", "normal");
      doc.text("Arme en cours de perception (non réintégrée).", 14, yPos + 8);
      yPos += 18;
    }

    // Pièces jointes
    const attachments = armement.attachments ?? [];
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

    doc.save(`armement_${armement.date_perception}_${armement.id}.pdf`);
  }
}
