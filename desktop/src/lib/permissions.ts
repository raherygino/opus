import type { User } from "@/types";

export type PermissionAction = "can_view" | "can_create" | "can_edit" | "can_delete" | "can_export";

export function hasPermission(
  user: User | null,
  module: string,
  action: PermissionAction,
): boolean {
  if (!user) return false;
  if (user.role_code === "SUPER_ADMIN") return true;
  if (!user.permissions) return false;
  const perm = user.permissions.find((p) => p.module === module);
  if (!perm) return false;
  return perm[action] === 1;
}

export function hasAnyPermission(
  user: User | null,
  module: string,
  actions: PermissionAction[],
): boolean {
  return actions.some((action) => hasPermission(user, module, action));
}
