<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\MaterielRoulant;
use App\Models\MaterielRoulantAttachment;

class MaterielRoulantAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/materiels-roulants';
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
     * GET /api/materiels-roulants/{id}/attachments
     */
    public function index(array $params): void
    {
        $materielRoulantId = (int) $params['id'];
        $materiel = MaterielRoulant::getById($materielRoulantId);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        $attachments = MaterielRoulantAttachment::getByMaterielRoulantId($materielRoulantId);
        Response::success($attachments);
    }

    /**
     * POST /api/materiels-roulants/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $materielRoulantId = (int) $params['id'];
        $materiel = MaterielRoulant::getById($materielRoulantId);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
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
        $owner = self::sanitizeFilename($materiel['date_perception'] ?? '');
        $storedName = $safeTitle . '_' . $owner . '_' . uniqid() . '.' . $extension;

        $destPath = $this->uploadDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        $id = MaterielRoulantAttachment::create([
            'materiel_roulant_id' => $materielRoulantId,
            'title'                => $title,
            'filename'             => $storedName,
            'original_filename'    => $uploadedFile['name'],
            'mime_type'            => $uploadedFile['type'] ?? null,
            'file_size'            => $uploadedFile['size'] ?? null,
        ]);

        $attachment = MaterielRoulantAttachment::getById($id);
        Response::created($attachment, 'Attachment added successfully');
    }

    /**
     * PUT /api/materiels-roulants/{id}/attachments/{attachId}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $materielRoulantId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $materiel = MaterielRoulant::getById($materielRoulantId);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        $attachment = MaterielRoulantAttachment::getById($attachId);
        if (!$attachment || !MaterielRoulantAttachment::belongsToMaterielRoulant($attachId, $materielRoulantId)) {
            Response::notFound('Attachment not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        MaterielRoulantAttachment::update($attachId, ['title' => $data['title']]);
        $attachment = MaterielRoulantAttachment::getById($attachId);

        Response::success($attachment, 'Attachment updated successfully');
    }

    /**
     * DELETE /api/materiels-roulants/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $materielRoulantId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $materiel = MaterielRoulant::getById($materielRoulantId);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        $attachment = MaterielRoulantAttachment::getById($attachId);
        if (!$attachment || !MaterielRoulantAttachment::belongsToMaterielRoulant($attachId, $materielRoulantId)) {
            Response::notFound('Attachment not found');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        MaterielRoulantAttachment::delete($attachId);
        Response::success(null, 'Attachment deleted successfully');
    }

    /**
     * GET /api/materiels-roulants/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $materielRoulantId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $materiel = MaterielRoulant::getById($materielRoulantId);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        $attachment = MaterielRoulantAttachment::getById($attachId);
        if (!$attachment || !MaterielRoulantAttachment::belongsToMaterielRoulant($attachId, $materielRoulantId)) {
            Response::notFound('Attachment not found');
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
