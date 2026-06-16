import { Skeleton } from "@/components/ui/skeleton";

export function ActivityPanelSkeleton() {
  return (
    <div className="flex h-full flex-col border-l border-border">
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-6 w-6 rounded-md" />
      </div>
      <div className="flex-1 space-y-3 p-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="flex items-start gap-3">
            <Skeleton className="h-6 w-6 rounded-full" variant="circular" />
            <div className="flex-1 space-y-1">
              <Skeleton className="h-3.5 w-3/4" />
              <Skeleton className="h-3 w-1/2" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function NotificationSkeleton() {
  return (
    <div className="space-y-2">
      {[1, 2, 3].map((i) => (
        <div
          key={i}
          className="flex items-start gap-3 rounded-lg border border-border p-3"
        >
          <Skeleton className="h-6 w-6 rounded-full" variant="circular" />
          <div className="flex-1 space-y-1">
            <Skeleton className="h-3.5 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
          <Skeleton className="h-3 w-12" />
        </div>
      ))}
    </div>
  );
}
