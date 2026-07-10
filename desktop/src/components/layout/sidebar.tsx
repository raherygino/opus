import { useState, useMemo, useEffect, useCallback } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "@/stores/auth-store";
import { useSidebarStore } from "@/stores/sidebar-store";
import { Button } from "@/components/ui/button";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";
import { hasPermission } from "@/lib/permissions";
import { getUnreadCount } from "@/lib/api/notifications";
import { motion, AnimatePresence } from "framer-motion";
import logoSrc from "@/assets/img/logo-opus.png";
import type { User } from "@/types";
import {
  LayoutDashboard,
  Users,
  UserCog,
  LogOut,
  Building2,
  Car,
  Scale,
  Settings,
  Map as MapIcon,
  UserCircle,
  ShieldCheck,
  ChevronRight,
  FileText,
  Handshake,
  ShieldEllipsis,
  Box,
  Columns4,
  BookOpenText,
  MessageCircleMore,
  ScrollText,
  FileSearchIcon,
  FileInput,
  File as FileIcon,
  ShieldBan,
  FileUp,
  UserRoundSearch,
  Gavel,
  NotebookText,
  Repeat,
  SquareMenu,
  Hotel,
  ReceiptText,
  Info,
  Grid2X2,
  FileQuestion,
  ShieldHalf,
  FileBox,
  MessageSquareText,
  MessageSquareMore,
  BellRing,
} from "lucide-react";

interface NavItem {
  icon: React.ElementType;
  label: string;
  path?: string;
  children?: NavItem[];
  module?: string;
}

const DIVISION_MODULES: Record<string, string[]> = {
  sedentaire: [
    "sedentaire_secretariat_correspondance",
    "sedentaire_secretariat_declaration_perte",
    "sedentaire_secretariat_rapport",
    "sedentaire_secretariat_main_courante",
    "sedentaire_poste_passation",
    "sedentaire_poste_armement",
    "sedentaire_poste_materiels",
    "sedentaire_poste_situation_gav",
    "sedentaire_poste_main_courante",
    "sedentaire_poste_renseignement",
    "personnel",
  ],
  sg: [
    "sg_spa",
    "sg_info_rassemblement",
    "sg_repartition",
    "sg_patrouille",
    "sg_intervention",
    "sg_dispositif_exceptionnel",
    "sg_instruction_autorite",
    "sg_compte_rendu",
    "sg_recherche",
    "sg_renseignement",
  ],
  pj: [
    "pj_plainte",
    "pj_enquete",
    "pj_mandat",
    "pj_convocation",
    "pj_arrestation",
    "pj_gav",
    "pj_requisition",
    "pj_personne_recherchee",
    "pj_objets",
    "pj_deferrement",
    "pj_renseignement",
  ],
};

function userCanAccessDivision(user: User, division: string): boolean {
  const modules = DIVISION_MODULES[division];
  if (!modules) return false;
  return modules.some((m) => hasPermission(user, m, "can_view"));
}

function hasVisibleNavItem(user: User, item: NavItem): boolean {
  if (item.path) {
    if (!item.module) return true;
    if (hasPermission(user, item.module, "can_view")) return true;
  }
  if (item.children) {
    return item.children.some((c) => hasVisibleNavItem(user, c));
  }
  return false;
}

function buildNavItems(user: User): NavItem[] {
  const items: NavItem[] = [];

  // Main dashboard – SUPER_ADMIN only
  if (user.role_code === "SUPER_ADMIN") {
    items.push({ icon: LayoutDashboard, label: "Dashboard", path: "/dashboard" });
  }

  const accessibleDivisions = Object.keys(DIVISION_MODULES).filter((d) =>
    userCanAccessDivision(user, d)
  );
  const flattenDivisions = accessibleDivisions.length === 1;

  // Sédentaire
  if (accessibleDivisions.includes("sedentaire")) {
    const secretariatChildren: NavItem[] = [
      { icon: Repeat, label: "Correspondance", path: "/sedentaire/secretariat/correspondance", module: "sedentaire_secretariat_correspondance" },
      { icon: Users, label: "Gestion du personnel", path: "/personnel", module: "personnel" },
      { icon: FileQuestion, label: "Déclaration de perte", path: "/sedentaire/secretariat/declaration-perte", module: "sedentaire_secretariat_declaration_perte" },
      { icon: SquareMenu, label: "Rapport", path: "/sedentaire/secretariat/rapport", module: "sedentaire_secretariat_rapport" },
      { icon: BookOpenText, label: "Main courante", path: "/sedentaire/secretariat/main-courante", module: "sedentaire_secretariat_main_courante" },
    ].filter((c) => hasVisibleNavItem(user, c));

    const posteChildren: NavItem[] = [
      { icon: Handshake, label: "Passation", path: "/sedentaire/poste/passation", module: "sedentaire_poste_passation" },
      { icon: ShieldEllipsis, label: "Armement", path: "/sedentaire/poste/armement", module: "sedentaire_poste_armement" },
      { icon: Box, label: "Matériels", path: "/sedentaire/poste/materiels", module: "sedentaire_poste_materiels" },
      { icon: Columns4, label: "Situation GAV", path: "/sedentaire/poste/situation-gav", module: "sedentaire_poste_situation_gav" },
      { icon: BookOpenText, label: "Main courante", path: "/sedentaire/poste/main-courante", module: "sedentaire_poste_main_courante" },
      { icon: MessageCircleMore, label: "Renseignement", path: "/sedentaire/poste/envoi-renseignement", module: "sedentaire_poste_renseignement" },
    ].filter((c) => hasVisibleNavItem(user, c));

    const hasSecretariat = secretariatChildren.length > 0;
    const hasPoste = posteChildren.length > 0;
    const showBoth = hasSecretariat && hasPoste;

    if (showBoth) {
      // Both sub-sections → nest under Sédentaire
      const children: NavItem[] = [];
      children.push({ icon: LayoutDashboard, label: "Dashboard Sédentaire", path: "/sedentaire/dashboard" });
      if (hasSecretariat) {
        children.push({ icon: NotebookText, label: "Secrétariat", children: secretariatChildren });
      }
      if (hasPoste) {
        children.push({ icon: Hotel, label: "Poste", children: posteChildren });
      }
      items.push({ icon: Building2, label: "Sédentaire", children });
    } else {
      // Only one sub-section → promote to top level
      if (hasSecretariat) {
        items.push({ icon: LayoutDashboard, label: "Dashboard Sédentaire", path: "/sedentaire/dashboard" });
        items.push({ icon: FileText, label: "Secrétariat", children: secretariatChildren });
      }
      if (hasPoste) {
        items.push({ icon: LayoutDashboard, label: "Dashboard Sédentaire", path: "/sedentaire/dashboard" });
        items.push({ icon: FileText, label: "Poste", children: posteChildren });
      }
    }
  }

  // Service Général
  if (accessibleDivisions.includes("sg")) {
    const sgChildren: NavItem[] = [
      { icon: ReceiptText, label: "SPA", path: "/sg/spa", module: "sg_spa" },
      { icon: Info, label: "Info rassemblement", path: "/sg/information-rassemblement", module: "sg_info_rassemblement" },
      { icon: Grid2X2, label: "Répartition", path: "/sg/repartition", module: "sg_repartition" },
      { icon: ShieldEllipsis, label: "Patrouille", path: "/sg/patrouille", module: "sg_patrouille" },
      { icon: ShieldHalf, label: "Intervention", path: "/sg/intervention", module: "sg_intervention" },
      { icon: FileBox, label: "Dispositif exceptionnel", path: "/sg/dispositif-exceptionnel", module: "sg_dispositif_exceptionnel" },
      { icon: MessageCircleMore, label: "Instruction autorité", path: "/sg/instruction-autorite", module: "sg_instruction_autorite" },
      { icon: MessageSquareText, label: "Compte rendu", path: "/sg/compte-rendu", module: "sg_compte_rendu" },
      { icon: MessageSquareMore, label: "Renseignement", path: "/sg/renseignement", module: "sg_renseignement" },
    ].filter((c) => hasVisibleNavItem(user, c));

    if (flattenDivisions) {
      items.push({ icon: LayoutDashboard, label: "Dashboard SG", path: "/sg/dashboard" });
      items.push(...sgChildren);
    } else {
      items.push({
        icon: Car,
        label: "Division service général",
        children: [
          { icon: LayoutDashboard, label: "Dashboard", path: "/sg/dashboard" },
          ...sgChildren,
        ],
      });
    }
  }

  // Police Judiciaire
  if (accessibleDivisions.includes("pj")) {
    const pjChildren: NavItem[] = [
      { icon: ScrollText, label: "Plainte", path: "/pj/plainte", module: "pj_plainte" },
      { icon: FileSearchIcon, label: "Registre d'enquête", path: "/pj/registre-enquete", module: "pj_enquete" },
      { icon: FileIcon, label: "Mandat", path: "/pj/mandat", module: "pj_mandat" },
      { icon: FileInput, label: "Convocation", path: "/pj/convocation", module: "pj_convocation" },
      { icon: ShieldBan, label: "Arrestation", path: "/pj/arrestation", module: "pj_arrestation" },
      { icon: Columns4, label: "GAV", path: "/pj/gav", module: "pj_gav" },
      { icon: FileUp, label: "Réquisition", path: "/pj/requisition", module: "pj_requisition" },
      { icon: UserRoundSearch, label: "Personne recherchée", path: "/pj/personne-recherchee", module: "pj_personne_recherchee" },
      { icon: Box, label: "Objets", path: "/pj/objets", module: "pj_objets" },
      { icon: Gavel, label: "Registre de déferrement", path: "/pj/registre-deferrement", module: "pj_deferrement" },
      { icon: MessageCircleMore, label: "Renseignement", path: "/pj/renseignement", module: "pj_renseignement" },
    ].filter((c) => hasVisibleNavItem(user, c));

    if (flattenDivisions) {
      items.push({ icon: LayoutDashboard, label: "Dashboard PJ", path: "/pj/dashboard" });
      items.push(...pjChildren);
    } else {
      items.push({
        icon: Scale,
        label: "Division PJ",
        children: [
          { icon: LayoutDashboard, label: "Dashboard", path: "/pj/dashboard" },
          ...pjChildren,
        ],
      });
    }
  }

  // Global modules
  if (hasPermission(user, "cartographie", "can_view")) {
    items.push({ icon: MapIcon, label: "Cartographie", path: "/cartographie" });
  }
  if (hasPermission(user, "users", "can_view")) {
    items.push({ icon: UserCog, label: "Utilisateurs", path: "/users" });
  }
  if (hasPermission(user, "roles", "can_view")) {
    items.push({ icon: ShieldCheck, label: "Rôles", path: "/roles" });
  }

  // Audit logs — SUPER_ADMIN only
  if (user.role_code === "SUPER_ADMIN") {
    items.push({ icon: ScrollText, label: "Journal d'audit", path: "/audit-logs" });
  }

  return items;
}

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { isOpen, width } = useSidebarStore();
  const { user, logout } = useAuthStore();
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchUnread = useCallback(async () => {
    try {
      const count = await getUnreadCount();
      setUnreadCount(count);
    } catch {
      // silently ignore
    }
  }, []);

  useEffect(() => {
    fetchUnread();
    const interval = setInterval(fetchUnread, 30000);
    return () => clearInterval(interval);
  }, [fetchUnread]);

  useEffect(() => {
    if (location.pathname === "/notifications") {
      fetchUnread();
    }
  }, [location.pathname, fetchUnread]);

  const navItems = useMemo(() => {
    if (!user) return [];
    if (user.role_code === "SUPER_ADMIN") {
      return buildNavItems(user);
    }
    return buildNavItems(user);
  }, [user]);

  function toggleExpand(label: string) {
    setExpandedItems((prev) => {
      const next = new Set(prev);
      if (next.has(label)) {
        next.delete(label);
      } else {
        next.add(label);
      }
      return next;
    });
  }

  function handleLogout() {
    logout();
    navigate("/login");
  }

  const userInitials = user
    ? `${user.firstname.charAt(0)}${user.lastname.charAt(0)}`
    : "??";

  function renderNavItem(item: NavItem, depth = 0) {
    const hasChildren = item.children && item.children.length > 0;
    const isExpanded = expandedItems.has(item.label);
    const isActive = item.path
      ? location.pathname === item.path ||
        (item.path !== "/" && location.pathname.startsWith(item.path))
      : false;
    const Icon = item.icon;

    return (
      <div key={item.label}>
        <Button
          variant="ghost"
          className={cn(
            "w-full justify-start gap-3 h-9 px-2 text-sm font-normal",
            isActive
              ? "bg-sidebar-accent text-sidebar-accent-foreground"
              : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
          )}
          style={{ paddingLeft: depth > 0 ? `${12 + depth * 12}px` : undefined }}
          onClick={() => {
            if (item.path) navigate(item.path);
            if (hasChildren) toggleExpand(item.label);
          }}
        >
          <Icon className="h-4 w-4 shrink-0" />
          <span className="flex-1 text-left truncate">{item.label}</span>
          {hasChildren && (
            <ChevronRight
              className={cn(
                "h-4 w-4 shrink-0 transition-transform duration-200",
                isExpanded && "rotate-90",
              )}
            />
          )}
        </Button>
        {hasChildren && (
          <AnimatePresence initial={false}>
            {isExpanded && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ duration: 0.2, ease: "easeInOut" }}
                className="overflow-hidden"
              >
                <div className="border-l border-sidebar-border pl-2 space-y-1 mt-1">
                  {item.children!.map((child) => renderNavItem(child, depth + 1))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        )}
      </div>
    );
  }

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
            <div className="flex items-center justify-center px-4 border-b border-sidebar-border">
              <div className="flex flex-col items-center gap-1 py-2">
                <div className="flex items-center justify-center rounded-md overflow-hidden">
                  <img src={logoSrc} alt="OPUS" style={{ width: "150px" }} className="h-full w-full object-contain" />
                </div>
                <span className="text-sm font-semibold tracking-tight">OPUS</span>
              </div>
              
            </div>

            <ScrollArea className="flex-1 px-2 py-3">
              <div className="space-y-1">
                <p className="px-2 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Navigation
                </p>
                {navItems.map((item) => renderNavItem(item))}
              </div>

              <Separator className="my-4" />

              <div className="space-y-1">
                <p className="px-2 text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                  Système
                </p>
                <Button
                  variant="ghost"
                  className={cn(
                    "w-full justify-start gap-3 h-9 px-2 text-sm font-normal",
                    location.pathname === "/notifications"
                      ? "bg-sidebar-accent text-sidebar-accent-foreground"
                      : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                  )}
                  onClick={() => navigate("/notifications")}
                >
                  <BellRing className="h-4 w-4 shrink-0" />
                  <span className="flex-1 text-left truncate">Notifications</span>
                  {unreadCount > 0 && (
                    <span className="ml-auto inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 text-[10px] font-semibold rounded-full bg-primary text-primary-foreground">
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </Button>
                <Button
                  variant="ghost"
                  className={cn(
                    "w-full justify-start gap-3 h-9 px-2 text-sm font-normal",
                    location.pathname === "/profile"
                      ? "bg-sidebar-accent text-sidebar-accent-foreground"
                      : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                  )}
                  onClick={() => navigate("/profile")}
                >
                  <UserCircle className="h-4 w-4 shrink-0" />
                  Profil
                </Button>
                <Button
                  variant="ghost"
                  className={cn(
                    "w-full justify-start gap-3 h-9 px-2 text-sm font-normal",
                    location.pathname === "/settings"
                      ? "bg-sidebar-accent text-sidebar-accent-foreground"
                      : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                  )}
                  onClick={() => navigate("/settings")}
                >
                  <Settings className="h-4 w-4 shrink-0" />
                  Paramètres
                </Button>
              </div>
            </ScrollArea>

            <div className="border-t border-sidebar-border p-3 space-y-2">
              <div className="flex items-center gap-2 rounded-lg bg-sidebar-accent/50 px-3 py-2">
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/20 text-[10px] font-medium text-primary shrink-0">
                  {userInitials}
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-xs font-medium truncate">
                    {user?.firstname} {user?.lastname}
                  </span>
                  <span className="text-[10px] text-muted-foreground truncate">
                    {user?.role_name}
                  </span>
                </div>
              </div>
              <Button
                variant="ghost"
                className="w-full justify-start gap-2 h-8 text-xs text-muted-foreground hover:text-destructive"
                onClick={handleLogout}
              >
                <LogOut className="h-3.5 w-3.5" />
                Déconnexion
              </Button>
            </div>
          </div>
        </motion.aside>
      )}
    </AnimatePresence>
  );
}
