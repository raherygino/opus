<?php

namespace App\Controllers;

use App\Helpers\Response;
use App\Models\MainCourante;
use App\Models\MainCouranteAttachment;
use App\Models\AuditLog;
use App\Models\Notification;

class MainCouranteController
{
    /** Map an origine to its permission module code (used for notifications). */
    private const MODULE_BY_ORIGINE = [
        'Secretariat' => 'sedentaire_secretariat_main_courante',
        'Poste'       => 'sedentaire_poste_main_courante',
    ];

    /** Map an origine to its notification link prefix. */
    private const LINK_PREFIX_BY_ORIGINE = [
        'Secretariat' => '/sedentaire/secretariat/main-courante/',
        'Poste'       => '/sedentaire/poste/main-courante/',
    ];

    /**
     * Validation shared by store() and update(). Returns an array of
     * field => message errors (empty when valid).
     */
    private static function validate(array $data, bool $isCreate): array
    {
        $errors = [];

        if ($isCreate || array_key_exists('date_evenement', $data)) {
            $value = $data['date_evenement'] ?? null;
            if (empty($value)) {
                $errors['date_evenement'] = 'La période (date) est requise';
            } elseif (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $value) || !strtotime($value)) {
                $errors['date_evenement'] = 'La date est invalide (format attendu : AAAA-MM-JJ)';
            }
        }

        if ($isCreate || array_key_exists('heure_evenement', $data)) {
            $value = $data['heure_evenement'] ?? null;
            if (empty($value)) {
                $errors['heure_evenement'] = "L'heure précise est requise";
            } elseif (!preg_match('/^([01]\d|2[0-3]):[0-5]\d(:[0-5]\d)?$/', $value)) {
                $errors['heure_evenement'] = "L'heure est invalide (format attendu : HH:MM)";
            }
        }

        if ($isCreate || array_key_exists('categorie', $data)) {
            $value = $data['categorie'] ?? null;
            if (empty($value)) {
                $errors['categorie'] = "La catégorie de l'événement est requise";
            } elseif (!\App\Models\MainCouranteCategorie::exists($value)) {
                $errors['categorie'] = "La catégorie est invalide";
            }
        }

        if ($isCreate || array_key_exists('description', $data)) {
            $value = trim((string) ($data['description'] ?? ''));
            if ($value === '') {
                $errors['description'] = 'La description des faits est requise';
            } elseif (mb_strlen($value) > 65535) {
                $errors['description'] = 'La description est trop longue';
            }
        }

        if (array_key_exists('origine', $data)) {
            $value = $data['origine'] ?? null;
            if ($value !== null && $value !== '' && !in_array($value, MainCourante::ORIGINES, true)) {
                $errors['origine'] = "L'origine est invalide";
            }
        }

        return $errors;
    }

    /**
     * GET /api/main-courante
     */
    public function index(array $params): void
    {
        $filters = [];
        foreach (['origine', 'categorie', 'date_from', 'date_to', 'search'] as $key) {
            if (isset($_GET[$key]) && $_GET[$key] !== '') {
                $filters[$key] = $_GET[$key];
            }
        }

        $list = MainCourante::getAll($filters);
        Response::success($list);
    }

    /**
     * GET /api/main-courante/{id}
     */
    public function show(array $params): void
    {
        $row = MainCourante::getById((int) $params['id']);
        if (!$row) {
            Response::notFound('Main courante introuvable');
        }
        $row['attachments'] = MainCouranteAttachment::getByMainCouranteId((int) $row['id']);

        // Auto-dismiss the notification for the viewing user once they open
        // the detail. Notifications are only sent on creation, so viewing the
        // detail means the user has now seen it.
        $authUser = AuthController::getAuthenticatedUser();
        if ($authUser && !empty($authUser['sub'])) {
            $link = self::linkFor($row);
            Notification::markAsReadByLink($link, (int) $authUser['sub']);
        }

        Response::success($row);
    }

    /**
     * POST /api/main-courante
     */
    public function store(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // Default origine to Secretariat when not provided.
        if (empty($data['origine'])) {
            $data['origine'] = 'Secretariat';
        }
        $data['description'] = trim((string) ($data['description'] ?? ''));
        $data['created_by'] = $authUser['sub'] ?? null;

        $errors = self::validate($data, true);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $id = MainCourante::create($data);
        $mainCourante = MainCourante::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'create',
            'module' => 'main_courante',
            'entity_id' => $id,
            'description' => "Création d'une main courante ({$mainCourante['categorie']}) — {$mainCourante['date_evenement']} {$mainCourante['heure_evenement']}",
            'new_values' => $mainCourante,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with view permission) ---
        self::notifyChange('create', $mainCourante, (int) $authUser['sub']);

        Response::created($mainCourante, 'Main courante enregistrée avec succès');
    }

    /**
     * PUT /api/main-courante/{id}
     */
    public function update(array $params): void
    {
        $authUser = AuthController::getAuthenticatedUser();
        if (!$authUser) {
            Response::unauthorized('Authentication required');
        }

        $id = (int) $params['id'];
        $mainCourante = MainCourante::getById($id);
        if (!$mainCourante) {
            Response::notFound('Main courante introuvable');
        }

        $data = json_decode(file_get_contents('php://input'), true) ?? [];

        // origine is not editable through update — it is fixed at creation.
        unset($data['origine']);

        if (array_key_exists('description', $data)) {
            $data['description'] = trim((string) $data['description']);
        }

        $errors = self::validate($data, false);
        if (!empty($errors)) {
            Response::error('Validation failed', 422, $errors);
        }

        $oldMainCourante = $mainCourante;
        MainCourante::update($id, $data);
        $mainCourante = MainCourante::getById($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'update',
            'module' => 'main_courante',
            'entity_id' => $id,
            'description' => "Modification d'une main courante ({$mainCourante['categorie']}) — {$mainCourante['date_evenement']} {$mainCourante['heure_evenement']}",
            'old_values' => $oldMainCourante,
            'new_values' => $mainCourante,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        // --- Notification (peer-to-peer: admins + all users with view permission) ---
        self::notifyChange('update', $mainCourante, $authUser['sub'] ?? null);

        Response::success($mainCourante, 'Main courante modifiée avec succès');
    }

    /**
     * DELETE /api/main-courante/{id}
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
        $mainCourante = MainCourante::getById($id);
        if (!$mainCourante) {
            Response::notFound('Main courante introuvable');
        }

        // Remove attachment files from disk before the cascade delete.
        $config = require __DIR__ . '/../../config/app.php';
        $uploadDir = rtrim($config['upload_dir'], '/') . '/main-courante';
        foreach (MainCouranteAttachment::getByMainCouranteId($id) as $attachment) {
            $filePath = $uploadDir . '/' . $attachment['filename'];
            if (file_exists($filePath)) {
                unlink($filePath);
            }
        }

        MainCourante::delete($id);

        // --- Audit log ---
        AuditLog::create([
            'user_id' => $authUser['sub'] ?? null,
            'action' => 'delete',
            'module' => 'main_courante',
            'entity_id' => $id,
            'description' => "Suppression d'une main courante ({$mainCourante['categorie']}) — {$mainCourante['date_evenement']} {$mainCourante['heure_evenement']}",
            'old_values' => $mainCourante,
            'ip_address' => $_SERVER['REMOTE_ADDR'] ?? null,
            'user_agent' => $_SERVER['HTTP_USER_AGENT'] ?? null,
        ]);

        Response::success(null, 'Main courante supprimée avec succès');
    }

    /**
     * Build the notification link for a main courante entry based on its origine.
     */
    private static function linkFor(array $mainCourante): string
    {
        $prefix = self::LINK_PREFIX_BY_ORIGINE[$mainCourante['origine']] ?? self::LINK_PREFIX_BY_ORIGINE['Secretariat'];
        return $prefix . $mainCourante['id'];
    }

    /**
     * Notify admins + feature users of a create/update. The module code is
     * selected from the entry's origine so each context notifies only its
     * own audience. Push delivery failures are isolated inside
     * Notification::create() and never affect the API response.
     */
    private static function notifyChange(string $action, array $mainCourante, ?int $actorId): void
    {
        $module = self::MODULE_BY_ORIGINE[$mainCourante['origine']] ?? self::MODULE_BY_ORIGINE['Secretariat'];
        $link = self::linkFor($mainCourante);

        $date = date('d/m/Y', strtotime($mainCourante['date_evenement']));
        $heure = substr((string) $mainCourante['heure_evenement'], 0, 5);
        $categorie = $mainCourante['categorie'];

        $verb = $action === 'create' ? 'enregistrée' : 'modifiée';
        $title = $action === 'create' ? 'Nouvelle main courante' : 'Main courante modifiée';

        $adminMessage = "Une main courante a été {$verb}. Catégorie: {$categorie} — le {$date} à {$heure}.";
        $userMessage = "Une main courante a été {$verb}. Catégorie: {$categorie} — le {$date} à {$heure}. Veuillez en prendre connaissance.";

        Notification::notifyFeatureChange($module, [
            'title'   => $title,
            'message' => $adminMessage,
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], [
            'title'   => $title,
            'message' => $userMessage,
            'type'    => 'info',
            'service' => 'Sedentaire',
            'link'    => $link,
        ], $actorId);
    }
}
