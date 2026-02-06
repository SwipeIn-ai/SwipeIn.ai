package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.repository.JobRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val TAG = "ProfileViewModel"

/**
 * UI State for Profile Screen
 */
data class ProfileUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    // Editable fields (separate from profile for real-time editing)
    val editFullName: String = "",
    val editEmail: String = "",
    val editPhone: String = "",
    val editBio: String = "",
    val editSkills: String = "", // Comma-separated for editing
    val editTechStack: String = "", // Comma-separated for editing
)

/**
 * Response model for Supabase profile fetch
 */
@Serializable
data class SupabaseProfileResponse(
    val id: String,
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val skills: List<String>? = null,
    val tech_stack: List<String>? = null,
    val education: List<EducationItem>? = null,
    val experience: List<ExperienceItem>? = null,
    val projects: List<ProjectItem>? = null
)

/**
 * ViewModel for Profile Screen
 * Handles fetching, editing, and saving user profile data
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JobRepository.getInstance(
        context = application.applicationContext,
        apiKey = ApiConfig.FINDWORK_API_KEY
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        fetchProfile()
    }

    /**
     * Fetch user profile from Supabase
     */
    fun fetchProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _uiState.update { 
                        it.copy(isLoading = false, error = "Not logged in") 
                    }
                    return@launch
                }

                val profile = fetchProfileFromSupabase(userId)
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            isLoading = false,
                            // Pre-populate edit fields
                            editFullName = profile.fullName,
                            editEmail = profile.email,
                            editPhone = profile.phone,
                            editBio = profile.bio,
                            editSkills = profile.skills.joinToString(", "),
                            editTechStack = profile.techStack.joinToString(", ")
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Profile not found")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profile", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load profile")
                }
            }
        }
    }

    private suspend fun fetchProfileFromSupabase(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.client
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                }

            val data = response.data
            Log.d(TAG, "Fetched profile data: $data")

            if (data == "[]" || data.isNullOrEmpty()) {
                return@withContext null
            }

            // Parse the JSON array and get first item
            val profiles = jsonParser.decodeFromString<List<SupabaseProfileResponse>>(data)
            val profileData = profiles.firstOrNull() ?: return@withContext null

            // Convert to UserProfile
            return@withContext UserProfile(
                fullName = profileData.full_name ?: "",
                email = profileData.email ?: "",
                phone = profileData.phone ?: "",
                bio = profileData.bio ?: "",
                skills = profileData.skills ?: emptyList(),
                techStack = profileData.tech_stack ?: emptyList(),
                education = profileData.education ?: emptyList(),
                experience = profileData.experience ?: emptyList(),
                projects = profileData.projects ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing profile", e)
            return@withContext null
        }
    }

    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        val currentState = _uiState.value
        if (currentState.isEditMode) {
            // Exiting edit mode without saving - restore original values
            val profile = currentState.profile
            _uiState.update {
                it.copy(
                    isEditMode = false,
                    editFullName = profile?.fullName ?: "",
                    editEmail = profile?.email ?: "",
                    editPhone = profile?.phone ?: "",
                    editBio = profile?.bio ?: "",
                    editSkills = profile?.skills?.joinToString(", ") ?: "",
                    editTechStack = profile?.techStack?.joinToString(", ") ?: ""
                )
            }
        } else {
            _uiState.update { it.copy(isEditMode = true) }
        }
    }

    /**
     * Update edit fields
     */
    fun updateFullName(value: String) {
        _uiState.update { it.copy(editFullName = value) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(editEmail = value) }
    }

    fun updatePhone(value: String) {
        _uiState.update { it.copy(editPhone = value) }
    }

    fun updateBio(value: String) {
        _uiState.update { it.copy(editBio = value) }
    }

    fun updateSkills(value: String) {
        _uiState.update { it.copy(editSkills = value) }
    }

    fun updateTechStack(value: String) {
        _uiState.update { it.copy(editTechStack = value) }
    }

    /**
     * Save profile to Supabase
     */
    fun saveProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }

            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Not logged in")
                    }
                    return@launch
                }

                val currentState = _uiState.value
                val currentProfile = currentState.profile

                // Validate
                if (currentState.editFullName.isBlank()) {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Name cannot be empty")
                    }
                    return@launch
                }

                // Build updated profile
                val updatedProfile = UserProfile(
                    fullName = currentState.editFullName.trim(),
                    email = currentState.editEmail.trim(),
                    phone = currentState.editPhone.trim(),
                    bio = currentState.editBio.trim(),
                    skills = currentState.editSkills
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                    techStack = currentState.editTechStack
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() },
                    // Keep existing complex fields
                    education = currentProfile?.education ?: emptyList(),
                    experience = currentProfile?.experience ?: emptyList(),
                    projects = currentProfile?.projects ?: emptyList()
                )

                // Save to Supabase
                val result = repository.saveUserProfile(userId, updatedProfile)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            profile = updatedProfile,
                            isSaving = false,
                            isEditMode = false,
                            saveSuccess = true
                        )
                    }
                    Log.d(TAG, "Profile saved successfully")
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to save"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Failed to save profile")
                }
            }
        }
    }

    /**
     * Clear success/error messages
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, saveSuccess = false) }
    }

    /**
     * Get current profile for AI ranking
     */
    fun getCurrentProfile(): UserProfile? = _uiState.value.profile
}
