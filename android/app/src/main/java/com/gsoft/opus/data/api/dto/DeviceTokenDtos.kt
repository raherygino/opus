package com.gsoft.opus.data.api.dto

import com.google.gson.annotations.SerializedName

data class DeviceTokenRequestDto(
    @SerializedName("token") val token: String,
    @SerializedName("device_name") val deviceName: String? = null
)

data class DeviceTokenResponseDto(
    @SerializedName("id") val id: Int
)
