package com.gsoft.opus.presentation.maincourante

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gsoft.opus.core.Resource
import com.gsoft.opus.domain.repository.MainCouranteCategorie
import com.gsoft.opus.domain.repository.MainCouranteCategorieRepository
import com.gsoft.opus.domain.repository.MainCouranteFormData
import com.gsoft.opus.domain.repository.MainCouranteRepository
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

data class MainCouranteFormUiState(
    val isEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val dateEvenement: String = "",
    val heureEvenement: String = "",
    val categorie: String = "",
    val description: String = "",
    val origine: String = "Secretariat",
    val categories: List<MainCouranteCategorie> = emptyList(),
    val attachments: List<AttachmentItem> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class MainCouranteFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mainCouranteRepository: MainCouranteRepository,
    private val categorieRepository: MainCouranteCategorieRepository
) : ViewModel() {

    private val mainCouranteId: Int = savedStateHandle.get<Int>("mainCouranteId") ?: 0
    val editId: Int get() = mainCouranteId

    private val _state = MutableStateFlow(
        MainCouranteFormUiState(
            isEdit = mainCouranteId > 0,
            dateEvenement = if (mainCouranteId > 0) "" else todayIso(),
            heureEvenement = if (mainCouranteId > 0) "" else nowHHmm(),
            origine = savedStateHandle.get<String>("origine") ?: "Secretariat"
        )
    )
    val state: StateFlow<MainCouranteFormUiState> = _state.asStateFlow()

    init {
        loadCategories()
        if (mainCouranteId > 0) loadEntry()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = categorieRepository.getCategories()) {
                is Resource.Success -> _state.update { s ->
                    val cats = result.data
                    // Default the categorie to the first available if it's still empty.
                    val defaultCategorie = if (s.categorie.isBlank() && cats.isNotEmpty()) cats.first().label else s.categorie
                    s.copy(categories = cats, categorie = defaultCategorie)
                }
                is Resource.Error -> {}
                is Resource.Loading -> {}
            }
        }
    }

    /** Called when the categorie dialog reports that the catalog changed. */
    fun onCategoriesChanged(categories: List<MainCouranteCategorie>) {
        _state.update { s ->
            val stillExists = categories.any { it.label == s.categorie }
            val newCategorie = if (stillExists) s.categorie
            else if (categories.isNotEmpty()) categories.first().label
            else ""
            s.copy(categories = categories, categorie = newCategorie)
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = mainCouranteRepository.getMainCourante(mainCouranteId)) {
                is Resource.Success -> {
                    val e = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            dateEvenement = e.dateEvenement.take(10),
                            heureEvenement = e.heureDisplay,
                            categorie = e.categorie,
                            description = e.description,
                            origine = e.origine
                        )
                    }
                    when (val atts = mainCouranteRepository.getAttachments(mainCouranteId)) {
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

    fun updateDateEvenement(value: String) { _state.update { it.copy(dateEvenement = value) } }
    fun updateHeureEvenement(value: String) { _state.update { it.copy(heureEvenement = value) } }
    fun updateCategorie(value: String) { _state.update { it.copy(categorie = value) } }
    fun updateDescription(value: String) { _state.update { it.copy(description = value) } }

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
        val errors = validateMainCouranteForm(
            dateEvenement = s.dateEvenement,
            heureEvenement = s.heureEvenement,
            categorie = s.categorie,
            description = s.description
        )
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errorMessage = errors.values.first()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            val data = MainCouranteFormData(
                dateEvenement = s.dateEvenement,
                heureEvenement = s.heureEvenement,
                categorie = s.categorie,
                description = s.description.trim(),
                origine = s.origine
            )
            if (s.isEdit) {
                when (val res = mainCouranteRepository.updateMainCourante(mainCouranteId, data)) {
                    is Resource.Success -> handleAttachments(res.data.id)
                    is Resource.Error -> _state.update { it.copy(isSaving = false, errorMessage = res.message) }
                    is Resource.Loading -> {}
                }
            } else {
                when (val res = mainCouranteRepository.createMainCourante(data)) {
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
            mainCouranteRepository.deleteAttachment(savedId, a.id!!)
        }
        // Add/update non-deleted attachments
        for (a in s.attachments.filter { !it.isDeleted }) {
            if (a.id != null && a.uploadFile != null) {
                // Replace: delete then re-create
                mainCouranteRepository.deleteAttachment(savedId, a.id)
                if (a.title.isNotBlank()) {
                    mainCouranteRepository.addAttachment(savedId, a.title, a.uploadFile)
                }
            } else if (a.id != null && a.title.isNotBlank()) {
                mainCouranteRepository.updateAttachmentTitle(savedId, a.id, a.title)
            } else if (a.uploadFile != null && a.title.isNotBlank()) {
                mainCouranteRepository.addAttachment(savedId, a.title, a.uploadFile)
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

private fun nowHHmm(): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date())
