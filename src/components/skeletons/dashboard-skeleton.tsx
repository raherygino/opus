import { motion } from "framer-motion";
import { Skeleton } from "@/components/ui/skeleton";

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
};

const item = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0 },
};

export function DashboardSkeleton() {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      className="mx-auto max-w-5xl space-y-8"
    >
      <motion.div variants={item}>
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <Skeleton className="h-7 w-48" />
            <Skeleton className="h-4 w-64" />
          </div>
          <Skeleton className="h-9 w-28 rounded-lg" />
        </div>
      </motion.div>

      <motion.div
        variants={item}
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"
      >
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="relative overflow-hidden rounded-xl border border-border p-4"
          >
            <div className="flex items-center justify-between pb-2">
              <Skeleton className="h-3.5 w-24" />
              <Skeleton className="h-4 w-4" />
            </div>
            <div className="flex items-baseline gap-2">
              <Skeleton className="h-8 w-12" />
              <Skeleton className="h-3 w-20" />
            </div>
          </div>
        ))}
      </motion.div>

      <motion.div variants={item}>
        <div className="rounded-xl border border-border">
          <div className="border-b border-border p-4">
            <Skeleton className="h-5 w-28" />
          </div>
          <div className="divide-y divide-border">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="flex items-start gap-4 px-6 py-4"
              >
                <Skeleton className="h-9 w-9 rounded-lg" />
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-4 w-44" />
                    <Skeleton className="h-3.5 w-3.5" />
                  </div>
                  <Skeleton className="h-3 w-64" />
                  <div className="flex items-center gap-2">
                    <Skeleton className="h-4 w-12 rounded-full" variant="text" />
                    <Skeleton className="h-4 w-16 rounded-full" variant="text" />
                    <Skeleton className="h-3 ml-auto w-16" />
                  </div>
                </div>
                <Skeleton className="h-8 w-8 rounded-md" />
              </div>
            ))}
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
