import { apiClient } from "@/lib/api-client";
import type { ApiResponse, AuthResponse, User, VerifiedIdentity } from "@/types";

export async function login(
  username: string,
  password: string,
): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
    "/auth/login",
    { username, password },
  );
  return data.data;
}

/**
 * Verify a user's credentials WITHOUT creating a session. Used by the
 * passation flow to authenticate the "chef de poste montant" mid-flow: the
 * caller (chef descendant) is already authenticated, and we only need to
 * confirm the montant's identity and retrieve their grade/firstname.
 * The password is never stored or logged.
 */
export async function verifyIdentity(
  username: string,
  password: string,
): Promise<VerifiedIdentity> {
  const { data } = await apiClient.post<ApiResponse<VerifiedIdentity>>(
    "/auth/verify",
    { username, password },
  );
  return data.data;
}

export async function refreshToken(
  refresh_token: string,
): Promise<{ access_token: string }> {
  const { data } = await apiClient.post<ApiResponse<{ access_token: string }>>(
    "/auth/refresh",
    { refresh_token },
  );
  return data.data;
}

export async function getMe(): Promise<User> {
  const { data } = await apiClient.get<ApiResponse<User>>("/auth/me");
  return data.data;
}

export async function changePassword(
  currentPassword: string,
  newPassword: string,
): Promise<void> {
  await apiClient.put("/auth/password", {
    current_password: currentPassword,
    new_password: newPassword,
  });
}

export async function uploadProfilePhoto(photo: File, thumbnail?: File): Promise<User> {
  const formData = new FormData();
  formData.append("photo", photo);
  if (thumbnail) {
    formData.append("thumbnail", thumbnail);
  }
  const { data } = await apiClient.post<ApiResponse<User>>(
    "/auth/photo",
    formData,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data.data;
}

export async function deleteProfilePhoto(): Promise<User> {
  const { data } = await apiClient.delete<ApiResponse<User>>("/auth/photo");
  return data.data;
}
