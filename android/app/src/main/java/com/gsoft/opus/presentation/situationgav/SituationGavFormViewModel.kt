package com.gsoft.opus.presentation.situationgav

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.GardeAVue
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.GardeAVueRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.SituationGavFormData
import com.gsoft.opus.domain.repository.SituationGavRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SituationGavFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val gardeAVueId: Int = 0,
    val dateControle: String = "",
    val agentControleId: Int = 0,
    val etatGeneral: String = "",
    val observations: String = "",
    val mesuresPrises: String = "",
    val gavOptions: List<GardeAVue> = emptyList(),
    val personnelOptions: List<Personnel> = emptyList(),
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SituationGavFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val situationGavRepository: SituationGavRepository,
    private val gardeAVueRepository: GardeAVueRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val situationGavId: Int = savedStateHandle.get<Int>("situationGavId") ?: 0
    val editId: Int get() = situationGavId

    private val _state = MutableStateFlow(
        SituationGavFormUiState(isEdit = situationGavId > 0)
    )
    val state: StateFlow<SituationGavFormUiState> = _state.asStateFlow()

    init {
        loadOptions()
        if (situationGavId > 0) loadSituation()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            when (val result = gardeAVueRepository.getGardeAVueList()) {
                is Resource.Success -> _state.update { it.copy(gavOptions = result.data) }
                else -> {}
            }
        }
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update { it.copy(personnelOptions = result.data) }
                else -> {}
            }
        }
    }

    private fun loadSituation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = situationGavRepository.getSituationGav(situationGavId)) {
                is Resource.Success -> {
                    val s = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            gardeAVueId = s.gardeAVueId,
                            dateControle = s.dateControle ?: "",
                            agentControleId = s.agentControleId ?: 0,
                            etatGeneral = s.etatGeneral ?: "",
                            observations = s.observations ?: "",
                            mesuresPrises = s.mesuresPrises ?: ""
                        )
                    }
                    when (val atts = situationGavRepository.getSituationGavAttachments(situationGavId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { a ->
                                AttachmentItem(id = a.id, title = a.title, existingFilename = a.originalFilename)
                            })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateGardeAVueId(value: Int) { _state.update { it.copy(gardeAVueId = value) } }
    fun updateDateControle(value: String) { _state.update { it.copy(dateControle = value) } }
    fun updateAgentControle(value: Int) { _state.update { it.copy(agentControleId = value) } }
    fun updateEtatGeneral(value: String) { _state.update { it.copy(etatGeneral = value) } }
    fun updateObservations(value: String) { _state.update { it.copy(observations = value) } }
    fun updateMesuresPrises(value: String) { _state.update { it.copy(mesuresPrises = value) } }

    fun addAttachment() {
        _state.update { it.copy(attachments = it.attachments + AttachmentItem()) }
    }

    fun updateAttachmentTitle(index: Int, title: String) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(title = title) else a })
        }
    }

    fun setAttachmentFile(index: Int, file: UploadFile) {
        _state.update { s ->
            s.copy(attachments = s.attachments.mapIndexed { i, a -> if (i == index) a.copy(uploadFile = file) else a })
        }
    }

    fun removeAttachment(index: Int) {
        _state.update { s ->
            val list = s.attachments.toMutableList()
            if (list[index].id != null) {
                list[index] = list[index].copy(isDeleted = true)
            } else {
                list.removeAt(index)
            }
            s.copy(attachments = list)
        }
    }

    fun save() {
        val s = _state.value
        val errors = validateSituationGavForm(
            gardeAVueId = s.gardeAVueId,
            dateControle = s.dateControle
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = SituationGavFormData(
                gardeAVueId = s.gardeAVueId,
                dateControle = s.dateControle.trim(),
                agentControleId = s.agentControleId.takeIf { it > 0 },
                etatGeneral = s.etatGeneral.trim().ifBlank { null },
                observations = s.observations.trim().ifBlank { null },
                mesuresPrises = s.mesuresPrises.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = situationGavRepository.updateSituationGav(situationGavId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = situationGavRepository.createSituationGav(data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            situationGavRepository.deleteSituationGavAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                situationGavRepository.deleteSituationGavAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    situationGavRepository.addSituationGavAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                situationGavRepository.updateSituationGavAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                situationGavRepository.addSituationGavAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
