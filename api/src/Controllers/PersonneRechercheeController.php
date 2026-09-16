<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\PersonneRecherchee;
use App\Models\PersonneRechercheePhoto;
use App\Models\AuditLog;
use App\Models\Notification;

class PersonneRechercheeController
{
    private const MODULE = 'pj_personne_recherchee';
    private const LINK_PREFIX = '/pj/personne-recherchee/';

    private string $photoDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->photoDir = rtrim($config['upload_dir'], '/') . '/personne_recherchee';
        if (!is_dir($this->photoDir)) {
            mkdir($this->photoDir, 0755, true);
        }
    }

    public static function sanitizeFilename(string $name): string
    {
        $name = mb_strtolower($name, 'UTF-8');
        $name = preg_replace('/[^a-z0-9]+/', '_', $name);
        return trim($name, '_');
    }

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('nom', $data)) {
            if (empty(trim((string) ($data['nom'] ?? '')))) {
                $errors['nom'] = 'Le nom est requis';
            }
        }

        if ($isCreate || array_key_exists('motif', $data)) {
            if (empty(trim((string) ($data['motif'] ?? '')))) {
                $errors['motif'] = 'Le motif est requis';
            }
        }

        return $errors;
    }

    /**
     * GET /api/personne-recherchee
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        if (isset($_GET['search']) && $_GET['search'] !== '') {
            $filters['search'] = $_GET['search'];
        }
        $list = PersonneRecherchee::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/personne-recherchee/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = PersonneRecherchee::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Personne recherchée introuvable');
        }
        $row['photos'] = PersonneRechercheePhoto::listForPersonne((int) $row['id']);

        if (!empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                self::LINK_PREFIX . $row['id'],
                (int) $authUser['sub']
            );
        }
        Response::success($row);
    }

    /**
     * POST /api/personne-recherchee
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $data['created_by'] = $authUser['sub'] ?? null;
        foreach (['nom', 'adresse', 'motif'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = PersonneRecherchee::create($data);
        $entry = PersonneRecherchee::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'personne_recherchee',
            'entity_id' => $id,
            'description' => "Création d'une personne recherchée — {$entry['nom']}",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Personne recherchée enregistrée avec succès');
    }

    /**
     * PUT /api/personne-recherchee/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = PersonneRecherchee::find($id);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        foreach (['nom', 'adresse', 'motif'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $entry;
        PersonneRecherchee::update($id, $data);
        $entry = PersonneRecherchee::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'personne_recherchee',
            'entity_id' => $id,
            'description' => "Modification d'une personne recherchée — {$entry['nom']}",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Personne recherchée modifiée avec succès');
    }

    /**
     * DELETE /api/personne-recherchee/{id}
     * Photos are removed by the ON DELETE CASCADE FK; files on disk
     * are cleaned up here.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = PersonneRecherchee::find($id);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        // Remove photo files from disk before the cascade delete.
        foreach (PersonneRechercheePhoto::listForPersonne($id) as $photo) {
            $filePath = $this->photoDir . '/' . $photo['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        PersonneRecherchee::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'personne_recherchee',
            'entity_id' => $id,
            'description' => "Suppression d'une personne recherchée — {$entry['nom']}",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Personne recherchée supprimée avec succès');
    }

    // ========================
    // Photo endpoints (dedicated multi-image, NOT generic attachments)
    // ========================

    /**
     * GET /api/personne-recherchee/{id}/photos
     */
    public function photosIndex(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $personneId = (int) $params['id'];
        $entry = PersonneRecherchee::find($personneId);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }
        $photos = PersonneRechercheePhoto::listForPersonne($personneId);
        Response::success($photos);
    }

    /**
     * POST /api/personne-recherchee/{id}/photos
     * Multipart: caption (optional) + file (image) + capture_source (optional: CAMERA|GALLERY)
     */
    public function photosStore(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $personneId = (int) $params['id'];
        $entry = PersonneRecherchee::find($personneId);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
            Response::error('File is required', 422, ['file' => 'Le fichier image est requis']);
        }

        $uploadedFile = $_FILES['file'];
        $mimeType = $uploadedFile['type'] ?? null;

        // Validate that the file is an image.
        if ($mimeType && !str_starts_with($mimeType, 'image/')) {
            Response::error('Only image files are allowed', 422, ['file' => 'Seuls les fichiers image sont autorisés']);
        }

        $caption = $_POST['caption'] ?? null;
        $captureSource = $_POST['capture_source'] ?? null;
        if ($captureSource !== null && !in_array($captureSource, PersonneRechercheePhoto::CAPTURE_SOURCES, true)) {
            $captureSource = null;
        }

        $extension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
        $safeName = self::sanitizeFilename($entry['nom']);
        $storedName = $safeName . '_' . uniqid() . '.' . $extension;

        $destPath = $this->photoDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        // Try to read image dimensions.
        $width = null;
        $height = null;
        $imageInfo = @getimagesize($destPath);
        if ($imageInfo !== false) {
            $width = $imageInfo[0];
            $height = $imageInfo[1];
            if ($mimeType === null) {
                $mimeType = $imageInfo['mime'];
            }
        }

        $sortOrder = PersonneRechercheePhoto::nextSortOrder($personneId);

        $photoId = PersonneRechercheePhoto::create([
            'personne_recherchee_id' => $personneId,
            'caption' => $caption,
            'filename' => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type' => $mimeType,
            'file_size' => $uploadedFile['size'] ?? null,
            'width' => $width,
            'height' => $height,
            'capture_source' => $captureSource,
            'sort_order' => $sortOrder,
        ]);

        $photo = PersonneRechercheePhoto::find($photoId);
        Response::created($photo, 'Photo ajoutée avec succès');
    }

    /**
     * PUT /api/personne-recherchee/{id}/photos/{photoId}
     * JSON: caption (optional)
     */
    public function photosUpdate(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $personneId = (int) $params['id'];
        $photoId = (int) $params['photoId'];

        $entry = PersonneRecherchee::find($personneId);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        $photo = PersonneRechercheePhoto::find($photoId);
        if (!$photo || !PersonneRechercheePhoto::belongsToPersonne($photoId, $personneId)) {
            Response::notFound('Photo not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $caption = $data['caption'] ?? null;

        PersonneRechercheePhoto::updateCaption($photoId, $caption);
        $photo = PersonneRechercheePhoto::find($photoId);

        Response::success($photo, 'Photo mise à jour avec succès');
    }

    /**
     * DELETE /api/personne-recherchee/{id}/photos/{photoId}
     */
    public function photosDestroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $personneId = (int) $params['id'];
        $photoId = (int) $params['photoId'];

        $entry = PersonneRecherchee::find($personneId);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        $photo = PersonneRechercheePhoto::find($photoId);
        if (!$photo || !PersonneRechercheePhoto::belongsToPersonne($photoId, $personneId)) {
            Response::notFound('Photo not found');
        }

        $filePath = $this->photoDir . '/' . $photo['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        PersonneRechercheePhoto::delete($photoId);
        Response::success(null, 'Photo supprimée avec succès');
    }

    /**
     * GET /api/personne-recherchee/{id}/photos/{photoId}/download
     */
    public function photosDownload(array $params): void
    {
        $personneId = (int) $params['id'];
        $photoId = (int) $params['photoId'];

        $entry = PersonneRecherchee::find($personneId);
        if (!$entry) {
            Response::notFound('Personne recherchée introuvable');
        }

        $photo = PersonneRechercheePhoto::find($photoId);
        if (!$photo || !PersonneRechercheePhoto::belongsToPersonne($photoId, $personneId)) {
            Response::notFound('Photo not found');
        }

        $filePath = $this->photoDir . '/' . $photo['filename'];
        if (!file_exists($filePath)) {
            Response::notFound('File not found on disk');
        }

        $mime = $photo['mime_type'] ?: 'application/octet-stream';
        header('Content-Type: ' . $mime);
        header('Content-Disposition: inline; filename="' . $photo['original_filename'] . '"');
        header('Content-Length: ' . filesize($filePath));
        readfile($filePath);
        exit;
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle personne recherchée' : 'Personne recherchée modifiée';

        $adminMessage = "Une personne recherchée a été {$verb}. Nom: {$entry['nom']}.";
        $userMessage = "Une personne recherchée a été {$verb}. Nom: {$entry['nom']}. Veuillez en prendre connaissance.";

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => $adminMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $userMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
