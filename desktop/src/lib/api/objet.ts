import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  ObjetSaisi,
  ObjetSaisiAttachment,
  ObjetSaisiInput,
  ObjetTrouve,
  ObjetTrouveAttachment,
  ObjetTrouveInput,
} from "@/types";

// ========================
// Objet Saisi API (OBJET tab)
// ========================

export async function getObjetSaisiList(
  filters?: Record<string, string>,
): Promise<ObjetSaisi[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<ObjetSaisi[]>>(
    `/objets/saisi${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getObjetSaisiById(id: number): Promise<ObjetSaisi> {
  const { data } = await apiClient.get<ApiResponse<ObjetSaisi>>(
    `/objets/saisi/${id}`,
  );
  return data.data;
}

export async function createObjetSaisi(input: ObjetSaisiInput): Promise<ObjetSaisi> {
  const { data } = await apiClient.post<ApiResponse<ObjetSaisi>>("/objets/saisi", input);
  return data.data;
}

export async function updateObjetSaisi(
  id: number,
  input: ObjetSaisiInput,
): Promise<ObjetSaisi> {
  const { data } = await apiClient.put<ApiResponse<ObjetSaisi>>(`/objets/saisi/${id}`, input);
  return data.data;
}

export async function deleteObjetSaisi(id: number): Promise<void> {
  await apiClient.delete(`/objets/saisi/${id}`);
}

// ─── Objet Saisi Attachments ────────────────────────────────

export async function getObjetSaisiAttachments(objetId: number): Promise<ObjetSaisiAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ObjetSaisiAttachment[]>>(
    `/objets/saisi/${objetId}/attachments`,
  );
  return data.data;
}

export async function createObjetSaisiAttachment(
  objetId: number,
  title: string,
  file: File,
): Promise<ObjetSaisiAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ObjetSaisiAttachment>>(
    `/objets/saisi/${objetId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateObjetSaisiAttachmentTitle(
  objetId: number,
  attachId: number,
  title: string,
): Promise<ObjetSaisiAttachment> {
  const { data } = await apiClient.put<ApiResponse<ObjetSaisiAttachment>>(
    `/objets/saisi/${objetId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteObjetSaisiAttachment(objetId: number, attachId: number): Promise<void> {
  await apiClient.delete(`/objets/saisi/${objetId}/attachments/${attachId}`);
}

export function getObjetSaisiAttachmentDownloadUrl(objetId: number, attachId: number): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/objets/saisi/${objetId}/attachments/${attachId}/download`;
}

// ========================
// Objet Trouvé API (OBJET TROUVÉ tab)
// ========================

export async function getObjetTrouveList(
  filters?: Record<string, string>,
): Promise<ObjetTrouve[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<ObjetTrouve[]>>(
    `/objets/trouve${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getObjetTrouveById(id: number): Promise<ObjetTrouve> {
  const { data } = await apiClient.get<ApiResponse<ObjetTrouve>>(
    `/objets/trouve/${id}`,
  );
  return data.data;
}

export async function createObjetTrouve(input: ObjetTrouveInput): Promise<ObjetTrouve> {
  const { data } = await apiClient.post<ApiResponse<ObjetTrouve>>("/objets/trouve", input);
  return data.data;
}

export async function updateObjetTrouve(
  id: number,
  input: ObjetTrouveInput,
): Promise<ObjetTrouve> {
  const { data } = await apiClient.put<ApiResponse<ObjetTrouve>>(`/objets/trouve/${id}`, input);
  return data.data;
}

export async function deleteObjetTrouve(id: number): Promise<void> {
  await apiClient.delete(`/objets/trouve/${id}`);
}

// ─── Objet Trouvé Attachments ────────────────────────────────

export async function getObjetTrouveAttachments(objetId: number): Promise<ObjetTrouveAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ObjetTrouveAttachment[]>>(
    `/objets/trouve/${objetId}/attachments`,
  );
  return data.data;
}

export async function createObjetTrouveAttachment(
  objetId: number,
  title: string,
  file: File,
): Promise<ObjetTrouveAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ObjetTrouveAttachment>>(
    `/objets/trouve/${objetId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateObjetTrouveAttachmentTitle(
  objetId: number,
  attachId: number,
  title: string,
): Promise<ObjetTrouveAttachment> {
  const { data } = await apiClient.put<ApiResponse<ObjetTrouveAttachment>>(
    `/objets/trouve/${objetId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteObjetTrouveAttachment(objetId: number, attachId: number): Promise<void> {
  await apiClient.delete(`/objets/trouve/${objetId}/attachments/${attachId}`);
}

export function getObjetTrouveAttachmentDownloadUrl(objetId: number, attachId: number): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/objets/trouve/${objetId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isObjetImageAttachment(mimeType: string | null, filename: string | null): boolean {
  if (mimeType) return mimeType.startsWith("image/");
  if (filename) {
    const ext = filename.split(".").pop()?.toLowerCase() ?? "";
    return ["jpg", "jpeg", "png", "gif", "webp", "bmp"].includes(ext);
  }
  return false;
}

export function validateObjetSaisiForm(input: ObjetSaisiInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.motif?.trim()) {
    errors.motif = "Le motif est requis";
  }
  if (!input.type_objet) {
    errors.type_objet = "Le type d'objet est requis";
  }
  return errors;
}

export function validateObjetTrouveForm(input: ObjetTrouveInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.affaire?.trim()) {
    errors.affaire = "L'affaire est requise";
  }
  if (!input.motif_decouverte) {
    errors.motif_decouverte = "Le motif de découverte est requis";
  }
  return errors;
}
