package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.PersonneRecherchee
import com.gsoft.opus.domain.model.PersonneRechercheePhoto

/** Input data for creating/updating a personne recherchée. */
data class PersonneRechercheeFormData(
    val nom: String,
    val adresse: String?,
    val motif: String
)

interface PersonneRechercheeRepository {
    suspend fun getPersonneRechercheeList(search: String? = null): Resource<List<PersonneRecherchee>>
    suspend fun getPersonneRecherchee(id: Int): Resource<PersonneRecherchee>
    suspend fun createPersonneRecherchee(data: PersonneRechercheeFormData): Resource<PersonneRecherchee>
    suspend fun updatePersonneRecherchee(id: Int, data: PersonneRechercheeFormData): Resource<PersonneRecherchee>
    suspend fun deletePersonneRecherchee(id: Int): Resource<Unit>

    // Dedicated multi-image endpoints (NOT the generic attachment system)
    suspend fun getPhotos(personneId: Int): Resource<List<PersonneRechercheePhoto>>
    suspend fun addPhoto(
        personneId: Int,
        caption: String?,
        captureSource: String?,
        file: UploadFile
    ): Resource<PersonneRechercheePhoto>
    suspend fun updatePhotoCaption(personneId: Int, photoId: Int, caption: String?): Resource<PersonneRechercheePhoto>
    suspend fun deletePhoto(personneId: Int, photoId: Int): Resource<Unit>
}
