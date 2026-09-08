<?php
/**
 * Seed script — insert default equipment types into type_materiel.
 *
 * Usage: php api/seed-type-materiel.php
 *
 * Safe to run multiple times: existing types (by name) are skipped.
 */

require_once __DIR__ . '/src/Database.php';
require __DIR__ . '/config/bootstrap.php';

use App\Database;
use App\Models\TypeMateriel;

$defaults = [
    ['nom' => 'Radio',         'description' => 'Talkie-walkie / radio de communication'],
    ['nom' => 'Bâton',         'description' => 'Bâton de défense / tonfa'],
    ['nom' => 'Gilet',         'description' => 'Gilet pare-balles ou gilet de signalisation'],
    ['nom' => 'Menottes',      'description' => 'Paire de menottes'],
    ['nom' => 'Lampe',         'description' => 'Lampe torche professionnelle'],
    ['nom' => 'Taser',         'description' => 'Arme à impulsion électrique'],
    ['nom' => 'Jumelles',      'description' => 'Jumelles d\'observation'],
    ['nom' => 'Gilet tactique', 'description' => 'Gilet tactique avec porte-plaques'],
];

$created = 0;
$skipped = 0;

foreach ($defaults as $type) {
    $existing = TypeMateriel::getByNom($type['nom']);
    if ($existing) {
        echo "SKIP: {$type['nom']} (déjà existant, id={$existing['id']})\n";
        $skipped++;
        continue;
    }

    $id = TypeMateriel::create([
        'nom'        => $type['nom'],
        'description' => $type['description'],
    ]);

    echo "OK:   {$type['nom']} créé (id={$id})\n";
    $created++;
}

echo "\nTerminé : $created type(s) créé(s), $skipped type(s) ignoré(s).\n";
