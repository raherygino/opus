package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment
import com.gsoft.opus.domain.repository.PersonnelFormData
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.UploadFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttachmentItem(
    val id: Int? = null,
    val title: String = "",
    val existingFilename: String? = null,
    val uploadFile: UploadFile? = null,
    val isDeleted: Boolean = false
)

data class PersonnelFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val im: String = "",
    val grade: String = "",
    val lastname: String = "",
    val firstname: String = "",
    val affectation: String = "",
    val phone: String = "",
    val address: String = "",
    val photoFile: UploadFile? = null,
    val photoPreview: String? = null,
    val hasServerPhoto: Boolean = false,
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PersonnelFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personnelRepository: PersonnelRepository
) : ViewModel() {

    private val personnelId: Int = savedStateHandle.get<Int>("personnelId") ?: 0
    val editId: Int get() = personnelId

    private val _state = MutableStateFlow(PersonnelFormUiState(isEdit = personnelId > 0))
    val state: StateFlow<PersonnelFormUiState> = _state.asStateFlow()

    init {
        if (personnelId > 0) loadPersonnel()
    }

    private fun loadPersonnel() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = personnelRepository.getPersonnel(personnelId)) {
                is Resource.Success -> {
                    val p = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            im = p.im,
                            grade = p.grade,
                            lastname = p.lastname,
                            firstname = p.firstname,
                            affectation = p.affectation ?: "",
                            phone = p.phone ?: "",
                            address = p.address ?: "",
                            photoPreview = if (p.photo != null) personnelPhotoUrl(personnelId) else null,
                            hasServerPhoto = p.photo != null
                        )
                    }
                    when (val atts = personnelRepository.getAttachments(personnelId)) {
                        is Resource.Success -> _state.update {
                            it.copy(attachments = atts.data.map { a -> AttachmentItem(id = a.id, title = a.title, existingFilename = a.originalFilename) })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateIm(value: String) { _state.update { it.copy(im = value) } }
    fun updateGrade(value: String) { _state.update { it.copy(grade = value) } }
    fun updateLastname(value: String) { _state.update { it.copy(lastname = value) } }
    fun updateFirstname(value: String) { _state.update { it.copy(firstname = value) } }
    fun updateAffectation(value: String) { _state.update { it.copy(affectation = value) } }
    fun updatePhone(value: String) { _state.update { it.copy(phone = value) } }
    fun updateAddress(value: String) { _state.update { it.copy(address = value) } }

    fun setPhoto(file: UploadFile, previewUri: String) {
        _state.update { it.copy(photoFile = file, photoPreview = previewUri) }
    }

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
        if (s.im.isBlank() || s.grade.isBlank() || s.lastname.isBlank() || s.firstname.isBlank()) {
            _state.update { it.copy(errorMessage = "Tous les champs obligatoires doivent être remplis") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PersonnelFormData(
                im = s.im, grade = s.grade, lastname = s.lastname, firstname = s.firstname,
                affectation = s.affectation.ifBlank { null },
                phone = s.phone.ifBlank { null },
                address = s.address.ifBlank { null }
            )
            if (s.isEdit) {
                when (val res = personnelRepository.updatePersonnel(personnelId, data)) {
                    is Resource.Success -> handleAttachmentsAndPhoto(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = personnelRepository.createPersonnel(data)) {
                    is Resource.Success -> handleAttachmentsAndPhoto(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handleAttachmentsAndPhoto(savedId: Int) {
        val s = _state.value
        // Delete marked attachments
        for (a in s.attachments.filter { it.isDeleted && it.id != null }) {
            personnelRepository.deleteAttachment(savedId, a.id!!)
        }
        // Upload photo
        if (s.photoFile != null) {
            personnelRepository.uploadPhoto(savedId, s.photoFile!!, null)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                personnelRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    personnelRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                personnelRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                personnelRepository.addAttachment(savedId, a.title, a.uploadFile)
            }
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
