import type React from "react";
import { cn } from "@/lib/utils";
import { motion } from "framer-motion";

interface SkeletonProps {
  className?: string;
  variant?: "text" | "circular" | "rectangular";
  width?: string | number;
  height?: string | number;
  style?: React.CSSProperties;
}

export function Skeleton({
  className,
  variant = "rectangular",
  width,
  height,
  style,
}: SkeletonProps) {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.2 }}
      className={cn(
        "relative isolate overflow-hidden bg-muted/60",
        variant === "circular" && "rounded-full",
        variant === "text" && "h-4 rounded-md",
        variant === "rectangular" && "rounded-lg",
        className,
      )}
      style={{ width, height, ...style }}
    >
      <div className="absolute inset-0 -translate-x-full animate-shimmer bg-gradient-to-r from-transparent via-muted-foreground/10 to-transparent" />
    </motion.div>
  );
}
