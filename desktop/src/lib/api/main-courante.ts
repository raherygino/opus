import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  MainCourante,
  MainCouranteAttachment,
  MainCouranteCategorie,
  MainCouranteCategorieItem,
  MainCouranteOrigine,
} from "@/types";

export interface MainCourantePayload {
  date_evenement: string;
  heure_evenement: string;
  categorie: MainCouranteCategorie;
  description: string;
  origine?: MainCouranteOrigine;
}

export async function getMainCouranteList(
  filters?: Record<string, string>,
): Promise<MainCourante[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<MainCourante[]>>(
    `/main-courante${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getMainCouranteById(
  id: number,
): Promise<MainCourante> {
  const { data } = await apiClient.get<ApiResponse<MainCourante>>(
    `/main-courante/${id}`,
  );
  return data.data;
}

export async function createMainCourante(
  mainCourante: MainCourantePayload,
): Promise<MainCourante> {
  const { data } = await apiClient.post<ApiResponse<MainCourante>>(
    "/main-courante",
    mainCourante,
  );
  return data.data;
}

export async function updateMainCourante(
  id: number,
  mainCourante: Partial<MainCourantePayload>,
): Promise<MainCourante> {
  const { data } = await apiClient.put<ApiResponse<MainCourante>>(
    `/main-courante/${id}`,
    mainCourante,
  );
  return data.data;
}

export async function deleteMainCourante(id: number): Promise<void> {
  await apiClient.delete(`/main-courante/${id}`);
}

// ========================
// Attachment API
// ========================

export async function getMainCouranteAttachments(
  mainCouranteId: number,
): Promise<MainCouranteAttachment[]> {
  const { data } = await apiClient.get<ApiResponse<MainCouranteAttachment[]>>(
    `/main-courante/${mainCouranteId}/attachments`,
  );
  return data.data;
}

export async function createMainCouranteAttachment(
  mainCouranteId: number,
  title: string,
  file: File,
): Promise<MainCouranteAttachment> {
  const formData = new FormData();
  formData.append("title", title);
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<MainCouranteAttachment>>(
    `/main-courante/${mainCouranteId}/attachments`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updateMainCouranteAttachmentTitle(
  mainCouranteId: number,
  attachId: number,
  title: string,
): Promise<MainCouranteAttachment> {
  const { data } = await apiClient.put<ApiResponse<MainCouranteAttachment>>(
    `/main-courante/${mainCouranteId}/attachments/${attachId}`,
    { title },
  );
  return data.data;
}

export async function deleteMainCouranteAttachment(
  mainCouranteId: number,
  attachId: number,
): Promise<void> {
  await apiClient.delete(
    `/main-courante/${mainCouranteId}/attachments/${attachId}`,
  );
}

export function getMainCouranteAttachmentDownloadUrl(
  mainCouranteId: number,
  attachId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/main-courante/${mainCouranteId}/attachments/${attachId}/download`;
}

const IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg"];

/** Whether an attachment is an image that can be previewed in-app. */
export function isMainCouranteImageAttachment(
  attachment: Pick<MainCouranteAttachment, "mime_type" | "original_filename">,
): boolean {
  if (attachment.mime_type?.startsWith("image/")) return true;
  const ext = attachment.original_filename?.split(".").pop()?.toLowerCase();
  return !!ext && IMAGE_EXTENSIONS.includes(ext);
}

// ========================
// Categories API (user-managed label catalog)
// ========================

export async function getMainCouranteCategories(): Promise<MainCouranteCategorieItem[]> {
  const { data } = await apiClient.get<ApiResponse<MainCouranteCategorieItem[]>>(
    "/main-courante-categories",
  );
  return data.data;
}

export async function createMainCouranteCategorie(
  label: string,
): Promise<MainCouranteCategorieItem> {
  const { data } = await apiClient.post<ApiResponse<MainCouranteCategorieItem>>(
    "/main-courante-categories",
    { label },
  );
  return data.data;
}

export async function updateMainCouranteCategorie(
  id: number,
  label: string,
): Promise<MainCouranteCategorieItem> {
  const { data } = await apiClient.put<ApiResponse<MainCouranteCategorieItem>>(
    `/main-courante-categories/${id}`,
    { label },
  );
  return data.data;
}

export async function deleteMainCouranteCategorie(id: number): Promise<void> {
  await apiClient.delete(`/main-courante-categories/${id}`);
}
