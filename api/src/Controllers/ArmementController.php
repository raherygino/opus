<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Armement;
use App\Models\ArmementAttachment;
use App\Models\Personnel;
use App\Models\AuditLog;
use App\Models\Notification;

class ArmementController
{
    /**
     * Validation shared by store() and update() — perception fields only.
     * Returns an array of field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('date_perception', $data)) {
            $value = $data['date_perception'] ?? null;
            if (empty($value)) {
                $errors['date_perception'] = 'La date de la perception est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_perception'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('heure_perception', $data)) {
            $value = $data['heure_perception'] ?? null;
            if (empty($value)) {
                $errors['heure_perception'] = "L'heure de la perception est requise";
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_perception'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        if ($isCreate || array_key_exists('type_arme', $data)) {
            if (empty(trim((string) ($data['type_arme'] ?? '')))) {
                $errors['type_arme'] = "Le type de l'arme est requis";
            }
        }

        if ($isCreate || array_key_exists('matricule_arme', $data)) {
            if (empty(trim((string) ($data['matricule_arme'] ?? '')))) {
                $errors['matricule_arme'] = "Le matricule de l'arme est requis";
            }
        }

        if (array_key_exists('munitions', $data) && $data['munitions'] !== null && $data['munitions'] !== '') {
            if (!is_numeric($data['munitions']) || (int) $data['munitions'] < 0) {
                $errors['munitions'] = 'Les munitions doivent être un nombre entier positif';
            }
        }

        // The agent preneur MUST be selected on create — their personnel_id
        // and snapshot identity (IM + grade + nom) are required.
        if ($isCreate) {
            if (empty($data['agent_preneur_personnel_id'])) {
                $errors['agent_preneur'] = "L'agent preneur est requis";
            }
            if (empty(trim((string) ($data['agent_preneur_grade'] ?? '')))) {
                $errors['agent_preneur'] = "L'identité de l'agent preneur est requise";
            }
            if (empty(trim((string) ($data['agent_preneur_nom'] ?? '')))) {
                $errors['agent_preneur'] = "L'identité de l'agent preneur est requise";
            }
        }

        return $errors;
    }

    /**
     * Validation for the reintegration transition — only the three
     * reintegration fields are accepted here.
     */
    private static function validateReintegration(array $data, array $armement): array
    {
        $errors = [];

        $value = $data['heure_reintegration'] ?? null;
        if (empty($value)) {
            $errors['heure_reintegration'] = "L'heure de la réintégration est requise";
        } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
            $errors['heure_reintegration'] = "L'heure est invalide (format attendu : HH:MM)";
        }

        if (empty(trim((string) ($data['etat_reintegration'] ?? '')))) {
            $errors['etat_reintegration'] = "L'état à la réintégration est requis";
        }

        $consommees = $data['munitions_consommees'] ?? null;
        if ($consommees === null || $consommees === '') {
            $errors['munitions_consommees'] = 'Les munitions consommées sont requises';
        } elseif (!is_numeric($consommees) || (int) $consommees < 0) {
            $errors['munitions_consommees'] = 'Les munitions consommées doivent être un nombre entier positif';
        } elseif ($armement['munitions'] !== null && (int) $consommees > (int) $armement['munitions']) {
            $errors['munitions_consommees'] = 'Les munitions consommées ne peuvent pas dépasser les munitions perçues';
        }

        return $errors;
    }

    /**
     * GET /api/armements
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'statut', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = Armement::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/armements/{id}
     */
    public function show(array $params): void
    {
        $row = Armement::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Armement not found');
        }
        $row['attachments'] = ArmementAttachment::getByArmementId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user once they
        // actually open the detail.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/poste/armement/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/armements
     *
     * The agent preneur identity (IM + grade + nom) is snapshotted
     * server-side from the personnel table — the client only sends
     * agent_preneur_personnel_id. The agent's code secret is verified
     * server-side before the perception is created; a failed verification
     * rejects the creation. The signature SVG is stored alongside the
     * verification status so the armement record permanently retains the
     * proof of identity and the agent's signature for this handover.
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $data['created_by'] = (int) $authUser['sub'];

        self::snapshotAgentPreneur($data);

        // Verify the agent preneur's code secret before accepting the
        // perception. The code must be provided and must match the
        // personnel's stored code_secret_hash.
        $codeSecret = (string) ($data['code_secret'] ?? '');
        if ($codeSecret === '') {
            Response::error('Validation failed', 422, [
                'code_secret' => "Le code secret de l'agent est requis",
            ]);
        }

        $personnelId = (int) ($data['agent_preneur_personnel_id'] ?? 0);
        if ($personnelId <= 0 || !Personnel::getById($personnelId)) {
            Response::error('Validation failed', 422, [
                'agent_preneur' => "L'agent preneur est requis",
            ]);
        }

        if (!Personnel::verifyCodeSecret($personnelId, $codeSecret)) {
            AuditLog::create([
                'user_id' => $authUser['sub'] ?? null,
                'action' => 'verify_code_secret',
                'module' => 'armements',
                'entity_id' => $personnelId,
                'description' => "Échec de vérification du code secret lors de la perception d'arme — Personnel ID: {$personnelId}",
                'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
                'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
            ]);
            Response::error('Code secret incorrect — la perception ne peut pas être enregistrée', 422, [
                'code_secret' => 'Le code secret de l\'agent est incorrect',
            ]);
        }

        // Verification succeeded — mark the armement as verified.
        $data['agent_verifie'] = 1;
        $data['agent_verifie_at'] = date('Y-m-d H:i:s');

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // The signature SVG is optional but, when provided, is stored
        // permanently on the armement record.
        // $data['signature_svg'] is passed through to Armement::create().

        $id = Armement::create($data);
        $armement = Armement::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'armements',
            'entity_id' => $id,
            'description' => "Perception d'arme ({$armement['type_arme']} {$armement['matricule_arme']}) le {$armement['date_perception']} — Agent preneur: {$armement['agent_preneur_grade']} {$armement['agent_preneur_nom']} (identité vérifiée)",
            'new_values' => $armement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with armement view) ---
        self::notifyChange('create', $armement, $authUser['sub'] ?? null);

        Response::created($armement, 'Perception enregistrée avec succès');
    }

    /**
     * PUT /api/armements/{id}
     *
     * Updates the perception fields only. The reintegration columns are NOT
     * editable here — they are set once via POST /api/armements/{id}/reintegration.
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $armement = Armement::getById($id);
        if (!$armement) {
            Response::notFound('Armement not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // The reintegration fields are NOT editable after the fact — ignore
        // any reintegration fields sent on update to protect the one-way
        // transition. The agent verification + signature fields are also
        // one-way (set at perception time) and cannot be modified here.
        unset(
            $data['heure_reintegration'],
            $data['etat_reintegration'],
            $data['munitions_consommees'],
            $data['agent_verifie'],
            $data['agent_verifie_at'],
            $data['signature_svg'],
            $data['code_secret']
        );

        // Re-snapshot the agent preneur identity when the personnel changes.
        self::snapshotAgentPreneur($data);

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldArmement = $armement;
        Armement::update($id, $data);
        $armement = Armement::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'armements',
            'entity_id' => $id,
            'description' => "Modification d'une perception d'arme ({$armement['type_arme']} {$armement['matricule_arme']}) le {$armement['date_perception']} — Agent preneur: {$armement['agent_preneur_grade']} {$armement['agent_preneur_nom']}",
            'old_values' => $oldArmement,
            'new_values' => $armement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with armement view) ---
        self::notifyChange('update', $armement, $authUser['sub'] ?? null);

        Response::success($armement, 'Perception modifiée avec succès');
    }

    /**
     * POST /api/armements/{id}/reintegration
     *
     * One-way transition: fills heure_reintegration, etat_reintegration and
     * munitions_consommees. Rejected with 409 when the weapon has already
     * been reintegrated. All perception data is preserved.
     */
    public function reintegrate(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $armement = Armement::getById($id);
        if (!$armement) {
            Response::notFound('Armement not found');
        }

        if ($armement['heure_reintegration'] !== null) {
            Response::error('Cette arme a déjà été réintégrée', 409);
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validateReintegration($data, $armement);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldArmement = $armement;
        Armement::reintegrate($id, [
            'heure_reintegration' => $data['heure_reintegration'],
            'etat_reintegration' => trim((string) $data['etat_reintegration']),
            'munitions_consommees' => (int) $data['munitions_consommees'],
        ]);
        $armement = Armement::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'reintegration',
            'module' => 'armements',
            'entity_id' => $id,
            'description' => "Réintégration d'arme ({$armement['type_arme']} {$armement['matricule_arme']}) le {$armement['date_perception']} à " . substr((string) $armement['heure_reintegration'], 0, 5) . " — Agent preneur: {$armement['agent_preneur_grade']} {$armement['agent_preneur_nom']}",
            'old_values' => $oldArmement,
            'new_values' => $armement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with armement view) ---
        self::notifyChange('reintegration', $armement, $authUser['sub'] ?? null);

        Response::success($armement, 'Arme réintégrée avec succès');
    }

    /**
     * DELETE /api/armements/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $armement = Armement::getById($id);
        if (!$armement) {
            Response::notFound('Armement not found');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/armements';
        foreach (ArmementAttachment::getByArmementId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Armement::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'armements',
            'entity_id' => $id,
            'description' => "Suppression d'une perception d'arme ({$armement['type_arme']} {$armement['matricule_arme']}) le {$armement['date_perception']} — Agent preneur: {$armement['agent_preneur_grade']} {$armement['agent_preneur_nom']}",
            'old_values' => $armement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Armement deleted successfully');
    }

    /**
     * Snapshot the agent preneur identity (IM + grade + full name) from the
     * personnel table when agent_preneur_personnel_id is provided.
     */
    private static function snapshotAgentPreneur(array &$data): void
    {
        if (empty($data['agent_preneur_personnel_id'])) {
            return;
        }
        $agent = Personnel::getById((int) $data['agent_preneur_personnel_id']);
        if (!$agent) {
            $data['agent_preneur_personnel_id'] = null;
            return;
        }
        $data['agent_preneur_im'] = $agent['im'] ?? null;
        $data['agent_preneur_grade'] = $agent['grade'] ?? null;
        $data['agent_preneur_nom'] = trim(
            (($agent['firstname'] ?? '') . ' ' . ($agent['lastname'] ?? ''))
        ) ?: null;
    }

    /**
     * Notify every admin AND every user with view permission on the
     * armement module that an armement was created, modified or
     * reintegrated. The actor is excluded and recipients are deduplicated
     * inside Notification::notifyFeatureChange(); push delivery failures
     * are isolated inside Notification::create() and never affect the API
     * response.
     */
    private static function notifyChange(string $action, array $armement, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($armement['date_perception']));
        $heure = substr((string) $armement['heure_perception'], 0, 5);
        $link = '/sedentaire/poste/armement/' . $armement['id'];

        $agent = trim(($armement['agent_preneur_grade'] ?? '') . ' ' . ($armement['agent_preneur_nom'] ?? ''));
        $arme = trim(($armement['type_arme'] ?? '') . ' ' . ($armement['matricule_arme'] ?? ''));

        if ($action === 'create') {
            $title = "Nouvelle perception d'arme";
            $adminMessage = "Une perception d'arme a été enregistrée ({$arme}). "
                . "Agent preneur: {$agent} — le {$date} à {$heure}.";
            $userMessage = "Une perception d'arme a été enregistrée ({$arme}). "
                . "Agent preneur: {$agent} — le {$date} à {$heure}. Veuillez en prendre connaissance.";
        } elseif ($action === 'reintegration') {
            $heureReint = substr((string) $armement['heure_reintegration'], 0, 5);
            $title = "Arme réintégrée";
            $adminMessage = "L'arme {$arme} perçue par {$agent} le {$date} a été réintégrée à {$heureReint}.";
            $userMessage = "L'arme {$arme} perçue par {$agent} le {$date} a été réintégrée à {$heureReint}. Veuillez en prendre connaissance.";
        } else {
            $title = "Perception d'arme modifiée";
            $adminMessage = "La perception d'arme du {$date} ({$arme} — Agent preneur: {$agent}) a été modifiée.";
            $userMessage = "La perception d'arme du {$date} ({$arme} — Agent preneur: {$agent}) a été modifiée. Veuillez en prendre connaissance des modifications.";
        }

        Notification::notifyFeatureChange('sedentaire_poste_armement', [
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
