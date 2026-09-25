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

// ========================
// Garde à Vue (Police Judiciaire)
// ========================

export interface GardeAVue {
  id: number;
  nom: string;
  prenoms: string | null;
  date_naissance: string | null;
  adresse: string | null;
  enqueteur_permance: string | null;
  opj_gav: string | null;
  motif: string | null;
  etat_sante: string | null;
  droits_notifies: string | null;
  personne_contacter: string | null;
  debut_gav: string | null;
  fin_gav: string | null;
  prolongation_gav: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: GardeAVueAttachment[];
  created_at: string;
  updated_at: string;
}

export interface GardeAVueAttachment {
  id: number;
  garde_a_vue_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface GardeAVueInput {
  nom: string;
  prenoms?: string | null;
  date_naissance?: string | null;
  adresse?: string | null;
  enqueteur_permance?: string | null;
  opj_gav?: string | null;
  motif?: string | null;
  etat_sante?: string | null;
  droits_notifies?: string | null;
  personne_contacter?: string | null;
  debut_gav?: string | null;
  fin_gav?: string | null;
  prolongation_gav?: string | null;
}

// ========================
// Requisition (Police Judiciaire)
// ========================

export interface Requisition {
  id: number;
  type: string;
  date_requisition: string;
  numero: string;
  numero_ttr: string | null;
  nom_substitut: string | null;
  affaire: string;
  numero_dossier: string | null;
  opj: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: RequisitionAttachment[];
  created_at: string;
  updated_at: string;
}

export interface RequisitionAttachment {
  id: number;
  requisition_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface RequisitionInput {
  type: string;
  date_requisition: string;
  numero?: string | null;
  numero_ttr?: string | null;
  nom_substitut?: string | null;
  affaire: string;
  numero_dossier?: string | null;
  opj?: string | null;
}

// ========================
// Personne Recherchée (Police Judiciaire)
// ========================

export interface PersonneRecherchee {
  id: number;
  nom: string;
  adresse: string | null;
  motif: string;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  photo_count: number;
  photos?: PersonneRechercheePhoto[];
  created_at: string;
  updated_at: string;
}

export interface PersonneRechercheePhoto {
  id: number;
  personne_recherchee_id: number;
  caption: string | null;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  width: number | null;
  height: number | null;
  capture_source: "CAMERA" | "GALLERY" | null;
  sort_order: number;
  created_at: string;
  updated_at: string;
}

export interface PersonneRechercheeInput {
  nom: string;
  adresse?: string | null;
  motif: string;
}

// ========================
// Objet (Police Judiciaire — OBJET SAISI / OBJET TROUVÉ tabs)
// ========================

export type ObjetSaisiType =
  | "TELEPHONE"
  | "ORDINATEUR"
  | "VEHICULE"
  | "DOCUMENT"
  | "ARGENT"
  | "ARME"
  | "EFFETS_PERSONNELS"
  | "AUTRE";

export const OBJET_SAISI_TYPE_LABELS: Record<ObjetSaisiType, string> = {
  TELEPHONE: "Téléphone",
  ORDINATEUR: "Ordinateur",
  VEHICULE: "Véhicule",
  DOCUMENT: "Document",
  ARGENT: "Argent",
  ARME: "Arme",
  EFFETS_PERSONNELS: "Effets personnels",
  AUTRE: "Autre",
};

export type ObjetTrouveMotif = "REQUISITION" | "SUR_PERSONNE" | "PERQUISITION";

export const OBJET_TROUVE_MOTIF_LABELS: Record<ObjetTrouveMotif, string> = {
  REQUISITION: "Réquisition",
  SUR_PERSONNE: "Sur une personne",
  PERQUISITION: "Perquisition",
};

export interface ObjetSaisi {
  id: number;
  numero_dossier: string | null;
  motif: string;
  type_objet: ObjetSaisiType;
  proprietaire: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: ObjetSaisiAttachment[];
  created_at: string;
  updated_at: string;
}

export interface ObjetSaisiAttachment {
  id: number;
  objet_saisi_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface ObjetSaisiInput {
  numero_dossier?: string | null;
  motif: string;
  type_objet: ObjetSaisiType | "";
  proprietaire?: string | null;
}

export interface ObjetTrouve {
  id: number;
  affaire: string;
  motif_decouverte: ObjetTrouveMotif;
  restitution: 0 | 1;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: ObjetTrouveAttachment[];
  created_at: string;
  updated_at: string;
}

export interface ObjetTrouveAttachment {
  id: number;
  objet_trouve_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface ObjetTrouveInput {
  affaire: string;
  motif_decouverte: ObjetTrouveMotif | "";
  restitution: boolean;
}

// ========================
// Perquisition (Police Judiciaire)
// ========================

export interface Perquisition {
  id: number;
  numero: string;
  numero_ttr: string | null;
  substitut: string | null;
  affaire: string;
  motif: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: PerquisitionAttachment[];
  created_at: string;
  updated_at: string;
}

export interface PerquisitionAttachment {
  id: number;
  perquisition_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface PerquisitionInput {
  numero?: string | null;
  numero_ttr?: string | null;
  substitut?: string | null;
  affaire: string;
  motif?: string | null;
}

// ========================
// Renseignement PJ (Police Judiciaire)
// ========================

export interface RenseignementPj {
  id: number;
  nature_infraction: string;
  date_lieu_faits: string | null;
  circonstances: string | null;
  prejudices: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: RenseignementPjAttachment[];
  created_at: string;
  updated_at: string;
}

export interface RenseignementPjAttachment {
  id: number;
  renseignement_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface RenseignementPjInput {
  nature_infraction: string;
  date_lieu_faits?: string | null;
  circonstances?: string | null;
  prejudices?: string | null;
}

// ========================
// Mandat (Police Judiciaire)
// ========================

export type MandatType = "AMENER" | "COMPARUTION" | "ARRET" | "DEPOT";

export const MANDAT_TYPES: { value: MandatType; label: string }[] = [
  { value: "AMENER", label: "Mandat d'amener" },
  { value: "COMPARUTION", label: "Mandat de comparution" },
  { value: "ARRET", label: "Mandat d'arrêt" },
  { value: "DEPOT", label: "Mandat de dépôt" },
];

export function getMandatTypeLabel(type: string): string {
  return MANDAT_TYPES.find((t) => t.value === type)?.label ?? type;
}

export interface Mandat {
  id: number;
  numero: string;
  type: string;
  autorite: string | null;
  personne_nom: string;
  date_lieu_naissance: string | null;
  motif: string | null;
  qualification_infraction: string | null;
  opj_execution: string | null;
  date_heure_execution: string | null;
  lieu_execution: string | null;
  observations: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: MandatAttachment[];
  created_at: string;
  updated_at: string;
}

export interface MandatAttachment {
  id: number;
  mandat_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface MandatInput {
  numero?: string | null;
  type: MandatType | "";
  autorite?: string | null;
  personne_nom: string;
  date_lieu_naissance?: string | null;
  motif?: string | null;
  qualification_infraction?: string | null;
  opj_execution?: string | null;
  date_heure_execution?: string | null;
  lieu_execution?: string | null;
  observations?: string | null;
}

// ========================
// Arrestation (Police Judiciaire)
// ========================

export interface Arrestation {
  id: number;
  numero: string;
  date_heure_arrestation: string;
  personne_nom: string;
  lieu_arrestation: string | null;
  motif: string | null;
  policiers: string | null;
  numero_dossier: string | null;
  observations: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  attachments?: ArrestationAttachment[];
  created_at: string;
  updated_at: string;
}

export interface ArrestationAttachment {
  id: number;
  arrestation_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface ArrestationInput {
  numero?: string | null;
  date_heure_arrestation: string;
  personne_nom: string;
  lieu_arrestation?: string | null;
  motif?: string | null;
  policiers?: string | null;
  numero_dossier?: string | null;
  observations?: string | null;
}

// ========================
// Rassemblement Journalier (Service Général)
// ========================

export interface RassemblementJournalier {
  id: number;
  date_rassemblement: string;
  heure_rassemblement: string;
  brigade_service: string;
  officier_permanence: string | null;
  inspecteur_permanence: string | null;
  chef_poste: string | null;
  instructions_autorite: string | null;
  effectif_theorique: number;
  present: number;
  absent: number;
  motif_absence: string | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  repartitions?: RepartitionSecteur[];
  created_at: string;
  updated_at: string;
}

/** Répartition par secteur — type Diurne ou Nocturne (même structure). */
export type RepartitionSecteurType = "diurne" | "nocturne";

export interface RepartitionSecteur {
  id: number;
  rassemblement_id: number;
  type: RepartitionSecteurType;
  secteur: string;
  effectif_engage: string | null;
  chef_element_contact: string | null;
  controle_contact: string | null;
  materiels_armements: string | null;
  missions: string | null;
  created_at: string;
  updated_at: string;
}

export interface RepartitionSecteurInput {
  type: RepartitionSecteurType;
  secteur: string;
  effectif_engage?: string | null;
  chef_element_contact?: string | null;
  controle_contact?: string | null;
  materiels_armements?: string | null;
  missions?: string | null;
}

export interface RassemblementJournalierInput {
  date_rassemblement: string;
  heure_rassemblement: string;
  brigade_service: string;
  officier_permanence?: string | null;
  inspecteur_permanence?: string | null;
  chef_poste?: string | null;
  instructions_autorite?: string | null;
  effectif_theorique: number;
  present: number;
  absent: number;
  motif_absence?: string | null;
  repartitions?: RepartitionSecteurInput[];
}

// ========================
// Évènements survenus (Service Général)
// ========================

/** type_evenement codes stored in DB — labels rendered in French by clients. */
/** Event type — stored as the label string directly (user-managed catalog). */
export type EvenementSurvenuType = string;

/** A row from the evenement_survenu_type catalog table. */
export interface EvenementSurvenuTypeItem {
  id: number;
  label: string;
  created_at?: string;
  updated_at?: string;
}

export interface EvenementSurvenu {
  id: number;
  date_evenement: string;
  heure_evenement: string;
  type_evenement: EvenementSurvenuType;
  lieu_exact: string;
  auteurs_presumes: string | null;
  victimes: string | null;
  temoins: string | null;
  mesures_prises: string | null;
  /** GPS position captured at record time (mobile only, null on desktop). */
  latitude: number | null;
  longitude: number | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  created_at: string;
  updated_at: string;
  /** Included by the detail endpoint (GET /evenements-survenus/{id}). */
  attachments?: EvenementSurvenuAttachment[];
}

export interface EvenementSurvenuAttachment {
  id: number;
  evenement_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface EvenementSurvenuInput {
  date_evenement: string;
  heure_evenement: string;
  type_evenement: EvenementSurvenuType | "";
  lieu_exact: string;
  auteurs_presumes?: string | null;
  victimes?: string | null;
  temoins?: string | null;
  mesures_prises?: string | null;
  /** GPS position (mobile only — desktop sends none). */
  latitude?: number | null;
  longitude?: number | null;
}

// ========================
// Activités (Service Général) — patrouilles et interventions
// ========================

/**
 * Patrol itinerary columns — one per type×mode pair. A null itinerary
 * means the patrol mode was not selected; an empty string means it was
 * selected without a detailed itinerary.
 */
export type PatrouilleItineraireField =
  | "patrouille_diurne_motorisee_itineraire"
  | "patrouille_diurne_pedestre_itineraire"
  | "patrouille_diurne_portee_itineraire"
  | "patrouille_nocturne_motorisee_itineraire"
  | "patrouille_nocturne_pedestre_itineraire"
  | "patrouille_nocturne_portee_itineraire";

export interface Activite {
  id: number;
  date_activite: string;
  heure_activite: string;
  patrouille_diurne_motorisee_itineraire: string | null;
  patrouille_diurne_pedestre_itineraire: string | null;
  patrouille_diurne_portee_itineraire: string | null;
  patrouille_nocturne_motorisee_itineraire: string | null;
  patrouille_nocturne_pedestre_itineraire: string | null;
  patrouille_nocturne_portee_itineraire: string | null;
  operation_ciblee: string | null;
  faits_constates: string | null;
  compte_rendu_hierarchie: string | null;
  conduite_a_tenir: string | null;
  nature_intervention: string | null;
  suites_donnees: string | null;
  /** GPS position captured at record time (mobile only, null on desktop). */
  latitude: number | null;
  longitude: number | null;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  created_at: string;
  updated_at: string;
  /** Included by the detail endpoint (GET /activites/{id}). */
  attachments?: ActiviteAttachment[];
}

export interface ActiviteAttachment {
  id: number;
  activite_id: number;
  title: string;
  filename: string;
  original_filename: string;
  mime_type: string | null;
  file_size: number | null;
  created_at: string;
  updated_at: string;
}

export interface ActiviteInput {
  date_activite: string;
  heure_activite: string;
  patrouille_diurne_motorisee_itineraire?: string | null;
  patrouille_diurne_pedestre_itineraire?: string | null;
  patrouille_diurne_portee_itineraire?: string | null;
  patrouille_nocturne_motorisee_itineraire?: string | null;
  patrouille_nocturne_pedestre_itineraire?: string | null;
  patrouille_nocturne_portee_itineraire?: string | null;
  operation_ciblee?: string | null;
  faits_constates?: string | null;
  compte_rendu_hierarchie?: string | null;
  conduite_a_tenir?: string | null;
  nature_intervention?: string | null;
  suites_donnees?: string | null;
  /** GPS position (mobile only — desktop sends none). */
  latitude?: number | null;
  longitude?: number | null;
}

// ========================
// Dispositif exceptionnel (Service Général)
// ========================

export interface DispositifExceptionnel {
  id: number;
  nature_evenement: string;
  date_debut: string;
  date_fin: string;
  created_by: number | null;
  agent_username?: string | null;
  agent_prenoms?: string | null;
  agent_nom?: string | null;
  /** Included by the detail endpoint (GET /dispositifs-exceptionnels/{id}). */
  effectifs?: DispositifExceptionnelEffectif[];
  created_at: string;
  updated_at: string;
}

/** One "Effectif engagé" sector row of a dispositif exceptionnel. */
export interface DispositifExceptionnelEffectif {
  id: number;
  dispositif_id: number;
  secteur: string;
  chef_element_contact: string | null;
  controle_contact: string | null;
  materiels_armements: string | null;
  missions: string | null;
  created_at: string;
  updated_at: string;
}

export interface DispositifExceptionnelEffectifInput {
  secteur: string;
  chef_element_contact?: string | null;
  controle_contact?: string | null;
  materiels_armements?: string | null;
  missions?: string | null;
}

export interface DispositifExceptionnelInput {
  nature_evenement: string;
  date_debut: string;
  date_fin: string;
  effectifs?: DispositifExceptionnelEffectifInput[];
}

// ========================
// Dashboard Stats (aggregated KPIs from GET /api/dashboard/stats)
// ========================
export interface DashboardStats {
  personnel_total: number;
  personnel_en_service: number;
  personnel_en_mouvement: number;
  mouvements_en_cours: number;
  users_total: number;
  users_actifs: number;
  gav_en_cours: number;
  armes_en_service: number;
  vehicules_en_service: number;
  activites_total: number;
  activites_7j: number;
  activites_aujourdhui: number;
  evenements_aujourdhui: number;
  main_courante_aujourdhui: number;
  correspondances_total: number;
  declarations_perte_total: number;
  plaintes_en_attente: number;
  personnes_recherchees: number;
  notifications_non_lues: number;
  generated_at: string;
}
