import { motion } from "framer-motion";
import { Construction } from "lucide-react";

export function ComingSoon() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex flex-col items-center justify-center h-full min-h-[60vh] gap-4 text-center"
    >
      <div className="flex items-center justify-center h-20 w-20 rounded-2xl bg-muted">
        <Construction className="h-10 w-10 text-muted-foreground" />
      </div>
      <h1 className="text-2xl font-semibold tracking-tight">Page en construction</h1>
      <p className="text-sm text-muted-foreground max-w-md">
        Cette fonctionnalité sera disponible prochainement.
      </p>
    </motion.div>
  );
}
