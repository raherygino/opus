package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.model.ActiviteAttachment

/** Input data for creating/updating an activité (patrouille / intervention). */
data class ActiviteFormData(
    val dateActivite: String,
    val heureActivite: String,
    /** Itinerary per patrol mode — null means the mode was not selected. */
    val patrouilleDiurneMotoriseeItineraire: String? = null,
    val patrouilleDiurnePedestreItineraire: String? = null,
    val patrouilleDiurnePorteeItineraire: String? = null,
    val patrouilleNocturneMotoriseeItineraire: String? = null,
    val patrouilleNocturnePedestreItineraire: String? = null,
    val patrouilleNocturnePorteeItineraire: String? = null,
    val operationCiblee: String? = null,
    val faitsConstates: String? = null,
    val compteRenduHierarchie: String? = null,
    val conduiteATenir: String? = null,
    val natureIntervention: String? = null,
    val suitesDonnees: String? = null,
    /** GPS position captured at record time (mobile only). */
    val latitude: Double? = null,
    val longitude: Double? = null
)

interface ActiviteRepository {
    suspend fun getActiviteList(search: String? = null): Resource<List<Activite>>
    suspend fun getActivite(id: Int): Resource<Activite>
    suspend fun createActivite(data: ActiviteFormData): Resource<Activite>
    suspend fun updateActivite(id: Int, data: ActiviteFormData): Resource<Activite>
    suspend fun deleteActivite(id: Int): Resource<Unit>

    suspend fun getAttachments(activiteId: Int): Resource<List<ActiviteAttachment>>
    suspend fun addAttachment(activiteId: Int, title: String, file: UploadFile): Resource<ActiviteAttachment>
    suspend fun updateAttachmentTitle(activiteId: Int, attachId: Int, title: String): Resource<ActiviteAttachment>
    suspend fun deleteAttachment(activiteId: Int, attachId: Int): Resource<Unit>
}
