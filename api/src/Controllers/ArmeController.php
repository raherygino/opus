<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Arme;
use App\Models\ArmeMunitionsConsommation;
use App\Models\TypeArme;
use App\Models\AuditLog;
use App\Database;

class ArmeController
{
    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('type_arme_id', $data)) {
            $value = $data['type_arme_id'] ?? null;
            if (empty($value) || (int) $value <= 0) {
                $errors['type_arme_id'] = 'Le type d\'arme est requis';
            } elseif (!TypeArme::getById((int) $value)) {
                $errors['type_arme_id'] = 'Le type d\'arme sélectionné n\'existe pas';
            }
        }

        if ($isCreate || array_key_exists('matricule', $data)) {
            $value = trim((string) ($data['matricule'] ?? ''));
            if ($value === '') {
                $errors['matricule'] = 'Le matricule est requis';
            } else {
                $existing = Arme::getByMatricule($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['matricule'] = 'Ce matricule existe déjà';
                }
            }
        }

        if (array_key_exists('munitions_stock', $data) && $data['munitions_stock'] !== null && $data['munitions_stock'] !== '') {
            if (!is_numeric($data['munitions_stock']) || (int) $data['munitions_stock'] < 0) {
                $errors['munitions_stock'] = 'Le stock de munitions doit être un nombre entier positif';
            }
        }

        return $errors;
    }

    /**
     * GET /api/armes
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['type_arme_id', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = Arme::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/armes/{id}
     */
    public function show(array $params): void
    {
        $row = Arme::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Arme introuvable');
        }
        Response::success($row);
    }

    /**
     * GET /api/armes/{id}/consommations
     *
     * Returns the ammunition consumption history for a specific weapon.
     */
    public function consommations(array $params): void
    {
        $id = (int) $params['id'];
        $arme = Arme::getById($id);
        if (!$arme) {
            Response::notFound('Arme introuvable');
        }
        $list = ArmeMunitionsConsommation::getByArmeId($id);
        Response::success($list);
    }

    /**
     * POST /api/armes
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

        $data['matricule'] = trim($data['matricule']);

        $id = Arme::create($data);
        $arme = Arme::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'armes',
            'entity_id' => $id,
            'description' => "Création de l'arme « {$arme['type_arme_nom']} » (matricule {$arme['matricule']})",
            'new_values' => $arme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($arme, 'Arme créée avec succès');
    }

    /**
     * PUT /api/armes/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $arme = Arme::getById($id);
        if (!$arme) {
            Response::notFound('Arme introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['matricule'])) {
            $data['matricule'] = trim($data['matricule']);
        }

        $oldArme = $arme;
        Arme::update($id, $data);
        $arme = Arme::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'armes',
            'entity_id' => $id,
            'description' => "Modification de l'arme « {$arme['type_arme_nom']} » (matricule {$arme['matricule']})",
            'old_values' => $oldArme,
            'new_values' => $arme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($arme, 'Arme modifiée avec succès');
    }

    /**
     * DELETE /api/armes/{id}
     *
     * Rejected with 409 when perceptions (armement) or consumption history
     * reference this weapon. Historical records must never be silently
     * orphaned — the caller must acknowledge the deletion explicitly.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $arme = Arme::getById($id);
        if (!$arme) {
            Response::notFound('Arme introuvable');
        }

        $armementsCount = Arme::countArmements($id);
        $consommationsCount = Arme::countConsommations($id);
        if ($armementsCount > 0 || $consommationsCount > 0) {
            $parts = [];
            if ($armementsCount > 0) {
                $parts[] = "{$armementsCount} perception(s)";
            }
            if ($consommationsCount > 0) {
                $parts[] = "{$consommationsCount} consommation(s)";
            }
            Response::error(
                "Impossible de supprimer cette arme : elle est référencée par " . implode(' et ', $parts) . ". Les enregistrements historiques doivent être conservés.",
                409
            );
        }

        Arme::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'armes',
            'entity_id' => $id,
            'description' => "Suppression de l'arme « {$arme['type_arme_nom']} » (matricule {$arme['matricule']})",
            'old_values' => $arme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Arme supprimée avec succès');
    }

    /**
     * POST /api/armes/{id}/consommation
     *
     * Records an ammunition consumption for a specific weapon and decreases
     * its stock atomically inside a single database transaction:
     *
     *   BEGIN
     *     1. Verify the weapon exists.
     *     2. Atomically decrease stock (rejected if insufficient).
     *     3. Insert the consumption history row.
     *   COMMIT
     *
     * The conditional UPDATE (WHERE munitions_stock >= ?) prevents the
     * stock from going negative AND prevents race conditions: MySQL takes
     * an exclusive row lock on the matching row, so two concurrent
     * consumers cannot both succeed when only one unit is left.
     *
     * Request body:
     *   {
     *     "agent_id": 123,        // optional — personnel who consumed
     *     "quantite": 1,          // required — rounds consumed
     *     "armement_id": 45       // optional — perception that triggered it
     *   }
     */
    public function consommationAction(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $arme = Arme::getById($id);
        if (!$arme) {
            Response::notFound('Arme introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // --- Validation -------------------------------------------------
        $errors = [];

        $quantite = $data['quantite'] ?? null;
        if ($quantite === null || $quantite === '') {
            $errors['quantite'] = 'La quantité consommée est requise';
        } elseif (!is_numeric($quantite) || (int) $quantite <= 0) {
            $errors['quantite'] = 'La quantité consommée doit être un nombre entier positif';
        }

        $agentId = $data['agent_id'] ?? null;
        if ($agentId !== null && $agentId !== '' && (int) $agentId > 0) {
            $agentId = (int) $agentId;
        } else {
            $agentId = null;
        }

        $armementId = $data['armement_id'] ?? null;
        if ($armementId !== null && $armementId !== '' && (int) $armementId > 0) {
            $armementId = (int) $armementId;
        } else {
            $armementId = null;
        }

        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $quantite = (int) $quantite;

        // --- Atomic transaction: stock deduction + history insert --------
        $db = Database::getInstance()->getConnection();

        try {
            $db->beginTransaction();

            // Re-read inside the transaction so the row lock is taken now.
            $current = Arme::getById($id);
            if (!$current) {
                $db->rollBack();
                Response::notFound('Arme introuvable');
            }

            // Atomic conditional decrease — fails if stock insufficient.
            // Stock is managed at the type_arme level (shared across all
            // weapons of the same type).
            if (!Arme::decreaseStock($id, $quantite)) {
                $db->rollBack();
                $typeStock = $current['type_arme_munitions_stock'] ?? 0;
                Response::error(
                    "Stock de munitions insuffisant pour ce type d'arme. Stock actuel : {$typeStock}, quantité demandée : {$quantite}.",
                    422,
                    ['quantite' => 'Stock insuffisant']
                );
            }

            // Record the consumption in the audit history.
            ArmeMunitionsConsommation::create([
                'arme_id' => $id,
                'agent_id' => $agentId,
                'armement_id' => $armementId,
                'quantite' => $quantite,
                'date_consommation' => date('Y-m-d H:i:s'),
            ]);

            $db->commit();
        } catch (\Throwable $e) {
            if ($db->inTransaction()) {
                $db->rollBack();
            }
            Response::error('Erreur lors de l\'enregistrement de la consommation', 500);
        }

        $arme = Arme::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'consommation',
            'module' => 'armes',
            'entity_id' => $id,
            'description' => "Consommation de {$quantite} munition(s) pour l'arme « {$arme['type_arme_nom']} » (matricule {$arme['matricule']}). Stock restant du type : {$arme['type_arme_munitions_stock']}.",
            'new_values' => $arme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($arme, 'Consommation enregistrée avec succès');
    }
}
