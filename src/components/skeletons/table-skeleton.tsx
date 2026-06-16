import { Skeleton } from "@/components/ui/skeleton";

interface TableSkeletonProps {
  rows?: number;
  columns?: number;
}

export function TableSkeleton({ rows = 5, columns = 4 }: TableSkeletonProps) {
  return (
    <div className="rounded-xl border border-border">
      <div className="flex items-center gap-4 border-b border-border bg-muted/30 px-4 py-3">
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton
            key={i}
            className="h-4"
            style={{ flex: i === 0 ? 2 : 1 }}
          />
        ))}
      </div>
      {Array.from({ length: rows }).map((_, rowIdx) => (
        <div
          key={rowIdx}
          className="flex items-center gap-4 border-b border-border px-4 py-3 last:border-0"
        >
          {Array.from({ length: columns }).map((_, colIdx) => (
            <Skeleton
              key={colIdx}
              className="h-3.5"
              style={{
                flex: colIdx === 0 ? 2 : 1,
                width: colIdx === columns - 1 ? "48px" : undefined,
              }}
            />
          ))}
        </div>
      ))}
    </div>
  );
}
