package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.ai.ReferralTemplateGenerator
import com.swipeapply.app.data.manager.GroqConsentManager
import com.swipeapply.app.data.model.EducationItem
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.ExperienceItem
import com.swipeapply.app.data.model.ProjectItem
import com.swipeapply.app.data.model.ReferralEmail
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.notifications.NotificationScheduler
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

private const val TAG = "ReferralEmailVM"

/**
 * UI State for the Referral Email Composer screen.
 */
data class ReferralEmailUiState(
    // Editable email fields
    val toName: String = "",
    val toEmail: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val subject: String = "",
    val body: String = "",
    
    // Context
    val companyName: String = "",
    val jobTitle: String? = null,
    val employeeRole: String? = null,
    
    // UI State
    val isGenerating: Boolean = false,
    val isEmailSent: Boolean = false,
    val generationError: String? = null,
    val aiGenerated: Boolean = false,
    val showFromField: Boolean = false,  // Toggle to show/hide "From" editor
    val requiresGroqConsent: Boolean = false
) {
    /**
     * Build a ReferralEmail object from current state.
     */
    fun toReferralEmail(): ReferralEmail {
        return ReferralEmail(
            fromName = fromName,
            fromEmail = fromEmail,
            toName = toName,
            toEmail = toEmail,
            subject = subject,
            body = body,
            companyName = companyName,
            jobTitle = jobTitle
        )
    }
    
    /**
     * Check if email is ready to send.
     */
    fun isValid(): Boolean {
        return toEmail.isNotBlank() &&
               toEmail.contains("@") &&
               subject.isNotBlank() &&
               body.isNotBlank()
    }
}

/**
 * ViewModel for the Referral Email Composer.
 * 
 * Handles:
 * - AI-powered email generation based on user profile & target employee
 * - Email field editing
 * - Email sending via intent
 */
class ReferralEmailViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ReferralEmailUiState())
    val uiState: StateFlow<ReferralEmailUiState> = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val consentManager = GroqConsentManager.getInstance(application)

    private var currentEmployee: Employee? = null
    private var cachedProfile: UserProfile? = null
    private var profileFetched = false

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Initialize the composer with employee and company details.
     * Requests a Groq-generated draft without static template prefill.
     */
    fun initialize(
        employee: Employee,
        companyName: String,
        jobTitle: String? = null
    ) {
        currentEmployee = employee

        // Set target context, then populate subject/body only from Groq.
        _uiState.update {
            it.copy(
                toName = employee.fullName,
                toEmail = employee.email,
                companyName = companyName,
                jobTitle = jobTitle,
                employeeRole = employee.jobTitle,
                subject = "",
                body = "",
                isGenerating = consentManager.hasOutreachConsent(),
                aiGenerated = false,
                generationError = null,
                isEmailSent = false,
                requiresGroqConsent = !consentManager.hasOutreachConsent()
            )
        }

        if (!consentManager.hasOutreachConsent()) {
            return
        }
        
        // Fetch profile and generate AI email
        viewModelScope.launch {
            fetchProfileThenGenerate(employee, companyName, jobTitle)
        }
    }

    /**
     * Regenerate the email with AI.
     */
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

        val employee = currentEmployee ?: return
        val state = _uiState.value
        
        viewModelScope.launch {
            generateEmail(employee, state.companyName, state.jobTitle)
        }
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

        val employee = currentEmployee ?: return
        val state = _uiState.value
        viewModelScope.launch {
            fetchProfileThenGenerate(employee, state.companyName, state.jobTitle)
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

    /**
     * Update individual fields.
     */
    fun updateToName(value: String) {
        _uiState.update { it.copy(toName = value) }
    }

    fun updateToEmail(value: String) {
        _uiState.update { it.copy(toEmail = value) }
    }

    fun updateFromName(value: String) {
        _uiState.update { it.copy(fromName = value) }
    }

    fun updateFromEmail(value: String) {
        _uiState.update { it.copy(fromEmail = value) }
    }

    fun updateSubject(value: String) {
        _uiState.update { it.copy(subject = value) }
    }

    fun updateBody(value: String) {
        _uiState.update { it.copy(body = value) }
    }

    fun toggleFromField() {
        _uiState.update { it.copy(showFromField = !it.showFromField) }
    }

    fun markEmailSent() {
        val state = _uiState.value

        NotificationScheduler.scheduleIntroFollowUp(
            context = getApplication(),
            recipientName = state.toName,
            companyName = state.companyName
        )

        _uiState.update { it.copy(isEmailSent = true) }
    }

    fun resetSentState() {
        _uiState.update { it.copy(isEmailSent = false) }
    }

    /**
     * Get full email text for copying.
     */
    fun getFullEmailText(): String {
        return _uiState.value.toReferralEmail().toFullText()
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private suspend fun fetchProfileThenGenerate(
        employee: Employee,
        companyName: String,
        jobTitle: String?
    ) {
        if (!profileFetched) {
            cachedProfile = fetchProfile()
            profileFetched = true
            
            // Update from fields with profile data
            cachedProfile?.let { profile ->
                _uiState.update { 
                    it.copy(
                        fromName = profile.fullName,
                        fromEmail = profile.email
                    )
                }
            }
        }
        generateEmail(employee, companyName, jobTitle)
    }

    private suspend fun generateEmail(
        employee: Employee,
        companyName: String,
        jobTitle: String?
    ) {
        if (!hasPersonalizationProfile(cachedProfile)) {
            _uiState.update {
                it.copy(
                    isGenerating = false,
                    aiGenerated = false,
                    generationError = "Add your parsed resume details in Profile to generate a personalized outreach email."
                )
            }
            return
        }
        val personalizationProfile = cachedProfile ?: return

        _uiState.update { it.copy(isGenerating = true, generationError = null) }

        val result = ReferralTemplateGenerator.generate(
            employee = employee,
            companyName = companyName,
            userProfile = personalizationProfile,
            jobTitle = jobTitle
        )

        if (result != null) {
            Log.d(TAG, "AI template generated for ${employee.email}")
            _uiState.update {
                it.copy(
                    subject = result.subject,
                    body = result.body,
                    isGenerating = false,
                    aiGenerated = true,
                    generationError = null
                )
            }
        } else {
            Log.w(TAG, "Groq generation failed")
            _uiState.update {
                it.copy(
                    isGenerating = false, 
                    generationError = "Groq could not generate a template. Check API key/network and retry."
                )
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
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id 
                ?: return@withContext null
            
            val response = SupabaseClient.client
                .from("profiles")
                .select { filter { eq("id", userId) } }
            
            val data = response.data
            if (data == "[]" || data.isNullOrEmpty()) return@withContext null
            
            val profiles = jsonParser.decodeFromString<List<ProfileResponse>>(data)
            val p = profiles.firstOrNull() ?: return@withContext null
            
            UserProfile(
                fullName = p.full_name ?: "",
                email = p.email ?: "",
                phone = p.phone ?: "",
                bio = p.bio ?: "",
                skills = p.skills ?: emptyList(),
                techStack = p.tech_stack ?: emptyList(),
                education = p.education ?: emptyList(),
                experience = p.experience ?: emptyList(),
                projects = p.projects ?: emptyList()
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
