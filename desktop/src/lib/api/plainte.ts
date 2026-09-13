import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  PlainteEntree,
  PlainteEntreeAttachment,
  PlainteEntreeInput,
  PlainteEntreeSummary,
  PlainteSortie,
  PlainteSortieAttachment,
  PlainteSortieInput,
} from "@/types";

// ========================
// Plainte ENTRÉE API
// ========================

export async function getPlainteEntreeList(
  filters?: Record<string, string>,
): Promise<PlainteEntree[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<PlainteEntree[]>>(
    `/plaintes-entree${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getPlainteEntreeById(id: number): Promise<PlainteEntree> {
  const { data } = await apiClient.get<ApiResponse<PlainteEntree>>(
    `/plaintes-entree/${id}`,
  );
  return data.data;
}

export async function getPlaintesEntreeWithoutSortie(): Promise<PlainteEntreeSummary[]> {
  const { data } = await apiClient.get<ApiResponse<PlainteEntreeSummary[]>>(
    `/plaintes-entree/without-sortie`,
  );
  return data.data;
}

/** Peek at the suggested next ENTRÉE dossier number for the given type. */
export async function peekPlainteEntreeNumber(type: string): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero_dossier: string }>>(
    `/plaintes-entree/next-number?type=${encodeURIComponent(type)}`,
  );
  return data.data.numero_dossier;
}

export async function createPlainteEntree(
  input: PlainteEntreeInput,
): Promise<PlainteEntree> {
  const { data } = await apiClient.post<ApiResponse<PlainteEntree>>(
    "/plaintes-entree",
    input,
  );
  return data.data;
}

export async function updatePlainteEntree(
  id: number,
  input: PlainteEntreeInput,
): Promise<PlainteEntree> {
  const { data } = await apiClient.put<ApiResponse<PlainteEntree>>(
    `/plaintes-entree/${id}`,
    input,
  );
  return data.data;
}

export async function deletePlainteEntree(id: number): Promise<void> {
  await apiClient.delete(`/plaintes-entree/${id}`);
}

// ========================
// Plainte ENTRÉE Attachments
// ========================

export async function getPlainteEntreeAttachments(
  plainteEntreeId: number,
): Promise<PlainteEntreeAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<PlainteEntreeAttachment[]>>(
    `/plaintes-entree/${plainteEntreeId}/attachments`,
  );
  return data.data;
}

export async function createPlainteEntreeAttachment(
  plainteEntreeId: number,
  title: string,
  file: File,
): Promise<PlainteEntreeAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<PlainteEntreeAttachment>>(
    `/plaintes-entree/${plainteEntreeId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updatePlainteEntreeAttachmentTitle(
  plainteEntreeId: number,
  attachId: number,
  title: string,
): Promise<PlainteEntreeAttachment> {
  const { data } = await apiClient.put<ApiResponse<PlainteEntreeAttachment>>(
    `/plaintes-entree/${plainteEntreeId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deletePlainteEntreeAttachment(
  plainteEntreeId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/plaintes-entree/${plainteEntreeId}/attachments/${attachId}`,
  );
}

export function getPlainteEntreeAttachmentDownloadUrl(
  plainteEntreeId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/plaintes-entree/${plainteEntreeId}/attachments/${attachId}/download`;
}

// ========================
// Plainte SORTIE API
// ========================

export async function getPlainteSortieList(
  filters?: Record<string, string>,
): Promise<PlainteSortie[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<PlainteSortie[]>>(
    `/plaintes-sortie${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getPlainteSortieById(id: number): Promise<PlainteSortie> {
  const { data } = await apiClient.get<ApiResponse<PlainteSortie>>(
    `/plaintes-sortie/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next SORTIE number. */
export async function peekPlainteSortieNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/plaintes-sortie/next-number`,
  );
  return data.data.numero;
}

export async function createPlainteSortie(
  input: PlainteSortieInput,
): Promise<PlainteSortie> {
  const { data } = await apiClient.post<ApiResponse<PlainteSortie>>(
    "/plaintes-sortie",
    input,
  );
  return data.data;
}

export async function updatePlainteSortie(
  id: number,
  input: PlainteSortieInput,
): Promise<PlainteSortie> {
  const { data } = await apiClient.put<ApiResponse<PlainteSortie>>(
    `/plaintes-sortie/${id}`,
    input,
  );
  return data.data;
}

export async function deletePlainteSortie(id: number): Promise<void> {
  await apiClient.delete(`/plaintes-sortie/${id}`);
}

// ========================
// Plainte SORTIE Attachments
// ========================

export async function getPlainteSortieAttachments(
  plainteSortieId: number,
): Promise<PlainteSortieAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<PlainteSortieAttachment[]>>(
    `/plaintes-sortie/${plainteSortieId}/attachments`,
  );
  return data.data;
}

export async function createPlainteSortieAttachment(
  plainteSortieId: number,
  title: string,
  file: File,
): Promise<PlainteSortieAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<PlainteSortieAttachment>>(
    `/plaintes-sortie/${plainteSortieId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updatePlainteSortieAttachmentTitle(
  plainteSortieId: number,
  attachId: number,
  title: string,
): Promise<PlainteSortieAttachment> {
  const { data } = await apiClient.put<ApiResponse<PlainteSortieAttachment>>(
    `/plaintes-sortie/${plainteSortieId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deletePlainteSortieAttachment(
  plainteSortieId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/plaintes-sortie/${plainteSortieId}/attachments/${attachId}`,
  );
}

export function getPlainteSortieAttachmentDownloadUrl(
  plainteSortieId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/plaintes-sortie/${plainteSortieId}/attachments/${attachId}/download`;
}

// ========================
// Shared helpers
// ========================

const IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg"];

/** Whether an attachment is an image that can be previewed in-app. */
export function isPlainteImageAttachment(
  attachment: Pick<PlainteEntreeAttachment, "mime_type" | "original_filename">,
): boolean {
  if (attachment.mime_type?.startsWith("image/")) return true;
  const ext = attachment.original_filename?.split(".").pop()?.toLowerCase();
  return !!ext && IMAGE_EXTENSIONS.includes(ext);
}

// ========================
// Validation (mirrors server-side controller rules)
// ========================

const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const HEURE_REGEX = /^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/;

export const PLAINTE_ENTREE_TYPES = ["ST_PARQUET", "PLAINTE_DIRECTE", "RAPPORT_POLICE"] as const;
export const PLAINTE_ENTREE_TYPE_LABELS: Record<string, string> = {
  ST_PARQUET: "ST Parquet",
  PLAINTE_DIRECTE: "Plainte directe",
  RAPPORT_POLICE: "Rapport de police",
};

export const PLAINTE_SORTIE_NATURES = ["DAT", "DEFERREMENT"] as const;
export const PLAINTE_SORTIE_NATURE_LABELS: Record<string, string> = {
  DAT: "DAT",
  DEFERREMENT: "Déferrement",
};

export function validatePlainteEntreeForm(input: PlainteEntreeInput): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!input.type) {
    errors.type = "Le type de plainte est requis";
  } else if (!PLAINTE_ENTREE_TYPES.includes(input.type)) {
    errors.type = "Le type de plainte est invalide";
  }

  if (!input.date_plainte) {
    errors.date_plainte = "La date est requise";
  } else if (!DATE_REGEX.test(input.date_plainte)) {
    errors.date_plainte = "La date est invalide (format attendu : AAAA-MM-JJ)";
  }

  if (input.type === "ST_PARQUET" && !input.numero_st?.trim()) {
    errors.numero_st = "Le numéro du ST est requis pour ce type";
  }

  if (!input.opj_personnel_id || input.opj_personnel_id <= 0) {
    errors.opj_personnel_id = "L'OPJ est requis";
  }
  if (!input.enqueteur_personnel_id || input.enqueteur_personnel_id <= 0) {
    errors.enqueteur_personnel_id = "L'enquêteur est requis";
  }

  if (input.type === "ST_PARQUET" || input.type === "PLAINTE_DIRECTE") {
    if (!input.partie_civile?.trim()) {
      errors.partie_civile = "La partie civile est requise pour ce type";
    }
    if (!input.adresse_pc?.trim()) {
      errors.adresse_pc = "L'adresse du PC est requise pour ce type";
    }
  }

  if (!input.mise_en_cause?.trim()) {
    errors.mise_en_cause = "La mise en cause est requise";
  }
  if (!input.infraction?.trim()) {
    errors.infraction = "L'infraction est requise";
  }
  if (!input.prejudice?.trim()) {
    errors.prejudice = "Le préjudice est requis";
  }
  if (!input.lieu_infraction?.trim()) {
    errors.lieu_infraction = "Le lieu de l'infraction est requis";
  }

  if (input.heure_infraction && !HEURE_REGEX.test(input.heure_infraction)) {
    errors.heure_infraction = "L'heure est invalide (format attendu : HH:MM)";
  }

  return errors;
}

export function validatePlainteSortieForm(input: PlainteSortieInput): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!input.nature) {
    errors.nature = "La nature est requise";
  } else if (!PLAINTE_SORTIE_NATURES.includes(input.nature)) {
    errors.nature = "La nature est invalide";
  }

  if (!input.date_sortie) {
    errors.date_sortie = "La date est requise";
  } else if (!DATE_REGEX.test(input.date_sortie)) {
    errors.date_sortie = "La date est invalide (format attendu : AAAA-MM-JJ)";
  }

  if (!input.numero_ttr?.trim()) {
    errors.numero_ttr = "Le N° TTR est requis";
  }
  if (!input.nom_substitut?.trim()) {
    errors.nom_substitut = "Le nom du substitut est requis";
  }

  if (input.nature === "DEFERREMENT") {
    if (!input.date_deferrement) {
      errors.date_deferrement = "La date du déferrement est requise pour ce type";
    } else if (!DATE_REGEX.test(input.date_deferrement)) {
      errors.date_deferrement = "La date du déferrement est invalide (format attendu : AAAA-MM-JJ)";
    }
  }

  return errors;
}
