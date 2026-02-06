package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.data.model.SwipeResult
import com.swipeapply.app.data.model.UserProfile
import com.swipeapply.app.data.repository.JobRepository
import com.swipeapply.app.data.service.JobRankingService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.math.min

private const val TAG = "HomeViewModel"

/**
 * Represents a single undo-able action
 */
data class UndoableAction(
    val card: JobCard,
    val result: SwipeResult,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * UI State for the Home/Swipe screen
 */
data class HomeUiState(
    val cards: List<JobCard> = emptyList(),
    val interestedCards: List<JobCard> = emptyList(),
    val skippedCards: List<JobCard> = emptyList(),
    val selectedCard: JobCard? = null,
    val showBottomSheet: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val undoHistory: List<UndoableAction> = emptyList(),
    val canUndo: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = true,
    val totalJobs: Int = 0,
    // AI Ranking state
    val isAiSortEnabled: Boolean = true,
    val isRanking: Boolean = false,
    val userProfile: UserProfile? = null
)

/**
 * ViewModel for the Home/Swipe screen.
 * Manages card stack state and swipe actions.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JobRepository.getInstance(
        context = application.applicationContext,
        apiKey = ApiConfig.FINDWORK_API_KEY
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        // Try to set current user from Supabase auth
        initializeCurrentUser()
        // Fetch user profile for AI ranking
        fetchUserProfileForRanking()
        loadCards()
    }
    
    /**
     * Initialize current user ID from Supabase auth (if logged in)
     */
    private fun initializeCurrentUser() {
        try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            repository.setCurrentUser(userId)
            Log.d(TAG, "Initialized user: ${userId ?: "local_user (dev mode)"}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not get current user, using local_user: ${e.message}")
            repository.setCurrentUser(null)
        }
    }

    /**
     * Fetch user profile from Supabase for AI ranking
     */
    private fun fetchUserProfileForRanking() {
        viewModelScope.launch {
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (userId == null) {
                    Log.d(TAG, "No user logged in, AI ranking will use basic sorting")
                    return@launch
                }

                val profile = fetchProfileFromSupabase(userId)
                if (profile != null) {
                    _uiState.update { it.copy(userProfile = profile) }
                    Log.d(TAG, "Loaded user profile for AI ranking: ${profile.fullName}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch profile for AI ranking: ${e.message}")
            }
        }
    }

    private suspend fun fetchProfileFromSupabase(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val response = SupabaseClient.client
                .from("profiles")
                .select { filter { eq("id", userId) } }

            val data = response.data
            if (data == "[]" || data.isNullOrEmpty()) return@withContext null

            val profiles = jsonParser.decodeFromString<List<ProfileResponse>>(data)
            val profileData = profiles.firstOrNull() ?: return@withContext null

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
            Log.e(TAG, "Error fetching profile: ${e.message}")
            return@withContext null
        }
    }

    /**
     * Load initial cards from repository
     */
    private fun loadCards() {
        Log.d(TAG, "loadCards() - Starting...")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                repository.getJobCards(
                    search = ApiConfig.DEFAULT_SEARCH_QUERY,
                    location = ApiConfig.DEFAULT_LOCATION,
                    remote = ApiConfig.DEFAULT_REMOTE_ONLY
                ).collect { result ->
                    result.fold(
                        onSuccess = { cards ->
                            Log.d(TAG, "loadCards() - SUCCESS: Got ${cards.size} cards")
                            
                            // Apply AI ranking if enabled and profile exists
                            val sortedCards = applyAiRankingIfEnabled(cards)
                            
                            _uiState.update {
                                it.copy(
                                    cards = sortedCards,
                                    isLoading = false,
                                    isEmpty = sortedCards.isEmpty(),
                                    error = null,
                                    hasMorePages = true,
                                    totalJobs = repository.getTotalCount()
                                )
                            }
                        },
                        onFailure = { exception ->
                            val errorMessage = exception.message ?: "Failed to load jobs"
                            Log.e(TAG, "loadCards() - FAILURE: $errorMessage", exception)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = errorMessage
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "An unexpected error occurred"
                Log.e(TAG, "loadCards() - EXCEPTION: $errorMessage", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            }
        }
    }

    /**
     * Apply AI ranking to jobs if enabled and user profile exists
     */
    private suspend fun applyAiRankingIfEnabled(jobs: List<JobCard>): List<JobCard> {
        val state = _uiState.value
        
        if (!state.isAiSortEnabled) {
            Log.d(TAG, "AI sorting is disabled")
            return jobs
        }

        val profile = state.userProfile
        if (profile == null) {
            Log.d(TAG, "No user profile, using quick match scoring")
            // Use quick local matching as fallback
            return jobs.sortedByDescending { job ->
                JobRankingService.quickMatchScore(job, UserProfile())
            }
        }

        // Check if profile has enough data for AI ranking
        if (profile.techStack.isEmpty() && profile.skills.isEmpty() && profile.bio.isEmpty()) {
            Log.d(TAG, "Profile is empty, skipping AI ranking")
            return jobs
        }

        _uiState.update { it.copy(isRanking = true) }
        
        return try {
            Log.d(TAG, "Applying AI ranking to ${jobs.size} jobs...")
            val rankedJobs = JobRankingService.rankJobs(jobs, profile)
            Log.d(TAG, "AI ranking complete")
            rankedJobs
        } catch (e: Exception) {
            Log.e(TAG, "AI ranking failed: ${e.message}", e)
            jobs // Return original order on failure
        } finally {
            _uiState.update { it.copy(isRanking = false) }
        }
    }

    /**
     * Toggle AI sorting on/off
     */
    fun toggleAiSort() {
        _uiState.update { it.copy(isAiSortEnabled = !it.isAiSortEnabled) }
        // Reload cards with new sorting preference
        loadCards()
    }

    /**
     * Manually trigger AI re-ranking of current cards
     */
    fun reRankJobs() {
        viewModelScope.launch {
            val currentCards = _uiState.value.cards
            if (currentCards.isEmpty()) return@launch
            
            val rankedCards = applyAiRankingIfEnabled(currentCards)
            _uiState.update { it.copy(cards = rankedCards) }
        }
    }

   

    /**
     * Refresh cards from API
     */
    fun refreshCards() {
    // Reset UI state first
    _uiState.update { it.copy(cards = emptyList(), isLoading = true, error = null) }
    
    viewModelScope.launch {
        try {
            repository.getJobCards(
                search = ApiConfig.DEFAULT_SEARCH_QUERY,
                location = ApiConfig.DEFAULT_LOCATION,
                remote = ApiConfig.DEFAULT_REMOTE_ONLY,
                forceRefresh = true
            ).collect { result ->
                result.fold(
                    onSuccess = { cards ->
                        _uiState.update { 
                            it.copy(
                                cards = cards, 
                                isLoading = false, 
                                isEmpty = cards.isEmpty()
                            ) 
                        }
                    },
                    onFailure = { e ->
                        _uiState.update { 
                            it.copy(isLoading = false, error = e.message) 
                        }
                    }
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }
}

// Replace your existing onCardSwiped with this:
fun onCardSwiped(card: JobCard, direction: SwipeDirection) {
        viewModelScope.launch {
            // 1. Record Swipe
            repository.recordSwipe(card.id, direction.name)

            // 2. Fetch exactly ONE replacement job from queue
            val nextJob = repository.getNextJobFromQueue(
                search = ApiConfig.DEFAULT_SEARCH_QUERY,
                location = ApiConfig.DEFAULT_LOCATION,
                remote = ApiConfig.DEFAULT_REMOTE_ONLY
            )

            _uiState.update { state ->
                // 3. Remove swiped card
                val currentList = state.cards.toMutableList()
                currentList.removeIf { it.id == card.id }

                // 4. Add replacement to back
                if (nextJob != null) {
                    currentList.add(nextJob)
                }

                // 5. Update State
                val result = SwipeResult(card.id, direction, direction == SwipeDirection.RIGHT)
                val undoableAction = UndoableAction(card, result)

                state.copy(
                    cards = currentList,
                    isEmpty = currentList.isEmpty() && nextJob == null,
                    interestedCards = if (direction == SwipeDirection.RIGHT) state.interestedCards + card else state.interestedCards,
                    skippedCards = if (direction == SwipeDirection.LEFT) state.skippedCards + card else state.skippedCards,
                    undoHistory = state.undoHistory + undoableAction,
                    canUndo = true
                )
            }
        }
    }

    fun selectCard(card: JobCard) {
        _uiState.update { it.copy(selectedCard = card, showBottomSheet = true) }
    }

    fun dismissBottomSheet() {
        _uiState.update { it.copy(showBottomSheet = false, selectedCard = null) }
    }

    fun undoLastSwipe() {
        viewModelScope.launch {
            val undoHistory = _uiState.value.undoHistory
            if (undoHistory.isEmpty()) return@launch

            val lastAction = undoHistory.last()
            val card = lastAction.card

            _uiState.update { state ->
                state.copy(
                    cards = listOf(card) + state.cards,
                    interestedCards = state.interestedCards.filter { it.id != card.id },
                    skippedCards = state.skippedCards.filter { it.id != card.id },
                    isEmpty = false,
                    undoHistory = undoHistory.dropLast(1),
                    canUndo = undoHistory.size > 1
                )
            }
        }
    }

    fun undoSwipes(count: Int) {
        viewModelScope.launch {
            val undoHistory = _uiState.value.undoHistory
            val actualCount = min(count, undoHistory.size)
            if (actualCount == 0) return@launch

            val actionsToUndo = undoHistory.takeLast(actualCount)
            val cardsToRestore = actionsToUndo.map { it.card }.reversed()

            _uiState.update { state ->
                val newInterestedCards = state.interestedCards.filter { card ->
                    actionsToUndo.none { it.card.id == card.id }
                }
                val newSkippedCards = state.skippedCards.filter { card ->
                    actionsToUndo.none { it.card.id == card.id }
                }

                state.copy(
                    cards = cardsToRestore + state.cards,
                    interestedCards = newInterestedCards,
                    skippedCards = newSkippedCards,
                    isEmpty = false,
                    undoHistory = undoHistory.dropLast(actualCount),
                    canUndo = undoHistory.size > actualCount
                )
            }
        }
    }

    fun clearUndoHistory() {
        _uiState.update { it.copy(undoHistory = emptyList(), canUndo = false) }
    }

    fun getUndoCount(): Int = _uiState.value.undoHistory.size

    fun resetCards() {
        _uiState.update {
            it.copy(
                interestedCards = emptyList(),
                skippedCards = emptyList(),
                undoHistory = emptyList(),
                canUndo = false
            )
        }
        loadCards()
    }

    fun getStats(): SwipeStats {
        val state = _uiState.value
        return SwipeStats(
            interested = state.interestedCards.size,
            skipped = state.skippedCards.size,
            remaining = state.cards.size,
            undoCount = state.undoHistory.size
        )
    }
    fun performSignOut() {
        viewModelScope.launch {
            // WIPE ALL DATA so the next user (or same user) gets a fresh start
            repository.clearAllLocalData()
            _uiState.update { HomeUiState() } // Reset UI state
            Log.d(TAG, "Sign out complete - local data cleared")
        }
    }
    fun debugClearHistory() {
    viewModelScope.launch {
        // Clear DB table
        repository.clearSwipeHistory() 
        // Reset UI
        refreshCards()
    }
}
}

data class SwipeStats(
    val interested: Int,
    val skipped: Int,
    val remaining: Int,
    val undoCount: Int
)

/**
 * Response model for Supabase profile fetch (used internally by HomeViewModel)
 */
@kotlinx.serialization.Serializable
private data class ProfileResponse(
    val id: String,
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val bio: String? = null,
    val skills: List<String>? = null,
    val tech_stack: List<String>? = null,
    val education: List<com.swipeapply.app.data.model.EducationItem>? = null,
    val experience: List<com.swipeapply.app.data.model.ExperienceItem>? = null,
    val projects: List<com.swipeapply.app.data.model.ProjectItem>? = null
)