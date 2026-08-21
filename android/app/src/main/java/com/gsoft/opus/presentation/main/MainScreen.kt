package com.gsoft.opus.presentation.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gsoft.opus.R
import com.gsoft.opus.core.PermissionAction
import com.gsoft.opus.core.hasPermission
import com.gsoft.opus.domain.model.User
import com.gsoft.opus.navigation.BottomNavItem
import com.gsoft.opus.navigation.MainRoutes
import com.gsoft.opus.presentation.contextmenu.ContextMenuItemScreens
import com.gsoft.opus.presentation.dashboard.DashboardScreen
import com.gsoft.opus.presentation.home.HomeViewModel
import com.gsoft.opus.presentation.notifications.NotificationsScreen
import com.gsoft.opus.presentation.notifications.UnreadBadgeViewModel
import com.gsoft.opus.presentation.profile.ProfileScreen
import com.gsoft.opus.presentation.settings.SettingsScreen
import com.gsoft.opus.presentation.signature.SignaturePairingScreen
import com.gsoft.opus.presentation.signature.SignaturePadScreen
import com.gsoft.opus.presentation.photo.PhotoCaptureScreen
import com.gsoft.opus.presentation.personnel.PersonnelScreen
import com.gsoft.opus.presentation.personnel.PersonnelDetailScreen
import com.gsoft.opus.presentation.personnel.PersonnelFormScreen
import com.gsoft.opus.presentation.personnel.MouvementFormScreen
import com.gsoft.opus.presentation.personnel.ComportementFormScreen
import com.gsoft.opus.presentation.personnel.PersonnelBrowseScreen
import com.gsoft.opus.presentation.personnel.PersonnelBrowseDetailScreen
import com.gsoft.opus.presentation.qrauth.QrAuthScannerScreen
import com.gsoft.opus.data.signature.QrPayload
import com.gsoft.opus.ui.components.AppBottomNavigation
import com.gsoft.opus.ui.components.ContextMenuItem
import com.gsoft.opus.ui.components.MainScaffold
import com.gsoft.opus.ui.components.drawer.OpusDrawerContent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Main shell of the application displayed after authentication.
 *
 * Hosts the bottom navigation bar, an inner [NavHost] with the
 * Dashboard / Notifications / Settings / Profile destinations, and a
 * standard Material 3 [ModalNavigationDrawer] that slides in from the
 * left edge with the default Compose drawer animation and gesture
 * support (edge swipe to open, scrim tap / system back to close).
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    openNotificationsRequests: StateFlow<Boolean>? = null,
    onOpenNotificationsConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // When a push notification is tapped, select the Notifications tab.
    // The request is sticky, so this also works right after a cold start.
    val openNotificationsRequested by
        (openNotificationsRequests ?: remember { kotlinx.coroutines.flow.MutableStateFlow(false) })
            .collectAsState()
    LaunchedEffect(openNotificationsRequested) {
        if (openNotificationsRequested) {
            navController.navigateToTab(MainRoutes.Notifications.route)
            onOpenNotificationsConsumed()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.state.collectAsState()

    // The badge is driven by the app-wide UnreadCountStore (via a thin
    // ViewModel) so it stays in sync even when the notifications screen has
    // never been opened. The NotificationsViewModel still syncs the store
    // whenever the user opens/reads notifications.
    val unreadBadgeViewModel: UnreadBadgeViewModel = hiltViewModel()
    val unreadCount by unreadBadgeViewModel.unreadCount.collectAsState()

    // Refresh the unread count from the server whenever the app comes back to
    // the foreground, so the badge is always current when the user returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                unreadBadgeViewModel.refreshFromServer()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    val drawerItems = remember(homeState.user) {
        buildDrawerItems(homeState.user)
    }

    val drawerRouteMap = remember {
        mapOf(
            "sed_dashboard" to MainRoutes.SedDashboard.route,
            "sed_correspondance" to MainRoutes.Correspondance.route,
            "sed_gestion_personnel" to MainRoutes.GestionPersonnel.route,
            "sed_declaration_perte" to MainRoutes.DeclarationPerte.route,
            "sed_rapport" to MainRoutes.Rapport.route,
            "sed_main_courante_sec" to MainRoutes.MainCouranteSec.route,
            "sed_passation" to MainRoutes.Passation.route,
            "sed_armement" to MainRoutes.Armement.route,
            "sed_materiels" to MainRoutes.Materiels.route,
            "sed_situation_gav" to MainRoutes.SituationGav.route,
            "sed_main_courante_poste" to MainRoutes.MainCourantePoste.route,
            "sed_renseignement" to MainRoutes.RenseignementSed.route,
            "sg_dashboard" to MainRoutes.SgDashboard.route,
            "sg_spa" to MainRoutes.Spa.route,
            "sg_info_rassemblement" to MainRoutes.InfoRassemblement.route,
            "sg_repartition" to MainRoutes.Repartition.route,
            "sg_patrouille" to MainRoutes.Patrouille.route,
            "sg_intervention" to MainRoutes.Intervention.route,
            "sg_dispositif_exceptionnel" to MainRoutes.DispositifExceptionnel.route,
            "sg_instruction_autorite" to MainRoutes.InstructionAutorite.route,
            "sg_compte_rendu" to MainRoutes.CompteRendu.route,
            "sg_recherche" to MainRoutes.RechercheSg.route,
            "sg_renseignement" to MainRoutes.RenseignementSg.route,
            "pj_dashboard" to MainRoutes.PjDashboard.route,
            "pj_plainte" to MainRoutes.Plainte.route,
            "pj_registre_enquete" to MainRoutes.RegistreEnquete.route,
            "pj_mandat" to MainRoutes.Mandat.route,
            "pj_convocation" to MainRoutes.Convocation.route,
            "pj_arrestation" to MainRoutes.Arrestation.route,
            "pj_gav" to MainRoutes.Gav.route,
            "pj_requisition" to MainRoutes.Requisition.route,
            "pj_personne_recherchee" to MainRoutes.PersonneRecherchee.route,
            "pj_objets" to MainRoutes.Objets.route,
            "pj_registre_deferrement" to MainRoutes.RegistreDeferrement.route,
            "pj_renseignement" to MainRoutes.RenseignementPj.route,
            "cartographie" to MainRoutes.Cartographie.route,
            "utilisateurs" to MainRoutes.Utilisateurs.route,
            "roles" to MainRoutes.Roles.route,
            "signature_pairing" to MainRoutes.SignaturePairing.route,
            "photo_pairing" to MainRoutes.PhotoPairing.route,
            "qr_auth_scanner" to MainRoutes.QrAuthScanner.route
        )
    }

    val routeToDrawerId = remember(drawerRouteMap) {
        drawerRouteMap.entries.associate { (id, route) -> route to id }
    }
    val selectedDrawerId = routeToDrawerId[currentRoute]

    val bottomNavRoutes = remember {
        BottomNavItem.items.map { it.route }.toSet()
    }

    // "Main" stays active when on Dashboard or any drawer route (all content is under Main)
    val drawerRouteSet = remember(drawerRouteMap) { drawerRouteMap.values.toSet() }
    val bottomNavSelectedRoute = when {
        currentRoute in bottomNavRoutes -> currentRoute
        currentRoute in drawerRouteSet -> MainRoutes.Dashboard.route
        else -> null
    }

    // Routes that should show the header (main screens, not detail/form/pairing)
    val routesWithoutHeader = remember {
        setOf(
            MainRoutes.SignaturePairing.route,
            MainRoutes.PhotoPairing.route,
            MainRoutes.PersonnelBrowseDetail.route,
            MainRoutes.QrAuthScanner.route
        )
    }
    val showHeader = (currentRoute in bottomNavRoutes || currentRoute in drawerRouteSet ||
            currentRoute == MainRoutes.Dashboard.route) && currentRoute !in routesWithoutHeader

    // Map each route to a human-readable title for the top app bar subtitle.
    val routeTitleMap = remember(drawerItems, drawerRouteMap) {
        val map = mutableMapOf<String, String>()
        // Drawer routes (including nested children)
        drawerItems.forEach { item ->
            drawerRouteMap[item.id]?.let { route -> map[route] = item.title }
            item.children?.forEach { child ->
                drawerRouteMap[child.id]?.let { route -> map[route] = child.title }
            }
        }
        // Bottom nav + settings routes
        map[MainRoutes.Dashboard.route] = context.getString(R.string.nav_dashboard)
        map[MainRoutes.Notifications.route] = context.getString(R.string.nav_notifications)
        map[MainRoutes.PersonnelList.route] = context.getString(R.string.nav_personnels)
        map[MainRoutes.Profile.route] = context.getString(R.string.nav_profile)
        map[MainRoutes.Settings.route] = context.getString(R.string.settings_title)
        map[MainRoutes.QrAuthScanner.route] = context.getString(R.string.qr_connect_computer)
        map
    }
    val appBarSubtitle = currentRoute?.let { routeTitleMap[it] }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                windowInsets = WindowInsets(0, 0, 0, 0)
            ) {
                OpusDrawerContent(
                    items = drawerItems,
                    selectedId = selectedDrawerId,
                    username = homeState.username,
                    firstName = homeState.firstName,
                    lastName = homeState.lastName,
                    personnelId = homeState.personnelId,
                    photo = homeState.photo,
                    role = homeState.roleName ?: homeState.grade,
                    progress = { 1f },
                    onItemClick = { item ->
                        val route = drawerRouteMap[item.id]
                        scope.launch {
                            drawerState.close()
                            route?.let { navController.navigateToDrawerItem(it) }
                        }
                    },
                    onProfileClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigateToTab(MainRoutes.Profile.route)
                        }
                    },
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                            homeViewModel.logout()
                            onLogout()
                        }
                    },
                    appVersion = appVersion
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AppBottomNavigation(
                    items = BottomNavItem.items,
                    selectedRoute = bottomNavSelectedRoute,
                    onItemSelected = { item ->
                        navController.navigateToTab(item.route)
                    },
                    notificationBadgeCount = unreadCount,
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { paddingValues ->
            MainScaffold(
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                showHeader = showHeader,
                subtitle = appBarSubtitle
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainRoutes.Dashboard.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding()),
                    enterTransition = {
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { it / 20 },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        )
                    },
                    exitTransition = {
                        fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it / 20 },
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
                    },
                    popEnterTransition = {
                        fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            initialOffsetX = { -it / 20 },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        )
                    },
                    popExitTransition = {
                        fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(
                            targetOffsetX = { it / 20 },
                            animationSpec = tween(180, easing = FastOutSlowInEasing)
                        )
                    }
                ) {
                    composable(MainRoutes.Dashboard.route) {
                        DashboardScreen(onLogout = onLogout)
                    }
                    composable(MainRoutes.Notifications.route) {
                        NotificationsScreen(
                            onPersonnelClick = { id ->
                                navController.navigate(MainRoutes.PersonnelDetail.createRoute(id))
                            }
                        )
                    }
                    composable(MainRoutes.Settings.route) { SettingsScreen() }
                    composable(MainRoutes.Profile.route) {
                        ProfileScreen(
                            onNavigateToSignature = {
                                navController.navigate(MainRoutes.SignaturePairing.route)
                            },
                            onNavigateToPhoto = {
                                navController.navigate(MainRoutes.PhotoPairing.route)
                            },
                            onNavigateToNotifications = {
                                navController.navigateToTab(MainRoutes.Notifications.route)
                            }
                        )
                    }
                    composable(MainRoutes.PersonnelList.route) {
                        PersonnelBrowseScreen(
                            onPersonnelClick = { id ->
                                navController.navigate(MainRoutes.PersonnelBrowseDetail.createRoute(id))
                            }
                        )
                    }

                    // Sédentaire – Secrétariat
                    composable(MainRoutes.SedDashboard.route) { ContextMenuItemScreens.SedDashboard() }
                    composable(MainRoutes.Correspondance.route) { ContextMenuItemScreens.Correspondance() }
                    composable(MainRoutes.GestionPersonnel.route) {
                        PersonnelScreen(
                            onPersonnelClick = { id ->
                                navController.navigate(MainRoutes.PersonnelDetail.createRoute(id))
                            },
                            onCreatePersonnel = {
                                navController.navigate(MainRoutes.PersonnelForm.createRoute(0))
                            },
                            onCreateMouvement = {
                                navController.navigate(MainRoutes.MouvementForm.createRoute(0))
                            },
                            onCreateComportement = {
                                navController.navigate(MainRoutes.ComportementForm.createRoute(0))
                            }
                        )
                    }
                    composable(MainRoutes.DeclarationPerte.route) { ContextMenuItemScreens.DeclarationPerte() }
                    composable(MainRoutes.Rapport.route) { ContextMenuItemScreens.Rapport() }
                    composable(MainRoutes.MainCouranteSec.route) { ContextMenuItemScreens.MainCouranteSec() }

                    // Sédentaire – Poste
                    composable(MainRoutes.Passation.route) { ContextMenuItemScreens.Passation() }
                    composable(MainRoutes.Armement.route) { ContextMenuItemScreens.Armement() }
                    composable(MainRoutes.Materiels.route) { ContextMenuItemScreens.Materiels() }
                    composable(MainRoutes.SituationGav.route) { ContextMenuItemScreens.SituationGav() }
                    composable(MainRoutes.MainCourantePoste.route) { ContextMenuItemScreens.MainCourantePoste() }
                    composable(MainRoutes.RenseignementSed.route) { ContextMenuItemScreens.RenseignementSed() }

                    // Division Service Général
                    composable(MainRoutes.SgDashboard.route) { ContextMenuItemScreens.SgDashboard() }
                    composable(MainRoutes.Spa.route) { ContextMenuItemScreens.Spa() }
                    composable(MainRoutes.InfoRassemblement.route) { ContextMenuItemScreens.InfoRassemblement() }
                    composable(MainRoutes.Repartition.route) { ContextMenuItemScreens.Repartition() }
                    composable(MainRoutes.Patrouille.route) { ContextMenuItemScreens.Patrouille() }
                    composable(MainRoutes.Intervention.route) { ContextMenuItemScreens.Intervention() }
                    composable(MainRoutes.DispositifExceptionnel.route) { ContextMenuItemScreens.DispositifExceptionnel() }
                    composable(MainRoutes.InstructionAutorite.route) { ContextMenuItemScreens.InstructionAutorite() }
                    composable(MainRoutes.CompteRendu.route) { ContextMenuItemScreens.CompteRendu() }
                    composable(MainRoutes.RechercheSg.route) { ContextMenuItemScreens.RechercheSg() }
                    composable(MainRoutes.RenseignementSg.route) { ContextMenuItemScreens.RenseignementSg() }

                    // Division Police Judiciaire
                    composable(MainRoutes.PjDashboard.route) { ContextMenuItemScreens.PjDashboard() }
                    composable(MainRoutes.Plainte.route) { ContextMenuItemScreens.Plainte() }
                    composable(MainRoutes.RegistreEnquete.route) { ContextMenuItemScreens.RegistreEnquete() }
                    composable(MainRoutes.Mandat.route) { ContextMenuItemScreens.Mandat() }
                    composable(MainRoutes.Convocation.route) { ContextMenuItemScreens.Convocation() }
                    composable(MainRoutes.Arrestation.route) { ContextMenuItemScreens.Arrestation() }
                    composable(MainRoutes.Gav.route) { ContextMenuItemScreens.Gav() }
                    composable(MainRoutes.Requisition.route) { ContextMenuItemScreens.Requisition() }
                    composable(MainRoutes.PersonneRecherchee.route) { ContextMenuItemScreens.PersonneRecherchee() }
                    composable(MainRoutes.Objets.route) { ContextMenuItemScreens.Objets() }
                    composable(MainRoutes.RegistreDeferrement.route) { ContextMenuItemScreens.RegistreDeferrement() }
                    composable(MainRoutes.RenseignementPj.route) { ContextMenuItemScreens.RenseignementPj() }

                    // Global modules
                    composable(MainRoutes.Cartographie.route) { ContextMenuItemScreens.Cartographie() }
                    composable(MainRoutes.Utilisateurs.route) { ContextMenuItemScreens.Utilisateurs() }
                    composable(MainRoutes.Roles.route) { ContextMenuItemScreens.Roles() }

                    // Detail / Form routes
                    composable(
                        route = MainRoutes.PersonnelDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("personnelId") {
                                type = androidx.navigation.NavType.IntType
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        PersonnelDetailScreen(
                            onEdit = { id ->
                                navController.navigate(MainRoutes.PersonnelForm.createRoute(id))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = MainRoutes.PersonnelBrowseDetail.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("personnelId") {
                                type = androidx.navigation.NavType.IntType
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        PersonnelBrowseDetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = MainRoutes.PersonnelForm.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("personnelId") {
                                type = androidx.navigation.NavType.IntType
                                defaultValue = 0
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        PersonnelFormScreen(
                            onSaved = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = MainRoutes.MouvementForm.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("personnelId") {
                                type = androidx.navigation.NavType.IntType
                                defaultValue = 0
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        MouvementFormScreen(
                            onSaved = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = MainRoutes.ComportementForm.route,
                        arguments = listOf(
                            androidx.navigation.navArgument("personnelId") {
                                type = androidx.navigation.NavType.IntType
                                defaultValue = 0
                            }
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) {
                        ComportementFormScreen(
                            onSaved = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Signature pad pairing
                    composable(MainRoutes.SignaturePairing.route) {
                        SignaturePairingScreen(
                            onQrScanned = { payload: QrPayload ->
                                val jsonStr = kotlinx.serialization.json.Json.encodeToString(QrPayload.serializer(), payload)
                                navController.navigate("signature_pad?qrPayload=${java.net.URLEncoder.encode(jsonStr, "UTF-8")}")
                            },
                            onManualCodeSubmit = { ip, port, code ->
                                navController.navigate("signature_pad?ip=$ip&port=$port&code=$code")
                            },
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "signature_pad?qrPayload={qrPayload}&ip={ip}&port={port}&code={code}",
                        arguments = listOf(
                            androidx.navigation.navArgument("qrPayload") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("ip") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("port") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("code") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) { backStackEntry ->
                        val qrPayloadStr = backStackEntry.arguments?.getString("qrPayload")
                        val ip = backStackEntry.arguments?.getString("ip")
                        val portStr = backStackEntry.arguments?.getString("port")
                        val code = backStackEntry.arguments?.getString("code")

                        val qrPayload = qrPayloadStr?.let {
                            try {
                                kotlinx.serialization.json.Json.decodeFromString(QrPayload.serializer(), java.net.URLDecoder.decode(it, "UTF-8"))
                            } catch (e: Exception) { null }
                        }

                        SignaturePadScreen(
                            qrPayload = qrPayload,
                            pairingIp = ip,
                            pairingPort = portStr?.toIntOrNull(),
                            pairingCode = code,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    // Photo capture pairing
                    composable(MainRoutes.PhotoPairing.route) {
                        SignaturePairingScreen(
                            screenTitle = "Couplage photo",
                            onQrScanned = { payload: QrPayload ->
                                val jsonStr = kotlinx.serialization.json.Json.encodeToString(QrPayload.serializer(), payload)
                                navController.navigate("photo_capture?qrPayload=${java.net.URLEncoder.encode(jsonStr, "UTF-8")}")
                            },
                            onManualCodeSubmit = { ip, port, code ->
                                navController.navigate("photo_capture?ip=$ip&port=$port&code=$code")
                            },
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "photo_capture?qrPayload={qrPayload}&ip={ip}&port={port}&code={code}",
                        arguments = listOf(
                            androidx.navigation.navArgument("qrPayload") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("ip") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("port") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            androidx.navigation.navArgument("code") {
                                type = androidx.navigation.NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        }
                    ) { backStackEntry ->
                        val qrPayloadStr = backStackEntry.arguments?.getString("qrPayload")
                        val ip = backStackEntry.arguments?.getString("ip")
                        val portStr = backStackEntry.arguments?.getString("port")
                        val code = backStackEntry.arguments?.getString("code")

                        val qrPayload = qrPayloadStr?.let {
                            try {
                                kotlinx.serialization.json.Json.decodeFromString(QrPayload.serializer(), java.net.URLDecoder.decode(it, "UTF-8"))
                            } catch (e: Exception) { null }
                        }

                        PhotoCaptureScreen(
                            qrPayload = qrPayload,
                            pairingIp = ip,
                            pairingPort = portStr?.toIntOrNull(),
                            pairingCode = code,
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    // QR auth — scan a desktop's QR code to approve its login
                    composable(MainRoutes.QrAuthScanner.route) {
                        QrAuthScannerScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Navigates to a bottom bar tab, keeping a single copy of each destination on
 * the back stack and preserving/restoring each tab's state.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Navigates to a drawer item destination, popping the back stack up to
 * the start destination so drawer and bottom nav items share a clean stack.
 */
private fun NavHostController.navigateToDrawerItem(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission-gated drawer construction
//
// Mirrors the desktop sidebar (`desktop/src/components/layout/sidebar.tsx`):
// each drawer entry declares the permission `module` it requires, divisions
// are only shown when at least one of their modules is viewable, and section
// headers are dropped when their section ends up empty. SUPER_ADMIN bypasses
// the permission table entirely (see [hasPermission]).
// ─────────────────────────────────────────────────────────────────────────────

private val DIVISION_MODULES: Map<String, List<String>> = mapOf(
    "sedentaire" to listOf(
        "sedentaire_secretariat_correspondance",
        "personnel",
        "sedentaire_secretariat_declaration_perte",
        "sedentaire_secretariat_rapport",
        "sedentaire_secretariat_main_courante",
        "sedentaire_poste_passation",
        "sedentaire_poste_armement",
        "sedentaire_poste_materiels",
        "sedentaire_poste_situation_gav",
        "sedentaire_poste_main_courante",
        "sedentaire_poste_renseignement",
    ),
    "sg" to listOf(
        "sg_spa",
        "sg_info_rassemblement",
        "sg_repartition",
        "sg_patrouille",
        "sg_intervention",
        "sg_dispositif_exceptionnel",
        "sg_instruction_autorite",
        "sg_compte_rendu",
        "sg_recherche",
        "sg_renseignement",
    ),
    "pj" to listOf(
        "pj_plainte",
        "pj_enquete",
        "pj_mandat",
        "pj_convocation",
        "pj_arrestation",
        "pj_gav",
        "pj_requisition",
        "pj_personne_recherchee",
        "pj_objets",
        "pj_deferrement",
        "pj_renseignement",
    ),
)

private fun userCanAccessDivision(user: User?, division: String): Boolean {
    val modules = DIVISION_MODULES[division] ?: return false
    return modules.any { hasPermission(user, it, PermissionAction.VIEW) }
}

/**
 * Returns true when [item] should be visible to [user]. Section headers are
 * never visible on their own (they are emitted by the section builder only
 * when the section contains at least one visible entry).
 */
private fun hasVisibleItem(user: User?, item: ContextMenuItem): Boolean {
    if (item.isSectionHeader) return false
    if (item.module == null) {
        // No module requirement: visible if it has no children, or if any
        // child is visible (mirrors desktop `hasVisibleNavItem`).
        return item.children?.any { hasVisibleItem(user, it) } ?: true
    }
    if (hasPermission(user, item.module, PermissionAction.VIEW)) return true
    return item.children?.any { hasVisibleItem(user, it) } ?: false
}

private fun buildDrawerItems(user: User?): List<ContextMenuItem> {
    val items = mutableListOf<ContextMenuItem>()

    // ── Sédentaire ──
    if (userCanAccessDivision(user, "sedentaire")) {
        items.add(ContextMenuItem(id = "section_sedentaire", title = "Sédentaire", isSectionHeader = true))
        items.add(ContextMenuItem(id = "sed_dashboard", title = "Dashboard Sédentaire", icon = Icons.Outlined.Dashboard))

        val secretariatChildren = listOf(
            ContextMenuItem(id = "sed_correspondance", title = "Correspondance", icon = Icons.Outlined.Repeat, module = "sedentaire_secretariat_correspondance"),
            ContextMenuItem(id = "sed_gestion_personnel", title = "Gestion du personnel", icon = Icons.Outlined.People, module = "personnel"),
            ContextMenuItem(id = "sed_declaration_perte", title = "Déclaration de perte", icon = Icons.Outlined.FileCopy, module = "sedentaire_secretariat_declaration_perte"),
            ContextMenuItem(id = "sed_rapport", title = "Rapport", icon = Icons.Outlined.Square, module = "sedentaire_secretariat_rapport"),
            ContextMenuItem(id = "sed_main_courante_sec", title = "Main courante", icon = Icons.Outlined.NoteAlt, module = "sedentaire_secretariat_main_courante"),
        ).filter { hasVisibleItem(user, it) }

        val posteChildren = listOf(
            ContextMenuItem(id = "sed_passation", title = "Passation", icon = Icons.Outlined.Handshake, module = "sedentaire_poste_passation"),
            ContextMenuItem(id = "sed_armement", title = "Armement", icon = Icons.Outlined.Security, module = "sedentaire_poste_armement"),
            ContextMenuItem(id = "sed_materiels", title = "Matériels", icon = Icons.Outlined.Inventory, module = "sedentaire_poste_materiels"),
            ContextMenuItem(id = "sed_situation_gav", title = "Situation GAV", icon = Icons.Outlined.ViewColumn, module = "sedentaire_poste_situation_gav"),
            ContextMenuItem(id = "sed_main_courante_poste", title = "Main courante", icon = Icons.Outlined.NoteAlt, module = "sedentaire_poste_main_courante"),
            ContextMenuItem(id = "sed_renseignement", title = "Envoi de renseignement", icon = Icons.Outlined.Message, module = "sedentaire_poste_renseignement"),
        ).filter { hasVisibleItem(user, it) }

        if (secretariatChildren.isNotEmpty()) {
            items.add(ContextMenuItem(id = "sed_secretariat", title = "Secrétariat", icon = Icons.Outlined.NoteAlt, children = secretariatChildren))
        }
        if (posteChildren.isNotEmpty()) {
            items.add(ContextMenuItem(id = "sed_poste", title = "Poste", icon = Icons.Outlined.Business, children = posteChildren))
        }
    }

    // ── Division Service Général ──
    if (userCanAccessDivision(user, "sg")) {
        items.add(ContextMenuItem(id = "section_sg", title = "Division Service Général", isSectionHeader = true))
        items.add(ContextMenuItem(id = "sg_dashboard", title = "Dashboard SG", icon = Icons.Outlined.Dashboard))
        val sgChildren = listOf(
            ContextMenuItem(id = "sg_spa", title = "SPA", subtitle = "Service Prise d'Armes", icon = Icons.Outlined.Receipt, module = "sg_spa"),
            ContextMenuItem(id = "sg_info_rassemblement", title = "Info rassemblement", icon = Icons.Outlined.Info, module = "sg_info_rassemblement"),
            ContextMenuItem(id = "sg_repartition", title = "Répartition", icon = Icons.Outlined.ViewColumn, module = "sg_repartition"),
            ContextMenuItem(id = "sg_patrouille", title = "Patrouille", icon = Icons.Outlined.Security, module = "sg_patrouille"),
            ContextMenuItem(id = "sg_intervention", title = "Intervention", icon = Icons.Outlined.Shield, module = "sg_intervention"),
            ContextMenuItem(id = "sg_dispositif_exceptionnel", title = "Dispositif exceptionnel", icon = Icons.Outlined.FilePresent, module = "sg_dispositif_exceptionnel"),
            ContextMenuItem(id = "sg_instruction_autorite", title = "Instruction autorité", icon = Icons.Outlined.Message, module = "sg_instruction_autorite"),
            ContextMenuItem(id = "sg_compte_rendu", title = "Compte rendu", subtitle = "Avec géolocalisation", icon = Icons.Outlined.Description, module = "sg_compte_rendu"),
            ContextMenuItem(id = "sg_recherche", title = "Recherche", icon = Icons.Outlined.FindInPage, module = "sg_recherche"),
            ContextMenuItem(id = "sg_renseignement", title = "Renseignement", icon = Icons.Outlined.Message, module = "sg_renseignement"),
        ).filter { hasVisibleItem(user, it) }
        items.addAll(sgChildren)
    }

    // ── Division Police Judiciaire ──
    if (userCanAccessDivision(user, "pj")) {
        items.add(ContextMenuItem(id = "section_pj", title = "Division Police Judiciaire", isSectionHeader = true))
        items.add(ContextMenuItem(id = "pj_dashboard", title = "Dashboard PJ", icon = Icons.Outlined.Dashboard))
        val pjChildren = listOf(
            ContextMenuItem(id = "pj_plainte", title = "Plainte", subtitle = "Plainte reçue", icon = Icons.Outlined.Description, module = "pj_plainte"),
            ContextMenuItem(id = "pj_registre_enquete", title = "Registre d'enquête", icon = Icons.Outlined.FindInPage, module = "pj_enquete"),
            ContextMenuItem(id = "pj_mandat", title = "Mandat", icon = Icons.Outlined.FilePresent, module = "pj_mandat"),
            ContextMenuItem(id = "pj_convocation", title = "Convocation", icon = Icons.Outlined.Email, module = "pj_convocation"),
            ContextMenuItem(id = "pj_arrestation", title = "Arrestation", icon = Icons.Outlined.LocalPolice, module = "pj_arrestation"),
            ContextMenuItem(id = "pj_gav", title = "GAV", subtitle = "Garde à vue", icon = Icons.Outlined.ViewColumn, module = "pj_gav"),
            ContextMenuItem(id = "pj_requisition", title = "Réquisition", icon = Icons.Outlined.FilePresent, module = "pj_requisition"),
            ContextMenuItem(id = "pj_personne_recherchee", title = "Personne recherchée", icon = Icons.Outlined.PersonSearch, module = "pj_personne_recherchee"),
            ContextMenuItem(id = "pj_objets", title = "Objets", icon = Icons.Outlined.Inventory2, module = "pj_objets"),
            ContextMenuItem(id = "pj_registre_deferrement", title = "Registre de déferrement", icon = Icons.Outlined.Gavel, module = "pj_deferrement"),
            ContextMenuItem(id = "pj_renseignement", title = "Renseignement", icon = Icons.Outlined.Message, module = "pj_renseignement"),
        ).filter { hasVisibleItem(user, it) }
        items.addAll(pjChildren)
    }

    // ── Global modules ──
    val globalChildren = listOf(
        ContextMenuItem(id = "cartographie", title = "Cartographie", icon = Icons.Outlined.Map, module = "cartographie"),
        ContextMenuItem(id = "utilisateurs", title = "Utilisateurs", icon = Icons.Outlined.Badge, module = "users"),
        ContextMenuItem(id = "roles", title = "Rôles", icon = Icons.Outlined.Tune, module = "roles"),
    ).filter { hasVisibleItem(user, it) }
    if (globalChildren.isNotEmpty()) {
        items.add(ContextMenuItem(id = "section_global", title = "Modules globaux", isSectionHeader = true))
        items.addAll(globalChildren)
    }

    // ── Signature / Photo / Connexion ──
    // Mobile-only pairing features: no backend permission module, so they
    // remain available to every authenticated user.
    items.add(ContextMenuItem(id = "section_signature", title = "Signature", isSectionHeader = true))
    items.add(ContextMenuItem(id = "signature_pairing", title = "Tablette de signature", icon = Icons.Outlined.Draw))
    items.add(ContextMenuItem(id = "section_photo", title = "Photo", isSectionHeader = true))
    items.add(ContextMenuItem(id = "photo_pairing", title = "Capture photo", icon = Icons.Outlined.PhotoCamera))
    items.add(ContextMenuItem(id = "section_connexion", title = "Connexion", isSectionHeader = true))
    items.add(ContextMenuItem(id = "qr_auth_scanner", title = "Connecter un ordinateur", subtitle = "Scanner un QR code", icon = Icons.Outlined.QrCodeScanner))

    return items
}
