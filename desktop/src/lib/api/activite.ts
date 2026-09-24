import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Activite,
  ActiviteAttachment,
  ActiviteInput,
  PatrouilleItineraireField,
} from "@/types";

// ========================
// Activités API (Service Général) — patrouilles et interventions
// ========================

/** Patrol types in display order: code → French label. */
export const PATROUILLE_TYPES = [
  { key: "diurne", label: "Patrouille diurne" },
  { key: "nocturne", label: "Patrouille nocturne" },
] as const;

/** Patrol modes in display order: code → French label. */
export const PATROUILLE_MODES = [
  { key: "motorisee", label: "Motorisée" },
  { key: "pedestre", label: "Pédestre" },
  { key: "portee", label: "Portée" },
] as const;

type PatrouilleTypeKey = (typeof PATROUILLE_TYPES)[number]["key"];
type PatrouilleModeKey = (typeof PATROUILLE_MODES)[number]["key"];

/** DB column name for a patrol type/mode itinerary. */
export function patrouilleItineraireField(
  type: PatrouilleTypeKey,
  mode: PatrouilleModeKey,
): PatrouilleItineraireField {
  return `patrouille_${type}_${mode}_itineraire`;
}

export async function getActiviteList(
  filters?: Record<string, string>,
): Promise<Activite[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Activite[]>>(
    `/activites${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getActiviteById(id: number): Promise<Activite> {
  const { data } = await apiClient.get<ApiResponse<Activite>>(
    `/activites/${id}`,
  );
  return data.data;
}

export async function createActivite(input: ActiviteInput): Promise<Activite> {
  const { data } = await apiClient.post<ApiResponse<Activite>>(
    `/activites`,
    input,
  );
  return data.data;
}

export async function updateActivite(
  id: number,
  input: ActiviteInput,
): Promise<Activite> {
  const { data } = await apiClient.put<ApiResponse<Activite>>(
    `/activites/${id}`,
    input,
  );
  return data.data;
}

export async function deleteActivite(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/activites/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getActiviteAttachments(
  activiteId: number,
): Promise<ActiviteAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ActiviteAttachment[]>>(
    `/activites/${activiteId}/attachments`,
  );
  return data.data;
}

export async function createActiviteAttachment(
  activiteId: number,
  title: string,
  file: File,
): Promise<ActiviteAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ActiviteAttachment>>(
    `/activites/${activiteId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateActiviteAttachmentTitle(
  activiteId: number,
  attachId: number,
  title: string,
): Promise<ActiviteAttachment> {
  const { data } = await apiClient.put<ApiResponse<ActiviteAttachment>>(
    `/activites/${activiteId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteActiviteAttachment(
  activiteId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/activites/${activiteId}/attachments/${attachId}`,
  );
}

export function getActiviteAttachmentDownloadUrl(
  activiteId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/activites/${activiteId}/attachments/${attachId}/download`;
}
