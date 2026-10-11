import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import logoPn from "@/assets/img/logo-pn.png";
import logoCsp from "@/assets/img/logo-csp.png";
import logoOpus from "@/assets/img/logo-opus.png";

export const PDF_MARGIN = 14;
const FOOTER_CLEARANCE = 24;

export type PdfRow = [string, string];

/**
 * Uniform "Fiche" report builder shared by all detail-page PDF exports.
 * Reproduces the Personnel Details layout: PN/CSP logos, centered title,
 * generation date, Senegal tricolor separator, gray-striped tables and an
 * OPUS footer with page numbers on every page.
 */
export class PdfReport {
  readonly doc: jsPDF;
  readonly pageWidth: number;
  readonly pageHeight: number;
  /** Y position of the first section title, right below the flag separator. */
  readonly contentTop: number;
  /** Current vertical cursor (baseline of the next element). */
  y: number;

  constructor(title: string) {
    this.doc = new jsPDF();
    this.pageWidth = this.doc.internal.pageSize.getWidth();
    this.pageHeight = this.doc.internal.pageSize.getHeight();

    const logoSize = 22;
    const logoY = 8;
    try {
      this.doc.addImage(logoPn, "PNG", PDF_MARGIN, logoY, logoSize, logoSize);
      this.doc.addImage(
        logoCsp,
        "PNG",
        this.pageWidth - PDF_MARGIN - logoSize,
        logoY,
        logoSize,
        logoSize,
      );
    } catch {
      // Images may fail to load in some environments
    }

    this.doc.setFontSize(18);
    this.doc.setFont("helvetica", "bold");
    this.doc.text(title, this.pageWidth / 2, logoY + 8, { align: "center" });

    this.doc.setFontSize(10);
    this.doc.setFont("helvetica", "normal");
    this.doc.text(
      `Généré le ${new Date().toLocaleDateString("fr-FR")}`,
      this.pageWidth / 2,
      logoY + 16,
      { align: "center" },
    );

    // Separator line: green | white | red (Senegal flag colors) with thin black border
    const lineHeight = 2;
    const lineY = logoY + logoSize + 8;
    const x0 = PDF_MARGIN;
    const x1 = this.pageWidth - PDF_MARGIN;
    const third = (x1 - x0) / 3;

    this.doc.setFillColor(0, 0, 0);
    this.doc.rect(x0, lineY, x1 - x0, lineHeight, "F");
    this.doc.setFillColor(0, 150, 80);
    this.doc.rect(x0 + 0.3, lineY + 0.3, third - 0.6, lineHeight - 0.6, "F");
    this.doc.setFillColor(255, 255, 255);
    this.doc.rect(x0 + third + 0.3, lineY + 0.3, third - 0.6, lineHeight - 0.6, "F");
    this.doc.setFillColor(220, 40, 40);
    this.doc.rect(x0 + third * 2 + 0.3, lineY + 0.3, third - 0.6, lineHeight - 0.6, "F");

    this.contentTop = lineY + lineHeight + 14;
    this.y = this.contentTop;
  }

  /** Adds a page when fewer than `needed` mm remain above the footer. */
  ensureSpace(needed: number): this {
    if (this.y + needed > this.pageHeight - FOOTER_CLEARANCE) {
      this.doc.addPage();
      this.y = 20;
    }
    return this;
  }

  /** Bold section heading; advances the cursor below it. */
  section(title: string): this {
    this.ensureSpace(14);
    this.doc.setFontSize(13);
    this.doc.setFont("helvetica", "bold");
    this.doc.text(title, PDF_MARGIN, this.y);
    this.y += 8;
    return this;
  }

  /**
   * Label / value rows: bold `Label :` at the left margin, wrapped value at
   * `valueX` (60 by default, matching the reference fiche).
   */
  rows(pairs: PdfRow[], valueX = 60): this {
    const valueWidth = this.pageWidth - valueX - PDF_MARGIN;
    this.doc.setFontSize(10);
    for (const [label, value] of pairs) {
      const lines = this.doc.splitTextToSize(
        value || "—",
        valueWidth,
      ) as string[];
      this.ensureSpace((lines.length - 1) * 5 + 7);
      this.doc.setFont("helvetica", "bold");
      this.doc.text(`${label} :`, PDF_MARGIN, this.y);
      this.doc.setFont("helvetica", "normal");
      this.doc.text(lines, valueX, this.y);
      this.y += (lines.length - 1) * 5 + 7;
    }
    this.y += 4;
    return this;
  }

  /** Wrapped paragraph of body text (long descriptions, observations…). */
  paragraph(text: string, indent = 0): this {
    this.doc.setFontSize(10);
    this.doc.setFont("helvetica", "normal");
    const lines = this.doc.splitTextToSize(
      text || "—",
      this.pageWidth - PDF_MARGIN * 2 - indent,
    ) as string[];
    this.ensureSpace(lines.length * 5);
    this.doc.text(lines, PDF_MARGIN + indent, this.y);
    this.y += lines.length * 5 + 2;
    return this;
  }

  /** Bold label on its own line followed by a wrapped paragraph. */
  labeledText(label: string, text: string): this {
    this.ensureSpace(12);
    this.doc.setFontSize(10);
    this.doc.setFont("helvetica", "bold");
    this.doc.text(`${label} :`, PDF_MARGIN, this.y);
    this.y += 5;
    this.paragraph(text);
    this.y += 2;
    return this;
  }

  /** Gray-striped table; the cursor moves below it afterwards. */
  table(head: string[], body: (string | number)[][], fontSize = 9): this {
    this.ensureSpace(14);
    autoTable(this.doc, {
      startY: this.y,
      theme: "striped",
      head: [head],
      body,
      headStyles: { fillColor: [100, 100, 100], textColor: [255, 255, 255] },
      styles: { fontSize },
      margin: { left: PDF_MARGIN, right: PDF_MARGIN, bottom: FOOTER_CLEARANCE },
    });
    this.y =
      (this.doc as unknown as { lastAutoTable?: { finalY: number } })
        .lastAutoTable?.finalY ?? this.y;
    this.y += 10;
    return this;
  }

  /** Blue clickable link line (e.g. "Voir sur la carte"). */
  link(text: string, url: string): this {
    this.ensureSpace(8);
    this.doc.setFontSize(10);
    this.doc.setFont("helvetica", "normal");
    this.doc.setTextColor(0, 0, 200);
    this.doc.textWithLink(text, PDF_MARGIN, this.y, { url });
    this.doc.setTextColor(0, 0, 0);
    this.y += 8;
    return this;
  }

  /** Renders an SVG (e.g. a captured signature) to PNG via canvas and embeds it. */
  async addSvg(svg: string, caption?: string, width = 60): Promise<this> {
    try {
      const blob = new Blob([svg], { type: "image/svg+xml" });
      const url = URL.createObjectURL(blob);
      const img = new Image();
      img.crossOrigin = "anonymous";
      const loaded = await new Promise<HTMLImageElement | null>((resolve) => {
        img.onload = () => resolve(img);
        img.onerror = () => resolve(null);
        img.src = url;
      });
      URL.revokeObjectURL(url);
      if (!loaded) return this;
      const h = (loaded.height / loaded.width) * width || 30;
      if (caption) this.section(caption);
      this.ensureSpace(h + 4);
      this.doc.addImage(loaded, "PNG", PDF_MARGIN, this.y, width, h);
      this.y += h + 6;
    } catch {
      // Signature conversion may fail in some environments — skip silently
    }
    return this;
  }

  /**
   * Square photo grid (e.g. wanted-person photos) with an optional caption
   * under each image. Images are fetched and embedded; failures draw an
   * empty frame so the layout stays intact.
   */
  async addPhotoGrid(
    items: { url: string; caption?: string | null }[],
    size = 45,
    gap = 5,
  ): Promise<this> {
    const maxX = this.pageWidth - PDF_MARGIN;
    let x = PDF_MARGIN;
    for (const item of items) {
      if (x + size > maxX) {
        x = PDF_MARGIN;
        this.y += size + 10;
      }
      const pagesBefore = this.doc.getNumberOfPages();
      this.ensureSpace(size + 10);
      if (this.doc.getNumberOfPages() !== pagesBefore) x = PDF_MARGIN;

      const dataUrl = await fetchImageDataUrl(item.url);
      let drawn = false;
      if (dataUrl) {
        const format = dataUrl.slice(5, dataUrl.indexOf(";")).toUpperCase();
        try {
          this.doc.addImage(dataUrl, format, x, this.y, size, size);
          drawn = true;
        } catch {
          // Fall through to the empty frame
        }
      }
      if (!drawn) {
        this.doc.setDrawColor(0, 0, 0);
        this.doc.setLineWidth(0.5);
        this.doc.rect(x, this.y, size, size);
      }
      if (item.caption) {
        this.doc.setFontSize(8);
        this.doc.setFont("helvetica", "normal");
        const cap = this.doc.splitTextToSize(item.caption, size) as string[];
        this.doc.text(cap[0] ?? "", x, this.y + size + 4);
      }
      x += size + gap;
    }
    this.y += size + 12;
    return this;
  }

  /**
   * Bordered photo box at the top-right of the content area (next to the
   * first section, like the reference fiche). Fetches and embeds the image
   * when a URL is provided, otherwise draws an empty frame.
   */
  async addTopRightPhoto(imageUrl: string | null, size = 30): Promise<this> {
    const x = this.pageWidth - PDF_MARGIN - size;
    const y = this.contentTop - 4;
    const dataUrl = imageUrl ? await fetchImageDataUrl(imageUrl) : null;
    if (dataUrl) {
      try {
        this.doc.addImage(dataUrl, "JPEG", x, y, size, size);
        return this;
      } catch {
        // Fall through to the empty frame
      }
    }
    this.doc.setDrawColor(0, 0, 0);
    this.doc.setLineWidth(0.5);
    this.doc.rect(x, y, size, size);
    return this;
  }

  /** Draws the OPUS footer (logo + page numbers) on every page, then saves. */
  save(filename: string): void {
    const logoSize = 14;
    const textY = this.pageHeight - 6;
    const logoY = textY - logoSize - 4;
    const pageCount = this.doc.getNumberOfPages();
    for (let i = 1; i <= pageCount; i++) {
      this.doc.setPage(i);
      try {
        this.doc.addImage(
          logoOpus,
          "PNG",
          this.pageWidth / 2 - logoSize / 2,
          logoY,
          logoSize,
          logoSize,
        );
      } catch {
        // Images may fail to load
      }
      this.doc.setFontSize(8);
      this.doc.setFont("helvetica", "normal");
      this.doc.setTextColor(150);
      this.doc.text(
        `OPUS — Page ${i}/${pageCount}`,
        this.pageWidth / 2,
        textY,
        { align: "center" },
      );
      this.doc.setTextColor(0, 0, 0);
    }
    this.doc.save(filename);
  }
}

/** Fetches an image URL and returns it as a data URL, or null on failure. */
export async function fetchImageDataUrl(url: string): Promise<string | null> {
  try {
    const response = await fetch(url);
    if (!response.ok) return null;
    const blob = await response.blob();
    return await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  } catch {
    return null;
  }
}
