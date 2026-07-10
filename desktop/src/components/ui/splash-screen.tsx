import { motion } from "framer-motion";
import { useEffect, useState } from "react";
import logoSrc from "@/assets/img/logo-opus.png";

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  const [visible, setVisible] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => {
      setVisible(false);
      setTimeout(onComplete, 400);
    }, 1200);
    return () => clearTimeout(timer);
  }, [onComplete]);

  return (
    <motion.div
      initial={{ opacity: 1 }}
      animate={{ opacity: visible ? 1 : 0 }}
      transition={{ duration: 0.4, ease: "easeInOut" }}
      className="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-background"
    >
      <motion.div
        initial={{ scale: 0.8, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="flex flex-col items-center gap-6"
      >
        <div className="flex items-center justify-center overflow-hidden">
          <img src={logoSrc} alt="OPUS" className="h-full w-full object-contain p-2" />
        </div>
        <div className="flex flex-col items-center gap-1">
          <h1 className="text-xl font-semibold tracking-tight">OPUS</h1>
          <p className="text-xs text-muted-foreground">Opérations Policières Unifiées et Structurées</p>
        </div>
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: 160 }}
          transition={{ duration: 0.8, ease: "easeInOut", delay: 0.3 }}
          className="h-1 rounded-full bg-muted overflow-hidden"
        >
          <motion.div
            initial={{ x: "-100%" }}
            animate={{ x: "100%" }}
            transition={{ duration: 0.8, ease: "easeInOut", repeat: 1 }}
            className="h-full w-full rounded-full bg-primary"
          />
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
