import { Skeleton } from "@/components/ui/skeleton";

export function SidebarSkeleton() {
  return (
    <aside className="flex h-full w-60 flex-shrink-0 flex-col border-r border-border bg-sidebar">
      <div className="flex h-12 items-center gap-2.5 border-b border-sidebar-border px-4">
        <Skeleton className="h-6 w-6 rounded-md" />
        <Skeleton className="h-4 w-20" />
      </div>
      <div className="flex-1 space-y-1 px-2 py-3">
        <Skeleton className="mb-2 ml-2 h-3 w-20" variant="text" />
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-center gap-3 px-2 py-2">
            <Skeleton className="h-4 w-4" variant="circular" />
            <Skeleton className="h-3.5 flex-1" />
          </div>
        ))}
        <div className="my-4 h-px bg-border" />
        <Skeleton className="mb-2 ml-2 h-3 w-14" variant="text" />
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-center gap-3 px-2 py-1.5">
            <Skeleton className="h-4 w-4" variant="circular" />
            <Skeleton className="h-3.5 flex-1" />
          </div>
        ))}
        <div className="my-4 h-px bg-border" />
        <Skeleton className="mb-2 ml-2 h-3 w-12" variant="text" />
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-center gap-3 px-2 py-1.5">
            <Skeleton className="h-2 w-2 rounded-full" />
            <Skeleton className="h-3.5 w-16" />
          </div>
        ))}
      </div>
      <div className="border-t border-sidebar-border p-3">
        <div className="flex items-center gap-2 rounded-lg bg-sidebar-accent/50 px-3 py-2">
          <Skeleton className="h-6 w-6 rounded-full" variant="circular" />
          <div className="flex flex-col gap-1">
            <Skeleton className="h-3 w-16" />
            <Skeleton className="h-2 w-12" />
          </div>
        </div>
      </div>
    </aside>
  );
}
