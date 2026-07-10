import { apiClient } from "@/lib/api-client";
import type { ApiResponse, Mouvement, MouvementAttachment, Personnel } from "@/types";

export async function getMouvementList(
  filters?: Record<string, string>,
): Promise<Mouvement[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Mouvement[]>>(
    `/mouvements${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getMouvementById(id: number): Promise<Mouvement> {
  const { data } = await apiClient.get<ApiResponse<Mouvement>>(
    `/mouvements/${id}`,
  );
  return data.data;
}

export async function createMouvement(
  mouvement: Omit<Mouvement, "id" | "created_at" | "updated_at">,
): Promise<Mouvement> {
  const { data } = await apiClient.post<ApiResponse<Mouvement>>(
    "/mouvements",
    mouvement,
  );
  return data.data;
}

export async function updateMouvement(
  id: number,
  mouvement: Partial<Mouvement>,
): Promise<Mouvement> {
  const { data } = await apiClient.put<ApiResponse<Mouvement>>(
    `/mouvements/${id}`,
    mouvement,
  );
  return data.data;
}

export async function deleteMouvement(id: number): Promise<void> {
  await apiClient.delete(`/mouvements/${id}`);
}

export async function getPersonnelByIm(
  im: string,
): Promise<Personnel | null> {
  try {
    const { data } = await apiClient.get<ApiResponse<Personnel[]>>(
      `/personnel?search=${encodeURIComponent(im)}`,
    );
    const found = data.data.find(
      (p) => p.im.toLowerCase() === im.toLowerCase(),
    );
    return found || null;
  } catch {
    return null;
  }
}

// ========================
// Attachment API
// ========================

export async function getMouvementAttachments(
  mouvementId: number,
): Promise<MouvementAttachment[]> {
  const { data } = await apiClient.get<
    ApiResponse<MouvementAttachment[]>
  >(`/mouvements/${mouvementId}/attachments`);
  return data.data;
}

export async function createMouvementAttachment(
  mouvementId: number,
  title: string,
  file: File,
): Promise<MouvementAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<
    ApiResponse<MouvementAttachment>
  >(`/mouvements/${mouvementId}/attachments`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data.data;
}

export async function deleteMouvementAttachment(
  mouvementId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/mouvements/${mouvementId}/attachments/${attachId}`,
  );
}

export function getMouvementAttachmentDownloadUrl(
  mouvementId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/mouvements/${mouvementId}/attachments/${attachId}/download`;
}
