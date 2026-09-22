<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\AuditLog;
use App\Models\EvenementSurvenuType;

class EvenementSurvenuTypeController
{
    /**
     * GET /api/evenement-survenu-types
     */
    public function index(array $params): void
    {
        Response::success(EvenementSurvenuType::getAll());
    }

    /**
     * POST /api/evenement-survenu-types
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
            Response::error('Validation failed', 422, ['label' => "Le libellé est requis"]);
        }
        if (mb_strlen($label) > 100) {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est trop long (100 caractères max)']);
        }
        if (EvenementSurvenuType::exists($label)) {
            Response::error('Validation failed', 422, ['label' => 'Ce type existe déjà']);
        }

        $id = EvenementSurvenuType::create($label);
        $row = EvenementSurvenuType::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'evenement_survenu_type',
            'entity_id' => $id,
            'description' => "Création du type d'évènement « $label »",
            'new_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::created($row, "Type d'évènement créé avec succès");
    }

    /**
     * PUT /api/evenement-survenu-types/{id}
     * Body: { "label": "..." }
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $row = EvenementSurvenuType::getById($id);
        if (!$row) {
            Response::notFound("Type d'évènement introuvable");
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $label = trim((string) ($data['label'] ?? ''));

        if ($label === '') {
            Response::error('Validation failed', 422, ['label' => "Le libellé est requis"]);
        }
        if (mb_strlen($label) > 100) {
            Response::error('Validation failed', 422, ['label' => 'Le libellé est trop long (100 caractères max)']);
        }
        // Allow keeping the same label; only reject if a DIFFERENT row has it.
        $existing = EvenementSurvenuType::getByLabel($label);
        if ($existing && (int) $existing['id'] !== $id) {
            Response::error('Validation failed', 422, ['label' => 'Ce type existe déjà']);
        }

        $oldRow = $row;
        EvenementSurvenuType::update($id, $label);
        $row = EvenementSurvenuType::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'evenement_survenu_type',
            'entity_id' => $id,
            'description' => "Renommage du type d'évènement « {$oldRow['label']} » en « $label »",
            'old_values' => $oldRow,
            'new_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($row, "Type d'évènement modifié avec succès");
    }

    /**
     * DELETE /api/evenement-survenu-types/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $row = EvenementSurvenuType::getById($id);
        if (!$row) {
            Response::notFound("Type d'évènement introuvable");
        }

        EvenementSurvenuType::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'evenement_survenu_type',
            'entity_id' => $id,
            'description' => "Suppression du type d'évènement « {$row['label']} »",
            'old_values' => $row,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, "Type d'évènement supprimé avec succès");
    }
}
