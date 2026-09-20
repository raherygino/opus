<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RassemblementJournalier;
use App\Models\AuditLog;
use App\Models\Notification;

class RassemblementJournalierController
{
    /** Permission module code for the RASSEMBLEMENT JOURNALIER feature. */
    private const MODULE = 'sg_rassemblement_journalier';

    /** Notification link prefix for RASSEMBLEMENT JOURNALIER detail. */
    private const LINK_PREFIX = '/sg/rassemblement-journalier/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        // ── date_rassemblement ──────────────────────────────────────
        if ($isCreate || array_key_exists('date_rassemblement', $data)) {
            $value = trim((string) ($data['date_rassemblement'] ?? ''));
            if ($value === '') {
                $errors['date_rassemblement'] = 'La date est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) && !strtotime($value)) {
                $errors['date_rassemblement'] = 'La date est invalide';
            }
        }

        // ── heure_rassemblement ─────────────────────────────────────
        if ($isCreate || array_key_exists('heure_rassemblement', $data)) {
            $value = trim((string) ($data['heure_rassemblement'] ?? ''));
            if ($value === '') {
                $errors['heure_rassemblement'] = "L'heure est requise";
            }
        }

        // ── brigade_service ─────────────────────────────────────────
        if ($isCreate || array_key_exists('brigade_service', $data)) {
            if (empty(trim((string) ($data['brigade_service'] ?? '')))) {
                $errors['brigade_service'] = 'La brigade de service est requise';
            }
        }

        // ── situation de prise d'arme ───────────────────────────────
        foreach (['effectif_theorique', 'present', 'absent'] as $f) {
            if ($isCreate || array_key_exists($f, $data)) {
                $value = $data[$f] ?? 0;
                if (!is_numeric($value) || (int) $value < 0) {
                    $errors[$f] = 'La valeur doit être un entier positif ou nul';
                }
            }
        }

        // ── repartitions (Diurne / Nocturne) ────────────────────────
        if (isset($data['repartitions']) && is_array($data['repartitions'])) {
            foreach ($data['repartitions'] as $i => $row) {
                $type = $row['type'] ?? null;
                if ($type !== 'diurne' && $type !== 'nocturne') {
                    $errors["repartitions.{$i}.type"] = "Le type doit être 'diurne' ou 'nocturne'";
                }
            }
        }

        return $errors;
    }

    /**
     * GET /api/rassemblements
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
        $list = RassemblementJournalier::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/rassemblements/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = RassemblementJournalier::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Rassemblement journalier introuvable');
        }
        $row['repartitions'] = RassemblementJournalier::repartitions((int) $row['id']);

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
     * POST /api/rassemblements
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

        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['brigade_service', 'officier_permanence', 'inspecteur_permanence', 'chef_poste', 'instructions_autorite', 'motif_absence'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        if (($data['motif_absence'] ?? null) === '') {
            $data['motif_absence'] = null;
        }

        $id = RassemblementJournalier::create($data);

        // Persist the repartition child table.
        $repartitions = $data['repartitions'] ?? [];
        if (is_array($repartitions)) {
            RassemblementJournalier::replaceRepartitions($id, $repartitions);
        }

        $entry = RassemblementJournalier::find($id);
        $entry['repartitions'] = RassemblementJournalier::repartitions($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'rassemblement_journalier',
            'entity_id' => $id,
            'description' => "Création d'un rassemblement journalier — {$entry['date_rassemblement']} {$entry['heure_rassemblement']}",
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $entry, (int) $authUser['sub']);

        Response::created($entry, 'Rassemblement journalier enregistré avec succès');
    }

    /**
     * PUT /api/rassemblements/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = RassemblementJournalier::find($id);
        if (!$entry) {
            Response::notFound('Rassemblement journalier introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // Keep existing values for fields not provided.
        foreach (['date_rassemblement', 'heure_rassemblement', 'brigade_service', 'officier_permanence', 'inspecteur_permanence', 'chef_poste', 'instructions_autorite', 'effectif_theorique', 'present', 'absent', 'motif_absence'] as $f) {
            if (!array_key_exists($f, $data)) {
                $data[$f] = $entry[$f];
            }
        }

        // Trim text fields.
        foreach (['brigade_service', 'officier_permanence', 'inspecteur_permanence', 'chef_poste', 'instructions_autorite', 'motif_absence'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        if (($data['motif_absence'] ?? null) === '') {
            $data['motif_absence'] = null;
        }

        $oldEntry = $entry;
        RassemblementJournalier::update($id, $data);

        // Replace the repartition child table if provided.
        if (array_key_exists('repartitions', $data) && is_array($data['repartitions'])) {
            RassemblementJournalier::replaceRepartitions($id, $data['repartitions']);
        }

        $entry = RassemblementJournalier::find($id);
        $entry['repartitions'] = RassemblementJournalier::repartitions($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'rassemblement_journalier',
            'entity_id' => $id,
            'description' => "Modification d'un rassemblement journalier — {$entry['date_rassemblement']} {$entry['heure_rassemblement']}",
            'old_values' => $oldEntry,
            'new_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $entry, $authUser['sub'] ?? null);

        Response::success($entry, 'Rassemblement journalier modifié avec succès');
    }

    /**
     * DELETE /api/rassemblements/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $entry = RassemblementJournalier::find($id);
        if (!$entry) {
            Response::notFound('Rassemblement journalier introuvable');
        }

        // Child tables are cascade-deleted by the FK constraint.
        RassemblementJournalier::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'rassemblement_journalier',
            'entity_id' => $id,
            'description' => "Suppression d'un rassemblement journalier — {$entry['date_rassemblement']} {$entry['heure_rassemblement']}",
            'old_values' => $entry,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Rassemblement journalier supprimé avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistré' : 'modifié';
        $title = $action === 'create' ? 'Nouveau rassemblement journalier' : 'Rassemblement journalier modifié';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Un rassemblement journalier a été {$verb}. {$entry['date_rassemblement']} à {$entry['heure_rassemblement']}.",
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Un rassemblement journalier a été {$verb}. {$entry['date_rassemblement']} à {$entry['heure_rassemblement']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'SG',
            'link'    => $link,
        ], $actorId);
    }
}
