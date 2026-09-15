package com.gsoft.opus.presentation.gardeavue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.GardeAVueFormData
import com.gsoft.opus.domain.repository.GardeAVueRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.presentation.personnel.AttachmentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GardeAVueFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val nom: String = "",
    val prenoms: String = "",
    val dateNaissance: String = "",
    val adresse: String = "",
    val enqueteurPermance: String = "",
    val opjGav: String = "",
    val motif: String = "",
    val etatSante: String = "",
    val droitsNotifies: String = "",
    val personneContacter: String = "",
    val debutGav: String = "",
    val finGav: String = "",
    val prolongationGav: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class GardeAVueFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gardeAVueRepository: GardeAVueRepository
) : ViewModel() {

    private val gardeAVueId: Int = savedStateHandle.get<Int>("gardeAVueId") ?: 0
    val editId: Int get() = gardeAVueId

    private val _state = MutableStateFlow(
        GardeAVueFormUiState(isEdit = gardeAVueId > 0)
    )
    val state: StateFlow<GardeAVueFormUiState> = _state.asStateFlow()

    init {
        if (gardeAVueId > 0) loadGardeAVue()
    }

    private fun loadGardeAVue() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = gardeAVueRepository.getGardeAVue(gardeAVueId)) {
                is Resource.Success -> {
                    val g = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            nom = g.nom,
                            prenoms = g.prenoms ?: "",
                            dateNaissance = g.dateNaissance?.take(10) ?: "",
                            adresse = g.adresse ?: "",
                            enqueteurPermance = g.enqueteurPermance ?: "",
                            opjGav = g.opjGav ?: "",
                            motif = g.motif ?: "",
                            etatSante = g.etatSante ?: "",
                            droitsNotifies = g.droitsNotifies ?: "",
                            personneContacter = g.personneContacter ?: "",
                            debutGav = g.debutGav ?: "",
                            finGav = g.finGav ?: "",
                            prolongationGav = g.prolongationGav ?: ""
                        )
                    }
                    when (val atts = gardeAVueRepository.getGardeAVueAttachments(gardeAVueId)) {
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

    fun updateNom(value: String) { _state.update { it.copy(nom = value) } }
    fun updatePrenoms(value: String) { _state.update { it.copy(prenoms = value) } }
    fun updateDateNaissance(value: String) { _state.update { it.copy(dateNaissance = value) } }
    fun updateAdresse(value: String) { _state.update { it.copy(adresse = value) } }
    fun updateEnqueteurPermance(value: String) { _state.update { it.copy(enqueteurPermance = value) } }
    fun updateOpjGav(value: String) { _state.update { it.copy(opjGav = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }
    fun updateEtatSante(value: String) { _state.update { it.copy(etatSante = value) } }
    fun updateDroitsNotifies(value: String) { _state.update { it.copy(droitsNotifies = value) } }
    fun updatePersonneContacter(value: String) { _state.update { it.copy(personneContacter = value) } }
    fun updateDebutGav(value: String) { _state.update { it.copy(debutGav = value) } }
    fun updateFinGav(value: String) { _state.update { it.copy(finGav = value) } }
    fun updateProlongationGav(value: String) { _state.update { it.copy(prolongationGav = value) } }

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
        val errors = validateGardeAVueForm(
            nom = s.nom,
            dateNaissance = s.dateNaissance,
            debutGav = s.debutGav,
            finGav = s.finGav,
            prolongationGav = s.prolongationGav
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = GardeAVueFormData(
                nom = s.nom.trim(),
                prenoms = s.prenoms.trim().ifBlank { null },
                dateNaissance = s.dateNaissance.trim().ifBlank { null },
                adresse = s.adresse.trim().ifBlank { null },
                enqueteurPermance = s.enqueteurPermance.trim().ifBlank { null },
                opjGav = s.opjGav.trim().ifBlank { null },
                motif = s.motif.trim().ifBlank { null },
                etatSante = s.etatSante.trim().ifBlank { null },
                droitsNotifies = s.droitsNotifies.trim().ifBlank { null },
                personneContacter = s.personneContacter.trim().ifBlank { null },
                debutGav = s.debutGav.trim().ifBlank { null },
                finGav = s.finGav.trim().ifBlank { null },
                prolongationGav = s.prolongationGav.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = gardeAVueRepository.updateGardeAVue(gardeAVueId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = gardeAVueRepository.createGardeAVue(data)) {
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
            gardeAVueRepository.deleteGardeAVueAttachment(savedId, a.id!!)
        }
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                gardeAVueRepository.deleteGardeAVueAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    gardeAVueRepository.addGardeAVueAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                gardeAVueRepository.updateGardeAVueAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                gardeAVueRepository.addGardeAVueAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
