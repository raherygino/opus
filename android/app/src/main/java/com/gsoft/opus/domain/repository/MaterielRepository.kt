package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.model.TypeMateriel

/** Input data for creating/updating a type de matériel. */
data class TypeMaterielFormData(
    val nom: String,
    val description: String? = null
)

/** One material line in an assignment. */
data class AffectationMaterielLigneFormData(
    val typeMaterielId: Int,
    val etatEmport: String? = null
)

/** Input data for creating/updating an affectation de matériel. */
data class AffectationMaterielFormData(
    val agentPersonnelId: Int,
    val datePerception: String,
    val heurePerception: String,
    val observations: String? = null,
    val lignes: List<AffectationMaterielLigneFormData>,
    /** Agent code secret — required on create, verified server-side. */
    val codeSecret: String? = null,
    /** Optional SVG signature captured after verification. */
    val signatureSvg: String? = null
)

/** The fields of the reintegration transition. */
data class ReintegrationMaterielData(
    val dateReintegration: String,
    val heureReintegration: String,
    /** Map of ligneId → etat_reintegration (one per material). */
    val ligneEtats: Map<Int, String>
)

interface MaterielRepository {
    // TypeMateriel
    suspend fun getTypeMaterielList(search: String? = null): Resource<List<TypeMateriel>>
    suspend fun getTypeMateriel(id: Int): Resource<TypeMateriel>
    suspend fun createTypeMateriel(data: TypeMaterielFormData): Resource<TypeMateriel>
    suspend fun updateTypeMateriel(id: Int, data: TypeMaterielFormData): Resource<TypeMateriel>
    suspend fun deleteTypeMateriel(id: Int): Resource<Unit>

    // AffectationMateriel
    suspend fun getAffectationMaterielList(
        search: String? = null,
        statut: String? = null,
        agentPersonnelId: Int? = null
    ): Resource<List<AffectationMateriel>>
    suspend fun getAffectationMateriel(id: Int): Resource<AffectationMateriel>
    suspend fun createAffectationMateriel(data: AffectationMaterielFormData): Resource<AffectationMateriel>
    suspend fun updateAffectationMateriel(id: Int, data: AffectationMaterielFormData): Resource<AffectationMateriel>
    suspend fun reintegrateAffectationMateriel(id: Int, data: ReintegrationMaterielData): Resource<AffectationMateriel>
    suspend fun deleteAffectationMateriel(id: Int): Resource<Unit>
}
