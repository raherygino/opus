package com.gsoft.opus.ui.components.drawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gsoft.opus.R
import com.gsoft.opus.core.Constants
import com.gsoft.opus.ui.components.ContextMenuItem
import com.gsoft.opus.ui.components.OpusDialog

private val ItemHeight = 56.dp
private val ItemCorner = 16.dp
private val IconSize = 24.dp
private const val StaggerStep = 0.03f
private const val StaggerWindow = 0.45f
/** Horizontal slide (in dp) applied to staggered entries before they settle. */
private val StaggerSlideDp = 24.dp

/**
 * Fixed left pane of the navigation drawer.
 *
 * Renders a centered application logo header, the navigation entries (with
 * section headers and expandable groups), and an outlined user profile card
 * at the bottom with an overflow menu for Profil and Déconnexion.
 * Items stagger-animate in as the drawer opens, driven by [progress].
 *
 * @param items       navigation entries (reuses the [ContextMenuItem] model).
 * @param selectedId  id of the currently selected entry, or null.
 * @param username    login username (used as fallback for the avatar letter).
 * @param firstName   user's first name (used for the avatar letter fallback).
 * @param lastName    user's last name (displayed as the primary text).
 * @param personnelId id of the Personnel record; used to build the authenticated
 *                    photo URL (`/api/personnel/{id}/photo`), mirroring desktop.
 * @param photo       profile picture filename from the Personnel record, used as a
 *                    cache-busting query param; or null when no photo is set.
 * @param role        user's role or grade (displayed as secondary text).
 * @param progress    live drawer open fraction for stagger animations.
 * @param onItemClick invoked for leaf items.
 * @param onProfileClick invoked when the Profil overflow action is tapped.
 * @param onLogout    invoked when the Déconnexion action is confirmed.
 * @param appVersion  version label displayed at the bottom.
 */
@Composable
fun OpusDrawerContent(
    items: List<ContextMenuItem>,
    selectedId: String?,
    username: String,
    firstName: String?,
    lastName: String?,
    personnelId: Int?,
    photo: String?,
    role: String?,
    drawerState: DrawerState,
    progress: () -> Float,
    onItemClick: (ContextMenuItem) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Precompute the stagger slide distance in pixels once, instead of
    // calling 24.dp.toPx() inside every StaggeredEntry's per-frame
    // graphicsLayer lambda (one toPx() per item per frame otherwise).
    val staggerSlidePx = with(LocalDensity.current) { StaggerSlideDp.toPx() }

    // Ensure the drawer list always starts at the top when opened. Without
    // this the LazyColumn can settle at the bottom on first open.
    val listState = rememberLazyListState()
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) {
            listState.scrollToItem(0)
        }
    }

    // Filter items by the search query. Supports division keywords
    // (sedentaire, service général / sg, police judiciaire / pj) which
    // expand to show every entry under the matching section.
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items else filterDrawerItems(items, searchQuery.trim())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 0.dp)
    ) {
        DrawerLogoHeader()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF4CAF50), Color.White, Color(0xFFF44336))
                    )
                )
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        DrawerSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucun menu trouvé",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->
                    StaggeredEntry(
                        index = index,
                        progress = progress,
                        slidePx = staggerSlidePx
                    ) {
                        when {
                            item.isSectionHeader -> DrawerSectionHeader(title = item.title)
                            item.children != null -> DrawerExpandableItem(
                                item = item,
                                selectedId = selectedId,
                                onItemClick = onItemClick
                            )
                            else -> DrawerItem(
                                item = item,
                                selected = item.id == selectedId,
                                onClick = { onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        DrawerProfileCard(
            username = username,
            firstName = firstName,
            lastName = lastName,
            personnelId = personnelId,
            photo = photo,
            role = role,
            onProfileClick = onProfileClick,
            onLogoutClick = { showLogoutDialog = true }
        )

        Text(
            text = "Version $appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
        )
    }

    OpusDialog(
        visible = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        title = stringResource(R.string.logout_title),
        message = stringResource(R.string.logout_message),
        confirmText = stringResource(R.string.logout_confirm),
        onConfirm = {
            showLogoutDialog = false
            onLogout()
        },
        cancelText = stringResource(R.string.logout_cancel),
        onCancel = { showLogoutDialog = false }
    )
}

/** Applies an index-based stagger (fade + slide) driven by drawer progress. */
@Composable
private fun StaggeredEntry(
    index: Int,
    progress: () -> Float,
    slidePx: Float,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val delay = (index * StaggerStep).coerceAtMost(StaggerWindow)
            val p = ((progress() - delay) / (1f - delay)).coerceIn(0f, 1f)
            alpha = p
            translationX = -slidePx * (1f - p)
        }
    ) {
        content()
    }
}

/**
 * Centered application branding header: large OPUS logo, the application
 * name and its full French description. Mirrors the desktop sidebar header
 * layout (centered logo + name) and extends it with the tagline.
 */
@Composable
private fun DrawerLogoHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_opus),
            contentDescription = "OPUS",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(132.dp)
                .aspectRatio(1f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "OPUS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "Opérations Policières Unifiées et Structurées",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Search bar for filtering drawer entries.
 *
 * Compact, rounded field with a leading search icon and a trailing clear
 * button that only appears when there is text to clear.
 */
@Composable
private fun DrawerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Rechercher") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Effacer")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawer search filtering
//
// Supports free-text matching against an item's title/subtitle as well as
// division keywords that expand to every entry under the matching section:
//   "sedentaire"           → all Sédentaire entries
//   "service général" / "sg" → all Service Général entries
//   "police judiciaire" / "pj" → all Police Judiciaire entries
// Section keywords are matched case-insensitively and on partial input.
// ─────────────────────────────────────────────────────────────────────────────

/** Maps a section header id to the division keywords that select it. */
private val SECTION_KEYWORDS: Map<String, List<String>> = mapOf(
    "section_sedentaire" to listOf("sedentaire", "sédentaire"),
    "section_sg" to listOf("service general", "service général", "sg"),
    "section_pj" to listOf("police judiciaire", "pj"),
)

/**
 * Returns true when [query] matches a division keyword for [sectionId].
 */
private fun matchesSectionKeyword(sectionId: String, query: String): Boolean {
    val keywords = SECTION_KEYWORDS[sectionId] ?: return false
    val normalized = query.lowercase().trim()
    return keywords.any { kw -> normalized.contains(kw) || kw.contains(normalized) }
}

/**
 * Returns true when [query] appears in the item's title or subtitle
 * (case-insensitive).
 */
private fun matchesText(item: ContextMenuItem, query: String): Boolean {
    val q = query.lowercase()
    return item.title.lowercase().contains(q) ||
        (item.subtitle?.lowercase()?.contains(q) == true)
}

/**
 * Filters the flat drawer item list by [query].
 *
 * Section headers are kept when their section is selected by a division
 * keyword or when at least one of the entries that follow them (up to the
 * next section header) matches the text query. Non-matching entries are
 * dropped. Expandable groups whose children match are kept with only the
 * matching children retained.
 */
private fun filterDrawerItems(items: List<ContextMenuItem>, query: String): List<ContextMenuItem> {
    val result = mutableListOf<ContextMenuItem>()
    var currentSectionId: String? = null
    var currentSectionKept = false
    var sectionHasMatch = false

    // First pass: resolve which sections are selected by a division keyword.
    val keywordSections = mutableSetOf<String>()
    for (item in items) {
        if (item.isSectionHeader && matchesSectionKeyword(item.id, query)) {
            keywordSections.add(item.id)
        }
    }

    for (item in items) {
        if (item.isSectionHeader) {
            // Flush previous section if it had no matches and isn't keyword-selected.
            if (currentSectionId != null && !currentSectionKept) {
                // nothing to flush — we only add when kept
            }
            currentSectionId = item.id
            currentSectionKept = item.id in keywordSections
            sectionHasMatch = currentSectionKept
            if (currentSectionKept) {
                result.add(item)
            }
            continue
        }

        val sectionSelected = currentSectionId != null && currentSectionId in keywordSections
        val textMatch = matchesText(item, query)

        if (sectionSelected || textMatch) {
            // Ensure the section header is emitted before the first kept entry.
            if (!currentSectionKept && currentSectionId != null) {
                val header = items.first { it.id == currentSectionId }
                result.add(header)
                currentSectionKept = true
            }
            sectionHasMatch = true

            if (item.children != null) {
                // Keep the group with only matching children (or all children
                // when the section is keyword-selected).
                val keptChildren = if (sectionSelected) {
                    item.children
                } else {
                    item.children.filter { matchesText(it, query) }
                }
                if (keptChildren.isNotEmpty()) {
                    result.add(item.copy(children = keptChildren))
                }
            } else {
                result.add(item)
            }
        }
    }

    return result
}

/**
 * Outlined user profile card pinned at the bottom of the drawer.
 *
 * Shows the user's avatar, display name and role inside a border-only card
 * (no shadow). A three-dot overflow icon on the right opens a Material
 * dropdown menu with Profil and Déconnexion actions, styled like a
 * Chrome-style context menu (rounded corners, leading icons, clear labels).
 */
@Composable
private fun DrawerProfileCard(
    username: String,
    firstName: String?,
    lastName: String?,
    personnelId: Int?,
    photo: String?,
    role: String?,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val displayName = listOfNotNull(
        firstName?.takeIf { it.isNotBlank() },
        lastName?.takeIf { it.isNotBlank() }
    ).joinToString(" ").ifBlank { username }

    val avatarLetter = firstName?.firstOrNull()?.uppercaseChar()
        ?: username.firstOrNull()?.uppercaseChar()
        ?: '?'

    // Build the authenticated photo URL the same way the desktop app does:
    //   /api/personnel/{id}/photo?v={filename}
    // The `?v=` cache-buster mirrors desktop so a freshly uploaded photo is
    // fetched instead of being served from Coil's disk cache. We only build
    // the URL when both a personnel id and a photo filename are present.
    val fullPhotoUrl = personnelId?.let { id ->
        photo?.takeIf { it.isNotBlank() }?.let { filename ->
            "${Constants.BASE_URL}${Constants.API_PREFIX}/personnel/$id/photo?v=$filename"
        }
    }

    // Track load failures so we can fall back to the letter avatar instead of
    // leaving an empty circle when the request errors (e.g. offline / 404).
    var photoLoadFailed by remember(fullPhotoUrl) { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (fullPhotoUrl != null && !photoLoadFailed) {
                AsyncImage(
                    model = fullPhotoUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { photoLoadFailed = true }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatarLetter.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!role.isNullOrBlank()) {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Plus d'options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.wrapContentSize()
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Profil",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onProfileClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Déconnexion",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onLogoutClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
    )
}

/**
 * A single drawer row with selection indicator, icon scale and color
 * micro-animations following the reference design.
 */
@Composable
private fun DrawerItem(
    item: ContextMenuItem,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (selected) 0.16f else 0f,
        animationSpec = tween(250),
        label = "drawer_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "drawer_item_color"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer_item_icon_scale"
    )
    val indicatorHeight by animateFloatAsState(
        targetValue = if (selected) 24f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drawer_item_indicator"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .clip(RoundedCornerShape(ItemCorner))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                role = Role.Button,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated left indicator bar.
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(indicatorHeight.dp)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(12.dp))

        if (item.icon != null) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(IconSize)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        trailing?.invoke()

        Spacer(modifier = Modifier.width(12.dp))
    }
}

/** Expandable group: parent row toggles an animated list of child items. */
@Composable
private fun DrawerExpandableItem(
    item: ContextMenuItem,
    selectedId: String?,
    onItemClick: (ContextMenuItem) -> Unit
) {
    val children = item.children.orEmpty()
    val hasSelectedChild = remember(selectedId, children) {
        children.any { it.id == selectedId }
    }
    var expanded by rememberSaveable(item.id) { mutableStateOf(hasSelectedChild) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "drawer_chevron"
    )

    Column {
        DrawerItem(
            item = item,
            selected = hasSelectedChild && !expanded,
            onClick = { expanded = !expanded },
            trailing = {
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(250)),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(150))
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                children.forEach { child ->
                    DrawerItem(
                        item = child,
                        selected = child.id == selectedId,
                        onClick = { onItemClick(child) }
                    )
                }
            }
        }
    }
}
