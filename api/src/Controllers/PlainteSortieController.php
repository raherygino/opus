<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\PlainteEntree;
use App\Models\PlainteSortie;
use App\Models\PlainteSortieAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class PlainteSortieController
{
    /** Permission module code for the PLAINTE feature. */
    private const MODULE = 'pj_plainte';

    /** Notification link prefix for SORTIE detail. */
    private const LINK_PREFIX = '/pj/plainte/sortie/';

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid). date_deferrement is
     * required only when nature = DEFERREMENT.
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── plainte_entree_id ────────────────────────────────────────
        if ($isCreate || array_key_exists('plainte_entree_id', $data)) {
            $value = $data['plainte_entree_id'] ?? null;
            if (empty($value)) {
                $errors['plainte_entree_id'] = 'La plainte ENTRÉE associée est requise';
            } elseif (!PlainteEntree::getById((int) $value)) {
                $errors['plainte_entree_id'] = 'La plainte ENTRÉE associée est introuvable';
            }
        }

        // ── nature ──────────────────────────────────────────────────
        if ($isCreate || array_key_exists('nature', $data)) {
            $value = $data['nature'] ?? null;
            if (empty($value)) {
                $errors['nature'] = 'La nature est requise';
            } elseif (!in_array($value, PlainteSortie::NATURES, true)) {
                $errors['nature'] = 'La nature est invalide';
            }
        }
        $nature = $data['nature'] ?? null;
        if (!$isCreate && !in_array($nature, PlainteSortie::NATURES, true)) {
            $current = $excludeId ? PlainteSortie::getById($excludeId) : null;
            $nature = $current['nature'] ?? null;
        }

        // ── date_sortie ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('date_sortie', $data)) {
            $value = $data['date_sortie'] ?? null;
            if (empty($value)) {
                $errors['date_sortie'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_sortie'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        // ── numero (user-editable; auto-generated if empty on create) ──
        if ($isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '') {
                $existing = PlainteSortie::getByNumero($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero'] = 'Ce numéro existe déjà';
                }
            }
        } elseif (!$isCreate && array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '') {
                $existing = PlainteSortie::getByNumero($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero'] = 'Ce numéro existe déjà';
                }
            }
        }

        // ── numero_ttr ───────────────────────────────────────────────
        if ($isCreate || array_key_exists('numero_ttr', $data)) {
            if (empty(trim((string) ($data['numero_ttr'] ?? '')))) {
                $errors['numero_ttr'] = 'Le N° TTR est requis';
            }
        }

        // ── nom_substitut ────────────────────────────────────────────
        if ($isCreate || array_key_exists('nom_substitut', $data)) {
            if (empty(trim((string) ($data['nom_substitut'] ?? '')))) {
                $errors['nom_substitut'] = 'Le nom du substitut est requis';
            }
        }

        // ── date_deferrement (DEFERREMENT only) ─────────────────────
        if ($nature === 'DEFERREMENT' && ($isCreate || array_key_exists('date_deferrement', $data))) {
            $value = $data['date_deferrement'] ?? null;
            if (empty($value)) {
                $errors['date_deferrement'] = 'La date du déferrement est requise pour ce type';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_deferrement'] = 'La date du déferrement est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        return $errors;
    }

    /**
     * GET /api/plaintes-sortie
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['nature', 'entree_id', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = PlainteSortie::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/plaintes-sortie/next-number
     *
     * Returns the suggested next sortie number, based on the current
     * sequence counter. The number is not consumed — it is only a preview.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $numero = PlainteSequence::peekNumber('SORTIE');
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/plaintes-sortie/{id}
     */
    public function show(array $params): void
    {
        $row = PlainteSortie::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Sortie introuvable');
        }
        $row['attachments'] = PlainteSortieAttachment::getByPlainteSortieId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user once they
        // actually open the detail.
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
     * POST /api/plaintes-sortie
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

        // Reject if the ENTRÉE already has a SORTIE (one-to-one).
        $existing = PlainteSortie::getByEntreeId((int) $data['plainte_entree_id']);
        if ($existing) {
            Response::error('Validation failed', 422, [
                'plainte_entree_id' => 'Cette plainte ENTRÉE a déjà une sortie associée',
            ]);
        }

        // Use the user-provided numero if non-empty; otherwise auto-generate.
        $userNumero = trim((string) ($data['numero'] ?? ''));
        if ($userNumero !== '') {
            $data['numero'] = $userNumero;
        } else {
            $data['numero'] = PlainteSequence::nextNumber('SORTIE');
        }
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['numero_ttr', 'nom_substitut', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = PlainteSortie::create($data);
        $sortie = PlainteSortie::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'plainte_sortie',
            'entity_id' => $id,
            'description' => "Création d'une sortie de plainte ({$sortie['nature']}) — N° {$sortie['numero']}",
            'new_values' => $sortie,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('create', $sortie, (int) $authUser['sub']);

        Response::created($sortie, 'Sortie enregistrée avec succès');
    }

    /**
     * PUT /api/plaintes-sortie/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $sortie = PlainteSortie::getById($id);
        if (!$sortie) {
            Response::notFound('Sortie introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // numero is never regenerated on update — drop it.
        unset($data['numero']);

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Trim text fields.
        foreach (['numero_ttr', 'nom_substitut', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldSortie = $sortie;
        PlainteSortie::update($id, $data);
        $sortie = PlainteSortie::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'plainte_sortie',
            'entity_id' => $id,
            'description' => "Modification d'une sortie de plainte ({$sortie['nature']}) — N° {$sortie['numero']}",
            'old_values' => $oldSortie,
            'new_values' => $sortie,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('update', $sortie, $authUser['sub'] ?? null);

        Response::success($sortie, 'Sortie modifiée avec succès');
    }

    /**
     * DELETE /api/plaintes-sortie/{id}
     * Attachments are removed by the ON DELETE CASCADE FK; files on disk
     * are cleaned up here. The linked ENTRÉE is NOT deleted.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $sortie = PlainteSortie::getById($id);
        if (!$sortie) {
            Response::notFound('Sortie introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/plainte-sortie';
        foreach (PlainteSortieAttachment::getByPlainteSortieId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        PlainteSortie::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'plainte_sortie',
            'entity_id' => $id,
            'description' => "Suppression d'une sortie de plainte ({$sortie['nature']}) — N° {$sortie['numero']}",
            'old_values' => $sortie,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Sortie supprimée avec succès');
    }

    /**
     * Notify admins + feature users of a create/update. Push delivery
     * failures are isolated inside Notification::create() and never
     * affect the API response.
     */
    private static function notifyChange(string $action, array $sortie, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $sortie['id'];
        $natureLabel = PlainteSortie::NATURE_LABELS[$sortie['nature']] ?? $sortie['nature'];
        $date = date('d/m/Y', strtotime($sortie['date_sortie']));

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle sortie de plainte' : 'Sortie de plainte modifiée';

        $adminMessage = "Une sortie de plainte a été {$verb}. Nature: {$natureLabel} — N° {$sortie['numero']} — le {$date}.";
        $userMessage = "Une sortie de plainte a été {$verb}. Nature: {$natureLabel} — N° {$sortie['numero']} — le {$date}. Veuillez en prendre connaissance.";

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
