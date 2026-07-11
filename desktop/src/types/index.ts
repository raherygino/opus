// ========================
// Auth Types
// ========================
export interface User {
  id: number;
  personnel_id: number;
  username: string;
  role_id: number;
  role_code: RoleCode;
  role_name: string;
  is_active: number;
  last_login: string | null;
  created_at: string;
  updated_at: string;
  im: string;
  lastname: string;
  firstname: string;
  grade: string;
  affectation: string | null;
  phone: string | null;
  photo: string | null;
  signature: string | null;
  address: string | null;
  personnel_status: string | null;
  permissions?: RolePermission[];
}

export type RoleCode =
  | "SUPER_ADMIN"
  | "CHIEF"
  | "STATION_ADMIN"
  | "HEAD_SG"
  | "HEAD_SED"
  | "HEAD_PJ"
  | "INVESTIGATOR"
  | "OFFICER"
  | "RECEPTION"
  | "CLERK"
  | "CUSTODY";

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  user: User;
}

export interface Division {
  code: string;
  label: string;
  description: string;
}

// ========================
// Personnel Types
// ========================
export interface Personnel {
  id: number;
  im: string;
  grade: string;
  lastname: string;
  firstname: string;
  affectation: string | null;
  phone: string | null;
  address: string | null;
  photo: string | null;
  signature: string | null;
  status: string;
  created_at: string;
  updated_at: string;
}

export interface PersonnelAttachment {
  id: number;
  personnel_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Mouvement Types
// ========================
export interface Mouvement {
  id: number;
  personnel_id: number;
  im: string;
  grade: string | null;
  service: string | null;
  nom: string | null;
  prenoms: string | null;
  type_mouvement: string;
  date_depart: string | null;
  days: number | null;
  date_retour: string | null;
  retour: "Oui" | "Non";
  created_at: string;
  updated_at: string;
}

export interface MouvementAttachment {
  id: number;
  mouvement_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Comportement Types
// ========================
export interface Comportement {
  id: number;
  personnel_id: number;
  im: string;
  grade: string | null;
  service: string | null;
  nom: string | null;
  prenoms: string | null;
  type: "Positive" | "Negative";
  date_comportement: string;
  motif: string;
  decision: string | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Role Types
// ========================
export interface Role {
  id: number;
  code: string;
  name: string;
  description: string | null;
  permissions?: RolePermission[];
}

export interface RolePermission {
  id: number;
  role_id: number;
  module: string;
  can_view: number;
  can_create: number;
  can_edit: number;
  can_delete: number;
  can_export: number;
}

// ========================
// API Response Types
// ========================
export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data: T;
}

export interface ApiError {
  success: boolean;
  message: string;
  errors?: Record<string, string>;
}

// ========================
// Navigation Types
// ========================
export interface NavItem {
  icon: string;
  label: string;
  path: string;
}

export interface NavSection {
  title: string;
  items: NavItem[];
}

// ========================
// Legacy Types (keep for compatibility)
// ========================
export interface Note {
  id: string;
  title: string;
  content: string;
  createdAt: Date;
  updatedAt: Date;
  tags: string[];
  pinned: boolean;
}

export interface RecentFile {
  id: string;
  name: string;
  path: string;
  lastOpened: Date;
}

export interface Command {
  id: string;
  label: string;
  description?: string;
  shortcut?: string;
  icon?: string;
  action: () => void;
}

export type Theme = "dark" | "light";
export type BuiltInThemeId = "dark" | "light" | "high-contrast" | "ondark" | "matrix" | "monokai" | "clean-light" | "warm-light";
export type ThemeId = BuiltInThemeId | `custom-${string}`;

export interface ThemeInfo {
  id: ThemeId;
  name: string;
  type: "light" | "dark";
  colors: Record<string, string>;
}

export interface CustomTheme {
  id: `custom-${string}`;
  name: string;
  type: "light" | "dark";
  colors: Record<string, string>;
}

export const COLOR_TOKENS: { key: string; label: string; category: string }[] = [
  { key: "--background", label: "Background", category: "Base" },
  { key: "--foreground", label: "Foreground", category: "Base" },
  { key: "--card", label: "Card", category: "Surface" },
  { key: "--card-foreground", label: "Card Foreground", category: "Surface" },
  { key: "--popover", label: "Popover", category: "Surface" },
  { key: "--popover-foreground", label: "Popover Foreground", category: "Surface" },
  { key: "--primary", label: "Primary", category: "Accent" },
  { key: "--primary-foreground", label: "Primary Foreground", category: "Accent" },
  { key: "--secondary", label: "Secondary", category: "Accent" },
  { key: "--secondary-foreground", label: "Secondary Foreground", category: "Accent" },
  { key: "--accent", label: "Accent", category: "Accent" },
  { key: "--accent-foreground", label: "Accent Foreground", category: "Accent" },
  { key: "--muted", label: "Muted", category: "Surface" },
  { key: "--muted-foreground", label: "Muted Foreground", category: "Surface" },
  { key: "--border", label: "Border", category: "Base" },
  { key: "--input", label: "Input", category: "Base" },
  { key: "--ring", label: "Focus Ring", category: "Accent" },
  { key: "--sidebar", label: "Sidebar", category: "Layout" },
  { key: "--sidebar-foreground", label: "Sidebar Foreground", category: "Layout" },
  { key: "--sidebar-border", label: "Sidebar Border", category: "Layout" },
  { key: "--sidebar-accent", label: "Sidebar Accent", category: "Layout" },
  { key: "--sidebar-accent-foreground", label: "Sidebar Accent Foreground", category: "Layout" },
  { key: "--titlebar", label: "Title Bar", category: "Layout" },
  { key: "--titlebar-foreground", label: "Title Bar Foreground", category: "Layout" },
  { key: "--statusbar", label: "Status Bar", category: "Layout" },
  { key: "--statusbar-foreground", label: "Status Bar Foreground", category: "Layout" },
];

export type NotificationType = "success" | "error" | "info" | "warning";

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message?: string;
  duration?: number;
}

// ========================
// App Notification Types (server-side persistent notifications)
// ========================
export interface AppNotification {
  id: number;
  title: string;
  message: string | null;
  type: NotificationType;
  service: string;
  user_id: number | null;
  personnel_id: number | null;
  created_by: number | null;
  is_read: number;
  created_at: string;
  updated_at: string;
  personnel_im?: string | null;
  personnel_nom?: string | null;
  personnel_prenoms?: string | null;
  personnel_grade?: string | null;
  created_by_username?: string | null;
}

// ========================
// Audit Log Types
// ========================
export interface AuditLog {
  id: number;
  user_id: number | null;
  action: string;
  module: string;
  entity_id: number | null;
  description: string | null;
  old_values: string | null;
  new_values: string | null;
  ip_address: string | null;
  user_agent: string | null;
  created_at: string;
  username?: string | null;
  prenoms?: string | null;
  nom?: string | null;
}

export interface AuditLogFilters {
  action?: string;
  module?: string;
  user_id?: string;
  search?: string;
  date_from?: string;
  date_to?: string;
}

export interface AppSettings {
  theme: ThemeId;
  sidebarOpen: boolean;
  sidebarWidth: number;
  fontSize: number;
  showStatusBar: boolean;
}
