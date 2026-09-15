<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\GardeAVue;
use App\Models\GardeAVueAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class GardeAVueController
{
    /** Permission module code for the GAV feature. */
    private const MODULE = 'pj_gav';

    /** Notification link prefix for GAV detail. */
    private const LINK_PREFIX = '/pj/gav/';

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── nom ──────────────────────────────────────────────────────
        if ($isCreate || array_key_exists('nom', $data)) {
            if (empty(trim((string) ($data['nom'] ?? '')))) {
                $errors['nom'] = 'Le nom est requis';
            }
        }

        // ── date_naissance ────────────────────────────────────────────
        if (array_key_exists('date_naissance', $data) && $data['date_naissance'] !== null && $data['date_naissance'] !== '') {
            if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $data['date_naissance']) || !strtotime($data['date_naissance'])) {
                $errors['date_naissance'] = 'La date de naissance est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        // ── datetime fields ───────────────────────────────────────────
        $datetimeFields = ['debut_gav', 'fin_gav', 'prolongation_gav'];
        $datetimeValues = [];
        foreach ($datetimeFields as $field) {
            if (array_key_exists($field, $data) && $data[$field] !== null && $data[$field] !== '') {
                $value = $data[$field];
                // Accept "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DDTHH:MM" (datetime-local).
                $normalized = self::normalizeDatetime($value);
                if ($normalized === null) {
                    $errors[$field] = 'La date/heure est invalide (format attendu : AAAA-MM-JJ HH:MM)';
                } else {
                    $datetimeValues[$field] = $normalized;
                }
            }
        }

        // ── logical consistency between GAV datetimes ────────────────
        $debut = $datetimeValues['debut_gav'] ?? ($data['debut_gav'] ?? null);
        $fin = $datetimeValues['fin_gav'] ?? ($data['fin_gav'] ?? null);
        $prolongation = $datetimeValues['prolongation_gav'] ?? ($data['prolongation_gav'] ?? null);

        if ($debut && $fin && strtotime($fin) < strtotime($debut)) {
            $errors['fin_gav'] = "La fin de la garde à vue ne peut pas être antérieure au début";
        }
        if ($fin && $prolongation && strtotime($prolongation) < strtotime($fin)) {
            $errors['prolongation_gav'] = "La prolongation ne peut pas être antérieure à la fin de la garde à vue";
        }

        return $errors;
    }

    /**
     * Normalize a datetime input to "YYYY-MM-DD HH:MM:SS".
     * Accepts "YYYY-MM-DD HH:MM:SS", "YYYY-MM-DD HH:MM", and the
     * datetime-local format "YYYY-MM-DDTHH:MM".
     */
    private static function normalizeDatetime(string $value): ?string
    {
        $value = trim($value);
        // datetime-local: "2026-09-15T14:30"
        if (preg_match('/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/', $value)) {
            $value = str_replace('T', ' ', $value) . ':00';
        } elseif (preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/', $value)) {
            $value .= ':00';
        }
        if (!preg_match('/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/', $value)) {
            return null;
        }
        return strtotime($value) ? $value : null;
    }

    /**
     * Normalize datetime fields in the data array (mutates $data).
     */
    private static function normalizeDatetimeFields(array $data): array
    {
        foreach (['debut_gav', 'fin_gav', 'prolongation_gav'] as $field) {
            if (isset($data[$field]) && is_string($data[$field]) && $data[$field] !== '') {
                $normalized = self::normalizeDatetime($data[$field]);
                if ($normalized !== null) {
                    $data[$field] = $normalized;
                }
            }
        }
        return $data;
    }

    /**
     * GET /api/garde-a-vue
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = GardeAVue::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/garde-a-vue/{id}
     */
    public function show(array $params): void
    {
        $row = GardeAVue::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Garde à vue introuvable');
        }
        $row['attachments'] = GardeAVueAttachment::getByGardeAVueId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                self::LINK_PREFIX . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/garde-a-vue
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

        $data = self::normalizeDatetimeFields($data);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['nom', 'prenoms', 'adresse', 'enqueteur_permance', 'opj_gav', 'motif', 'etat_sante', 'droits_notifies', 'personne_contacter'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = GardeAVue::create($data);
        $gav = GardeAVue::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'garde_a_vue',
            'entity_id' => $id,
            'description' => "Création d'une garde à vue — {$gav['nom']}",
            'new_values' => $gav,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('create', $gav, (int) $authUser['sub']);

        Response::created($gav, 'Garde à vue enregistrée avec succès');
    }

    /**
     * PUT /api/garde-a-vue/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $gav = GardeAVue::getById($id);
        if (!$gav) {
            Response::notFound('Garde à vue introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $data = self::normalizeDatetimeFields($data);

        // Trim text fields.
        foreach (['nom', 'prenoms', 'adresse', 'enqueteur_permance', 'opj_gav', 'motif', 'etat_sante', 'droits_notifies', 'personne_contacter'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldGav = $gav;
        GardeAVue::update($id, $data);
        $gav = GardeAVue::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'garde_a_vue',
            'entity_id' => $id,
            'description' => "Modification d'une garde à vue — {$gav['nom']}",
            'old_values' => $oldGav,
            'new_values' => $gav,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('update', $gav, $authUser['sub'] ?? null);

        Response::success($gav, 'Garde à vue modifiée avec succès');
    }

    /**
     * DELETE /api/garde-a-vue/{id}
     * Attachments are removed by the ON DELETE CASCADE FK; files on disk
     * are cleaned up here.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $gav = GardeAVue::getById($id);
        if (!$gav) {
            Response::notFound('Garde à vue introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/garde_a_vue';
        foreach (GardeAVueAttachment::getByGardeAVueId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        GardeAVue::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'garde_a_vue',
            'entity_id' => $id,
            'description' => "Suppression d'une garde à vue — {$gav['nom']}",
            'old_values' => $gav,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Garde à vue supprimée avec succès');
    }

    /**
     * Notify admins + feature users of a create/update.
     */
    private static function notifyChange(string $action, array $gav, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $gav['id'];

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle garde à vue' : 'Garde à vue modifiée';

        $adminMessage = "Une garde à vue a été {$verb}. Personne: {$gav['nom']}.";
        $userMessage = "Une garde à vue a été {$verb}. Personne: {$gav['nom']}. Veuillez en prendre connaissance.";

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => $adminMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $userMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
