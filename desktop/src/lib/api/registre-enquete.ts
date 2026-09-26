import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  RegistreEnquete,
  RegistreEnqueteAttachment,
  RegistreEnqueteInput,
} from "@/types";

// ========================
// Registre d'enquête API (Police Judiciaire)
// ========================

export async function getRegistreEnqueteList(
  filters?: Record<string, string>,
): Promise<RegistreEnquete[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<RegistreEnquete[]>>(
    `/registres-enquete${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getRegistreEnqueteById(id: number): Promise<RegistreEnquete> {
  const { data } = await apiClient.get<ApiResponse<RegistreEnquete>>(
    `/registres-enquete/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next registre d'enquête number. */
export async function peekRegistreEnqueteNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/registres-enquete/next-number`,
  );
  return data.data.numero;
}

export async function createRegistreEnquete(
  input: RegistreEnqueteInput,
): Promise<RegistreEnquete> {
  const { data } = await apiClient.post<ApiResponse<RegistreEnquete>>(
    "/registres-enquete",
    input,
  );
  return data.data;
}

export async function updateRegistreEnquete(
  id: number,
  input: RegistreEnqueteInput,
): Promise<RegistreEnquete> {
  const { data } = await apiClient.put<ApiResponse<RegistreEnquete>>(
    `/registres-enquete/${id}`,
    input,
  );
  return data.data;
}

export async function deleteRegistreEnquete(id: number): Promise<void> {
  await apiClient.delete(`/registres-enquete/${id}`);
}

// ========================
// Registre d'enquête Attachments
// ========================

export async function getRegistreEnqueteAttachments(
  enqueteId: number,
): Promise<RegistreEnqueteAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<RegistreEnqueteAttachment[]>>(
    `/registres-enquete/${enqueteId}/attachments`,
  );
  return data.data;
}

export async function createRegistreEnqueteAttachment(
  enqueteId: number,
  title: string,
  file: File,
): Promise<RegistreEnqueteAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<RegistreEnqueteAttachment>>(
    `/registres-enquete/${enqueteId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateRegistreEnqueteAttachmentTitle(
  enqueteId: number,
  attachId: number,
  title: string,
): Promise<RegistreEnqueteAttachment> {
  const { data } = await apiClient.put<ApiResponse<RegistreEnqueteAttachment>>(
    `/registres-enquete/${enqueteId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteRegistreEnqueteAttachment(
  enqueteId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/registres-enquete/${enqueteId}/attachments/${attachId}`,
  );
}

export function getRegistreEnqueteAttachmentDownloadUrl(
  enqueteId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/registres-enquete/${enqueteId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isRegistreEnqueteImageAttachment(
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

export function validateRegistreEnqueteForm(
  input: RegistreEnqueteInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.date_ouverture?.trim()) {
    errors.date_ouverture = "La date d'ouverture est requise";
  }
  if (!input.nature_infraction?.trim()) {
    errors.nature_infraction = "La nature de l'infraction est requise";
  }
  if (!input.enqueteur_personnel_id || input.enqueteur_personnel_id <= 0) {
    errors.enqueteur_personnel_id = "L'enquêteur est requis";
  }
  return errors;
}
