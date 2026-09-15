<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RenseignementPj;
use App\Models\RenseignementPjAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class RenseignementPjController
{
    /** Permission module code for the RENSEIGNEMENT PJ feature. */
    private const MODULE = 'pj_renseignement';

    /** Notification link prefix for RENSEIGNEMENT detail. */
    private const LINK_PREFIX = '/pj/renseignement/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── nature_infraction ────────────────────────────────────────
        if ($isCreate || array_key_exists('nature_infraction', $data)) {
            if (empty(trim((string) ($data['nature_infraction'] ?? '')))) {
                $errors['nature_infraction'] = "La nature de l'infraction est requise";
            }
        }

        return $errors;
    }

    /**
     * GET /api/renseignements-pj
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
        $list = RenseignementPj::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/renseignements-pj/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = RenseignementPj::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Renseignement introuvable');
        }
        $row['attachments'] = RenseignementPjAttachment::listForRenseignement((int) $row['id']);

        // Auto-dismiss the notification for the viewing user.
        if (!empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                self::LINK_PREFIX . $row['id'],
                (int) $authUser['sub']
            );
        }
        Response::success($row);
    }

    /**
     * POST /api/renseignements-pj
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

        // Trim text fields.
        foreach (['nature_infraction', 'date_lieu_faits', 'circonstances', 'prejudices'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = RenseignementPj::create($data);
        $renseignement = RenseignementPj::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'renseignement_pj',
            'entity_id' => $id,
            'description' => "Création d'un renseignement PJ — {$renseignement['nature_infraction']}",
            'new_values' => $renseignement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $renseignement, (int) $authUser['sub']);

        Response::created($renseignement, 'Renseignement enregistré avec succès');
    }

    /**
     * PUT /api/renseignements-pj/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $renseignement = RenseignementPj::find($id);
        if (!$renseignement) {
            Response::notFound('Renseignement introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Trim text fields.
        foreach (['nature_infraction', 'date_lieu_faits', 'circonstances', 'prejudices'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $renseignement;
        RenseignementPj::update($id, $data);
        $renseignement = RenseignementPj::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'renseignement_pj',
            'entity_id' => $id,
            'description' => "Modification d'un renseignement PJ — {$renseignement['nature_infraction']}",
            'old_values' => $oldEntry,
            'new_values' => $renseignement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $renseignement, $authUser['sub'] ?? null);

        Response::success($renseignement, 'Renseignement modifié avec succès');
    }

    /**
     * DELETE /api/renseignements-pj/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $renseignement = RenseignementPj::find($id);
        if (!$renseignement) {
            Response::notFound('Renseignement introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/renseignement_pj';
        foreach (RenseignementPjAttachment::listForRenseignement($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        RenseignementPj::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'renseignement_pj',
            'entity_id' => $id,
            'description' => "Suppression d'un renseignement PJ — {$renseignement['nature_infraction']}",
            'old_values' => $renseignement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Renseignement supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouveau renseignement' : 'Renseignement modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un renseignement PJ a été {$verb}. {$entry['nature_infraction']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un renseignement PJ a été {$verb}. {$entry['nature_infraction']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
