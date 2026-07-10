import { apiClient } from "@/lib/api-client";
import type { ApiResponse, Role, RolePermission } from "@/types";

export async function getRoleList(): Promise<Role[]> {
  const { data } = await apiClient.get<ApiResponse<Role[]>>("/roles");
  return data.data;
}

export async function getRoleById(id: number): Promise<Role> {
  const { data } = await apiClient.get<ApiResponse<Role>>(`/roles/${id}`);
  return data.data;
}

export async function createRole(role: {
  code: string;
  name: string;
  description?: string;
}): Promise<Role> {
  const { data } = await apiClient.post<ApiResponse<Role>>("/roles", role);
  return data.data;
}

export async function updateRole(
  id: number,
  role: Partial<{
    code: string;
    name: string;
    description: string;
  }>,
): Promise<Role> {
  const { data } = await apiClient.put<ApiResponse<Role>>(`/roles/${id}`, role);
  return data.data;
}

export async function deleteRole(id: number): Promise<void> {
  await apiClient.delete(`/roles/${id}`);
}

export async function getRolePermissions(
  id: number,
): Promise<{ role: Role; permissions: RolePermission[] }> {
  const { data } = await apiClient.get<
    ApiResponse<{ role: Role; permissions: RolePermission[] }>
  >(`/roles/${id}/permissions`);
  return data.data;
}

export async function updateRolePermissions(
  id: number,
  permissions: Record<string, { can_view: number; can_create: number; can_edit: number; can_delete: number; can_export: number }>,
): Promise<RolePermission[]> {
  const { data } = await apiClient.put<
    ApiResponse<RolePermission[]>
  >(`/roles/${id}/permissions`, { permissions });
  return data.data;
}
