import {
  AlignmentType,
  BorderStyle,
  Document,
  ExternalHyperlink,
  Footer,
  HeightRule,
  ImageRun,
  Packer,
  PageNumber,
  Paragraph,
  Table,
  TableCell,
  TableRow,
  TextRun,
  TextWrappingSide,
  TextWrappingType,
  VerticalAlign,
  HorizontalPositionRelativeFrom,
  HorizontalPositionAlign,
  PageOrientation,
  VerticalPositionRelativeFrom,
  VerticalPositionAlign,
  WidthType,
  type FileChild,
  type ITableBordersOptions,
} from "docx";
import logoPn from "@/assets/img/logo-pn.png";
import logoCsp from "@/assets/img/logo-csp.png";
import logoOpus from "@/assets/img/logo-opus.png";
import {
  fetchImageDataUrl,
  PDF_MARGIN,
  type PdfReport,
  type ReportElement,
} from "./pdf-report";

const TWIPS_PER_MM = 56.6929;
const PX_PER_MM = 96 / 25.4;
const EMU_PER_MM = 36000;
const A4_WIDTH_MM = 210;
const A4_HEIGHT_MM = 297;

const HEADER_FILL = "646464"; // matches the PDF gray table headers
const STRIPE_FILL = "F5F5F5"; // matches the striped theme
const MUTED = "969696";
const LINK_COLOR = "0000C8";
const CONTENT_WIDTH = Math.round((A4_WIDTH_MM - PDF_MARGIN * 2) * TWIPS_PER_MM);

export const mmToTwips = (mm: number) => Math.round(mm * TWIPS_PER_MM);
const mmToPx = (mm: number) => Math.round(mm * PX_PER_MM);
/** Font size in points → docx half-points. */
export const pt = (size: number) => Math.round(size * 2);

const NO_BORDERS: ITableBordersOptions = {
  top: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
  bottom: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
  left: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
  right: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
  insideHorizontal: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
  insideVertical: { style: BorderStyle.NONE, size: 0, color: "FFFFFF" },
};

const THIN_BORDER = { style: BorderStyle.SINGLE, size: 4, color: "000000" };
const CELL_BORDERS = {
  top: THIN_BORDER,
  bottom: THIN_BORDER,
  left: THIN_BORDER,
  right: THIN_BORDER,
};
const THIN_BORDERS: ITableBordersOptions = {
  ...CELL_BORDERS,
  insideHorizontal: THIN_BORDER,
  insideVertical: THIN_BORDER,
};

export type DocxCellSpec =
  | string
  | number
  | {
      text: string;
      bold?: boolean;
      color?: string;
      fill?: string;
      colSpan?: number;
      mono?: boolean;
      alignRight?: boolean;
    };

function imageType(dataUrl: string): "png" | "jpg" | "gif" | "bmp" {
  const mime = dataUrl.slice(5, dataUrl.indexOf(";")).toLowerCase();
  if (mime.includes("jpeg") || mime.includes("jpg")) return "jpg";
  if (mime.includes("gif")) return "gif";
  if (mime.includes("bmp")) return "bmp";
  return "png";
}

export function imageRun(
  dataUrl: string,
  widthMm: number,
  heightMm: number,
): ImageRun {
  return new ImageRun({
    type: imageType(dataUrl),
    data: dataUrl,
    transformation: { width: mmToPx(widthMm), height: mmToPx(heightMm) },
  });
}

function rowsTable(pairs: [string, string][], valueX: number): Table {
  const labelWidth = mmToTwips(valueX - PDF_MARGIN);
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [labelWidth, CONTENT_WIDTH - labelWidth],
    borders: NO_BORDERS,
    rows: pairs.map(
      ([label, value]) =>
        new TableRow({
          children: [
            new TableCell({
              children: [
                new Paragraph({
                  spacing: { after: 60 },
                  children: [
                    new TextRun({
                      text: `${label} :`,
                      bold: true,
                      size: pt(10),
                    }),
                  ],
                }),
              ],
            }),
            new TableCell({
              children: [
                new Paragraph({
                  spacing: { after: 60 },
                  children: [
                    new TextRun({ text: value || "—", size: pt(10) }),
                  ],
                }),
              ],
            }),
          ],
        }),
    ),
  });
}

function bodyParagraph(text: string, indent = 0): Paragraph {
  return new Paragraph({
    spacing: { after: 120 },
    indent: indent ? { left: mmToTwips(indent) } : undefined,
    children: [new TextRun({ text: text || "—", size: pt(10) })],
  });
}

function specCell(
  spec: DocxCellSpec,
  fill: string | undefined,
  fontSize: number,
  widthTwips?: number,
): TableCell {
  const cell: Exclude<DocxCellSpec, string | number> =
    typeof spec === "object" ? spec : { text: String(spec) };
  const resolvedFill = cell.fill ?? fill;
  return new TableCell({
    columnSpan: cell.colSpan,
    width: widthTwips ? { size: widthTwips, type: WidthType.DXA } : undefined,
    shading: resolvedFill ? { fill: resolvedFill } : undefined,
    margins: { top: 40, bottom: 40, left: 80, right: 80 },
    children: [
      new Paragraph({
        alignment: cell.alignRight ? AlignmentType.RIGHT : undefined,
        children: [
          new TextRun({
            text: cell.text || "—",
            bold: cell.bold,
            color: cell.color,
            font: cell.mono ? "Courier New" : undefined,
            size: pt(fontSize),
          }),
        ],
      }),
    ],
  });
}

/**
 * Generic data table matching the PDF "striped" theme: gray header with white
 * bold text, alternating light-gray body rows, no borders. Cells may be plain
 * values or {@link DocxCellSpec} objects (bold, color, fill, colSpan, mono).
 */
export function dataTable(
  head: DocxCellSpec[],
  body: DocxCellSpec[][],
  opts: {
    fontSize?: number;
    headFill?: string;
    striped?: boolean;
    widthTwips?: number;
    /** Column widths in mm; null/0 entries share the remaining width. */
    widthsMm?: (number | null)[];
  } = {},
): Table {
  const fontSize = opts.fontSize ?? 9;
  const width = opts.widthTwips ?? CONTENT_WIDTH;
  let columnWidths: number[] | undefined;
  if (opts.widthsMm?.length) {
    const fixedMm = opts.widthsMm.reduce<number>(
      (s, w) => s + (w ?? 0),
      0,
    );
    const autoCount = opts.widthsMm.filter((w) => w == null || w === 0).length;
    const autoTwips = Math.max(
      0,
      Math.floor((width - mmToTwips(fixedMm)) / Math.max(autoCount, 1)),
    );
    columnWidths = opts.widthsMm.map((w) =>
      w == null || w === 0 ? autoTwips : mmToTwips(w),
    );
  }
  const headerRow = new TableRow({
    tableHeader: true,
    children: head.map((h, i) => {
      const spec: Exclude<DocxCellSpec, string | number> =
        typeof h === "object" ? h : { text: String(h) };
      return new TableCell({
        columnSpan: spec.colSpan,
        width: columnWidths?.[i]
          ? { size: columnWidths[i], type: WidthType.DXA }
          : undefined,
        shading: { fill: opts.headFill ?? HEADER_FILL },
        margins: { top: 40, bottom: 40, left: 80, right: 80 },
        children: [
          new Paragraph({
            alignment: spec.alignRight ? AlignmentType.RIGHT : undefined,
            children: [
              new TextRun({
                text: spec.text,
                bold: true,
                color: "FFFFFF",
                size: pt(fontSize),
              }),
            ],
          }),
        ],
      });
    }),
  });
  const bodyRows = body.map((row, i) => {
    const fill = opts.striped !== false && i % 2 === 1 ? STRIPE_FILL : undefined;
    return new TableRow({
      children: row.map((spec, colIdx) =>
        specCell(spec, fill, fontSize, columnWidths?.[colIdx]),
      ),
    });
  });
  return new Table({
    width: { size: width, type: WidthType.DXA },
    columnWidths,
    borders: NO_BORDERS,
    rows: [headerRow, ...bodyRows],
  });
}

/** Bold title + gray info lines, matching the plain "OPUS — …" report header. */
export function titleBlock(
  title: string,
  infoLines: string[],
): Paragraph[] {
  return [
    new Paragraph({
      spacing: { after: 160 },
      children: [new TextRun({ text: title, bold: true, size: pt(16) })],
    }),
    ...infoLines.map(
      (line) =>
        new Paragraph({
          spacing: { after: 40 },
          children: [new TextRun({ text: line, size: pt(9), color: "646464" })],
        }),
    ),
    new Paragraph({ spacing: { after: 120 } }),
  ];
}

/** Centered "OPUS — Page X/Y" footer (with the OPUS logo when available). */
export async function opusFooter(): Promise<Footer> {
  const logo = await fetchImageDataUrl(logoOpus);
  const runs: (TextRun | ImageRun)[] = [];
  if (logo) runs.push(imageRun(logo, 4, 4));
  runs.push(
    new TextRun({ text: "OPUS — Page ", size: pt(8), color: MUTED }),
    new TextRun({ children: [PageNumber.CURRENT], size: pt(8), color: MUTED }),
    new TextRun({ text: "/", size: pt(8), color: MUTED }),
    new TextRun({
      children: [PageNumber.TOTAL_PAGES],
      size: pt(8),
      color: MUTED,
    }),
  );
  return new Footer({
    children: [
      new Paragraph({ alignment: AlignmentType.CENTER, children: runs }),
    ],
  });
}

/**
 * Builds a plain OPUS document (title metadata, Arial, A4, standard margins,
 * page-number footer) around the given blocks and downloads it. Used by the
 * pages that generate their Word layout directly (rapport, audit logs).
 */
export async function saveSimpleDocx(opts: {
  title: string;
  children: FileChild[];
  filename: string;
  landscape?: boolean;
}): Promise<void> {
  const doc = new Document({
    creator: "OPUS",
    title: opts.title,
    styles: {
      default: { document: { run: { font: "Arial", size: pt(10) } } },
    },
    sections: [
      {
        properties: {
          page: {
            size: opts.landscape
              ? {
                  width: mmToTwips(A4_HEIGHT_MM),
                  height: mmToTwips(A4_WIDTH_MM),
                  orientation: PageOrientation.LANDSCAPE,
                }
              : {
                  width: mmToTwips(A4_WIDTH_MM),
                  height: mmToTwips(A4_HEIGHT_MM),
                },
            margin: {
              top: mmToTwips(PDF_MARGIN),
              bottom: mmToTwips(PDF_MARGIN),
              left: mmToTwips(PDF_MARGIN),
              right: mmToTwips(PDF_MARGIN),
            },
          },
        },
        footers: { default: await opusFooter() },
        children: opts.children,
      },
    ],
  });
  await saveDocxFile(doc, opts.filename);
}

/** Packs the document and triggers the browser/Electron save flow. */
export async function saveDocxFile(
  doc: Document,
  filename: string,
): Promise<void> {
  if (!/\.docx$/i.test(filename)) filename += ".docx";
  const blob = await Packer.toBlob(doc);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function linkParagraph(text: string, url: string): Paragraph {
  return new Paragraph({
    spacing: { after: 120 },
    children: [
      new ExternalHyperlink({
        link: url,
        children: [
          new TextRun({
            text,
            color: LINK_COLOR,
            underline: {},
            size: pt(10),
          }),
        ],
      }),
    ],
  });
}

function photoGridTable(
  items: { dataUrl: string | null; caption?: string | null }[],
  sizeMm: number,
): Table {
  const cols = Math.max(
    1,
    Math.floor((A4_WIDTH_MM - PDF_MARGIN * 2) / (sizeMm + 5)),
  );
  const cellWidth = Math.floor(CONTENT_WIDTH / cols);
  const rows: TableRow[] = [];
  for (let i = 0; i < items.length; i += cols) {
    const chunk = items.slice(i, i + cols);
    rows.push(
      new TableRow({
        children: chunk.map((item) => {
          const children: Paragraph[] = [
            item.dataUrl
              ? new Paragraph({
                  alignment: AlignmentType.CENTER,
                  children: [imageRun(item.dataUrl, sizeMm, sizeMm)],
                })
              : new Paragraph({ children: [new TextRun(" ")] }),
          ];
          if (item.caption) {
            children.push(
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 40 },
                children: [
                  new TextRun({ text: item.caption, size: pt(8) }),
                ],
              }),
            );
          }
          return new TableCell({
            width: { size: cellWidth, type: WidthType.DXA },
            borders: item.dataUrl ? CELL_BORDERS : undefined,
            verticalAlign: VerticalAlign.CENTER,
            children,
          });
        }),
      }),
    );
  }
  return new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: Array(cols).fill(cellWidth),
    borders: NO_BORDERS,
    rows,
  });
}

/** Floating top-right photo (square wrap, text flows on the left). */
function floatingImageRun(dataUrl: string, sizeMm: number): ImageRun {
  return new ImageRun({
    type: imageType(dataUrl),
    data: dataUrl,
    transformation: { width: mmToPx(sizeMm), height: mmToPx(sizeMm) },
    floating: {
      horizontalPosition: {
        relative: HorizontalPositionRelativeFrom.MARGIN,
        align: HorizontalPositionAlign.RIGHT,
      },
      verticalPosition: {
        relative: VerticalPositionRelativeFrom.PARAGRAPH,
        align: VerticalPositionAlign.TOP,
      },
      wrap: { type: TextWrappingType.SQUARE, side: TextWrappingSide.LEFT },
      margins: { left: 4 * EMU_PER_MM, bottom: 4 * EMU_PER_MM },
    },
  });
}

/** PN / CSP logos, centered title + generation date, Senegal tricolor strip. */
async function brandedHeaderBlocks(
  title: string,
): Promise<FileChild[]> {
  const logoMm = 22;
  const logoTwips = mmToTwips(logoMm);
  const [pn, csp] = await Promise.all([
    fetchImageDataUrl(logoPn),
    fetchImageDataUrl(logoCsp),
  ]);

  const logoCell = (
    dataUrl: string | null,
    align: (typeof AlignmentType)[keyof typeof AlignmentType],
  ) =>
    new TableCell({
      width: { size: logoTwips, type: WidthType.DXA },
      verticalAlign: VerticalAlign.TOP,
      children: [
        new Paragraph({
          alignment: align,
          children: dataUrl ? [imageRun(dataUrl, logoMm, logoMm)] : [],
        }),
      ],
    });

  const header = new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [logoTwips, CONTENT_WIDTH - logoTwips * 2, logoTwips],
    borders: NO_BORDERS,
    rows: [
      new TableRow({
        children: [
          logoCell(pn, AlignmentType.LEFT),
          new TableCell({
            verticalAlign: VerticalAlign.TOP,
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 60 },
                children: [
                  new TextRun({ text: title, bold: true, size: pt(18) }),
                ],
              }),
              new Paragraph({
                alignment: AlignmentType.CENTER,
                spacing: { before: 80 },
                children: [
                  new TextRun({
                    text: `Généré le ${new Date().toLocaleDateString("fr-FR")}`,
                    size: pt(10),
                  }),
                ],
              }),
            ],
          }),
          logoCell(csp, AlignmentType.RIGHT),
        ],
      }),
    ],
  });

  const third = Math.floor(CONTENT_WIDTH / 3);
  const flag = new Table({
    width: { size: CONTENT_WIDTH, type: WidthType.DXA },
    columnWidths: [third, third, CONTENT_WIDTH - third * 2],
    borders: THIN_BORDERS,
    rows: [
      new TableRow({
        height: { value: mmToTwips(2), rule: HeightRule.EXACT },
        children: ["009650", "FFFFFF", "DC2828"].map(
          (fill) =>
            new TableCell({
              shading: { fill },
              children: [new Paragraph({ children: [new TextRun(" ")] })],
            }),
        ),
      }),
    ],
  });

  return [header, flag, new Paragraph({ spacing: { after: 80 } })];
}

function renderElement(el: ReportElement): FileChild[] {
  switch (el.type) {
    case "section":
      return [
        new Paragraph({
          keepNext: true,
          spacing: { before: 280, after: 140 },
          children: [
            new TextRun({ text: el.title, bold: true, size: pt(13) }),
          ],
        }),
      ];
    case "rows":
      return [rowsTable(el.pairs, el.valueX)];
    case "paragraph":
      return [bodyParagraph(el.text, el.indent)];
    case "labeledText":
      return [
        new Paragraph({
          spacing: { before: 120, after: 40 },
          children: [
            new TextRun({ text: `${el.label} :`, bold: true, size: pt(10) }),
          ],
        }),
        bodyParagraph(el.text),
      ];
    case "table":
      return [dataTable(el.head, el.body, { fontSize: el.fontSize })];
    case "link":
      return [linkParagraph(el.text, el.url)];
    case "image":
      return [
        new Paragraph({
          spacing: { after: 160 },
          children: [imageRun(el.dataUrl, el.widthMm, el.heightMm)],
        }),
      ];
    case "photoGrid":
      return [photoGridTable(el.items, el.sizeMm)];
    case "topRightPhoto":
      return []; // hoisted: rendered as a floating image on the first block
  }
}

/**
 * Renders a recorded {@link PdfReport} to a `.docx` matching the PDF layout:
 * branded header, tricolor separator, sections, key/value rows, striped
 * tables, links, images, photo grids and the OPUS page-number footer.
 */
export async function saveReportAsDocx(
  report: PdfReport,
  filename: string,
): Promise<void> {
  const children: FileChild[] = [
    ...(await brandedHeaderBlocks(report.title)),
  ];

  const photoEl = report.elements.find((e) => e.type === "topRightPhoto");
  let pendingPhoto =
    photoEl && photoEl.type === "topRightPhoto" && photoEl.dataUrl
      ? floatingImageRun(photoEl.dataUrl, photoEl.sizeMm)
      : null;

  for (const el of report.elements) {
    const blocks = renderElement(el);
    if (pendingPhoto && blocks.length > 0) {
      // Anchor the floating photo at the first rendered block: the image
      // sits at the top-right margin and content wraps on its left.
      children.push(new Paragraph({ children: [pendingPhoto] }));
      pendingPhoto = null;
    }
    children.push(...blocks);
  }
  if (pendingPhoto) {
    children.push(new Paragraph({ children: [pendingPhoto] }));
  }

  const doc = new Document({
    creator: "OPUS",
    title: report.title,
    styles: {
      default: { document: { run: { font: "Arial", size: pt(10) } } },
    },
    sections: [
      {
        properties: {
          page: {
            size: {
              width: mmToTwips(A4_WIDTH_MM),
              height: mmToTwips(A4_HEIGHT_MM),
            },
            margin: {
              top: mmToTwips(PDF_MARGIN),
              bottom: mmToTwips(PDF_MARGIN),
              left: mmToTwips(PDF_MARGIN),
              right: mmToTwips(PDF_MARGIN),
            },
          },
        },
        footers: { default: await opusFooter() },
        children,
      },
    ],
  });
  await saveDocxFile(doc, filename);
}
