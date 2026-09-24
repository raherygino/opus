<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\DispositifExceptionnel;
use App\Models\AuditLog;
use App\Models\Notification;

class DispositifExceptionnelController
{
    /** Permission module code for the DISPOSITIF EXCEPTIONNEL feature. */
    private const MODULE = 'sg_dispositif_exceptionnel';

    /** Notification link prefix for DISPOSITIF EXCEPTIONNEL detail. */
    private const LINK_PREFIX = '/sg/dispositifs-exceptionnels/';

    /** All writable columns (used to keep existing values on partial update). */
    private const FIELDS = ['nature_evenement', 'date_debut', 'date_fin'];

    /** Editable columns of an "Effectif engagé" row. */
    private const EFFECTIF_FIELDS = [
        'secteur', 'chef_element_contact', 'controle_contact', 'materiels_armements', 'missions',
    ];

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── nature_evenement ────────────────────────────────────────
        if ($isCreate || array_key_exists('nature_evenement', $data)) {
            if (empty(trim((string) ($data['nature_evenement'] ?? '')))) {
                $errors['nature_evenement'] = "La nature de l'évènement est requise";
            }
        }

        // ── période (date_debut / date_fin) ─────────────────────────
        foreach (['date_debut' => 'de début', 'date_fin' => 'de fin'] as $f => $label) {
            if ($isCreate || array_key_exists($f, $data)) {
                $value = trim((string) ($data[$f] ?? ''));
                if ($value === '') {
                    $errors[$f] = "La date $label est requise";
                } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) && !strtotime($value)) {
                    $errors[$f] = "La date $label est invalide";
                }
            }
        }

        // Range coherence — only checked when both dates are present and valid.
        $debut = trim((string) ($data['date_debut'] ?? ''));
        $fin = trim((string) ($data['date_fin'] ?? ''));
        if (!isset($errors['date_debut']) && !isset($errors['date_fin'])
            && $debut !== '' && $fin !== ''
            && strtotime($debut) !== false && strtotime($fin) !== false
            && strtotime($fin) < strtotime($debut)) {
            $errors['date_fin'] = 'La date de fin doit être postérieure ou égale à la date de début';
        }

        // ── effectifs (Effectif engagé rows) ────────────────────────
        if (isset($data['effectifs']) && is_array($data['effectifs'])) {
            foreach ($data['effectifs'] as $i => $row) {
                if (!is_array($row)) {
                    $errors["effectifs.{$i}"] = 'La ligne est invalide';
                    continue;
                }
                if (empty(trim((string) ($row['secteur'] ?? '')))) {
                    $errors["effectifs.{$i}.secteur"] = 'Le secteur est requis';
                }
                foreach (self::EFFECTIF_FIELDS as $f) {
                    if (array_key_exists($f, $row) && !is_string($row[$f]) && !is_null($row[$f])) {
                        $errors["effectifs.{$i}.{$f}"] = 'La valeur doit être une chaîne de caractères';
                    }
                }
            }
        }

        return $errors;
    }

    /**
     * GET /api/dispositifs-exceptionnels
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
        $list = DispositifExceptionnel::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/dispositifs-exceptionnels/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = DispositifExceptionnel::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Dispositif exceptionnel introuvable');
        }
        $row['effectifs'] = DispositifExceptionnel::effectifs((int) $row['id']);

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
     * POST /api/dispositifs-exceptionnels
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

        $id = DispositifExceptionnel::create($data);

        // Persist the effectif engagé child table.
        $effectifs = $data['effectifs'] ?? [];
        if (is_array($effectifs)) {
            DispositifExceptionnel::replaceEffectifs($id, $effectifs);
        }

        $entry = DispositifExceptionnel::find($id);
        $entry['effectifs'] = DispositifExceptionnel::effectifs($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'dispositif_exceptionnel',
            'entity_id' => $id,
            'description' => "Création d'un dispositif exceptionnel — {$entry['nature_evenement']} ({$entry['date_debut']} → {$entry['date_fin']})",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Dispositif exceptionnel enregistré avec succès');
    }

    /**
     * PUT /api/dispositifs-exceptionnels/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = DispositifExceptionnel::find($id);
        if (!$entry) {
            Response::notFound('Dispositif exceptionnel introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Keep existing values for fields not provided (needed for the
        // date-range coherence check below).
        foreach (self::FIELDS as $f) {
            if (!array_key_exists($f, $data)) {
                $data[$f] = $entry[$f];
            }
        }

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        self::sanitize($data);

        $oldEntry = $entry;
        DispositifExceptionnel::update($id, $data);

        // Replace the effectif engagé child table if provided.
        if (array_key_exists('effectifs', $data) && is_array($data['effectifs'])) {
            DispositifExceptionnel::replaceEffectifs($id, $data['effectifs']);
        }

        $entry = DispositifExceptionnel::find($id);
        $entry['effectifs'] = DispositifExceptionnel::effectifs($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'dispositif_exceptionnel',
            'entity_id' => $id,
            'description' => "Modification d'un dispositif exceptionnel — {$entry['nature_evenement']} ({$entry['date_debut']} → {$entry['date_fin']})",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Dispositif exceptionnel modifié avec succès');
    }

    /**
     * DELETE /api/dispositifs-exceptionnels/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = DispositifExceptionnel::find($id);
        if (!$entry) {
            Response::notFound('Dispositif exceptionnel introuvable');
        }

        // Child rows are cascade-deleted by the FK constraint.
        DispositifExceptionnel::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'dispositif_exceptionnel',
            'entity_id' => $id,
            'description' => "Suppression d'un dispositif exceptionnel — {$entry['nature_evenement']} ({$entry['date_debut']} → {$entry['date_fin']})",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Dispositif exceptionnel supprimé avec succès');
    }

    /**
     * Trim text fields in place; empty strings become null. Effectif row
     * fields are trimmed the same way (secteur is kept verbatim).
     */
    private static function sanitize(array &$data): void
    {
        if (isset($data['nature_evenement']) && is_string($data['nature_evenement'])) {
            $data['nature_evenement'] = trim($data['nature_evenement']);
        }
        if (isset($data['effectifs']) && is_array($data['effectifs'])) {
            foreach ($data['effectifs'] as $i => $row) {
                if (!is_array($row)) {
                    continue;
                }
                foreach (self::EFFECTIF_FIELDS as $f) {
                    if (isset($row[$f]) && is_string($row[$f])) {
                        $row[$f] = trim($row[$f]);
                    }
                    if ($f !== 'secteur' && ($row[$f] ?? null) === '') {
                        $row[$f] = null;
                    }
                }
                $data['effectifs'][$i] = $row;
            }
        }
    }

    /**
     * Full body for the "new/updated dispositif" notification — carries the
     * main content so the record is visible from the desktop notifications
     * feed without opening the detail page.
     */
    private static function notificationBody(string $verb, array $entry): string
    {
        $lines = ["Un dispositif exceptionnel a été {$verb}."];
        $lines[] = "Nature : {$entry['nature_evenement']}";
        $lines[] = "Période : du {$entry['date_debut']} au {$entry['date_fin']}";
        $count = is_array($entry['effectifs'] ?? null) ? count($entry['effectifs']) : 0;
        if ($count > 0) {
            $lines[] = "Effectif engagé : $count secteur(s)";
        }
        return implode("\n", $lines);
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouveau dispositif exceptionnel' : 'Dispositif exceptionnel modifié';
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
