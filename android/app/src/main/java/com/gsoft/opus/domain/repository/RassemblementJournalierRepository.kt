package com.gsoft.opus.domain.repository

import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.RassemblementJournalier

/** One repartition row — Diurne and Nocturne share the same structure. */
data class RepartitionSecteurInput(
    val type: String,
    val secteur: String,
    val effectifEngage: String? = null,
    val chefElementContact: String? = null,
    val controleContact: String? = null,
    val materielsArmements: String? = null,
    val missions: String? = null
)

/** Input data for creating/updating a rassemblement journalier. */
data class RassemblementJournalierFormData(
    val dateRassemblement: String,
    val heureRassemblement: String,
    val brigadeService: String,
    val officierPermanence: String? = null,
    val inspecteurPermanence: String? = null,
    val chefPoste: String? = null,
    val instructionsAutorite: String? = null,
    val effectifTheorique: Int = 0,
    val present: Int = 0,
    val absent: Int = 0,
    val motifAbsence: String? = null,
    val repartitions: List<RepartitionSecteurInput> = emptyList()
)

interface RassemblementJournalierRepository {
    suspend fun getRassemblementList(search: String? = null): Resource<List<RassemblementJournalier>>
    suspend fun getRassemblement(id: Int): Resource<RassemblementJournalier>
    suspend fun createRassemblement(data: RassemblementJournalierFormData): Resource<RassemblementJournalier>
    suspend fun updateRassemblement(id: Int, data: RassemblementJournalierFormData): Resource<RassemblementJournalier>
    suspend fun deleteRassemblement(id: Int): Resource<Unit>
}
