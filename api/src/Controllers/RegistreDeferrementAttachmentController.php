<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RegistreDeferrement;
use App\Models\RegistreDeferrementAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class RegistreDeferrementAttachmentController
{
    /** Permission module code for the REGISTRE DE DÉFERREMENT feature. */
    private const MODULE = 'pj_deferrement';

    /** Sub-folder under api/uploads/ where files are stored. */
    private const UPLOAD_SUBDIR = 'registre_deferrement';

    private const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /** Upload directory (derived from config/upload_dir). */
    private static function uploadDir(): string
    {
        $config = require __DIR__ . '/../../config/app.php';
        return rtrim($config['upload_dir'], '/') . '/' . self::UPLOAD_SUBDIR;
    }

    private static function findAttachment(int $id, int $deferrementId): ?array
    {
        $attachment = RegistreDeferrementAttachment::find($id);
        if (!$attachment || !RegistreDeferrementAttachment::belongsToDeferrement($id, $deferrementId)) {
            return null;
        }
        return $attachment;
    }

    /**
     * GET /api/registres-deferrement/{id}/attachments
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $deferrementId = (int) $params['id'];
        if (!RegistreDeferrement::find($deferrementId)) {
            Response::notFound('Déferrement introuvable');
        }
        Response::success(RegistreDeferrementAttachment::listForDeferrement($deferrementId));
    }

    /**
     * GET /api/registres-deferrement/{id}/attachments/{attachId}/download
     */
    public function download(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $deferrementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];
        $attachment = self::findAttachment($attachId, $deferrementId);
        if (!$attachment) {
            Response::notFound('Pièce jointe introuvable');
        }

        $filePath = self::uploadDir() . '/' . $attachment['filename'];
        if (!file_exists($filePath) || !is_readable($filePath)) {
            Response::notFound('Fichier introuvable sur le serveur');
        }

        $mime = $attachment['mime_type'] ?: mime_content_type($filePath) ?: 'application/octet-stream';
        header('Content-Type: ' . $mime);
        header('Content-Length: ' . filesize($filePath));
        header('Content-Disposition: inline; filename="' . rawurlencode($attachment['original_filename']) . '"');
        header('Cache-Control: private, max-age=3600');
        readfile($filePath);
        exit;
    }

    /**
     * POST /api/registres-deferrement/{id}/attachments (multipart: title + file)
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $deferrementId = (int) $params['id'];
        if (!RegistreDeferrement::find($deferrementId)) {
            Response::notFound('Déferrement introuvable');
        }

        $title = trim((string) ($_POST['title'] ?? ''));
        if ($title === '') {
            Response::error('Validation failed', 422, ['title' => 'Le titre est requis']);
        }
        if (!isset($_FILES['file']) || $_FILES['file']['error'] !== UPLOAD_ERR_OK) {
            Response::error('Validation failed', 422, ['file' => 'Le fichier est requis']);
        }
        if ($_FILES['file']['size'] > self::MAX_FILE_SIZE) {
            Response::error('Validation failed', 422, ['file' => 'Le fichier dépasse 10 Mo']);
        }

        $uploadDir = self::uploadDir();
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0775, true);
        }

        $origName = $_FILES['file']['name'];
        $ext = strtolower(pathinfo($origName, PATHINFO_EXTENSION));
        $safeExt = $ext !== '' ? '.' . $ext : '';
        $filename = 'def' . $deferrementId . '_' . uniqid('', true) . $safeExt;
        $dest = $uploadDir . '/' . $filename;

        if (!move_uploaded_file($_FILES['file']['tmp_name'], $dest)) {
            Response::error("Impossible d'enregistrer le fichier", 500);
        }

        $id = RegistreDeferrementAttachment::create([
            'registre_deferrement_id' => $deferrementId,
            'title' => $title,
            'filename' => $filename,
            'original_filename' => $origName,
            'mime_type' => $_FILES['file']['type'] ?? null,
            'file_size' => $_FILES['file']['size'] ?? null,
        ]);
        $attachment = RegistreDeferrementAttachment::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'upload',
            'module' => 'registre_deferrement',
            'entity_id' => $deferrementId,
            'description' => "Ajout d'une pièce jointe à une entrée du registre de déferrement — {$title}",
            'new_values' => $attachment,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => 'Nouvelle pièce jointe PJ',
            'message' => "Une pièce jointe « {$title} » a été ajoutée au registre de déferrement.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => '/pj/registre-deferrement/' . $deferrementId,
        ], [
            'title'   => 'Nouvelle pièce jointe PJ',
            'message' => "Une pièce jointe « {$title} » a été ajoutée au registre de déferrement. Veuillez la consulter.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => '/pj/registre-deferrement/' . $deferrementId,
        ], (int) $authUser['sub']);

        Response::created($attachment, 'Pièce jointe ajoutée');
    }

    /**
     * PUT /api/registres-deferrement/{id}/attachments/{attachId} (JSON: title)
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $deferrementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];
        $attachment = self::findAttachment($attachId, $deferrementId);
        if (!$attachment) {
            Response::notFound('Pièce jointe introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $title = trim((string) ($data['title'] ?? ''));
        if ($title === '') {
            Response::error('Validation failed', 422, ['title' => 'Le titre est requis']);
        }

        RegistreDeferrementAttachment::updateTitle($attachId, $title);
        $attachment = RegistreDeferrementAttachment::find($attachId);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'registre_deferrement',
            'entity_id' => $deferrementId,
            'description' => "Modification d'une pièce jointe du registre de déferrement — {$title}",
            'new_values' => $attachment,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($attachment, 'Titre mis à jour');
    }

    /**
     * DELETE /api/registres-deferrement/{id}/attachments/{attachId}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $deferrementId = (int) $params['id'];
        $attachId = (int) $params['attachId'];
        $attachment = self::findAttachment($attachId, $deferrementId);
        if (!$attachment) {
            Response::notFound('Pièce jointe introuvable');
        }

        $filePath = self::uploadDir() . '/' . $attachment['filename'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }
        RegistreDeferrementAttachment::delete($attachId);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'registre_deferrement',
            'entity_id' => $deferrementId,
            'description' => "Suppression d'une pièce jointe du registre de déferrement — {$attachment['title']}",
            'old_values' => $attachment,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Pièce jointe supprimée');
    }
}
