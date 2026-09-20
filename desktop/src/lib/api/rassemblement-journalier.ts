import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  RassemblementJournalier,
  RassemblementJournalierInput,
} from "@/types";

// ========================
// Rassemblement Journalier API
// ========================

export async function getRassemblementList(
  filters?: Record<string, string>,
): Promise<RassemblementJournalier[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<RassemblementJournalier[]>>(
    `/rassemblements${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getRassemblementById(
  id: number,
): Promise<RassemblementJournalier> {
  const { data } = await apiClient.get<ApiResponse<RassemblementJournalier>>(
    `/rassemblements/${id}`,
  );
  return data.data;
}

export async function createRassemblement(
  input: RassemblementJournalierInput,
): Promise<RassemblementJournalier> {
  const { data } = await apiClient.post<ApiResponse<RassemblementJournalier>>(
    `/rassemblements`,
    input,
  );
  return data.data;
}

export async function updateRassemblement(
  id: number,
  input: RassemblementJournalierInput,
): Promise<RassemblementJournalier> {
  const { data } = await apiClient.put<ApiResponse<RassemblementJournalier>>(
    `/rassemblements/${id}`,
    input,
  );
  return data.data;
}

export async function deleteRassemblement(id: number): Promise<void> {
  await apiClient.delete<ApiResponse<null>>(`/rassemblements/${id}`);
}
