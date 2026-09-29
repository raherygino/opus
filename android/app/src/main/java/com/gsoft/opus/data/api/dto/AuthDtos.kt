package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

data class LoginRequestDto(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequestDto(
    @SerializedName("refresh_token") val refreshToken: String
)

/** Body of PUT /api/auth/password. */
data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

/**
 * Body of PUT /api/auth/profile — self-service update of the authenticated
 * user's own personnel record. im/grade/affectation are administrative and
 * are not editable through this endpoint.
 */
data class UpdateProfileRequest(
    @SerializedName("lastname") val lastname: String,
    @SerializedName("firstname") val firstname: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("address") val address: String?
)
