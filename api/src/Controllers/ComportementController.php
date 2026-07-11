<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\ComportementPersonnel;
use App\Models\Personnel;
use App\Models\AuditLog;

class ComportementController
{
    /**
     * GET /api/comportements
     */
    public function index(array $params): void
    {
        $filters = [];
        if (isset($_GET['personnel_id'])) {
            $filters['personnel_id'] = $_GET['personnel_id'];
        }
        if (isset($_GET['type'])) {
            $filters['type'] = $_GET['type'];
        }
        if (isset($_GET['search'])) {
            $filters['search'] = $_GET['search'];
        }

        $list = ComportementPersonnel::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/comportements/{id}
     */
    public function show(array $params): void
    {
        $row = ComportementPersonnel::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Comportement not found');
        }
        Response::success($row);
    }

    /**
     * POST /api/comportements
     */
    public function store(array $params): void
    {
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['personnel_id'])) {
            Response::error('personnel_id is required', 422, ['personnel_id' => "L'identifiant du personnel est requis"]);
        }
        if (empty($data['type']) || !in_array($data['type'], ['Positive', 'Negative'])) {
            Response::error('type must be Positive or Negative', 422, ['type' => 'Le type doit être Positive ou Negative']);
        }
        if (empty($data['date_comportement'])) {
            Response::error('date_comportement is required', 422, ['date_comportement' => 'La date est requise']);
        }
        if (empty($data['motif'])) {
            Response::error('motif is required', 422, ['motif' => 'Le motif est requis']);
        }

        $person = Personnel::getById((int) $data['personnel_id']);
        if (!$person) {
            Response::notFound('Personnel not found');
        }

        $data['im'] = $person['im'];
        $data['grade'] = $data['grade'] ?? $person['grade'];
        $data['service'] = $data['service'] ?? $person['affectation'];
        $data['nom'] = $data['nom'] ?? $person['lastname'];
        $data['prenoms'] = $data['prenoms'] ?? $person['firstname'];

        $id = ComportementPersonnel::create($data);
        $comportement = ComportementPersonnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Création d'un comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}",
            'new_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        $creatorId = $authUser['sub'] ?? null;
        $admins = \App\Models\Notification::getAdminUsers();
        foreach ($admins as $admin) {
            if ($creatorId && (int) $admin['id'] === (int) $creatorId) {
                continue;
            }
            \App\Models\Notification::create([
                'title' => 'Nouveau comportement',
                'message' => "Comportement {$comportement['type']} enregistré pour {$comportement['nom']} {$comportement['prenoms']} (IM: {$comportement['im']}).",
                'type' => $comportement['type'] === 'Positive' ? 'success' : 'warning',
                'service' => $comportement['service'] ?? 'System',
                'user_id' => $admin['id'],
                'personnel_id' => $comportement['personnel_id'],
                'created_by' => $creatorId,
            ]);
        }

        Response::created($comportement, 'Comportement created successfully');
    }

    /**
     * PUT /api/comportements/{id}
     */
    public function update(array $params): void
    {
        $id = (int) $params['id'];
        $comportement = ComportementPersonnel::getById($id);
        if (!$comportement) {
            Response::notFound('Comportement not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $oldComportement = $comportement;
        ComportementPersonnel::update($id, $data);
        $comportement = ComportementPersonnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Modification d'un comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}",
            'old_values' => $oldComportement,
            'new_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($comportement, 'Comportement updated successfully');
    }

    /**
     * DELETE /api/comportements/{id}
     */
    public function destroy(array $params): void
    {
        $id = (int) $params['id'];
        $comportement = ComportementPersonnel::getById($id);
        if (!$comportement) {
            Response::notFound('Comportement not found');
        }

        ComportementPersonnel::delete($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Suppression d'un comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}",
            'old_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Comportement deleted successfully');
    }
}
