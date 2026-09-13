package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.model.MaterielRoulantAttachment

/** Input data for creating/updating a matériel roulant perception. */
data class MaterielRoulantFormData(
    val datePerception: String,
    val heurePerception: String,
    val typeMateriel: String,
    val numeroImmatriculation: String? = null,
    val descriptionVehicule: String? = null,
    val agentConducteurPersonnelId: Int,
    val chefDeBordPersonnelId: Int? = null,
    val kilometrageDepart: String? = null,
    val niveauCarburantDepart: String? = null,
    // Agent verification (create only — set at perception time, one-way).
    val codeSecret: String? = null,
    val signatureSvg: String? = null
)

/** The fields of the reintegration transition. */
data class ReintegrationMaterielRoulantData(
    val dateReintegration: String,
    val heureReintegration: String,
    val kilometrageRetour: String,
    val niveauCarburantRetour: String? = null,
    val observationsTechniques: String? = null,
    val defaillances: String? = null
)

interface MaterielRoulantRepository {
    suspend fun getMaterielRoulantList(
        search: String? = null,
        statut: String? = null,
        typeMateriel: String? = null
    ): Resource<List<MaterielRoulant>>

    suspend fun getMaterielRoulant(id: Int): Resource<MaterielRoulant>
    suspend fun createMaterielRoulant(data: MaterielRoulantFormData): Resource<MaterielRoulant>
    suspend fun updateMaterielRoulant(id: Int, data: MaterielRoulantFormData): Resource<MaterielRoulant>
    suspend fun reintegrateMaterielRoulant(id: Int, data: ReintegrationMaterielRoulantData): Resource<MaterielRoulant>
    suspend fun deleteMaterielRoulant(id: Int): Resource<Unit>

    suspend fun getAttachments(materielRoulantId: Int): Resource<List<MaterielRoulantAttachment>>
    suspend fun addAttachment(materielRoulantId: Int, title: String, file: UploadFile): Resource<MaterielRoulantAttachment>
    suspend fun updateAttachmentTitle(materielRoulantId: Int, attachId: Int, title: String): Resource<MaterielRoulantAttachment>
    suspend fun deleteAttachment(materielRoulantId: Int, attachId: Int): Resource<Unit>
}
