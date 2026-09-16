<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Perquisition;
use App\Models\PerquisitionAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class PerquisitionController
{
    /** Permission module code for the PERQUISITION feature. */
    private const MODULE = 'pj_perquisition';

    /** Notification link prefix for PERQUISITION detail. */
    private const LINK_PREFIX = '/pj/perquisition/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── numero (user-editable; auto-generated if empty on create) ──
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && Perquisition::numeroExists($value, $excludeId)) {
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
     * GET /api/perquisitions
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        if (isset($_GET['search']) && $_GET['search'] !== '') {
            $filters['search'] = $_GET['search'];
        }
        $list = Perquisition::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/perquisitions/next-number
     *
     * Returns the suggested next numero for a perquisition, based on the
     * current PEQ sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(PlainteSequence::PERQUISITION_KEY);
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/perquisitions/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = Perquisition::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Perquisition introuvable');
        }
        $row['attachments'] = PerquisitionAttachment::listForPerquisition((int) $row['id']);

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
     * POST /api/perquisitions
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

        // Use the user-provided numero if non-empty; otherwise auto-generate.
        $userNumero = trim((string) ($data['numero'] ?? ''));
        if ($userNumero !== '') {
            $data['numero'] = $userNumero;
        } else {
            $data['numero'] = PlainteSequence::nextNumber(PlainteSequence::PERQUISITION_KEY);
        }
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['numero_ttr', 'substitut', 'affaire', 'motif'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = Perquisition::create($data);
        $perquisition = Perquisition::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'perquisition',
            'entity_id' => $id,
            'description' => "Création d'une perquisition — N° {$perquisition['numero']}",
            'new_values' => $perquisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $perquisition, (int) $authUser['sub']);

        Response::created($perquisition, 'Perquisition enregistrée avec succès');
    }

    /**
     * PUT /api/perquisitions/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $perquisition = Perquisition::find($id);
        if (!$perquisition) {
            Response::notFound('Perquisition introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // numero: keep existing if not provided; apply user value if non-empty.
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '') {
                $data['numero'] = $value;
            } else {
                $data['numero'] = $perquisition['numero'];
            }
        } else {
            $data['numero'] = $perquisition['numero'];
        }

        // Trim text fields.
        foreach (['numero_ttr', 'substitut', 'affaire', 'motif'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $perquisition;
        Perquisition::update($id, $data);
        $perquisition = Perquisition::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'perquisition',
            'entity_id' => $id,
            'description' => "Modification d'une perquisition — N° {$perquisition['numero']}",
            'old_values' => $oldEntry,
            'new_values' => $perquisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $perquisition, $authUser['sub'] ?? null);

        Response::success($perquisition, 'Perquisition modifiée avec succès');
    }

    /**
     * DELETE /api/perquisitions/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $perquisition = Perquisition::find($id);
        if (!$perquisition) {
            Response::notFound('Perquisition introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/perquisition';
        foreach (PerquisitionAttachment::listForPerquisition($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Perquisition::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'perquisition',
            'entity_id' => $id,
            'description' => "Suppression d'une perquisition — N° {$perquisition['numero']}",
            'old_values' => $perquisition,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Perquisition supprimée avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle perquisition' : 'Perquisition modifiée';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Une perquisition a été {$verb}. N° {$entry['numero']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Une perquisition a été {$verb}. N° {$entry['numero']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
