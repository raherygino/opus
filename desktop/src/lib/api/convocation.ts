import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Convocation,
  ConvocationAttachment,
  ConvocationInput,
} from "@/types";

// ========================
// Convocation API
// ========================

export async function getConvocationList(
  filters?: Record<string, string>,
): Promise<Convocation[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Convocation[]>>(
    `/convocations${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getConvocationById(id: number): Promise<Convocation> {
  const { data } = await apiClient.get<ApiResponse<Convocation>>(
    `/convocations/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next convocation number for the given type. */
export async function peekConvocationNumber(type: string): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/convocations/next-number?type=${encodeURIComponent(type)}`,
  );
  return data.data.numero;
}

export async function createConvocation(
  input: ConvocationInput,
): Promise<Convocation> {
  const { data } = await apiClient.post<ApiResponse<Convocation>>(
    "/convocations",
    input,
  );
  return data.data;
}

export async function updateConvocation(
  id: number,
  input: ConvocationInput,
): Promise<Convocation> {
  const { data } = await apiClient.put<ApiResponse<Convocation>>(
    `/convocations/${id}`,
    input,
  );
  return data.data;
}

export async function deleteConvocation(id: number): Promise<void> {
  await apiClient.delete(`/convocations/${id}`);
}

// ========================
// Convocation Attachments
// ========================

export async function getConvocationAttachments(
  convocationId: number,
): Promise<ConvocationAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ConvocationAttachment[]>>(
    `/convocations/${convocationId}/attachments`,
  );
  return data.data;
}

export async function createConvocationAttachment(
  convocationId: number,
  title: string,
  file: File,
): Promise<ConvocationAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ConvocationAttachment>>(
    `/convocations/${convocationId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateConvocationAttachmentTitle(
  convocationId: number,
  attachId: number,
  title: string,
): Promise<ConvocationAttachment> {
  const { data } = await apiClient.put<ApiResponse<ConvocationAttachment>>(
    `/convocations/${convocationId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteConvocationAttachment(
  convocationId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/convocations/${convocationId}/attachments/${attachId}`,
  );
}

export function getConvocationAttachmentDownloadUrl(
  convocationId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/convocations/${convocationId}/attachments/${attachId}/download`;
}

// ========================
// Constants & validation
// ========================

export const CONVOCATION_TYPES = ["ST_PARQUET", "PLAINTE_DIRECTE"] as const;
export const CONVOCATION_TYPE_LABELS: Record<string, string> = {
  ST_PARQUET: "ST Parquet",
  PLAINTE_DIRECTE: "Plainte directe",
};

export function isConvocationImageAttachment(
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

export function validateConvocationForm(
  input: ConvocationInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.type) {
    errors.type = "Le type de convocation est requis";
  } else if (!CONVOCATION_TYPES.includes(input.type as never)) {
    errors.type = "Le type de convocation est invalide";
  }
  if (!input.date_convocation) {
    errors.date_convocation = "La date est requise";
  } else if (!/^\d{4}-\d{2}-\d{2}$/.test(input.date_convocation) || isNaN(Date.parse(input.date_convocation))) {
    errors.date_convocation = "La date est invalide (format attendu : AAAA-MM-JJ)";
  }
  if (!input.nom?.trim()) {
    errors.nom = "Le nom est requis";
  }
  if (!input.infraction?.trim()) {
    errors.infraction = "L'infraction est requise";
  }
  return errors;
}
