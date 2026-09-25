import { apiClient } from "@/lib/api-client";
import type { ApiResponse, DashboardStats } from "@/types";

/**
 * GET /api/dashboard/stats — aggregated KPI counts for the main dashboard.
 * Single request computing every figure server-side.
 */
export async function getDashboardStats(): Promise<DashboardStats> {
  const { data } = await apiClient.get<ApiResponse<DashboardStats>>(
    "/dashboard/stats",
  );
  return data.data;
}
