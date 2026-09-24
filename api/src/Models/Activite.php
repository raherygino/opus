<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * Activite — patrol / intervention record (Service Général).
 *
 * Single-table feature: patrol itineraries live in six nullable columns
 * (one per type×mode pair); a NULL itinerary means the mode was not
 * selected, an empty string means it was selected without details.
 */
class Activite
{
    /** The six patrol itinerary columns (NULL = mode not selected). */
    public const ITINERAIRE_FIELDS = [
        'patrouille_diurne_motorisee_itineraire',
        'patrouille_diurne_pedestre_itineraire',
        'patrouille_diurne_portee_itineraire',
        'patrouille_nocturne_motorisee_itineraire',
        'patrouille_nocturne_pedestre_itineraire',
        'patrouille_nocturne_portee_itineraire',
    ];

    /** Other free-text columns (trimmed; empty string → null). */
    public const TEXT_FIELDS = [
        'operation_ciblee', 'faits_constates', 'compte_rendu_hierarchie',
        'conduite_a_tenir', 'nature_intervention', 'suites_donnees',
    ];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(a.operation_ciblee LIKE ? OR a.faits_constates LIKE ? OR a.nature_intervention LIKE ? OR a.suites_donnees LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }

        $sql = 'SELECT a.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM activite a
                LEFT JOIN users u ON a.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY a.date_activite DESC, a.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT a.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM activite a
             LEFT JOIN users u ON a.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE a.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO activite
                (date_activite, heure_activite,
                 patrouille_diurne_motorisee_itineraire, patrouille_diurne_pedestre_itineraire, patrouille_diurne_portee_itineraire,
                 patrouille_nocturne_motorisee_itineraire, patrouille_nocturne_pedestre_itineraire, patrouille_nocturne_portee_itineraire,
                 operation_ciblee, faits_constates, compte_rendu_hierarchie, conduite_a_tenir,
                 nature_intervention, suites_donnees, latitude, longitude, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['date_activite'],
            $data['heure_activite'],
            $data['patrouille_diurne_motorisee_itineraire'] ?? null,
            $data['patrouille_diurne_pedestre_itineraire'] ?? null,
            $data['patrouille_diurne_portee_itineraire'] ?? null,
            $data['patrouille_nocturne_motorisee_itineraire'] ?? null,
            $data['patrouille_nocturne_pedestre_itineraire'] ?? null,
            $data['patrouille_nocturne_portee_itineraire'] ?? null,
            $data['operation_ciblee'] ?? null,
            $data['faits_constates'] ?? null,
            $data['compte_rendu_hierarchie'] ?? null,
            $data['conduite_a_tenir'] ?? null,
            $data['nature_intervention'] ?? null,
            $data['suites_donnees'] ?? null,
            $data['latitude'] ?? null,
            $data['longitude'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE activite SET
                date_activite = ?, heure_activite = ?,
                patrouille_diurne_motorisee_itineraire = ?, patrouille_diurne_pedestre_itineraire = ?, patrouille_diurne_portee_itineraire = ?,
                patrouille_nocturne_motorisee_itineraire = ?, patrouille_nocturne_pedestre_itineraire = ?, patrouille_nocturne_portee_itineraire = ?,
                operation_ciblee = ?, faits_constates = ?, compte_rendu_hierarchie = ?, conduite_a_tenir = ?,
                nature_intervention = ?, suites_donnees = ?, latitude = ?, longitude = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['date_activite'],
            $data['heure_activite'],
            $data['patrouille_diurne_motorisee_itineraire'] ?? null,
            $data['patrouille_diurne_pedestre_itineraire'] ?? null,
            $data['patrouille_diurne_portee_itineraire'] ?? null,
            $data['patrouille_nocturne_motorisee_itineraire'] ?? null,
            $data['patrouille_nocturne_pedestre_itineraire'] ?? null,
            $data['patrouille_nocturne_portee_itineraire'] ?? null,
            $data['operation_ciblee'] ?? null,
            $data['faits_constates'] ?? null,
            $data['compte_rendu_hierarchie'] ?? null,
            $data['conduite_a_tenir'] ?? null,
            $data['nature_intervention'] ?? null,
            $data['suites_donnees'] ?? null,
            $data['latitude'] ?? null,
            $data['longitude'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM activite WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
