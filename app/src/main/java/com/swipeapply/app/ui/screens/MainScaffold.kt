package com.swipeapply.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swipeapply.app.data.model.JobCard
import com.swipeapply.app.data.repository.SavedContactsRepository

sealed class MainTab(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Jobs : MainTab(0, "Jobs", Icons.Filled.Work, Icons.Outlined.WorkOutline)
    object Contacts : MainTab(1, "Contacts", Icons.Filled.People, Icons.Outlined.People)

    companion object {
        val all = listOf(Jobs, Contacts)
    }
}

private val NavCyan   = Color(0xFF06B6D4)
private val NavPurple = Color(0xFF8B5CF6)

/**
 * Root scaffold that wraps Jobs + Contacts inside a LinkedIn / Instagram-style
 * bottom navigation bar. Pass-through callbacks are forwarded to HomeScreen.
 */
@Composable
fun MainScaffold(
    onCardClicked: (JobCard) -> Unit,
    onRequestIntro: (JobCard) -> Unit,
    onNavigateToEmployeeFinder: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onSignOutSuccess: () -> Unit,
    onDarkModeToggle: (Boolean?) -> Unit,
    isDarkModeEnabled: Boolean?,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val savedCount by SavedContactsRepository.companies.collectAsState()
    val totalSaved = savedCount.sumOf { it.contacts.size }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Tab content area ───
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it * direction } + fadeIn(tween(220))) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) { -it * direction } + fadeOut(tween(180)))
                    },
                    label = "mainTabContent"
                ) { tab ->
                    when (tab) {
                        0 -> HomeScreen(
                            onCardClicked = onCardClicked,
                            onRequestIntro = onRequestIntro,
                            onNavigateToEmployeeFinder = onNavigateToEmployeeFinder,
                            onDarkModeToggle = onDarkModeToggle,
                            isDarkModeEnabled = isDarkModeEnabled,
                            onNavigateToProfile = onNavigateToProfile,
                            onSignOutSuccess = onSignOutSuccess,
                            modifier = Modifier.fillMaxSize()
                        )
                        1 -> ContactsScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            // ─── Bottom Navigation Bar ───
            BottomNavBar(
                selectedIndex = selectedTabIndex,
                contactsBadgeCount = totalSaved,
                onTabSelected = { selectedTabIndex = it }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════
// Bottom Navigation Bar  (LinkedIn / Instagram style)
// ════════════════════════════════════════════════════════════

@Composable
private fun BottomNavBar(
    selectedIndex: Int,
    contactsBadgeCount: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 20.dp, clip = false)
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainTab.all.forEachIndexed { index, tab ->
                    val isSelected = selectedIndex == index
                    val badge = if (tab is MainTab.Contacts && contactsBadgeCount > 0)
                        contactsBadgeCount else -1

                    BottomNavItem(
                        tab = tab,
                        isSelected = isSelected,
                        badgeCount = badge,
                        onClick = { onTabSelected(index) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: MainTab,
    isSelected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "tabScale"
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.55f,
        animationSpec = tween(200),
        label = "labelAlpha"
    )

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            ) {
                // Icon container
                Box(contentAlignment = Alignment.TopEnd) {
                    // Pill background for selected state
                    this@Column.AnimatedVisibility(
                        visible = isSelected,
                        enter = scaleIn(spring(dampingRatio = 0.7f)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = 56.dp, height = 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            NavCyan.copy(alpha = 0.16f),
                                            NavPurple.copy(alpha = 0.16f)
                                        )
                                    )
                                )
                        )
                    }

                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                            .size(24.dp),
                        tint = if (isSelected) NavCyan else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Badge dot
                    if (badgeCount > 0) {
                        this@Column.AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(spring(dampingRatio = 0.6f)) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(if (badgeCount >= 10) 18.dp else 16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NavCyan, NavPurple))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Label
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) NavCyan
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = labelAlpha),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
        }
    }
}
