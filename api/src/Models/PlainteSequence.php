<?php

namespace App\Models;

use App\Database;
use PDO;

/**
 * Reusable dossier/record number generator for the PLAINTE feature.
 *
 * Each (type_key, year) pair has its own independent counter that resets
 * to 1 at the start of each new year. The unique constraint on
 * (type_key, year) in the plainte_sequence table guarantees correctness
 * under concurrent inserts via INSERT ... ON DUPLICATE KEY UPDATE.
 *
 * Supported formats:
 *   ST     → N°{seq}/TRIMO/ST/{YY}
 *   PD     → N°{seq}/TRIMO/PD/{YY}
 *   RP     → N°{seq}/TRIMO/RP/{YY}
 *   SORTIE → N°{seq}/MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/{YY}
 *   COV_ST → N°{seq}/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/{YY}
 *   COV_PD → N°{seq}/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/{YY}
 *
 * {YY} is the 2-digit year (e.g. 26 for 2026) and {seq} is zero-padded to
 * 3 digits (e.g. 001).
 */
class PlainteSequence
{
    /** Map of type_key → format template (uses {seq} and {yy} placeholders). */
    public const FORMATS = [
        'ST'     => 'N°{seq}/TRIMO/ST/{yy}',
        'PD'     => 'N°{seq}/TRIMO/PD/{yy}',
        'RP'     => 'N°{seq}/TRIMO/RP/{yy}',
        'SORTIE' => 'N°{seq}/MSP/DGPN/DGA/DRSP-1/CSP/A-TRIMO/{yy}',
        'COV_ST' => 'N°{seq}/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/{yy}',
        'COV_PD' => 'N°{seq}/MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/{yy}',
        'REQ'    => 'N°{seq}/MSP/SG/DGPN/DRSP.1/REQ/CSP/A-TRIMO/{yy}',
        'PEQ'    => 'N°{seq}/MSP/SG/DGPN/DGA/DRSP.1/PEQ/CSP/TRIMO/{yy}',
        'MAN'    => 'N°{seq}/MSP/SG/DGPN/DGA/DRSP.1/MAN/CSP/TRIMO/{yy}',
        'ARR'    => 'N°{seq}/MSP/SG/DGPN/DGA/DRSP.1/ARR/CSP/TRIMO/{yy}',
    ];

    /** ENTRÉE type → sequence type_key map. */
    public const TYPE_TO_KEY = [
        'ST_PARQUET'      => 'ST',
        'PLAINTE_DIRECTE' => 'PD',
        'RAPPORT_POLICE'  => 'RP',
    ];

    /** CONVOCATION type → sequence type_key map. */
    public const CONVOCATION_TYPE_TO_KEY = [
        'ST_PARQUET'      => 'COV_ST',
        'PLAINTE_DIRECTE' => 'COV_PD',
    ];

    /** REQUISITION sequence type_key (single sequence for all requisition types). */
    public const REQUISITION_KEY = 'REQ';

    /** PERQUISITION sequence type_key. */
    public const PERQUISITION_KEY = 'PEQ';

    /** MANDAT sequence type_key. */
    public const MANDAT_KEY = 'MAN';

    /** ARRESTATION sequence type_key. */
    public const ARRESTATION_KEY = 'ARR';

    /**
     * Generate the next dossier/record number for the given type_key.
     *
     * The sequence counter is atomically incremented inside a transaction
     * using INSERT ... ON DUPLICATE KEY UPDATE so that concurrent calls
     * never produce duplicate numbers. The year defaults to the current
     * 2-digit year.
     *
     * @param string   $typeKey One of: ST, PD, RP, SORTIE.
     * @param int|null $year    2-digit year (defaults to current year).
     * @return string The formatted number (e.g. "N°001/TRIMO/ST/26").
     */
    public static function nextNumber(string $typeKey, ?int $year = null): string
    {
        if (!isset(self::FORMATS[$typeKey])) {
            throw new \InvalidArgumentException("Unknown plainte sequence type_key: $typeKey");
        }

        $yy = $year ?? (int) date('y');
        $db = Database::getInstance()->getConnection();

        // Atomically reserve the next sequence number. The unique
        // (type_key, year) constraint makes this safe under concurrency.
        $db->beginTransaction();
        try {
            $stmt = $db->prepare(
                'INSERT INTO plainte_sequence (type_key, year, last_number)
                 VALUES (?, ?, 1)
                 ON DUPLICATE KEY UPDATE last_number = last_number + 1'
            );
            $stmt->execute([$typeKey, $yy]);

            $select = $db->prepare(
                'SELECT last_number FROM plainte_sequence WHERE type_key = ? AND year = ?'
            );
            $select->execute([$typeKey, $yy]);
            $seq = (int) $select->fetchColumn();

            $db->commit();
        } catch (\Throwable $e) {
            $db->rollBack();
            throw $e;
        }

        $padded = str_pad((string) $seq, 3, '0', STR_PAD_LEFT);
        return str_replace(['{seq}', '{yy}'], [$padded, (string) $yy], self::FORMATS[$typeKey]);
    }

    /**
     * Peek at what the next number WOULD be without consuming it.
     * Useful for preview; the actual number is only assigned on save.
     */
    public static function peekNumber(string $typeKey, ?int $year = null): string
    {
        if (!isset(self::FORMATS[$typeKey])) {
            throw new \InvalidArgumentException("Unknown plainte sequence type_key: $typeKey");
        }
        $yy = $year ?? (int) date('y');
        $db = Database::getInstance()->getConnection();
        $stmt = $db->prepare(
            'SELECT last_number FROM plainte_sequence WHERE type_key = ? AND year = ?'
        );
        $stmt->execute([$typeKey, $yy]);
        $current = (int) $stmt->fetchColumn();
        $next = $current + 1;
        $padded = str_pad((string) $next, 3, '0', STR_PAD_LEFT);
        return str_replace(['{seq}', '{yy}'], [$padded, (string) $yy], self::FORMATS[$typeKey]);
    }
}
