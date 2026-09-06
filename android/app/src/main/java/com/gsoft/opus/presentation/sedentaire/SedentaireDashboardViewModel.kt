package com.gsoft.opus.presentation.sedentaire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.repository.PassationRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val DASH_MODULE_CORRESPONDANCE = "sedentaire_secretariat_correspondance"
const val DASH_MODULE_DECLARATION_PERTE = "sedentaire_secretariat_declaration_perte"
const val DASH_MODULE_PASSATION = "sedentaire_poste_passation"
const val DASH_MODULE_PERSONNEL = "personnel"

/** A single merged activity entry shown in the "Activité récente" list. */
data class ActivityItem(
    val id: String,
    val action: String,
    val createdAt: String?,
    val type: ActivityType,
    val targetId: Int
)

enum class ActivityType { CORRESPONDANCE, DECLARATION, PASSATION, PERSONNEL }

data class SedentaireDashboardUiState(
    val isLoading: Boolean = true,
    val correspondances: List<Correspondance> = emptyList(),
    val declarations: List<DeclarationPerte> = emptyList(),
    val passations: List<Passation> = emptyList(),
    val personnel: List<Personnel> = emptyList(),
    val canViewCorrespondance: Boolean = false,
    val canViewDeclaration: Boolean = false,
    val canViewPassation: Boolean = false,
    val canViewPersonnel: Boolean = false,
    val canCreateCorrespondance: Boolean = false,
    val canCreateDeclaration: Boolean = false,
    val canCreatePassation: Boolean = false,
    val canCreatePersonnel: Boolean = false,
    val errorMessage: String? = null
) {
    /** Recent items merged across all modules, sorted by createdAt descending. */
    val recentActivity: List<ActivityItem>
        get() {
            val items = mutableListOf<ActivityItem>()
            correspondances.forEach { c ->
                items.add(
                    ActivityItem(
                        id = "corr-${c.id}",
                        action = "Correspondance ${c.sens.lowercase()} #${c.id} — ${c.objet.ifBlank { c.reference }.ifBlank { "sans objet" }}",
                        createdAt = c.createdAt,
                        type = ActivityType.CORRESPONDANCE,
                        targetId = c.id
                    )
                )
            }
            declarations.forEach { d ->
                items.add(
                    ActivityItem(
                        id = "decl-${d.id}",
                        action = "Déclaration de perte #${d.id} — ${d.natureObjet.ifBlank { d.identiteDeclarant }}",
                        createdAt = d.createdAt,
                        type = ActivityType.DECLARATION,
                        targetId = d.id
                    )
                )
            }
            passations.forEach { p ->
                val arrow = if (p.chefDescendantLastname.isNullOrBlank() && p.chefMontantLastname.isNullOrBlank()) {
                    ""
                } else {
                    " → "
                }
                items.add(
                    ActivityItem(
                        id = "pass-${p.id}",
                        action = "Passation #${p.id} — ${p.chefDescendantLastname ?: ""}$arrow${p.chefMontantLastname ?: ""}".trim(),
                        createdAt = p.createdAt,
                        type = ActivityType.PASSATION,
                        targetId = p.id
                    )
                )
            }
            personnel.forEach { p ->
                items.add(
                    ActivityItem(
                        id = "pers-${p.id}",
                        action = "Personnel enregistré — ${p.grade} ${p.lastname} ${p.firstname}".trim(),
                        createdAt = p.createdAt,
                        type = ActivityType.PERSONNEL,
                        targetId = p.id
                    )
                )
            }
            return items.sortedByDescending { it.createdAt ?: "" }.take(8)
        }
}

@HiltViewModel
class SedentaireDashboardViewModel @Inject constructor(
    private val correspondanceRepository: CorrespondanceRepository,
    private val declarationPerteRepository: DeclarationPerteRepository,
    private val passationRepository: PassationRepository,
    private val personnelRepository: PersonnelRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SedentaireDashboardUiState())
    val state: StateFlow<SedentaireDashboardUiState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() = loadDashboard()

    private fun loadDashboard() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val canViewCorr = hasPermission(user, DASH_MODULE_CORRESPONDANCE, PermissionAction.VIEW)
            val canViewDecl = hasPermission(user, DASH_MODULE_DECLARATION_PERTE, PermissionAction.VIEW)
            val canViewPass = hasPermission(user, DASH_MODULE_PASSATION, PermissionAction.VIEW)
            val canViewPers = hasPermission(user, DASH_MODULE_PERSONNEL, PermissionAction.VIEW)

            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    canViewCorrespondance = canViewCorr,
                    canViewDeclaration = canViewDecl,
                    canViewPassation = canViewPass,
                    canViewPersonnel = canViewPers,
                    canCreateCorrespondance = hasPermission(user, DASH_MODULE_CORRESPONDANCE, PermissionAction.CREATE),
                    canCreateDeclaration = hasPermission(user, DASH_MODULE_DECLARATION_PERTE, PermissionAction.CREATE),
                    canCreatePassation = hasPermission(user, DASH_MODULE_PASSATION, PermissionAction.CREATE),
                    canCreatePersonnel = hasPermission(user, DASH_MODULE_PERSONNEL, PermissionAction.CREATE)
                )
            }

            // Fetch each module in parallel; a failure in one doesn't block the others.
            val corrDeferred = async {
                if (canViewCorr) correspondanceRepository.getCorrespondanceList() else Resource.success(emptyList<Correspondance>())
            }
            val declDeferred = async {
                if (canViewDecl) declarationPerteRepository.getDeclarationPerteList() else Resource.success(emptyList<DeclarationPerte>())
            }
            val passDeferred = async {
                if (canViewPass) passationRepository.getPassationList() else Resource.success(emptyList<Passation>())
            }
            val persDeferred = async {
                if (canViewPers) personnelRepository.getPersonnelList() else Resource.success(emptyList<Personnel>())
            }

            val corrResult = corrDeferred.await()
            val declResult = declDeferred.await()
            val passResult = passDeferred.await()
            val persResult = persDeferred.await()

            val errors = mutableListOf<String>()
            val corr = (corrResult as? Resource.Success)?.data ?: emptyList()
            if (corrResult is Resource.Error && canViewCorr) errors.add("Correspondances indisponibles")
            val decl = (declResult as? Resource.Success)?.data ?: emptyList()
            if (declResult is Resource.Error && canViewDecl) errors.add("Déclarations de perte indisponibles")
            val pass = (passResult as? Resource.Success)?.data ?: emptyList()
            if (passResult is Resource.Error && canViewPass) errors.add("Passations indisponibles")
            val pers = (persResult as? Resource.Success)?.data ?: emptyList()
            if (persResult is Resource.Error && canViewPers) errors.add("Personnel indisponible")

            _state.update {
                it.copy(
                    isLoading = false,
                    correspondances = corr,
                    declarations = decl,
                    passations = pass,
                    personnel = pers,
                    errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                )
            }
        }
    }
}
