import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  GardeAVue,
  GardeAVueAttachment,
  GardeAVueInput,
} from "@/types";

// ========================
// Garde à Vue API
// ========================

export async function getGardeAVueList(
  filters?: Record<string, string>,
): Promise<GardeAVue[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<GardeAVue[]>>(
    `/garde-a-vue${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getGardeAVueById(id: number): Promise<GardeAVue> {
  const { data } = await apiClient.get<ApiResponse<GardeAVue>>(
    `/garde-a-vue/${id}`,
  );
  return data.data;
}

export async function createGardeAVue(
  input: GardeAVueInput,
): Promise<GardeAVue> {
  const { data } = await apiClient.post<ApiResponse<GardeAVue>>(
    "/garde-a-vue",
    input,
  );
  return data.data;
}

export async function updateGardeAVue(
  id: number,
  input: GardeAVueInput,
): Promise<GardeAVue> {
  const { data } = await apiClient.put<ApiResponse<GardeAVue>>(
    `/garde-a-vue/${id}`,
    input,
  );
  return data.data;
}

export async function deleteGardeAVue(id: number): Promise<void> {
  await apiClient.delete(`/garde-a-vue/${id}`);
}

// ========================
// Garde à Vue Attachments
// ========================

export async function getGardeAVueAttachments(
  gardeAVueId: number,
): Promise<GardeAVueAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<GardeAVueAttachment[]>>(
    `/garde-a-vue/${gardeAVueId}/attachments`,
  );
  return data.data;
}

export async function createGardeAVueAttachment(
  gardeAVueId: number,
  title: string,
  file: File,
): Promise<GardeAVueAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<GardeAVueAttachment>>(
    `/garde-a-vue/${gardeAVueId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateGardeAVueAttachmentTitle(
  gardeAVueId: number,
  attachId: number,
  title: string,
): Promise<GardeAVueAttachment> {
  const { data } = await apiClient.put<ApiResponse<GardeAVueAttachment>>(
    `/garde-a-vue/${gardeAVueId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteGardeAVueAttachment(
  gardeAVueId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/garde-a-vue/${gardeAVueId}/attachments/${attachId}`,
  );
}

export function getGardeAVueAttachmentDownloadUrl(
  gardeAVueId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/garde-a-vue/${gardeAVueId}/attachments/${attachId}/download`;
}

// ========================
// Constants & validation
// ========================

export function isGardeAVueImageAttachment(
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

export function validateGardeAVueForm(
  input: GardeAVueInput,
): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!input.nom?.trim()) {
    errors.nom = "Le nom est requis";
  }

  if (input.date_naissance) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(input.date_naissance) || isNaN(Date.parse(input.date_naissance))) {
      errors.date_naissance = "La date de naissance est invalide (format attendu : AAAA-MM-JJ)";
    }
  }

  function parseDateTime(value: string | null | undefined): Date | null {
    if (!value || !value.trim()) return null;
    const d = new Date(value.replace(" ", "T"));
    return isNaN(d.getTime()) ? null : d;
  }

  const debut = parseDateTime(input.debut_gav);
  const fin = parseDateTime(input.fin_gav);
  const prolongation = parseDateTime(input.prolongation_gav);

  if (input.debut_gav && !debut) {
    errors.debut_gav = "Le début de la garde à vue est invalide (format attendu : AAAA-MM-JJ HH:MM)";
  }
  if (input.fin_gav && !fin) {
    errors.fin_gav = "La fin de la garde à vue est invalide (format attendu : AAAA-MM-JJ HH:MM)";
  }
  if (input.prolongation_gav && !prolongation) {
    errors.prolongation_gav = "La prolongation est invalide (format attendu : AAAA-MM-JJ HH:MM)";
  }

  if (debut && fin && fin < debut) {
    errors.fin_gav = "La fin de la garde à vue ne peut pas être antérieure au début";
  }
  if (fin && prolongation && prolongation < fin) {
    errors.prolongation_gav = "La prolongation ne peut pas être antérieure à la fin de la garde à vue";
  }

  return errors;
}
