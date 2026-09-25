<?php

namespace App\Models;

use App\Database;
use PDO;

/**
 * Aggregated KPI counts for the main dashboard (desktop + Android).
 *
 * All figures are computed server-side with COUNT queries so the clients
 * get a single, consistent payload instead of fetching every list endpoint.
 */
class DashboardStats
{
    /**
     * @param int    $userId   Authenticated user id (for unread notifications).
     * @param string $roleCode Authenticated user role code (admin roles see all).
     */
    public static function get(int $userId, ?string $roleCode = null): array
    {
        return [
            // Personnel & comptes
            'personnel_total'        => self::count('SELECT COUNT(*) FROM personnel'),
            'personnel_en_service'   => self::count(
                'SELECT COUNT(*) FROM personnel p
                 WHERE NOT EXISTS (SELECT 1 FROM mouvement_personnel mp
                                   WHERE mp.personnel_id = p.id AND mp.retour = "Non")'
            ),
            'personnel_en_mouvement' => self::count(
                'SELECT COUNT(DISTINCT personnel_id) FROM mouvement_personnel WHERE retour = "Non"'
            ),
            'mouvements_en_cours'    => self::count('SELECT COUNT(*) FROM mouvement_personnel WHERE retour = "Non"'),
            'users_total'            => self::count('SELECT COUNT(*) FROM users'),
            'users_actifs'           => self::count('SELECT COUNT(*) FROM users WHERE is_active = 1'),

            // Situation opérationnelle (éléments "en cours")
            'gav_en_cours'           => self::count(
                'SELECT COUNT(*) FROM garde_a_vue
                 WHERE debut_gav IS NOT NULL
                   AND (prolongation_gav > NOW()
                        OR (prolongation_gav IS NULL AND (fin_gav IS NULL OR fin_gav > NOW())))'
            ),
            'armes_en_service'       => self::count('SELECT COUNT(*) FROM armement WHERE heure_reintegration IS NULL'),
            'vehicules_en_service'   => self::count('SELECT COUNT(*) FROM materiel_roulant WHERE statut = "En service"'),

            // Activité récente
            'activites_total'        => self::count('SELECT COUNT(*) FROM activite'),
            'activites_7j'           => self::count(
                'SELECT COUNT(*) FROM activite WHERE date_activite >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)'
            ),
            'activites_aujourdhui'   => self::count('SELECT COUNT(*) FROM activite WHERE date_activite = CURDATE()'),
            'evenements_aujourdhui'  => self::count('SELECT COUNT(*) FROM evenement_survenu WHERE date_evenement = CURDATE()'),
            'main_courante_aujourdhui' => self::count('SELECT COUNT(*) FROM main_courante WHERE date_evenement = CURDATE()'),

            // Volumétrie par module
            'correspondances_total'    => self::count('SELECT COUNT(*) FROM correspondance'),
            'declarations_perte_total' => self::count('SELECT COUNT(*) FROM declaration_perte'),
            'plaintes_en_attente'      => self::count(
                'SELECT COUNT(*) FROM plainte_entree pe
                 WHERE NOT EXISTS (SELECT 1 FROM plainte_sortie ps WHERE ps.plainte_entree_id = pe.id)'
            ),
            'personnes_recherchees'    => self::count('SELECT COUNT(*) FROM personne_recherchee'),

            // Notifications non lues pour l'utilisateur courant
            'notifications_non_lues' => Notification::getUnreadCount(
                $userId,
                $roleCode,
                self::userServiceCode($userId)
            ),

            'generated_at' => date('c'),
        ];
    }

    private static function count(string $sql): int
    {
        $db = Database::getInstance()->getConnection();
        return (int) $db->query($sql)->fetchColumn();
    }

    /**
     * Map the user's affectation to a notification service code
     * (same rules as NotificationController::serviceToCode).
     */
    private static function userServiceCode(int $userId): ?string
    {
        $user = User::getById($userId);
        $affectation = $user['affectation'] ?? '';
        if ($affectation === '') return null;

        if (stripos($affectation, 'PJ') !== false) return 'PJ';
        if (stripos($affectation, 'SG') !== false) return 'SG';
        if (stripos($affectation, 'Sédentaire') !== false || stripos($affectation, 'Sedentaire') !== false) return 'Sedentaire';
        return 'System';
    }
}
