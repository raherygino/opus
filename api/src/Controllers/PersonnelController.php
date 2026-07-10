<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Personnel;
use App\Models\PersonnelAttachment;
use App\Models\User;
use App\Models\Notification;
use App\Models\AuditLog;
use App\Validators\PersonnelValidator;

class PersonnelController
{
    /**
     * GET /api/personnel
     * GET /api/personnel?search=diop
     */
    public function index(array $params): void
    {
        $filters = [];
        if (isset($_GET['status'])) {
            $filters['status'] = $_GET['status'];
        }
        if (isset($_GET['grade'])) {
            $filters['grade'] = $_GET['grade'];
        }
        if (isset($_GET['affectation'])) {
            $filters['affectation'] = $_GET['affectation'];
        }
        if (isset($_GET['search'])) {
            $filters['search'] = $_GET['search'];
        }

        $list = Personnel::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/personnel/available
     * Personnel without user accounts (for user creation dropdown)
     */
    public function available(array $params): void
    {
        $list = Personnel::getAvailableForUser();
        Response::success($list);
    }

    /**
     * GET /api/personnel/{id}
     */
    public function show(array $params): void
    {
        $person = Personnel::getById((int) $params['id']);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        // Include linked user if exists
        // (We don't expose password_hash here since it's in users table)
        Response::success($person);
    }

    /**
     * POST /api/personnel
     */
    public function store(array $params): void
    {
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = PersonnelValidator::validateCreate($data);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $id = Personnel::create($data);
        $person = Personnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Création du personnel '{$person['firstname']} {$person['lastname']}' (IM: {$person['im']})",
            'new_values' => $person,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Generate notification ---
        $authUser = AuthController::getAuthenticatedUser();
        $creatorRole = $authUser['role_code'] ?? null;
        $creatorId = $authUser['sub'] ?? null;
        $personnelName = $person['firstname'] . ' ' . $person['lastname'];

        if ($creatorRole === 'SUPER_ADMIN' || $creatorRole === 'STATION_ADMIN') {
            // Admin added personnel → notify all admins
            $admins = Notification::getAdminUsers();
            foreach ($admins as $admin) {
                if ($creatorId && (int) $admin['id'] === (int) $creatorId) {
                    continue;
                }
                Notification::create([
                    'title' => "Nouveau personnel ajouté",
                    'message' => "{$personnelName} (IM: {$person['im']}) a été ajouté par un administrateur.",
                    'type' => 'info',
                    'service' => self::affectationToCode($person['affectation'] ?? ''),
                    'user_id' => $admin['id'],
                    'personnel_id' => $id,
                    'created_by' => $creatorId,
                ]);
            }
        } else {
            // Non-admin added personnel → notify all admins
            $admins = Notification::getAdminUsers();
            foreach ($admins as $admin) {
                Notification::create([
                    'title' => "Nouveau personnel en attente de validation",
                    'message' => "{$personnelName} (IM: {$person['im']}) a été ajouté et nécessite votre attention.",
                    'type' => 'warning',
                    'service' => self::affectationToCode($person['affectation'] ?? ''),
                    'user_id' => $admin['id'],
                    'personnel_id' => $id,
                    'created_by' => $creatorId,
                ]);
            }
        }

        Response::created($person, 'Personnel created successfully');
    }

    /**
     * PUT /api/personnel/{id}
     */
    public function update(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = PersonnelValidator::validateCreate($data, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldPerson = $person;
        Personnel::update($id, $data);
        $person = Personnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Modification du personnel '{$person['firstname']} {$person['lastname']}' (IM: {$person['im']})",
            'old_values' => $oldPerson,
            'new_values' => $person,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($person, 'Personnel updated successfully');
    }

    /**
     * DELETE /api/personnel/{id}
     */
    public function destroy(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        // Check if linked to a user
        if (User::personnelHasUser($id)) {
            Response::error('Cannot delete personnel linked to a user account. Remove the user account first.', 409);
        }

        // Delete attachment files from disk
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel';
        $attachments = PersonnelAttachment::getByPersonnelId($id);
        foreach ($attachments as $a) {
            $filePath = $uploadDir . '/' . $a['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        // Delete photo file from disk
        if (!empty($person['photo'])) {
            $photoPath = rtrim($config['upload_dir'], '/') . '/personnel/photos/' . $person['photo'];
            if (file_exists($photoPath)) {
                unlink($photoPath);
            }
        }

        // Delete signature file from disk
        if (!empty($person['signature'])) {
            $sigPath = rtrim($config['upload_dir'], '/') . '/personnel/signatures/' . $person['signature'];
            if (file_exists($sigPath)) {
                unlink($sigPath);
            }
        }

        Personnel::delete($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Suppression du personnel '{$person['firstname']} {$person['lastname']}' (IM: {$person['im']})",
            'old_values' => $person,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Personnel deleted successfully');
    }

    private static function affectationToCode(string $affectation): string
    {
        if (stripos($affectation, 'PJ') !== false || stripos($affectation, 'Police Judiciaire') !== false) {
            return 'PJ';
        }
        if (stripos($affectation, 'SG') !== false || stripos($affectation, 'Service Général') !== false) {
            return 'SG';
        }
        if (stripos($affectation, 'Sédentaire') !== false || stripos($affectation, 'Sedentaire') !== false) {
            return 'Sedentaire';
        }
        return 'System';
    }

    /**
     * POST /api/personnel/{id}/photo
     * Multipart: photo (file)
     */
    public function uploadPhoto(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        if (!isset($_FILES['photo']) || $_FILES['photo']['error'] !== UPLOAD_ERR_OK) {
            Response::error('Photo file is required', 422, ['photo' => 'Le fichier photo est requis']);
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel/photos';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        $uploadedFile = $_FILES['photo'];
        $extension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
        $allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

        if (!in_array($extension, $allowedExtensions)) {
            Response::error('Invalid file type', 422, ['photo' => 'Seuls les formats JPG, PNG, GIF et WebP sont autorisés']);
        }

        // Delete old photo if exists
        if (!empty($person['photo'])) {
            $oldPath = $uploadDir . '/' . $person['photo'];
            if (file_exists($oldPath)) {
                unlink($oldPath);
            }
        }

        $storedName = 'photo_' . $id . '_' . uniqid() . '.' . $extension;
        $destPath = $uploadDir . '/' . $storedName;

        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save photo', 500);
        }

        Personnel::update($id, ['photo' => $storedName]);
        $person = Personnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'photo_upload',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Photo mise à jour pour le personnel '{$person['firstname']} {$person['lastname']}' (ID: {$id})",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($person, 'Photo uploaded successfully');
    }

    /**
     * POST /api/personnel/{id}/signature
     * Multipart: signature (file)
     */
    public function uploadSignature(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        if (!isset($_FILES['signature']) || $_FILES['signature']['error'] !== UPLOAD_ERR_OK) {
            Response::error('Signature file is required', 422, ['signature' => 'Le fichier signature est requis']);
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel/signatures';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        $uploadedFile = $_FILES['signature'];
        $extension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
        $allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

        if (!in_array($extension, $allowedExtensions)) {
            Response::error('Invalid file type', 422, ['signature' => 'Seuls les formats JPG, PNG, GIF et WebP sont autorisés']);
        }

        // Delete old signature if exists
        if (!empty($person['signature'])) {
            $oldPath = $uploadDir . '/' . $person['signature'];
            if (file_exists($oldPath)) {
                unlink($oldPath);
            }
        }

        $storedName = 'sig_' . $id . '_' . uniqid() . '.' . $extension;
        $destPath = $uploadDir . '/' . $storedName;

        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save signature', 500);
        }

        Personnel::update($id, ['signature' => $storedName]);
        $person = Personnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'signature_upload',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Signature mise à jour pour le personnel '{$person['firstname']} {$person['lastname']}' (ID: {$id})",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($person, 'Signature uploaded successfully');
    }

    /**
     * GET /api/personnel/{id}/photo
     * Serve the personnel photo file
     */
    public function servePhoto(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person || empty($person['photo'])) {
            Response::notFound('Photo not found');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $filePath = rtrim($config['upload_dir'], '/') . '/personnel/photos/' . $person['photo'];

        if (!file_exists($filePath)) {
            Response::notFound('Photo file not found on disk');
        }

        $extension = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
        $mimeTypes = [
            'jpg' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png' => 'image/png',
            'gif' => 'image/gif',
            'webp' => 'image/webp',
        ];
        $mime = $mimeTypes[$extension] ?? 'image/jpeg';

        header('Content-Type: ' . $mime);
        header('Content-Length: ' . filesize($filePath));
        header('Cache-Control: max-age=86400');
        readfile($filePath);
        exit;
    }

    /**
     * GET /api/personnel/{id}/signature
     * Serve the personnel signature file
     */
    public function serveSignature(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person || empty($person['signature'])) {
            Response::notFound('Signature not found');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $filePath = rtrim($config['upload_dir'], '/') . '/personnel/signatures/' . $person['signature'];

        if (!file_exists($filePath)) {
            Response::notFound('Signature file not found on disk');
        }

        $extension = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
        $mimeTypes = [
            'jpg' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png' => 'image/png',
            'gif' => 'image/gif',
            'webp' => 'image/webp',
        ];
        $mime = $mimeTypes[$extension] ?? 'image/jpeg';

        header('Content-Type: ' . $mime);
        header('Content-Length: ' . filesize($filePath));
        header('Cache-Control: max-age=86400');
        readfile($filePath);
        exit;
    }
}
