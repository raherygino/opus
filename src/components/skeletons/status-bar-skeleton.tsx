import { Skeleton } from "@/components/ui/skeleton";

export function StatusBarSkeleton() {
  return (
    <footer className="flex h-6 items-center justify-between border-t border-border bg-muted/50 px-4">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-2 w-2 rounded-full" variant="circular" />
          <Skeleton className="h-3 w-16" />
        </div>
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-3 w-3" />
          <Skeleton className="h-3 w-10" />
        </div>
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-3 w-3" />
          <Skeleton className="h-3 w-8" />
        </div>
      </div>
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-3 w-3" />
          <Skeleton className="h-3 w-8" />
        </div>
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-3 w-3" />
          <Skeleton className="h-3 w-12" />
        </div>
        <div className="flex items-center gap-1.5">
          <Skeleton className="h-2 w-2 rounded-full" variant="circular" />
          <Skeleton className="h-3 w-8" />
        </div>
      </div>
    </footer>
  );
}
