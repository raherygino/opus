import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Arrestation,
  ArrestationAttachment,
  ArrestationInput,
} from "@/types";

// ========================
// Arrestation API (Police Judiciaire)
// ========================

export async function getArrestationList(
  filters?: Record<string, string>,
): Promise<Arrestation[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Arrestation[]>>(
    `/arrestations${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getArrestationById(id: number): Promise<Arrestation> {
  const { data } = await apiClient.get<ApiResponse<Arrestation>>(
    `/arrestations/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next arrestation number. */
export async function peekArrestationNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/arrestations/next-number`,
  );
  return data.data.numero;
}

export async function createArrestation(input: ArrestationInput): Promise<Arrestation> {
  const { data } = await apiClient.post<ApiResponse<Arrestation>>(
    "/arrestations",
    input,
  );
  return data.data;
}

export async function updateArrestation(
  id: number,
  input: ArrestationInput,
): Promise<Arrestation> {
  const { data } = await apiClient.put<ApiResponse<Arrestation>>(
    `/arrestations/${id}`,
    input,
  );
  return data.data;
}

export async function deleteArrestation(id: number): Promise<void> {
  await apiClient.delete(`/arrestations/${id}`);
}

// ========================
// Arrestation Attachments
// ========================

export async function getArrestationAttachments(
  arrestationId: number,
): Promise<ArrestationAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ArrestationAttachment[]>>(
    `/arrestations/${arrestationId}/attachments`,
  );
  return data.data;
}

export async function createArrestationAttachment(
  arrestationId: number,
  title: string,
  file: File,
): Promise<ArrestationAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ArrestationAttachment>>(
    `/arrestations/${arrestationId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateArrestationAttachmentTitle(
  arrestationId: number,
  attachId: number,
  title: string,
): Promise<ArrestationAttachment> {
  const { data } = await apiClient.put<ApiResponse<ArrestationAttachment>>(
    `/arrestations/${arrestationId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteArrestationAttachment(
  arrestationId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(`/arrestations/${arrestationId}/attachments/${attachId}`);
}

export function getArrestationAttachmentDownloadUrl(
  arrestationId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/arrestations/${arrestationId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isArrestationImageAttachment(
  mimeType: string | null,
  filename: string | null,
): boolean {
  if (mimeType) return mimeType.startsWith("image/");
  if (filename) {
    const ext = filename.split(".").pop()?.toLowerCase() ?? "";
    return ["jpg", "jpeg", "png", "gif", "webp", "bmp"].includes(ext);
  }
  return false;
}

export function validateArrestationForm(
  input: ArrestationInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.date_heure_arrestation) {
    errors.date_heure_arrestation = "La date et l'heure de l'arrestation sont requises";
  } else if (!strtotime(input.date_heure_arrestation)) {
    errors.date_heure_arrestation = "La date et l'heure de l'arrestation sont invalides";
  }
  if (!input.personne_nom?.trim()) {
    errors.personne_nom = "Le nom et prénom de la personne arrêtée sont requis";
  }
  return errors;
}

/** Lightweight check mirroring PHP strtotime for datetime-local values. */
function strtotime(value: string): boolean {
  return !isNaN(Date.parse(value.replace(" ", "T")));
}
