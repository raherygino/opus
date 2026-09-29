import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  SituationGav,
  SituationGavAttachment,
  SituationGavInput,
} from "@/types";

// ========================
// Situation GAV API (Sédentaire > Poste)
// ========================

export async function getSituationGavList(
  filters?: Record<string, string>,
): Promise<SituationGav[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<SituationGav[]>>(
    `/situations-gav${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getSituationGavById(id: number): Promise<SituationGav> {
  const { data } = await apiClient.get<ApiResponse<SituationGav>>(
    `/situations-gav/${id}`,
  );
  return data.data;
}

export async function createSituationGav(
  input: SituationGavInput,
): Promise<SituationGav> {
  const { data } = await apiClient.post<ApiResponse<SituationGav>>(
    "/situations-gav",
    input,
  );
  return data.data;
}

export async function updateSituationGav(
  id: number,
  input: SituationGavInput,
): Promise<SituationGav> {
  const { data } = await apiClient.put<ApiResponse<SituationGav>>(
    `/situations-gav/${id}`,
    input,
  );
  return data.data;
}

export async function deleteSituationGav(id: number): Promise<void> {
  await apiClient.delete(`/situations-gav/${id}`);
}

// ========================
// Situation GAV Attachments
// ========================

export async function getSituationGavAttachments(
  situationGavId: number,
): Promise<SituationGavAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<SituationGavAttachment[]>>(
    `/situations-gav/${situationGavId}/attachments`,
  );
  return data.data;
}

export async function createSituationGavAttachment(
  situationGavId: number,
  title: string,
  file: File,
): Promise<SituationGavAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<SituationGavAttachment>>(
    `/situations-gav/${situationGavId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateSituationGavAttachmentTitle(
  situationGavId: number,
  attachId: number,
  title: string,
): Promise<SituationGavAttachment> {
  const { data } = await apiClient.put<ApiResponse<SituationGavAttachment>>(
    `/situations-gav/${situationGavId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteSituationGavAttachment(
  situationGavId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/situations-gav/${situationGavId}/attachments/${attachId}`,
  );
}

export function getSituationGavAttachmentDownloadUrl(
  situationGavId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/situations-gav/${situationGavId}/attachments/${attachId}/download`;
}
