<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Passation;
use App\Models\PassationAttachment;
use App\Models\User;
use App\Models\AuditLog;
use App\Models\Notification;

class PassationController
{
    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('date_passation', $data)) {
            $value = $data['date_passation'] ?? null;
            if (empty($value)) {
                $errors['date_passation'] = 'La date de la passation est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_passation'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('heure_passation', $data)) {
            $value = $data['heure_passation'] ?? null;
            if (empty($value)) {
                $errors['heure_passation'] = "L'heure de la passation est requise";
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_passation'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        // The chef montant MUST be authenticated (verified) before the
        // passation can be saved — their user_id and snapshot identity are
        // required on create.
        if ($isCreate) {
            if (empty($data['chef_montant_user_id'])) {
                $errors['chef_montant'] = "Le chef de poste montant doit être authentifié";
            }
            if (empty(trim((string) ($data['chef_montant_grade'] ?? '')))) {
                $errors['chef_montant'] = "L'identité du chef de poste montant est requise";
            }
            if (empty(trim((string) ($data['chef_montant_lastname'] ?? '')))) {
                $errors['chef_montant'] = "L'identité du chef de poste montant est requise";
            }
            if (empty($data['chef_descendant_user_id'])) {
                $errors['chef_descendant'] = "Le chef de poste descendant est requis";
            }
            if (empty(trim((string) ($data['chef_descendant_grade'] ?? '')))) {
                $errors['chef_descendant'] = "L'identité du chef de poste descendant est requise";
            }
            if (empty(trim((string) ($data['chef_descendant_lastname'] ?? '')))) {
                $errors['chef_descendant'] = "L'identité du chef de poste descendant est requise";
            }
        }

        return $errors;
    }

    /**
     * GET /api/passations
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = Passation::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/passations/{id}
     */
    public function show(array $params): void
    {
        $row = Passation::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Passation not found');
        }
        $row['attachments'] = PassationAttachment::getByPassationId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user once they
        // actually open the detail.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/poste/passation/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/passations
     *
     * The chef descendant is determined from the authenticated user (the
     * caller). The chef montant identity (user_id + grade + lastname
     * snapshots) is provided by the client after a successful /auth/verify
     * call — the password itself is NEVER sent to this endpoint.
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Snapshot the chef descendant identity from the authenticated user.
        $descendant = User::getById((int) $authUser['sub']);
        if (!$descendant) {
            Response::unauthorized('Authentication required');
        }
        $data['chef_descendant_user_id'] = (int) $authUser['sub'];
        $data['chef_descendant_grade'] = $descendant['grade'] ?? null;
        // Store the full name (firstname + lastname) in the lastname column
        // so both chefs are displayed consistently with a single name field.
        $data['chef_descendant_lastname'] = trim(
            (($descendant['firstname'] ?? '') . ' ' . ($descendant['lastname'] ?? ''))
        ) ?: null;
        $data['created_by'] = (int) $authUser['sub'];

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $id = Passation::create($data);
        $passation = Passation::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'passations',
            'entity_id' => $id,
            'description' => "Création d'une passation le {$passation['date_passation']} — Descendant: {$passation['chef_descendant_grade']} {$passation['chef_descendant_lastname']} — Montant: {$passation['chef_montant_grade']} {$passation['chef_montant_lastname']}",
            'new_values' => $passation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with passation view) ---
        self::notifyChange('create', $passation, $authUser['sub'] ?? null);

        Response::created($passation, 'Passation enregistrée avec succès');
    }

    /**
     * PUT /api/passations/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $passation = Passation::getById($id);
        if (!$passation) {
            Response::notFound('Passation not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // The chef identities are NOT editable after creation — ignore any
        // chef_* fields sent on update to protect the snapshot integrity.
        unset(
            $data['chef_descendant_user_id'],
            $data['chef_descendant_grade'],
            $data['chef_descendant_lastname'],
            $data['chef_montant_user_id'],
            $data['chef_montant_grade'],
            $data['chef_montant_lastname']
        );

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldPassation = $passation;
        Passation::update($id, $data);
        $passation = Passation::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'passations',
            'entity_id' => $id,
            'description' => "Modification d'une passation le {$passation['date_passation']} — Descendant: {$passation['chef_descendant_grade']} {$passation['chef_descendant_lastname']}",
            'old_values' => $oldPassation,
            'new_values' => $passation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with passation view) ---
        self::notifyChange('update', $passation, $authUser['sub'] ?? null);

        Response::success($passation, 'Passation modifiée avec succès');
    }

    /**
     * DELETE /api/passations/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $passation = Passation::getById($id);
        if (!$passation) {
            Response::notFound('Passation not found');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/passations';
        foreach (PassationAttachment::getByPassationId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Passation::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'passations',
            'entity_id' => $id,
            'description' => "Suppression d'une passation le {$passation['date_passation']} — Descendant: {$passation['chef_descendant_grade']} {$passation['chef_descendant_lastname']}",
            'old_values' => $passation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Passation deleted successfully');
    }

    /**
     * Notify every admin AND every user with view permission on the
     * passation module that a passation was created or modified.
     * The actor is excluded and recipients are deduplicated inside
     * Notification::notifyFeatureChange(); push delivery failures are
     * isolated inside Notification::create() and never affect the API
     * response.
     */
    private static function notifyChange(string $action, array $passation, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($passation['date_passation']));
        $heure = substr((string) $passation['heure_passation'], 0, 5);
        $link = '/sedentaire/poste/passation/' . $passation['id'];

        $descendant = trim(($passation['chef_descendant_grade'] ?? '') . ' ' . ($passation['chef_descendant_lastname'] ?? ''));
        $montant = trim(($passation['chef_montant_grade'] ?? '') . ' ' . ($passation['chef_montant_lastname'] ?? ''));

        if ($action === 'create') {
            $title = 'Nouvelle passation';
            $adminMessage = "Une passation a été enregistrée. "
                . "Descendant: {$descendant} — Montant: {$montant}"
                . " — le {$date} à {$heure}.";
            $userMessage = "Une passation a été enregistrée. "
                . "Descendant: {$descendant} — Montant: {$montant}"
                . " — le {$date} à {$heure}. Veuillez en prendre connaissance.";
        } else {
            $title = 'Passation modifiée';
            $adminMessage = "La passation du {$date} (Descendant: {$descendant} — Montant: {$montant}) a été modifiée.";
            $userMessage = "La passation du {$date} (Descendant: {$descendant} — Montant: {$montant}) a été modifiée. Veuillez en prendre connaissance des modifications.";
        }

        Notification::notifyFeatureChange('sedentaire_poste_passation', [
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
