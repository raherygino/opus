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
  /** FK to the exact arme perceived (nullable for legacy records). */
  arme_id: number | null;
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
  /** GPS latitude captured at perception time (mobile only, null on desktop). */
  latitude: string | null;
  /** GPS longitude captured at perception time (mobile only, null on desktop). */
  longitude: string | null;
  heure_reintegration: string | null;
  date_reintegration: string | null;
  etat_reintegration: string | null;
  munitions_consommees: number | null;
  /** GPS latitude captured at reintegration (mobile only, null on desktop). */
  reintegration_latitude: string | null;
  /** GPS longitude captured at reintegration (mobile only, null on desktop). */
  reintegration_longitude: string | null;
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
// TypeArme & Arme Types (weapon catalog + individual weapon instances)
// ========================

/** Weapon type/category (e.g. "Pistolet PA 9mm", "Fusil AK-47").
 *  Munitions stock is managed at this level because all weapons of the
 *  same type share the same caliber/munition pool. */
export interface TypeArme {
  id: number;
  nom: string;
  description: string | null;
  /** Shared ammunition stock for all weapons of this type. */
  munitions_stock: number;
  created_at: string;
  updated_at: string;
}

/** Individual physical weapon identified by its unique matricule. */
export interface Arme {
  id: number;
  type_arme_id: number;
  /** Joined type name from the API. */
  type_arme_nom: string | null;
  matricule: string;
  /** Legacy per-weapon stock (kept for backward compat, no longer the
   *  active stock — use type_arme_munitions_stock instead). */
  munitions_stock: number;
  /** Joined from type_arme — the active shared stock for this weapon's type. */
  type_arme_munitions_stock: number;
  created_at: string;
  updated_at: string;
}

/** Ammunition consumption history row (auditable log). */
export interface ArmeMunitionsConsommation {
  id: number;
  arme_id: number;
  agent_id: number | null;
  armement_id: number | null;
  quantite: number;
  date_consommation: string;
  created_at: string;
  /** Joined from arme via the API. */
  arme_matricule?: string | null;
  /** Joined from type_arme via the API. */
  type_arme_nom?: string | null;
  /** Joined from personnel via the API. */
  agent_im?: string | null;
  agent_grade?: string | null;
  agent_firstname?: string | null;
  agent_lastname?: string | null;
}

// ========================
// Matériel Types (Sédentaire > Poste — equipment assignment & return)
// ========================

/** Equipment type catalog (e.g. Radio, Bâton, Gilet, Menottes, Lampe). */
export interface TypeMateriel {
  id: number;
  nom: string;
  description: string | null;
  created_at: string;
  updated_at: string;
}

/** One material line item within an assignment (type + states). */
export interface AffectationMaterielLigne {
  id: number;
  affectation_id: number;
  type_materiel_id: number;
  /** Snapshot of the type name at assignment time. */
  type_materiel_nom: string;
  /** Condition state at issue (perception). */
  etat_emport: string | null;
  /** Condition state at return (réintégration). Null until returned. */
  etat_reintegration: string | null;
  created_at: string;
  updated_at: string;
}

export type AffectationMaterielStatut = "Assigné" | "Réintégré";

/** Equipment assignment header (agent + perception/reintegration dates + lignes). */
export interface AffectationMateriel {
  id: number;
  agent_personnel_id: number;
  agent_im: string | null;
  agent_grade: string | null;
  agent_nom: string | null;
  date_perception: string;
  heure_perception: string;
  date_reintegration: string | null;
  heure_reintegration: string | null;
  statut: AffectationMaterielStatut;
  observations: string | null;
  agent_verifie: number;
  agent_verifie_at: string | null;
  signature_svg: string | null;
  created_by: number | null;
  agent_personnel_im?: string | null;
  lignes?: AffectationMaterielLigne[];
  created_at: string;
  updated_at: string;
}

// ========================
// Matériel Roulant (vehicle perception & reintegration — VHL / Moto)
// ========================

export type MaterielRoulantType = "VHL" | "Moto";
export type MaterielRoulantStatut = "En service" | "Réintégré";

export interface MaterielRoulant {
  id: number;
  date_perception: string;
  heure_perception: string;
  type_materiel: MaterielRoulantType;
  numero_immatriculation: string | null;
  description_vehicule: string | null;
  agent_conducteur_personnel_id: number | null;
  agent_conducteur_im: string | null;
  agent_conducteur_grade: string | null;
  agent_conducteur_nom: string | null;
  chef_de_bord_personnel_id: number | null;
  chef_de_bord_im: string | null;
  chef_de_bord_grade: string | null;
  chef_de_bord_nom: string | null;
  kilometrage_depart: string | null;
  niveau_carburant_depart: string | null;
  heure_reintegration: string | null;
  date_reintegration: string | null;
  kilometrage_retour: string | null;
  niveau_carburant_retour: string | null;
  observations_techniques: string | null;
  defaillances: string | null;
  agent_verifie: number;
  agent_verifie_at: string | null;
  signature_svg: string | null;
  statut: MaterielRoulantStatut;
  created_by: number | null;
  agent_conducteur_personnel_im?: string | null;
  chef_de_bord_personnel_im?: string | null;
  attachments?: MaterielRoulantAttachment[];
  created_at: string;
  updated_at: string;
}

export interface MaterielRoulantAttachment {
  id: number;
  materiel_roulant_id: number;
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

// ========================
// Main courante Types (Sédentaire > Secrétariat & Poste)
// ========================
export type MainCouranteOrigine = "Secretariat" | "Poste";

// Categories are now user-managed through a dedicated dialog on the form
// page. The main_courante.categorie column stores the label string, so we
// keep this as a plain string rather than a fixed union.
export type MainCouranteCategorie = string;

/** A row from the main_courante_categorie catalog table. */
export interface MainCouranteCategorieItem {
  id: number;
  label: string;
  created_at?: string;
  updated_at?: string;
}

export interface MainCourante {
  id: number;
  date_evenement: string;
  heure_evenement: string;
  categorie: MainCouranteCategorie;
  description: string;
  origine: MainCouranteOrigine;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: MainCouranteAttachment[];
  created_at: string;
  updated_at: string;
}

export interface MainCouranteAttachment {
  id: number;
  main_courante_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

// ========================
// Plainte (Police Judiciaire) — ENTRÉE + SORTIE
// ========================

export type PlainteEntreeType = "ST_PARQUET" | "PLAINTE_DIRECTE" | "RAPPORT_POLICE";
export type PlainteSortieNature = "DAT" | "DEFERREMENT";

export interface PlainteEntree {
  id: number;
  type: PlainteEntreeType;
  date_plainte: string;
  numero_dossier: string;
  numero_st: string | null;
  opj_personnel_id: number | null;
  enqueteur_personnel_id: number | null;
  partie_civile: string | null;
  mise_en_cause: string | null;
  adresse_pc: string | null;
  infraction: string | null;
  prejudice: string | null;
  lieu_infraction: string | null;
  heure_infraction: string | null;
  observation: string | null;
  created_by: number | null;
  opj_prenoms?: string | null;
  opj_nom?: string | null;
  opj_grade?: string | null;
  opj_im?: string | null;
  enqueteur_prenoms?: string | null;
  enqueteur_nom?: string | null;
  enqueteur_grade?: string | null;
  enqueteur_im?: string | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: PlainteEntreeAttachment[];
  created_at: string;
  updated_at: string;
}

export interface PlainteEntreeAttachment {
  id: number;
  plainte_entree_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

/** Lightweight ENTRÉE summary returned by /api/plaintes-entree/without-sortie. */
export interface PlainteEntreeSummary {
  id: number;
  type: PlainteEntreeType;
  numero_dossier: string;
  date_plainte: string;
  partie_civile: string | null;
  mise_en_cause: string | null;
  infraction: string | null;
  opj_prenoms?: string | null;
  opj_nom?: string | null;
  opj_grade?: string | null;
}

export interface PlainteSortie {
  id: number;
  plainte_entree_id: number;
  nature: PlainteSortieNature;
  date_sortie: string;
  numero: string;
  numero_ttr: string | null;
  nom_substitut: string | null;
  date_deferrement: string | null;
  observation: string | null;
  created_by: number | null;
  entree_type?: PlainteEntreeType | null;
  entree_numero_dossier?: string | null;
  entree_date_plainte?: string | null;
  entree_infraction?: string | null;
  entree_mise_en_cause?: string | null;
  entree_partie_civile?: string | null;
  entree_opj_prenoms?: string | null;
  entree_opj_nom?: string | null;
  entree_opj_grade?: string | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: PlainteSortieAttachment[];
  created_at: string;
  updated_at: string;
}

export interface PlainteSortieAttachment {
  id: number;
  plainte_sortie_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface PlainteEntreeInput {
  type: PlainteEntreeType;
  date_plainte: string;
  numero_dossier?: string | null;
  numero_st?: string | null;
  opj_personnel_id?: number | null;
  enqueteur_personnel_id?: number | null;
  partie_civile?: string | null;
  mise_en_cause?: string | null;
  adresse_pc?: string | null;
  infraction?: string | null;
  prejudice?: string | null;
  lieu_infraction?: string | null;
  heure_infraction?: string | null;
  observation?: string | null;
}

export interface PlainteSortieInput {
  plainte_entree_id: number;
  nature: PlainteSortieNature;
  date_sortie: string;
  numero?: string | null;
  numero_ttr: string;
  nom_substitut: string;
  date_deferrement?: string | null;
  observation?: string | null;
}

// ========================
// Convocation (Police Judiciaire)
// ========================

export type ConvocationType = "ST_PARQUET" | "PLAINTE_DIRECTE";

export interface Convocation {
  id: number;
  type: ConvocationType;
  date_convocation: string;
  numero: string;
  nom: string;
  adresse: string | null;
  infraction: string | null;
  personne_accuse_recu: string | null;
  numero_dossier: string | null;
  observation: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: ConvocationAttachment[];
  created_at: string;
  updated_at: string;
}

export interface ConvocationAttachment {
  id: number;
  convocation_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface ConvocationInput {
  type: ConvocationType;
  date_convocation: string;
  numero?: string | null;
  nom: string;
  adresse?: string | null;
  infraction?: string | null;
  personne_accuse_recu?: string | null;
  numero_dossier?: string | null;
  observation?: string | null;
}
