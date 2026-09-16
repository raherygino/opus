package com.gsoft.opus.presentation.pjdashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Arrestation
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.model.GardeAVue
import com.gsoft.opus.domain.model.Mandat
import com.gsoft.opus.domain.model.ObjetSaisi
import com.gsoft.opus.domain.model.ObjetTrouve
import com.gsoft.opus.domain.model.Perquisition
import com.gsoft.opus.domain.model.PersonneRecherchee
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteEntreeSummary
import com.gsoft.opus.domain.model.RenseignementPj
import com.gsoft.opus.domain.model.Requisition
import com.gsoft.opus.domain.repository.ArrestationRepository
import com.gsoft.opus.domain.repository.ConvocationRepository
import com.gsoft.opus.domain.repository.GardeAVueRepository
import com.gsoft.opus.domain.repository.MandatRepository
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.repository.ObjetTrouveRepository
import com.gsoft.opus.domain.repository.PerquisitionRepository
import com.gsoft.opus.domain.repository.PersonneRechercheeRepository
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.repository.RequisitionRepository
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PJ_DASH_PLAINTE = "pj_plainte"
const val PJ_DASH_ENQUETE = "pj_enquete"
const val PJ_DASH_MANDAT = "pj_mandat"
const val PJ_DASH_CONVOCATION = "pj_convocation"
const val PJ_DASH_ARRESTATION = "pj_arrestation"
const val PJ_DASH_GAV = "pj_gav"
const val PJ_DASH_REQUISITION = "pj_requisition"
const val PJ_DASH_PERSONNE_RECHERCHEE = "pj_personne_recherchee"
const val PJ_DASH_OBJETS = "pj_objets"
const val PJ_DASH_PERQUISITION = "pj_perquisition"
const val PJ_DASH_DEFERREMENT = "pj_deferrement"
const val PJ_DASH_RENSEIGNEMENT = "pj_renseignement"

/** A single merged activity entry shown in the "Activité récente" list. */
data class PjActivityItem(
    val id: String,
    val action: String,
    val createdAt: String?,
    val type: PjActivityType,
    val targetId: Int
)

enum class PjActivityType {
    PLAINTE, MANDAT, CONVOCATION, ARRESTATION, GAV, REQUISITION,
    PERSONNE_RECHERCHEE, OBJET_SAISI, OBJET_TROUVE, PERQUISITION, RENSEIGNEMENT
}

data class PjDashboardUiState(
    val isLoading: Boolean = true,
    val plaintes: List<PlainteEntree> = emptyList(),
    val plaintesPending: List<PlainteEntreeSummary> = emptyList(),
    val mandats: List<Mandat> = emptyList(),
    val convocations: List<Convocation> = emptyList(),
    val arrestations: List<Arrestation> = emptyList(),
    val gavs: List<GardeAVue> = emptyList(),
    val requisitions: List<Requisition> = emptyList(),
    val personnesRecherchees: List<PersonneRecherchee> = emptyList(),
    val objetsSaisis: List<ObjetSaisi> = emptyList(),
    val objetsTrouves: List<ObjetTrouve> = emptyList(),
    val perquisitions: List<Perquisition> = emptyList(),
    val renseignements: List<RenseignementPj> = emptyList(),
    val canViewPlainte: Boolean = false,
    val canViewEnquete: Boolean = false,
    val canViewMandat: Boolean = false,
    val canViewConvocation: Boolean = false,
    val canViewArrestation: Boolean = false,
    val canViewGav: Boolean = false,
    val canViewRequisition: Boolean = false,
    val canViewPersonneRecherchee: Boolean = false,
    val canViewObjets: Boolean = false,
    val canViewPerquisition: Boolean = false,
    val canViewDeferrement: Boolean = false,
    val canViewRenseignement: Boolean = false,
    val canCreatePlainte: Boolean = false,
    val canCreateEnquete: Boolean = false,
    val canCreateMandat: Boolean = false,
    val canCreateConvocation: Boolean = false,
    val canCreateArrestation: Boolean = false,
    val canCreateGav: Boolean = false,
    val canCreateRequisition: Boolean = false,
    val canCreatePersonneRecherchee: Boolean = false,
    val canCreateObjets: Boolean = false,
    val canCreatePerquisition: Boolean = false,
    val canCreateDeferrement: Boolean = false,
    val canCreateRenseignement: Boolean = false,
    val errorMessage: String? = null
) {
    /** Recent items merged across all PJ modules, sorted by createdAt descending. */
    val recentActivity: List<PjActivityItem>
        get() {
            val items = mutableListOf<PjActivityItem>()
            plaintes.forEach { p ->
                items.add(
                    PjActivityItem(
                        id = "plainte-${p.id}",
                        action = "Plainte ${plainteTypeLabel(p.type)} — ${p.numeroDossier} · ${p.infraction ?: p.miseEnCause ?: ""}".trim(),
                        createdAt = p.createdAt,
                        type = PjActivityType.PLAINTE,
                        targetId = p.id
                    )
                )
            }
            mandats.forEach { m ->
                items.add(
                    PjActivityItem(
                        id = "mandat-${m.id}",
                        action = "Mandat ${m.numero} — ${m.personneNom}",
                        createdAt = m.createdAt,
                        type = PjActivityType.MANDAT,
                        targetId = m.id
                    )
                )
            }
            convocations.forEach { c ->
                items.add(
                    PjActivityItem(
                        id = "convocation-${c.id}",
                        action = "Convocation ${c.numero} — ${c.nom}",
                        createdAt = c.createdAt,
                        type = PjActivityType.CONVOCATION,
                        targetId = c.id
                    )
                )
            }
            arrestations.forEach { a ->
                items.add(
                    PjActivityItem(
                        id = "arrestation-${a.id}",
                        action = "Arrestation ${a.numero} — ${a.personneNom}",
                        createdAt = a.createdAt,
                        type = PjActivityType.ARRESTATION,
                        targetId = a.id
                    )
                )
            }
            gavs.forEach { g ->
                items.add(
                    PjActivityItem(
                        id = "gav-${g.id}",
                        action = "GAV — ${g.nom}${if (g.prenoms.isNullOrBlank()) "" else " " + g.prenoms}".trim(),
                        createdAt = g.createdAt,
                        type = PjActivityType.GAV,
                        targetId = g.id
                    )
                )
            }
            requisitions.forEach { r ->
                items.add(
                    PjActivityItem(
                        id = "requisition-${r.id}",
                        action = "Réquisition ${r.numero} — ${r.affaire}",
                        createdAt = r.createdAt,
                        type = PjActivityType.REQUISITION,
                        targetId = r.id
                    )
                )
            }
            personnesRecherchees.forEach { p ->
                items.add(
                    PjActivityItem(
                        id = "recherchee-${p.id}",
                        action = "Personne recherchée — ${p.nom}",
                        createdAt = p.createdAt,
                        type = PjActivityType.PERSONNE_RECHERCHEE,
                        targetId = p.id
                    )
                )
            }
            objetsSaisis.forEach { o ->
                items.add(
                    PjActivityItem(
                        id = "objet-saisi-${o.id}",
                        action = "Objet saisi — ${o.motif}${if (o.proprietaire.isNullOrBlank()) "" else " · " + o.proprietaire}".trim(),
                        createdAt = o.createdAt,
                        type = PjActivityType.OBJET_SAISI,
                        targetId = o.id
                    )
                )
            }
            objetsTrouves.forEach { o ->
                items.add(
                    PjActivityItem(
                        id = "objet-trouve-${o.id}",
                        action = "Objet trouvé — ${o.affaire}",
                        createdAt = o.createdAt,
                        type = PjActivityType.OBJET_TROUVE,
                        targetId = o.id
                    )
                )
            }
            perquisitions.forEach { p ->
                items.add(
                    PjActivityItem(
                        id = "perquisition-${p.id}",
                        action = "Perquisition ${p.numero} — ${p.affaire}",
                        createdAt = p.createdAt,
                        type = PjActivityType.PERQUISITION,
                        targetId = p.id
                    )
                )
            }
            renseignements.forEach { r ->
                items.add(
                    PjActivityItem(
                        id = "renseignement-${r.id}",
                        action = "Renseignement PJ — ${r.natureInfraction}",
                        createdAt = r.createdAt,
                        type = PjActivityType.RENSEIGNEMENT,
                        targetId = r.id
                    )
                )
            }
            return items.sortedByDescending { it.createdAt ?: "" }.take(8)
        }
}

private fun plainteTypeLabel(type: String): String = when (type) {
    "ST_PARQUET" -> "ST Parquet"
    "PLAINTE_DIRECTE" -> "Plainte directe"
    "RAPPORT_POLICE" -> "Rapport de police"
    else -> type
}

@HiltViewModel
class PjDashboardViewModel @Inject constructor(
    private val plainteRepository: PlainteRepository,
    private val mandatRepository: MandatRepository,
    private val convocationRepository: ConvocationRepository,
    private val arrestationRepository: ArrestationRepository,
    private val gardeAVueRepository: GardeAVueRepository,
    private val requisitionRepository: RequisitionRepository,
    private val personneRechercheeRepository: PersonneRechercheeRepository,
    private val objetSaisiRepository: ObjetSaisiRepository,
    private val objetTrouveRepository: ObjetTrouveRepository,
    private val perquisitionRepository: PerquisitionRepository,
    private val renseignementPjRepository: RenseignementPjRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PjDashboardUiState())
    val state: StateFlow<PjDashboardUiState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun refresh() = loadDashboard()

    private fun loadDashboard() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val canViewPlainte = hasPermission(user, PJ_DASH_PLAINTE, PermissionAction.VIEW)
            val canViewEnquete = hasPermission(user, PJ_DASH_ENQUETE, PermissionAction.VIEW)
            val canViewMandat = hasPermission(user, PJ_DASH_MANDAT, PermissionAction.VIEW)
            val canViewConvocation = hasPermission(user, PJ_DASH_CONVOCATION, PermissionAction.VIEW)
            val canViewArrestation = hasPermission(user, PJ_DASH_ARRESTATION, PermissionAction.VIEW)
            val canViewGav = hasPermission(user, PJ_DASH_GAV, PermissionAction.VIEW)
            val canViewRequisition = hasPermission(user, PJ_DASH_REQUISITION, PermissionAction.VIEW)
            val canViewPersonneRecherchee = hasPermission(user, PJ_DASH_PERSONNE_RECHERCHEE, PermissionAction.VIEW)
            val canViewObjets = hasPermission(user, PJ_DASH_OBJETS, PermissionAction.VIEW)
            val canViewPerquisition = hasPermission(user, PJ_DASH_PERQUISITION, PermissionAction.VIEW)
            val canViewDeferrement = hasPermission(user, PJ_DASH_DEFERREMENT, PermissionAction.VIEW)
            val canViewRenseignement = hasPermission(user, PJ_DASH_RENSEIGNEMENT, PermissionAction.VIEW)

            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    canViewPlainte = canViewPlainte,
                    canViewEnquete = canViewEnquete,
                    canViewMandat = canViewMandat,
                    canViewConvocation = canViewConvocation,
                    canViewArrestation = canViewArrestation,
                    canViewGav = canViewGav,
                    canViewRequisition = canViewRequisition,
                    canViewPersonneRecherchee = canViewPersonneRecherchee,
                    canViewObjets = canViewObjets,
                    canViewPerquisition = canViewPerquisition,
                    canViewDeferrement = canViewDeferrement,
                    canViewRenseignement = canViewRenseignement,
                    canCreatePlainte = hasPermission(user, PJ_DASH_PLAINTE, PermissionAction.CREATE),
                    canCreateEnquete = hasPermission(user, PJ_DASH_ENQUETE, PermissionAction.CREATE),
                    canCreateMandat = hasPermission(user, PJ_DASH_MANDAT, PermissionAction.CREATE),
                    canCreateConvocation = hasPermission(user, PJ_DASH_CONVOCATION, PermissionAction.CREATE),
                    canCreateArrestation = hasPermission(user, PJ_DASH_ARRESTATION, PermissionAction.CREATE),
                    canCreateGav = hasPermission(user, PJ_DASH_GAV, PermissionAction.CREATE),
                    canCreateRequisition = hasPermission(user, PJ_DASH_REQUISITION, PermissionAction.CREATE),
                    canCreatePersonneRecherchee = hasPermission(user, PJ_DASH_PERSONNE_RECHERCHEE, PermissionAction.CREATE),
                    canCreateObjets = hasPermission(user, PJ_DASH_OBJETS, PermissionAction.CREATE),
                    canCreatePerquisition = hasPermission(user, PJ_DASH_PERQUISITION, PermissionAction.CREATE),
                    canCreateDeferrement = hasPermission(user, PJ_DASH_DEFERREMENT, PermissionAction.CREATE),
                    canCreateRenseignement = hasPermission(user, PJ_DASH_RENSEIGNEMENT, PermissionAction.CREATE)
                )
            }

            // Fetch each module in parallel; a failure in one doesn't block the others.
            val plainteDeferred = async {
                if (canViewPlainte) plainteRepository.getPlainteEntreeList() else Resource.success(emptyList<PlainteEntree>())
            }
            val pendingDeferred = async {
                if (canViewPlainte) plainteRepository.getEntreesWithoutSortie() else Resource.success(emptyList<PlainteEntreeSummary>())
            }
            val mandatDeferred = async {
                if (canViewMandat) mandatRepository.getMandatList() else Resource.success(emptyList<Mandat>())
            }
            val convocationDeferred = async {
                if (canViewConvocation) convocationRepository.getConvocationList() else Resource.success(emptyList<Convocation>())
            }
            val arrestationDeferred = async {
                if (canViewArrestation) arrestationRepository.getArrestationList() else Resource.success(emptyList<Arrestation>())
            }
            val gavDeferred = async {
                if (canViewGav) gardeAVueRepository.getGardeAVueList() else Resource.success(emptyList<GardeAVue>())
            }
            val requisitionDeferred = async {
                if (canViewRequisition) requisitionRepository.getRequisitionList() else Resource.success(emptyList<Requisition>())
            }
            val personneRechercheeDeferred = async {
                if (canViewPersonneRecherchee) personneRechercheeRepository.getPersonneRechercheeList() else Resource.success(emptyList<PersonneRecherchee>())
            }
            val objetSaisiDeferred = async {
                if (canViewObjets) objetSaisiRepository.getObjetSaisiList() else Resource.success(emptyList<ObjetSaisi>())
            }
            val objetTrouveDeferred = async {
                if (canViewObjets) objetTrouveRepository.getObjetTrouveList() else Resource.success(emptyList<ObjetTrouve>())
            }
            val perquisitionDeferred = async {
                if (canViewPerquisition) perquisitionRepository.getPerquisitionList() else Resource.success(emptyList<Perquisition>())
            }
            val renseignementDeferred = async {
                if (canViewRenseignement) renseignementPjRepository.getRenseignementPjList() else Resource.success(emptyList<RenseignementPj>())
            }

            val plaintes = (plainteDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val pending = (pendingDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val mandats = (mandatDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val convocations = (convocationDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val arrestations = (arrestationDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val gavs = (gavDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val requisitions = (requisitionDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val personnesRecherchees = (personneRechercheeDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val objetsSaisis = (objetSaisiDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val objetsTrouves = (objetTrouveDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val perquisitions = (perquisitionDeferred.await() as? Resource.Success)?.data ?: emptyList()
            val renseignements = (renseignementDeferred.await() as? Resource.Success)?.data ?: emptyList()

            _state.update {
                it.copy(
                    isLoading = false,
                    plaintes = plaintes,
                    plaintesPending = pending,
                    mandats = mandats,
                    convocations = convocations,
                    arrestations = arrestations,
                    gavs = gavs,
                    requisitions = requisitions,
                    personnesRecherchees = personnesRecherchees,
                    objetsSaisis = objetsSaisis,
                    objetsTrouves = objetsTrouves,
                    perquisitions = perquisitions,
                    renseignements = renseignements
                )
            }
        }
    }
}
