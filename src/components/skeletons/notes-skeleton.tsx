import { Skeleton } from "@/components/ui/skeleton";

export function NotesSkeleton() {
  return (
    <div className="flex h-full -m-6">
      <div className="flex w-72 flex-shrink-0 flex-col border-r border-border bg-sidebar">
        <div className="flex items-center justify-between p-4 pb-2">
          <Skeleton className="h-4 w-14" />
          <Skeleton className="h-7 w-16 rounded-md" />
        </div>
        <div className="px-4 pb-3">
          <Skeleton className="h-8 w-full rounded-md" />
        </div>
        <div className="flex-1 space-y-0.5 px-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="rounded-lg px-3 py-2">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0 flex-1">
                  <Skeleton className="h-3.5 w-3.5 shrink-0" />
                  <Skeleton className="h-3.5 flex-1" />
                </div>
                <Skeleton className="h-3 w-3 shrink-0" />
              </div>
              <Skeleton className="mt-1.5 h-3 w-3/4" />
              <div className="mt-2 flex items-center gap-1.5">
                <Skeleton className="h-3.5 w-10 rounded-full" variant="text" />
                <Skeleton className="h-3 w-8 ml-auto" />
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="flex flex-1 items-center justify-center">
        <div className="text-center">
          <Skeleton className="mx-auto h-12 w-12 rounded-xl" />
          <Skeleton className="mx-auto mt-4 h-4 w-36" />
          <Skeleton className="mx-auto mt-2 h-3 w-52" />
          <Skeleton className="mx-auto mt-4 h-8 w-28 rounded-md" />
        </div>
      </div>
    </div>
  );
}
