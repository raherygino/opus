<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * RegistreDeferrement — registre de déferrement (Police Judiciaire).
 *
 * Records each person brought before the judicial authority (défèrement):
 * identity of the deferred person, date/time of presentation, offence,
 * authority, destination, escort and outcome.
 *
 * The numero is auto-generated via PlainteSequence::DEF when the user
 * does not supply one, and is user-overridable.
 */
class RegistreDeferrement
{
    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['search'])) {
            $where[] = '(r.numero LIKE ? OR r.numero_dossier LIKE ? OR r.personne_nom LIKE ? OR r.infraction LIKE ? OR r.autorite LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s, $s);
        }

        $sql = 'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
                FROM registre_deferrement r
                LEFT JOIN users u ON r.created_by = u.id
                LEFT JOIN personnel p ON u.personnel_id = p.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY r.created_at DESC, r.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT r.*, u.username AS agent_username, u.personnel_id AS agent_personnel_id, p.firstname AS agent_prenoms, p.lastname AS agent_nom
             FROM registre_deferrement r
             LEFT JOIN users u ON r.created_by = u.id
             LEFT JOIN personnel p ON u.personnel_id = p.id
             WHERE r.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function numeroExists(string $numero, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId !== null) {
            $stmt = $db->prepare('SELECT 1 FROM registre_deferrement WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM registre_deferrement WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO registre_deferrement
                (numero, date_heure_deferrement, personne_nom, date_lieu_naissance, infraction,
                 numero_dossier, autorite, destination, escorte, suite_donnee, observations, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero'],
            $data['date_heure_deferrement'],
            $data['personne_nom'],
            $data['date_lieu_naissance'] ?? null,
            $data['infraction'] ?? null,
            $data['numero_dossier'] ?? null,
            $data['autorite'] ?? null,
            $data['destination'] ?? null,
            $data['escorte'] ?? null,
            $data['suite_donnee'] ?? null,
            $data['observations'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE registre_deferrement SET
                numero = ?, date_heure_deferrement = ?, personne_nom = ?, date_lieu_naissance = ?,
                infraction = ?, numero_dossier = ?, autorite = ?, destination = ?,
                escorte = ?, suite_donnee = ?, observations = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero'],
            $data['date_heure_deferrement'],
            $data['personne_nom'],
            $data['date_lieu_naissance'] ?? null,
            $data['infraction'] ?? null,
            $data['numero_dossier'] ?? null,
            $data['autorite'] ?? null,
            $data['destination'] ?? null,
            $data['escorte'] ?? null,
            $data['suite_donnee'] ?? null,
            $data['observations'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM registre_deferrement WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
