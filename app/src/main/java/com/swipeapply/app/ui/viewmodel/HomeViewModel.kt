package com.swipeapply.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.model.SwipeDirection
import com.swipeapply.app.data.model.SwipeResult
import com.swipeapply.app.data.repository.MockJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val isEmpty: Boolean = false,
    val undoHistory: List<UndoableAction> = emptyList(),
    val canUndo: Boolean = false
)

/**
 * ViewModel for the Home/Swipe screen.
 * Manages card stack state and swipe actions.
 */
class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _swipeHistory = MutableStateFlow<List<SwipeResult>>(emptyList())
    val swipeHistory: StateFlow<List<SwipeResult>> = _swipeHistory.asStateFlow()

    init {
        loadCards()
    }

    /**
     * Load initial cards from repository
     */
    private fun loadCards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Simulate network delay for realism
            kotlinx.coroutines.delay(300)

            val cards = MockJobRepository.getJobCards()
            _uiState.update {
                it.copy(
                    cards = cards,
                    isLoading = false,
                    isEmpty = cards.isEmpty()
                )
            }
        }
    }

    /**
     * Handle a card being swiped
     */
    fun onCardSwiped(card: JobCard, direction: SwipeDirection) {
        viewModelScope.launch {
            val result = SwipeResult(
                cardId = card.id,
                direction = direction,
                isInterested = direction == SwipeDirection.RIGHT
            )

            // Create undoable action
            val undoableAction = UndoableAction(
                card = card,
                result = result
            )

            // Update state based on direction
            _uiState.update { state ->
                val newCards = state.cards.filter { it.id != card.id }
                val newInterested = if (direction == SwipeDirection.RIGHT) {
                    state.interestedCards + card
                } else state.interestedCards
                val newSkipped = if (direction == SwipeDirection.LEFT) {
                    state.skippedCards + card
                } else state.skippedCards

                // Add to undo history
                val newUndoHistory = state.undoHistory + undoableAction

                state.copy(
                    cards = newCards,
                    interestedCards = newInterested,
                    skippedCards = newSkipped,
                    isEmpty = newCards.isEmpty(),
                    undoHistory = newUndoHistory,
                    canUndo = newUndoHistory.isNotEmpty()
                )
            }
        }
    }

    /**
     * Select a card to show details in bottom sheet
     */
    fun selectCard(card: JobCard) {
        _uiState.update {
            it.copy(
                selectedCard = card,
                showBottomSheet = true
            )
        }
    }

    /**
     * Dismiss the bottom sheet
     */
    fun dismissBottomSheet() {
        _uiState.update {
            it.copy(
                showBottomSheet = false,
                selectedCard = null
            )
        }
    }

    /**
     * Undo the last swipe
     */
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

    /**
     * Undo multiple swipes
     */
    fun undoSwipes(count: Int) {
        viewModelScope.launch {
            val undoHistory = _uiState.value.undoHistory
            val actualCount = minOf(count, undoHistory.size)
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

    /**
     * Clear undo history
     */
    fun clearUndoHistory() {
        _uiState.update {
            it.copy(
                undoHistory = emptyList(),
                canUndo = false
            )
        }
    }

    /**
     * Get undo history count
     */
    fun getUndoCount(): Int = _uiState.value.undoHistory.size

    /**
     * Reset all cards
     */
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

    /**
     * Get stats for display
     */
    fun getStats(): SwipeStats {
        val state = _uiState.value
        return SwipeStats(
            interested = state.interestedCards.size,
            skipped = state.skippedCards.size,
            remaining = state.cards.size,
            undoCount = state.undoHistory.size
        )
    }
}

data class SwipeStats(
    val interested: Int,
    val skipped: Int,
    val remaining: Int,
    val undoCount: Int
)