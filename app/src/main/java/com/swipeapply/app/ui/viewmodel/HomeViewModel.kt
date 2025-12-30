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
 * UI State for the Home/Swipe screen
 */
data class HomeUiState(
    val cards: List<JobCard> = emptyList(),
    val interestedCards: List<JobCard> = emptyList(),
    val skippedCards: List<JobCard> = emptyList(),
    val selectedCard: JobCard? = null,
    val showBottomSheet: Boolean = false,
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false
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
            
            // Add to history
            _swipeHistory.update { it + result }
            
            // Update state based on direction
            _uiState.update { state ->
                val newCards = state.cards.filter { it.id != card.id }
                val newInterested = if (direction == SwipeDirection.RIGHT) {
                    state.interestedCards + card
                } else state.interestedCards
                val newSkipped = if (direction == SwipeDirection.LEFT) {
                    state.skippedCards + card
                } else state.skippedCards
                
                state.copy(
                    cards = newCards,
                    interestedCards = newInterested,
                    skippedCards = newSkipped,
                    isEmpty = newCards.isEmpty()
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
            val lastResult = _swipeHistory.value.lastOrNull() ?: return@launch
            
            val card = when {
                lastResult.isInterested -> 
                    _uiState.value.interestedCards.find { it.id == lastResult.cardId }
                else -> 
                    _uiState.value.skippedCards.find { it.id == lastResult.cardId }
            } ?: return@launch
            
            _swipeHistory.update { it.dropLast(1) }
            
            _uiState.update { state ->
                state.copy(
                    cards = listOf(card) + state.cards,
                    interestedCards = state.interestedCards.filter { it.id != card.id },
                    skippedCards = state.skippedCards.filter { it.id != card.id },
                    isEmpty = false
                )
            }
        }
    }
    
    /**
     * Reset all cards
     */
    fun resetCards() {
        _swipeHistory.update { emptyList() }
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
            remaining = state.cards.size
        )
    }
}

data class SwipeStats(
    val interested: Int,
    val skipped: Int,
    val remaining: Int
)
