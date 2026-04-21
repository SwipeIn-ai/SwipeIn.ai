package com.swipeapply.app.ui.screens

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swipeapply.app.data.model.ApplicationStatus
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.ui.components.ShimmerSavedJobCard
import com.swipeapply.app.ui.theme.AccentGreen
import com.swipeapply.app.ui.theme.AccentRed
import com.swipeapply.app.ui.viewmodel.SavedJobItem
import com.swipeapply.app.ui.viewmodel.SavedJobsViewModel
import com.swipeapply.app.ui.viewmodel.ViewModelFactory

private enum class SavedJobsFilter(val label: String) {
    ALL("All"),
    SAVED("Saved"),
    APPLIED("Applied"),
    INTERVIEW("Interview"),
    REJECTED("Rejected"),
    OFFER("Offer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedJobsScreen(
    onNavigateToEmployeeFinder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as Application
    val viewModel: SavedJobsViewModel = viewModel(factory = ViewModelFactory(application))
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeFilter by rememberSaveable { mutableStateOf(SavedJobsFilter.ALL) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val filteredJobs = remember(uiState.jobs, searchQuery, activeFilter) {
        uiState.jobs.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.card.company.name.contains(searchQuery, ignoreCase = true) ||
                item.card.title.contains(searchQuery, ignoreCase = true) ||
                item.card.location.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (activeFilter) {
                SavedJobsFilter.ALL -> true
                SavedJobsFilter.SAVED -> item.status == ApplicationStatus.SAVED
                SavedJobsFilter.APPLIED -> item.status == ApplicationStatus.APPLIED
                SavedJobsFilter.INTERVIEW -> item.status == ApplicationStatus.INTERVIEW
                SavedJobsFilter.REJECTED -> item.status == ApplicationStatus.REJECTED
                SavedJobsFilter.OFFER -> item.status == ApplicationStatus.OFFER
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Saved Jobs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Track what you liked and move each role forward.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search liked jobs") }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SavedJobsFilter.entries.forEach { filter ->
                val count = when (filter) {
                    SavedJobsFilter.ALL -> uiState.jobs.size
                    SavedJobsFilter.SAVED -> uiState.jobs.count { it.status == ApplicationStatus.SAVED }
                    SavedJobsFilter.APPLIED -> uiState.jobs.count { it.status == ApplicationStatus.APPLIED }
                    SavedJobsFilter.INTERVIEW -> uiState.jobs.count { it.status == ApplicationStatus.INTERVIEW }
                    SavedJobsFilter.REJECTED -> uiState.jobs.count { it.status == ApplicationStatus.REJECTED }
                    SavedJobsFilter.OFFER -> uiState.jobs.count { it.status == ApplicationStatus.OFFER }
                }
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text("${filter.label} ($count)") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isLoading -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        ShimmerSavedJobCard()
                    }
                }
            }

            uiState.error != null -> {
                SavedJobsEmptyState(
                    title = "Could not load saved jobs",
                    message = uiState.error ?: "Something went wrong.",
                    icon = Icons.Default.Refresh,
                    actionLabel = "Retry",
                    onAction = { viewModel.refresh() }
                )
            }

            uiState.jobs.isEmpty() -> {
                SavedJobsEmptyState(
                    title = "No liked jobs yet",
                    message = "Swipe right on roles you want to keep and they will land here.",
                    icon = Icons.Default.Bookmark
                )
            }

            filteredJobs.isEmpty() -> {
                SavedJobsEmptyState(
                    title = "No matches for this view",
                    message = "Try another status filter or a broader search.",
                    icon = Icons.Default.SearchOff,
                    actionLabel = "Clear filters",
                    onAction = {
                        searchQuery = ""
                        activeFilter = SavedJobsFilter.ALL
                    }
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredJobs, key = { it.card.id }) { item ->
                        SavedJobCard(
                            item = item,
                            onUpdateStatus = { status -> viewModel.updateStatus(item.card.id, status) },
                            onFindPeople = { onNavigateToEmployeeFinder(item.card.company.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedJobCard(
    item: SavedJobItem,
    onUpdateStatus: (ApplicationStatus) -> Unit,
    onFindPeople: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.card.company.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.card.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${item.card.location} • ${DateUtils.getRelativeTimeSpanString(item.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(status = item.status)
            }

            if (item.card.techStack.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.card.techStack.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApplicationStatus.entries.forEach { status ->
                    AssistChip(
                        onClick = { onUpdateStatus(status) },
                        label = { Text(status.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onFindPeople,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Group, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Find people")
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ApplicationStatus) {
    val background = when (status) {
        ApplicationStatus.SAVED -> MaterialTheme.colorScheme.secondaryContainer
        ApplicationStatus.APPLIED -> Color(0xFFE1F0FF)
        ApplicationStatus.INTERVIEW -> Color(0xFFFFF1C7)
        ApplicationStatus.REJECTED -> Color(0xFFFFE1E1)
        ApplicationStatus.OFFER -> Color(0xFFE1F7EA)
    }
    val content = when (status) {
        ApplicationStatus.SAVED -> MaterialTheme.colorScheme.onSecondaryContainer
        ApplicationStatus.APPLIED -> Color(0xFF0A66C2)
        ApplicationStatus.INTERVIEW -> Color(0xFF8A6100)
        ApplicationStatus.REJECTED -> AccentRed
        ApplicationStatus.OFFER -> AccentGreen
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
    ) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = content,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SavedJobsEmptyState(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onAction) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
