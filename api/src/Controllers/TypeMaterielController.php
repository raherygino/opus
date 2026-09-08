<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\TypeMateriel;
use App\Models\AuditLog;

class TypeMaterielController
{
    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('nom', $data)) {
            $value = trim((string) ($data['nom'] ?? ''));
            if ($value === '') {
                $errors['nom'] = 'Le nom du type de matériel est requis';
            } else {
                $existing = TypeMateriel::getByNom($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['nom'] = 'Ce type de matériel existe déjà';
                }
            }
        }

        if (array_key_exists('description', $data) && $data['description'] !== null && $data['description'] !== '') {
            if (!is_string($data['description'])) {
                $errors['description'] = 'La description doit être un texte';
            }
        }

        return $errors;
    }

    /**
     * GET /api/types-materiels
     */
    public function index(array $params): void
    {
        $filters = [];
        if (isset($_GET['search']) && $_GET['search'] !== '') {
            $filters['search'] = $_GET['search'];
        }

        $list = TypeMateriel::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/types-materiels/{id}
     */
    public function show(array $params): void
    {
        $row = TypeMateriel::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Type de matériel introuvable');
        }
        Response::success($row);
    }

    /**
     * POST /api/types-materiels
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

        $data['nom'] = trim($data['nom']);

        $id = TypeMateriel::create($data);
        $typeMateriel = TypeMateriel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'types_materiels',
            'entity_id' => $id,
            'description' => "Création du type de matériel « {$typeMateriel['nom']} »",
            'new_values' => $typeMateriel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($typeMateriel, 'Type de matériel créé avec succès');
    }

    /**
     * PUT /api/types-materiels/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $typeMateriel = TypeMateriel::getById($id);
        if (!$typeMateriel) {
            Response::notFound('Type de matériel introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['nom'])) {
            $data['nom'] = trim($data['nom']);
        }

        $oldTypeMateriel = $typeMateriel;
        TypeMateriel::update($id, $data);
        $typeMateriel = TypeMateriel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'types_materiels',
            'entity_id' => $id,
            'description' => "Modification du type de matériel « {$typeMateriel['nom']} »",
            'old_values' => $oldTypeMateriel,
            'new_values' => $typeMateriel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($typeMateriel, 'Type de matériel modifié avec succès');
    }

    /**
     * DELETE /api/types-materiels/{id}
     *
     * Rejected with 409 when assignment line items still reference this
     * type — deleting it would orphan the historical records.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $typeMateriel = TypeMateriel::getById($id);
        if (!$typeMateriel) {
            Response::notFound('Type de matériel introuvable');
        }

        $usageCount = TypeMateriel::countAffectationLignes($id);
        if ($usageCount > 0) {
            Response::error(
                "Impossible de supprimer ce type de matériel : {$usageCount} affectation(s) l'utilisent encore.",
                409
            );
        }

        TypeMateriel::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'types_materiels',
            'entity_id' => $id,
            'description' => "Suppression du type de matériel « {$typeMateriel['nom']} »",
            'old_values' => $typeMateriel,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Type de matériel supprimé avec succès');
    }
}
