<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\ObjetSaisi;
use App\Models\ObjetSaisiAttachment;

class ObjetSaisiAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/objet_saisi';
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
     * GET /api/objets/saisi/{id}/attachments
     */
    public function index(array $params): void
    {
        $objetId = (int) $params['id'];
        if (!ObjetSaisi::find($objetId)) {
            Response::notFound('Objet saisi introuvable');
        }
        Response::success(ObjetSaisiAttachment::listForObjet($objetId));
    }

    /**
     * POST /api/objets/saisi/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $objetId = (int) $params['id'];
        $entry = ObjetSaisi::find($objetId);
        if (!$entry) {
            Response::notFound('Objet saisi introuvable');
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
        $storedName = $safeTitle . '_' . uniqid() . '.' . $extension;

        $destPath = $this->uploadDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        $id = ObjetSaisiAttachment::create([
            'objet_saisi_id'    => $objetId,
            'title'             => $title,
            'filename'          => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type'         => $uploadedFile['type'] ?? null,
            'file_size'         => $uploadedFile['size'] ?? null,
        ]);

        Response::created(ObjetSaisiAttachment::find($id), 'Attachment added successfully');
    }

    /**
     * PUT /api/objets/saisi/{id}/attachments/{attachId}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $objetId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!ObjetSaisi::find($objetId)) {
            Response::notFound('Objet saisi introuvable');
        }

        $attachment = ObjetSaisiAttachment::find($attachId);
        if (!$attachment || !ObjetSaisiAttachment::belongsToObjet($attachId, $objetId)) {
            Response::notFound('Attachment not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        ObjetSaisiAttachment::updateTitle($attachId, $data['title']);
        Response::success(ObjetSaisiAttachment::find($attachId), 'Attachment updated successfully');
    }

    /**
     * DELETE /api/objets/saisi/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $objetId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!ObjetSaisi::find($objetId)) {
            Response::notFound('Objet saisi introuvable');
        }

        $attachment = ObjetSaisiAttachment::find($attachId);
        if (!$attachment || !ObjetSaisiAttachment::belongsToObjet($attachId, $objetId)) {
            Response::notFound('Attachment not found');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        ObjetSaisiAttachment::delete($attachId);
        Response::success(null, 'Attachment deleted successfully');
    }

    /**
     * GET /api/objets/saisi/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $objetId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!ObjetSaisi::find($objetId)) {
            Response::notFound('Objet saisi introuvable');
        }

        $attachment = ObjetSaisiAttachment::find($attachId);
        if (!$attachment || !ObjetSaisiAttachment::belongsToObjet($attachId, $objetId)) {
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
