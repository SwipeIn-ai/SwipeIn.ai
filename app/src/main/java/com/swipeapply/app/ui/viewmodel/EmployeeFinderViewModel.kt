package com.swipeapply.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.model.Employee
import com.swipeapply.app.data.model.EmployeeSearchResponse
import com.swipeapply.app.data.repository.EmployeeRepository
import com.swipeapply.app.data.repository.SavedContactsRepository
import com.swipeapply.app.data.repository.UserSyncRepository

import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "EmployeeFinderVM"

/**
 * UI state for the Employee Finder screen.
 */
data class EmployeeFinderUiState(
    val companyName: String = "",
    val companyDomain: String = "",
    val employees: List<Employee> = emptyList(),
    val currentIndex: Int = 0,
    val savedContacts: List<Employee> = emptyList(),
    val dismissedContacts: List<Employee> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val remainingSwipes: Int = -1,
    val totalAvailable: Int = 0,
    val source: String = "",
    val isEmpty: Boolean = false,
    val allReviewed: Boolean = false
) {
    val currentEmployee: Employee?
        get() = employees.getOrNull(currentIndex)

    val visibleEmployees: List<Employee>
        get() = employees.drop(currentIndex).take(3)

    val reviewedCount: Int
        get() = savedContacts.size + dismissedContacts.size

    val progressFraction: Float
        get() = if (employees.isEmpty()) 0f 
                else reviewedCount.toFloat() / employees.size.toFloat()
}

/**
 * ViewModel for the Employee Finder / Referral Contacts screen.
 * Handles loading employee data and managing swipe state.
 */
class EmployeeFinderViewModel : ViewModel() {

    private val repository = EmployeeRepository.getInstance()

    private val _uiState = MutableStateFlow(EmployeeFinderUiState())
    val uiState: StateFlow<EmployeeFinderUiState> = _uiState.asStateFlow()

    /**
     * Returns a backend user ID that is guaranteed to exist in the SwipeIn backend.
     *
     * Flow:
     * 1. Await Supabase session restore (fixes race condition on app start)
     * 2. Read the real Google/LinkedIn auth UID + email from Supabase
     * 3. Call POST /api/v1/users/sync with {supabaseId, email} so the backend
     *    creates-or-returns a user whose primary key == the Supabase UUID
     * 4. Cache the result for the rest of the session
     *
     * This means X-User-ID will always be valid on the backend, ending the 500 errors.
     */
    private suspend fun getCurrentUserId(): String {
        SupabaseClient.client.auth.awaitInitialization()
        val user = SupabaseClient.client.auth.currentUserOrNull()
        val supabaseId = user?.id
        val email = user?.email
        Log.d(TAG, "Supabase user: id=$supabaseId, email=$email")
        // Throws IllegalStateException if supabaseId is null (not authenticated)
        return UserSyncRepository.syncAndGetUserId(
            supabaseId = supabaseId,
            email = email,
            displayName = user?.userMetadata?.get("full_name")?.toString()
        )
    }

    /**
     * Load employees for a given company.
     * Called when the user swipes right on a job card and navigates to this screen.
     */
    fun loadEmployees(companyName: String) {
        if (_uiState.value.isLoading) return

        _uiState.update { 
            it.copy(
                companyName = companyName,
                isLoading = true, 
                error = null,
                employees = emptyList(),
                currentIndex = 0,
                savedContacts = emptyList(),
                dismissedContacts = emptyList(),
                isEmpty = false,
                allReviewed = false
            ) 
        }

        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()

                // Clean company name: remove suffixes, extract domain-friendly identifier
                val cleanedIdentifier = cleanCompanyIdentifier(companyName)
                Log.d(TAG, "Loading employees for '$companyName' → identifier: '$cleanedIdentifier'")

                val result = repository.fetchEmployees(cleanedIdentifier, userId)

                result.onSuccess { response ->
                    Log.d(TAG, "✅ Loaded ${response.employees.size} employees")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            employees = response.employees,
                            companyDomain = response.companyDomain,
                            remainingSwipes = response.remainingSwipes,
                            totalAvailable = response.totalAvailable,
                            source = response.source,
                            isEmpty = response.employees.isEmpty(),
                            error = if (response.employees.isEmpty())
                                response.message ?: "No employee contacts found for this company"
                            else null
                        )
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "❌ Failed: ${exception.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load employee contacts",
                            isEmpty = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Auth/setup error: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Authentication required. Please sign in.",
                        isEmpty = true
                    )
                }
            }
        }
    }

    /**
     * Save the current employee contact (right swipe equivalent - interested in referral).
     */
    fun saveContact() {
        val current = _uiState.value.currentEmployee ?: return
        _uiState.update { state ->
            val newSaved = state.savedContacts + current
            val newIndex = state.currentIndex + 1
            val allDone = newIndex >= state.employees.size
            SavedContactsRepository.saveContacts(state.companyName, newSaved)
            state.copy(
                savedContacts = newSaved,
                currentIndex = newIndex,
                allReviewed = allDone
            )
        }
    }

    /**
     * Dismiss the current employee contact (left swipe equivalent - not interested).
     */
    fun dismissContact() {
        val current = _uiState.value.currentEmployee ?: return
        _uiState.update { state ->
            val newDismissed = state.dismissedContacts + current
            val newIndex = state.currentIndex + 1
            state.copy(
                dismissedContacts = newDismissed,
                currentIndex = newIndex,
                allReviewed = newIndex >= state.employees.size
            )
        }
    }

    /**
     * Undo the last action.
     */
    fun undoLast() {
        _uiState.update { state ->
            if (state.currentIndex <= 0) return@update state

            val prevIndex = state.currentIndex - 1
            val prevEmployee = state.employees.getOrNull(prevIndex) ?: return@update state

            val wasSaved = state.savedContacts.any { it.email == prevEmployee.email }
            
            state.copy(
                currentIndex = prevIndex,
                savedContacts = if (wasSaved) state.savedContacts.dropLast(1) else state.savedContacts,
                dismissedContacts = if (!wasSaved) state.dismissedContacts.dropLast(1) else state.dismissedContacts,
                allReviewed = false
            )
        }
    }

    /**
     * Reset and re-review all employees.
     */
    fun resetReview() {
        _uiState.update {
            it.copy(
                currentIndex = 0,
                savedContacts = emptyList(),
                dismissedContacts = emptyList(),
                allReviewed = false
            )
        }
    }

    /**
     * Clean company name to create a suitable API identifier.
     * Examples:
     *   "Google LLC" → "google"
     *   "MAQ Software" → "maqsoftware"  
     *   "Amazon.com, Inc." → "amazon.com"
     */
    private fun cleanCompanyIdentifier(name: String): String {
        // If it already looks like a domain, use it
        if (name.contains(".") && !name.contains(" ")) {
            return name.lowercase().trim()
        }

        // Remove common corporate suffixes
        val suffixes = listOf(
            " inc.", " inc", " llc", " ltd", " ltd.", " corp", " corp.",
            " corporation", " company", " co.", " co", " plc", " gmbh",
            " ag", " sa", " s.a.", " pvt", " pvt.", " private", " limited"
        )

        var cleaned = name.lowercase().trim()
        for (suffix in suffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.removeSuffix(suffix).trim()
            }
        }

        // Remove special characters except dots and hyphens
        cleaned = cleaned.replace(Regex("[^a-z0-9.\\-]"), "")
        
        return cleaned.ifEmpty { name.lowercase().replace(" ", "") }
    }
}
