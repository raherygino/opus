import { useNavigate, useLocation } from "react-router-dom";
import { useSidebarStore } from "@/stores/sidebar-store";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard,
  FileText,
  Settings,
  File,
} from "lucide-react";

const mainNavItems = [
  { icon: LayoutDashboard, label: "Dashboard", path: "/dashboard" },
  { icon: FileText, label: "Notes", path: "/notes" },
  { icon: Settings, label: "Settings", path: "/settings" },
];

const recentItems = [
  { icon: File, label: "Meeting Notes", path: "/notes/1" },
  { icon: File, label: "Project Plan", path: "/notes/2" },
  { icon: File, label: "Ideas & Brainstorm", path: "/notes/3" },
];

const tags = [
  { label: "work", color: "bg-blue-500" },
  { label: "personal", color: "bg-green-500" },
  { label: "ideas", color: "bg-purple-500" },
];

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isOpen, width } = useSidebarStore();

  return (
    <AnimatePresence initial={false}>
      {isOpen && (
        <motion.aside
          initial={{ width: 0, opacity: 0 }}
          animate={{ width, opacity: 1 }}
          exit={{ width: 0, opacity: 0 }}
          transition={{ duration: 0.2, ease: "easeInOut" }}
          className="flex-shrink-0 border-r border-border bg-sidebar overflow-hidden"
          style={{ width }}
        >
          <div className="flex h-full flex-col">
            <div className="flex h-12 items-center px-4 border-b border-sidebar-border">
              <div className="flex items-center gap-2.5">
                <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary">
                  <span className="text-[10px] font-bold text-primary-foreground">G</span>
                </div>
                <span className="text-sm font-semibold">Gescom</span>
              </div>
            </div>

            <ScrollArea className="flex-1 px-2 py-3">
              <div className="space-y-1">
                <p className="px-2 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Navigation
                </p>
                {mainNavItems.map((item) => {
                  const isActive = location.pathname === item.path ||
                    (item.path !== "/" && location.pathname.startsWith(item.path));
                  const Icon = item.icon;
                  return (
                    <Button
                      key={item.path}
                      variant="ghost"
                      className={cn(
                        "w-full justify-start gap-3 h-9 px-2 text-sm font-normal",
                        isActive
                          ? "bg-sidebar-accent text-sidebar-accent-foreground"
                          : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                      )}
                      onClick={() => navigate(item.path)}
                    >
                      <Icon className="h-4 w-4 shrink-0" />
                      {item.label}
                    </Button>
                  );
                })}
              </div>

              <Separator className="my-4" />

              <div className="space-y-1">
                <p className="px-2 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Recent
                </p>
                {recentItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <Button
                      key={item.path}
                      variant="ghost"
                      className="w-full justify-start gap-3 h-8 px-2 text-sm font-normal text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                      onClick={() => navigate(item.path)}
                    >
                      <Icon className="h-4 w-4 shrink-0 text-muted-foreground" />
                      <span className="truncate">{item.label}</span>
                    </Button>
                  );
                })}
              </div>

              <Separator className="my-4" />

              <div className="space-y-1">
                <p className="px-2 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Tags
                </p>
                {tags.map((tag) => (
                  <Button
                    key={tag.label}
                    variant="ghost"
                    className="w-full justify-start gap-3 h-8 px-2 text-sm font-normal text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                  >
                    <div className={cn("h-2 w-2 rounded-full", tag.color)} />
                    {tag.label}
                  </Button>
                ))}
              </div>
            </ScrollArea>

            <div className="border-t border-sidebar-border p-3">
              <div className="flex items-center gap-2 rounded-lg bg-sidebar-accent/50 px-3 py-2">
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/20 text-[10px] font-medium text-primary">
                  JD
                </div>
                <div className="flex flex-col">
                  <span className="text-xs font-medium">John Doe</span>
                  <span className="text-[10px] text-muted-foreground">Free plan</span>
                </div>
              </div>
            </div>
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
