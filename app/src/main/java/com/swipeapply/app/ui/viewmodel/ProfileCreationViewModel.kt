package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.repository.JobRepository
import com.swipeapply.app.utils.ResumeParser
import com.swipeapply.app.utils.ResumeParserV2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val isSaved: Boolean = false,
    val error: String? = null,
    val parseProgress: Float = 0f,
    val parseStage: String = ""
)

class ProfileCreationViewModel(
    application: Application
) : AndroidViewModel(application) {

    // ✅ Correct repository init (same as HomeViewModel)
    private val repository = JobRepository.getInstance(
        context = application.applicationContext,
        apiKey = ApiConfig.FINDWORK_API_KEY
    )

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    fun parseResume(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, parseProgress = 0f, parseStage = "") }

            _uiState.update {
                it.copy(parseProgress = 0.1f, parseStage = "Analyzing your resume...")
            }

            val result = ResumeParser.parseResume(context, uri) ?: run {
                _uiState.update {
                    it.copy(parseProgress = 0.2f, parseStage = "Using alternative parser...")
                }

                ResumeParserV2.parseResume(
                    context = context,
                    uri = uri,
                    onProgress = { progress, stage ->
                        val mappedProgress = 0.2f + (progress * 0.8f)
                        _uiState.update {
                            it.copy(parseProgress = mappedProgress.coerceIn(0f, 1f), parseStage = stage)
                        }
                    }
                )
            }

            if (result != null) {
                Log.d("ProfileCreationVM", "Resume parsed successfully: ${result.fullName}")
                _uiState.update {
                    it.copy(isLoading = false, profile = result, parseProgress = 1f, parseStage = "Done!")
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Could not parse your resume automatically. Please fill in your details manually.",
                        profile = UserProfile(),
                        parseProgress = 0f,
                        parseStage = ""
                    )
                }
            }
        }
    }

    fun updateProfileField(newProfile: UserProfile?) {
        _uiState.update { it.copy(profile = newProfile, error = null) }
    }

    fun saveProfile() {
        viewModelScope.launch {

            val currentProfile = _uiState.value.profile
            if (currentProfile == null) {
                _uiState.update { it.copy(error = "No profile data to save") }
                return@launch
            }

            if (currentProfile.fullName.isBlank()) {
                _uiState.update { it.copy(error = "Full name is required") }
                return@launch
            }

            _uiState.update { it.copy(error = null) }

            try {
                // Get user ID reliably (with JWT fallback)
                val userId = SupabaseClient.getCurrentUserId()

                if (userId == null) {
                    _uiState.update {
                        it.copy(error = "Not logged in. Please restart the app and sign in again.")
                    }
                    return@launch
                }

                val result = repository.saveUserProfile(userId, currentProfile)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(isSaved = true)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = result.exceptionOrNull()?.message ?: "Failed to save profile"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("ProfileCreationVM", "Save error", e)

                _uiState.update {
                    it.copy(
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}