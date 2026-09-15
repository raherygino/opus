package com.gsoft.opus.presentation.personnerecherchee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.model.PersonneRechercheePhoto
import com.gsoft.opus.domain.repository.PersonneRechercheeFormData
import com.gsoft.opus.domain.repository.PersonneRechercheeRepository
import com.gsoft.opus.domain.repository.UploadFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A photo staged for upload or already saved. */
data class PhotoItem(
    val id: Int? = null,
    val caption: String = "",
    val uploadFile: UploadFile? = null,
    val previewUri: String? = null,
    val captureSource: String? = null,
    val existingFilename: String? = null,
    val isDeleted: Boolean = false
)

data class PersonneRechercheeFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val nom: String = "",
    val adresse: String = "",
    val motif: String = "",
    val photos: List<PhotoItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class PersonneRechercheeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PersonneRechercheeRepository
) : ViewModel() {

    private val entryId: Int = savedStateHandle.get<Int>("personneRechercheeId") ?: 0
    val editId: Int get() = entryId

    private val _state = MutableStateFlow(
        PersonneRechercheeFormUiState(isEdit = entryId > 0)
    )
    val state: StateFlow<PersonneRechercheeFormUiState> = _state.asStateFlow()

    init {
        if (entryId > 0) {
            loadEntry()
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = repository.getPersonneRecherchee(entryId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            nom = e.nom,
                            adresse = e.adresse ?: "",
                            motif = e.motif
                        )
                    }
                    when (val photos = repository.getPhotos(entryId)) {
                        is Resource.Success -> _state.update {
                            it.copy(photos = photos.data.map { p -> p.toPhotoItem() })
                        }
                        else -> {}
                    }
                }
                is Resource.Error -> _state.update { it.copy(isLoading = false, errorMessage = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    private fun PersonneRechercheePhoto.toPhotoItem() = PhotoItem(
        id = id,
        caption = caption ?: "",
        existingFilename = originalFilename,
        captureSource = captureSource
    )

    fun updateNom(value: String) { _state.update { it.copy(nom = value) } }
    fun updateAdresse(value: String) { _state.update { it.copy(adresse = value) } }
    fun updateMotif(value: String) { _state.update { it.copy(motif = value) } }

    fun addPhoto(file: UploadFile, previewUri: String, captureSource: String) {
        _state.update {
            it.copy(photos = it.photos + PhotoItem(
                uploadFile = file,
                previewUri = previewUri,
                captureSource = captureSource
            ))
        }
    }

    fun updatePhotoCaption(index: Int, caption: String) {
        _state.update { s ->
            s.copy(photos = s.photos.mapIndexed { i, p -> if (i == index) p.copy(caption = caption) else p })
        }
    }

    fun removePhoto(index: Int) {
        _state.update { s ->
            val list = s.photos.toMutableList()
            if (list[index].id != null) {
                list[index] = list[index].copy(isDeleted = true)
            } else {
                list.removeAt(index)
            }
            s.copy(photos = list)
        }
    }

    fun save() {
        val s = _state.value
        val errors = validatePersonneRechercheeForm(s.nom, s.motif)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = PersonneRechercheeFormData(
                nom = s.nom.trim(),
                adresse = s.adresse.trim().ifBlank { null },
                motif = s.motif.trim()
            )
            if (s.isEdit) {
                when (val res = repository.updatePersonneRecherchee(entryId, data)) {
                    is Resource.Success -> handlePhotos(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = repository.createPersonneRecherchee(data)) {
                    is Resource.Success -> handlePhotos(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    private suspend fun handlePhotos(savedId: Int) {
        val s = _state.value
        for (p in s.photos.filter { it.isDeleted && it.id != null }) {
            val pid = p.id ?: continue
            repository.deletePhoto(savedId, pid)
        }
        for (p in s.photos.filter { !it.isDeleted && it.uploadFile != null }) {
            repository.addPhoto(savedId, p.caption.ifBlank { null }, p.captureSource, p.uploadFile!!)
        }
        for (p in s.photos.filter { !it.isDeleted && it.id != null }) {
            val pid = p.id ?: continue
            repository.updatePhotoCaption(savedId, pid, p.caption.ifBlank { null })
        }
        _state.update { it.copy(isSaving = false, saved = true) }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
