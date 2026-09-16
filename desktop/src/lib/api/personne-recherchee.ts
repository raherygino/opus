import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  PersonneRecherchee,
  PersonneRechercheePhoto,
  PersonneRechercheeInput,
} from "@/types";

// ========================
// Personne Recherchée API
// ========================

export async function getPersonneRechercheeList(
  filters?: Record<string, string>,
): Promise<PersonneRecherchee[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<PersonneRecherchee[]>>(
    `/personne-recherchee${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getPersonneRechercheeById(
  id: number,
): Promise<PersonneRecherchee> {
  const { data } = await apiClient.get<ApiResponse<PersonneRecherchee>>(
    `/personne-recherchee/${id}`,
  );
  return data.data;
}

export async function createPersonneRecherchee(
  input: PersonneRechercheeInput,
): Promise<PersonneRecherchee> {
  const { data } = await apiClient.post<ApiResponse<PersonneRecherchee>>(
    "/personne-recherchee",
    input,
  );
  return data.data;
}

export async function updatePersonneRecherchee(
  id: number,
  input: PersonneRechercheeInput,
): Promise<PersonneRecherchee> {
  const { data } = await apiClient.put<ApiResponse<PersonneRecherchee>>(
    `/personne-recherchee/${id}`,
    input,
  );
  return data.data;
}

export async function deletePersonneRecherchee(id: number): Promise<void> {
  await apiClient.delete(`/personne-recherchee/${id}`);
}

// ========================
// Photos (dedicated multi-image, NOT generic attachments)
// ========================

export async function getPersonneRechercheePhotos(
  personneId: number,
): Promise<PersonneRechercheePhoto[]> {
  const { data } = await apiClient.get<
    ApiResponse<PersonneRechercheePhoto[]>
  >(`/personne-recherchee/${personneId}/photos`);
  return data.data;
}

export async function createPersonneRechercheePhoto(
  personneId: number,
  file: File,
  caption: string | null,
  captureSource: "CAMERA" | "GALLERY" | null,
): Promise<PersonneRechercheePhoto> {
  const formData = new FormData();
  formData.append("caption", caption ?? "");
  formData.append("capture_source", captureSource ?? "");
  formData.append("file", file);
  const { data } = await apiClient.post<ApiResponse<PersonneRechercheePhoto>>(
    `/personne-recherchee/${personneId}/photos`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function updatePersonneRechercheePhotoCaption(
  personneId: number,
  photoId: number,
  caption: string | null,
): Promise<PersonneRechercheePhoto> {
  const { data } = await apiClient.put<ApiResponse<PersonneRechercheePhoto>>(
    `/personne-recherchee/${personneId}/photos/${photoId}`,
    { caption },
  );
  return data.data;
}

export async function deletePersonneRechercheePhoto(
  personneId: number,
  photoId: number,
): Promise<void> {
  await apiClient.delete(`/personne-recherchee/${personneId}/photos/${photoId}`);
}

export function getPersonneRechercheePhotoDownloadUrl(
  personneId: number,
  photoId: number,
): string {
  const baseUrl = import.meta.env.VITE_API_URL || "/api";
  return `${baseUrl}/personne-recherchee/${personneId}/photos/${photoId}/download`;
}

// ========================
// Validation
// ========================

export function validatePersonneRechercheeForm(
  input: PersonneRechercheeInput,
): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!input.nom?.trim()) {
    errors.nom = "Le nom est requis";
  }
  if (!input.motif?.trim()) {
    errors.motif = "Le motif est requis";
  }
  return errors;
}

/**
 * Convert a base64 data URL (from PhotoCaptureDialog) into a File.
 * Mirrors the conversion used by the personnel form.
 */
export function dataUrlToFile(dataUrl: string, filename: string): File {
  const [meta, base64] = dataUrl.split(",");
  const mimeType = meta?.match(/:(.*?);/)?.[1] || "image/jpeg";
  const byteChars = atob(base64);
  const byteNumbers = new Array(byteChars.length);
  for (let i = 0; i < byteChars.length; i++) {
    byteNumbers[i] = byteChars.charCodeAt(i);
  }
  const byteArray = new Uint8Array(byteNumbers);
  const blob = new Blob([byteArray], { type: mimeType });
  return new File([blob], filename, { type: mimeType });
}
