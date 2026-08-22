package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.CorrespondanceAttachment

/** Input data for creating/updating a correspondance record. */
data class CorrespondanceFormData(
    val dateCorrespondance: String,
    val heureEnregistrement: String,
    val sens: String,
    val reference: String,
    val emetteurDestinataire: String,
    val objet: String,
    val statut: String?
)

interface CorrespondanceRepository {
    suspend fun getCorrespondanceList(
        sens: String? = null,
        statut: String? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<Correspondance>>
    suspend fun getCorrespondance(id: Int): Resource<Correspondance>
    suspend fun createCorrespondance(data: CorrespondanceFormData): Resource<Correspondance>
    suspend fun updateCorrespondance(id: Int, data: CorrespondanceFormData): Resource<Correspondance>
    suspend fun deleteCorrespondance(id: Int): Resource<Unit>

    suspend fun getAttachments(correspondanceId: Int): Resource<List<CorrespondanceAttachment>>
    suspend fun addAttachment(correspondanceId: Int, title: String, file: UploadFile): Resource<CorrespondanceAttachment>
    suspend fun updateAttachmentTitle(correspondanceId: Int, attachId: Int, title: String): Resource<CorrespondanceAttachment>
    suspend fun deleteAttachment(correspondanceId: Int, attachId: Int): Resource<Unit>
}
