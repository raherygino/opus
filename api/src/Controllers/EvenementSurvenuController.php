<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\EvenementSurvenu;
use App\Models\EvenementSurvenuAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class EvenementSurvenuController
{
    /** Permission module code for the ÉVÈNEMENTS SURVENUS feature. */
    private const MODULE = 'sg_evenement_survenu';

    /** Notification link prefix for ÉVÈNEMENTS SURVENUS detail. */
    private const LINK_PREFIX = '/sg/evenements-survenus/';

    /** Text fields trimmed before persistence (empty string → null). */
    private const TEXT_FIELDS = [
        'lieu_exact', 'auteurs_presumes', 'victimes', 'temoins', 'mesures_prises',
    ];

    /** All writable columns (used to keep existing values on partial update). */
    private const FIELDS = [
        'date_evenement', 'heure_evenement', 'type_evenement', 'lieu_exact',
        'auteurs_presumes', 'victimes', 'temoins', 'mesures_prises',
        'latitude', 'longitude',
    ];

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── date_evenement ──────────────────────────────────────────
        if ($isCreate || array_key_exists('date_evenement', $data)) {
            $value = trim((string) ($data['date_evenement'] ?? ''));
            if ($value === '') {
                $errors['date_evenement'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) && !strtotime($value)) {
                $errors['date_evenement'] = 'La date est invalide';
            }
        }

        // ── heure_evenement ─────────────────────────────────────────
        if ($isCreate || array_key_exists('heure_evenement', $data)) {
            $value = trim((string) ($data['heure_evenement'] ?? ''));
            if ($value === '') {
                $errors['heure_evenement'] = "L'heure est requise";
            }
        }

        // ── type_evenement ──────────────────────────────────────────
        if ($isCreate || array_key_exists('type_evenement', $data)) {
            $value = trim((string) ($data['type_evenement'] ?? ''));
            if ($value === '') {
                $errors['type_evenement'] = "Le type d'événement est requis";
            } elseif (!in_array($value, EvenementSurvenu::TYPES, true)) {
                $errors['type_evenement'] = "Le type d'événement est invalide";
            }
        }

        // ── lieu_exact ──────────────────────────────────────────────
        if ($isCreate || array_key_exists('lieu_exact', $data)) {
            if (empty(trim((string) ($data['lieu_exact'] ?? '')))) {
                $errors['lieu_exact'] = 'Le lieu exact est requis';
            }
        }

        // ── latitude / longitude (optional, sent by mobile) ──────────
        if (isset($data['latitude']) && $data['latitude'] !== '' && !is_numeric($data['latitude'])) {
            $errors['latitude'] = 'La latitude doit être un nombre';
        }
        if (isset($data['longitude']) && $data['longitude'] !== '' && !is_numeric($data['longitude'])) {
            $errors['longitude'] = 'La longitude doit être un nombre';
        }

        return $errors;
    }

    /**
     * GET /api/evenements-survenus
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
        if (isset($_GET['type']) && $_GET['type'] !== '') {
            $filters['type'] = $_GET['type'];
        }
        $list = EvenementSurvenu::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/evenements-survenus/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = EvenementSurvenu::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Événement introuvable');
        }
        $row['attachments'] = EvenementSurvenuAttachment::getByEvenementId((int) $row['id']);

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
     * POST /api/evenements-survenus
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
        self::sanitize($data);

        $id = EvenementSurvenu::create($data);
        $entry = EvenementSurvenu::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'evenement_survenu',
            'entity_id' => $id,
            'description' => "Création d'un événement survenu — {$entry['date_evenement']} {$entry['heure_evenement']}",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Événement enregistré avec succès');
    }

    /**
     * PUT /api/evenements-survenus/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = EvenementSurvenu::find($id);
        if (!$entry) {
            Response::notFound('Événement introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Keep existing values for fields not provided.
        foreach (self::FIELDS as $f) {
            if (!array_key_exists($f, $data)) {
                $data[$f] = $entry[$f];
            }
        }

        self::sanitize($data);

        $oldEntry = $entry;
        EvenementSurvenu::update($id, $data);
        $entry = EvenementSurvenu::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'evenement_survenu',
            'entity_id' => $id,
            'description' => "Modification d'un événement survenu — {$entry['date_evenement']} {$entry['heure_evenement']}",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Événement modifié avec succès');
    }

    /**
     * DELETE /api/evenements-survenus/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = EvenementSurvenu::find($id);
        if (!$entry) {
            Response::notFound('Événement introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/evenements-survenus';
        foreach (EvenementSurvenuAttachment::getByEvenementId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        EvenementSurvenu::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'evenement_survenu',
            'entity_id' => $id,
            'description' => "Suppression d'un événement survenu — {$entry['date_evenement']} {$entry['heure_evenement']}",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Événement supprimé avec succès');
    }

    /** Trim text fields in place; empty strings become null. */
    private static function sanitize(array &$data): void
    {
        foreach (self::TEXT_FIELDS as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
            if (($data[$f] ?? null) === '') {
                $data[$f] = null;
            }
        }
        if (isset($data['type_evenement']) && is_string($data['type_evenement'])) {
            $data['type_evenement'] = trim($data['type_evenement']);
        }
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouvel événement survenu' : 'Événement survenu modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un événement survenu a été {$verb}. {$entry['date_evenement']} à {$entry['heure_evenement']} — {$entry['lieu_exact']}.",
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un événement survenu a été {$verb}. {$entry['date_evenement']} à {$entry['heure_evenement']} — {$entry['lieu_exact']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], $actorId);
    }
}
