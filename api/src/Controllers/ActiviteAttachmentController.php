<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Activite;
use App\Models\ActiviteAttachment;

class ActiviteAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/activites';
        if (!is_dir($this->uploadDir)) {
            mkdir($this->uploadDir, 0755, true);
        }
    }

    public static function sanitizeFilename(string $name): string
    {
        $name = mb_strtolower($name, 'UTF-8');
        $name = preg_replace('/[^a-z0-9]+/', '_', $name);
        return trim($name, '_');
    }

    /**
     * GET /api/activites/{id}/attachments
     */
    public function index(array $params): void
    {
        $activiteId = (int) $params['id'];
        $activite = Activite::find($activiteId);
        if (!$activite) {
            Response::notFound('Activité introuvable');
        }

        $attachments = ActiviteAttachment::getByActiviteId($activiteId);
        Response::success($attachments);
    }

    /**
     * POST /api/activites/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $activiteId = (int) $params['id'];
        $activite = Activite::find($activiteId);
        if (!$activite) {
            Response::notFound('Activité introuvable');
        }

        $title = $_POST['title'] ?? '';
        if (empty($title)) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
            Response::error('File is required', 422, ['file' => 'Le fichier est requis']);
        }

        $uploadedFile = $_FILES['file'];
        $extension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));

        $safeTitle = self::sanitizeFilename($title);
        $owner = self::sanitizeFilename($activite['date_activite'] ?? '');
        $storedName = $safeTitle . '_' . $owner . '_' . uniqid() . '.' . $extension;

        $destPath = $this->uploadDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        $id = ActiviteAttachment::create([
            'activite_id'       => $activiteId,
            'title'             => $title,
            'filename'          => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type'         => $uploadedFile['type'] ?? null,
            'file_size'         => $uploadedFile['size'] ?? null,
        ]);

        $attachment = ActiviteAttachment::getById($id);
        Response::created($attachment, 'Pièce jointe ajoutée avec succès');
    }

    /**
     * PUT /api/activites/{id}/attachments/{attachId}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $activiteId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $activite = Activite::find($activiteId);
        if (!$activite) {
            Response::notFound('Activité introuvable');
        }

        $attachment = ActiviteAttachment::getById($attachId);
        if (!$attachment || !ActiviteAttachment::belongsToActivite($attachId, $activiteId)) {
            Response::notFound('Pièce jointe introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        ActiviteAttachment::update($attachId, ['title' => $data['title']]);
        $attachment = ActiviteAttachment::getById($attachId);

        Response::success($attachment, 'Pièce jointe modifiée avec succès');
    }

    /**
     * DELETE /api/activites/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $activiteId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $activite = Activite::find($activiteId);
        if (!$activite) {
            Response::notFound('Activité introuvable');
        }

        $attachment = ActiviteAttachment::getById($attachId);
        if (!$attachment || !ActiviteAttachment::belongsToActivite($attachId, $activiteId)) {
            Response::notFound('Pièce jointe introuvable');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        ActiviteAttachment::delete($attachId);
        Response::success(null, 'Pièce jointe supprimée avec succès');
    }

    /**
     * GET /api/activites/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $activiteId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $activite = Activite::find($activiteId);
        if (!$activite) {
            Response::notFound('Activité introuvable');
        }

        $attachment = ActiviteAttachment::getById($attachId);
        if (!$attachment || !ActiviteAttachment::belongsToActivite($attachId, $activiteId)) {
            Response::notFound('Pièce jointe introuvable');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (!file_exists($filePath)) {
            Response::notFound('File not found on disk');
        }

        $mime = $attachment['mime_type'] ?: 'application/octet-stream';
        header('Content-Type: ' . $mime);
        header('Content-Disposition: attachment; filename="' . $attachment['original_filename'] . '"');
        header('Content-Length: ' . filesize($filePath));
        readfile($filePath);
        exit;
    }
}
