package com.gsoft.opus.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.AuthRepository
import com.gsoft.opus.domain.repository.UploadFile
import com.gsoft.opus.domain.usecase.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isPhotoUploading: Boolean = false,
    val username: String = "",
    val personnelId: Int? = null,
    val photo: String? = null,
    // Administrative fields — shown read-only, managed by the secretariat.
    val im: String? = null,
    val grade: String? = null,
    val affectation: String? = null,
    // Editable fields
    val lastname: String = "",
    val firstname: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val errorMessage: String? = null,
    val photoError: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getCurrentUserUseCase()) {
                is Resource.Success -> {
                    val user = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            username = user.username,
                            personnelId = user.personnelId,
                            photo = user.photo,
                            im = user.im,
                            grade = user.grade,
                            affectation = user.affectation,
                            lastname = user.lastName ?: "",
                            firstname = user.firstName ?: "",
                            phone = user.phone ?: "",
                            email = user.email ?: "",
                            address = user.address ?: ""
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateLastname(value: String) = _state.update { it.copy(lastname = value) }
    fun updateFirstname(value: String) = _state.update { it.copy(firstname = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value) }
    fun updateAddress(value: String) = _state.update { it.copy(address = value) }
    fun dismissError() = _state.update { it.copy(errorMessage = null, photoError = null) }

    fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return

        val lastname = current.lastname.trim()
        val firstname = current.firstname.trim()
        val phone = current.phone.trim().ifBlank { null }
        val email = current.email.trim().ifBlank { null }
        val address = current.address.trim().ifBlank { null }

        when {
            lastname.isBlank() -> {
                _state.update { it.copy(errorMessage = "Le nom est requis") }
                return
            }
            firstname.isBlank() -> {
                _state.update { it.copy(errorMessage = "Le prénom est requis") }
                return
            }
            email != null && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _state.update { it.copy(errorMessage = "Adresse e-mail invalide") }
                return
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = authRepository.updateProfile(lastname, firstname, phone, email, address)) {
                is Resource.Success -> _state.update {
                    it.copy(
                        isSaving = false,
                        saved = true,
                        lastname = result.data.lastName ?: lastname,
                        firstname = result.data.firstName ?: firstname
                    )
                }
                is Resource.Error -> _state.update {
                    it.copy(isSaving = false, errorMessage = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun uploadPhoto(photo: UploadFile) {
        if (_state.value.isPhotoUploading) return
        viewModelScope.launch {
            _state.update { it.copy(isPhotoUploading = true, photoError = null) }
            when (val result = authRepository.uploadProfilePhoto(photo)) {
                is Resource.Success -> _state.update {
                    it.copy(isPhotoUploading = false, photo = result.data.photo)
                }
                is Resource.Error -> _state.update {
                    it.copy(isPhotoUploading = false, photoError = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deletePhoto() {
        if (_state.value.isPhotoUploading) return
        viewModelScope.launch {
            _state.update { it.copy(isPhotoUploading = true, photoError = null) }
            when (val result = authRepository.deleteProfilePhoto()) {
                is Resource.Success -> _state.update {
                    it.copy(isPhotoUploading = false, photo = result.data.photo)
                }
                is Resource.Error -> _state.update {
                    it.copy(isPhotoUploading = false, photoError = result.message)
                }
                is Resource.Loading -> Unit
            }
        }
    }
}
