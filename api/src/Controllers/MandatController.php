<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Mandat;
use App\Models\MandatAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class MandatController
{
    /** Permission module code for the MANDAT feature. */
    private const MODULE = 'pj_mandat';

    /** Notification link prefix for MANDAT detail. */
    private const LINK_PREFIX = '/pj/mandat/';

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
                $errors['type'] = 'Le type de mandat est requis';
            } elseif (!in_array($value, Mandat::TYPES, true)) {
                $errors['type'] = 'Le type de mandat est invalide';
            }
        }

        // ── numero (user-editable; auto-generated if empty on create) ──
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && Mandat::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        }

        // ── personne_nom ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('personne_nom', $data)) {
            if (empty(trim((string) ($data['personne_nom'] ?? '')))) {
                $errors['personne_nom'] = 'Le nom et prénom de la personne concernée sont requis';
            }
        }

        // ── date_heure_execution (optional datetime) ──────────────────
        if (array_key_exists('date_heure_execution', $data)) {
            $value = trim((string) ($data['date_heure_execution'] ?? ''));
            if ($value !== '' && !strtotime($value)) {
                $errors['date_heure_execution'] = "La date et l'heure d'exécution sont invalides";
            }
        }

        return $errors;
    }

    /**
     * GET /api/mandats
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['type', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        $list = Mandat::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/mandats/next-number
     *
     * Returns the suggested next numero for a mandat, based on the
     * current MAN sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(PlainteSequence::MANDAT_KEY);
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/mandats/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = Mandat::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Mandat introuvable');
        }
        $row['attachments'] = MandatAttachment::listForMandat((int) $row['id']);

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
     * POST /api/mandats
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $raw = file_get_contents('php://input');
        $data = json_decode($raw, true) ?? [];
        file_put_contents(__DIR__ . '/../../_debug_mandat.log', date('c') . " RAW: $raw\n DATA: " . json_encode($data) . "\n", FILE_APPEND);

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            file_put_contents(__DIR__ . '/../../_debug_mandat.log', " ERRORS: " . json_encode($errors) . "\n", FILE_APPEND);
            Response::error('Validation failed', 422, $errors);
        }

        // Use the user-provided numero if non-empty; otherwise auto-generate.
        $userNumero = trim((string) ($data['numero'] ?? ''));
        if ($userNumero !== '') {
            $data['numero'] = $userNumero;
        } else {
            $data['numero'] = PlainteSequence::nextNumber(PlainteSequence::MANDAT_KEY);
        }
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['autorite', 'personne_nom', 'date_lieu_naissance', 'motif', 'qualification_infraction', 'opj_execution', 'lieu_execution', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        // Normalize empty datetime to null.
        if (isset($data['date_heure_execution']) && trim((string) $data['date_heure_execution']) === '') {
            $data['date_heure_execution'] = null;
        }

        $id = Mandat::create($data);
        $mandat = Mandat::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'mandat',
            'entity_id' => $id,
            'description' => "Création d'un mandat ({$mandat['type']}) — N° {$mandat['numero']}",
            'new_values' => $mandat,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $mandat, (int) $authUser['sub']);

        Response::created($mandat, 'Mandat enregistré avec succès');
    }

    /**
     * PUT /api/mandats/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $mandat = Mandat::find($id);
        if (!$mandat) {
            Response::notFound('Mandat introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // numero: keep existing if not provided; apply user value if non-empty.
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            $data['numero'] = $value !== '' ? $value : $mandat['numero'];
        } else {
            $data['numero'] = $mandat['numero'];
        }
        // type: keep existing if not provided.
        if (!array_key_exists('type', $data) || $data['type'] === '' || $data['type'] === null) {
            $data['type'] = $mandat['type'];
        }

        // Trim text fields.
        foreach (['autorite', 'personne_nom', 'date_lieu_naissance', 'motif', 'qualification_infraction', 'opj_execution', 'lieu_execution', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        // Normalize empty datetime to null.
        if (isset($data['date_heure_execution']) && trim((string) $data['date_heure_execution']) === '') {
            $data['date_heure_execution'] = null;
        }

        $oldEntry = $mandat;
        Mandat::update($id, $data);
        $mandat = Mandat::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'mandat',
            'entity_id' => $id,
            'description' => "Modification d'un mandat — N° {$mandat['numero']}",
            'old_values' => $oldEntry,
            'new_values' => $mandat,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $mandat, $authUser['sub'] ?? null);

        Response::success($mandat, 'Mandat modifié avec succès');
    }

    /**
     * DELETE /api/mandats/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $mandat = Mandat::find($id);
        if (!$mandat) {
            Response::notFound('Mandat introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/mandat';
        foreach (MandatAttachment::listForMandat($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Mandat::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'mandat',
            'entity_id' => $id,
            'description' => "Suppression d'un mandat — N° {$mandat['numero']}",
            'old_values' => $mandat,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Mandat supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouveau mandat' : 'Mandat modifié';
        $typeLabel = Mandat::TYPE_LABELS[$entry['type']] ?? $entry['type'];

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un mandat a été {$verb}. {$typeLabel} — N° {$entry['numero']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un mandat a été {$verb}. {$typeLabel} — N° {$entry['numero']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
