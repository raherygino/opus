import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  RegistreDeferrement,
  RegistreDeferrementAttachment,
  RegistreDeferrementInput,
} from "@/types";

// ========================
// Registre de déferrement API (Police Judiciaire)
// ========================

export async function getRegistreDeferrementList(
  filters?: Record<string, string>,
): Promise<RegistreDeferrement[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<RegistreDeferrement[]>>(
    `/registres-deferrement${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getRegistreDeferrementById(id: number): Promise<RegistreDeferrement> {
  const { data } = await apiClient.get<ApiResponse<RegistreDeferrement>>(
    `/registres-deferrement/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next registre de déferrement number. */
export async function peekRegistreDeferrementNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/registres-deferrement/next-number`,
  );
  return data.data.numero;
}

export async function createRegistreDeferrement(
  input: RegistreDeferrementInput,
): Promise<RegistreDeferrement> {
  const { data } = await apiClient.post<ApiResponse<RegistreDeferrement>>(
    "/registres-deferrement",
    input,
  );
  return data.data;
}

export async function updateRegistreDeferrement(
  id: number,
  input: RegistreDeferrementInput,
): Promise<RegistreDeferrement> {
  const { data } = await apiClient.put<ApiResponse<RegistreDeferrement>>(
    `/registres-deferrement/${id}`,
    input,
  );
  return data.data;
}

export async function deleteRegistreDeferrement(id: number): Promise<void> {
  await apiClient.delete(`/registres-deferrement/${id}`);
}

// ========================
// Registre de déferrement Attachments
// ========================

export async function getRegistreDeferrementAttachments(
  deferrementId: number,
): Promise<RegistreDeferrementAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<RegistreDeferrementAttachment[]>>(
    `/registres-deferrement/${deferrementId}/attachments`,
  );
  return data.data;
}

export async function createRegistreDeferrementAttachment(
  deferrementId: number,
  title: string,
  file: File,
): Promise<RegistreDeferrementAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<RegistreDeferrementAttachment>>(
    `/registres-deferrement/${deferrementId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateRegistreDeferrementAttachmentTitle(
  deferrementId: number,
  attachId: number,
  title: string,
): Promise<RegistreDeferrementAttachment> {
  const { data } = await apiClient.put<ApiResponse<RegistreDeferrementAttachment>>(
    `/registres-deferrement/${deferrementId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteRegistreDeferrementAttachment(
  deferrementId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/registres-deferrement/${deferrementId}/attachments/${attachId}`,
  );
}

export function getRegistreDeferrementAttachmentDownloadUrl(
  deferrementId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/registres-deferrement/${deferrementId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isRegistreDeferrementImageAttachment(
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

export function validateRegistreDeferrementForm(
  input: RegistreDeferrementInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.date_heure_deferrement?.trim()) {
    errors.date_heure_deferrement = "La date et l'heure du déferrement sont requises";
  }
  if (!input.personne_nom?.trim()) {
    errors.personne_nom = "Le nom et prénom de la personne déférée sont requis";
  }
  return errors;
}
