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

// ========================
// QR Auth Types (scan-to-log-in)
// ========================
export type QrAuthDeviceType = "desktop" | "android";
export type QrAuthStatus =
  | "pending"
  | "scanned"
  | "approved"
  | "rejected"
  | "expired"
  | "cancelled"
  | "consumed";

export interface QrAuthRequestResponse {
  request_code: string;
  device_type: QrAuthDeviceType;
  device_name: string;
  expires_at: string;
  ttl_seconds: number;
}

export interface QrAuthRequesterInfo {
  username: string;
  firstname: string;
  lastname: string;
  role_code: string;
  role_name: string;
}

export interface QrAuthScanResponse {
  request_code: string;
  device_type: QrAuthDeviceType;
  device_name: string;
  requester: QrAuthRequesterInfo | null;
  expires_at: string;
}

export interface QrAuthStatusResponse {
  request_code: string;
  device_type: QrAuthDeviceType;
  device_name: string;
  status: QrAuthStatus;
  expires_at: string;
  scanned_at: string | null;
  resolved_at: string | null;
  // Present only once when status === "approved" (one-time retrieval)
  access_token?: string;
  refresh_token?: string;
  user?: User;
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
  thumbnail: string | null;
  signature: string | null;
  signature_svg: string | null;
  status: string;
  created_at: string;
  updated_at: string;
  /** True when this personnel record is linked to an admin user account. */
  is_admin_profile?: boolean;
  /** True when a code secret is set (hash never exposed). */
  has_code_secret?: boolean;
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
// Correspondance Types
// ========================
export type CorrespondanceSens = "Entrant" | "Sortant";

// Statut is free-text; these are only suggested defaults.
export type CorrespondanceStatut = string;

export interface Correspondance {
  id: number;
  date_correspondance: string;
  heure_enregistrement: string;
  sens: CorrespondanceSens;
  reference: string;
  emetteur_destinataire: string;
  objet: string;
  statut: CorrespondanceStatut;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: CorrespondanceAttachment[];
  created_at: string;
  updated_at: string;
}

export interface CorrespondanceAttachment {
  id: number;
  correspondance_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Déclaration de perte Types
// ========================
export interface DeclarationPerte {
  id: number;
  date_declaration: string;
  heure_declaration: string;
  identite_declarant: string;
  nature_objet: string;
  description_objet: string;
  date_perte: string;
  lieu_perte: string;
  numero_attestation: string;
  nom_agent: string;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: DeclarationPerteAttachment[];
  created_at: string;
  updated_at: string;
}

export interface DeclarationPerteAttachment {
  id: number;
  declaration_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Passation Types (Sédentaire > Poste)
// ========================
export interface Passation {
  id: number;
  date_passation: string;
  heure_passation: string;
  chef_descendant_user_id: number | null;
  chef_descendant_grade: string | null;
  chef_descendant_lastname: string | null;
  chef_montant_user_id: number | null;
  chef_montant_grade: string | null;
  chef_montant_lastname: string | null;
  instructions_autorite: string | null;
  incidents_survenus: string | null;
  created_by: number | null;
  chef_descendant_username?: string | null;
  chef_montant_username?: string | null;
  attachments?: PassationAttachment[];
  created_at: string;
  updated_at: string;
}

export interface PassationAttachment {
  id: number;
  passation_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

/** Identity returned by POST /api/auth/verify (chef montant credential check). */
export interface VerifiedIdentity {
  id: number;
  username: string;
  grade: string | null;
  firstname: string | null;
  lastname: string | null;
}

// ========================
// Armement Types (Sédentaire > Poste)
// ========================
export interface Armement {
  id: number;
  date_perception: string;
  heure_perception: string;
  agent_preneur_personnel_id: number | null;
  agent_preneur_im: string | null;
  agent_preneur_grade: string | null;
  agent_preneur_nom: string | null;
  type_arme: string;
  matricule_arme: string;
  munitions: number | null;
  secteur_mission: string | null;
  etat_perception: string | null;
  /** Whether the agent preneur identity was verified via code secret. */
  agent_verifie: number;
  /** When the agent identity was verified (timestamp string). */
  agent_verifie_at: string | null;
  /** SVG vector data of the agent signature captured at perception. */
  signature_svg: string | null;
  heure_reintegration: string | null;
  etat_reintegration: string | null;
  munitions_consommees: number | null;
  created_by: number | null;
  agent_preneur_personnel_im?: string | null;
  attachments?: ArmementAttachment[];
  created_at: string;
  updated_at: string;
}

export interface ArmementAttachment {
  id: number;
  armement_id: number;
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
export type ComportementStatus = "pending" | "confirmed" | "rejected";

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
  status: ComportementStatus;
  confirmed_by: number | null;
  confirmed_at: string | null;
  rejected_reason: string | null;
  confirmed_by_username?: string | null;
  created_by: number | null;
  created_by_username?: string | null;
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
  link?: string | null;
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
