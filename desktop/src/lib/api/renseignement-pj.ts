import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  RenseignementPj,
  RenseignementPjAttachment,
  RenseignementPjInput,
} from "@/types";

// ========================
// Renseignement PJ API (Police Judiciaire)
// ========================

export async function getRenseignementPjList(
  filters?: Record<string, string>,
): Promise<RenseignementPj[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<RenseignementPj[]>>(
    `/renseignements-pj${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getRenseignementPjById(id: number): Promise<RenseignementPj> {
  const { data } = await apiClient.get<ApiResponse<RenseignementPj>>(
    `/renseignements-pj/${id}`,
  );
  return data.data;
}

export async function createRenseignementPj(
  input: RenseignementPjInput,
): Promise<RenseignementPj> {
  const { data } = await apiClient.post<ApiResponse<RenseignementPj>>(
    "/renseignements-pj",
    input,
  );
  return data.data;
}

export async function updateRenseignementPj(
  id: number,
  input: RenseignementPjInput,
): Promise<RenseignementPj> {
  const { data } = await apiClient.put<ApiResponse<RenseignementPj>>(
    `/renseignements-pj/${id}`,
    input,
  );
  return data.data;
}

export async function deleteRenseignementPj(id: number): Promise<void> {
  await apiClient.delete(`/renseignements-pj/${id}`);
}

// ========================
// Renseignement PJ Attachments
// ========================

export async function getRenseignementPjAttachments(
  renseignementId: number,
): Promise<RenseignementPjAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<RenseignementPjAttachment[]>>(
    `/renseignements-pj/${renseignementId}/attachments`,
  );
  return data.data;
}

export async function createRenseignementPjAttachment(
  renseignementId: number,
  title: string,
  file: File,
): Promise<RenseignementPjAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<RenseignementPjAttachment>>(
    `/renseignements-pj/${renseignementId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateRenseignementPjAttachmentTitle(
  renseignementId: number,
  attachId: number,
  title: string,
): Promise<RenseignementPjAttachment> {
  const { data } = await apiClient.put<ApiResponse<RenseignementPjAttachment>>(
    `/renseignements-pj/${renseignementId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteRenseignementPjAttachment(
  renseignementId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/renseignements-pj/${renseignementId}/attachments/${attachId}`,
  );
}

export function getRenseignementPjAttachmentDownloadUrl(
  renseignementId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/renseignements-pj/${renseignementId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isRenseignementPjImageAttachment(
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

export function validateRenseignementPjForm(
  input: RenseignementPjInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.nature_infraction?.trim()) {
    errors.nature_infraction = "La nature de l'infraction est requise";
  }
  return errors;
}
