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
}
