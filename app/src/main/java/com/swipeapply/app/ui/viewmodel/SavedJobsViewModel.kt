package com.swipeapply.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.ApplicationStatus
import com.swipeapply.app.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SavedJobItem(
    val card: com.swipeapply.app.data.model.JobCard,
    val timestamp: Long,
    val status: ApplicationStatus
)

data class SavedJobsUiState(
    val jobs: List<SavedJobItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SavedJobsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JobRepository.getInstance(
        context = application.applicationContext,
        apiKey = ApiConfig.FINDWORK_API_KEY
    )

    private val _uiState = MutableStateFlow(SavedJobsUiState(isLoading = true))
    val uiState: StateFlow<SavedJobsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.getLikedJobs().map {
                    SavedJobItem(
                        card = it.card,
                        timestamp = it.timestamp,
                        status = it.applicationStatus ?: ApplicationStatus.SAVED
                    )
                }
            }.onSuccess { jobs ->
                _uiState.update { it.copy(jobs = jobs, isLoading = false, error = null) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load saved jobs"
                    )
                }
            }
        }
    }

    fun updateStatus(jobId: String, status: ApplicationStatus) {
        viewModelScope.launch {
            repository.updateApplicationStatus(jobId, status)
            _uiState.update { state ->
                state.copy(
                    jobs = state.jobs.map { item ->
                        if (item.card.id == jobId) item.copy(status = status) else item
                    }
                )
            }
        }
    }
}
