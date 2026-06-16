import { Skeleton } from "@/components/ui/skeleton";

interface FormSkeletonProps {
  fields?: number;
}

export function FormSkeleton({ fields = 4 }: FormSkeletonProps) {
  return (
    <div className="space-y-6">
      {Array.from({ length: fields }).map((_, i) => (
        <div key={i} className="space-y-2">
          <Skeleton className="h-3.5 w-24" />
          <Skeleton className="h-9 w-full rounded-lg" />
          {i < fields - 1 && <Skeleton className="h-3 w-40" />}
        </div>
      ))}
      <div className="flex items-center gap-3 pt-2">
        <Skeleton className="h-9 w-24 rounded-lg" />
        <Skeleton className="h-9 w-20 rounded-lg" />
      </div>
    </div>
  );
}

export function FormGroupSkeleton() {
  return (
    <div className="rounded-xl border border-border">
      <div className="border-b border-border p-4">
        <Skeleton className="h-5 w-32" />
        <Skeleton className="mt-1 h-3.5 w-48" />
      </div>
      <div className="space-y-4 p-4">
        {[1, 2, 3].map((i) => (
          <div key={i} className="flex items-center justify-between">
            <div className="space-y-1">
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-3 w-40" />
            </div>
            <Skeleton className="h-8 w-20 rounded-md" />
          </div>
        ))}
      </div>
    </div>
  );
}
