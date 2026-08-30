package com.gsoft.opus.domain.model

data class Personnel(
    val id: Int,
    val im: String,
    val grade: String,
    val lastname: String,
    val firstname: String,
    val affectation: String?,
    val phone: String?,
    val address: String?,
    val photo: String?,
    val thumbnail: String?,
    val signature: String?,
    val signatureSvg: String?,
    val status: String,
    val createdAt: String?,
    val updatedAt: String?,
    /** True when this personnel record is linked to an admin user account. */
    val isAdminProfile: Boolean = false,
    /** True when a code secret is set (hash never exposed). */
    val hasCodeSecret: Boolean = false
)

data class PersonnelAttachment(
    val id: Int,
    val personnelId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

data class Mouvement(
    val id: Int,
    val personnelId: Int,
    val im: String,
    val grade: String?,
    val service: String?,
    val nom: String?,
    val prenoms: String?,
    val typeMouvement: String,
    val dateDepart: String?,
    val days: Int?,
    val dateRetour: String?,
    val retour: String,
    val createdAt: String?
) {
    val isReturned: Boolean get() = retour == "Oui"
}

data class MouvementAttachment(
    val id: Int,
    val mouvementId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

data class Comportement(
    val id: Int,
    val personnelId: Int,
    val im: String,
    val grade: String?,
    val service: String?,
    val nom: String?,
    val prenoms: String?,
    val type: String,
    val dateComportement: String,
    val motif: String,
    val decision: String?,
    val status: String,
    val confirmedBy: Int?,
    val confirmedAt: String?,
    val rejectedReason: String?,
    val confirmedByUsername: String?,
    val createdBy: Int?,
    val createdByUsername: String?,
    val createdAt: String?
) {
    val isPending: Boolean get() = status == "pending"
    val isConfirmed: Boolean get() = status == "confirmed"
    val isRejected: Boolean get() = status == "rejected"
}
