import { apiClient } from "@/lib/api-client";
import type { ApiResponse, Personnel, PersonnelAttachment } from "@/types";

export async function getPersonnelList(
  filters?: Record<string, string>,
): Promise<Personnel[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Personnel[]>>(
    `/personnel${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getAvailablePersonnel(): Promise<Personnel[]> {
  const { data } = await apiClient.get<ApiResponse<Personnel[]>>(
    "/personnel/available",
  );
  return data.data;
}

export async function getPersonnelById(id: number): Promise<Personnel> {
  const { data } = await apiClient.get<ApiResponse<Personnel>>(
    `/personnel/${id}`,
  );
  return data.data;
}

export async function createPersonnel(
  personnel: Omit<Personnel, "id" | "created_at" | "updated_at" | "photo" | "signature" | "status"> & {
    photo?: string;
    signature?: string;
  },
): Promise<Personnel> {
  const { data } = await apiClient.post<ApiResponse<Personnel>>(
    "/personnel",
    personnel,
  );
  return data.data;
}

export async function updatePersonnel(
  id: number,
  personnel: Partial<Personnel>,
): Promise<Personnel> {
  const { data } = await apiClient.put<ApiResponse<Personnel>>(
    `/personnel/${id}`,
    personnel,
  );
  return data.data;
}

export async function deletePersonnel(id: number): Promise<void> {
  await apiClient.delete(`/personnel/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getPersonnelAttachments(
  personnelId: number,
): Promise<PersonnelAttachment[]> {
  const { data } = await apiClient.get<
    ApiResponse<PersonnelAttachment[]>
  >(`/personnel/${personnelId}/attachments`);
  return data.data;
}

export async function createPersonnelAttachment(
  personnelId: number,
  title: string,
  file: File,
): Promise<PersonnelAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<
    ApiResponse<PersonnelAttachment>
  >(`/personnel/${personnelId}/attachments`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data.data;
}

export async function updatePersonnelAttachmentTitle(
  personnelId: number,
  attachId: number,
  title: string,
): Promise<PersonnelAttachment> {
  const { data } = await apiClient.put<
    ApiResponse<PersonnelAttachment>
  >(`/personnel/${personnelId}/attachments/${attachId}`, { title });
  return data.data;
}

export async function deletePersonnelAttachment(
  personnelId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/personnel/${personnelId}/attachments/${attachId}`,
  );
}

export function getAttachmentDownloadUrl(
  personnelId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/personnel/${personnelId}/attachments/${attachId}/download`;
}

// ========================
// Photo API
// ========================

export async function uploadPersonnelPhoto(
  personnelId: number,
  photo: File,
  thumbnail?: File,
): Promise<Personnel> {
  const formData = new FormData();
  formData.append("photo", photo);
  if (thumbnail) {
    formData.append("thumbnail", thumbnail);
  }
  const { data } = await apiClient.post<ApiResponse<Personnel>>(
    `/personnel/${personnelId}/photo`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function generateThumbnail(
  file: File,
  maxSize: number = 200,
): Promise<File> {
  const img = await createImageBitmap(file);
  const canvas = document.createElement("canvas");
  let w = img.width;
  let h = img.height;
  if (w > h) {
    if (w > maxSize) { h = (h / w) * maxSize; w = maxSize; }
  } else {
    if (h > maxSize) { w = (w / h) * maxSize; h = maxSize; }
  }
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d")!;
  ctx.drawImage(img, 0, 0, w, h);
  img.close();
  const blob = await new Promise<Blob>((resolve) =>
    canvas.toBlob((b) => resolve(b!), "image/jpeg", 70),
  );
  return new File([blob], "thumbnail.jpg", { type: "image/jpeg" });
}

export async function cropFileToSquare(
  file: File,
  offsetX: number = 0.5,
  offsetY: number = 0.5,
  quality: number = 90,
): Promise<File> {
  const img = await createImageBitmap(file);
  const side = Math.min(img.width, img.height);
  const sx = Math.round(offsetX * (img.width - side));
  const sy = Math.round(offsetY * (img.height - side));
  const canvas = document.createElement("canvas");
  canvas.width = side;
  canvas.height = side;
  const ctx = canvas.getContext("2d")!;
  ctx.drawImage(img, sx, sy, side, side, 0, 0, side, side);
  img.close();
  const blob = await new Promise<Blob>((resolve) =>
    canvas.toBlob((b) => resolve(b!), "image/jpeg", quality),
  );
  return new File([blob], "photo.jpg", { type: "image/jpeg" });
}

export function getPersonnelPhotoUrl(personnelId: number): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/personnel/${personnelId}/photo`;
}

export function getPersonnelThumbnailUrl(personnelId: number): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/personnel/${personnelId}/thumbnail`;
}

// ========================
// Signature API
// ========================

export async function uploadPersonnelSignature(
  personnelId: number,
  signature: File,
): Promise<Personnel> {
  const formData = new FormData();
  formData.append("signature", signature);
  const { data } = await apiClient.post<ApiResponse<Personnel>>(
    `/personnel/${personnelId}/signature`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export function getPersonnelSignatureUrl(personnelId: number): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/personnel/${personnelId}/signature`;
}

export async function savePersonnelSignatureSvg(
  personnelId: number,
  svg: string,
): Promise<Personnel> {
  const { data } = await apiClient.post<ApiResponse<Personnel>>(
    `/personnel/${personnelId}/signature/svg`,
    { svg },
  );
  return data.data;
}
