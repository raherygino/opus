<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Convocation;
use App\Models\ConvocationAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class ConvocationController
{
    /** Permission module code for the CONVOCATION feature. */
    private const MODULE = 'pj_convocation';

    /** Notification link prefix for CONVOCATION detail. */
    private const LINK_PREFIX = '/pj/convocation/';

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid). Both types share the
     * same fields; only the numero format differs (handled by sequence).
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── type ──────────────────────────────────────────────────────
        if ($isCreate || array_key_exists('type', $data)) {
            $value = $data['type'] ?? null;
            if (empty($value)) {
                $errors['type'] = 'Le type de convocation est requis';
            } elseif (!in_array($value, Convocation::TYPES, true)) {
                $errors['type'] = 'Le type de convocation est invalide';
            }
        }

        // ── date_convocation ─────────────────────────────────────────
        if ($isCreate || array_key_exists('date_convocation', $data)) {
            $value = $data['date_convocation'] ?? null;
            if (empty($value)) {
                $errors['date_convocation'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_convocation'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        // ── numero (user-editable; auto-generated if empty on create) ──
        if ($isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '') {
                $existing = Convocation::getByNumero($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero'] = 'Ce numéro existe déjà';
                }
            }
        } elseif (!$isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '') {
                $existing = Convocation::getByNumero($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero'] = 'Ce numéro existe déjà';
                }
            }
        }

        // ── nom ──────────────────────────────────────────────────────
        if ($isCreate || array_key_exists('nom', $data)) {
            if (empty(trim((string) ($data['nom'] ?? '')))) {
                $errors['nom'] = 'Le nom est requis';
            }
        }

        // ── infraction ───────────────────────────────────────────────
        if ($isCreate || array_key_exists('infraction', $data)) {
            if (empty(trim((string) ($data['infraction'] ?? '')))) {
                $errors['infraction'] = "L'infraction est requise";
            }
        }

        return $errors;
    }

    /**
     * GET /api/convocations
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['type', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = Convocation::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/convocations/next-number?type=ST_PARQUET
     *
     * Returns the suggested next numero for the given type, based
     * on the current sequence counter. The number is not consumed —
     * it is only a preview. The actual counter is incremented on save.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $type = $_GET['type'] ?? null;
        if (!in_array($type, Convocation::TYPES, true)) {
            Response::error('Type de convocation invalide', 422, [
                'type' => 'Le type de convocation est requis et doit être valide',
            ]);
        }

        $typeKey = Convocation::TYPE_PREFIXES[$type];
        $numero = PlainteSequence::peekNumber($typeKey);
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/convocations/{id}
     */
    public function show(array $params): void
    {
        $row = Convocation::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Convocation introuvable');
        }
        $row['attachments'] = ConvocationAttachment::getByConvocationId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            Notification::markAsReadByLink(
                self::LINK_PREFIX . $row['id'],
                (int) $authUser['sub']
            );
        }

        Response::success($row);
    }

    /**
     * POST /api/convocations
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
        $typeKey = Convocation::TYPE_PREFIXES[$data['type']];
        $userNumero = trim((string) ($data['numero'] ?? ''));
        if ($userNumero !== '') {
            $data['numero'] = $userNumero;
        } else {
            $data['numero'] = PlainteSequence::nextNumber($typeKey);
        }
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['nom', 'adresse', 'infraction', 'personne_accuse_recu', 'numero_dossier', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = Convocation::create($data);
        $convocation = Convocation::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'convocation',
            'entity_id' => $id,
            'description' => "Création d'une convocation ({$convocation['type']}) — N° {$convocation['numero']}",
            'new_values' => $convocation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('create', $convocation, (int) $authUser['sub']);

        Response::created($convocation, 'Convocation enregistrée avec succès');
    }

    /**
     * PUT /api/convocations/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $convocation = Convocation::getById($id);
        if (!$convocation) {
            Response::notFound('Convocation introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // numero is never regenerated on update — drop it.
        unset($data['numero']);

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Trim text fields.
        foreach (['nom', 'adresse', 'infraction', 'personne_accuse_recu', 'numero_dossier', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldConvocation = $convocation;
        Convocation::update($id, $data);
        $convocation = Convocation::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'convocation',
            'entity_id' => $id,
            'description' => "Modification d'une convocation ({$convocation['type']}) — N° {$convocation['numero']}",
            'old_values' => $oldConvocation,
            'new_values' => $convocation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('update', $convocation, $authUser['sub'] ?? null);

        Response::success($convocation, 'Convocation modifiée avec succès');
    }

    /**
     * DELETE /api/convocations/{id}
     * Attachments are removed by the ON DELETE CASCADE FK; files on disk
     * are cleaned up here.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $convocation = Convocation::getById($id);
        if (!$convocation) {
            Response::notFound('Convocation introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/convocation';
        foreach (ConvocationAttachment::getByConvocationId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Convocation::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'convocation',
            'entity_id' => $id,
            'description' => "Suppression d'une convocation ({$convocation['type']}) — N° {$convocation['numero']}",
            'old_values' => $convocation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Convocation supprimée avec succès');
    }

    /**
     * Notify admins + feature users of a create/update.
     */
    private static function notifyChange(string $action, array $convocation, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $convocation['id'];
        $typeLabel = Convocation::TYPE_LABELS[$convocation['type']] ?? $convocation['type'];
        $date = date('d/m/Y', strtotime($convocation['date_convocation']));

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle convocation' : 'Convocation modifiée';

        $adminMessage = "Une convocation a été {$verb}. Type: {$typeLabel} — N° {$convocation['numero']} — le {$date}.";
        $userMessage = "Une convocation a été {$verb}. Type: {$typeLabel} — N° {$convocation['numero']} — le {$date}. Veuillez en prendre connaissance.";

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
