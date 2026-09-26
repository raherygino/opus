<?php
declare(strict_types=1);

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\RegistreEnquete;
use App\Models\RegistreEnqueteAttachment;
use App\Models\Personnel;
use App\Models\PlainteSequence;
use App\Models\AuditLog;
use App\Models\Notification;

class RegistreEnqueteController
{
    /** Permission module code for the REGISTRE D'ENQUÊTE feature. */
    private const MODULE = 'pj_enquete';

    /** Notification link prefix for the registre d'enquête detail. */
    private const LINK_PREFIX = '/pj/registre-enquete/';

    /**
     * Validation shared by store() and update().
     */
    private static function validate(array $data, bool $isCreate, ?int $excludeId = null): array
    {
        $errors = [];

        // ── numero (user-editable; auto-generated if empty on create) ──
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            if ($value !== '' && RegistreEnquete::numeroExists($value, $excludeId)) {
                $errors['numero'] = 'Ce numéro existe déjà';
            }
        }

        // ── date_ouverture (required date) ────────────────────────────
        if ($isCreate || array_key_exists('date_ouverture', $data)) {
            $value = trim((string) ($data['date_ouverture'] ?? ''));
            if ($value === '') {
                $errors['date_ouverture'] = "La date d'ouverture est requise";
            } elseif (!strtotime($value)) {
                $errors['date_ouverture'] = "La date d'ouverture est invalide";
            }
        }

        // ── nature_infraction ─────────────────────────────────────────
        if ($isCreate || array_key_exists('nature_infraction', $data)) {
            if (empty(trim((string) ($data['nature_infraction'] ?? '')))) {
                $errors['nature_infraction'] = "La nature de l'infraction est requise";
            }
        }

        // ── enqueteur_personnel_id ────────────────────────────────────
        if ($isCreate || array_key_exists('enqueteur_personnel_id', $data)) {
            $value = $data['enqueteur_personnel_id'] ?? null;
            if (empty($value)) {
                $errors['enqueteur_personnel_id'] = "L'enquêteur est requis";
            } elseif (!Personnel::getById((int) $value)) {
                $errors['enqueteur_personnel_id'] = "L'enquêteur sélectionné est introuvable";
            }
        }

        // ── opj_personnel_id (optional; must exist when provided) ─────
        if (array_key_exists('opj_personnel_id', $data) && !empty($data['opj_personnel_id'])) {
            if (!Personnel::getById((int) $data['opj_personnel_id'])) {
                $errors['opj_personnel_id'] = "L'OPJ sélectionné est introuvable";
            }
        }

        // ── statut ────────────────────────────────────────────────────
        if (array_key_exists('statut', $data) && !empty($data['statut'])) {
            if (!in_array($data['statut'], RegistreEnquete::STATUTS, true)) {
                $errors['statut'] = "Le statut est invalide";
            }
        }

        return $errors;
    }

    /**
     * GET /api/registres-enquete
     */
    public function index(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $filters = [];
        foreach (['search', 'statut'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }
        $list = RegistreEnquete::all($filters);
        Response::success($list);
    }

    /**
     * GET /api/registres-enquete/next-number
     *
     * Returns the suggested next numero for an enquête entry, based on the
     * current ENQ sequence counter. Not consumed — preview only.
     */
    public function nextNumber(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $numero = PlainteSequence::peekNumber(
            PlainteSequence::ENQUETE_KEY,
            null,
            fn($n) => RegistreEnquete::numeroExists($n)
        );
        Response::success(['numero' => $numero]);
    }

    /**
     * GET /api/registres-enquete/{id}
     */
    public function show(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }
        $row = RegistreEnquete::find((int) $params['id']);
        if (!$row) {
            Response::notFound('Enquête introuvable');
        }
        $row['attachments'] = RegistreEnqueteAttachment::listForEnquete((int) $row['id']);

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
     * POST /api/registres-enquete
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
        $exists = fn(string $n): bool => RegistreEnquete::numeroExists($n);
        $userNumero = trim((string) ($data['numero'] ?? ''));
        $data['numero'] = ($userNumero !== '' && $userNumero !== PlainteSequence::peekNumber(PlainteSequence::ENQUETE_KEY, null, $exists))
            ? $userNumero
            : PlainteSequence::nextAvailable(PlainteSequence::ENQUETE_KEY, $exists);
        $data['created_by'] = $authUser['sub'] ?? null;

        // Trim text fields.
        foreach (['numero_dossier', 'nature_infraction', 'date_lieu_faits', 'plaignant', 'mise_en_cause', 'statut', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        if (empty($data['statut'])) {
            $data['statut'] = 'EN_COURS';
        }

        $id = RegistreEnquete::create($data);
        $enquete = RegistreEnquete::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'registre_enquete',
            'entity_id' => $id,
            'description' => "Création d'une entrée au registre d'enquête — N° {$enquete['numero']}",
            'new_values' => $enquete,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('create', $enquete, (int) $authUser['sub']);

        Response::created($enquete, 'Enquête enregistrée avec succès');
    }

    /**
     * PUT /api/registres-enquete/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $enquete = RegistreEnquete::find($id);
        if (!$enquete) {
            Response::notFound('Enquête introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        $errors = self::validate($data, false, $id);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        // numero: keep existing if not provided; apply user value if non-empty.
        if (array_key_exists('numero', $data)) {
            $value = trim((string) ($data['numero'] ?? ''));
            $data['numero'] = $value !== '' ? $value : $enquete['numero'];
        } else {
            $data['numero'] = $enquete['numero'];
        }

        // Trim text fields.
        foreach (['numero_dossier', 'nature_infraction', 'date_lieu_faits', 'plaignant', 'mise_en_cause', 'statut', 'observations'] as $f) {
            if (isset($data[$f]) && is_string($data[$f])) {
                $data[$f] = trim($data[$f]);
            }
        }
        if (empty($data['statut'])) {
            $data['statut'] = $enquete['statut'];
        }

        $oldEntry = $enquete;
        RegistreEnquete::update($id, $data);
        $enquete = RegistreEnquete::find($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'registre_enquete',
            'entity_id' => $id,
            'description' => "Modification d'une entrée au registre d'enquête — N° {$enquete['numero']}",
            'old_values' => $oldEntry,
            'new_values' => $enquete,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        self::notifyChange('update', $enquete, $authUser['sub'] ?? null);

        Response::success($enquete, 'Enquête modifiée avec succès');
    }

    /**
     * DELETE /api/registres-enquete/{id}
     */
    public function destroy(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $enquete = RegistreEnquete::find($id);
        if (!$enquete) {
            Response::notFound('Enquête introuvable');
        }

        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/registre_enquete';
        foreach (RegistreEnqueteAttachment::listForEnquete($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        RegistreEnquete::delete($id);

        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'registre_enquete',
            'entity_id' => $id,
            'description' => "Suppression d'une entrée du registre d'enquête — N° {$enquete['numero']}",
            'old_values' => $enquete,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Enquête supprimée avec succès');
    }

    private static function notifyChange(string $action, array $entry, ?int $actorId): void
    {
        $link = self::LINK_PREFIX . $entry['id'];
        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? "Nouvelle entrée au registre d'enquête" : 'Entrée modifiée au registre d\'enquête';

        Notification::notifyFeatureChange(self::MODULE, [
            'title'   => $title,
            'message' => "Une entrée au registre d'enquête a été {$verb}. N° {$entry['numero']} — {$entry['nature_infraction']}.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => "Une entrée au registre d'enquête a été {$verb}. N° {$entry['numero']} — {$entry['nature_infraction']}. Veuillez en prendre connaissance.",
            'type'    => 'info',
            'service' => 'PJ',
            'link'    => $link,
        ], $actorId);
    }
}
