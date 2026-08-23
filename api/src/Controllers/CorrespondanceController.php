<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Correspondance;
use App\Models\CorrespondanceAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class CorrespondanceController
{
    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('date_correspondance', $data)) {
            $value = $data['date_correspondance'] ?? null;
            if (empty($value)) {
                $errors['date_correspondance'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_correspondance'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('heure_enregistrement', $data)) {
            $value = $data['heure_enregistrement'] ?? null;
            if (empty($value)) {
                $errors['heure_enregistrement'] = "L'heure d'enregistrement est requise";
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_enregistrement'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        if ($isCreate || array_key_exists('sens', $data)) {
            $value = $data['sens'] ?? null;
            if (empty($value) || !in_array($value, Correspondance::SENS, true)) {
                $errors['sens'] = 'Le sens doit être Entrant ou Sortant';
            }
        }

        if ($isCreate || array_key_exists('reference', $data)) {
            $value = trim((string) ($data['reference'] ?? ''));
            if ($value === '') {
                $errors['reference'] = "Le numéro d'ordre / la référence est requis";
            } else {
                $sens = $data['sens'] ?? null;
                if (!$isCreate && !in_array($sens, Correspondance::SENS, true)) {
                    // On update without sens change, look up the current sens.
                    $current = $excludeId ? Correspondance::getById($excludeId) : null;
                    $sens = $current['sens'] ?? null;
                }
                if ($sens) {
                    $existing = Correspondance::getByReference($sens, $value);
                    if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                        $errors['reference'] = 'Cette référence existe déjà pour ce sens';
                    }
                }
            }
        }

        if ($isCreate || array_key_exists('emetteur_destinataire', $data)) {
            if (empty(trim((string) ($data['emetteur_destinataire'] ?? '')))) {
                $errors['emetteur_destinataire'] = "L'émetteur / le destinataire est requis";
            }
        }

        if ($isCreate || array_key_exists('objet', $data)) {
            if (empty(trim((string) ($data['objet'] ?? '')))) {
                $errors['objet'] = "L'objet est requis";
            }
        }

        if (array_key_exists('statut', $data)
            && (!is_string($data['statut']) || !in_array($data['statut'], Correspondance::STATUTS, true))) {
            $errors['statut'] = 'Le statut est invalide';
        }

        return $errors;
    }

    /**
     * GET /api/correspondances
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['sens', 'statut', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = Correspondance::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/correspondances/{id}
     */
    public function show(array $params): void
    {
        $row = Correspondance::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Correspondance not found');
        }
        $row['attachments'] = CorrespondanceAttachment::getByCorrespondanceId((int) $row['id']);

        // Auto-dismiss the "new correspondance" notification for the viewing
        // admin once they actually open the detail. Notifications are only sent
        // on creation, so viewing the detail means the admin has now seen it.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/secretariat/correspondance/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/correspondances
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

        $data['reference'] = trim($data['reference']);
        $data['created_by'] = $authUser['sub'] ?? null;

        $id = Correspondance::create($data);
        $correspondance = Correspondance::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'correspondances',
            'entity_id' => $id,
            'description' => "Création d'une correspondance {$correspondance['sens']} (Réf: {$correspondance['reference']}) — {$correspondance['objet']}",
            'new_values' => $correspondance,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with correspondance view) ---
        // Regardless of who the actor is, every other admin AND every other
        // user with view permission on the correspondance module is notified.
        self::notifyAdmins($correspondance, (int) $authUser['sub']);
        self::notifyUsers($correspondance, (int) $authUser['sub']);

        Response::created($correspondance, 'Correspondance enregistrée avec succès');
    }

    /**
     * PUT /api/correspondances/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $correspondance = Correspondance::getById($id);
        if (!$correspondance) {
            Response::notFound('Correspondance not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['reference'])) {
            $data['reference'] = trim($data['reference']);
        }

        // Uniqueness guard when the sens changes without a new reference.
        $effectiveSens = $data['sens'] ?? $correspondance['sens'];
        $effectiveRef = $data['reference'] ?? $correspondance['reference'];
        if ($effectiveSens !== $correspondance['sens'] || $effectiveRef !== $correspondance['reference']) {
            $existing = Correspondance::getByReference($effectiveSens, $effectiveRef);
            if ($existing && (int) $existing['id'] !== $id) {
                Response::error('Validation failed', 422, ['reference' => 'Cette référence existe déjà pour ce sens']);
            }
        }

        $oldCorrespondance = $correspondance;
        Correspondance::update($id, $data);
        $correspondance = Correspondance::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'correspondances',
            'entity_id' => $id,
            'description' => "Modification d'une correspondance {$correspondance['sens']} (Réf: {$correspondance['reference']}) — {$correspondance['objet']}",
            'old_values' => $oldCorrespondance,
            'new_values' => $correspondance,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with correspondance view) ---
        $actorId = $authUser['sub'] ?? null;
        $link = '/sedentaire/secretariat/correspondance/' . $correspondance['id'];

        Notification::notifyFeatureChange('sedentaire_secretariat_correspondance', [
            'title'   => 'Correspondance modifiée',
            'message' => "La correspondance {$correspondance['sens']} (Réf: {$correspondance['reference']}) — {$correspondance['objet']} a été modifiée.",
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], [
            'title'   => 'Correspondance modifiée',
            'message' => "La correspondance {$correspondance['sens']} (Réf: {$correspondance['reference']}) a été modifiée. Objet: {$correspondance['objet']}. Veuillez en prendre connaissance des modifications.",
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], $actorId);

        Response::success($correspondance, 'Correspondance modifiée avec succès');
    }

    /**
     * DELETE /api/correspondances/{id}
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
        $correspondance = Correspondance::getById($id);
        if (!$correspondance) {
            Response::notFound('Correspondance not found');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/correspondances';
        foreach (CorrespondanceAttachment::getByCorrespondanceId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Correspondance::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'correspondances',
            'entity_id' => $id,
            'description' => "Suppression d'une correspondance {$correspondance['sens']} (Réf: {$correspondance['reference']}) — {$correspondance['objet']}",
            'old_values' => $correspondance,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Correspondance deleted successfully');
    }

    /**
     * Notify every admin (except the actor) that a correspondance was
     * created. This is called only on creation — updates do not trigger a
     * new notification. Once an admin opens the correspondance detail
     * (show()), the notification is auto-dismissed.
     * Push delivery failures are isolated inside Notification::create()
     * and never affect the API response.
     */
    private static function notifyAdmins(array $correspondance, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($correspondance['date_correspondance']));
        $heure = substr((string) $correspondance['heure_enregistrement'], 0, 5);
        $agent = trim(($correspondance['agent_prenoms'] ?? '') . ' ' . ($correspondance['agent_nom'] ?? ''));
        if ($agent === '') {
            $agent = $correspondance['agent_username'] ?? 'Inconnu';
        }

        $title = 'Nouvelle correspondance';
        $message = "Une correspondance {$correspondance['sens']} a été enregistrée. "
            . "Réf: {$correspondance['reference']} — {$correspondance['sens']} — {$correspondance['objet']}"
            . " — Émetteur/Destinataire: {$correspondance['emetteur_destinataire']}"
            . " — le {$date} à {$heure} — Agent secrétariat: {$agent}.";

        Notification::notifyAdmins([
            'title'   => $title,
            'message' => $message,
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => '/sedentaire/secretariat/correspondance/' . $correspondance['id'],
        ], $actorId);
    }

    /**
     * Admin → regular users counterpart of notifyAdmins(): when an
     * administrator registers a correspondance, every staff member who can
     * view the correspondance module is informed. The wording mirrors the
     * user → admin message but is phrased for the admin → user direction
     * ("enregistrée par un administrateur ... Veuillez en prendre
     * connaissance").
     */
    private static function notifyUsers(array $correspondance, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($correspondance['date_correspondance']));
        $heure = substr((string) $correspondance['heure_enregistrement'], 0, 5);

        $title = 'Nouvelle correspondance';
        $message = "Une correspondance {$correspondance['sens']} a été enregistrée. "
            . "Réf: {$correspondance['reference']} — {$correspondance['objet']}"
            . " — Émetteur/Destinataire: {$correspondance['emetteur_destinataire']}"
            . " — le {$date} à {$heure}. Veuillez en prendre connaissance.";

        Notification::notifyModuleUsers('sedentaire_secretariat_correspondance', [
            'title'        => $title,
            'message'      => $message,
            'type'         => 'info',
            'service'      => 'Sedentaire',
            'personnel_id' => null,
            'link'         => '/sedentaire/secretariat/correspondance/' . $correspondance['id'],
        ], $actorId);
    }
}
