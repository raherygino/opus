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
    val updatedAt: String?
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
    val createdAt: String?
)
