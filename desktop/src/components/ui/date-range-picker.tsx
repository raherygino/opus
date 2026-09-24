import { ArrowRight } from "lucide-react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface DateRangePickerProps {
  /** ISO dates (yyyy-MM-dd); empty string when unset. */
  start: string;
  end: string;
  onChange: (range: { start: string; end: string }) => void;
  startInvalid?: boolean;
  endInvalid?: boolean;
  disabled?: boolean;
  id?: string;
}

/**
 * Période (date range) picker — two native date inputs presented as a single
 * "Du … au …" control. Uses the same native <input type="date"> as every
 * other form in the app; selecting a start date bounds the end input's
 * minimum (and vice versa) so the range stays coherent.
 */
export function DateRangePicker({
  start,
  end,
  onChange,
  startInvalid = false,
  endInvalid = false,
  disabled = false,
  id,
}: DateRangePickerProps) {
  return (
    <div
      id={id}
      className={cn(
        "flex items-center gap-2 rounded-md border border-input bg-background px-3 py-2",
        (startInvalid || endInvalid) && "border-destructive",
      )}
    >
      <div className="flex flex-1 items-center gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground whitespace-nowrap">
          Du
        </span>
        <Input
          type="date"
          value={start}
          max={end || undefined}
          disabled={disabled}
          aria-label="Date de début"
          aria-invalid={startInvalid || undefined}
          onChange={(e) => onChange({ start: e.target.value, end })}
          className="h-8 border-transparent bg-transparent shadow-none px-1 focus-visible:ring-1"
        />
      </div>
      <ArrowRight className="h-4 w-4 shrink-0 text-muted-foreground" />
      <div className="flex flex-1 items-center gap-2">
        <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground whitespace-nowrap">
          Au
        </span>
        <Input
          type="date"
          value={end}
          min={start || undefined}
          disabled={disabled}
          aria-label="Date de fin"
          aria-invalid={endInvalid || undefined}
          onChange={(e) => onChange({ start, end: e.target.value })}
          className="h-8 border-transparent bg-transparent shadow-none px-1 focus-visible:ring-1"
        />
      </div>
    </div>
  );
}
