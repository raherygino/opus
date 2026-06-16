import { useState, type ReactNode } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { SplashScreen } from "@/components/ui/splash-screen";

interface AppBootProps {
  children: ReactNode;
}

export function AppBoot({ children }: AppBootProps) {
  const [booted, setBooted] = useState(false);

  return (
    <>
      <AnimatePresence mode="wait">
        {!booted ? (
          <SplashScreen key="splash" onComplete={() => setBooted(true)} />
        ) : (
          <motion.div
            key="app"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.3, ease: "easeInOut" }}
            className="h-full w-full"
          >
            {children}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
