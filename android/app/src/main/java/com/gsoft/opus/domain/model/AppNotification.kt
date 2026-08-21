package com.gsoft.opus.domain.model

/**
 * A notification as displayed in the app (same content as the desktop
 * notifications page).
 */
data class AppNotification(
    val id: Int,
    val title: String,
    val message: String?,
    val type: String,     // info | success | warning | error
    val service: String,  // PJ | SG | Sedentaire | System
    val isRead: Boolean,
    val createdAt: String,
    val personnelId: Int?,
    val personnelIm: String?,
    val personnelNom: String?,
    val personnelPrenoms: String?,
    val personnelGrade: String?,
    val personnelPhoto: String?,
    val createdByUsername: String?,
    val createdByFirstname: String?,
    val createdByPhoto: String?,
    val createdByPersonnelId: Int?
)
