import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  DispositifExceptionnel,
  DispositifExceptionnelInput,
} from "@/types";

// ========================
// Dispositif exceptionnel API (Service Général)
// ========================

export async function getDispositifExceptionnelList(
  filters?: Record<string, string>,
): Promise<DispositifExceptionnel[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<DispositifExceptionnel[]>>(
    `/dispositifs-exceptionnels${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getDispositifExceptionnelById(
  id: number,
): Promise<DispositifExceptionnel> {
  const { data } = await apiClient.get<ApiResponse<DispositifExceptionnel>>(
    `/dispositifs-exceptionnels/${id}`,
  );
  return data.data;
}

export async function createDispositifExceptionnel(
  input: DispositifExceptionnelInput,
): Promise<DispositifExceptionnel> {
  const { data } = await apiClient.post<ApiResponse<DispositifExceptionnel>>(
    `/dispositifs-exceptionnels`,
    input,
  );
  return data.data;
}

export async function updateDispositifExceptionnel(
  id: number,
  input: DispositifExceptionnelInput,
): Promise<DispositifExceptionnel> {
  const { data } = await apiClient.put<ApiResponse<DispositifExceptionnel>>(
    `/dispositifs-exceptionnels/${id}`,
    input,
  );
  return data.data;
}

export async function deleteDispositifExceptionnel(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/dispositifs-exceptionnels/${id}`);
}
