<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\DeclarationPerte;
use App\Models\DeclarationPerteAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class DeclarationPerteController
{
    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('date_declaration', $data)) {
            $value = $data['date_declaration'] ?? null;
            if (empty($value)) {
                $errors['date_declaration'] = 'La date de la déclaration est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_declaration'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('heure_declaration', $data)) {
            $value = $data['heure_declaration'] ?? null;
            if (empty($value)) {
                $errors['heure_declaration'] = "L'heure de la déclaration est requise";
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_declaration'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        if ($isCreate || array_key_exists('identite_declarant', $data)) {
            if (empty(trim((string) ($data['identite_declarant'] ?? '')))) {
                $errors['identite_declarant'] = "L'identité du déclarant est requise";
            }
        }

        if ($isCreate || array_key_exists('nature_objet', $data)) {
            if (empty(trim((string) ($data['nature_objet'] ?? '')))) {
                $errors['nature_objet'] = "La nature de l'objet perdu est requise";
            }
        }

        if ($isCreate || array_key_exists('description_objet', $data)) {
            if (empty(trim((string) ($data['description_objet'] ?? '')))) {
                $errors['description_objet'] = "La description de l'objet est requise";
            }
        }

        if ($isCreate || array_key_exists('date_perte', $data)) {
            $value = $data['date_perte'] ?? null;
            if (empty($value)) {
                $errors['date_perte'] = 'La date présumée de la perte est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_perte'] = 'La date présumée de la perte est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('lieu_perte', $data)) {
            if (empty(trim((string) ($data['lieu_perte'] ?? '')))) {
                $errors['lieu_perte'] = 'Le lieu présumé de la perte est requis';
            }
        }

        if ($isCreate || array_key_exists('numero_attestation', $data)) {
            $value = trim((string) ($data['numero_attestation'] ?? ''));
            if ($value === '') {
                $errors['numero_attestation'] = "Le numéro d'attestation délivrée est requis";
            } else {
                $existing = DeclarationPerte::getByAttestation($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero_attestation'] = 'Ce numéro d\'attestation existe déjà';
                }
            }
        }

        if ($isCreate || array_key_exists('nom_agent', $data)) {
            if (empty(trim((string) ($data['nom_agent'] ?? '')))) {
                $errors['nom_agent'] = "Le nom de l'agent est requis";
            }
        }

        return $errors;
    }

    /**
     * GET /api/declarations-perte
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = DeclarationPerte::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/declarations-perte/{id}
     */
    public function show(array $params): void
    {
        $row = DeclarationPerte::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Declaration de perte not found');
        }
        $row['attachments'] = DeclarationPerteAttachment::getByDeclarationId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user once they
        // actually open the detail.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/secretariat/declaration-perte/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/declarations-perte
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

        $data['numero_attestation'] = trim($data['numero_attestation']);
        $data['created_by'] = $authUser['sub'] ?? null;

        $id = DeclarationPerte::create($data);
        $declaration = DeclarationPerte::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'declarations-perte',
            'entity_id' => $id,
            'description' => "Création d'une déclaration de perte (Attestation: {$declaration['numero_attestation']}) — {$declaration['identite_declarant']} — {$declaration['nature_objet']}",
            'new_values' => $declaration,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with declaration view) ---
        self::notifyChange('create', $declaration, $authUser['sub'] ?? null);

        Response::created($declaration, 'Déclaration de perte enregistrée avec succès');
    }

    /**
     * PUT /api/declarations-perte/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $declaration = DeclarationPerte::getById($id);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['numero_attestation'])) {
            $data['numero_attestation'] = trim($data['numero_attestation']);
        }

        // Uniqueness guard when the attestation number changes.
        $effectiveRef = $data['numero_attestation'] ?? $declaration['numero_attestation'];
        if ($effectiveRef !== $declaration['numero_attestation']) {
            $existing = DeclarationPerte::getByAttestation($effectiveRef);
            if ($existing && (int) $existing['id'] !== $id) {
                Response::error('Validation failed', 422, ['numero_attestation' => 'Ce numéro d\'attestation existe déjà']);
            }
        }

        $oldDeclaration = $declaration;
        DeclarationPerte::update($id, $data);
        $declaration = DeclarationPerte::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'declarations-perte',
            'entity_id' => $id,
            'description' => "Modification d'une déclaration de perte (Attestation: {$declaration['numero_attestation']}) — {$declaration['identite_declarant']}",
            'old_values' => $oldDeclaration,
            'new_values' => $declaration,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with declaration view) ---
        self::notifyChange('update', $declaration, $authUser['sub'] ?? null);

        Response::success($declaration, 'Déclaration de perte modifiée avec succès');
    }

    /**
     * DELETE /api/declarations-perte/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $declaration = DeclarationPerte::getById($id);
        if (!$declaration) {
            Response::notFound('Declaration de perte not found');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/declarations-perte';
        foreach (DeclarationPerteAttachment::getByDeclarationId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        DeclarationPerte::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'declarations-perte',
            'entity_id' => $id,
            'description' => "Suppression d'une déclaration de perte (Attestation: {$declaration['numero_attestation']}) — {$declaration['identite_declarant']}",
            'old_values' => $declaration,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Declaration de perte deleted successfully');
    }

    /**
     * Notify every admin AND every user with view permission on the
     * declaration_perte module that a declaration was created or modified.
     * The actor is excluded and recipients are deduplicated inside
     * Notification::notifyFeatureChange(); push delivery failures are
     * isolated inside Notification::create() and never affect the API
     * response.
     */
    private static function notifyChange(string $action, array $declaration, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($declaration['date_declaration']));
        $heure = substr((string) $declaration['heure_declaration'], 0, 5);
        $link = '/sedentaire/secretariat/declaration-perte/' . $declaration['id'];

        if ($action === 'create') {
            $title = 'Nouvelle déclaration de perte';
            $adminMessage = "Une déclaration de perte a été enregistrée. "
                . "Attestation: {$declaration['numero_attestation']} — Déclarant: {$declaration['identite_declarant']}"
                . " — Objet: {$declaration['nature_objet']}"
                . " — le {$date} à {$heure} — Agent: {$declaration['nom_agent']}.";
            $userMessage = "Une déclaration de perte a été enregistrée. "
                . "Attestation: {$declaration['numero_attestation']} — Déclarant: {$declaration['identite_declarant']}"
                . " — Objet: {$declaration['nature_objet']}"
                . " — le {$date} à {$heure}. Veuillez en prendre connaissance.";
        } else {
            $title = 'Déclaration de perte modifiée';
            $adminMessage = "La déclaration de perte (Attestation: {$declaration['numero_attestation']}) — {$declaration['identite_declarant']} a été modifiée.";
            $userMessage = "La déclaration de perte (Attestation: {$declaration['numero_attestation']}) — {$declaration['identite_declarant']} a été modifiée. Veuillez en prendre connaissance des modifications.";
        }

        Notification::notifyFeatureChange('sedentaire_secretariat_declaration_perte', [
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
