<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\MaterielRoulant;
use App\Models\Personnel;
use App\Models\AuditLog;
use App\Models\Notification;

class MaterielRoulantController
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

        if ($isCreate || array_key_exists('type_materiel', $data)) {
            $value = $data['type_materiel'] ?? null;
            if (empty($value)) {
                $errors['type_materiel'] = 'Le type de matériel est requis';
            } elseif (!in_array($value, MaterielRoulant::TYPES, true)) {
                $errors['type_materiel'] = 'Le type de matériel doit être VHL ou Moto';
            }
        }

        // Numéro d'immatriculation — optional free text, trimmed and capped at 50 chars.
        if (array_key_exists('numero_immatriculation', $data) && $data['numero_immatriculation'] !== null && $data['numero_immatriculation'] !== '') {
            $value = trim((string) $data['numero_immatriculation']);
            if (mb_strlen($value) > 50) {
                $errors['numero_immatriculation'] = 'Le numéro d\'immatriculation ne peut pas dépasser 50 caractères';
            }
        }

        // Description du véhicule — optional free text (body type, brand, model), capped at 255 chars.
        if (array_key_exists('description_vehicule', $data) && $data['description_vehicule'] !== null && $data['description_vehicule'] !== '') {
            $value = trim((string) $data['description_vehicule']);
            if (mb_strlen($value) > 255) {
                $errors['description_vehicule'] = 'La description du véhicule ne peut pas dépasser 255 caractères';
            }
        }

        // The driver (agent conducteur) MUST be selected on create.
        if ($isCreate) {
            if (empty($data['agent_conducteur_personnel_id'])) {
                $errors['agent_conducteur_personnel_id'] = "L'agent conducteur est requis";
            }
        }

        // Chef de bord is optional but, when provided, must reference a valid
        // personnel id (existence is checked separately against the DB).
        if (!empty($data['chef_de_bord_personnel_id']) && (int) $data['chef_de_bord_personnel_id'] <= 0) {
            $errors['chef_de_bord_personnel_id'] = 'Le chef de bord sélectionné est invalide';
        }

        // Departure mileage — numeric, non-negative.
        if (array_key_exists('kilometrage_depart', $data) && $data['kilometrage_depart'] !== null && $data['kilometrage_depart'] !== '') {
            if (!is_numeric($data['kilometrage_depart']) || (float) $data['kilometrage_depart'] < 0) {
                $errors['kilometrage_depart'] = 'Le kilométrage de départ doit être un nombre positif';
            }
        }

        // Departure fuel — numeric, 0-100.
        if (array_key_exists('niveau_carburant_depart', $data) && $data['niveau_carburant_depart'] !== null && $data['niveau_carburant_depart'] !== '') {
            $value = $data['niveau_carburant_depart'];
            if (!is_numeric($value) || (float) $value < 0 || (float) $value > 100) {
                $errors['niveau_carburant_depart'] = 'Le niveau de carburant doit être compris entre 0 et 100';
            }
        }

        return $errors;
    }

    /**
     * Validation for the reintegration transition — only the return fields
     * and the technical observations/defaillances are accepted here.
     */
    private static function validateReintegration(array $data, array $materiel): array
    {
        $errors = [];

        $value = $data['heure_reintegration'] ?? null;
        if (empty($value)) {
            $errors['heure_reintegration'] = "L'heure de la réintégration est requise";
        } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
            $errors['heure_reintegration'] = "L'heure est invalide (format attendu : HH:MM)";
        }

        $dateReint = $data['date_reintegration'] ?? null;
        if (empty($dateReint)) {
            $errors['date_reintegration'] = 'La date de la réintégration est requise';
        } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $dateReint)) {
            $errors['date_reintegration'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
        }

        // Business rule: reintegration date must not be earlier than perception date.
        if (!empty($dateReint) && !empty($materiel['date_perception'])) {
            if ($dateReint < $materiel['date_perception']) {
                $errors['date_reintegration'] = 'La date de réintégration ne peut pas être antérieure à la date de perception';
            }
        }

        // Business rule: return time cannot be earlier than perception time
        // when the reintegration happens on the same day as the perception.
        $heureReint = $data['heure_reintegration'] ?? null;
        $heurePerc = $materiel['heure_perception'] ?? null;
        if (!empty($dateReint) && !empty($heureReint) && !empty($heurePerc)
            && $dateReint === $materiel['date_perception']
            && substr($heureReint, 0, 5) < substr($heurePerc, 0, 5)) {
            $errors['heure_reintegration'] = "L'heure de réintégration ne peut pas être antérieure à l'heure de perception";
        }

        // Return mileage — numeric, non-negative, and cannot be lower than departure.
        $kmRetour = $data['kilometrage_retour'] ?? null;
        if ($kmRetour === null || $kmRetour === '') {
            $errors['kilometrage_retour'] = 'Le kilométrage de retour est requis';
        } elseif (!is_numeric($kmRetour) || (float) $kmRetour < 0) {
            $errors['kilometrage_retour'] = 'Le kilométrage de retour doit être un nombre positif';
        } elseif ($materiel['kilometrage_depart'] !== null
            && (float) $kmRetour < (float) $materiel['kilometrage_depart']) {
            $errors['kilometrage_retour'] = 'Le kilométrage de retour ne peut pas être inférieur au kilométrage de départ';
        }

        // Return fuel — numeric, 0-100.
        $carburantRetour = $data['niveau_carburant_retour'] ?? null;
        if ($carburantRetour !== null && $carburantRetour !== '') {
            if (!is_numeric($carburantRetour) || (float) $carburantRetour < 0 || (float) $carburantRetour > 100) {
                $errors['niveau_carburant_retour'] = 'Le niveau de carburant doit être compris entre 0 et 100';
            }
        }

        // Technical observations and defaillances are both optional free-text,
        // but a normal observation must be distinct from an actual reported
        // defect/failure — we reject the case where both fields hold the exact
        // same non-empty value, since that would mean the operator copied the
        // same text into both boxes instead of distinguishing them.
        $obs = trim((string) ($data['observations_techniques'] ?? ''));
        $def = trim((string) ($data['defaillances'] ?? ''));
        if ($obs !== '' && $obs === $def) {
            $errors['defaillances'] = "Les observations techniques et les défaillances doivent être distinctes";
        }

        return $errors;
    }

    /**
     * Snapshot the driver identity (IM + grade + full name) from the
     * personnel table when agent_conducteur_personnel_id is provided.
     */
    private static function snapshotAgentConducteur(array &$data): void
    {
        if (empty($data['agent_conducteur_personnel_id'])) {
            return;
        }
        $agent = Personnel::getById((int) $data['agent_conducteur_personnel_id']);
        if (!$agent) {
            $data['agent_conducteur_personnel_id'] = null;
            return;
        }
        $data['agent_conducteur_im'] = $agent['im'] ?? null;
        $data['agent_conducteur_grade'] = $agent['grade'] ?? null;
        $data['agent_conducteur_nom'] = trim(
            (($agent['firstname'] ?? '') . ' ' . ($agent['lastname'] ?? ''))
        ) ?: null;
    }

    /**
     * Snapshot the chef de bord identity (IM + grade + full name) from the
     * personnel table when chef_de_bord_personnel_id is provided.
     */
    private static function snapshotChefDeBord(array &$data): void
    {
        if (empty($data['chef_de_bord_personnel_id'])) {
            return;
        }
        $chef = Personnel::getById((int) $data['chef_de_bord_personnel_id']);
        if (!$chef) {
            $data['chef_de_bord_personnel_id'] = null;
            return;
        }
        $data['chef_de_bord_im'] = $chef['im'] ?? null;
        $data['chef_de_bord_grade'] = $chef['grade'] ?? null;
        $data['chef_de_bord_nom'] = trim(
            (($chef['firstname'] ?? '') . ' ' . ($chef['lastname'] ?? ''))
        ) ?: null;
    }

    /**
     * GET /api/materiels-roulants
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['date_from', 'date_to', 'statut', 'type_materiel', 'agent_conducteur_personnel_id', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = MaterielRoulant::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/materiels-roulants/{id}
     */
    public function show(array $params): void
    {
        $row = MaterielRoulant::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Matériel roulant introuvable');
        }

        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                '/sedentaire/poste/materiel-roulant/' . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/materiels-roulants
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $data['created_by'] = (int) $authUser['sub'];

        // Normalize the registration plate: trim, empty string becomes null.
        if (array_key_exists('numero_immatriculation', $data)) {
            $data['numero_immatriculation'] = trim((string) $data['numero_immatriculation']) ?: null;
        }

        // Normalize the vehicle description: trim, empty string becomes null.
        if (array_key_exists('description_vehicule', $data)) {
            $data['description_vehicule'] = trim((string) $data['description_vehicule']) ?: null;
        }

        self::snapshotAgentConducteur($data);
        self::snapshotChefDeBord($data);

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Validate driver exists
        $driverId = (int) ($data['agent_conducteur_personnel_id'] ?? 0);
        if ($driverId <= 0 || !Personnel::getById($driverId)) {
            Response::error('Validation failed', 422, [
                'agent_conducteur_personnel_id' => "L'agent conducteur sélectionné n'existe pas",
            ]);
        }

        // Verify the conducteur's code secret before accepting the
        // perception. The code must be provided and must match the
        // personnel's stored code_secret_hash.
        $codeSecret = (string) ($data['code_secret'] ?? '');
        if ($codeSecret === '') {
            Response::error('Validation failed', 422, [
                'code_secret' => "Le code secret du conducteur est requis",
            ]);
        }

        if (!Personnel::verifyCodeSecret($driverId, $codeSecret)) {
            AuditLog::create([
                'user_id' => $authUser['sub'] ?? null,
                'action' => 'verify_code_secret',
                'module' => 'materiels_roulants',
                'entity_id' => $driverId,
                'description' => "Échec de vérification du code secret lors de la perception de matériel roulant — Personnel ID: {$driverId}",
                'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
                'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
            ]);
            Response::error('Code secret incorrect — la perception ne peut pas être enregistrée', 422, [
                'code_secret' => 'Le code secret du conducteur est incorrect',
            ]);
        }

        // Verification succeeded — mark the perception as verified.
        $data['agent_verifie'] = 1;
        $data['agent_verifie_at'] = date('Y-m-d H:i:s');

        // Validate chef de bord exists when provided
        if (!empty($data['chef_de_bord_personnel_id'])) {
            $chefId = (int) $data['chef_de_bord_personnel_id'];
            if ($chefId <= 0 || !Personnel::getById($chefId)) {
                Response::error('Validation failed', 422, [
                    'chef_de_bord_personnel_id' => 'Le chef de bord sélectionné n\'existe pas',
                ]);
            }
        }

        $id = MaterielRoulant::create($data);
        $materiel = MaterielRoulant::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'materiels_roulants',
            'entity_id' => $id,
            'description' => "Perception de matériel roulant ({$materiel['type_materiel']}) le {$materiel['date_perception']} — Conducteur: {$materiel['agent_conducteur_grade']} {$materiel['agent_conducteur_nom']} (identité vérifiée)",
            'new_values' => $materiel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $materiel, $authUser['sub'] ?? null);

        Response::created($materiel, 'Perception enregistrée avec succès');
    }

    /**
     * PUT /api/materiels-roulants/{id}
     *
     * Updates the perception fields only. The reintegration columns and the
     * technical observations/defaillances captured at return are NOT editable
     * here — they are set once via POST .../reintegration, preserving the
     * operational history rather than silently overwriting it.
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $materiel = MaterielRoulant::getById($id);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Reintegration fields, technical observations/defaillances, and the
        // agent verification fields are NOT editable through update — they
        // are set once at perception/reintegration time.
        unset(
            $data['heure_reintegration'],
            $data['date_reintegration'],
            $data['kilometrage_retour'],
            $data['niveau_carburant_retour'],
            $data['observations_techniques'],
            $data['defaillances'],
            $data['statut'],
            $data['code_secret'],
            $data['agent_verifie'],
            $data['agent_verifie_at'],
            $data['signature_svg']
        );

        // Normalize the registration plate: trim, empty string becomes null.
        if (array_key_exists('numero_immatriculation', $data)) {
            $data['numero_immatriculation'] = trim((string) $data['numero_immatriculation']) ?: null;
        }

        // Normalize the vehicle description: trim, empty string becomes null.
        if (array_key_exists('description_vehicule', $data)) {
            $data['description_vehicule'] = trim((string) $data['description_vehicule']) ?: null;
        }

        self::snapshotAgentConducteur($data);
        self::snapshotChefDeBord($data);

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Validate driver exists when provided
        if (!empty($data['agent_conducteur_personnel_id'])) {
            $driverId = (int) $data['agent_conducteur_personnel_id'];
            if ($driverId <= 0 || !Personnel::getById($driverId)) {
                Response::error('Validation failed', 422, [
                    'agent_conducteur_personnel_id' => "L'agent conducteur sélectionné n'existe pas",
                ]);
            }
        }

        // Validate chef de bord exists when provided
        if (!empty($data['chef_de_bord_personnel_id'])) {
            $chefId = (int) $data['chef_de_bord_personnel_id'];
            if ($chefId <= 0 || !Personnel::getById($chefId)) {
                Response::error('Validation failed', 422, [
                    'chef_de_bord_personnel_id' => 'Le chef de bord sélectionné n\'existe pas',
                ]);
            }
        }

        $oldMateriel = $materiel;
        MaterielRoulant::update($id, $data);
        $materiel = MaterielRoulant::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'materiels_roulants',
            'entity_id' => $id,
            'description' => "Modification d'une perception de matériel roulant ({$materiel['type_materiel']}) le {$materiel['date_perception']} — Conducteur: {$materiel['agent_conducteur_grade']} {$materiel['agent_conducteur_nom']}",
            'old_values' => $oldMateriel,
            'new_values' => $materiel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $materiel, $authUser['sub'] ?? null);

        Response::success($materiel, 'Perception modifiée avec succès');
    }

    /**
     * POST /api/materiels-roulants/{id}/reintegration
     *
     * One-way transition: fills the reintegration columns, the return
     * mileage/fuel and the technical observations/defaillances, and sets
     * statut to 'Réintégré'. Rejected with 409 when already reintegrated.
     */
    public function reintegrate(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $materiel = MaterielRoulant::getById($id);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        if ($materiel['heure_reintegration'] !== null) {
            Response::error('Ce matériel roulant a déjà été réintégré', 409);
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validateReintegration($data, $materiel);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldMateriel = $materiel;

        MaterielRoulant::reintegrate($id, [
            'heure_reintegration' => $data['heure_reintegration'],
            'date_reintegration' => $data['date_reintegration'] ?? null,
            'kilometrage_retour' => $data['kilometrage_retour'] ?? null,
            'niveau_carburant_retour' => $data['niveau_carburant_retour'] ?? null,
            'observations_techniques' => trim((string) ($data['observations_techniques'] ?? '')) ?: null,
            'defaillances' => trim((string) ($data['defaillances'] ?? '')) ?: null,
        ]);

        $materiel = MaterielRoulant::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'reintegration',
            'module' => 'materiels_roulants',
            'entity_id' => $id,
            'description' => "Réintégration de matériel roulant ({$materiel['type_materiel']}) le {$materiel['date_reintegration']} à " . substr((string) $materiel['heure_reintegration'], 0, 5) . " — Conducteur: {$materiel['agent_conducteur_grade']} {$materiel['agent_conducteur_nom']}",
            'old_values' => $oldMateriel,
            'new_values' => $materiel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('reintegration', $materiel, $authUser['sub'] ?? null);

        Response::success($materiel, 'Matériel roulant réintégré avec succès');
    }

    /**
     * DELETE /api/materiels-roulants/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $materiel = MaterielRoulant::getById($id);
        if (!$materiel) {
            Response::notFound('Matériel roulant introuvable');
        }

        MaterielRoulant::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'materiels_roulants',
            'entity_id' => $id,
            'description' => "Suppression d'une perception de matériel roulant ({$materiel['type_materiel']}) le {$materiel['date_perception']} — Conducteur: {$materiel['agent_conducteur_grade']} {$materiel['agent_conducteur_nom']}",
            'old_values' => $materiel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Matériel roulant supprimé avec succès');
    }

    /**
     * Notify admins + users with view permission on the matériel roulant module.
     */
    private static function notifyChange(string $action, array $materiel, ?int $actorId): void
    {
        $date = date('d/m/Y', strtotime($materiel['date_perception']));
        $heure = substr((string) $materiel['heure_perception'], 0, 5);
        $link = '/sedentaire/poste/materiel-roulant/' . $materiel['id'];

        $conducteur = trim(($materiel['agent_conducteur_grade'] ?? '') . ' ' . ($materiel['agent_conducteur_nom'] ?? ''));
        $type = $materiel['type_materiel'] ?? '';

        if ($action === 'create') {
            $title = 'Nouvelle perception de matériel roulant';
            $adminMessage = "Une perception de matériel roulant ({$type}) a été enregistrée. "
                . "Conducteur: {$conducteur} — le {$date} à {$heure}.";
            $userMessage = "Une perception de matériel roulant ({$type}) a été enregistrée. "
                . "Conducteur: {$conducteur} — le {$date} à {$heure}. Veuillez en prendre connaissance.";
        } elseif ($action === 'reintegration') {
            $dateReint = $materiel['date_reintegration'] ? date('d/m/Y', strtotime($materiel['date_reintegration'])) : '';
            $heureReint = substr((string) $materiel['heure_reintegration'], 0, 5);
            $title = 'Matériel roulant réintégré';
            $hasDefaillances = !empty(trim((string) ($materiel['defaillances'] ?? '')));
            $defaillanceNote = $hasDefaillances ? ' Des défaillances ont été signalées.' : '';
            $adminMessage = "Le matériel roulant ({$type}) perçu par {$conducteur} le {$date} a été réintégré le {$dateReint} à {$heureReint}.{$defaillanceNote}";
            $userMessage = "Le matériel roulant ({$type}) perçu par {$conducteur} le {$date} a été réintégré le {$dateReint} à {$heureReint}.{$defaillanceNote} Veuillez en prendre connaissance.";
        } else {
            $title = 'Perception de matériel roulant modifiée';
            $adminMessage = "La perception de matériel roulant du {$date} ({$type} — Conducteur: {$conducteur}) a été modifiée.";
            $userMessage = "La perception de matériel roulant du {$date} ({$type} — Conducteur: {$conducteur}) a été modifiée. Veuillez en prendre connaissance des modifications.";
        }

        Notification::notifyFeatureChange('sedentaire_poste_materiel_roulant', [
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
