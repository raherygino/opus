<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\Arrestation;
use App\Models\ArrestationAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class ArrestationController
{
    /** Permission module code for the ARRESTATION feature. */
    private const MODULE = 'pj_arrestation';

    /** Notification link prefix for ARRESTATION detail. */
    private const LINK_PREFIX = '/pj/arrestation/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── numero (user-editable; auto-generated if empty on create) ──
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && Arrestation::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        }

        // ── date_heure_arrestation (required datetime) ───────────────
        if ($isCreate || array_key_exists('date_heure_arrestation', $data)) {
            $value = trim((string) ($data['date_heure_arrestation'] ?? ''));
            if ($value === '') {
                $errors['date_heure_arrestation'] = "La date et l'heure de l'arrestation sont requises";
            } elseif (!strtotime($value)) {
                $errors['date_heure_arrestation'] = "La date et l'heure de l'arrestation sont invalides";
            }
        }

        // ── personne_nom ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('personne_nom', $data)) {
            if (empty(trim((string) ($data['personne_nom'] ?? '')))) {
                $errors['personne_nom'] = "Le nom et prénom de la personne arrêtée sont requis";
            }
        }

        return $errors;
    }

    /**
     * GET /api/arrestations
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
        $list = Arrestation::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/arrestations/next-number
     *
     * Returns the suggested next numero for an arrestation, based on the
     * current ARR sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(
            PlainteSequence::ARRESTATION_KEY,
            null,
            fn($n) => Arrestation::numeroExists($n)
        );
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/arrestations/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = Arrestation::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Arrestation introuvable');
        }
        $row['attachments'] = ArrestationAttachment::listForArrestation((int) $row['id']);

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
     * POST /api/arrestations
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
        $exists = fn(string $n): bool => Arrestation::numeroExists($n);
        $userNumero = trim((string) ($data['numero'] ?? ''));
        $data['numero'] = ($userNumero !== '' && $userNumero !== PlainteSequence::peekNumber(PlainteSequence::ARRESTATION_KEY, null, $exists))
            ? $userNumero
            : PlainteSequence::nextAvailable(PlainteSequence::ARRESTATION_KEY, $exists);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['personne_nom', 'lieu_arrestation', 'motif', 'policiers', 'numero_dossier', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = Arrestation::create($data);
        $arrestation = Arrestation::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'arrestation',
            'entity_id' => $id,
            'description' => "Création d'une arrestation — N° {$arrestation['numero']}",
            'new_values' => $arrestation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $arrestation, (int) $authUser['sub']);

        Response::created($arrestation, 'Arrestation enregistrée avec succès');
    }

    /**
     * PUT /api/arrestations/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $arrestation = Arrestation::find($id);
        if (!$arrestation) {
            Response::notFound('Arrestation introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // numero: keep existing if not provided; apply user value if non-empty.
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            $data['numero'] = $value !== '' ? $value : $arrestation['numero'];
        } else {
            $data['numero'] = $arrestation['numero'];
        }
        // date_heure_arrestation: keep existing if not provided.
        if (!array_key_exists('date_heure_arrestation', $data) || $data['date_heure_arrestation'] === '' || $data['date_heure_arrestation'] === null) {
            $data['date_heure_arrestation'] = $arrestation['date_heure_arrestation'];
        }
        // personne_nom: keep existing if not provided.
        if (!array_key_exists('personne_nom', $data) || $data['personne_nom'] === '' || $data['personne_nom'] === null) {
            $data['personne_nom'] = $arrestation['personne_nom'];
        }

        // Trim text fields.
        foreach (['personne_nom', 'lieu_arrestation', 'motif', 'policiers', 'numero_dossier', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldEntry = $arrestation;
        Arrestation::update($id, $data);
        $arrestation = Arrestation::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'arrestation',
            'entity_id' => $id,
            'description' => "Modification d'une arrestation — N° {$arrestation['numero']}",
            'old_values' => $oldEntry,
            'new_values' => $arrestation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $arrestation, $authUser['sub'] ?? null);

        Response::success($arrestation, 'Arrestation modifiée avec succès');
    }

    /**
     * DELETE /api/arrestations/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $arrestation = Arrestation::find($id);
        if (!$arrestation) {
            Response::notFound('Arrestation introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/arrestation';
        foreach (ArrestationAttachment::listForArrestation($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        Arrestation::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'arrestation',
            'entity_id' => $id,
            'description' => "Suppression d'une arrestation — N° {$arrestation['numero']}",
            'old_values' => $arrestation,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Arrestation supprimée avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle arrestation' : 'Arrestation modifiée';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Une arrestation a été {$verb}. N° {$entry['numero']} — {$entry['personne_nom']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Une arrestation a été {$verb}. N° {$entry['numero']} — {$entry['personne_nom']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
