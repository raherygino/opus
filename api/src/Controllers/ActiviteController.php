<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Activite;
use App\Models\ActiviteAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class ActiviteController
{
    /** Permission module code for the ACTIVITÉ feature. */
    private const MODULE = 'sg_activite';

    /** Notification link prefix for ACTIVITÉ detail. */
    private const LINK_PREFIX = '/sg/activites/';

    /** All writable columns (used to keep existing values on partial update). */
    private const FIELDS = [
        'date_activite', 'heure_activite',
        'patrouille_diurne_motorisee_itineraire', 'patrouille_diurne_pedestre_itineraire', 'patrouille_diurne_portee_itineraire',
        'patrouille_nocturne_motorisee_itineraire', 'patrouille_nocturne_pedestre_itineraire', 'patrouille_nocturne_portee_itineraire',
        'operation_ciblee', 'faits_constates', 'compte_rendu_hierarchie', 'conduite_a_tenir',
        'nature_intervention', 'suites_donnees', 'latitude', 'longitude',
    ];

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── date_activite ───────────────────────────────────────────
        if ($isCreate || array_key_exists('date_activite', $data)) {
            $value = trim((string) ($data['date_activite'] ?? ''));
            if ($value === '') {
                $errors['date_activite'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) && !strtotime($value)) {
                $errors['date_activite'] = 'La date est invalide';
            }
        }

        // ── heure_activite ──────────────────────────────────────────
        if ($isCreate || array_key_exists('heure_activite', $data)) {
            $value = trim((string) ($data['heure_activite'] ?? ''));
            if ($value === '') {
                $errors['heure_activite'] = "L'heure est requise";
            }
        }

        // ── patrol itineraries (string or null; NULL = mode not selected) ──
        foreach (Activite::ITINERAIRE_FIELDS as $f) {
            if (array_key_exists($f, $data) && !is_string($data[$f]) && !is_null($data[$f])) {
                $errors[$f] = "L'itinéraire doit être une chaîne de caractères";
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
     * GET /api/activites
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
        $list = Activite::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/activites/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = Activite::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Activité introuvable');
        }
        $row['attachments'] = ActiviteAttachment::getByActiviteId((int) $row['id']);

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
     * POST /api/activites
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

        $id = Activite::create($data);
        $entry = Activite::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'activite',
            'entity_id' => $id,
            'description' => "Création d'une activité — {$entry['date_activite']} {$entry['heure_activite']}",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Activité enregistrée avec succès');
    }

    /**
     * PUT /api/activites/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = Activite::find($id);
        if (!$entry) {
            Response::notFound('Activité introuvable');
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
        Activite::update($id, $data);
        $entry = Activite::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'activite',
            'entity_id' => $id,
            'description' => "Modification d'une activité — {$entry['date_activite']} {$entry['heure_activite']}",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Activité modifiée avec succès');
    }

    /**
     * DELETE /api/activites/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = Activite::find($id);
        if (!$entry) {
            Response::notFound('Activité introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/activites';
        foreach (ActiviteAttachment::getByActiviteId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Activite::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'activite',
            'entity_id' => $id,
            'description' => "Suppression d'une activité — {$entry['date_activite']} {$entry['heure_activite']}",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Activité supprimée avec succès');
    }

    /**
     * Trim text fields in place; empty strings become null for the plain
     * text fields. Patrol itineraries are trimmed but '' is kept (a NULL
     * itinerary means the mode was not selected).
     */
    private static function sanitize(array &$data): void
    {
        foreach (Activite::TEXT_FIELDS as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
            if (($data[$f] ?? null) === '') {
                $data[$f] = null;
            }
        }
        foreach (Activite::ITINERAIRE_FIELDS as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
    }

    /**
     * Short patrol summary, e.g. "Diurne motorisée (PK3 → Marché) •
     * Nocturne portée (Quartier administratif)". Empty when no patrol
     * mode was selected (itinerary column NULL).
     */
    private static function patrouilleSummary(array $entry): string
    {
        $types = ['diurne' => 'Diurne', 'nocturne' => 'Nocturne'];
        $modes = ['motorisee' => 'motorisée', 'pedestre' => 'pédestre', 'portee' => 'portée'];
        $parts = [];
        foreach ($types as $type => $typeLabel) {
            foreach ($modes as $mode => $modeLabel) {
                $itineraire = $entry["patrouille_{$type}_{$mode}_itineraire"] ?? null;
                if ($itineraire !== null) {
                    $parts[] = "$typeLabel $modeLabel" . ($itineraire !== '' ? " ($itineraire)" : '');
                }
            }
        }
        return implode(' • ', $parts);
    }

    /**
     * Full body for the "new/updated activité" notification — carries the
     * main activity content so the record is visible from the desktop
     * notifications feed without opening the detail page.
     */
    private static function notificationBody(string $verb, array $entry): string
    {
        $lines = ["Une activité a été {$verb}.", "Date : {$entry['date_activite']} à {$entry['heure_activite']}"];

        $patrouilles = self::patrouilleSummary($entry);
        if ($patrouilles !== '') {
            $lines[] = "Patrouille : $patrouilles";
        }
        if (!empty($entry['operation_ciblee'])) {
            $lines[] = "Opération ciblée : {$entry['operation_ciblee']}";
        }
        if (!empty($entry['nature_intervention'])) {
            $lines[] = "Intervention : {$entry['nature_intervention']}";
        }
        if (!empty($entry['suites_donnees'])) {
            $lines[] = "Suites données : {$entry['suites_donnees']}";
        }
        return implode("\n", $lines);
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle activité' : 'Activité modifiée';
        $body = self::notificationBody($verb, $entry);

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => $body,
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $body . "\nVeuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], $actorId);
    }
}
