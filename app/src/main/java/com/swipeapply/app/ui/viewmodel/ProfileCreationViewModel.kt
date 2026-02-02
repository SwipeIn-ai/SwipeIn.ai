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
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val isSaved: Boolean = false,
    val error: String? = null
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = ResumeParser.parseResume(context, uri)

            if (result != null) {
                Log.d("ProfileCreationVM", "Resume parsed successfully")
                _uiState.update {
                    it.copy(isLoading = false, profile = result)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Could not parse resume. Please fill manually.",
                        profile = UserProfile()
                    )
                }
            }
        }
    }

    fun updateProfileField(newProfile: UserProfile) {
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

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id

                if (userId == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "User not logged in")
                    }
                    return@launch
                }

                val result = repository.saveUserProfile(userId, currentProfile)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(isLoading = false, isSaved = true)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to save profile"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("ProfileCreationVM", "Save error", e)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }
}
