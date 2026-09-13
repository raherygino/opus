package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.MainCourante
import com.gsoft.opus.domain.model.MainCouranteAttachment

/** Input data for creating/updating a main courante entry. */
data class MainCouranteFormData(
    val dateEvenement: String,
    val heureEvenement: String,
    val categorie: String,
    val description: String,
    val origine: String
)

interface MainCouranteRepository {
    suspend fun getMainCouranteList(
        origine: String? = null,
        categorie: String? = null,
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null
    ): Resource<List<MainCourante>>
    suspend fun getMainCourante(id: Int): Resource<MainCourante>
    suspend fun createMainCourante(data: MainCouranteFormData): Resource<MainCourante>
    suspend fun updateMainCourante(id: Int, data: MainCouranteFormData): Resource<MainCourante>
    suspend fun deleteMainCourante(id: Int): Resource<Unit>

    suspend fun getAttachments(mainCouranteId: Int): Resource<List<MainCouranteAttachment>>
    suspend fun addAttachment(mainCouranteId: Int, title: String, file: UploadFile): Resource<MainCouranteAttachment>
    suspend fun updateAttachmentTitle(mainCouranteId: Int, attachId: Int, title: String): Resource<MainCouranteAttachment>
    suspend fun deleteAttachment(mainCouranteId: Int, attachId: Int): Resource<Unit>
}
