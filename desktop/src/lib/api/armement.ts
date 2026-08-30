import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  Armement,
  ArmementAttachment,
} from "@/types";

export interface ArmementPayload {
  date_perception: string;
  heure_perception: string;
  // The agent preneur identity (IM + grade + nom) is snapshotted
  // server-side from the personnel table — only the id is sent.
  agent_preneur_personnel_id: number;
  type_arme: string;
  matricule_arme: string;
  munitions: number | null;
  secteur_mission: string;
  etat_perception: string;
  // The agent preneur's code secret — verified server-side before the
  // perception is created. Required on create, ignored on update.
  code_secret?: string;
  // SVG vector data of the agent signature captured after verification.
  // Optional on create, ignored on update (one-way field).
  signature_svg?: string | null;
}

/** The three fields of the reintegration transition. */
export interface ReintegrationPayload {
  heure_reintegration: string;
  etat_reintegration: string;
  munitions_consommees: number;
}

export async function getArmementList(
  filters?: Record<string, string>,
): Promise<Armement[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Armement[]>>(
    `/armements${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getArmementById(id: number): Promise<Armement> {
  const { data } = await apiClient.get<ApiResponse<Armement>>(
    `/armements/${id}`,
  );
  return data.data;
}

export async function createArmement(
  armement: ArmementPayload,
): Promise<Armement> {
  const { data } = await apiClient.post<ApiResponse<Armement>>(
    "/armements",
    armement,
  );
  return data.data;
}

export async function updateArmement(
  id: number,
  armement: Partial<ArmementPayload>,
): Promise<Armement> {
  const { data } = await apiClient.put<ApiResponse<Armement>>(
    `/armements/${id}`,
    armement,
  );
  return data.data;
}

export async function reintegrateArmement(
  id: number,
  payload: ReintegrationPayload,
): Promise<Armement> {
  const { data } = await apiClient.post<ApiResponse<Armement>>(
    `/armements/${id}/reintegration`,
    payload,
  );
  return data.data;
}

export async function deleteArmement(id: number): Promise<void> {
  await apiClient.delete(`/armements/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getArmementAttachments(
  armementId: number,
): Promise<ArmementAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<ArmementAttachment[]>>(
    `/armements/${armementId}/attachments`,
  );
  return data.data;
}

export async function createArmementAttachment(
  armementId: number,
  title: string,
  file: File,
): Promise<ArmementAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<ArmementAttachment>>(
    `/armements/${armementId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateArmementAttachmentTitle(
  armementId: number,
  attachId: number,
  title: string,
): Promise<ArmementAttachment> {
  const { data } = await apiClient.put<ApiResponse<ArmementAttachment>>(
    `/armements/${armementId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteArmementAttachment(
  armementId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/armements/${armementId}/attachments/${attachId}`,
  );
}

export function getArmementAttachmentDownloadUrl(
  armementId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/armements/${armementId}/attachments/${attachId}/download`;
}
