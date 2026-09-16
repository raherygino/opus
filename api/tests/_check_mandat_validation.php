<?php
require __DIR__ . '/../config/bootstrap.php';
use App\Controllers\MandatController;

// Simulate a typical desktop payload
$data = [
    'type' => 'AMENER',
    'autorite' => null,
    'personne_nom' => 'Test Person',
    'date_lieu_naissance' => null,
    'motif' => null,
    'qualification_infraction' => null,
    'opj_execution' => null,
    'date_heure_execution' => null,
    'lieu_execution' => null,
    'observations' => null,
];

$method = new ReflectionMethod(MandatController::class, 'validate');
$method->setAccessible(true);
$errors = $method->invoke(null, $data, true, null);
echo "Errors for null-date payload: " . json_encode($errors) . "\n";

// Now with datetime-local format
$data2 = array_merge($data, ['date_heure_execution' => '2026-01-15T10:00']);
$errors2 = $method->invoke(null, $data2, true, null);
echo "Errors for datetime-local payload: " . json_encode($errors2) . "\n";

// Now with empty type (simulating no selection)
$data3 = array_merge($data, ['type' => '']);
$errors3 = $method->invoke(null, $data3, true, null);
echo "Errors for empty-type payload: " . json_encode($errors3) . "\n";
