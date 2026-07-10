<?php

namespace App\Validators;

use App\Models\Personnel;

class PersonnelValidator
{
    public static function validateCreate(array $data, ?int $excludeId = null): array
    {
        $errors = [];

        if (empty($data['lastname'])) {
            $errors['lastname'] = 'Nom is required';
        }

        if (empty($data['firstname'])) {
            $errors['firstname'] = 'Prénoms is required';
        }

        if (empty($data['im'])) {
            $errors['im'] = 'IM (Matricule) is required';
        } else {
            $existing = Personnel::getByIM($data['im']);
            if ($existing && (!$excludeId || (int) $existing['id'] !== $excludeId)) {
                $errors['im'] = 'This IM already exists';
            }
        }

        if (empty($data['grade'])) {
            $errors['grade'] = 'Grade is required';
        }

        return $errors;
    }
}
