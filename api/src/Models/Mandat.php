<?php
declare(strict_types=1);

namespace App\Models;

use App\Database;

/**
 * Mandat — judicial warrant (Police Judiciaire).
 *
 * The numero is auto-generated via PlainteSequence::MAN when the user
 * does not supply one, and is user-overridable.
 */
class Mandat
{
    /** Allowed mandat types (objet du mandat). */
    public const TYPES = ['AMENER', 'COMPARUTION', 'ARRET', 'DEPOT'];

    /** Human-readable labels for each type. */
    public const TYPE_LABELS = [
        'AMENER'      => "Mandat d'amener",
        'COMPARUTION' => 'Mandat de comparution',
        'ARRET'       => "Mandat d'arrêt",
        'DEPOT'       => 'Mandat de dépôt',
    ];

    public static function all(array $filters = []): array
    {
        $db = Database::getInstance()->getConnection();
        $where = [];
        $args = [];

        if (!empty($filters['type'])) {
            $where[] = 'm.type = ?';
            $args[] = $filters['type'];
        }
        if (!empty($filters['search'])) {
            $where[] = '(m.numero LIKE ? OR m.personne_nom LIKE ? OR m.autorite LIKE ? OR m.qualification_infraction LIKE ?)';
            $s = '%' . $filters['search'] . '%';
            array_push($args, $s, $s, $s, $s);
        }

        $sql = 'SELECT m.*, u.username AS agent_username, u.prenoms AS agent_prenoms, u.nom AS agent_nom
                FROM mandat m
                LEFT JOIN users u ON m.created_by = u.id';
        if ($where) {
            $sql .= ' WHERE ' . implode(' AND ', $where);
        }
        $sql .= ' ORDER BY m.created_at DESC, m.id DESC';

        $stmt = $db->prepare($sql);
        $stmt->execute($args);
        return $stmt->fetchAll(\PDO::FETCH_ASSOC);
    }

    public static function find(int $id): ?array
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT m.*, u.username AS agent_username, u.prenoms AS agent_prenoms, u.nom AS agent_nom
             FROM mandat m
             LEFT JOIN users u ON m.created_by = u.id
             WHERE m.id = ?'
        );
        $stmt->execute([$id]);
        $row = $stmt->fetch(\PDO::FETCH_ASSOC);
        return $row !== false ? $row : null;
    }

    public static function numeroExists(string $numero, ?int $excludeId = null): bool
    {
        $db = Database::getInstance()->getConnection();
        if ($excludeId !== null) {
            $stmt = $db->prepare('SELECT 1 FROM mandat WHERE numero = ? AND id <> ?');
            $stmt->execute([$numero, $excludeId]);
        } else {
            $stmt = $db->prepare('SELECT 1 FROM mandat WHERE numero = ?');
            $stmt->execute([$numero]);
        }
        return $stmt->fetch() !== false;
    }

    public static function create(array $data): int
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'INSERT INTO mandat
                (numero, type, autorite, personne_nom, date_lieu_naissance, motif,
                 qualification_infraction, opj_execution, date_heure_execution, lieu_execution,
                 observations, created_by)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $stmt->execute([
            $data['numero'],
            $data['type'],
            $data['autorite'] ?? null,
            $data['personne_nom'],
            $data['date_lieu_naissance'] ?? null,
            $data['motif'] ?? null,
            $data['qualification_infraction'] ?? null,
            $data['opj_execution'] ?? null,
            $data['date_heure_execution'] ?? null,
            $data['lieu_execution'] ?? null,
            $data['observations'] ?? null,
            $data['created_by'] ?? null,
        ]);
        return (int) $db->lastInsertId();
    }

    public static function update(int $id, array $data): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'UPDATE mandat SET
                numero = ?, type = ?, autorite = ?, personne_nom = ?, date_lieu_naissance = ?,
                motif = ?, qualification_infraction = ?, opj_execution = ?, date_heure_execution = ?,
                lieu_execution = ?, observations = ?
             WHERE id = ?'
        );
        return $stmt->execute([
            $data['numero'],
            $data['type'],
            $data['autorite'] ?? null,
            $data['personne_nom'],
            $data['date_lieu_naissance'] ?? null,
            $data['motif'] ?? null,
            $data['qualification_infraction'] ?? null,
            $data['opj_execution'] ?? null,
            $data['date_heure_execution'] ?? null,
            $data['lieu_execution'] ?? null,
            $data['observations'] ?? null,
            $id,
        ]);
    }

    public static function delete(int $id): bool
    {
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare('DELETE FROM mandat WHERE id = ?');
        return $stmt->execute([$id]);
    }
}
