import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Requisition,
  RequisitionAttachment,
  RequisitionInput,
} from "@/types";

// ========================
// Requisition API
// ========================

export async function getRequisitionList(
  filters?: Record<string, string>,
): Promise<Requisition[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Requisition[]>>(
    `/requisitions${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getRequisitionById(id: number): Promise<Requisition> {
  const { data } = await apiClient.get<ApiResponse<Requisition>>(
    `/requisitions/${id}`,
  );
  return data.data;
}

/** Peek at the suggested next requisition number. */
export async function peekRequisitionNumber(): Promise<string> {
  const { data } = await apiClient.get<ApiResponse<{ numero: string }>>(
    `/requisitions/next-number`,
  );
  return data.data.numero;
}

export async function createRequisition(
  input: RequisitionInput,
): Promise<Requisition> {
  const { data } = await apiClient.post<ApiResponse<Requisition>>(
    "/requisitions",
    input,
  );
  return data.data;
}

export async function updateRequisition(
  id: number,
  input: RequisitionInput,
): Promise<Requisition> {
  const { data } = await apiClient.put<ApiResponse<Requisition>>(
    `/requisitions/${id}`,
    input,
  );
  return data.data;
}

export async function deleteRequisition(id: number): Promise<void> {
  await apiClient.delete(`/requisitions/${id}`);
}

// ========================
// Requisition Attachments
// ========================

export async function getRequisitionAttachments(
  requisitionId: number,
): Promise<RequisitionAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<RequisitionAttachment[]>>(
    `/requisitions/${requisitionId}/attachments`,
  );
  return data.data;
}

export async function createRequisitionAttachment(
  requisitionId: number,
  title: string,
  file: File,
): Promise<RequisitionAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<RequisitionAttachment>>(
    `/requisitions/${requisitionId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateRequisitionAttachmentTitle(
  requisitionId: number,
  attachId: number,
  title: string,
): Promise<RequisitionAttachment> {
  const { data } = await apiClient.put<ApiResponse<RequisitionAttachment>>(
    `/requisitions/${requisitionId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteRequisitionAttachment(
  requisitionId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/requisitions/${requisitionId}/attachments/${attachId}`,
  );
}

export function getRequisitionAttachmentDownloadUrl(
  requisitionId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/requisitions/${requisitionId}/attachments/${attachId}/download`;
}

// ========================
// Constants & validation
// ========================

export const REQUISITION_TYPES = ["TPH", "MEDECIN_LEGISTE", "CIM", "AUTRE"] as const;
export const REQUISITION_TYPE_LABELS: Record<string, string> = {
  TPH: "TPH",
  MEDECIN_LEGISTE: "Médecin légiste",
  CIM: "CIM",
  AUTRE: "Autre",
};

export function isRequisitionImageAttachment(
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

export function validateRequisitionForm(
  input: RequisitionInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.type) {
    errors.type = "Le type de réquisition est requis";
  } else if (!REQUISITION_TYPES.includes(input.type as never)) {
    errors.type = "Le type de réquisition est invalide";
  }
  if (!input.date_requisition) {
    errors.date_requisition = "La date est requise";
  } else if (!/^\d{4}-\d{2}-\d{2}$/.test(input.date_requisition) || isNaN(Date.parse(input.date_requisition))) {
    errors.date_requisition = "La date est invalide (format attendu : AAAA-MM-JJ)";
  }
  if (!input.affaire?.trim()) {
    errors.affaire = "L'affaire est requise";
  }
  return errors;
}
