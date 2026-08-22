<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\ComportementPersonnel;
use App\Models\Personnel;
use App\Models\AuditLog;

class ComportementController
{
    /**
     * Whether the authenticated user is an administrator who can bypass
     * the confirmation workflow (records they create are auto-confirmed).
     */
    private static function isAdmin(?array $authUser): bool
    {
        if (!$authUser) return false;
        $roleCode = $authUser['role_code'] ?? null;
        return $roleCode === 'SUPER_ADMIN' || $roleCode === 'STATION_ADMIN';
    }

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
        if (isset($_GET['status'])) {
            $filters['status'] = $_GET['status'];
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

        // --- Confirmation workflow ---
        $authUser = AuthController::getAuthenticatedUser();
        $isAdmin = self::isAdmin($authUser);
        $creatorId = $authUser['sub'] ?? null;

        // Track who created the record so they can be notified on confirm/reject.
        $data['created_by'] = $creatorId;

        if ($isAdmin) {
            // Administrators self-validate: the record is immediately confirmed.
            $data['status'] = 'confirmed';
            $data['confirmed_by'] = $creatorId;
            $data['confirmed_at'] = date('Y-m-d H:i:s');
        } else {
            // Staff records require administrator confirmation.
            $data['status'] = 'pending';
        }

        $id = ComportementPersonnel::create($data);
        $comportement = ComportementPersonnel::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Création d'un comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}" . ($isAdmin ? ' (auto-confirmé)' : ' (en attente de confirmation)'),
            'new_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        // Only notify admins when a non-admin creates a record that requires
        // confirmation. When an administrator creates the record themselves,
        // no notification is sent (they are already the validator).
        if (!$isAdmin) {
            $admins = \App\Models\Notification::getAdminUsers();
            foreach ($admins as $admin) {
                if ($creatorId && (int) $admin['id'] === (int) $creatorId) {
                    continue;
                }
                \App\Models\Notification::create([
                    'title' => 'Comportement à confirmer',
                    'message' => "Comportement {$comportement['type']} en attente de confirmation pour {$comportement['nom']} {$comportement['prenoms']} (IM: {$comportement['im']}).",
                    'type' => 'warning',
                    'service' => $comportement['service'] ?? 'System',
                    'user_id' => $admin['id'],
                    'personnel_id' => $comportement['personnel_id'],
                    'created_by' => $creatorId,
                ]);
            }
        }

        $message = $isAdmin
            ? 'Comportement créé et confirmé avec succès'
            : 'Comportement enregistré. En attente de confirmation par un administrateur.';
        Response::created($comportement, $message);
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
     * PUT /api/comportements/{id}/confirm
     * Administrator confirms a pending comportement record.
     */
    public function confirm(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!self::isAdmin($authUser)) {
            Response::error('Seuls les administrateurs peuvent confirmer un comportement', 403);
        }

        $id = (int) $params['id'];
        $comportement = ComportementPersonnel::getById($id);
        if (!$comportement) {
            Response::notFound('Comportement not found');
        }

        $oldComportement = $comportement;
        ComportementPersonnel::confirm($id, (int) $authUser['sub']);
        $comportement = ComportementPersonnel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'],
            'action' => 'confirm',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Confirmation du comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}",
            'old_values' => $oldComportement,
            'new_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify the creator that their record was confirmed ---
        $this->notifyCreator($comportement, 'confirmed', null, (int) $authUser['sub']);

        Response::success($comportement, 'Comportement confirmé avec succès');
    }

    /**
     * PUT /api/comportements/{id}/reject
     * Administrator rejects a pending comportement record.
     * Optional body: { "reason": "..." }
     */
    public function reject(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!self::isAdmin($authUser)) {
            Response::error('Seuls les administrateurs peuvent rejeter un comportement', 403);
        }

        $id = (int) $params['id'];
        $comportement = ComportementPersonnel::getById($id);
        if (!$comportement) {
            Response::notFound('Comportement not found');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];
        $reason = !empty($data['reason']) ? trim($data['reason']) : null;

        $oldComportement = $comportement;
        ComportementPersonnel::reject($id, (int) $authUser['sub'], $reason);
        $comportement = ComportementPersonnel::getById($id);

        AuditLog::create([
            'user_id' => $authUser['sub'],
            'action' => 'reject',
            'module' => 'comportements',
            'entity_id' => $id,
            'description' => "Rejet du comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']}" . ($reason ? " — Raison: $reason" : ''),
            'old_values' => $oldComportement,
            'new_values' => $comportement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notify the creator that their record was rejected ---
        $this->notifyCreator($comportement, 'rejected', $reason, (int) $authUser['sub']);

        Response::success($comportement, 'Comportement rejeté');
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

    /**
     * Notify the staff member who created a comportement record that an
     * administrator has confirmed or rejected it. No notification is sent
     * when the creator is the same user performing the action (admin
     * self-confirmed at creation time) or when no creator is recorded.
     *
     * @param array  $comportement  The updated comportement row.
     * @param string $action        "confirmed" or "rejected".
     * @param string|null $reason   Rejection reason, if any.
     * @param int    $actorId       The administrator performing the action.
     */
    private function notifyCreator(array $comportement, string $action, ?string $reason, int $actorId): void
    {
        $creatorId = $comportement['created_by'] ?? null;
        if (!$creatorId || (int) $creatorId === $actorId) {
            return;
        }

        if ($action === 'confirmed') {
            $title = 'Comportement confirmé';
            $message = "Votre comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']} (IM: {$comportement['im']}) a été confirmé par un administrateur.";
            $type = 'success';
        } else {
            $title = 'Comportement rejeté';
            $message = "Votre comportement {$comportement['type']} pour {$comportement['nom']} {$comportement['prenoms']} (IM: {$comportement['im']}) a été rejeté par un administrateur.";
            if ($reason) {
                $message .= " Raison: $reason";
            }
            $type = 'error';
        }

        \App\Models\Notification::create([
            'title' => $title,
            'message' => $message,
            'type' => $type,
            'service' => $comportement['service'] ?? 'System',
            'user_id' => (int) $creatorId,
            'personnel_id' => $comportement['personnel_id'],
            'created_by' => $actorId,
        ]);
    }
}
