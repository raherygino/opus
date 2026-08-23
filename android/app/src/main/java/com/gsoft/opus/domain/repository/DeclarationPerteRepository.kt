package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.DeclarationPerteAttachment

/** Input data for creating/updating a déclaration de perte record. */
data class DeclarationPerteFormData(
    val dateDeclaration: String,
    val heureDeclaration: String,
    val identiteDeclarant: String,
    val natureObjet: String,
    val descriptionObjet: String,
    val datePerte: String,
    val lieuPerte: String,
    val numeroAttestation: String,
    val nomAgent: String
)

interface DeclarationPerteRepository {
    suspend fun getDeclarationPerteList(
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<DeclarationPerte>>
    suspend fun getDeclarationPerte(id: Int): Resource<DeclarationPerte>
    suspend fun createDeclarationPerte(data: DeclarationPerteFormData): Resource<DeclarationPerte>
    suspend fun updateDeclarationPerte(id: Int, data: DeclarationPerteFormData): Resource<DeclarationPerte>
    suspend fun deleteDeclarationPerte(id: Int): Resource<Unit>

    suspend fun getAttachments(declarationId: Int): Resource<List<DeclarationPerteAttachment>>
    suspend fun addAttachment(declarationId: Int, title: String, file: UploadFile): Resource<DeclarationPerteAttachment>
    suspend fun updateAttachmentTitle(declarationId: Int, attachId: Int, title: String): Resource<DeclarationPerteAttachment>
    suspend fun deleteAttachment(declarationId: Int, attachId: Int): Resource<Unit>
}
