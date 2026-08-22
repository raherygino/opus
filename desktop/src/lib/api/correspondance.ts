import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Correspondance,
  CorrespondanceAttachment,
} from "@/types";

export interface CorrespondancePayload {
  date_correspondance: string;
  heure_enregistrement: string;
  sens: Correspondance["sens"];
  reference: string;
  emetteur_destinataire: string;
  objet: string;
  statut?: Correspondance["statut"];
}

export async function getCorrespondanceList(
  filters?: Record<string, string>,
): Promise<Correspondance[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Correspondance[]>>(
    `/correspondances${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getCorrespondanceById(
  id: number,
): Promise<Correspondance> {
  const { data } = await apiClient.get<ApiResponse<Correspondance>>(
    `/correspondances/${id}`,
  );
  return data.data;
}

export async function createCorrespondance(
  correspondance: CorrespondancePayload,
): Promise<Correspondance> {
  const { data } = await apiClient.post<ApiResponse<Correspondance>>(
    "/correspondances",
    correspondance,
  );
  return data.data;
}

export async function updateCorrespondance(
  id: number,
  correspondance: Partial<CorrespondancePayload>,
): Promise<Correspondance> {
  const { data } = await apiClient.put<ApiResponse<Correspondance>>(
    `/correspondances/${id}`,
    correspondance,
  );
  return data.data;
}

export async function deleteCorrespondance(id: number): Promise<void> {
  await apiClient.delete(`/correspondances/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getCorrespondanceAttachments(
  correspondanceId: number,
): Promise<CorrespondanceAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<CorrespondanceAttachment[]>>(
    `/correspondances/${correspondanceId}/attachments`,
  );
  return data.data;
}

export async function createCorrespondanceAttachment(
  correspondanceId: number,
  title: string,
  file: File,
): Promise<CorrespondanceAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<CorrespondanceAttachment>>(
    `/correspondances/${correspondanceId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateCorrespondanceAttachmentTitle(
  correspondanceId: number,
  attachId: number,
  title: string,
): Promise<CorrespondanceAttachment> {
  const { data } = await apiClient.put<ApiResponse<CorrespondanceAttachment>>(
    `/correspondances/${correspondanceId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteCorrespondanceAttachment(
  correspondanceId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/correspondances/${correspondanceId}/attachments/${attachId}`,
  );
}

export function getCorrespondanceAttachmentDownloadUrl(
  correspondanceId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/correspondances/${correspondanceId}/attachments/${attachId}/download`;
}
