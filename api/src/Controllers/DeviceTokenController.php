<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Helpers\FcmSender;
use App\Models\DeviceToken;
use App\Controllers\AuthController;

/**
 * DeviceTokenController — manages FCM registration tokens for push notifications.
 *
 * POST   /api/devices/register     — register/refresh a device token
 * POST   /api/devices/unregister   — deactivate a device token
 * DELETE /api/devices              — deactivate all tokens for the current user (logout)
 * GET    /api/devices              — list the current user's registered tokens
 * POST   /api/devices/test-push    — send a test push to the current user
 */
class DeviceTokenController
{
    private static function getAuthUser(): ?array
    {
        return AuthController::getAuthenticatedUser();
    }

    /**
     * GET /api/devices
     * Lists all device tokens (active and inactive) for the authenticated user.
     * Useful for debugging token registration.
     */
    public function index(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $tokens = DeviceToken::getActiveByUserId((int) $authUser['sub']);
        Response::success([
            'user_id'     => (int) $authUser['sub'],
            'token_count' => count($tokens),
            'tokens'      => array_map(fn($t) => [
                'id'          => (int) $t['id'],
                'token'       => substr($t['token'], 0, 20) . '…',
                'device_name' => $t['device_name'] ?? null,
            ], $tokens),
        ]);
    }

    /**
     * POST /api/devices/register
     * Body: { token: string, device_name?: string }
     */
    public function register(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['token'])) {
            Response::error('Validation failed', 422, ['token' => 'FCM token is required']);
        }

        $id = DeviceToken::register(
            (int) $authUser['sub'],
            $data['token'],
            $data['device_name'] ?? null
        );

        Response::success(['id' => $id], 'Device registered successfully');
    }

    /**
     * POST /api/devices/unregister
     * Body: { token: string }
     */
    public function unregister(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['token'])) {
            Response::error('Validation failed', 422, ['token' => 'FCM token is required']);
        }

        DeviceToken::deactivate($data['token']);
        Response::success(null, 'Device unregistered successfully');
    }

    /**
     * DELETE /api/devices
     * Deactivate all tokens for the authenticated user (used on logout).
     */
    public function unregisterAll(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        DeviceToken::deactivateByUserId((int) $authUser['sub']);
        Response::success(null, 'All devices unregistered successfully');
    }

    /**
     * POST /api/devices/test-push
     * Sends a test FCM push notification to the authenticated user's devices.
     * Useful for verifying that FCM is configured correctly.
     */
    public function testPush(array $params): void
    {
        $authUser = self::getAuthUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $tokens = DeviceToken::getActiveByUserId((int) $authUser['sub']);
        $tokenCount = count($tokens);

        if ($tokenCount === 0) {
            Response::error('No active device tokens found for this user. Make sure the Android app has registered its FCM token.', 404);
        }

        $result = FcmSender::sendToUser((int) $authUser['sub'], [
            'title' => 'OPUS — Test Push',
            'body'  => 'This is a test push notification. If you see this, FCM is working correctly!',
            'data'  => [
                'notification_id' => '0',
                'type'            => 'info',
                'service'         => 'System',
                'click_action'    => 'OPEN_NOTIFICATIONS',
            ],
        ]);

        Response::success([
            'tokens'  => $tokenCount,
            'success' => $result['success'],
            'failure' => $result['failure'],
        ], "Test push sent: {$result['success']} succeeded, {$result['failure']} failed (out of $tokenCount tokens)");
    }
}
