import { apiClient } from "@/lib/api-client";
import type { ApiResponse, Comportement } from "@/types";

export async function getComportementList(
  filters?: Record<string, string>,
): Promise<Comportement[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<Comportement[]>>(
    `/comportements${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getComportementById(id: number): Promise<Comportement> {
  const { data } = await apiClient.get<ApiResponse<Comportement>>(
    `/comportements/${id}`,
  );
  return data.data;
}

export async function createComportement(
  comportement: Omit<Comportement, "id" | "created_at" | "updated_at">,
): Promise<Comportement> {
  const { data } = await apiClient.post<ApiResponse<Comportement>>(
    "/comportements",
    comportement,
  );
  return data.data;
}

export async function updateComportement(
  id: number,
  comportement: Partial<Comportement>,
): Promise<Comportement> {
  const { data } = await apiClient.put<ApiResponse<Comportement>>(
    `/comportements/${id}`,
    comportement,
  );
  return data.data;
}

export async function deleteComportement(id: number): Promise<void> {
  await apiClient.delete(`/comportements/${id}`);
}
