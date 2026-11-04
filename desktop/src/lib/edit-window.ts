import type { User } from "@/types";

/**
 * 24-hour edit window.
 *
 * Business records may only be modified within 24 hours of their creation
 * (created_at). After that they become read-only. Administrators
 * (SUPER_ADMIN / STATION_ADMIN) bypass the lock.
 *
 * This is a UX mirror of the server-side guard (api/src/Helpers/EditWindow.php)
 * which is the authoritative enforcement — the API rejects any mutation of an
 * expired record with 403 even if a client lets it through.
 */

export const EDIT_WINDOW_MS = 24 * 60 * 60 * 1000;

export function isAdminUser(user: User | null | undefined): boolean {
  return user?.role_code === "SUPER_ADMIN" || user?.role_code === "STATION_ADMIN";
}

/**
 * True while the record is still inside its 24h edit window.
 * created_at comes back from the API as "YYYY-MM-DD HH:MM:SS" (server time);
 * a missing/unparseable value stays editable (the API decides anyway).
 */
export function isWithinEditWindow(createdAt: string | null | undefined): boolean {
  if (!createdAt) return true;
  const created = new Date(createdAt.replace(" ", "T"));
  if (Number.isNaN(created.getTime())) return true;
  return Date.now() - created.getTime() <= EDIT_WINDOW_MS;
}

/**
 * Convenience guard: admins always pass; everyone else only inside the window.
 */
export function canEditWithinWindow(
  user: User | null | undefined,
  createdAt: string | null | undefined,
): boolean {
  return isAdminUser(user) || isWithinEditWindow(createdAt);
}

export const EDIT_WINDOW_CLOSED_MESSAGE =
  "Le délai de modification de 24 heures est écoulé — cet enregistrement est en lecture seule.";
