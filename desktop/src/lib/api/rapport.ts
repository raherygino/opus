import { apiClient } from "@/lib/api-client";
import type { ApiResponse, Rapport, RapportType } from "@/types";

/**
 * GET /api/rapports — aggregated record counts per Sédentaire module for a
 * daily, weekly (ISO Mon–Sun) or monthly period around the given anchor
 * date (Y-m-d). Single request computing every figure server-side, so
 * Desktop and Android display identical data.
 */
export async function getRapport(
  type: RapportType,
  date: string,
): Promise<Rapport> {
  const params = new URLSearchParams({ type, date });
  const { data } = await apiClient.get<ApiResponse<Rapport>>(
    `/rapports?${params.toString()}`,
  );
  return data.data;
}
