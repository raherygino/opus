<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\AffectationMateriel;
use App\Models\TypeMateriel;
use App\Models\Personnel;
use App\Models\AuditLog;
use App\Models\Notification;
use App\Database;

class AffectationMaterielController
{
    /**
     * Validation shared by store() and update() — perception fields only.
     * Returns an array of field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('agent_personnel_id', $data)) {
            $value = (int) ($data['agent_personnel_id'] ?? 0);
            if ($value <= 0) {
                $errors['agent_personnel_id'] = 'L\'agent est requis';
            }
        }

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
                $errors['heure_perception'] = 'L\'heure de la perception est requise';
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_perception'] = 'L\'heure est invalide (format attendu : HH:MM)';
            }
        }

        // Line items (lignes) — at least one material type is required on create.
        if ($isCreate || array_key_exists('lignes', $data)) {
            $lignes = $data['lignes'] ?? [];
            if (!is_array($lignes)) {
                $errors['lignes'] = 'Les lignes de matériel doivent être une liste';
            } elseif ($isCreate && count($lignes) === 0) {
                $errors['lignes'] = 'Au moins un type de matériel est requis';
            } else {
                foreach ($lignes as $i => $ligne) {
                    if (empty((int) ($ligne['type_materiel_id'] ?? 0))) {
                        $errors["lignes[$i].type_materiel_id"] = 'Le type de matériel est requis';
                        continue;
                    }
                    if (empty(trim((string) ($ligne['numero_materiel'] ?? '')))) {
                        $errors["lignes[$i].numero_materiel"] = 'L\'ID Matériel est requis';
                    }
                }
            }
        }

        return $errors;
    }

    /**
     * Validation for the reintegration transition.
     */
    private static function validateReintegration(array $data, array $affectation): array
    {
        $errors = [];

        $value = $data['heure_reintegration'] ?? null;
        if (empty($value)) {
            $errors['heure_reintegration'] = 'L\'heure de la réintégration est requise';
        } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
            $errors['heure_reintegration'] = 'L\'heure est invalide (format attendu : HH:MM)';
        }

        $dateReint = $data['date_reintegration'] ?? null;
        if (empty($dateReint)) {
            $errors['date_reintegration'] = 'La date de la réintégration est requise';
        } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateReint)) {
            $errors['date_reintegration'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
        }

        // Business rule 5: reintegration date must not be earlier than perception date.
        if (!empty($dateReint) && !empty($affectation['date_perception'])) {
            if ($dateReint < $affectation['date_perception']) {
                $errors['date_reintegration'] = 'La date de réintégration ne peut pas être antérieure à la date de perception';
            }
        }

        // Business rule 6: etat_reintegration is required per line item when returning.
        $ligneEtats = $data['ligne_etats'] ?? [];
        if (!is_array($ligneEtats)) {
            $errors['ligne_etats'] = 'Les états de réintégration doivent être fournis pour chaque matériel';
        } else {
            foreach ($affectation['lignes'] as $ligne) {
                $lid = (int) $ligne['id'];
                $etat = trim((string) ($ligneEtats[$lid] ?? ''));
                if ($etat === '') {
                    $errors["ligne_etats[$lid]"] = 'L\'état à la réintégration est requis pour chaque matériel';
                }
            }
        }

        return $errors;
    }

    /**
     * Snapshot the agent identity (IM + grade + full name) from the
     * personnel table when agent_personnel_id is provided.
     */
    private static function snapshotAgent(array &$data): void
    {
        if (empty($data['agent_personnel_id'])) {
            return;
        }
        $agent = Personnel::getById((int) $data['agent_personnel_id']);
        if (!$agent) {
            $data['agent_personnel_id'] = null;
            return;
        }
        $data['agent_im'] = $agent['im'] ?? null;
        $data['agent_grade'] = $agent['grade'] ?? null;
        $data['agent_nom'] = trim(
            (($agent['firstname'] ?? '') . ' ' . ($agent['lastname'] ?? ''))
        ) ?: null;
    }

    /**
     * Enrich line items with the type_materiel_nom snapshot from the
     * type_materiel table.
     */
    private static function snapshotLignes(array &$lignes): void
    {
        foreach ($lignes as &$ligne) {
            $type = TypeMateriel::getById((int) ($ligne['type_materiel_id'] ?? 0));
            if ($type) {
                $ligne['type_materiel_nom'] = $type['nom'];
            } else {
                $ligne['type_materiel_nom'] = $ligne['type_materiel_nom'] ?? '';
            }
            $ligne['numero_materiel'] = trim((string) ($ligne['numero_materiel'] ?? ''));
        }
        unset($ligne);
    }

    /**
     * GET /api/affectations-materiels
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'statut', 'agent_personnel_id', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = AffectationMateriel::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/affectations-materiels/{id}
     */
    public function show(array $params): void
    {
        $row = AffectationMateriel::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Affectation de matériel introuvable');
        }

        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/poste/materiels/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/affectations-materiels
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $data['created_by'] = (int) $authUser['sub'];

        self::snapshotAgent($data);

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Validate agent exists
        $agentPersonnelId = (int) ($data['agent_personnel_id'] ?? 0);
        if ($agentPersonnelId <= 0 || !Personnel::getById($agentPersonnelId)) {
            Response::error('Validation failed', 422, [
                'agent_personnel_id' => 'L\'agent sélectionné n\'existe pas',
            ]);
        }

        // Verify the agent's code secret before accepting the perception
        // (same flow as armement). The code must be provided and must match
        // the personnel's stored code_secret_hash.
        $codeSecret = (string) ($data['code_secret'] ?? '');
        if ($codeSecret === '') {
            Response::error('Validation failed', 422, [
                'code_secret' => "Le code secret de l'agent est requis",
            ]);
        }

        if (!Personnel::verifyCodeSecret($agentPersonnelId, $codeSecret)) {
            AuditLog::create([
                'user_id' => $authUser['sub'] ?? null,
                'action' => 'verify_code_secret',
                'module' => 'affectations_materiels',
                'entity_id' => $agentPersonnelId,
                'description' => "Échec de vérification du code secret lors de l'affectation de matériel — Personnel ID: {$agentPersonnelId}",
                'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
                'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
            ]);
            Response::error('Code secret incorrect — l\'affectation ne peut pas être enregistrée', 422, [
                'code_secret' => 'Le code secret de l\'agent est incorrect',
            ]);
        }

        // Verification succeeded — mark the affectation as verified.
        $data['agent_verifie'] = 1;
        $data['agent_verifie_at'] = date('Y-m-d H:i:s');

        // The signature SVG is optional but, when provided, is stored
        // permanently on the affectation record.
        // $data['signature_svg'] is passed through to AffectationMateriel::create().

        // Enrich line items with type names
        $lignes = $data['lignes'] ?? [];
        self::snapshotLignes($lignes);

        // Business rule 7: a material currently assigned should not be
        // simultaneously assigned to another agent.
        foreach ($lignes as $ligne) {
            $numero = trim((string) ($ligne['numero_materiel'] ?? ''));
            if ($numero !== '' && AffectationMateriel::isNumeroMaterielActivementAffecte($numero)) {
                Response::error('Validation failed', 422, [
                    'lignes' => "Le matériel « {$numero} » est actuellement affecté à un autre agent",
                ]);
            }
        }

        // Create the assignment + line items atomically.
        $db = Database::getInstance()->getConnection();
        $db->beginTransaction();
        try {
            $id = AffectationMateriel::create($data);
            foreach ($lignes as $ligne) {
                AffectationMateriel::createLigne([
                    'affectation_id'    => $id,
                    'type_materiel_id'  => $ligne['type_materiel_id'],
                    'type_materiel_nom' => $ligne['type_materiel_nom'],
                    'numero_materiel'   => $ligne['numero_materiel'],
                    'etat_emport'       => $ligne['etat_emport'] ?? null,
                ]);
            }
            $db->commit();
        } catch (\Throwable $e) {
            if ($db->inTransaction()) {
                $db->rollBack();
            }
            Response::error('Erreur lors de l\'enregistrement de l\'affectation', 500);
        }

        $affectation = AffectationMateriel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'affectations_materiels',
            'entity_id' => $id,
            'description' => "Affectation de matériel à {$affectation['agent_grade']} {$affectation['agent_nom']} le {$affectation['date_perception']} — " . count($affectation['lignes']) . ' matériel(s)',
            'new_values' => $affectation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $affectation, $authUser['sub'] ?? null);

        Response::created($affectation, 'Affectation enregistrée avec succès');
    }

    /**
     * PUT /api/affectations-materiels/{id}
     *
     * Updates the perception fields and line items. Reintegration columns
     * are NOT editable here — they are set once via POST .../reintegration.
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $affectation = AffectationMateriel::getById($id);
        if (!$affectation) {
            Response::notFound('Affectation de matériel introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Reintegration fields are NOT editable through update.
        unset(
            $data['heure_reintegration'],
            $data['date_reintegration'],
            $data['statut']
        );

        self::snapshotAgent($data);

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Validate agent exists when provided
        if (!empty($data['agent_personnel_id'])) {
            $agentPersonnelId = (int) $data['agent_personnel_id'];
            if ($agentPersonnelId <= 0 || !Personnel::getById($agentPersonnelId)) {
                Response::error('Validation failed', 422, [
                    'agent_personnel_id' => 'L\'agent sélectionné n\'existe pas',
                ]);
            }
        }

        // Replace line items if provided
        $lignes = $data['lignes'] ?? null;
        if ($lignes !== null) {
            self::snapshotLignes($lignes);

            // Business rule 7: check that no numero_materiel is actively
            // assigned in a DIFFERENT assignment.
            foreach ($lignes as $ligne) {
                $numero = trim((string) ($ligne['numero_materiel'] ?? ''));
                if ($numero !== '' && AffectationMateriel::isNumeroMaterielActivementAffecte($numero, $id)) {
                    Response::error('Validation failed', 422, [
                        'lignes' => "Le matériel « {$numero} » est actuellement affecté à un autre agent",
                    ]);
                }
            }
        }

        $oldAffectation = $affectation;

        $db = Database::getInstance()->getConnection();
        $db->beginTransaction();
        try {
            AffectationMateriel::update($id, $data);
            if ($lignes !== null) {
                AffectationMateriel::replaceLignes($id, $lignes);
            }
            $db->commit();
        } catch (\Throwable $e) {
            if ($db->inTransaction()) {
                $db->rollBack();
            }
            Response::error('Erreur lors de la modification de l\'affectation', 500);
        }

        $affectation = AffectationMateriel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'affectations_materiels',
            'entity_id' => $id,
            'description' => "Modification d'une affectation de matériel à {$affectation['agent_grade']} {$affectation['agent_nom']} le {$affectation['date_perception']}",
            'old_values' => $oldAffectation,
            'new_values' => $affectation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $affectation, $authUser['sub'] ?? null);

        Response::success($affectation, 'Affectation modifiée avec succès');
    }

    /**
     * POST /api/affectations-materiels/{id}/reintegration
     *
     * One-way transition: fills the reintegration columns and sets statut
     * to 'Réintégré'. Also fills etat_reintegration on each line item.
     * Rejected with 409 when already reintegrated.
     */
    public function reintegrate(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $affectation = AffectationMateriel::getById($id);
        if (!$affectation) {
            Response::notFound('Affectation de matériel introuvable');
        }

        if ($affectation['heure_reintegration'] !== null) {
            Response::error('Ce matériel a déjà été réintégré', 409);
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validateReintegration($data, $affectation);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldAffectation = $affectation;
        $ligneEtats = $data['ligne_etats'] ?? [];

        AffectationMateriel::reintegrate($id, [
            'heure_reintegration' => $data['heure_reintegration'],
            'date_reintegration'  => $data['date_reintegration'] ?? null,
        ], $ligneEtats);

        $affectation = AffectationMateriel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'reintegration',
            'module' => 'affectations_materiels',
            'entity_id' => $id,
            'description' => "Réintégration de matériel — {$affectation['agent_grade']} {$affectation['agent_nom']} le {$affectation['date_reintegration']}",
            'old_values' => $oldAffectation,
            'new_values' => $affectation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('reintegration', $affectation, $authUser['sub'] ?? null);

        Response::success($affectation, 'Matériel réintégré avec succès');
    }

    /**
     * DELETE /api/affectations-materiels/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $affectation = AffectationMateriel::getById($id);
        if (!$affectation) {
            Response::notFound('Affectation de matériel introuvable');
        }

        AffectationMateriel::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'affectations_materiels',
            'entity_id' => $id,
            'description' => "Suppression d'une affectation de matériel — {$affectation['agent_grade']} {$affectation['agent_nom']} le {$affectation['date_perception']}",
            'old_values' => $affectation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Affectation supprimée avec succès');
    }

    /**
     * Notify admins + users with view permission on the materiels module.
     */
    private static function notifyChange(string $action, array $affectation, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($affectation['date_perception']));
        $heure = substr((string) $affectation['heure_perception'], 0, 5);
        $link = '/sedentaire/poste/materiels/' . $affectation['id'];

        $agent = trim(($affectation['agent_grade'] ?? '') . ' ' . ($affectation['agent_nom'] ?? ''));
        $nbMateriels = count($affectation['lignes'] ?? []);

        if ($action === 'create') {
            $title = 'Nouvelle affectation de matériel';
            $adminMessage = "Une affectation de {$nbMateriels} matériel(s) a été enregistrée. Agent: {$agent} — le {$date} à {$heure}.";
            $userMessage = "Une affectation de {$nbMateriels} matériel(s) a été enregistrée. Agent: {$agent} — le {$date} à {$heure}. Veuillez en prendre connaissance.";
        } elseif ($action === 'reintegration') {
            $dateReint = $affectation['date_reintegration'] ? date('d/m/Y', strtotime($affectation['date_reintegration'])) : '';
            $heureReint = substr((string) $affectation['heure_reintegration'], 0, 5);
            $title = 'Matériel réintégré';
            $adminMessage = "Le matériel affecté à {$agent} a été réintégré le {$dateReint} à {$heureReint}.";
            $userMessage = "Le matériel affecté à {$agent} a été réintégré le {$dateReint} à {$heureReint}. Veuillez en prendre connaissance.";
        } else {
            $title = 'Affectation de matériel modifiée';
            $adminMessage = "L'affectation de matériel du {$date} (Agent: {$agent}) a été modifiée.";
            $userMessage = "L'affectation de matériel du {$date} (Agent: {$agent}) a été modifiée. Veuillez en prendre connaissance des modifications.";
        }

        Notification::notifyFeatureChange('sedentaire_poste_materiels', [
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
