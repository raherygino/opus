<?php

namespace App\Validators;

class AuthValidator
{
    public static function validateLogin(array $data): array
    {
        $errors = [];

        if (empty($data['username'])) {
            $errors['username'] = 'Username is required';
        }

        if (empty($data['password'])) {
            $errors['password'] = 'Password is required';
        }

        return $errors;
    }

    public static function validatePasswordChange(array $data): array
    {
        $errors = [];

        if (empty($data['current_password'])) {
            $errors['current_password'] = 'Current password is required';
        }

        if (empty($data['new_password'])) {
            $errors['new_password'] = 'New password is required';
        } elseif (strlen($data['new_password']) < 6) {
            $errors['new_password'] = 'New password must be at least 6 characters';
        }

        return $errors;
    }

    /**
     * Validates a self-service profile update (PUT /api/auth/profile).
     * Only the user's own personnel contact/identity fields are editable;
     * IM, grade and affectation stay administrative and are never accepted here.
     */
    public static function validateProfileUpdate(array $data): array
    {
        $errors = [];

        if (empty($data['lastname']) || trim((string) $data['lastname']) === '') {
            $errors['lastname'] = 'Le nom est requis';
        }

        if (empty($data['firstname']) || trim((string) $data['firstname']) === '') {
            $errors['firstname'] = 'Le prénom est requis';
        }

        if (isset($data['email']) && $data['email'] !== '' && $data['email'] !== null
            && !filter_var($data['email'], FILTER_VALIDATE_EMAIL)) {
            $errors['email'] = 'Adresse e-mail invalide';
        }

        return $errors;
    }
}
