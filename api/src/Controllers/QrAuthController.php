<?php

namespace App\Controllers;

use App\Helpers\JWT;
use App\Helpers\Response;
use App\Models\QrAuthRequest;
use App\Models\User;
use App\Models\AuditLog;
use App\Models\RolePermission;

/**
 * QR-code-based authentication controller.
 *
 * Coordinates scan-to-log-in between the desktop and Android apps. The QR code
 * only ever carries a short-lived, one-time `request_code` — never credentials
 * or tokens. The backend validates every state transition and issues fresh
 * tokens for the requesting device only after the phone explicitly approves.
 *
 * Flow (desktop needs auth):
 *   1. Desktop  → POST /qr-auth/request        → { request_code }
 *   2. Desktop  → GET  /qr-auth/{code}         → polls status (pending…)
 *   3. Phone    → POST /qr-auth/{code}/scan    → { device_name } (shows identity)
 *   4. Phone    → POST /qr-auth/{code}/approve → server issues desktop tokens
 *   5. Desktop  → GET  /qr-auth/{code}         → { status: approved, access_token, … }
 *   6. Desktop  → tokens consumed, request marked "consumed"
 *
 * Reverse flow (phone needs auth, desktop is logged in):
 *   Desktop creates the request WITH its Bearer token (requester_user_id set).
 *   The phone scans, approves, and receives tokens directly in the approve
 *   response (device_type=android → tokens returned to the phone).
 */
class QrAuthController
{
    private function config(): array
    {
        return require __DIR__ . '/../../config/app.php';
    }

    /**
     * Get the authenticated user from the Authorization header (if any).
     * Used for the reverse flow where the desktop is already logged in.
     */
    private function getOptionalAuthUser(): ?array
    {
        return AuthController::getAuthenticatedUser();
    }

    /**
     * Require an authenticated user (for approve/reject from the phone).
     */
    private function requireAuthUser(): ?array
    {
        $payload = AuthController::getAuthenticatedUser();
        if (!$payload || !isset($payload['sub'])) {
            Response::unauthorized('Authentication required');
        }
        return $payload;
    }

    /**
     * Generate a fresh access + refresh token pair for a user.
     */
    private function issueTokens(array $user): array
    {
        $config = $this->config();
        $accessToken = JWT::encode([
            'sub'          => $user['id'],
            'username'     => $user['username'],
            'role_id'      => $user['role_id'],
            'role_code'    => $user['role_code'],
            'personnel_id' => $user['personnel_id'],
        ]);
        $refreshToken = JWT::encode([
            'sub'  => $user['id'],
            'type' => 'refresh',
            'exp'  => time() + $config['jwt_refresh_ttl'],
        ]);
        return [$accessToken, $refreshToken];
    }

    /**
     * Build a public-safe user object (no password_hash, with permissions).
     */
    private function publicUser(array $user): array
    {
        $user['permissions'] = RolePermission::getByRoleId($user['role_id']);
        unset($user['password_hash']);
        return $user;
    }

    /**
     * Convert a MySQL TIMESTAMP string (UTC, e.g. "2026-08-16 12:30:00")
     * to an ISO 8601 string with a "Z" suffix so clients parse it as UTC
     * instead of local time.
     */
    private function toIsoUtc(?string $timestamp): ?string
    {
        if (!$timestamp) return null;
        return str_replace(' ', 'T', $timestamp) . 'Z';
    }

    /**
     * POST /api/qr-auth/request
     * Body: { device_type: "desktop"|"android", device_name: string }
     * Auth: optional (required for reverse flow — desktop already logged in)
     *
     * Creates a pending QR auth request and returns the one-time request_code
     * to embed in the QR code.
     */
    public function request(array $params): void
    {
        // Housekeeping: mark old pending requests as expired
        QrAuthRequest::expireStale();

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $deviceType = $data['device_type'] ?? '';
        $deviceName = trim($data['device_name'] ?? '');

        if (!in_array($deviceType, ['desktop', 'android'], true)) {
            Response::error('device_type must be "desktop" or "android"', 422);
        }
        if ($deviceName === '') {
            Response::error('device_name is required', 422, ['device_name' => 'Le nom de l\'appareil est requis']);
        }
        if (mb_strlen($deviceName) > 255) {
            Response::error('device_name is too long (max 255 characters)', 422);
        }

        // If a Bearer token is present, capture the requester user (reverse flow)
        $requesterUserId = null;
        $authPayload = $this->getOptionalAuthUser();
        if ($authPayload && isset($authPayload['sub'])) {
            $requesterUserId = (int) $authPayload['sub'];
        }

        $config = $this->config();
        $ttl = $config['qr_auth']['ttl_seconds'] ?? 120;
        $expiresAt = gmdate('Y-m-d H:i:s', time() + $ttl);

        $request = QrAuthRequest::create([
            'device_type'       => $deviceType,
            'device_name'       => $deviceName,
            'requester_user_id' => $requesterUserId,
            'expires_at'        => $expiresAt,
            'client_ip'         => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent'        => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success([
            'request_code' => $request['request_code'],
            'device_type'  => $request['device_type'],
            'device_name'  => $request['device_name'],
            'expires_at'   => $this->toIsoUtc($request['expires_at']),
            'ttl_seconds'  => $ttl,
        ], 'QR auth request created');
    }

    /**
     * GET /api/qr-auth/{code}
     *
     * Returns the current status of a QR auth request. When the status is
     * "approved" AND the request has not yet been consumed, the issued tokens
     * and user info are included (one-time retrieval). After retrieval the
     * request is marked "consumed" and tokens are no longer returned.
     */
    public function status(array $params): void
    {
        $code = $params['code'] ?? '';
        $request = QrAuthRequest::getByCode($code);

        if (!$request) {
            Response::notFound('QR auth request not found');
        }

        // Auto-expire if past TTL and still pending/scanned
        if (QrAuthRequest::isExpired($request) && in_array($request['status'], ['pending', 'scanned'], true)) {
            QrAuthRequest::updateStatus($code, 'expired');
            $request['status'] = 'expired';
        }

        $response = [
            'request_code' => $request['request_code'],
            'device_type'  => $request['device_type'],
            'device_name'  => $request['device_name'],
            'status'       => $request['status'],
            'expires_at'   => $this->toIsoUtc($request['expires_at']),
            'scanned_at'   => $this->toIsoUtc($request['scanned_at']),
            'resolved_at'  => $this->toIsoUtc($request['resolved_at']),
        ];

        // One-time token retrieval for the requesting desktop device (forward flow)
        if ($request['status'] === 'approved' && $request['consumed_at'] === null) {
            if ($request['device_type'] === 'desktop') {
                $user = User::getById((int) $request['approver_user_id']);
                if ($user && $request['issued_access_token']) {
                    $response['access_token'] = $request['issued_access_token'];
                    $response['refresh_token'] = $request['issued_refresh_token'];
                    $response['user'] = $this->publicUser($user);

                    // Mark as consumed so tokens can never be retrieved again
                    QrAuthRequest::updateStatus($code, 'consumed');
                }
            } else {
                // Reverse flow (device_type === 'android'): tokens were already returned
                // directly to phone in approve response. Mark as consumed so status is closed.
                QrAuthRequest::updateStatus($code, 'consumed');
            }
        }

        Response::success($response);
    }

    /**
     * POST /api/qr-auth/{code}/scan
     *
     * Marks the request as "scanned" and returns the device/user identity so the
     * phone can display it before asking the user to approve.
     *
     * In forward flow (device_type=desktop): the phone is logged in and approves
     *   the desktop login, so the phone MUST be authenticated.
     * In reverse flow (device_type=android): the desktop is already logged in
     *   (requester_user_id is set) and the phone is authenticating itself,
     *   so the phone does NOT require an existing Bearer token.
     */
    public function scan(array $params): void
    {
        $code = $params['code'] ?? '';
        $request = QrAuthRequest::getByCode($code);

        if (!$request) {
            Response::notFound('QR auth request not found');
        }
        if (QrAuthRequest::isExpired($request)) {
            if ($request['status'] === 'pending' || $request['status'] === 'scanned') {
                QrAuthRequest::updateStatus($code, 'expired');
            }
            Response::error('Ce QR code a expiré', 410);
        }
        if ($request['status'] !== 'pending') {
            Response::error('Ce QR code n\'est plus valide (statut: ' . $request['status'] . ')', 409);
        }

        $isReverseFlow = ($request['device_type'] === 'android');

        if ($isReverseFlow) {
            // Reverse flow: verify the desktop requester user is active
            if (empty($request['requester_user_id'])) {
                Response::error('Ce QR code n\'est pas valide pour la connexion mobile', 422);
            }
            $requesterUser = User::getById((int) $request['requester_user_id']);
            if (!$requesterUser || !$requesterUser['is_active']) {
                Response::error('Le compte utilisateur n\'est plus actif', 403);
            }
        } else {
            // Forward flow: require an authenticated phone user
            $payload = $this->requireAuthUser();
            $phoneUser = User::getById($payload['sub']);
            if (!$phoneUser || !$phoneUser['is_active']) {
                Response::unauthorized('User not found or deactivated');
            }
        }

        QrAuthRequest::updateStatus($code, 'scanned');

        // Build a description of who is requesting login
        $requesterInfo = null;
        if ($request['requester_user_id']) {
            $requester = User::getById((int) $request['requester_user_id']);
            if ($requester) {
                $requesterInfo = [
                    'username'  => $requester['username'],
                    'firstname' => $requester['firstname'],
                    'lastname'  => $requester['lastname'],
                    'role_code' => $requester['role_code'],
                    'role_name' => $requester['role_name'],
                ];
            }
        }

        Response::success([
            'request_code'  => $request['request_code'],
            'device_type'   => $request['device_type'],
            'device_name'   => $request['device_name'],
            'requester'     => $requesterInfo,
            'expires_at'    => $this->toIsoUtc($request['expires_at']),
        ], 'QR code scanned — awaiting approval');
    }

    /**
     * POST /api/qr-auth/{code}/approve
     *
     * Approves the request and generates fresh tokens:
     *  - Forward flow (device_type=desktop): phone user is authenticated and
     *    approves the desktop login. Tokens stored for desktop to poll.
     *  - Reverse flow (device_type=android): phone user approves logging in
     *    as the desktop's authenticated user (requester_user_id). Tokens
     *    returned directly to the phone in this response.
     */
    public function approve(array $params): void
    {
        $code = $params['code'] ?? '';
        $request = QrAuthRequest::getByCode($code);

        if (!$request) {
            Response::notFound('QR auth request not found');
        }
        if (QrAuthRequest::isExpired($request)) {
            Response::error('Ce QR code a expiré', 410);
        }
        if ($request['status'] !== 'scanned') {
            Response::error('Le QR code doit être scanné avant d\'être approuvé (statut: ' . $request['status'] . ')', 409);
        }

        $isReverseFlow = ($request['device_type'] === 'android');

        if ($isReverseFlow) {
            // Reverse flow: target user is the authenticated desktop user
            if (empty($request['requester_user_id'])) {
                Response::error('Ce QR code n\'est pas valide pour la connexion mobile', 422);
            }
            $targetUserId = (int) $request['requester_user_id'];
            $targetUser = User::getById($targetUserId);
            if (!$targetUser || !$targetUser['is_active']) {
                Response::error('Le compte utilisateur n\'est plus actif', 403);
            }

            [$accessToken, $refreshToken] = $this->issueTokens($targetUser);

            QrAuthRequest::updateStatus($code, 'approved', [
                'approver_user_id'     => $targetUserId,
                'issued_access_token'  => $accessToken,
                'issued_refresh_token' => $refreshToken,
            ]);

            User::updateLastLogin($targetUser['id']);

            AuditLog::create([
                'user_id'     => $targetUser['id'],
                'action'      => 'qr_auth_approved',
                'module'      => 'auth',
                'description' => "Connexion mobile par QR code approuvée pour '{$targetUser['username']}' depuis {$request['device_name']}",
                'ip_address'  => $_SERVER['REMOTE_ADDR'] ?? null,
                'user_agent'  => $_SERVER['HTTP_USER_AGENT'] ?? null,
            ]);

            Response::success([
                'access_token'  => $accessToken,
                'refresh_token' => $refreshToken,
                'user'          => $this->publicUser($targetUser),
            ], 'Authentification approuvée');
        } else {
            // Forward flow: phone user approves the desktop login
            $payload = $this->requireAuthUser();
            $phoneUser = User::getById($payload['sub']);
            if (!$phoneUser || !$phoneUser['is_active']) {
                Response::unauthorized('User not found or deactivated');
            }

            $targetUser = $phoneUser;
            [$accessToken, $refreshToken] = $this->issueTokens($targetUser);

            QrAuthRequest::updateStatus($code, 'approved', [
                'approver_user_id'     => (int) $phoneUser['id'],
                'issued_access_token'  => $accessToken,
                'issued_refresh_token' => $refreshToken,
            ]);

            User::updateLastLogin($targetUser['id']);

            AuditLog::create([
                'user_id'     => $targetUser['id'],
                'action'      => 'qr_auth_approved',
                'module'      => 'auth',
                'description' => "Connexion QR approuvée pour '{$targetUser['username']}' sur {$request['device_type']} ({$request['device_name']})",
                'ip_address'  => $_SERVER['REMOTE_ADDR'] ?? null,
                'user_agent'  => $_SERVER['HTTP_USER_AGENT'] ?? null,
            ]);

            Response::success([
                'device_type' => $request['device_type'],
                'device_name' => $request['device_name'],
            ], 'Connexion approuvée — en attente de récupération par l\'ordinateur');
        }
    }

    /**
     * POST /api/qr-auth/{code}/reject
     */
    public function reject(array $params): void
    {
        $code = $params['code'] ?? '';
        $request = QrAuthRequest::getByCode($code);

        if (!$request) {
            Response::notFound('QR auth request not found');
        }
        if (QrAuthRequest::isExpired($request)) {
            Response::error('Ce QR code a expiré', 410);
        }
        if (!in_array($request['status'], ['pending', 'scanned'], true)) {
            Response::error('Ce QR code n\'est plus valide (statut: ' . $request['status'] . ')', 409);
        }

        $payload = $this->getOptionalAuthUser();
        $userId = $payload ? (int) $payload['sub'] : ($request['requester_user_id'] ? (int) $request['requester_user_id'] : null);

        QrAuthRequest::updateStatus($code, 'rejected', [
            'approver_user_id' => $userId,
        ]);

        AuditLog::create([
            'user_id'     => $userId,
            'action'      => 'qr_auth_rejected',
            'module'      => 'auth',
            'description' => "Connexion QR refusée pour {$request['device_type']} ({$request['device_name']})",
            'ip_address'  => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent'  => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Connexion refusée');
    }

    /**
     * POST /api/qr-auth/{code}/cancel
     * Auth: none required (the requesting device cancels its own request)
     *
     * Cancels a pending/scanned request. Used when the user clicks "Annuler"
     * on the desktop or closes the QR dialog.
     */
    public function cancel(array $params): void
    {
        $code = $params['code'] ?? '';
        $request = QrAuthRequest::getByCode($code);

        if (!$request) {
            Response::notFound('QR auth request not found');
        }
        if (!in_array($request['status'], ['pending', 'scanned'], true)) {
            // Already resolved — nothing to cancel
            Response::success(null, 'Request already resolved');
        }

        QrAuthRequest::updateStatus($code, 'cancelled');

        Response::success(null, 'Demande annulée');
    }
}
