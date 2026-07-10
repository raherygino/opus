import { apiClient } from "@/lib/api-client";
import type { ApiResponse, AuditLog, AuditLogFilters } from "@/types";

export async function getAuditLogs(filters?: AuditLogFilters): Promise<AuditLog[]> {
  const params = new URLSearchParams();
  if (filters?.action) params.set("action", filters.action);
  if (filters?.module) params.set("module", filters.module);
  if (filters?.user_id) params.set("user_id", filters.user_id);
  if (filters?.search) params.set("search", filters.search);
  if (filters?.date_from) params.set("date_from", filters.date_from);
  if (filters?.date_to) params.set("date_to", filters.date_to);

  const query = params.toString() ? `?${params.toString()}` : "";
  const { data } = await apiClient.get<ApiResponse<AuditLog[]>>(`/audit-logs${query}`);
  return data.data;
}

export async function getAuditLogById(id: number): Promise<AuditLog> {
  const { data } = await apiClient.get<ApiResponse<AuditLog>>(`/audit-logs/${id}`);
  return data.data;
}
