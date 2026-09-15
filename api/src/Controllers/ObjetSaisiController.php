<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\ObjetSaisi;
use App\Models\ObjetSaisiAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class ObjetSaisiController
{
    private const MODULE = 'pj_objets';
    private const LINK_PREFIX = '/pj/objets/saisi/';

    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('motif', $data)) {
            if (empty(trim((string) ($data['motif'] ?? '')))) {
                $errors['motif'] = 'Le motif est requis';
            }
        }

        if ($isCreate || array_key_exists('type_objet', $data)) {
            $value = $data['type_objet'] ?? null;
            if (empty($value)) {
                $errors['type_objet'] = "Le type d'objet est requis";
            } elseif (!in_array($value, ObjetSaisi::TYPES, true)) {
                $errors['type_objet'] = "Le type d'objet est invalide";
            }
        }

        return $errors;
    }

    /**
     * GET /api/objets/saisi
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['type_objet', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        Response::success(ObjetSaisi::all($filters));
    }

    /**
     * GET /api/objets/saisi/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = ObjetSaisi::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Objet saisi introuvable');
        }
        $row['attachments'] = ObjetSaisiAttachment::listForObjet((int) $row['id']);

        if (!empty($authUser['sub'])) {
            Notification::markAsReadByLink(self::LINK_PREFIX . $row['id'], (int) $authUser['sub']);
        }
        Response::success($row);
    }

    /**
     * POST /api/objets/saisi
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
        foreach (['numero_dossier', 'motif', 'proprietaire'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = ObjetSaisi::create($data);
        $entry = ObjetSaisi::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'objet_saisi',
            'entity_id' => $id,
            'description' => "Création d'un objet saisi ({$entry['type_objet']})",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Objet saisi enregistré avec succès');
    }

    /**
     * PUT /api/objets/saisi/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = ObjetSaisi::find($id);
        if (!$entry) {
            Response::notFound('Objet saisi introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        foreach (['numero_dossier', 'motif', 'proprietaire'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $entry;
        ObjetSaisi::update($id, $data);
        $entry = ObjetSaisi::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'objet_saisi',
            'entity_id' => $id,
            'description' => "Modification d'un objet saisi ({$entry['type_objet']})",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Objet saisi modifié avec succès');
    }

    /**
     * DELETE /api/objets/saisi/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = ObjetSaisi::find($id);
        if (!$entry) {
            Response::notFound('Objet saisi introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/objet_saisi';
        foreach (ObjetSaisiAttachment::listForObjet($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        ObjetSaisi::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'objet_saisi',
            'entity_id' => $id,
            'description' => "Suppression d'un objet saisi ({$entry['type_objet']})",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Objet saisi supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $typeLabel = ObjetSaisi::TYPE_LABELS[$entry['type_objet']] ?? $entry['type_objet'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouvel objet saisi' : 'Objet saisi modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un objet saisi a été {$verb}. Type: {$typeLabel}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un objet saisi a été {$verb}. Type: {$typeLabel}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
