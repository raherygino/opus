import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Passation,
  PassationAttachment,
} from "@/types";

export interface PassationPayload {
  date_passation: string;
  heure_passation: string;
  // The chef descendant is determined server-side from the auth user, but
  // the montant identity is provided by the client after a successful
  // /auth/verify call. The password is NEVER sent here.
  chef_montant_user_id: number;
  chef_montant_grade: string;
  chef_montant_lastname: string;
  instructions_autorite: string;
  incidents_survenus: string;
}

export async function getPassationList(
  filters?: Record<string, string>,
): Promise<Passation[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Passation[]>>(
    `/passations${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getPassationById(id: number): Promise<Passation> {
  const { data } = await apiClient.get<ApiResponse<Passation>>(
    `/passations/${id}`,
  );
  return data.data;
}

export async function createPassation(
  passation: PassationPayload,
): Promise<Passation> {
  const { data } = await apiClient.post<ApiResponse<Passation>>(
    "/passations",
    passation,
  );
  return data.data;
}

export async function updatePassation(
  id: number,
  passation: Partial<PassationPayload>,
): Promise<Passation> {
  const { data } = await apiClient.put<ApiResponse<Passation>>(
    `/passations/${id}`,
    passation,
  );
  return data.data;
}

export async function deletePassation(id: number): Promise<void> {
  await apiClient.delete(`/passations/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getPassationAttachments(
  passationId: number,
): Promise<PassationAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<PassationAttachment[]>>(
    `/passations/${passationId}/attachments`,
  );
  return data.data;
}

export async function createPassationAttachment(
  passationId: number,
  title: string,
  file: File,
): Promise<PassationAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<PassationAttachment>>(
    `/passations/${passationId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updatePassationAttachmentTitle(
  passationId: number,
  attachId: number,
  title: string,
): Promise<PassationAttachment> {
  const { data } = await apiClient.put<ApiResponse<PassationAttachment>>(
    `/passations/${passationId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deletePassationAttachment(
  passationId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/passations/${passationId}/attachments/${attachId}`,
  );
}

export function getPassationAttachmentDownloadUrl(
  passationId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/passations/${passationId}/attachments/${attachId}/download`;
}
