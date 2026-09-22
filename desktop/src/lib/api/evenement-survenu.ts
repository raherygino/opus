import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  EvenementSurvenu,
  EvenementSurvenuAttachment,
  EvenementSurvenuInput,
  EvenementSurvenuType,
} from "@/types";

// ========================
// Évènements survenus API (Service Général)
// ========================

export const EVENEMENT_TYPES: EvenementSurvenuType[] = [
  "infraction",
  "incident",
  "accident",
  "autre",
];

export const EVENEMENT_TYPE_LABELS: Record<EvenementSurvenuType, string> = {
  infraction: "Infraction",
  incident: "Incident",
  accident: "Accident",
  autre: "Autre",
};

export async function getEvenementSurvenuList(
  filters?: Record<string, string>,
): Promise<EvenementSurvenu[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<EvenementSurvenu[]>>(
    `/evenements-survenus${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getEvenementSurvenuById(
  id: number,
): Promise<EvenementSurvenu> {
  const { data } = await apiClient.get<ApiResponse<EvenementSurvenu>>(
    `/evenements-survenus/${id}`,
  );
  return data.data;
}

export async function createEvenementSurvenu(
  input: EvenementSurvenuInput,
): Promise<EvenementSurvenu> {
  const { data } = await apiClient.post<ApiResponse<EvenementSurvenu>>(
    `/evenements-survenus`,
    input,
  );
  return data.data;
}

export async function updateEvenementSurvenu(
  id: number,
  input: EvenementSurvenuInput,
): Promise<EvenementSurvenu> {
  const { data } = await apiClient.put<ApiResponse<EvenementSurvenu>>(
    `/evenements-survenus/${id}`,
    input,
  );
  return data.data;
}

export async function deleteEvenementSurvenu(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/evenements-survenus/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getEvenementSurvenuAttachments(
  evenementId: number,
): Promise<EvenementSurvenuAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<EvenementSurvenuAttachment[]>>(
    `/evenements-survenus/${evenementId}/attachments`,
  );
  return data.data;
}

export async function createEvenementSurvenuAttachment(
  evenementId: number,
  title: string,
  file: File,
): Promise<EvenementSurvenuAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<EvenementSurvenuAttachment>>(
    `/evenements-survenus/${evenementId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateEvenementSurvenuAttachmentTitle(
  evenementId: number,
  attachId: number,
  title: string,
): Promise<EvenementSurvenuAttachment> {
  const { data } = await apiClient.put<ApiResponse<EvenementSurvenuAttachment>>(
    `/evenements-survenus/${evenementId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteEvenementSurvenuAttachment(
  evenementId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/evenements-survenus/${evenementId}/attachments/${attachId}`,
  );
}

export function getEvenementSurvenuAttachmentDownloadUrl(
  evenementId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/evenements-survenus/${evenementId}/attachments/${attachId}/download`;
}
