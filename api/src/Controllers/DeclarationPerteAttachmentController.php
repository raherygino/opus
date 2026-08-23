<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\DeclarationPerte;
use App\Models\DeclarationPerteAttachment;

class DeclarationPerteAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/declarations-perte';
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
     * GET /api/declarations-perte/{id}/attachments
     */
    public function index(array $params): void
    {
        $declarationId = (int) $params['id'];
        $declaration = DeclarationPerte::getById($declarationId);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        $attachments = DeclarationPerteAttachment::getByDeclarationId($declarationId);
        Response::success($attachments);
    }

    /**
     * POST /api/declarations-perte/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $declarationId = (int) $params['id'];
        $declaration = DeclarationPerte::getById($declarationId);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
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
        $owner = self::sanitizeFilename($declaration['numero_attestation'] ?? '');
        $storedName = $safeTitle . '_' . $owner . '_' . uniqid() . '.' . $extension;

        $destPath = $this->uploadDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        $id = DeclarationPerteAttachment::create([
            'declaration_id'   => $declarationId,
            'title'            => $title,
            'filename'          => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type'         => $uploadedFile['type'] ?? null,
            'file_size'         => $uploadedFile['size'] ?? null,
        ]);

        $attachment = DeclarationPerteAttachment::getById($id);
        Response::created($attachment, 'Attachment added successfully');
    }

    /**
     * PUT /api/declarations-perte/{id}/attachments/{attachId}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $declarationId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $declaration = DeclarationPerte::getById($declarationId);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        $attachment = DeclarationPerteAttachment::getById($attachId);
        if (!$attachment || !DeclarationPerteAttachment::belongsToDeclaration($attachId, $declarationId)) {
            Response::notFound('Attachment not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        DeclarationPerteAttachment::update($attachId, ['title' => $data['title']]);
        $attachment = DeclarationPerteAttachment::getById($attachId);

        Response::success($attachment, 'Attachment updated successfully');
    }

    /**
     * DELETE /api/declarations-perte/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $declarationId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $declaration = DeclarationPerte::getById($declarationId);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        $attachment = DeclarationPerteAttachment::getById($attachId);
        if (!$attachment || !DeclarationPerteAttachment::belongsToDeclaration($attachId, $declarationId)) {
            Response::notFound('Attachment not found');
        }

        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        DeclarationPerteAttachment::delete($attachId);
        Response::success(null, 'Attachment deleted successfully');
    }

    /**
     * GET /api/declarations-perte/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $declarationId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $declaration = DeclarationPerte::getById($declarationId);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        $attachment = DeclarationPerteAttachment::getById($attachId);
        if (!$attachment || !DeclarationPerteAttachment::belongsToDeclaration($attachId, $declarationId)) {
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
