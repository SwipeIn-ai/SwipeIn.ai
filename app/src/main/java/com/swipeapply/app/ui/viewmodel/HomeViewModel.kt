package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.manager.StreakManager
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

data class UndoableAction(
    val card: JobCard,
    val result: SwipeResult,
    val timestamp: Long = System.currentTimeMillis()
)

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
    val isRankingEnabled: Boolean = true,
    val isRanking: Boolean = false,
    val userProfile: UserProfile? = null,
    val searchQuery: String? = null,  // Dynamic search based on profile
    val currentStreak: Int = 0,
    val todaySwipeCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JobRepository.getInstance(
        context = application.applicationContext,
        apiKey = ApiConfig.FINDWORK_API_KEY
    )

    private val streakManager = StreakManager.getInstance(application.applicationContext)

    private val _uiState = MutableStateFlow(
        HomeUiState(
            currentStreak = streakManager.currentStreak,
            todaySwipeCount = streakManager.todaySwipeCount
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        JobRankingService.initialize(application.applicationContext)
        initializeCurrentUser()
        // First fetch profile, then load cards with profile-based search
        fetchUserProfileThenLoadCards()
    }
    
    private fun initializeCurrentUser() {
        try {
            val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
            repository.setCurrentUser(userId)
            Log.d(TAG, "Initialized user: ${userId ?: "local_user (dev mode)"}")
        } catch (e: Exception) {
            Log.w(TAG, "Could not get current user: ${e.message}")
            repository.setCurrentUser(null)
        }
    }

    /**
     * Fetch profile first, build search query, THEN load cards.
     * This ensures HR resume gets HR jobs, SDE resume gets SDE jobs.
     */
    private fun fetchUserProfileThenLoadCards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                
                if (userId != null) {
                    val profile = fetchProfileFromSupabase(userId)
                    if (profile != null) {
                        val searchQuery = buildSearchQueryFromProfile(profile)
                        _uiState.update { 
                            it.copy(
                                userProfile = profile,
                                searchQuery = searchQuery
                            )
                        }
                        Log.d(TAG, "Built search query from profile: $searchQuery")
                    }
                }
                
                // Now load cards with the profile-based search query
                loadCardsWithCurrentSearch()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profile: ${e.message}")
                // Fall back to loading without profile filter
                loadCardsWithCurrentSearch()
            }
        }
    }

    /**
     * Build search query from user's skills and tech stack.
     * This ensures different resumes see different jobs.
     * 
     * Strategy:
     * - Use ALL skills and tech keywords (no limits)
     * - Add role-based keywords from experience
     * - Combine with OR logic for MAXIMUM results
     * - Example: "python OR java OR react" shows all related jobs
     */
    private fun buildSearchQueryFromProfile(profile: UserProfile): String? {
        val allKeywords = mutableListOf<String>()
        
        // Add ALL skills (no limit)
        profile.skills.forEach { skill ->
            allKeywords.add(skill.lowercase().trim())
        }
        
        // Add ALL tech stack (no limit)
        profile.techStack.forEach { tech ->
            allKeywords.add(tech.lowercase().trim())
        }
        
        // Extract keywords from ALL experience roles
        profile.experience.forEach { exp ->
            val title = exp.role.lowercase()
            // Add the role itself as keyword
            title.split(" ").forEach { word ->
                if (word.length > 2) allKeywords.add(word)
            }
            // Add related keywords based on role type
            when {
                title.contains("hr") || title.contains("recruit") || title.contains("talent") -> {
                    allKeywords.addAll(listOf("hr", "recruiter", "talent", "human resources", "hiring"))
                }
                title.contains("manager") -> allKeywords.addAll(listOf("manager", "management", "lead"))
                title.contains("engineer") || title.contains("developer") -> {
                    allKeywords.addAll(listOf("engineer", "developer", "software", "programming"))
                }
                title.contains("design") -> allKeywords.addAll(listOf("designer", "design", "ui", "ux"))
                title.contains("market") -> allKeywords.addAll(listOf("marketing", "growth", "seo", "content"))
                title.contains("product") -> allKeywords.addAll(listOf("product", "pm", "roadmap"))
                title.contains("data") -> allKeywords.addAll(listOf("data", "analytics", "ml", "ai"))
                title.contains("devops") || title.contains("sre") -> {
                    allKeywords.addAll(listOf("devops", "sre", "infrastructure", "cloud"))
                }
                title.contains("frontend") || title.contains("front-end") -> {
                    allKeywords.addAll(listOf("frontend", "react", "vue", "angular", "javascript"))
                }
                title.contains("backend") || title.contains("back-end") -> {
                    allKeywords.addAll(listOf("backend", "api", "server", "database"))
                }
                title.contains("fullstack") || title.contains("full-stack") -> {
                    allKeywords.addAll(listOf("fullstack", "full-stack", "frontend", "backend"))
                }
            }
        }
        
        // Remove duplicates and empty strings - NO LIMIT on count
        val uniqueKeywords = allKeywords
            .filter { it.isNotBlank() && it.length > 1 }
            .distinct()
        
        if (uniqueKeywords.isEmpty()) {
            Log.d(TAG, "No keywords found in profile, using default search")
            return null
        }
        
        // Join ALL keywords with " OR " for MAXIMUM results
        // This ensures C++ AND Python jobs both show up
        val query = uniqueKeywords.joinToString(" OR ")
        Log.d(TAG, "🔍 Full keyword list (${uniqueKeywords.size} keywords): $uniqueKeywords")
        Log.d(TAG, "🔍 Search query: $query")
        return query
    }

    /**
     * Get current search query - from profile or fall back to default
     */
    private fun getCurrentSearchQuery(): String? {
        return _uiState.value.searchQuery ?: ApiConfig.DEFAULT_SEARCH_QUERY
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

    private fun loadCardsWithCurrentSearch() {
        val searchQuery = getCurrentSearchQuery()
        Log.d(TAG, "loadCardsWithCurrentSearch() - query: $searchQuery")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                repository.getJobCards(
                    search = searchQuery,
                    location = ApiConfig.DEFAULT_LOCATION,
                    remote = ApiConfig.DEFAULT_REMOTE_ONLY
                ).collect { result ->
                    result.fold(
                        onSuccess = { cards ->
                            Log.d(TAG, "loadCards() - SUCCESS: Got ${cards.size} cards for query: $searchQuery")
                            val sortedCards = applyRankingIfEnabled(cards)
                            
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
                            Log.e(TAG, "loadCards() - FAILURE: ${exception.message}", exception)
                            _uiState.update {
                                it.copy(isLoading = false, error = exception.message ?: "Failed to load jobs")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCards() - EXCEPTION: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "An unexpected error occurred") }
            }
        }
    }

    private suspend fun applyRankingIfEnabled(jobs: List<JobCard>): List<JobCard> {
        val state = _uiState.value
        
        if (!state.isRankingEnabled) {
            Log.d(TAG, "Ranking is disabled")
            return jobs
        }

        val profile = state.userProfile
        if (profile == null) {
            Log.d(TAG, "No user profile, using quick match scoring")
            return jobs.sortedByDescending { job -> JobRankingService.quickMatchScore(job, UserProfile()) }
        }

        if (profile.techStack.isEmpty() && profile.skills.isEmpty()) {
            Log.d(TAG, "Profile is empty, skipping ranking")
            return jobs
        }

        _uiState.update { it.copy(isRanking = true) }
        
        return try {
            Log.d(TAG, "Applying deterministic ranking to ${jobs.size} jobs...")
            val rankedJobs = JobRankingService.rankJobs(jobs, profile)
            Log.d(TAG, "Deterministic ranking complete")
            rankedJobs
        } catch (e: Exception) {
            Log.e(TAG, "Ranking failed: ${e.message}", e)
            jobs
        } finally {
            _uiState.update { it.copy(isRanking = false) }
        }
    }

    fun toggleRanking() {
        _uiState.update { it.copy(isRankingEnabled = !it.isRankingEnabled) }
        loadCardsWithCurrentSearch()
    }

    fun reRankJobs() {
        viewModelScope.launch {
            val currentCards = _uiState.value.cards
            if (currentCards.isEmpty()) return@launch
            val rankedCards = applyRankingIfEnabled(currentCards)
            _uiState.update { it.copy(cards = rankedCards) }
        }
    }

    fun refreshCards() {
        _uiState.update { it.copy(cards = emptyList(), isLoading = true, error = null) }
        val searchQuery = getCurrentSearchQuery()
        
        viewModelScope.launch {
            try {
                repository.getJobCards(
                    search = searchQuery,
                    location = ApiConfig.DEFAULT_LOCATION,
                    remote = ApiConfig.DEFAULT_REMOTE_ONLY,
                    forceRefresh = true
                ).collect { result ->
                    result.fold(
                        onSuccess = { cards ->
                            val sortedCards = applyRankingIfEnabled(cards)
                            _uiState.update { it.copy(cards = sortedCards, isLoading = false, isEmpty = cards.isEmpty()) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onCardSwiped(card: JobCard, direction: SwipeDirection) {
        val searchQuery = getCurrentSearchQuery()
        
        viewModelScope.launch {
            repository.recordSwipe(card.id, direction.name)

            val (newStreak, newTodayCount) = streakManager.recordSwipe()

            val nextJob = repository.getNextJobFromQueue(
                search = searchQuery,
                location = ApiConfig.DEFAULT_LOCATION,
                remote = ApiConfig.DEFAULT_REMOTE_ONLY
            )

            _uiState.update { state ->
                val currentList = state.cards.toMutableList()
                currentList.removeIf { it.id == card.id }

                if (nextJob != null) {
                    currentList.add(nextJob)
                }

                val result = SwipeResult(card.id, direction, direction == SwipeDirection.RIGHT)
                val undoableAction = UndoableAction(card, result)

                state.copy(
                    cards = currentList,
                    isEmpty = currentList.isEmpty() && nextJob == null,
                    interestedCards = if (direction == SwipeDirection.RIGHT) state.interestedCards + card else state.interestedCards,
                    skippedCards = if (direction == SwipeDirection.LEFT) state.skippedCards + card else state.skippedCards,
                    undoHistory = state.undoHistory + undoableAction,
                    canUndo = true,
                    currentStreak = newStreak,
                    todaySwipeCount = newTodayCount
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
            it.copy(interestedCards = emptyList(), skippedCards = emptyList(), undoHistory = emptyList(), canUndo = false)
        }
        loadCardsWithCurrentSearch()
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
            repository.clearAllLocalData()
            _uiState.update { HomeUiState() }
            Log.d(TAG, "Sign out complete - local data cleared")
        }
    }

    fun debugClearHistory() {
        viewModelScope.launch {
            repository.clearSwipeHistory()
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
