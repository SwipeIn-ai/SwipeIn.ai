package com.swipeapply.app.data.repository

import android.content.Context
import android.util.Log
import com.swipeapply.app.SupabaseClient
import com.swipeapply.app.data.local.SwipeApplyDatabase
import com.swipeapply.app.data.local.entity.SavedContactEntity
import com.swipeapply.app.data.model.Employee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A grouped entry of saved contacts for one company.
 */
data class SavedCompanyContacts(
    val companyName: String,
    val contacts: List<Employee>,
    val savedAtMs: Long = System.currentTimeMillis()
)

/**
 * Repository that persists right-swiped employee contacts to Room DB.
 * Exposes a StateFlow so every observer is instantly updated.
 * Data survives app restarts.
 */
object SavedContactsRepository {

    private const val TAG = "SavedContactsRepo"

    private val _companies = MutableStateFlow<List<SavedCompanyContacts>>(emptyList())
    val companies: StateFlow<List<SavedCompanyContacts>> = _companies.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dao: com.swipeapply.app.data.local.dao.SavedContactDao? = null
    private var initialized = false

    val totalSavedCount: Int
        get() = _companies.value.sumOf { it.contacts.size }

    /**
     * Initialize with application context. Call once from MainActivity or Application.
     */
    fun initialize(context: Context) {
        if (initialized) return
        dao = SwipeApplyDatabase.getDatabase(context).savedContactDao()
        initialized = true
        loadFromDb()
    }

    private fun getCurrentUserId(): String {
        return SupabaseClient.getCurrentUserId() ?: "local_user"
    }

    private fun loadFromDb() {
        val contactDao = dao ?: return
        scope.launch {
            try {
                contactDao.getContactsByUser(getCurrentUserId()).collect { entities ->
                    val grouped = entities
                        .groupBy { it.companyName }
                        .map { (company, contacts) ->
                            SavedCompanyContacts(
                                companyName = company,
                                contacts = contacts.map { it.toEmployee() },
                                savedAtMs = contacts.maxOfOrNull { it.savedAtMs } ?: System.currentTimeMillis()
                            )
                        }
                        .sortedByDescending { it.savedAtMs }
                    _companies.value = grouped
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load contacts from DB", e)
            }
        }
    }

    /**
     * Persist a batch of contacts for one company after the user finishes the finder screen.
     * If the company already exists its list is replaced.
     */
    fun saveContacts(companyName: String, contacts: List<Employee>) {
        if (contacts.isEmpty()) return
        
        // Update in-memory immediately for instant UI response
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

        // Persist to Room DB in background
        val contactDao = dao ?: return
        val userId = getCurrentUserId()
        scope.launch {
            try {
                contactDao.deleteByCompany(userId, companyName)
                contactDao.insertContacts(contacts.map { it.toEntity(userId, companyName) })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist contacts for $companyName", e)
            }
        }
    }

    /** Remove all contacts for a company. */
    fun removeCompany(companyName: String) {
        _companies.update { it.filter { c -> !c.companyName.equals(companyName, ignoreCase = true) } }
        
        val contactDao = dao ?: return
        val userId = getCurrentUserId()
        scope.launch {
            try {
                contactDao.deleteByCompany(userId, companyName)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove contacts for $companyName", e)
            }
        }
    }

    /** Clear everything. */
    fun clearAll() {
        _companies.value = emptyList()
        
        val contactDao = dao ?: return
        val userId = getCurrentUserId()
        scope.launch {
            try {
                contactDao.deleteAllForUser(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all contacts", e)
            }
        }
    }

    // ─── Mapping helpers ───

    private fun Employee.toEntity(userId: String, companyName: String) = SavedContactEntity(
        userId = userId,
        companyName = companyName,
        fullName = fullName,
        email = email,
        emailStatus = emailStatus,
        jobTitle = jobTitle,
        seniority = seniority,
        department = department,
        linkedinUrl = linkedinUrl,
        city = city,
        state = state,
        country = country,
        confidence = confidence
    )

    private fun SavedContactEntity.toEmployee() = Employee(
        fullName = fullName,
        email = email,
        emailStatus = emailStatus,
        jobTitle = jobTitle,
        seniority = seniority,
        department = department,
        linkedinUrl = linkedinUrl,
        city = city,
        state = state,
        country = country,
        confidence = confidence
    )
}
