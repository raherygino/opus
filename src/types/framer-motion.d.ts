declare module "framer-motion" {
  import { ComponentType, ReactNode, HTMLAttributes } from "react";

  interface MotionProps {
    initial?: unknown;
    animate?: unknown;
    exit?: unknown;
    variants?: unknown;
    transition?: unknown;
    whileHover?: unknown;
    whileTap?: unknown;
    layout?: boolean;
    layoutId?: string;
    className?: string;
    children?: ReactNode;
    onClick?: () => void;
    onMouseEnter?: () => void;
    style?: Record<string, unknown>;
    ref?: unknown;
    [key: string]: unknown;
  }

  type DivType = ComponentType<MotionProps & HTMLAttributes<HTMLDivElement>>;
  type AsideType = ComponentType<MotionProps & HTMLAttributes<HTMLElement>>;
  type ButtonType = ComponentType<MotionProps & HTMLAttributes<HTMLButtonElement>>;
  type SectionType = ComponentType<MotionProps & HTMLAttributes<HTMLElement>>;

  export const motion: {
    div: DivType;
    aside: AsideType;
    button: ButtonType;
    section: SectionType;
  };

  export const AnimatePresence: ComponentType<{
    children?: ReactNode;
    initial?: boolean;
    mode?: "wait" | "sync" | "popLayout";
  }>;
}
