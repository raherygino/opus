<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\MouvementAttachment;
use App\Models\MouvementPersonnel;
use App\Models\Personnel;
use App\Models\AuditLog;

class MouvementController
{
    /**
     * GET /api/mouvements
     */
    public function index(array $params): void
    {
        $filters = [];
        if (isset($_GET['personnel_id'])) {
            $filters['personnel_id'] = $_GET['personnel_id'];
        }
        if (isset($_GET['type_mouvement'])) {
            $filters['type_mouvement'] = $_GET['type_mouvement'];
        }
        if (isset($_GET['search'])) {
            $filters['search'] = $_GET['search'];
        }

        $list = MouvementPersonnel::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/mouvements/{id}
     */
    public function show(array $params): void
    {
        $mouvement = MouvementPersonnel::getById((int) $params['id']);
        if (!$mouvement) {
            Response::notFound('Mouvement not found');
        }
        Response::success($mouvement);
    }

    /**
     * POST /api/mouvements
     */
    public function store(array $params): void
    {
        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        if (empty($data['personnel_id'])) {
            Response::error('personnel_id is required', 422, ['personnel_id' => "L'identifiant du personnel est requis"]);
        }
        if (empty($data['type_mouvement'])) {
            Response::error('type_mouvement is required', 422, ['type_mouvement' => 'Le type de mouvement est requis']);
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

        // Compute date_retour from date_depart + days if not provided
        if (empty($data['date_retour']) && !empty($data['date_depart']) && isset($data['days'])) {
            $data['date_retour'] = date('Y-m-d', strtotime($data['date_depart'] . ' + ' . (int) $data['days'] . ' days'));
        }

        $id = MouvementPersonnel::create($data);
        $mouvement = MouvementPersonnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'mouvements',
            'entity_id' => $id,
            'description' => "Création d'un mouvement de type '{$mouvement['type_mouvement']}' pour {$mouvement['nom']} {$mouvement['prenoms']}",
            'new_values' => $mouvement,
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
                'title' => 'Nouveau mouvement',
                'message' => "Mouvement de type '{$mouvement['type_mouvement']}' enregistré pour {$mouvement['nom']} {$mouvement['prenoms']} (IM: {$mouvement['im']}).",
                'type' => 'info',
                'service' => $mouvement['service'] ?? 'System',
                'user_id' => $admin['id'],
                'personnel_id' => $mouvement['personnel_id'],
                'created_by' => $creatorId,
            ]);
        }

        Response::created($mouvement, 'Mouvement created successfully');
    }

    /**
     * PUT /api/mouvements/{id}
     */
    public function update(array $params): void
    {
        $id = (int) $params['id'];
        $mouvement = MouvementPersonnel::getById($id);
        if (!$mouvement) {
            Response::notFound('Mouvement not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Recompute date_retour if date_depart or days changed and date_retour not explicitly set
        if (!isset($data['date_retour'])) {
            $dateDepart = $data['date_depart'] ?? $mouvement['date_depart'];
            $days = isset($data['days']) ? (int) $data['days'] : $mouvement['days'];
            if ($dateDepart && $days !== null) {
                $data['date_retour'] = date('Y-m-d', strtotime($dateDepart . ' + ' . $days . ' days'));
            }
        }

        $oldMouvement = $mouvement;
        MouvementPersonnel::update($id, $data);
        $mouvement = MouvementPersonnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'mouvements',
            'entity_id' => $id,
            'description' => "Modification d'un mouvement de type '{$mouvement['type_mouvement']}' pour {$mouvement['nom']} {$mouvement['prenoms']}",
            'old_values' => $oldMouvement,
            'new_values' => $mouvement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success($mouvement, 'Mouvement updated successfully');
    }

    /**
     * PUT /api/mouvements/{id}/retour
     * Toggle retour to "Oui"
     */
    public function retour(array $params): void
    {
        $id = (int) $params['id'];
        $mouvement = MouvementPersonnel::getById($id);
        if (!$mouvement) {
            Response::notFound('Mouvement not found');
        }

        MouvementPersonnel::update($id, ['retour' => 'Oui']);
        $mouvement = MouvementPersonnel::getById($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'retour',
            'module' => 'mouvements',
            'entity_id' => $id,
            'description' => "Retour enregistré pour le mouvement de {$mouvement['nom']} {$mouvement['prenoms']}",
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
                'title' => 'Retour de mouvement',
                'message' => "Retour enregistré pour {$mouvement['nom']} {$mouvement['prenoms']} (IM: {$mouvement['im']}) — Type: {$mouvement['type_mouvement']}.",
                'type' => 'info',
                'service' => $mouvement['service'] ?? 'System',
                'user_id' => $admin['id'],
                'personnel_id' => $mouvement['personnel_id'],
                'created_by' => $creatorId,
            ]);
        }

        Response::success($mouvement, 'Retour enregistré');
    }

    /**
     * DELETE /api/mouvements/{id}
     */
    public function destroy(array $params): void
    {
        $id = (int) $params['id'];
        $mouvement = MouvementPersonnel::getById($id);
        if (!$mouvement) {
            Response::notFound('Mouvement not found');
        }

        // Delete attachment files from disk
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/mouvements';
        $attachments = MouvementAttachment::getByMouvementId($id);
        foreach ($attachments as $a) {
            $filePath = $uploadDir . '/' . $a['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        MouvementPersonnel::delete($id);

        // --- Audit log ---
        $authUser = AuthController::getAuthenticatedUser();
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'mouvements',
            'entity_id' => $id,
            'description' => "Suppression d'un mouvement de type '{$mouvement['type_mouvement']}' pour {$mouvement['nom']} {$mouvement['prenoms']}",
            'old_values' => $mouvement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Mouvement deleted successfully');
    }
}
