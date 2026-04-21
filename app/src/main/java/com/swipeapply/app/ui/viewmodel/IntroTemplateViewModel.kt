package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.ai.IntroTemplateGenerator
import com.swipeapply.app.data.manager.GroqConsentManager
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.UserProfile
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

private const val TAG = "IntroTemplateVM"

/**
 * UI State for intro template screen
 */
data class IntroTemplateUiState(
    val subject: String = "",
    val body: String = "",
    val isCopied: Boolean = false,
    val isGenerating: Boolean = false,
    val generationError: String? = null,
    val aiGenerated: Boolean = false,
    val requiresGroqConsent: Boolean = false
)

/**
 * ViewModel for the Intro Template screen.
 * Fetches the user profile, then generates a personalised email via AI.
 * Uses Groq output only (no static template prefill).
 */
class IntroTemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IntroTemplateUiState())
    val uiState: StateFlow<IntroTemplateUiState> = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val consentManager = GroqConsentManager.getInstance(application)

    private var currentJobCard: JobCard? = null
    private var cachedProfile: UserProfile? = null
    private var profileFetched = false

    // ─── Public API ────────────────────────────────────────────────────────────

    /**
     * Call once when the screen opens.
     * Clears any previous draft and requests a fresh Groq-generated template.
     */
    fun initializeAndGenerate(jobCard: JobCard) {
        currentJobCard = jobCard

        if (!consentManager.hasOutreachConsent()) {
            _uiState.update {
                it.copy(
                    subject = "",
                    body = "",
                    isCopied = false,
                    isGenerating = false,
                    aiGenerated = false,
                    generationError = null,
                    requiresGroqConsent = true
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                subject = "",
                body = "",
                isCopied = false,
                isGenerating = true,
                aiGenerated = false,
                generationError = null,
                requiresGroqConsent = false
            )
        }
        viewModelScope.launch { fetchProfileThenGenerate(jobCard) }
    }

    /** Re-generate a fresh AI draft. */
    fun regenerate() {
        if (!consentManager.hasOutreachConsent()) {
            _uiState.update {
                it.copy(
                    requiresGroqConsent = true,
                    isGenerating = false,
                    generationError = "Consent is required to generate with Groq."
                )
            }
            return
        }

        val job = currentJobCard ?: return
        viewModelScope.launch { generate(job, cachedProfile) }
    }

    fun grantGroqConsent() {
        consentManager.grantOutreachConsent()
        _uiState.update {
            it.copy(
                requiresGroqConsent = false,
                generationError = null,
                isGenerating = true
            )
        }

        currentJobCard?.let { job ->
            viewModelScope.launch { fetchProfileThenGenerate(job) }
        }
    }

    fun declineGroqConsent() {
        _uiState.update {
            it.copy(
                requiresGroqConsent = true,
                isGenerating = false,
                generationError = "Consent is required before sharing profile data with Groq."
            )
        }
    }

    fun updateSubject(subject: String) {
        _uiState.update { it.copy(subject = subject, isCopied = false) }
    }

    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, isCopied = false) }
    }

    fun markAsCopied() {
        _uiState.update { it.copy(isCopied = true) }
    }

    fun resetCopiedState() {
        _uiState.update { it.copy(isCopied = false) }
    }

    fun getFullEmail(): String {
        val s = _uiState.value
        return "Subject: ${s.subject}\n\n${s.body}"
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private suspend fun fetchProfileThenGenerate(job: JobCard) {
        if (!profileFetched) {
            cachedProfile = fetchProfile()
            profileFetched = true
        }
        generate(job, cachedProfile)
    }

    private suspend fun generate(job: JobCard, profile: UserProfile?) {
        if (!hasPersonalizationProfile(profile)) {
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    aiGenerated = false,
                    generationError = "Add your parsed resume details in Profile to generate a personalized outreach email."
                )
            }
            return
        }
        val personalizationProfile = profile ?: return

        _uiState.update { it.copy(isGenerating = true, generationError = null) }
        val result = IntroTemplateGenerator.generate(job, personalizationProfile)
        if (result != null) {
            Log.d(TAG, "AI template ready for ${job.id}")
            _uiState.update {
                it.copy(
                    subject = result.subject,
                    body = result.body,
                    isGenerating = false,
                    aiGenerated = true,
                    isCopied = false,
                    generationError = null
                )
            }
        } else {
            Log.w(TAG, "Groq generation failed")
            _uiState.update {
                it.copy(isGenerating = false, generationError = "Groq could not generate a template. Check API key/network and retry.")
            }
        }
    }

    private fun hasPersonalizationProfile(profile: UserProfile?): Boolean {
        if (profile == null) return false
        return profile.skills.isNotEmpty() ||
            profile.techStack.isNotEmpty() ||
            profile.experience.isNotEmpty() ||
            profile.projects.isNotEmpty() ||
            profile.bio.isNotBlank()
    }

    private suspend fun fetchProfile(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@withContext null
            val response = SupabaseClient.client
                .from("profiles")
                .select { filter { eq("id", userId) } }
            val data = response.data
            if (data == "[]" || data.isNullOrEmpty()) return@withContext null
            val profiles = jsonParser.decodeFromString<List<ProfileResponse>>(data)
            val p = profiles.firstOrNull() ?: return@withContext null
            UserProfile(
                fullName   = p.full_name ?: "",
                email      = p.email ?: "",
                phone      = p.phone ?: "",
                bio        = p.bio ?: "",
                skills     = p.skills ?: emptyList(),
                techStack  = p.tech_stack ?: emptyList(),
                education  = p.education ?: emptyList(),
                experience = p.experience ?: emptyList(),
                projects   = p.projects ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Profile fetch failed: ${e.message}")
            null
        }
    }

    @Serializable
    private data class ProfileResponse(
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
}
