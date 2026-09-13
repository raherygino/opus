<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\PlainteEntree;
use App\Models\PlainteEntreeAttachment;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;
use App\Models\Personnel;

class PlainteEntreeController
{
    /** Permission module code for the PLAINTE feature. */
    private const MODULE = 'pj_plainte';

    /** Notification link prefix for ENTRÉE detail. */
    private const LINK_PREFIX = '/pj/plainte/entree/';

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid). Type-conditional rules:
     *   ST_PARQUET      — requires numero_st, partie_civile, mise_en_cause, adresse_pc
     *   PLAINTE_DIRECTE — requires partie_civile, mise_en_cause, adresse_pc (no numero_st)
     *   RAPPORT_POLICE  — requires mise_en_cause only (no PC, no adresse, no numero_st)
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── type ──────────────────────────────────────────────────────
        if ($isCreate || array_key_exists('type', $data)) {
            $value = $data['type'] ?? null;
            if (empty($value)) {
                $errors['type'] = 'Le type de plainte est requis';
            } elseif (!in_array($value, PlainteEntree::TYPES, true)) {
                $errors['type'] = 'Le type de plainte est invalide';
            }
        }
        $type = $data['type'] ?? null;
        // On update without type change, look up the current type.
        if (!$isCreate && !in_array($type, PlainteEntree::TYPES, true)) {
            $current = $excludeId ? PlainteEntree::getById($excludeId) : null;
            $type = $current['type'] ?? null;
        }

        // ── date_plainte ─────────────────────────────────────────────
        if ($isCreate || array_key_exists('date_plainte', $data)) {
            $value = $data['date_plainte'] ?? null;
            if (empty($value)) {
                $errors['date_plainte'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_plainte'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        // ── numero_dossier ───────────────────────────────────────────
        // On create, the numero is server-generated; if the client sends
        // one, validate uniqueness. On update, the numero is read-only
        // (never regenerated) — only validate uniqueness if changed.
        if (!$isCreate && array_key_exists('numero_dossier', $data)) {
            $value = trim((string) ($data['numero_dossier'] ?? ''));
            if ($value !== '') {
                $existing = PlainteEntree::getByNumeroDossier($value);
                if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                    $errors['numero_dossier'] = 'Ce numéro de dossier existe déjà';
                }
            }
        }

        // ── numero_st (ST_PARQUET only) ──────────────────────────────
        if ($type === 'ST_PARQUET' && ($isCreate || array_key_exists('numero_st', $data))) {
            if (empty(trim((string) ($data['numero_st'] ?? '')))) {
                $errors['numero_st'] = 'Le numéro du ST est requis pour ce type';
            }
        }

        // ── opj_personnel_id ─────────────────────────────────────────
        if ($isCreate || array_key_exists('opj_personnel_id', $data)) {
            $value = $data['opj_personnel_id'] ?? null;
            if (empty($value)) {
                $errors['opj_personnel_id'] = "L'OPJ est requis";
            } elseif (!Personnel::getById((int) $value)) {
                $errors['opj_personnel_id'] = "L'OPJ sélectionné est introuvable";
            }
        }

        // ── enqueteur_personnel_id ───────────────────────────────────
        if ($isCreate || array_key_exists('enqueteur_personnel_id', $data)) {
            $value = $data['enqueteur_personnel_id'] ?? null;
            if (empty($value)) {
                $errors['enqueteur_personnel_id'] = "L'enquêteur est requis";
            } elseif (!Personnel::getById((int) $value)) {
                $errors['enqueteur_personnel_id'] = "L'enquêteur sélectionné est introuvable";
            }
        }

        // ── partie_civile (ST_PARQUET & PLAINTE_DIRECTE) ─────────────
        if (in_array($type, ['ST_PARQUET', 'PLAINTE_DIRECTE'], true)
            && ($isCreate || array_key_exists('partie_civile', $data))) {
            if (empty(trim((string) ($data['partie_civile'] ?? '')))) {
                $errors['partie_civile'] = 'La partie civile est requise pour ce type';
            }
        }

        // ── mise_en_cause (all types) ────────────────────────────────
        if ($isCreate || array_key_exists('mise_en_cause', $data)) {
            if (empty(trim((string) ($data['mise_en_cause'] ?? '')))) {
                $errors['mise_en_cause'] = 'La mise en cause est requise';
            }
        }

        // ── adresse_pc (ST_PARQUET & PLAINTE_DIRECTE) ────────────────
        if (in_array($type, ['ST_PARQUET', 'PLAINTE_DIRECTE'], true)
            && ($isCreate || array_key_exists('adresse_pc', $data))) {
            if (empty(trim((string) ($data['adresse_pc'] ?? '')))) {
                $errors['adresse_pc'] = "L'adresse du PC est requise pour ce type";
            }
        }

        // ── infraction ──────────────────────────────────────────────
        if ($isCreate || array_key_exists('infraction', $data)) {
            if (empty(trim((string) ($data['infraction'] ?? '')))) {
                $errors['infraction'] = "L'infraction est requise";
            }
        }

        // ── prejudice ───────────────────────────────────────────────
        if ($isCreate || array_key_exists('prejudice', $data)) {
            if (empty(trim((string) ($data['prejudice'] ?? '')))) {
                $errors['prejudice'] = 'Le préjudice est requis';
            }
        }

        // ── lieu_infraction ─────────────────────────────────────────
        if ($isCreate || array_key_exists('lieu_infraction', $data)) {
            if (empty(trim((string) ($data['lieu_infraction'] ?? '')))) {
                $errors['lieu_infraction'] = "Le lieu de l'infraction est requis";
            }
        }

        // ── heure_infraction (optional but validated if present) ────
        if (array_key_exists('heure_infraction', $data) && !empty($data['heure_infraction'])) {
            $value = $data['heure_infraction'];
            if (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_infraction'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        return $errors;
    }

    /**
     * GET /api/plaintes-entree
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['type', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = PlainteEntree::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/plaintes-entree/without-sortie
     */
    public function withoutSortie(array $params): void
    {
        $list = PlainteEntree::getWithoutSortie();
        Response::success($list);
    }

    /**
     * GET /api/plaintes-entree/{id}
     */
    public function show(array $params): void
    {
        $row = PlainteEntree::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Plainte introuvable');
        }
        $row['attachments'] = PlainteEntreeAttachment::getByPlainteEntreeId((int) $row['id']);

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
     * POST /api/plaintes-entree
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

        // Generate the dossier number server-side using the per-type prefix.
        $typeKey = PlainteEntree::TYPE_PREFIXES[$data['type']];
        $data['numero_dossier'] = PlainteSequence::nextNumber($typeKey);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['numero_st', 'partie_civile', 'mise_en_cause', 'adresse_pc', 'infraction', 'prejudice', 'lieu_infraction', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $id = PlainteEntree::create($data);
        $plainte = PlainteEntree::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'plainte_entree',
            'entity_id' => $id,
            'description' => "Création d'une plainte ENTRÉE ({$plainte['type']}) — N° {$plainte['numero_dossier']}",
            'new_values' => $plainte,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with pj_plainte view) ---
        self::notifyChange('create', $plainte, (int) $authUser['sub']);

        Response::created($plainte, 'Plainte enregistrée avec succès');
    }

    /**
     * PUT /api/plaintes-entree/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $plainte = PlainteEntree::getById($id);
        if (!$plainte) {
            Response::notFound('Plainte introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // numero_dossier is never regenerated on update — drop it.
        unset($data['numero_dossier']);

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Trim text fields.
        foreach (['numero_st', 'partie_civile', 'mise_en_cause', 'adresse_pc', 'infraction', 'prejudice', 'lieu_infraction', 'observation'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }

        $oldPlainte = $plainte;
        PlainteEntree::update($id, $data);
        $plainte = PlainteEntree::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'plainte_entree',
            'entity_id' => $id,
            'description' => "Modification d'une plainte ENTRÉE ({$plainte['type']}) — N° {$plainte['numero_dossier']}",
            'old_values' => $oldPlainte,
            'new_values' => $plainte,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification ---
        self::notifyChange('update', $plainte, $authUser['sub'] ?? null);

        Response::success($plainte, 'Plainte modifiée avec succès');
    }

    /**
     * DELETE /api/plaintes-entree/{id}
     * Attachments are removed by the ON DELETE CASCADE FK; files on disk
     * are cleaned up here. The linked SORTIE (if any) is also cascade-deleted.
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $plainte = PlainteEntree::getById($id);
        if (!$plainte) {
            Response::notFound('Plainte introuvable');
        }

        // Remove ENTRÉE attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/plainte-entree';
        foreach (PlainteEntreeAttachment::getByPlainteEntreeId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        // Remove SORTIE attachment files too (cascade will delete the rows).
        $sortieUploadDir = rtrim($config['upload_dir'], '/') . '/plainte-sortie';
        $sortie = \App\Models\PlainteSortie::getByEntreeId($id);
        if ($sortie) {
            foreach (\App\Models\PlainteSortieAttachment::getByPlainteSortieId((int) $sortie['id']) as $attachment) {
                $filePath = $sortieUploadDir . '/' . $attachment['filename'];
                if (file_exists($filePath)) {
                    unlink($filePath);
                }
            }
        }

        PlainteEntree::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'plainte_entree',
            'entity_id' => $id,
            'description' => "Suppression d'une plainte ENTRÉE ({$plainte['type']}) — N° {$plainte['numero_dossier']}",
            'old_values' => $plainte,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Plainte supprimée avec succès');
    }

    /**
     * Notify admins + feature users of a create/update. Push delivery
     * failures are isolated inside Notification::create() and never
     * affect the API response.
     */
    private static function notifyChange(string $action, array $plainte, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $plainte['id'];
        $typeLabel = PlainteEntree::TYPE_LABELS[$plainte['type']] ?? $plainte['type'];
        $date = date('d/m/Y', strtotime($plainte['date_plainte']));

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle plainte ENTRÉE' : 'Plainte ENTRÉE modifiée';

        $adminMessage = "Une plainte ENTRÉE a été {$verb}. Type: {$typeLabel} — N° {$plainte['numero_dossier']} — le {$date}.";
        $userMessage = "Une plainte ENTRÉE a été {$verb}. Type: {$typeLabel} — N° {$plainte['numero_dossier']} — le {$date}. Veuillez en prendre connaissance.";

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
