<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\TypeArme;
use App\Models\AuditLog;

class TypeArmeController
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
                $errors['nom'] = 'Le nom du type d\'arme est requis';
            } else {
                $existing = TypeArme::getByNom($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['nom'] = 'Ce type d\'arme existe déjà';
                }
            }
        }

        if (array_key_exists('description', $data) && $data['description'] !== null && $data['description'] !== '') {
            if (!is_string($data['description'])) {
                $errors['description'] = 'La description doit être un texte';
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
     * GET /api/types-armes
     */
    public function index(array $params): void
    {
        $filters = [];
        if (isset($_GET['search']) && $_GET['search'] !== '') {
            $filters['search'] = $_GET['search'];
        }

        $list = TypeArme::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/types-armes/{id}
     */
    public function show(array $params): void
    {
        $row = TypeArme::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Type d\'arme introuvable');
        }
        Response::success($row);
    }

    /**
     * POST /api/types-armes
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

        $id = TypeArme::create($data);
        $typeArme = TypeArme::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'types_armes',
            'entity_id' => $id,
            'description' => "Création du type d'arme « {$typeArme['nom']} »",
            'new_values' => $typeArme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($typeArme, 'Type d\'arme créé avec succès');
    }

    /**
     * PUT /api/types-armes/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $typeArme = TypeArme::getById($id);
        if (!$typeArme) {
            Response::notFound('Type d\'arme introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        if (isset($data['nom'])) {
            $data['nom'] = trim($data['nom']);
        }

        $oldTypeArme = $typeArme;
        TypeArme::update($id, $data);
        $typeArme = TypeArme::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'types_armes',
            'entity_id' => $id,
            'description' => "Modification du type d'arme « {$typeArme['nom']} »",
            'old_values' => $oldTypeArme,
            'new_values' => $typeArme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($typeArme, 'Type d\'arme modifié avec succès');
    }

    /**
     * DELETE /api/types-armes/{id}
     *
     * Rejected with 409 when weapons (arme) still reference this type —
     * deleting it would orphan the weapons. The caller must reassign or
     * delete the weapons first.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $typeArme = TypeArme::getById($id);
        if (!$typeArme) {
            Response::notFound('Type d\'arme introuvable');
        }

        $armesCount = TypeArme::countArmes($id);
        if ($armesCount > 0) {
            Response::error(
                "Impossible de supprimer ce type d'arme : {$armesCount} arme(s) l'utilisent encore. Réaffectez ou supprimez ces armes d'abord.",
                409
            );
        }

        TypeArme::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'types_armes',
            'entity_id' => $id,
            'description' => "Suppression du type d'arme « {$typeArme['nom']} »",
            'old_values' => $typeArme,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Type d\'arme supprimé avec succès');
    }
}
