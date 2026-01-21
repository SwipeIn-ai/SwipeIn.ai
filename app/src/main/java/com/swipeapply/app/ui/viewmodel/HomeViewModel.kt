package com.swipeapply.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.data.config.ApiConfig
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.data.model.SwipeResult
import com.swipeapply.app.data.repository.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val totalJobs: Int = 0
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

    init {
        loadCards()
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
                            _uiState.update {
                                it.copy(
                                    cards = cards,
                                    isLoading = false,
                                    isEmpty = cards.isEmpty(),
                                    error = null,
                                    hasMorePages = true, // Reset assumption
                                    totalJobs = repository.getTotalCount() // Optional: might not be accurate with filter
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
            repository.clearSwipeHistory()
            _uiState.update { HomeUiState() } // Reset UI state
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