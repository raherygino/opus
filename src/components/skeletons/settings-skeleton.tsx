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
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0 },
};

export function SettingsSkeleton() {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      className="mx-auto flex max-w-5xl gap-8"
    >
      <motion.aside variants={item} className="w-48 shrink-0 space-y-1">
        {[1, 2, 3, 4, 5, 6, 7].map((i) => (
          <div key={i} className="flex items-center gap-2.5 rounded-lg px-3 py-2">
            <Skeleton className="h-4 w-4" />
            <Skeleton className="h-3.5 w-24" />
          </div>
        ))}
      </motion.aside>

      <motion.div variants={item} className="flex-1 space-y-6 pb-12">
        <div className="space-y-2">
          <Skeleton className="h-7 w-32" />
          <Skeleton className="h-4 w-56" />
        </div>

        <div className="rounded-xl border border-border">
          <div className="border-b border-border p-4">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="mt-1 h-3.5 w-48" />
          </div>
          <div className="p-4">
            <div className="flex gap-3">
              {[1, 2].map((i) => (
                <div
                  key={i}
                  className="flex flex-1 flex-col items-center gap-3 rounded-xl border-2 border-border p-4"
                >
                  <Skeleton className="h-6 w-6" />
                  <div className="space-y-1 text-center">
                    <Skeleton className="h-4 w-16" />
                    <Skeleton className="h-3 w-24" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-border">
          <div className="border-b border-border p-4">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="mt-1 h-3.5 w-48" />
          </div>
          <div className="p-4">
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-3 w-40" />
              </div>
              <Skeleton className="h-8 w-20 rounded-md" />
            </div>
          </div>
        </div>
      </motion.div>
    </motion.div>
  );
}
