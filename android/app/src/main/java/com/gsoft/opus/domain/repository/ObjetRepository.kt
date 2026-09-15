package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetSaisiAttachment
import com.gsoft.opus.domain.model.ObjetTrouve
import com.gsoft.opus.domain.model.ObjetTrouveAttachment

/** Input data for creating/updating an objet saisi. */
data class ObjetSaisiFormData(
    val numeroDossier: String?,
    val motif: String,
    val typeObjet: String,
    val proprietaire: String?
)

/** Input data for creating/updating an objet trouvé. */
data class ObjetTrouveFormData(
    val affaire: String,
    val motifDecouverte: String,
    val restitution: Boolean
)

interface ObjetSaisiRepository {
    suspend fun getObjetSaisiList(
        typeObjet: String? = null,
        search: String? = null
    ): Resource<List<ObjetSaisi>>
    suspend fun getObjetSaisi(id: Int): Resource<ObjetSaisi>
    suspend fun createObjetSaisi(data: ObjetSaisiFormData): Resource<ObjetSaisi>
    suspend fun updateObjetSaisi(id: Int, data: ObjetSaisiFormData): Resource<ObjetSaisi>
    suspend fun deleteObjetSaisi(id: Int): Resource<Unit>

    suspend fun getObjetSaisiAttachments(objetId: Int): Resource<List<ObjetSaisiAttachment>>
    suspend fun addObjetSaisiAttachment(objetId: Int, title: String, file: UploadFile): Resource<ObjetSaisiAttachment>
    suspend fun updateObjetSaisiAttachmentTitle(objetId: Int, attachId: Int, title: String): Resource<ObjetSaisiAttachment>
    suspend fun deleteObjetSaisiAttachment(objetId: Int, attachId: Int): Resource<Unit>
}

interface ObjetTrouveRepository {
    suspend fun getObjetTrouveList(
        motifDecouverte: String? = null,
        restitution: Boolean? = null,
        search: String? = null
    ): Resource<List<ObjetTrouve>>
    suspend fun getObjetTrouve(id: Int): Resource<ObjetTrouve>
    suspend fun createObjetTrouve(data: ObjetTrouveFormData): Resource<ObjetTrouve>
    suspend fun updateObjetTrouve(id: Int, data: ObjetTrouveFormData): Resource<ObjetTrouve>
    suspend fun deleteObjetTrouve(id: Int): Resource<Unit>

    suspend fun getObjetTrouveAttachments(objetId: Int): Resource<List<ObjetTrouveAttachment>>
    suspend fun addObjetTrouveAttachment(objetId: Int, title: String, file: UploadFile): Resource<ObjetTrouveAttachment>
    suspend fun updateObjetTrouveAttachmentTitle(objetId: Int, attachId: Int, title: String): Resource<ObjetTrouveAttachment>
    suspend fun deleteObjetTrouveAttachment(objetId: Int, attachId: Int): Resource<Unit>
}
