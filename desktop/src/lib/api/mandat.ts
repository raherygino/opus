import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Mandat,
  MandatAttachment,
  MandatInput,
} from "@/types";

// ========================
// Mandat API (Police Judiciaire)
// ========================

export async function getMandatList(
  filters?: Record<string, string>,
): Promise<Mandat[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Mandat[]>>(
    `/mandats${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getMandatById(id: number): Promise<Mandat> {
  const { data } = await apiClient.get<ApiResponse<Mandat>>(
    `/mandats/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next mandat number. */
export async function peekMandatNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/mandats/next-number`,
  );
  return data.data.numero;
}

export async function createMandat(input: MandatInput): Promise<Mandat> {
  const { data } = await apiClient.post<ApiResponse<Mandat>>(
    "/mandats",
    input,
  );
  return data.data;
}

export async function updateMandat(
  id: number,
  input: MandatInput,
): Promise<Mandat> {
  const { data } = await apiClient.put<ApiResponse<Mandat>>(
    `/mandats/${id}`,
    input,
  );
  return data.data;
}

export async function deleteMandat(id: number): Promise<void> {
  await apiClient.delete(`/mandats/${id}`);
}

// ========================
// Mandat Attachments
// ========================

export async function getMandatAttachments(
  mandatId: number,
): Promise<MandatAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<MandatAttachment[]>>(
    `/mandats/${mandatId}/attachments`,
  );
  return data.data;
}

export async function createMandatAttachment(
  mandatId: number,
  title: string,
  file: File,
): Promise<MandatAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<MandatAttachment>>(
    `/mandats/${mandatId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateMandatAttachmentTitle(
  mandatId: number,
  attachId: number,
  title: string,
): Promise<MandatAttachment> {
  const { data } = await apiClient.put<ApiResponse<MandatAttachment>>(
    `/mandats/${mandatId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteMandatAttachment(
  mandatId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(`/mandats/${mandatId}/attachments/${attachId}`);
}

export function getMandatAttachmentDownloadUrl(
  mandatId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/mandats/${mandatId}/attachments/${attachId}/download`;
}

// ========================
// Helpers & validation
// ========================

export function isMandatImageAttachment(
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

export function validateMandatForm(
  input: MandatInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.type) {
    errors.type = "Le type de mandat est requis";
  }
  if (!input.personne_nom?.trim()) {
    errors.personne_nom = "Le nom et prénom de la personne concernée sont requis";
  }
  return errors;
}
