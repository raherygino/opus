package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteEntreeAttachment
import com.gsoft.opus.domain.model.PlainteEntreeSummary
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.domain.model.PlainteSortieAttachment

/** Input data for creating/updating a plainte ENTRÉE. */
data class PlainteEntreeFormData(
    val type: String,
    val datePlainte: String,
    val numeroDossier: String? = null,
    val numeroSt: String?,
    val opjPersonnelId: Int?,
    val enqueteurPersonnelId: Int?,
    val partieCivile: String?,
    val miseEnCause: String?,
    val adressePc: String?,
    val infraction: String?,
    val prejudice: String?,
    val lieuInfraction: String?,
    val heureInfraction: String?,
    val observation: String?
)

/** Input data for creating/updating a plainte SORTIE. */
data class PlainteSortieFormData(
    val plainteEntreeId: Int,
    val nature: String,
    val dateSortie: String,
    val numero: String? = null,
    val numeroTtr: String,
    val nomSubstitut: String,
    val dateDeferrement: String?,
    val observation: String?
)

interface PlainteRepository {
    // ── ENTRÉE ────────────────────────────────────────────────────
    suspend fun getPlainteEntreeList(
        type: String? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<PlainteEntree>>

    suspend fun getPlainteEntree(id: Int): Resource<PlainteEntree>
    suspend fun createPlainteEntree(data: PlainteEntreeFormData): Resource<PlainteEntree>
    suspend fun updatePlainteEntree(id: Int, data: PlainteEntreeFormData): Resource<PlainteEntree>
    suspend fun deletePlainteEntree(id: Int): Resource<Unit>
    suspend fun getEntreesWithoutSortie(): Resource<List<PlainteEntreeSummary>>
    suspend fun peekEntreeNumber(type: String): Resource<String>

    suspend fun getEntreeAttachments(plainteEntreeId: Int): Resource<List<PlainteEntreeAttachment>>
    suspend fun addEntreeAttachment(plainteEntreeId: Int, title: String, file: UploadFile): Resource<PlainteEntreeAttachment>
    suspend fun updateEntreeAttachmentTitle(plainteEntreeId: Int, attachId: Int, title: String): Resource<PlainteEntreeAttachment>
    suspend fun deleteEntreeAttachment(plainteEntreeId: Int, attachId: Int): Resource<Unit>

    // ── SORTIE ────────────────────────────────────────────────────
    suspend fun getPlainteSortieList(
        nature: String? = null,
        entreeId: Int? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<PlainteSortie>>

    suspend fun getPlainteSortie(id: Int): Resource<PlainteSortie>
    suspend fun createPlainteSortie(data: PlainteSortieFormData): Resource<PlainteSortie>
    suspend fun updatePlainteSortie(id: Int, data: PlainteSortieFormData): Resource<PlainteSortie>
    suspend fun deletePlainteSortie(id: Int): Resource<Unit>
    suspend fun peekSortieNumber(): Resource<String>

    suspend fun getSortieAttachments(plainteSortieId: Int): Resource<List<PlainteSortieAttachment>>
    suspend fun addSortieAttachment(plainteSortieId: Int, title: String, file: UploadFile): Resource<PlainteSortieAttachment>
    suspend fun updateSortieAttachmentTitle(plainteSortieId: Int, attachId: Int, title: String): Resource<PlainteSortieAttachment>
    suspend fun deleteSortieAttachment(plainteSortieId: Int, attachId: Int): Resource<Unit>
}
