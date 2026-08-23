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

        // Indicate whether this personnel record belongs to an administrator
        // so clients can hide edit/delete controls for other admins' profiles.
        $person['is_admin_profile'] = User::personnelBelongsToAdmin((int) $person['id']);

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
        // Peer-to-peer: regardless of who the actor is (admin or regular
        // user), every other admin AND every other user with view permission
        // on the personnel module is notified. The actor is always excluded.
        $authUser = AuthController::getAuthenticatedUser();
        $creatorId = $authUser['sub'] ?? null;
        $personnelName = $person['firstname'] . ' ' . $person['lastname'];
        $service = self::affectationToCode($person['affectation'] ?? '');

        Notification::notifyFeatureChange('personnel', [
            'title'        => 'Nouveau personnel ajouté',
            'message'      => "{$personnelName} (IM: {$person['im']}) a été ajouté.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
        ], [
            'title'        => 'Nouveau personnel ajouté',
            'message'      => "{$personnelName} (IM: {$person['im']}) a été ajouté. Veuillez en prendre connaissance.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
            'link'         => '/personnel',
        ], $creatorId);

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

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin may not edit another admin's personnel profile
        self::guardAdminProfile($person, $authUser);

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = PersonnelValidator::validateCreate($data, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldPerson = $person;
        Personnel::update($id, $data);
        $person = Personnel::getById($id);

        // --- Audit log ---
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

        // --- Notify all affected users (admins + peers with personnel view) ---
        $creatorId = $authUser['sub'] ?? null;
        $personnelName = $person['firstname'] . ' ' . $person['lastname'];
        $service = self::affectationToCode($person['affectation'] ?? '');

        Notification::notifyFeatureChange('personnel', [
            'title'        => 'Personnel modifié',
            'message'      => "{$personnelName} (IM: {$person['im']}) a été modifié.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
        ], [
            'title'        => 'Personnel modifié',
            'message'      => "{$personnelName} (IM: {$person['im']}) a été modifié. Veuillez en prendre connaissance des informations mises à jour.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
            'link'         => '/personnel',
        ], $creatorId);

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

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin's personnel profile may never be deleted (even by
        // another admin). The owner may not delete their own profile either —
        // deletion of a personnel record that is linked to a user account is
        // always blocked (see check below).
        if (User::personnelBelongsToAdmin($id)) {
            Response::forbidden('Le profil d\'un administrateur ne peut pas être supprimé');
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

    public static function affectationToCode(string $affectation): string
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
     * Enforce the "admin cannot edit another admin's profile" rule.
     *
     * An administrator may always edit their OWN personnel record. They may
     * NOT edit (or delete) the personnel record of another administrator.
     * Non-admin personnel records are always editable (subject to RBAC).
     *
     * @param array $person   The personnel record being modified.
     * @param array $authUser The authenticated user from the JWT.
     * @throws \App\Helpers\Response (sends 403 and exits) when forbidden.
     */
    private static function guardAdminProfile(array $person, array $authUser): void
    {
        $personnelId = (int) $person['id'];
        $linkedUser = \App\Models\User::getByPersonnelId($personnelId);

        // Personnel not linked to any user account → free to edit
        if ($linkedUser === null) {
            return;
        }

        // Personnel linked to a non-admin user → free to edit
        if (!\App\Models\User::isAdminRoleCode($linkedUser['role_code'] ?? '')) {
            return;
        }

        // Personnel linked to an admin user → only the owner may edit
        $currentUserId = (int) ($authUser['sub'] ?? 0);
        if ((int) $linkedUser['id'] === $currentUserId) {
            return;
        }

        Response::forbidden(
            'Vous ne pouvez pas modifier le profil d\'un autre administrateur'
        );
    }

    /**
     * POST /api/personnel/{id}/photo
     * Multipart: photo (file), thumbnail (file, optional)
     */
    public function uploadPhoto(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin may not change another admin's photo
        self::guardAdminProfile($person, $authUser);

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
        // Delete old thumbnail if exists
        if (!empty($person['thumbnail'])) {
            $oldThumbPath = $uploadDir . '/' . $person['thumbnail'];
            if (file_exists($oldThumbPath)) {
                unlink($oldThumbPath);
            }
        }

        $storedName = 'photo_' . $id . '_' . uniqid() . '.' . $extension;
        $destPath = $uploadDir . '/' . $storedName;

        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save photo', 500);
        }

        $updateData = ['photo' => $storedName];

        // Handle optional thumbnail upload
        if (isset($_FILES['thumbnail']) && $_FILES['thumbnail']['error'] === UPLOAD_ERR_OK) {
            $thumbExt = strtolower(pathinfo($_FILES['thumbnail']['name'], PATHINFO_EXTENSION));
            if (in_array($thumbExt, $allowedExtensions)) {
                $thumbName = 'thumb_' . $id . '_' . uniqid() . '.' . $thumbExt;
                $thumbDest = $uploadDir . '/' . $thumbName;
                if (move_uploaded_file($_FILES['thumbnail']['tmp_name'], $thumbDest)) {
                    $updateData['thumbnail'] = $thumbName;
                }
            }
        }

        Personnel::update($id, $updateData);
        $person = Personnel::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'photo_upload',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Photo mise à jour pour le personnel '{$person['firstname']} {$person['lastname']}' (ID: {$id})",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify all affected users (admins + peers with personnel view) ---
        $creatorId = $authUser['sub'] ?? null;
        $personnelName = $person['firstname'] . ' ' . $person['lastname'];
        $service = self::affectationToCode($person['affectation'] ?? '');

        Notification::notifyFeatureChange('personnel', [
            'title'        => 'Photo mise à jour',
            'message'      => "La photo de {$personnelName} (IM: {$person['im']}) a été mise à jour.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
        ], [
            'title'        => 'Photo mise à jour',
            'message'      => "La photo de {$personnelName} (IM: {$person['im']}) a été mise à jour. Veuillez en prendre connaissance.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
            'link'         => '/personnel',
        ], $creatorId);

        Response::success($person, 'Photo uploaded successfully');
    }

    /**
     * DELETE /api/personnel/{id}/photo
     */
    public function deletePhoto(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized();
        }

        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        // Enforce: an admin may not delete another admin's photo
        self::guardAdminProfile($person, $authUser);

        if (empty($person['photo'])) {
            Response::error('No photo to delete', 422);
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel/photos';

        // Delete photo file
        $photoPath = $uploadDir . '/' . $person['photo'];
        if (file_exists($photoPath)) {
            unlink($photoPath);
        }

        // Delete thumbnail file if exists
        if (!empty($person['thumbnail'])) {
            $thumbPath = $uploadDir . '/' . $person['thumbnail'];
            if (file_exists($thumbPath)) {
                unlink($thumbPath);
            }
        }

        Personnel::update($id, ['photo' => null, 'thumbnail' => null]);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'photo_delete',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Photo supprimée pour le personnel '{$person['firstname']} {$person['lastname']}' (ID: {$id})",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify all affected users (admins + peers with personnel view) ---
        $creatorId = $authUser['sub'] ?? null;
        $personnelName = $person['firstname'] . ' ' . $person['lastname'];
        $service = self::affectationToCode($person['affectation'] ?? '');

        Notification::notifyFeatureChange('personnel', [
            'title'        => 'Photo mise à jour',
            'message'      => "La photo de {$personnelName} (IM: {$person['im']}) a été supprimée.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
        ], [
            'title'        => 'Photo mise à jour',
            'message'      => "La photo de {$personnelName} (IM: {$person['im']}) a été supprimée. Veuillez en prendre connaissance.",
            'type'         => 'info',
            'service'      => $service,
            'personnel_id' => $id,
            'link'         => '/personnel',
        ], $creatorId);

        $person = Personnel::getById($id);
        Response::success($person, 'Photo deleted successfully');
    }

    /**
     * GET /api/personnel/{id}/thumbnail
     */
    public function serveThumbnail(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person || empty($person['thumbnail'])) {
            Response::notFound('Thumbnail not found');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/personnel/photos';
        $filePath = $uploadDir . '/' . $person['thumbnail'];

        if (!file_exists($filePath)) {
            Response::notFound('Thumbnail not found');
        }

        $ext = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
        $mimeTypes = [
            'jpg' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png' => 'image/png',
            'gif' => 'image/gif',
            'webp' => 'image/webp',
        ];
        $mime = $mimeTypes[$ext] ?? 'application/octet-stream';

        header('Content-Type: ' . $mime);
        header('Cache-Control: public, max-age=86400');
        readfile($filePath);
        exit;
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

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin may not change another admin's signature
        self::guardAdminProfile($person, $authUser);

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
        header('Cache-Control: no-cache, must-revalidate');
        header('Pragma: no-cache');
        readfile($filePath);
        exit;
    }

    /**
     * POST /api/personnel/{id}/signature/svg
     * Body: { "svg": "<svg>...</svg>" }
     * Saves the SVG vector data of the signature drawn on the Android signature pad.
     */
    public function saveSignatureSvg(array $params): void
    {
        $id = (int) $params['id'];
        $person = Personnel::getById($id);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $authUser = AuthController::getAuthenticatedUser();
        // Enforce: an admin may not change another admin's signature
        self::guardAdminProfile($person, $authUser);

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        if (empty($data['svg'])) {
            Response::error('SVG data is required', 422, ['svg' => 'Les données SVG sont requises']);
        }

        $svg = $data['svg'];

        Personnel::update($id, ['signature_svg' => $svg]);
        $person = Personnel::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'signature_svg_save',
            'module' => 'personnel',
            'entity_id' => $id,
            'description' => "Signature SVG enregistrée pour le personnel '{$person['firstname']} {$person['lastname']}' (ID: {$id})",
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($person, 'Signature SVG saved successfully');
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
