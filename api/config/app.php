<?php

return [
    'name'       => 'OPUS API',
    'version'    => '1.0.0',
    'debug'      => getenv('APP_DEBUG') ?: false,
    'jwt_secret' => getenv('JWT_SECRET') ?: 'opus-secret-key-change-in-production',
    'jwt_ttl'    => 900,        // 15 minutes (access token)
    'jwt_refresh_ttl' => 86400, // 24 hours (refresh token)
    'cors' => [
        'allowed_origins' => ['*'],
        'allowed_methods' => ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
        'allowed_headers' => ['Content-Type', 'Authorization'],
    ],
    'upload_dir' => __DIR__ . '/../uploads',

    // Firebase Cloud Messaging (FCM) — push notifications for Android
    // Download the service account JSON from Firebase Console:
    //   Project Settings > Service Accounts > Generate New Private Key
    // Save it to api/config/firebase-service-account.json (gitignored)
    'fcm' => [
        'enabled' => (getenv('FCM_ENABLED') ?: true) !== 'false',
        // Absolute path to the Firebase service account JSON
        'service_account_path' => __DIR__ . '/firebase-service-account.json',
        // Cache file for the OAuth2 access token (avoid re-auth on every push)
        'access_token_cache' => __DIR__ . '/../uploads/.fcm_token_cache',
    ],
];
