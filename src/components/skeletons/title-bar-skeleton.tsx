import { Skeleton } from "@/components/ui/skeleton";

export function TitleBarSkeleton() {
  return (
    <header className="flex h-10 items-center border-b border-border bg-background px-2">
      <div className="flex items-center gap-1">
        <Skeleton className="h-7 w-7 rounded-md" />
        <div className="ml-1 flex items-center gap-1">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-7 w-16 rounded-md" />
          ))}
        </div>
      </div>
      <div className="flex flex-1 items-center justify-center px-4">
        <div className="relative w-full max-w-sm">
          <Skeleton className="h-7 w-full rounded-md" />
        </div>
      </div>
      <div className="flex items-center gap-1 px-2">
        <Skeleton className="h-7 w-7 rounded-md" />
        <Skeleton className="h-7 w-7 rounded-md" />
        <Skeleton className="h-7 w-7 rounded-md" />
      </div>
    </header>
  );
}
