package com.swipeapply.app.data.repository

import com.swipeapply.app.data.model.Employee
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A grouped entry of saved contacts for one company.
 */
data class SavedCompanyContacts(
    val companyName: String,
    val contacts: List<Employee>,
    val savedAtMs: Long = System.currentTimeMillis()
)

/**
 * Singleton repository that holds all right-swiped employee contacts in memory,
 * grouped by company. Exposes a StateFlow so every observer is instantly updated.
 */
object SavedContactsRepository {

    private val _companies = MutableStateFlow<List<SavedCompanyContacts>>(emptyList())
    val companies: StateFlow<List<SavedCompanyContacts>> = _companies.asStateFlow()

    val totalSavedCount: Int
        get() = _companies.value.sumOf { it.contacts.size }

    /**
     * Persist a batch of contacts for one company after the user finishes the finder screen.
     * If the company already exists its list is replaced.
     */
    fun saveContacts(companyName: String, contacts: List<Employee>) {
        if (contacts.isEmpty()) return
        _companies.update { current ->
            val existing = current.indexOfFirst {
                it.companyName.equals(companyName, ignoreCase = true)
            }
            if (existing >= 0) {
                current.toMutableList().also {
                    it[existing] = SavedCompanyContacts(companyName, contacts)
                }
            } else {
                listOf(SavedCompanyContacts(companyName, contacts)) + current
            }
        }
    }

    /** Remove all contacts for a company. */
    fun removeCompany(companyName: String) {
        _companies.update { it.filter { c -> !c.companyName.equals(companyName, ignoreCase = true) } }
    }

    /** Clear everything. */
    fun clearAll() {
        _companies.value = emptyList()
    }
}
