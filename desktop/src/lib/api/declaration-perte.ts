import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  DeclarationPerte,
  DeclarationPerteAttachment,
} from "@/types";

export interface DeclarationPertePayload {
  date_declaration: string;
  heure_declaration: string;
  identite_declarant: string;
  nature_objet: string;
  description_objet: string;
  date_perte: string;
  lieu_perte: string;
  numero_attestation: string;
  nom_agent: string;
}

export async function getDeclarationPerteList(
  filters?: Record<string, string>,
): Promise<DeclarationPerte[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<DeclarationPerte[]>>(
    `/declarations-perte${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getDeclarationPerteById(
  id: number,
): Promise<DeclarationPerte> {
  const { data } = await apiClient.get<ApiResponse<DeclarationPerte>>(
    `/declarations-perte/${id}`,
  );
  return data.data;
}

export async function createDeclarationPerte(
  declaration: DeclarationPertePayload,
): Promise<DeclarationPerte> {
  const { data } = await apiClient.post<ApiResponse<DeclarationPerte>>(
    "/declarations-perte",
    declaration,
  );
  return data.data;
}

export async function updateDeclarationPerte(
  id: number,
  declaration: Partial<DeclarationPertePayload>,
): Promise<DeclarationPerte> {
  const { data } = await apiClient.put<ApiResponse<DeclarationPerte>>(
    `/declarations-perte/${id}`,
    declaration,
  );
  return data.data;
}

export async function deleteDeclarationPerte(id: number): Promise<void> {
  await apiClient.delete(`/declarations-perte/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getDeclarationPerteAttachments(
  declarationId: number,
): Promise<DeclarationPerteAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<DeclarationPerteAttachment[]>>(
    `/declarations-perte/${declarationId}/attachments`,
  );
  return data.data;
}

export async function createDeclarationPerteAttachment(
  declarationId: number,
  title: string,
  file: File,
): Promise<DeclarationPerteAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<DeclarationPerteAttachment>>(
    `/declarations-perte/${declarationId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateDeclarationPerteAttachmentTitle(
  declarationId: number,
  attachId: number,
  title: string,
): Promise<DeclarationPerteAttachment> {
  const { data } = await apiClient.put<ApiResponse<DeclarationPerteAttachment>>(
    `/declarations-perte/${declarationId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteDeclarationPerteAttachment(
  declarationId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/declarations-perte/${declarationId}/attachments/${attachId}`,
  );
}

export function getDeclarationPerteAttachmentDownloadUrl(
  declarationId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/declarations-perte/${declarationId}/attachments/${attachId}/download`;
}
