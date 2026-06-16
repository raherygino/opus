import { Skeleton } from "@/components/ui/skeleton";

export function EditorSkeleton() {
  return (
    <div className="flex h-full flex-col">
      <div className="flex items-center justify-between border-b border-border px-6 py-3">
        <div className="flex items-center gap-2">
          <Skeleton className="h-8 w-8 rounded-md" />
          <Skeleton className="h-8 w-8 rounded-md" />
        </div>
        <div className="flex items-center gap-2">
          <Skeleton className="h-3.5 w-28" />
          <Skeleton className="h-8 w-8 rounded-md" />
          <Skeleton className="h-8 w-8 rounded-md" />
          <Skeleton className="h-8 w-8 rounded-md" />
        </div>
      </div>
      <div className="flex-1 overflow-auto px-6 py-4">
        <Skeleton className="mb-4 h-8 w-3/4" />
        <div className="mb-6 flex items-center gap-2">
          <Skeleton className="h-5 w-12 rounded-full" variant="text" />
          <Skeleton className="h-5 w-14 rounded-full" variant="text" />
          <Skeleton className="h-5 w-16 rounded-full" variant="text" />
        </div>
        <div className="space-y-3">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-4/5" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-11/12" />
        </div>
      </div>
    </div>
  );
}
