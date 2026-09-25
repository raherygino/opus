package com.gsoft.opus.presentation.sgdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.model.DispositifExceptionnel
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.model.RassemblementJournalier
import com.gsoft.opus.domain.repository.ActiviteRepository
import com.gsoft.opus.domain.repository.DispositifExceptionnelRepository
import com.gsoft.opus.domain.repository.EvenementSurvenuRepository
import com.gsoft.opus.domain.repository.RassemblementJournalierRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

const val SG_DASH_RASSEMBLEMENT = "sg_rassemblement_journalier"
const val SG_DASH_EVENEMENT = "sg_evenement_survenu"
const val SG_DASH_ACTIVITE = "sg_activite"
const val SG_DASH_DISPOSITIF = "sg_dispositif_exceptionnel"

/** A single merged activity entry shown in the "Activité récente" list. */
data class SgActivityItem(
    val id: String,
    val action: String,
    val createdAt: String?,
    val type: SgActivityType,
    val targetId: Int
)

enum class SgActivityType { RASSEMBLEMENT, EVENEMENT, ACTIVITE, DISPOSITIF }

data class SgDashboardUiState(
    val isLoading: Boolean = true,
    val rassemblements: List<RassemblementJournalier> = emptyList(),
    val evenements: List<EvenementSurvenu> = emptyList(),
    val activites: List<Activite> = emptyList(),
    val dispositifs: List<DispositifExceptionnel> = emptyList(),
    val canViewRassemblement: Boolean = false,
    val canViewEvenement: Boolean = false,
    val canViewActivite: Boolean = false,
    val canViewDispositif: Boolean = false,
    val canCreateRassemblement: Boolean = false,
    val canCreateEvenement: Boolean = false,
    val canCreateActivite: Boolean = false,
    val canCreateDispositif: Boolean = false,
    val errorMessage: String? = null
) {
    private val today: String get() = LocalDate.now().toString()

    /** Événements enregistrés aujourd'hui. */
    val evenementsToday: Int get() = evenements.count { it.dateEvenement == today }

    /** Activités enregistrées aujourd'hui. */
    val activitesToday: Int get() = activites.count { it.dateActivite == today }

    /** Dispositifs dont la période couvre aujourd'hui. */
    val dispositifsActifs: Int
        get() = dispositifs.count { it.dateDebut <= today && it.dateFin >= today }

    /** Recent items merged across all SG modules, sorted by createdAt descending. */
    val recentActivity: List<SgActivityItem>
        get() {
            val items = mutableListOf<SgActivityItem>()
            rassemblements.forEach { r ->
                items.add(
                    SgActivityItem(
                        id = "rassemblement-${r.id}",
                        action = "Rassemblement ${r.dateRassemblement} — ${r.brigadeService}",
                        createdAt = r.createdAt,
                        type = SgActivityType.RASSEMBLEMENT,
                        targetId = r.id
                    )
                )
            }
            evenements.forEach { e ->
                items.add(
                    SgActivityItem(
                        id = "evenement-${e.id}",
                        action = "Événement ${e.typeLabel} — ${e.lieuExact}",
                        createdAt = e.createdAt,
                        type = SgActivityType.EVENEMENT,
                        targetId = e.id
                    )
                )
            }
            activites.forEach { a ->
                items.add(
                    SgActivityItem(
                        id = "activite-${a.id}",
                        action = "Activité ${a.dateActivite} — ${a.natureIntervention ?: "Patrouille"}",
                        createdAt = a.createdAt,
                        type = SgActivityType.ACTIVITE,
                        targetId = a.id
                    )
                )
            }
            dispositifs.forEach { d ->
                items.add(
                    SgActivityItem(
                        id = "dispositif-${d.id}",
                        action = "Dispositif exceptionnel — ${d.natureEvenement}",
                        createdAt = d.createdAt,
                        type = SgActivityType.DISPOSITIF,
                        targetId = d.id
                    )
                )
            }
            return items.sortedByDescending { it.createdAt ?: "" }.take(8)
        }
}

@HiltViewModel
class SgDashboardViewModel @Inject constructor(
    private val rassemblementRepository: RassemblementJournalierRepository,
    private val evenementRepository: EvenementSurvenuRepository,
    private val activiteRepository: ActiviteRepository,
    private val dispositifRepository: DispositifExceptionnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SgDashboardUiState())
    val state: StateFlow<SgDashboardUiState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() = loadDashboard()

    private fun loadDashboard() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val canViewRassemblement = hasPermission(user, SG_DASH_RASSEMBLEMENT, PermissionAction.VIEW)
            val canViewEvenement = hasPermission(user, SG_DASH_EVENEMENT, PermissionAction.VIEW)
            val canViewActivite = hasPermission(user, SG_DASH_ACTIVITE, PermissionAction.VIEW)
            val canViewDispositif = hasPermission(user, SG_DASH_DISPOSITIF, PermissionAction.VIEW)

            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    canViewRassemblement = canViewRassemblement,
                    canViewEvenement = canViewEvenement,
                    canViewActivite = canViewActivite,
                    canViewDispositif = canViewDispositif,
                    canCreateRassemblement = hasPermission(user, SG_DASH_RASSEMBLEMENT, PermissionAction.CREATE),
                    canCreateEvenement = hasPermission(user, SG_DASH_EVENEMENT, PermissionAction.CREATE),
                    canCreateActivite = hasPermission(user, SG_DASH_ACTIVITE, PermissionAction.CREATE),
                    canCreateDispositif = hasPermission(user, SG_DASH_DISPOSITIF, PermissionAction.CREATE)
                )
            }

            // Fetch each module in parallel; a failure in one doesn't block the others.
            val rassemblementDeferred = async {
                if (canViewRassemblement) rassemblementRepository.getRassemblementList() else Resource.success(emptyList<RassemblementJournalier>())
            }
            val evenementDeferred = async {
                if (canViewEvenement) evenementRepository.getEvenementList() else Resource.success(emptyList<EvenementSurvenu>())
            }
            val activiteDeferred = async {
                if (canViewActivite) activiteRepository.getActiviteList() else Resource.success(emptyList<Activite>())
            }
            val dispositifDeferred = async {
                if (canViewDispositif) dispositifRepository.getDispositifList() else Resource.success(emptyList<DispositifExceptionnel>())
            }

            val rassemblementsRes = rassemblementDeferred.await()
            val evenementsRes = evenementDeferred.await()
            val activitesRes = activiteDeferred.await()
            val dispositifsRes = dispositifDeferred.await()
            val anyError = listOf(rassemblementsRes, evenementsRes, activitesRes, dispositifsRes)
                .any { it is Resource.Error }

            _state.update {
                it.copy(
                    isLoading = false,
                    rassemblements = (rassemblementsRes as? Resource.Success)?.data ?: emptyList(),
                    evenements = (evenementsRes as? Resource.Success)?.data ?: emptyList(),
                    activites = (activitesRes as? Resource.Success)?.data ?: emptyList(),
                    dispositifs = (dispositifsRes as? Resource.Success)?.data ?: emptyList(),
                    errorMessage = if (anyError) "Certaines données n'ont pas pu être chargées" else null
                )
            }
        }
    }
}
