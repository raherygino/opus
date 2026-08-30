package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.model.ArmementAttachment

/** Input data for creating/updating an armement perception record. */
data class ArmementFormData(
    val datePerception: String,
    val heurePerception: String,
    val agentPreneurPersonnelId: Int,
    val typeArme: String,
    val matriculeArme: String,
    val munitions: Int?,
    val secteurMission: String,
    val etatPerception: String,
    /** The agent preneur's code secret — verified server-side on create. */
    val codeSecret: String? = null,
    /** SVG vector data of the agent signature captured after verification. */
    val signatureSvg: String? = null
)

/** The three fields of the reintegration transition. */
data class ReintegrationData(
    val heureReintegration: String,
    val etatReintegration: String,
    val munitionsConsommees: Int
)

interface ArmementRepository {
    suspend fun getArmementList(
        search: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        statut: String? = null
    ): Resource<List<Armement>>
    suspend fun getArmement(id: Int): Resource<Armement>
    suspend fun createArmement(data: ArmementFormData): Resource<Armement>
    suspend fun updateArmement(id: Int, data: ArmementFormData): Resource<Armement>
    suspend fun reintegrateArmement(id: Int, data: ReintegrationData): Resource<Armement>
    suspend fun deleteArmement(id: Int): Resource<Unit>

    suspend fun getAttachments(armementId: Int): Resource<List<ArmementAttachment>>
    suspend fun addAttachment(armementId: Int, title: String, file: UploadFile): Resource<ArmementAttachment>
    suspend fun updateAttachmentTitle(armementId: Int, attachId: Int, title: String): Resource<ArmementAttachment>
    suspend fun deleteAttachment(armementId: Int, attachId: Int): Resource<Unit>
}
