<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RenseignementPj;
use App\Models\RenseignementPjAttachment;

class RenseignementPjAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/renseignement_pj';
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
     * GET /api/renseignements-pj/{id}/attachments
     */
    public function index(array $params): void
    {
        $renseignementId = (int) $params['id'];
        if (!RenseignementPj::find($renseignementId)) {
            Response::notFound('Renseignement introuvable');
        }
        Response::success(RenseignementPjAttachment::listForRenseignement($renseignementId));
    }

    /**
     * POST /api/renseignements-pj/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $renseignementId = (int) $params['id'];
        $entry = RenseignementPj::find($renseignementId);
        if (!$entry) {
            Response::notFound('Renseignement introuvable');
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

        $id = RenseignementPjAttachment::create([
            'renseignement_id'  => $renseignementId,
            'title'             => $title,
            'filename'          => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type'         => $uploadedFile['type'] ?? null,
            'file_size'         => $uploadedFile['size'] ?? null,
        ]);

        Response::created(RenseignementPjAttachment::find($id), 'Attachment added successfully');
    }

    /**
     * PUT /api/renseignements-pj/{id}/attachments/{attachId}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $renseignementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!RenseignementPj::find($renseignementId)) {
            Response::notFound('Renseignement introuvable');
        }

        $attachment = RenseignementPjAttachment::find($attachId);
        if (!$attachment || !RenseignementPjAttachment::belongsToRenseignement($attachId, $renseignementId)) {
            Response::notFound('Attachment not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        RenseignementPjAttachment::updateTitle($attachId, $data['title']);
        Response::success(RenseignementPjAttachment::find($attachId), 'Attachment updated successfully');
    }

    /**
     * DELETE /api/renseignements-pj/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $renseignementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!RenseignementPj::find($renseignementId)) {
            Response::notFound('Renseignement introuvable');
        }

        $attachment = RenseignementPjAttachment::find($attachId);
        if (!$attachment || !RenseignementPjAttachment::belongsToRenseignement($attachId, $renseignementId)) {
            Response::notFound('Attachment not found');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        RenseignementPjAttachment::delete($attachId);
        Response::success(null, 'Attachment deleted successfully');
    }

    /**
     * GET /api/renseignements-pj/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $renseignementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        if (!RenseignementPj::find($renseignementId)) {
            Response::notFound('Renseignement introuvable');
        }

        $attachment = RenseignementPjAttachment::find($attachId);
        if (!$attachment || !RenseignementPjAttachment::belongsToRenseignement($attachId, $renseignementId)) {
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
