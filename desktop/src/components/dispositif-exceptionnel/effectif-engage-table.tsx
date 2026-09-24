import { useMemo, type Dispatch, type SetStateAction } from "react";
import {
  useReactTable,
  getCoreRowModel,
  flexRender,
  type ColumnDef,
} from "@tanstack/react-table";
import { Plus, Trash2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";

/**
 * Effectif engagé — sector rows of a dispositif exceptionnel. Single flat
 * table; same inline-edit affordance as the rassemblement's
 * RepartitionSecteurTable.
 */

/** An effectif row with a stable local key (stripped before submission). */
export interface EditableEffectifRow {
  _key: string;
  secteur: string;
  chef_element_contact?: string | null;
  controle_contact?: string | null;
  materiels_armements?: string | null;
  missions?: string | null;
}

export function newEffectifKey(): string {
  return crypto.randomUUID();
}

export function blankEffectifRow(): EditableEffectifRow {
  return {
    _key: newEffectifKey(),
    secteur: "",
    chef_element_contact: "",
    controle_contact: "",
    materiels_armements: "",
    missions: "",
  };
}

// Inline-edit affordance: cells read as plain text until hovered/focused.
const CELL_INPUT_CLASS =
  "h-8 px-2 border-transparent bg-transparent shadow-none focus-visible:ring-1 focus-visible:bg-background hover:bg-accent/60 dark:hover:bg-accent/40";
const CELL_TEXTAREA_CLASS =
  "min-h-8 py-1.5 px-2 border-transparent bg-transparent shadow-none focus-visible:ring-1 focus-visible:bg-background hover:bg-accent/60 dark:hover:bg-accent/40 resize-y";

interface EffectifEngageTableProps {
  rows: EditableEffectifRow[];
  /** When omitted the table is read-only (detail view). */
  onChange?: Dispatch<SetStateAction<EditableEffectifRow[]>>;
  readOnly?: boolean;
  /** Field keys of cells that failed validation — highlighted in red. */
  invalidSecteurKeys?: Set<string>;
}

export function EffectifEngageTable({
  rows,
  onChange,
  readOnly = false,
  invalidSecteurKeys,
}: EffectifEngageTableProps) {
  const editable = !readOnly && !!onChange;

  // Functional updates keep these callbacks independent of the current `rows`
  // snapshot, so `columns` can stay memoized — a new cell renderer identity
  // per keystroke would remount inputs and drop focus.
  function updateField(key: string, field: keyof EditableEffectifRow, value: string) {
    onChange?.((prev) => prev.map((r) => (r._key === key ? { ...r, [field]: value } : r)));
  }

  function addRow() {
    onChange?.((prev) => [...prev, blankEffectifRow()]);
  }

  function removeRow(key: string) {
    onChange?.((prev) => prev.filter((r) => r._key !== key));
  }

  const columns = useMemo<ColumnDef<EditableEffectifRow>[]>(
    () => [
      {
        id: "secteur",
        header: "Secteur *",
        cell: ({ row }) => renderCell(row, "secteur", false),
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
            } as ColumnDef<EditableEffectifRow>,
          ]
        : []),
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [editable, onChange, invalidSecteurKeys],
  );

  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    getRowId: (row) => row._key,
  });

  function renderCell(
    row: { id: string; original: EditableEffectifRow },
    field: keyof EditableEffectifRow,
    multiline: boolean,
  ) {
    const value = (row.original[field] as string | null | undefined) ?? "";
    if (!editable) {
      if (value === "") return <span className="text-muted-foreground">—</span>;
      return multiline ? <span className="whitespace-pre-wrap">{value}</span> : value;
    }
    const set = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      updateField(row.id, field, e.target.value);
    const invalid = field === "secteur" && invalidSecteurKeys?.has(row.id);
    return multiline ? (
      <Textarea rows={2} value={value} onChange={set} className={CELL_TEXTAREA_CLASS} />
    ) : (
      <Input
        value={value}
        onChange={set}
        aria-invalid={invalid || undefined}
        className={cn(CELL_INPUT_CLASS, invalid && "border-destructive focus-visible:ring-destructive")}
      />
    );
  }

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
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={table.getAllLeafColumns().length}
                className="px-3 py-4 text-center text-sm text-muted-foreground"
              >
                Aucune ligne d'effectif engagé
              </td>
            </tr>
          ) : (
            table.getRowModel().rows.map((row) => (
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
        </tbody>
      </table>
      {editable && (
        <div className="border-t border-border px-3 py-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-7"
            onClick={addRow}
          >
            <Plus className="mr-1 h-3.5 w-3.5" />
            Ajouter une ligne
          </Button>
        </div>
      )}
    </div>
  );
}
