import { Skeleton } from "@/components/ui/skeleton";

interface ListSkeletonProps {
  items?: number;
  variant?: "default" | "compact" | "card";
}

export function ListSkeleton({ items = 5, variant = "default" }: ListSkeletonProps) {
  if (variant === "card") {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: items }).map((_, i) => (
          <div key={i} className="rounded-xl border border-border p-4">
            <div className="flex items-start gap-3">
              <Skeleton className="h-10 w-10 rounded-lg" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-full" />
                <Skeleton className="h-3 w-5/6" />
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (variant === "compact") {
    return (
      <div className="space-y-0.5">
        {Array.from({ length: items }).map((_, i) => (
          <div key={i} className="flex items-center gap-3 rounded-lg px-3 py-2">
            <Skeleton className="h-3.5 w-3.5" variant="circular" />
            <Skeleton className="h-3.5 flex-1" />
            <Skeleton className="h-3 w-12" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="divide-y divide-border rounded-xl border border-border">
      {Array.from({ length: items }).map((_, i) => (
        <div key={i} className="flex items-start gap-4 px-6 py-4">
          <Skeleton className="mt-0.5 h-8 w-8 rounded-lg" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-72" />
            <div className="flex items-center gap-2">
              <Skeleton className="h-3.5 w-14 rounded-full" variant="text" />
              <Skeleton className="h-3 w-16" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
