<?php

namespace App\Models;

use App\Database;
use DateTimeImmutable;
use PDO;

/**
 * Rapport d'activité (Sédentaire > Secrétariat > Rapport).
 *
 * Aggregated record counts for a given period — daily, weekly (Monday–Sunday,
 * ISO week) or monthly — computed server-side so Desktop and Android display
 * identical figures from a single endpoint.
 *
 * For every covered module the report exposes three counters:
 *  - created : records whose created_at falls inside the period
 *  - updated : records whose updated_at falls inside the period while
 *              created_at predates it (i.e. pre-existing records modified
 *              during the period — never double-counted with "created")
 *  - total   : created + updated → distinct records created or updated
 *              during the period
 */
class RapportStats
{
    public const TYPES = ['daily', 'weekly', 'monthly'];

    private const TYPE_LABELS = [
        'daily'   => 'Journalier',
        'weekly'  => 'Hebdomadaire',
        'monthly' => 'Mensuel',
    ];

    /**
     * Modules covered by the report, grouped exactly like the Sédentaire
     * navigation (Secrétariat / Poste). Each section points at the table it
     * counts; `where` is an optional extra filter (main courante is shared
     * between the two contexts, split by the `origine` column).
     */
    private const GROUPS = [
        [
            'key'      => 'secretariat',
            'label'    => 'Secrétariat',
            'sections' => [
                ['key' => 'correspondance',            'label' => 'Correspondances',          'table' => 'correspondance'],
                ['key' => 'personnel',                 'label' => 'Personnel enregistré',     'table' => 'personnel'],
                ['key' => 'mouvement_personnel',       'label' => 'Mouvements de personnel',  'table' => 'mouvement_personnel'],
                ['key' => 'comportement_personnel',    'label' => 'Comportements',            'table' => 'comportement_personnel'],
                ['key' => 'declaration_perte',         'label' => 'Déclarations de perte',    'table' => 'declaration_perte'],
                ['key' => 'main_courante_secretariat', 'label' => 'Main courante',            'table' => 'main_courante', 'where' => "origine = 'Secretariat'"],
            ],
        ],
        [
            'key'      => 'poste',
            'label'    => 'Poste',
            'sections' => [
                ['key' => 'passation',            'label' => 'Passations',               'table' => 'passation'],
                ['key' => 'armement',             'label' => 'Armements',                'table' => 'armement'],
                ['key' => 'arme',                 'label' => 'Armes enregistrées',       'table' => 'arme'],
                ['key' => 'affectation_materiel', 'label' => 'Affectations de matériel', 'table' => 'affectation_materiel'],
                ['key' => 'materiel_roulant',     'label' => 'Matériel roulant',         'table' => 'materiel_roulant'],
                ['key' => 'situation_gav',        'label' => 'Gardes à vue',             'table' => 'garde_a_vue'],
                ['key' => 'main_courante_poste',  'label' => 'Main courante',            'table' => 'main_courante', 'where' => "origine = 'Poste'"],
            ],
        ],
    ];

    /**
     * @param string $type daily|weekly|monthly (validated by the controller)
     * @param string $date Anchor date (Y-m-d) the period is built around.
     */
    public static function generate(string $type, string $date): array
    {
        [$start, $endExclusive] = self::period($type, $date);
        $db = Database::getInstance()->getConnection();

        $groups = [];
        $totals = ['created' => 0, 'updated' => 0, 'total' => 0];

        foreach (self::GROUPS as $group) {
            $sections = [];
            foreach ($group['sections'] as $section) {
                $created = self::countCreated($db, $section, $start, $endExclusive);
                $updated = self::countUpdated($db, $section, $start, $endExclusive);
                $sections[] = [
                    'key'     => $section['key'],
                    'label'   => $section['label'],
                    'created' => $created,
                    'updated' => $updated,
                    'total'   => $created + $updated,
                ];
                $totals['created'] += $created;
                $totals['updated'] += $updated;
                $totals['total']   += $created + $updated;
            }
            $groups[] = [
                'key'      => $group['key'],
                'label'    => $group['label'],
                'sections' => $sections,
            ];
        }

        return [
            'type'         => $type,
            'type_label'   => self::TYPE_LABELS[$type] ?? $type,
            'date'         => $date,
            // Inclusive bounds for display; counting uses [start, end) so
            // boundary timestamps are never missed.
            'period_start' => $start->format('Y-m-d'),
            'period_end'   => $endExclusive->modify('-1 day')->format('Y-m-d'),
            'groups'       => $groups,
            'totals'       => $totals,
            'generated_at' => date('c'),
        ];
    }

    /**
     * Resolve the period covered by the report.
     * @return array{0: DateTimeImmutable, 1: DateTimeImmutable} [start, endExclusive)
     */
    private static function period(string $type, string $date): array
    {
        $start = new DateTimeImmutable($date);
        switch ($type) {
            case 'weekly':
                // ISO week: Monday to Sunday (French convention).
                $start = $start->modify('monday this week');
                $endExclusive = $start->modify('+7 days');
                break;
            case 'monthly':
                $start = $start->modify('first day of this month');
                $endExclusive = $start->modify('first day of next month');
                break;
            default: // daily
                $endExclusive = $start->modify('+1 day');
        }
        return [$start, $endExclusive];
    }

    /** Records created inside the period. */
    private static function countCreated(PDO $db, array $section, DateTimeImmutable $start, DateTimeImmutable $end): int
    {
        $sql = "SELECT COUNT(*) FROM `{$section['table']}`
                WHERE created_at >= :start AND created_at < :end";
        if (isset($section['where'])) {
            $sql .= " AND {$section['where']}";
        }
        $stmt = $db->prepare($sql);
        $stmt->execute([
            'start' => $start->format('Y-m-d 00:00:00'),
            'end'   => $end->format('Y-m-d 00:00:00'),
        ]);
        return (int) $stmt->fetchColumn();
    }

    /**
     * Records modified inside the period that were created before it —
     * a record created during the period is reported once, under "created".
     */
    private static function countUpdated(PDO $db, array $section, DateTimeImmutable $start, DateTimeImmutable $end): int
    {
        $sql = "SELECT COUNT(*) FROM `{$section['table']}`
                WHERE updated_at >= :start AND updated_at < :end
                  AND created_at < :created_before";
        if (isset($section['where'])) {
            $sql .= " AND {$section['where']}";
        }
        $stmt = $db->prepare($sql);
        $stmt->execute([
            'start'          => $start->format('Y-m-d 00:00:00'),
            'end'            => $end->format('Y-m-d 00:00:00'),
            'created_before' => $start->format('Y-m-d 00:00:00'),
        ]);
        return (int) $stmt->fetchColumn();
    }
}
