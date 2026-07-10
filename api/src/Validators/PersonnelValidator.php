<?php

namespace App\Validators;

use App\Models\Personnel;

class PersonnelValidator
{
    public static function validateCreate(array $data, ?int $excludeId = null): array
    {
        $errors = [];

        if (empty($data['lastname'])) {
            $errors['lastname'] = 'Lastname is required';
        }

        if (empty($data['firstname'])) {
            $errors['firstname'] = 'Firstname is required';
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

        if (empty($data['fonction'])) {
            $errors['fonction'] = 'Fonction is required';
        }

        if (!empty($data['email']) && !filter_var($data['email'], FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'Invalid email format';
        }

        if (!empty($data['status']) && !in_array($data['status'], ['active', 'inactive'])) {
            $errors['status'] = 'Status must be active or inactive';
        }

        return $errors;
    }
}
