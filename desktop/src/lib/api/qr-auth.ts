import { apiClient } from "@/lib/api-client";
import type {
  ApiResponse,
  AuthResponse,
  QrAuthDeviceType,
  QrAuthRequestResponse,
  QrAuthScanResponse,
  QrAuthStatusResponse,
} from "@/types";

/**
 * QR-code-based authentication API.
 *
 * The QR code only ever carries a short-lived, one-time `request_code`.
 * Tokens are issued server-side only after the phone explicitly approves.
 */

/** Create a pending QR auth request. Returns the code to embed in the QR. */
export async function createQrAuthRequest(
  deviceType: QrAuthDeviceType,
  deviceName: string,
): Promise<QrAuthRequestResponse> {
  const { data } = await apiClient.post<ApiResponse<QrAuthRequestResponse>>(
    "/qr-auth/request",
    { device_type: deviceType, device_name: deviceName },
  );
  return data.data;
}

/** Poll the status of a QR auth request. Returns tokens on first approved poll. */
export async function getQrAuthStatus(
  code: string,
): Promise<QrAuthStatusResponse> {
  const { data } = await apiClient.get<ApiResponse<QrAuthStatusResponse>>(
    `/qr-auth/${code}`,
  );
  return data.data;
}

/** Phone calls this after scanning to mark the request as scanned. */
export async function scanQrAuth(
  code: string,
): Promise<QrAuthScanResponse> {
  const { data } = await apiClient.post<ApiResponse<QrAuthScanResponse>>(
    `/qr-auth/${code}/scan`,
  );
  return data.data;
}

/** Phone calls this to approve the request (requires phone's Bearer token). */
export async function approveQrAuth(
  code: string,
): Promise<AuthResponse | { device_type: string; device_name: string }> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse | { device_type: string; device_name: string }>>(
    `/qr-auth/${code}/approve`,
  );
  return data.data;
}

/** Phone calls this to reject the request. */
export async function rejectQrAuth(code: string): Promise<void> {
  await apiClient.post(`/qr-auth/${code}/reject`);
}

/** Requesting device cancels the request. */
export async function cancelQrAuth(code: string): Promise<void> {
  await apiClient.post(`/qr-auth/${code}/cancel`);
}
