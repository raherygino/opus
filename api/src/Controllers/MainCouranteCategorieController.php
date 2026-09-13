<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\AuditLog;
use App\Models\MainCouranteCategorie;

class MainCouranteCategorieController
{
    /**
     * GET /api/main-courante-categories
     */
    public function index(array $params): void
    {
        Response::success(MainCouranteCategorie::getAll());
    }

    /**
     * POST /api/main-courante-categories
     * Body: { "label": "..." }
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $label = trim((string) ($data['label'] ?? ''));

        if ($label === '') {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est requis']);
        }
        if (mb_strlen($label) > 100) {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est trop long (100 caractères max)']);
        }
        if (MainCouranteCategorie::exists($label)) {
            Response::error('Validation failed', 422, ['label' => 'Cette catégorie existe déjà']);
        }

        $id = MainCouranteCategorie::create($label);
        $row = MainCouranteCategorie::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'main_courante_categorie',
            'entity_id' => $id,
            'description' => "Création de la catégorie de main courante « $label »",
            'new_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($row, 'Catégorie créée avec succès');
    }

    /**
     * PUT /api/main-courante-categories/{id}
     * Body: { "label": "..." }
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $row = MainCouranteCategorie::getById($id);
        if (!$row) {
            Response::notFound('Catégorie introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $label = trim((string) ($data['label'] ?? ''));

        if ($label === '') {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est requis']);
        }
        if (mb_strlen($label) > 100) {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est trop long (100 caractères max)']);
        }
        // Allow keeping the same label; only reject if a DIFFERENT row has it.
        $existing = MainCouranteCategorie::getByLabel($label);
        if ($existing && (int) $existing['id'] !== $id) {
            Response::error('Validation failed', 422, ['label' => 'Cette catégorie existe déjà']);
        }

        $oldRow = $row;
        MainCouranteCategorie::update($id, $label);
        $row = MainCouranteCategorie::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'main_courante_categorie',
            'entity_id' => $id,
            'description' => "Renommage de la catégorie de main courante « {$oldRow['label']} » en « $label »",
            'old_values' => $oldRow,
            'new_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($row, 'Catégorie modifiée avec succès');
    }

    /**
     * DELETE /api/main-courante-categories/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $row = MainCouranteCategorie::getById($id);
        if (!$row) {
            Response::notFound('Catégorie introuvable');
        }

        MainCouranteCategorie::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'main_courante_categorie',
            'entity_id' => $id,
            'description' => "Suppression de la catégorie de main courante « {$row['label']} »",
            'old_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Catégorie supprimée avec succès');
    }
}
