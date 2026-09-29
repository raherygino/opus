<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\SituationGav;
use App\Models\SituationGavAttachment;
use App\Models\GardeAVue;
use App\Models\Personnel;
use App\Models\AuditLog;
use App\Models\Notification;

class SituationGavController
{
    /** Permission module code for the Situation GAV feature. */
    private const MODULE = 'sedentaire_poste_situation_gav';

    /** Notification link prefix for Situation GAV detail. */
    private const LINK_PREFIX = '/sedentaire/poste/situation-gav/';

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── garde_a_vue_id (personne concernée) ───────────────────────
        if ($isCreate || array_key_exists('garde_a_vue_id', $data)) {
            $gavId = $data['garde_a_vue_id'] ?? null;
            if (empty($gavId) || !is_numeric($gavId) || (int) $gavId <= 0) {
                $errors['garde_a_vue_id'] = 'La personne concernée est requise';
            } elseif (!GardeAVue::getById((int) $gavId)) {
                $errors['garde_a_vue_id'] = "La garde à vue sélectionnée n'existe pas";
            }
        }

        // ── date_controle ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('date_controle', $data)) {
            $value = $data['date_controle'] ?? null;
            if ($value === null || $value === '') {
                $errors['date_controle'] = 'La date et l\'heure du contrôle sont requises';
            } elseif (self::normalizeDatetime((string) $value) === null) {
                $errors['date_controle'] = 'La date/heure du contrôle est invalide (format attendu : AAAA-MM-JJ HH:MM)';
            }
        }

        // ── agent_controle_id (agent ayant effectué le contrôle) ──────
        if (array_key_exists('agent_controle_id', $data) && $data['agent_controle_id'] !== null && $data['agent_controle_id'] !== '') {
            $agentId = $data['agent_controle_id'];
            if (!is_numeric($agentId) || (int) $agentId <= 0) {
                $errors['agent_controle_id'] = "L'agent sélectionné est invalide";
            } elseif (!Personnel::getById((int) $agentId)) {
                $errors['agent_controle_id'] = "L'agent sélectionné n'existe pas";
            }
        }

        // ── text fields ───────────────────────────────────────────────
        foreach (['etat_general', 'observations', 'mesures_prises'] as $field) {
            if (array_key_exists($field, $data) && $data[$field] !== null && !is_string($data[$field])) {
                $errors[$field] = 'Le champ est invalide';
            }
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
     * Normalize the date_controle field in the data array.
     */
    private static function normalizeDatetimeFields(array $data): array
    {
        if (isset($data['date_controle']) && is_string($data['date_controle']) && $data['date_controle'] !== '') {
            $normalized = self::normalizeDatetime($data['date_controle']);
            if ($normalized !== null) {
                $data['date_controle'] = $normalized;
            }
        }
        if (isset($data['agent_controle_id']) && ($data['agent_controle_id'] === '' || $data['agent_controle_id'] === null)) {
            $data['agent_controle_id'] = null;
        }
        return $data;
    }

    /**
     * GET /api/situations-gav
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'search', 'garde_a_vue_id'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = SituationGav::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/situations-gav/{id}
     */
    public function show(array $params): void
    {
        $row = SituationGav::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Situation GAV introuvable');
        }
        $row['attachments'] = SituationGavAttachment::getBySituationGavId((int) $row['id']);

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
     * POST /api/situations-gav
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
        $data['garde_a_vue_id'] = (int) $data['garde_a_vue_id'];
        if ($data['agent_controle_id'] !== null) {
            $data['agent_controle_id'] = (int) $data['agent_controle_id'];
        }
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['etat_general', 'observations', 'mesures_prises'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = SituationGav::create($data);
        $situation = SituationGav::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'situation_gav',
            'entity_id' => $id,
            'description' => "Création d'une situation GAV — {$situation['personne_nom']}",
            'new_values' => $situation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('create', $situation, (int) $authUser['sub']);

        Response::created($situation, 'Situation GAV enregistrée avec succès');
    }

    /**
     * PUT /api/situations-gav/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $situation = SituationGav::getById($id);
        if (!$situation) {
            Response::notFound('Situation GAV introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $data = self::normalizeDatetimeFields($data);
        if (isset($data['garde_a_vue_id'])) {
            $data['garde_a_vue_id'] = (int) $data['garde_a_vue_id'];
        }
        if (array_key_exists('agent_controle_id', $data) && $data['agent_controle_id'] !== null) {
            $data['agent_controle_id'] = (int) $data['agent_controle_id'];
        }

        // Trim text fields.
        foreach (['etat_general', 'observations', 'mesures_prises'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldSituation = $situation;
        SituationGav::update($id, $data);
        $situation = SituationGav::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'situation_gav',
            'entity_id' => $id,
            'description' => "Modification d'une situation GAV — {$situation['personne_nom']}",
            'old_values' => $oldSituation,
            'new_values' => $situation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('update', $situation, $authUser['sub'] ?? null);

        Response::success($situation, 'Situation GAV modifiée avec succès');
    }

    /**
     * DELETE /api/situations-gav/{id}
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
        $situation = SituationGav::getById($id);
        if (!$situation) {
            Response::notFound('Situation GAV introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/situation_gav';
        foreach (SituationGavAttachment::getBySituationGavId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        SituationGav::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'situation_gav',
            'entity_id' => $id,
            'description' => "Suppression d'une situation GAV — {$situation['personne_nom']}",
            'old_values' => $situation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Situation GAV supprimée avec succès');
    }

    /**
     * Notify admins + feature users of a create/update.
     */
    private static function notifyChange(string $action, array $situation, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $situation['id'];

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle situation GAV' : 'Situation GAV modifiée';

        $adminMessage = "Une situation GAV a été {$verb}. Personne: {$situation['personne_nom']}.";
        $userMessage = "Une situation GAV a été {$verb}. Personne: {$situation['personne_nom']}. Veuillez en prendre connaissance.";

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => $adminMessage,
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $userMessage,
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], $actorId);
    }
}
