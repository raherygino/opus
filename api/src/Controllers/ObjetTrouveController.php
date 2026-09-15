<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\ObjetTrouve;
use App\Models\ObjetTrouveAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class ObjetTrouveController
{
    private const MODULE = 'pj_objets';
    private const LINK_PREFIX = '/pj/objets/trouve/';

    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('affaire', $data)) {
            if (empty(trim((string) ($data['affaire'] ?? '')))) {
                $errors['affaire'] = "L'affaire est requise";
            }
        }

        if ($isCreate || array_key_exists('motif_decouverte', $data)) {
            $value = $data['motif_decouverte'] ?? null;
            if (empty($value)) {
                $errors['motif_decouverte'] = 'Le motif de découverte est requis';
            } elseif (!in_array($value, ObjetTrouve::MOTIFS, true)) {
                $errors['motif_decouverte'] = 'Le motif de découverte est invalide';
            }
        }

        return $errors;
    }

    /**
     * GET /api/objets/trouve
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['motif_decouverte', 'restitution', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        Response::success(ObjetTrouve::all($filters));
    }

    /**
     * GET /api/objets/trouve/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = ObjetTrouve::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Objet trouvé introuvable');
        }
        $row['attachments'] = ObjetTrouveAttachment::listForObjet((int) $row['id']);

        if (!empty($authUser['sub'])) {
            Notification::markAsReadByLink(self::LINK_PREFIX . $row['id'], (int) $authUser['sub']);
        }
        Response::success($row);
    }

    /**
     * POST /api/objets/trouve
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
        if (isset($data['affaire']) && is_string($data['affaire'])) {
            $data['affaire'] = trim($data['affaire']);
        }
        $data['restitution'] = !empty($data['restitution']);

        $id = ObjetTrouve::create($data);
        $entry = ObjetTrouve::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'objet_trouve',
            'entity_id' => $id,
            'description' => "Création d'un objet trouvé ({$entry['motif_decouverte']})",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Objet trouvé enregistré avec succès');
    }

    /**
     * PUT /api/objets/trouve/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = ObjetTrouve::find($id);
        if (!$entry) {
            Response::notFound('Objet trouvé introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['affaire']) && is_string($data['affaire'])) {
            $data['affaire'] = trim($data['affaire']);
        }
        $data['restitution'] = !empty($data['restitution']);

        $oldEntry = $entry;
        ObjetTrouve::update($id, $data);
        $entry = ObjetTrouve::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'objet_trouve',
            'entity_id' => $id,
            'description' => "Modification d'un objet trouvé ({$entry['motif_decouverte']})",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Objet trouvé modifié avec succès');
    }

    /**
     * DELETE /api/objets/trouve/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = ObjetTrouve::find($id);
        if (!$entry) {
            Response::notFound('Objet trouvé introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/objet_trouve';
        foreach (ObjetTrouveAttachment::listForObjet($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        ObjetTrouve::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'objet_trouve',
            'entity_id' => $id,
            'description' => "Suppression d'un objet trouvé ({$entry['motif_decouverte']})",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Objet trouvé supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $motifLabel = ObjetTrouve::MOTIF_LABELS[$entry['motif_decouverte']] ?? $entry['motif_decouverte'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouvel objet trouvé' : 'Objet trouvé modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un objet trouvé a été {$verb}. Motif: {$motifLabel}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un objet trouvé a été {$verb}. Motif: {$motifLabel}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
