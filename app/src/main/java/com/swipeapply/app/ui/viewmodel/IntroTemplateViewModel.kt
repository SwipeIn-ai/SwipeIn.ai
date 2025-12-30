package com.swipeapply.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * UI State for intro template screen
 */
data class IntroTemplateUiState(
    val subject: String = "",
    val body: String = "",
    val isCopied: Boolean = false,
    val recipientName: String = "[Hiring Manager]",
    val senderName: String = "[Your Name]"
)

/**
 * ViewModel for the Intro Template screen.
 * Manages email template editing and copying.
 */
class IntroTemplateViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(IntroTemplateUiState())
    val uiState: StateFlow<IntroTemplateUiState> = _uiState.asStateFlow()
    
    /**
     * Initialize with template from a job card
     */
    fun initializeTemplate(subject: String, body: String) {
        _uiState.update {
            it.copy(
                subject = subject,
                body = body,
                isCopied = false
            )
        }
    }
    
    /**
     * Update the subject line
     */
    fun updateSubject(subject: String) {
        _uiState.update { it.copy(subject = subject, isCopied = false) }
    }
    
    /**
     * Update the body text
     */
    fun updateBody(body: String) {
        _uiState.update { it.copy(body = body, isCopied = false) }
    }
    
    /**
     * Update recipient name placeholder
     */
    fun updateRecipient(name: String) {
        val newBody = _uiState.value.body.replace(
            _uiState.value.recipientName,
            name
        )
        _uiState.update { 
            it.copy(
                body = newBody,
                recipientName = name,
                isCopied = false
            )
        }
    }
    
    /**
     * Update sender name placeholder
     */
    fun updateSender(name: String) {
        val newBody = _uiState.value.body.replace(
            _uiState.value.senderName,
            name
        )
        _uiState.update { 
            it.copy(
                body = newBody,
                senderName = name,
                isCopied = false
            )
        }
    }
    
    /**
     * Mark the email as copied
     */
    fun markAsCopied() {
        _uiState.update { it.copy(isCopied = true) }
    }
    
    /**
     * Get the full email text for copying
     */
    fun getFullEmail(): String {
        val state = _uiState.value
        return "Subject: ${state.subject}\n\n${state.body}"
    }
    
    /**
     * Reset the copied state
     */
    fun resetCopiedState() {
        _uiState.update { it.copy(isCopied = false) }
    }
}
