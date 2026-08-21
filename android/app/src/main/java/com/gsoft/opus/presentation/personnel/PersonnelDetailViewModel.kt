package com.gsoft.opus.presentation.personnel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.Resource
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.repository.MouvementRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonnelDetailUiState(
    val personnel: Personnel? = null,
    val attachments: List<PersonnelAttachment> = emptyList(),
    val mouvements: List<Mouvement> = emptyList(),
    val comportements: List<Comportement> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val canViewRecords: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val deleteAttachmentTarget: PersonnelAttachment? = null,
    val userMessage: String? = null,
    val notFound: Boolean = false
)

@HiltViewModel
class PersonnelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val personnelRepository: PersonnelRepository,
    private val mouvementRepository: MouvementRepository,
    private val comportementRepository: ComportementRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val personnelId: Int = savedStateHandle.get<Int>("personnelId") ?: 0

    private val _state = MutableStateFlow(PersonnelDetailUiState())
    val state: StateFlow<PersonnelDetailUiState> = _state.asStateFlow()

    init {
        loadPermissions()
        refresh()
    }

    private fun loadPermissions() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().getOrNull()
            val hasEditPerm = hasPermission(user, "personnel", PermissionAction.EDIT)
            val hasDeletePerm = hasPermission(user, "personnel", PermissionAction.DELETE)
            val canViewRecords = hasPermission(user, "personnel", PermissionAction.VIEW)

            // An admin may edit/delete their OWN personnel profile, but must
            // NOT be able to edit/delete another admin's profile. This mirrors
            // the backend guard in PersonnelController::guardAdminProfile.
            val personnel = _state.value.personnel
            val isOwnProfile = user?.personnelId != null && personnel?.id == user.personnelId
            val isOtherAdminProfile = personnel?.isAdminProfile == true && !isOwnProfile

            _state.update {
                it.copy(
                    canEdit = hasEditPerm && !isOtherAdminProfile,
                    canDelete = hasDeletePerm && !isOtherAdminProfile,
                    canViewRecords = canViewRecords
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = personnelRepository.getPersonnel(personnelId)) {
                is Resource.Success -> {
                    _state.update { it.copy(personnel = result.data) }
                    // Re-evaluate edit/delete permissions now that we know
                    // whether this personnel belongs to an admin.
                    loadPermissions()
                    val attachmentsDeferred = async { personnelRepository.getAttachments(personnelId) }
                    val mouvementsDeferred = async { mouvementRepository.getMouvementList(personnelId) }
                    val comportementsDeferred = async { comportementRepository.getComportementList(personnelId) }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            attachments = attachmentsDeferred.await().getOrNull() ?: emptyList(),
                            mouvements = mouvementsDeferred.await().getOrNull() ?: emptyList(),
                            comportements = comportementsDeferred.await().getOrNull() ?: emptyList()
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message, notFound = true)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun addAttachment(title: String, file: UploadFile) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAttachment = true) }
            when (val result = personnelRepository.addAttachment(personnelId, title, file)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isUploadingAttachment = false,
                        attachments = it.attachments + result.data,
                        userMessage = "Pièce jointe ajoutée"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isUploadingAttachment = false, userMessage = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun requestDeleteAttachment(attachment: PersonnelAttachment) {
        _state.update { it.copy(deleteAttachmentTarget = attachment) }
    }

    fun cancelDeleteAttachment() {
        _state.update { it.copy(deleteAttachmentTarget = null) }
    }

    fun confirmDeleteAttachment() {
        val target = _state.value.deleteAttachmentTarget ?: return
        viewModelScope.launch {
            when (personnelRepository.deleteAttachment(personnelId, target.id)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        deleteAttachmentTarget = null,
                        attachments = it.attachments.filterNot { a -> a.id == target.id },
                        userMessage = "Pièce jointe supprimée"
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(deleteAttachmentTarget = null, userMessage = "Impossible de supprimer la pièce jointe")
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(userMessage = null) }
    }
}
