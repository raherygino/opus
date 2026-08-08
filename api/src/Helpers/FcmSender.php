<?php

namespace App\Helpers;

use App\Models\DeviceToken;

/**
 * FcmSender — sends push notifications via Firebase Cloud Messaging HTTP v1 API.
 *
 * Uses a Firebase service account (JSON) to obtain an OAuth2 access token via
 * JWT bearer flow (RFC 7523), then sends messages to the FCM v1 endpoint.
 *
 * Pure PHP (no Composer dependencies) — uses cURL and OpenSSL.
 *
 * Usage:
 *   FcmSender::sendToUser($userId, [
 *       'title' => 'New notification',
 *       'body'  => 'You have a new message',
 *       'data'  => ['notification_id' => '42', 'type' => 'info'],
 *   ]);
 */
class FcmSender
{
    private const FCM_ENDPOINT = 'https://fcm.googleapis.com/v1/projects/%s/messages:send';
    private const OAUTH_TOKEN_URI = 'https://oauth2.googleapis.com/token';
    private const JWT_AUD = 'https://oauth2.googleapis.com/token';
    private const SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';
    private const TOKEN_TTL = 3300; // ~55 min (Google tokens last 1 hour)

    /** @var array|null Cached service account data */
    private static ?array $serviceAccount = null;

    /**
     * Send a push notification to all active devices belonging to a user.
     *
     * @param int $userId
     * @param array{title: string, body: string, data?: array<string,string>} $payload
     * @return array{success: int, failure: int}
     */
    public static function sendToUser(int $userId, array $payload): array
    {
        $tokens = DeviceToken::getActiveByUserId($userId);
        return self::sendToTokens(
            array_column($tokens, 'token'),
            $payload
        );
    }

    /**
     * Send a push notification to multiple users.
     *
     * @param int[] $userIds
     * @param array{title: string, body: string, data?: array<string,string>} $payload
     * @return array{success: int, failure: int}
     */
    public static function sendToUsers(array $userIds, array $payload): array
    {
        if (empty($userIds)) {
            return ['success' => 0, 'failure' => 0];
        }
        $tokens = DeviceToken::getActiveByUserIds($userIds);
        return self::sendToTokens(
            array_column($tokens, 'token'),
            $payload
        );
    }

    /**
     * Send a push notification to all admin users (broadcast).
     *
     * @param array{title: string, body: string, data?: array<string,string>} $payload
     * @return array{success: int, failure: int}
     */
    public static function sendToAdmins(array $payload): array
    {
        $tokens = DeviceToken::getActiveForAdmins();
        return self::sendToTokens(
            array_column($tokens, 'token'),
            $payload
        );
    }

    /**
     * Send a push notification to a list of FCM tokens.
     *
     * @param string[] $tokens
     * @param array{title: string, body: string, data?: array<string,string>} $payload
     * @return array{success: int, failure: int}
     */
    public static function sendToTokens(array $tokens, array $payload): array
    {
        $tokens = array_values(array_filter($tokens));
        if (empty($tokens)) {
            return ['success' => 0, 'failure' => 0];
        }

        if (!self::isEnabled()) {
            return ['success' => 0, 'failure' => 0];
        }

        $success = 0;
        $failure = 0;

        foreach ($tokens as $token) {
            $result = self::sendSingle($token, $payload);
            if ($result['sent']) {
                $success++;
            } else {
                $failure++;
                // Only deactivate the token if FCM explicitly says it's
                // UNREGISTERED or INVALID.  Transient errors (network timeouts,
                // OAuth2 failures, 5xx server errors) must NOT deactivate the
                // token — otherwise a single transient failure would permanently
                // kill push delivery to that device until the app re-registers.
                if ($result['deactivate']) {
                    DeviceToken::deactivate($token);
                    error_log("[FcmSender] Deactivated invalid token: " . substr($token, 0, 12) . '…');
                }
            }
        }

        return ['success' => $success, 'failure' => $failure];
    }

    /**
     * Send a single message to one FCM token via the HTTP v1 API.
     *
     * @return array{sent: bool, deactivate: bool} 'sent' = push delivered,
     *         'deactivate' = token is permanently invalid and should be deactivated.
     */
    private static function sendSingle(string $token, array $payload): array
    {
        $projectId = self::getProjectId();
        if ($projectId === null) {
            return ['sent' => false, 'deactivate' => false];
        }

        $accessToken = self::getAccessToken();
        if ($accessToken === null) {
            return ['sent' => false, 'deactivate' => false];
        }

        // Build a DATA-ONLY message. We intentionally do NOT include a
        // 'notification' block. When a 'notification' block is present and the
        // app is in the background, Android displays the notification itself
        // using the default launcher icon and never calls onMessageReceived().
        // By sending data-only messages, we guarantee that
        // OpusMessagingService.onMessageReceived() is always invoked —
        // regardless of foreground/background state — so our NotificationHelper
        // can display the notification with the correct custom icon.
        $data = array_merge(
            [
                'title' => $payload['title'] ?? 'OPUS',
                'body'  => $payload['body'] ?? '',
            ],
            $payload['data'] ?? []
        );

        $message = [
            'message' => [
                'token' => $token,
                'data'  => array_map(
                    fn($v) => is_string($v) ? $v : (string) $v,
                    $data
                ),
                'android' => [
                    'priority' => 'high',
                ],
            ],
        ];

        $url = sprintf(self::FCM_ENDPOINT, $projectId);
        $body = json_encode($message);

        // Transient failures (network timeouts, FCM 5xx, rate limits) get one
        // retry so a momentary hiccup doesn't silently drop the notification.
        $maxAttempts = 2;

        for ($attempt = 1; $attempt <= $maxAttempts; $attempt++) {
            $ch = curl_init($url);
            curl_setopt_array($ch, [
                CURLOPT_POST           => true,
                CURLOPT_POSTFIELDS     => $body,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_HTTPHEADER     => [
                    'Content-Type: application/json',
                    'Authorization: Bearer ' . $accessToken,
                ],
                CURLOPT_TIMEOUT        => 10,
                CURLOPT_SSL_VERIFYPEER => true,
            ]);

            $response = curl_exec($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $error = curl_error($ch);
            curl_close($ch);

            // cURL-level error (network, DNS, timeout) — transient, don't deactivate
            if ($error) {
                error_log("[FcmSender] cURL error (attempt $attempt/$maxAttempts): $error");
                if ($attempt < $maxAttempts) {
                    usleep(500000);
                    continue;
                }
                return ['sent' => false, 'deactivate' => false];
            }

            // 200 = success
            if ($httpCode === 200) {
                return ['sent' => true, 'deactivate' => false];
            }

            // Transient server-side errors (5xx) and rate limiting (429) — retry once
            if (($httpCode >= 500 || $httpCode === 429) && $attempt < $maxAttempts) {
                error_log("[FcmSender] Transient FCM error (HTTP $httpCode, attempt $attempt/$maxAttempts): $response");
                usleep(500000);
                continue;
            }

            // Parse the FCM error response to determine if the token is invalid
            $deactivate = self::isTokenInvalidResponse($httpCode, $response);
            error_log("[FcmSender] FCM error (HTTP $httpCode, deactivate=" . ($deactivate ? 'yes' : 'no') . "): $response");
            return ['sent' => false, 'deactivate' => $deactivate];
        }

        return ['sent' => false, 'deactivate' => false];
    }

    /**
     * Check if an FCM error response indicates the token is permanently invalid
     * (UNREGISTERED / INVALID_REGISTRATION / NOT_FOUND) and should be deactivated.
     * Transient errors (5xx, rate limits, etc.) return false.
     */
    private static function isTokenInvalidResponse(int $httpCode, string|false $response): bool
    {
        // Only 4xx errors can indicate an invalid token
        if ($httpCode < 400 || $httpCode >= 500) {
            return false;
        }

        if (!$response) {
            return false;
        }

        $data = json_decode($response, true);
        if (!isset($data['error'])) {
            return false;
        }

        $errorDetails = $data['error']['details'] ?? [];
        foreach ($errorDetails as $detail) {
            $reason = $detail['reason'] ?? '';
            if (in_array($reason, ['UNREGISTERED', 'INVALID_REGISTRATION', 'NOT_FOUND'], true)) {
                return true;
            }
        }

        // Also check the error message for these keywords as a fallback
        $message = strtolower($data['error']['message'] ?? '');
        if (str_contains($message, 'unregistered') || str_contains($message, 'not_found')) {
            return true;
        }

        return false;
    }

    // ─── OAuth2 / JWT ───────────────────────────────────────────────

    /**
     * Get (or cache) an OAuth2 access token for the FCM scope.
     */
    private static function getAccessToken(): ?string
    {
        $cachePath = self::getConfig()['access_token_cache'] ?? null;

        // Try cache first
        if ($cachePath && is_file($cachePath)) {
            $cached = json_decode(@file_get_contents($cachePath), true);
            if ($cached && isset($cached['token'], $cached['expires_at'])) {
                if (time() < $cached['expires_at'] - 60) {
                    return $cached['token'];
                }
            }
        }

        $sa = self::getServiceAccount();
        if ($sa === null) {
            return null;
        }

        $now = time();
        try {
            $jwt = self::buildJwt($sa, $now);
        } catch (\Throwable $e) {
            error_log("[FcmSender] JWT build failed: " . $e->getMessage());
            return null;
        }

        $ch = curl_init(self::OAUTH_TOKEN_URI);
        curl_setopt_array($ch, [
            CURLOPT_POST           => true,
            CURLOPT_POSTFIELDS     => http_build_query([
                'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
                'assertion'  => $jwt,
            ]),
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER     => [
                'Content-Type: application/x-www-form-urlencoded',
            ],
            CURLOPT_TIMEOUT        => 10,
            CURLOPT_SSL_VERIFYPEER => true,
        ]);

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode !== 200 || !$response) {
            error_log("[FcmSender] OAuth2 token request failed (HTTP $httpCode): $response");
            return null;
        }

        $data = json_decode($response, true);
        if (empty($data['access_token'])) {
            error_log("[FcmSender] No access_token in OAuth2 response");
            return null;
        }

        $token = $data['access_token'];
        $expiresAt = $now + ($data['expires_in'] ?? self::TOKEN_TTL);

        // Cache it
        if ($cachePath) {
            $dir = dirname($cachePath);
            if (!is_dir($dir)) {
                @mkdir($dir, 0755, true);
            }
            @file_put_contents($cachePath, json_encode([
                'token'      => $token,
                'expires_at' => $expiresAt,
            ]), LOCK_EX);
        }

        return $token;
    }

    /**
     * Build a signed JWT for the OAuth2 JWT bearer flow.
     *
     * @throws \RuntimeException if the private key is invalid or signing fails.
     */
    private static function buildJwt(array $sa, int $now): string
    {
        $header = ['alg' => 'RS256', 'typ' => 'JWT'];
        $claims = [
            'iss'   => $sa['client_email'],
            'scope' => self::SCOPE,
            'aud'   => self::JWT_AUD,
            'exp'   => $now + 3600,
            'iat'   => $now,
        ];

        $base64Header  = self::base64UrlEncode(json_encode($header));
        $base64Payload = self::base64UrlEncode(json_encode($claims));
        $signingInput  = $base64Header . '.' . $base64Payload;

        $signature = '';
        $privateKey = openssl_pkey_get_private($sa['private_key']);
        if ($privateKey === false) {
            throw new \RuntimeException('Failed to load private key from service account JSON');
        }
        $signed = openssl_sign($signingInput, $signature, $privateKey, OPENSSL_ALGO_SHA256);
        // openssl_free_key was deprecated in PHP 8.0 and removed in PHP 8.4+.
        // The key resource is freed automatically when the variable goes out of scope.
        unset($privateKey);

        if (!$signed || empty($signature)) {
            throw new \RuntimeException('Failed to sign JWT with service account private key');
        }

        return $signingInput . '.' . self::base64UrlEncode($signature);
    }

    /**
     * Base64 URL-safe encoding (no padding).
     */
    private static function base64UrlEncode(string $data): string
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    // ─── Config / Service Account ───────────────────────────────────

    private static function isEnabled(): bool
    {
        $config = self::getConfig();
        return $config['enabled'] ?? false;
    }

    private static function getConfig(): array
    {
        static $config = null;
        if ($config === null) {
            $appConfig = require __DIR__ . '/../../config/app.php';
            $config = $appConfig['fcm'] ?? ['enabled' => false];
        }
        return $config;
    }

    private static function getServiceAccount(): ?array
    {
        if (self::$serviceAccount !== null) {
            return self::$serviceAccount;
        }

        $path = self::getConfig()['service_account_path'] ?? null;
        if (!$path || !is_file($path)) {
            error_log("[FcmSender] Service account file not found: $path");
            return null;
        }

        $json = json_decode(file_get_contents($path), true);
        if (!isset($json['private_key'], $json['client_email'], $json['project_id'])) {
            error_log("[FcmSender] Invalid service account JSON (missing required fields)");
            return null;
        }

        self::$serviceAccount = $json;
        return self::$serviceAccount;
    }

    private static function getProjectId(): ?string
    {
        $sa = self::getServiceAccount();
        return $sa['project_id'] ?? null;
    }
}
