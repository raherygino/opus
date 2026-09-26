<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RegistreDeferrement;
use App\Models\RegistreDeferrementAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class RegistreDeferrementController
{
    /** Permission module code for the REGISTRE DE DÉFERREMENT feature. */
    private const MODULE = 'pj_deferrement';

    /** Notification link prefix for the registre de déferrement detail. */
    private const LINK_PREFIX = '/pj/registre-deferrement/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── numero (user-editable; auto-generated if empty on create) ──
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && RegistreDeferrement::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        }

        // ── date_heure_deferrement (required datetime) ────────────────
        if ($isCreate || array_key_exists('date_heure_deferrement', $data)) {
            $value = trim((string) ($data['date_heure_deferrement'] ?? ''));
            if ($value === '') {
                $errors['date_heure_deferrement'] = 'La date et l\'heure du déferrement sont requises';
            } elseif (!strtotime($value)) {
                $errors['date_heure_deferrement'] = 'La date et l\'heure du déferrement sont invalides';
            }
        }

        // ── personne_nom ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('personne_nom', $data)) {
            if (empty(trim((string) ($data['personne_nom'] ?? '')))) {
                $errors['personne_nom'] = 'Le nom et prénom de la personne déférée sont requis';
            }
        }

        return $errors;
    }

    /**
     * GET /api/registres-deferrement
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        $list = RegistreDeferrement::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/registres-deferrement/next-number
     *
     * Returns the suggested next numero for a déferrement entry, based on
     * the current DEF sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(
            PlainteSequence::DEFERREMENT_KEY,
            null,
            fn($n) => RegistreDeferrement::numeroExists($n)
        );
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/registres-deferrement/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = RegistreDeferrement::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Déferrement introuvable');
        }
        $row['attachments'] = RegistreDeferrementAttachment::listForDeferrement((int) $row['id']);

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
     * POST /api/registres-deferrement
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
        $exists = fn(string $n): bool => RegistreDeferrement::numeroExists($n);
        $userNumero = trim((string) ($data['numero'] ?? ''));
        $data['numero'] = ($userNumero !== '' && $userNumero !== PlainteSequence::peekNumber(PlainteSequence::DEFERREMENT_KEY, null, $exists))
            ? $userNumero
            : PlainteSequence::nextAvailable(PlainteSequence::DEFERREMENT_KEY, $exists);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['personne_nom', 'date_lieu_naissance', 'infraction', 'numero_dossier', 'autorite', 'destination', 'escorte', 'suite_donnee', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = RegistreDeferrement::create($data);
        $deferrement = RegistreDeferrement::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'registre_deferrement',
            'entity_id' => $id,
            'description' => "Création d'une entrée au registre de déferrement — N° {$deferrement['numero']}",
            'new_values' => $deferrement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $deferrement, (int) $authUser['sub']);

        Response::created($deferrement, 'Déferrement enregistré avec succès');
    }

    /**
     * PUT /api/registres-deferrement/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $deferrement = RegistreDeferrement::find($id);
        if (!$deferrement) {
            Response::notFound('Déferrement introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // numero: keep existing if not provided; apply user value if non-empty.
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            $data['numero'] = $value !== '' ? $value : $deferrement['numero'];
        } else {
            $data['numero'] = $deferrement['numero'];
        }

        // Trim text fields.
        foreach (['personne_nom', 'date_lieu_naissance', 'infraction', 'numero_dossier', 'autorite', 'destination', 'escorte', 'suite_donnee', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $deferrement;
        RegistreDeferrement::update($id, $data);
        $deferrement = RegistreDeferrement::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'registre_deferrement',
            'entity_id' => $id,
            'description' => "Modification d'une entrée au registre de déferrement — N° {$deferrement['numero']}",
            'old_values' => $oldEntry,
            'new_values' => $deferrement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $deferrement, $authUser['sub'] ?? null);

        Response::success($deferrement, 'Déferrement modifié avec succès');
    }

    /**
     * DELETE /api/registres-deferrement/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $deferrement = RegistreDeferrement::find($id);
        if (!$deferrement) {
            Response::notFound('Déferrement introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/registre_deferrement';
        foreach (RegistreDeferrementAttachment::listForDeferrement($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        RegistreDeferrement::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'registre_deferrement',
            'entity_id' => $id,
            'description' => "Suppression d'une entrée du registre de déferrement — N° {$deferrement['numero']}",
            'old_values' => $deferrement,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Déferrement supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouveau déferrement' : 'Déferrement modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Une entrée au registre de déferrement a été {$verb}. N° {$entry['numero']} — {$entry['personne_nom']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Une entrée au registre de déferrement a été {$verb}. N° {$entry['numero']} — {$entry['personne_nom']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
