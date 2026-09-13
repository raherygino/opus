package com.gsoft.opus.presentation.plainte

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.repository.PlainteEntreeFormData
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
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

data class PlainteFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val type: String = "ST_PARQUET",
    val datePlainte: String = "",
    val numeroDossier: String = "",
    val numeroSt: String = "",
    val opjPersonnelId: Int = 0,
    val enqueteurPersonnelId: Int = 0,
    val personnelOptions: List<Personnel> = emptyList(),
    val partieCivile: String = "",
    val miseEnCause: String = "",
    val adressePc: String = "",
    val infraction: String = "",
    val prejudice: String = "",
    val lieuInfraction: String = "",
    val heureInfraction: String = "",
    val observation: String = "",
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PlainteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val plainteRepository: PlainteRepository,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val plainteEntreeId: Int = savedStateHandle.get<Int>("plainteEntreeId") ?: 0
    val editId: Int get() = plainteEntreeId

    private val _state = MutableStateFlow(
        PlainteFormUiState(
            isEdit = plainteEntreeId > 0,
            datePlainte = if (plainteEntreeId > 0) "" else todayIso()
        )
    )
    val state: StateFlow<PlainteFormUiState> = _state.asStateFlow()

    init {
        loadPersonnel()
        if (plainteEntreeId > 0) loadEntry()
    }

    private fun loadPersonnel() {
        viewModelScope.launch {
            when (val result = personnelRepository.getPersonnelList()) {
                is Resource.Success -> _state.update { it.copy(personnelOptions = result.data) }
                else -> {}
            }
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = plainteRepository.getPlainteEntree(plainteEntreeId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            type = e.type,
                            datePlainte = e.datePlainte.take(10),
                            numeroDossier = e.numeroDossier,
                            numeroSt = e.numeroSt ?: "",
                            opjPersonnelId = e.opjPersonnelId ?: 0,
                            enqueteurPersonnelId = e.enqueteurPersonnelId ?: 0,
                            partieCivile = e.partieCivile ?: "",
                            miseEnCause = e.miseEnCause ?: "",
                            adressePc = e.adressePc ?: "",
                            infraction = e.infraction ?: "",
                            prejudice = e.prejudice ?: "",
                            lieuInfraction = e.lieuInfraction ?: "",
                            heureInfraction = e.heureInfraction?.take(5) ?: "",
                            observation = e.observation ?: ""
                        )
                    }
                    when (val atts = plainteRepository.getEntreeAttachments(plainteEntreeId)) {
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

    fun updateType(value: String) {
        _state.update { it.copy(type = value) }
    }
    fun updateDatePlainte(value: String) { _state.update { it.copy(datePlainte = value) } }
    fun updateNumeroSt(value: String) { _state.update { it.copy(numeroSt = value) } }
    fun updateOpj(personnelId: Int) { _state.update { it.copy(opjPersonnelId = personnelId) } }
    fun updateEnqueteur(personnelId: Int) { _state.update { it.copy(enqueteurPersonnelId = personnelId) } }
    fun updatePartieCivile(value: String) { _state.update { it.copy(partieCivile = value) } }
    fun updateMiseEnCause(value: String) { _state.update { it.copy(miseEnCause = value) } }
    fun updateAdressePc(value: String) { _state.update { it.copy(adressePc = value) } }
    fun updateInfraction(value: String) { _state.update { it.copy(infraction = value) } }
    fun updatePrejudice(value: String) { _state.update { it.copy(prejudice = value) } }
    fun updateLieuInfraction(value: String) { _state.update { it.copy(lieuInfraction = value) } }
    fun updateHeureInfraction(value: String) { _state.update { it.copy(heureInfraction = value) } }
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
        val errors = validatePlainteEntreeForm(
            type = s.type,
            datePlainte = s.datePlainte,
            numeroSt = s.numeroSt,
            opjPersonnelId = s.opjPersonnelId,
            enqueteurPersonnelId = s.enqueteurPersonnelId,
            partieCivile = s.partieCivile,
            miseEnCause = s.miseEnCause,
            adressePc = s.adressePc,
            infraction = s.infraction,
            prejudice = s.prejudice,
            lieuInfraction = s.lieuInfraction,
            heureInfraction = s.heureInfraction
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PlainteEntreeFormData(
                type = s.type,
                datePlainte = s.datePlainte,
                numeroSt = s.numeroSt.ifBlank { null },
                opjPersonnelId = s.opjPersonnelId,
                enqueteurPersonnelId = s.enqueteurPersonnelId,
                partieCivile = s.partieCivile.ifBlank { null },
                miseEnCause = s.miseEnCause.ifBlank { null },
                adressePc = s.adressePc.ifBlank { null },
                infraction = s.infraction.trim().ifBlank { null },
                prejudice = s.prejudice.trim().ifBlank { null },
                lieuInfraction = s.lieuInfraction.trim().ifBlank { null },
                heureInfraction = s.heureInfraction.ifBlank { null },
                observation = s.observation.trim().ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = plainteRepository.updatePlainteEntree(plainteEntreeId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = plainteRepository.createPlainteEntree(data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handleAttachments(savedId: Int) {
        val s = _state.value
        // Delete marked attachments
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            plainteRepository.deleteEntreeAttachment(savedId, a.id!!)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                plainteRepository.deleteEntreeAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    plainteRepository.addEntreeAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                plainteRepository.updateEntreeAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                plainteRepository.addEntreeAttachment(savedId, a.title, a.uploadFile)
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
