package com.gsoft.opus.presentation.plainte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.PlainteEntreeSummary
import com.gsoft.opus.domain.model.PlainteSortieAttachment
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.repository.PlainteSortieFormData
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class PlainteSortieFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val plainteEntreeId: Int = 0,
    val entreeSummary: PlainteEntreeSummary? = null,
    val availableEntrees: List<PlainteEntreeSummary> = emptyList(),
    val isLoadingEntrees: Boolean = false,
    val nature: String = "DAT",
    val dateSortie: String = "",
    val numero: String = "",
    val numeroTtr: String = "",
    val nomSubstitut: String = "",
    val dateDeferrement: String = "",
    val observation: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PlainteSortieFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plainteRepository: PlainteRepository
) : ViewModel() {

    private val plainteSortieId: Int = savedStateHandle.get<Int>("plainteSortieId") ?: 0
    private val preselectedEntreeId: Int = savedStateHandle.get<Int>("plainteEntreeId") ?: 0
    val editId: Int get() = plainteSortieId

    private val _state = MutableStateFlow(
        PlainteSortieFormUiState(
            isEdit = plainteSortieId > 0,
            plainteEntreeId = preselectedEntreeId,
            dateSortie = if (plainteSortieId > 0) "" else todayIso()
        )
    )
    val state: StateFlow<PlainteSortieFormUiState> = _state.asStateFlow()

    init {
        if (plainteSortieId > 0) {
            loadSortie()
        } else if (preselectedEntreeId > 0) {
            loadEntreeSummary(preselectedEntreeId)
        } else {
            // No ENTRÉE preselected — load the list of ENTRÉEs without sortie
            // so the user can pick one in the form.
            loadAvailableEntrees()
        }
        // Load the suggested sortie number on create.
        if (plainteSortieId == 0) loadSuggestedNumber()
    }

    private fun loadSuggestedNumber() {
        viewModelScope.launch {
            when (val result = plainteRepository.peekSortieNumber()) {
                is Resource.Success -> _state.update { it.copy(numero = result.data) }
                else -> {}
            }
        }
    }

    fun refreshSuggestedNumber() {
        if (!_state.value.isEdit) loadSuggestedNumber()
    }

    private fun loadAvailableEntrees() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingEntrees = true) }
            when (val result = plainteRepository.getEntreesWithoutSortie()) {
                is Resource.Success -> _state.update {
                    it.copy(isLoadingEntrees = false, availableEntrees = result.data)
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoadingEntrees = false, errorMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun selectEntree(entreeId: Int) {
        viewModelScope.launch {
            val summary = _state.value.availableEntrees.firstOrNull { it.id == entreeId }
            _state.update { it.copy(plainteEntreeId = entreeId, entreeSummary = summary) }
        }
    }

    fun changeEntree() {
        // Allow the user to pick a different ENTRÉE.
        _state.update { it.copy(plainteEntreeId = 0, entreeSummary = null) }
        if (_state.value.availableEntrees.isEmpty()) {
            loadAvailableEntrees()
        }
    }

    private fun loadEntreeSummary(entreeId: Int) {
        viewModelScope.launch {
            // Find the ENTRÉE summary from the without-sortie list.
            when (val result = plainteRepository.getEntreesWithoutSortie()) {
                is Resource.Success -> {
                    val summary = result.data.firstOrNull { it.id == entreeId }
                    _state.update { it.copy(entreeSummary = summary) }
                }
                else -> {}
            }
        }
    }

    private fun loadSortie() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = plainteRepository.getPlainteSortie(plainteSortieId)) {
                is Resource.Success -> {
                    val s = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            plainteEntreeId = s.plainteEntreeId,
                            nature = s.nature,
                            dateSortie = s.dateSortie.take(10),
                            numero = s.numero,
                            numeroTtr = s.numeroTtr ?: "",
                            nomSubstitut = s.nomSubstitut ?: "",
                            dateDeferrement = s.dateDeferrement?.take(10) ?: "",
                            observation = s.observation ?: ""
                        )
                    }
                    when (val atts = plainteRepository.getSortieAttachments(plainteSortieId)) {
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

    fun updateNature(value: String) { _state.update { it.copy(nature = value) } }
    fun updateDateSortie(value: String) { _state.update { it.copy(dateSortie = value) } }
    fun updateNumero(value: String) { _state.update { it.copy(numero = value) } }
    fun updateNumeroTtr(value: String) { _state.update { it.copy(numeroTtr = value) } }
    fun updateNomSubstitut(value: String) { _state.update { it.copy(nomSubstitut = value) } }
    fun updateDateDeferrement(value: String) { _state.update { it.copy(dateDeferrement = value) } }
    fun updateObservation(value: String) { _state.update { it.copy(observation = value) } }

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
        if (s.plainteEntreeId <= 0) {
            _state.update { it.copy(errorMessage = "Aucune plainte ENTRÉE associée") }
            return
        }
        val errors = validatePlainteSortieForm(
            nature = s.nature,
            dateSortie = s.dateSortie,
            numeroTtr = s.numeroTtr,
            nomSubstitut = s.nomSubstitut,
            dateDeferrement = s.dateDeferrement
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PlainteSortieFormData(
                plainteEntreeId = s.plainteEntreeId,
                nature = s.nature,
                dateSortie = s.dateSortie,
                numero = s.numero.takeIf { it.isNotBlank() },
                numeroTtr = s.numeroTtr.trim(),
                nomSubstitut = s.nomSubstitut.trim(),
                dateDeferrement = s.dateDeferrement.ifBlank { null },
                observation = s.observation.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = plainteRepository.updatePlainteSortie(plainteSortieId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = plainteRepository.createPlainteSortie(data)) {
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
            plainteRepository.deleteSortieAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                plainteRepository.deleteSortieAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    plainteRepository.addSortieAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                plainteRepository.updateSortieAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                plainteRepository.addSortieAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

private fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
