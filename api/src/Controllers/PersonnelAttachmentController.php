<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Personnel;
use App\Models\PersonnelAttachment;

class PersonnelAttachmentController
{
    private string $uploadDir;

    public function __construct()
    {
        $config = require __DIR__ . '/../../config/app.php';
        $this->uploadDir = rtrim($config['upload_dir'], '/') . '/personnel';
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
     * GET /api/personnel/{id}/attachments
     */
    public function index(array $params): void
    {
        $personnelId = (int) $params['id'];
        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $attachments = PersonnelAttachment::getByPersonnelId($personnelId);
        Response::success($attachments);
    }

    /**
     * POST /api/personnel/{id}/attachments
     * Multipart: title + file
     */
    public function store(array $params): void
    {
        $personnelId = (int) $params['id'];
        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel not found');
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
        $owner = self::sanitizeFilename(($person['lastname'] ?? '') . '_' . ($person['firstname'] ?? ''));
        $storedName = $safeTitle . '_' . $owner . '_' . uniqid() . '.' . $extension;

        $destPath = $this->uploadDir . '/' . $storedName;
        if (!move_uploaded_file($uploadedFile['tmp_name'], $destPath)) {
            Response::error('Failed to save file', 500);
        }

        $id = PersonnelAttachment::create([
            'personnel_id'      => $personnelId,
            'title'             => $title,
            'filename'          => $storedName,
            'original_filename' => $uploadedFile['name'],
            'mime_type'         => $uploadedFile['type'] ?? null,
            'file_size'         => $uploadedFile['size'] ?? null,
        ]);

        $attachment = PersonnelAttachment::getById($id);
        Response::created($attachment, 'Attachment added successfully');
    }

    /**
     * PUT /api/personnel/{id}/attachments/{attachId}
     * JSON body: { "title": "New Title" }
     *
     * Note: File replacement is handled via DELETE + POST (upload new).
     */
    public function update(array $params): void
    {
        $personnelId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $attachment = PersonnelAttachment::getById($attachId);
        if (!$attachment || !PersonnelAttachment::belongsToPersonnel($attachId, $personnelId)) {
            Response::notFound('Attachment not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['title'])) {
            Response::error('Title is required', 422, ['title' => 'Le titre est requis']);
        }

        PersonnelAttachment::update($attachId, ['title' => $data['title']]);
        $attachment = PersonnelAttachment::getById($attachId);

        Response::success($attachment, 'Attachment updated successfully');
    }

    /**
     * DELETE /api/personnel/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $personnelId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $attachment = PersonnelAttachment::getById($attachId);
        if (!$attachment || !PersonnelAttachment::belongsToPersonnel($attachId, $personnelId)) {
            Response::notFound('Attachment not found');
        }

        // Delete file from disk
        $filePath = $this->uploadDir . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        PersonnelAttachment::delete($attachId);
        Response::success(null, 'Attachment deleted successfully');
    }

    /**
     * GET /api/personnel/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $personnelId = (int) $params['id'];
        $attachId = (int) $params['attachId'];

        $person = Personnel::getById($personnelId);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $attachment = PersonnelAttachment::getById($attachId);
        if (!$attachment || !PersonnelAttachment::belongsToPersonnel($attachId, $personnelId)) {
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
