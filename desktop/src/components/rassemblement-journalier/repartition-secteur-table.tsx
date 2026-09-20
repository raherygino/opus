import { Fragment, useMemo } from "react";
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from "@tanstack/react-table";
import { Plus, Trash2, Sun, Moon } from "lucide-react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import type { RepartitionSecteurType } from "@/types";

/**
 * Répartition par secteur — the two sections (Diurne / Nocturne) share the
 * exact same structure; only the `type` value differs. Both sections are
 * generated from this single component and rendered as one unified table.
 */
export const REPARTITION_TYPES: {
  type: RepartitionSecteurType;
  label: string;
  icon: typeof Sun;
  iconClass: string;
}[] = [
  { type: "diurne", label: "Diurne", icon: Sun, iconClass: "text-amber-500" },
  { type: "nocturne", label: "Nocturne", icon: Moon, iconClass: "text-indigo-400" },
];

/** A repartition row with a stable local key (stripped before submission). */
export interface EditableRepartitionRow {
  _key: string;
  type: RepartitionSecteurType;
  secteur: string;
  effectif_engage?: string | null;
  chef_element_contact?: string | null;
  controle_contact?: string | null;
  materiels_armements?: string | null;
  missions?: string | null;
}

export function newRepartitionKey(): string {
  return crypto.randomUUID();
}

// Inline-edit affordance: cells read as plain text until hovered/focused.
const CELL_INPUT_CLASS =
  "h-8 px-2 border-transparent bg-transparent shadow-none focus-visible:ring-1 focus-visible:bg-background hover:bg-accent/60 dark:hover:bg-accent/40";
const CELL_TEXTAREA_CLASS =
  "min-h-8 py-1.5 px-2 border-transparent bg-transparent shadow-none focus-visible:ring-1 focus-visible:bg-background hover:bg-accent/60 dark:hover:bg-accent/40 resize-y";

interface RepartitionSecteurTableProps {
  rows: EditableRepartitionRow[];
  /** When omitted the table is read-only (detail view). */
  onChange?: (rows: EditableRepartitionRow[]) => void;
  readOnly?: boolean;
}

export function RepartitionSecteurTable({
  rows,
  onChange,
  readOnly = false,
}: RepartitionSecteurTableProps) {
  const editable = !readOnly && !!onChange;

  function updateField(key: string, field: keyof EditableRepartitionRow, value: string) {
    onChange?.(rows.map((r) => (r._key === key ? { ...r, [field]: value } : r)));
  }

  function addRow(type: RepartitionSecteurType) {
    onChange?.([
      ...rows,
      {
        _key: newRepartitionKey(),
        type,
        secteur: "",
        effectif_engage: "",
        chef_element_contact: "",
        controle_contact: "",
        materiels_armements: "",
        missions: "",
      },
    ]);
  }

  function removeRow(key: string) {
    onChange?.(rows.filter((r) => r._key !== key));
  }

  const columns = useMemo<ColumnDef<EditableRepartitionRow>[]>(
    () => [
      {
        id: "secteur",
        header: "Secteur",
        cell: ({ row }) => renderCell(row, "secteur", false),
      },
      {
        id: "effectif_engage",
        header: "Effectif engagé",
        cell: ({ row }) => renderCell(row, "effectif_engage", false),
      },
      {
        id: "chef_element_contact",
        header: "Chef d'élément avec contact",
        cell: ({ row }) => renderCell(row, "chef_element_contact", false),
      },
      {
        id: "controle_contact",
        header: "Contrôle avec contact",
        cell: ({ row }) => renderCell(row, "controle_contact", false),
      },
      {
        id: "materiels_armements",
        header: "Matériels et armements",
        cell: ({ row }) => renderCell(row, "materiels_armements", true),
      },
      {
        id: "missions",
        header: "Missions",
        cell: ({ row }) => renderCell(row, "missions", true),
      },
      ...(editable
        ? [
            {
              id: "actions",
              header: "",
              cell: ({ row }: { row: { id: string } }) => (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => removeRow(row.id)}
                  title="Supprimer la ligne"
                >
                  <Trash2 className="h-3.5 w-3.5 text-destructive" />
                </Button>
              ),
            } as ColumnDef<EditableRepartitionRow>,
          ]
        : []),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [editable, rows],
  );

  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row._key,
  });

  function renderCell(
    row: { id: string; original: EditableRepartitionRow },
    field: keyof EditableRepartitionRow,
    multiline: boolean,
  ) {
    const value = (row.original[field] as string | null | undefined) ?? "";
    if (!editable) {
      if (value === "") return <span className="text-muted-foreground">—</span>;
      return multiline ? <span className="whitespace-pre-wrap">{value}</span> : value;
    }
    const set = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      updateField(row.id, field, e.target.value);
    return multiline ? (
      <Textarea rows={2} value={value} onChange={set} className={CELL_TEXTAREA_CLASS} />
    ) : (
      <Input value={value} onChange={set} className={CELL_INPUT_CLASS} />
    );
  }

  const columnCount = table.getAllLeafColumns().length;

  return (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table className="w-full text-sm">
        <thead>
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id} className="border-b border-border bg-muted/50">
              {headerGroup.headers.map((header) => (
                <th
                  key={header.id}
                  className="h-10 whitespace-nowrap border-l border-border px-3 text-left text-xs font-semibold uppercase tracking-wider text-muted-foreground first:border-l-0"
                >
                  {header.isPlaceholder
                    ? null
                    : flexRender(header.column.columnDef.header, header.getContext())}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody className="divide-y divide-border">
          {REPARTITION_TYPES.map((section) => {
            const sectionRows = table
              .getRowModel()
              .rows.filter((r) => r.original.type === section.type);
            const Icon = section.icon;
            return (
              <Fragment key={section.type}>
                {/* Section header — shared structure, only the type differs */}
                <tr className="border-b border-border bg-muted/30">
                  <td colSpan={columnCount} className="px-3 py-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Icon className={cn("h-4 w-4", section.iconClass)} />
                        <span className="text-sm font-medium">
                          Répartition par secteur – {section.label}
                        </span>
                        <Badge variant="secondary" className="ml-1">
                          {sectionRows.length}
                        </Badge>
                      </div>
                      {editable && (
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="h-7"
                          onClick={() => addRow(section.type)}
                        >
                          <Plus className="mr-1 h-3.5 w-3.5" />
                          Ajouter une ligne
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
                {sectionRows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={columnCount}
                      className="px-3 py-4 text-center text-sm text-muted-foreground"
                    >
                      Aucune répartition {section.label.toLowerCase()}
                    </td>
                  </tr>
                ) : (
                  sectionRows.map((row) => (
                    <tr key={row.id} className="group/row transition-colors hover:bg-accent/30">
                      {row.getVisibleCells().map((cell) => (
                        <td
                          key={cell.id}
                          className={cn(
                            "border-l border-border px-1.5 py-1 align-top first:border-l-0",
                            cell.column.id === "actions" && "w-12",
                          )}
                        >
                          {flexRender(cell.column.columnDef.cell, cell.getContext())}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </Fragment>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
