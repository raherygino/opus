import { apiClient } from "@/lib/api-client";
import type { ApiResponse, AppNotification } from "@/types";

export async function getNotifications(
  filters?: Record<string, string>,
): Promise<AppNotification[]> {
  const params = new URLSearchParams(filters);
  const query = params.toString();
  const { data } = await apiClient.get<ApiResponse<AppNotification[]>>(
    `/notifications${query ? `?${query}` : ""}`,
  );
  return data.data;
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await apiClient.get<ApiResponse<{ count: number }>>(
    `/notifications/unread-count`,
  );
  return data.data.count;
}

export async function markNotificationAsRead(id: number): Promise<AppNotification> {
  const { data } = await apiClient.put<ApiResponse<AppNotification>>(
    `/notifications/${id}/read`,
  );
  return data.data;
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await apiClient.put(`/notifications/read-all`);
}

export async function deleteNotification(id: number): Promise<void> {
  await apiClient.delete(`/notifications/${id}`);
}
