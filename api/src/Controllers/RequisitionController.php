<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Requisition;
use App\Models\RequisitionAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class RequisitionController
{
    /** Permission module code for the REQUISITION feature. */
    private const MODULE = 'pj_requisition';

    /** Notification link prefix for REQUISITION detail. */
    private const LINK_PREFIX = '/pj/requisition/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── type ──────────────────────────────────────────────────────
        if ($isCreate || array_key_exists('type', $data)) {
            $value = $data['type'] ?? null;
            if (empty($value)) {
                $errors['type'] = 'Le type de réquisition est requis';
            } elseif (!in_array($value, Requisition::TYPES, true)) {
                $errors['type'] = 'Le type de réquisition est invalide';
            }
        }

        // ── date_requisition ──────────────────────────────────────────
        if ($isCreate || array_key_exists('date_requisition', $data)) {
            $value = $data['date_requisition'] ?? null;
            if (empty($value)) {
                $errors['date_requisition'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_requisition'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        // ── numero (user-editable; auto-generated if empty on create) ──
        if ($isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && Requisition::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        } elseif (!$isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && Requisition::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        }

        // ── affaire ──────────────────────────────────────────────────
        if ($isCreate || array_key_exists('affaire', $data)) {
            if (empty(trim((string) ($data['affaire'] ?? '')))) {
                $errors['affaire'] = "L'affaire est requise";
            }
        }

        return $errors;
    }

    /**
     * GET /api/requisitions
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['type', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        $list = Requisition::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/requisitions/next-number
     *
     * Returns the suggested next numero for a requisition, based on the
     * current REQ sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(
            PlainteSequence::REQUISITION_KEY,
            null,
            fn($n) => Requisition::numeroExists($n)
        );
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/requisitions/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = Requisition::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Réquisition introuvable');
        }
        $row['attachments'] = RequisitionAttachment::listForRequisition((int) $row['id']);

        // Auto-dismiss the notification for the viewing user.
        if (!empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                self::LINK_PREFIX . $row['id'],
                (int) $authUser['sub']
            );
        }
        Response::success($row);
    }

    /**
     * POST /api/requisitions
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

        // When the client submits the current suggestion (or nothing), the
        // sequence is consumed — skipping numbers already in use — so the
        // counter advances and consecutive creates never collide.
        $exists = fn(string $n): bool => Requisition::numeroExists($n);
        $userNumero = trim((string) ($data['numero'] ?? ''));
        $data['numero'] = ($userNumero !== '' && $userNumero !== PlainteSequence::peekNumber(PlainteSequence::REQUISITION_KEY, null, $exists))
            ? $userNumero
            : PlainteSequence::nextAvailable(PlainteSequence::REQUISITION_KEY, $exists);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['numero_ttr', 'nom_substitut', 'affaire', 'numero_dossier', 'opj'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = Requisition::create($data);
        $requisition = Requisition::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'requisition',
            'entity_id' => $id,
            'description' => "Création d'une réquisition ({$requisition['type']}) — N° {$requisition['numero']}",
            'new_values' => $requisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $requisition, (int) $authUser['sub']);

        Response::created($requisition, 'Réquisition enregistrée avec succès');
    }

    /**
     * PUT /api/requisitions/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $requisition = Requisition::find($id);
        if (!$requisition) {
            Response::notFound('Réquisition introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // numero is never regenerated on update — drop it.
        unset($data['numero']);

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        foreach (['numero_ttr', 'nom_substitut', 'affaire', 'numero_dossier', 'opj'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldRequisition = $requisition;
        Requisition::update($id, $data);
        $requisition = Requisition::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'requisition',
            'entity_id' => $id,
            'description' => "Modification d'une réquisition ({$requisition['type']}) — N° {$requisition['numero']}",
            'old_values' => $oldRequisition,
            'new_values' => $requisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $requisition, $authUser['sub'] ?? null);

        Response::success($requisition, 'Réquisition modifiée avec succès');
    }

    /**
     * DELETE /api/requisitions/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $requisition = Requisition::find($id);
        if (!$requisition) {
            Response::notFound('Réquisition introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/requisition';
        foreach (RequisitionAttachment::listForRequisition($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Requisition::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'requisition',
            'entity_id' => $id,
            'description' => "Suppression d'une réquisition ({$requisition['type']}) — N° {$requisition['numero']}",
            'old_values' => $requisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Réquisition supprimée avec succès');
    }

    private static function notifyChange(string $action, array $requisition, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $requisition['id'];
        $typeLabel = Requisition::TYPE_LABELS[$requisition['type']] ?? $requisition['type'];
        $date = date('d/m/Y', strtotime($requisition['date_requisition']));

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle réquisition' : 'Réquisition modifiée';

        $adminMessage = "Une réquisition a été {$verb}. Type: {$typeLabel} — N° {$requisition['numero']} — le {$date}.";
        $userMessage = "Une réquisition a été {$verb}. Type: {$typeLabel} — N° {$requisition['numero']} — le {$date}. Veuillez en prendre connaissance.";

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => $adminMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $userMessage,
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
