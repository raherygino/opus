import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Perquisition,
  PerquisitionAttachment,
  PerquisitionInput,
} from "@/types";

// ========================
// Perquisition API (Police Judiciaire)
// ========================

export async function getPerquisitionList(
  filters?: Record<string, string>,
): Promise<Perquisition[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Perquisition[]>>(
    `/perquisitions${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getPerquisitionById(id: number): Promise<Perquisition> {
  const { data } = await apiClient.get<ApiResponse<Perquisition>>(
    `/perquisitions/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next perquisition number. */
export async function peekPerquisitionNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/perquisitions/next-number`,
  );
  return data.data.numero;
}

export async function createPerquisition(
  input: PerquisitionInput,
): Promise<Perquisition> {
  const { data } = await apiClient.post<ApiResponse<Perquisition>>(
    "/perquisitions",
    input,
  );
  return data.data;
}

export async function updatePerquisition(
  id: number,
  input: PerquisitionInput,
): Promise<Perquisition> {
  const { data } = await apiClient.put<ApiResponse<Perquisition>>(
    `/perquisitions/${id}`,
    input,
  );
  return data.data;
}

export async function deletePerquisition(id: number): Promise<void> {
  await apiClient.delete(`/perquisitions/${id}`);
}

// ========================
// Perquisition Attachments
// ========================

export async function getPerquisitionAttachments(
  perquisitionId: number,
): Promise<PerquisitionAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<PerquisitionAttachment[]>>(
    `/perquisitions/${perquisitionId}/attachments`,
  );
  return data.data;
}

export async function createPerquisitionAttachment(
  perquisitionId: number,
  title: string,
  file: File,
): Promise<PerquisitionAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<PerquisitionAttachment>>(
    `/perquisitions/${perquisitionId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updatePerquisitionAttachmentTitle(
  perquisitionId: number,
  attachId: number,
  title: string,
): Promise<PerquisitionAttachment> {
  const { data } = await apiClient.put<ApiResponse<PerquisitionAttachment>>(
    `/perquisitions/${perquisitionId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deletePerquisitionAttachment(
  perquisitionId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/perquisitions/${perquisitionId}/attachments/${attachId}`,
  );
}

export function getPerquisitionAttachmentDownloadUrl(
  perquisitionId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/perquisitions/${perquisitionId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isPerquisitionImageAttachment(
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

export function validatePerquisitionForm(
  input: PerquisitionInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.affaire?.trim()) {
    errors.affaire = "L'affaire est requise";
  }
  return errors;
}
