package com.gsoft.opus.presentation.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gsoft.opus.navigation.BottomNavItem
import com.gsoft.opus.navigation.MainRoutes
import com.gsoft.opus.presentation.contextmenu.ContextMenuItemScreens
import com.gsoft.opus.presentation.dashboard.DashboardScreen
import com.gsoft.opus.presentation.home.HomeViewModel
import com.gsoft.opus.presentation.notifications.NotificationsScreen
import com.gsoft.opus.presentation.profile.ProfileScreen
import com.gsoft.opus.presentation.settings.SettingsScreen
import com.gsoft.opus.presentation.signature.SignaturePairingScreen
import com.gsoft.opus.presentation.signature.SignaturePadScreen
import com.gsoft.opus.presentation.photo.PhotoCaptureScreen
import com.gsoft.opus.data.signature.QrPayload
import com.gsoft.opus.ui.components.ContextMenuItem
import com.gsoft.opus.ui.components.OpusBottomNavBar
import com.gsoft.opus.ui.components.drawer.OpusAnimatedDrawer
import com.gsoft.opus.ui.components.drawer.OpusDrawerContent
import com.gsoft.opus.ui.components.drawer.rememberOpusDrawerState
import kotlinx.coroutines.launch

/**
 * Main shell of the application displayed after authentication.
 *
 * Hosts the bottom navigation bar, an inner [NavHost] with the
 * Dashboard / Notifications / Settings / Profile destinations, and a
 * premium animated navigation drawer: the blue drawer stays fixed on
 * the left while the whole content transforms into a floating card.
 */
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val drawerState = rememberOpusDrawerState()
    val scope = rememberCoroutineScope()

    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeState by homeViewModel.state.collectAsState()

    val context = LocalContext.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    val drawerItems = listOf(
        // ── Sédentaire ──
        ContextMenuItem(id = "section_sedentaire", title = "Sédentaire", isSectionHeader = true),
        ContextMenuItem(
            id = "sed_dashboard",
            title = "Dashboard Sédentaire",
            icon = Icons.Outlined.Dashboard
        ),
        ContextMenuItem(
            id = "sed_secretariat",
            title = "Secrétariat",
            icon = Icons.Outlined.NoteAlt,
            children = listOf(
                ContextMenuItem(
                    id = "sed_correspondance",
                    title = "Correspondance",
                    icon = Icons.Outlined.Repeat
                ),
                ContextMenuItem(
                    id = "sed_gestion_personnel",
                    title = "Gestion du personnel",
                    icon = Icons.Outlined.People
                ),
                ContextMenuItem(
                    id = "sed_declaration_perte",
                    title = "Déclaration de perte",
                    icon = Icons.Outlined.FileCopy
                ),
                ContextMenuItem(
                    id = "sed_rapport",
                    title = "Rapport",
                    icon = Icons.Outlined.Square
                ),
                ContextMenuItem(
                    id = "sed_main_courante_sec",
                    title = "Main courante",
                    icon = Icons.Outlined.NoteAlt
                )
            )
        ),
        ContextMenuItem(
            id = "sed_poste",
            title = "Poste",
            icon = Icons.Outlined.Business,
            children = listOf(
                ContextMenuItem(
                    id = "sed_passation",
                    title = "Passation",
                    icon = Icons.Outlined.Handshake
                ),
                ContextMenuItem(
                    id = "sed_armement",
                    title = "Armement",
                    icon = Icons.Outlined.Security
                ),
                ContextMenuItem(
                    id = "sed_materiels",
                    title = "Matériels",
                    icon = Icons.Outlined.Inventory
                ),
                ContextMenuItem(
                    id = "sed_situation_gav",
                    title = "Situation GAV",
                    icon = Icons.Outlined.ViewColumn
                ),
                ContextMenuItem(
                    id = "sed_main_courante_poste",
                    title = "Main courante",
                    icon = Icons.Outlined.NoteAlt
                ),
                ContextMenuItem(
                    id = "sed_renseignement",
                    title = "Envoi de renseignement",
                    icon = Icons.Outlined.Message
                )
            )
        ),

        // ── Division Service Général ──
        ContextMenuItem(
            id = "section_sg",
            title = "Division Service Général",
            isSectionHeader = true
        ),
        ContextMenuItem(
            id = "sg_dashboard",
            title = "Dashboard SG",
            icon = Icons.Outlined.Dashboard
        ),
        ContextMenuItem(
            id = "sg_spa",
            title = "SPA",
            subtitle = "Service Prise d'Armes",
            icon = Icons.Outlined.Receipt
        ),
        ContextMenuItem(
            id = "sg_info_rassemblement",
            title = "Info rassemblement",
            icon = Icons.Outlined.Info
        ),
        ContextMenuItem(
            id = "sg_repartition",
            title = "Répartition",
            icon = Icons.Outlined.ViewColumn
        ),
        ContextMenuItem(
            id = "sg_patrouille",
            title = "Patrouille",
            icon = Icons.Outlined.Security
        ),
        ContextMenuItem(
            id = "sg_intervention",
            title = "Intervention",
            icon = Icons.Outlined.Shield
        ),
        ContextMenuItem(
            id = "sg_dispositif_exceptionnel",
            title = "Dispositif exceptionnel",
            icon = Icons.Outlined.FilePresent
        ),
        ContextMenuItem(
            id = "sg_instruction_autorite",
            title = "Instruction autorité",
            icon = Icons.Outlined.Message
        ),
        ContextMenuItem(
            id = "sg_compte_rendu",
            title = "Compte rendu",
            subtitle = "Avec géolocalisation",
            icon = Icons.Outlined.Description
        ),
        ContextMenuItem(
            id = "sg_recherche",
            title = "Recherche",
            icon = Icons.Outlined.FindInPage
        ),
        ContextMenuItem(
            id = "sg_renseignement",
            title = "Renseignement",
            icon = Icons.Outlined.Message
        ),

        // ── Division Police Judiciaire ──
        ContextMenuItem(
            id = "section_pj",
            title = "Division Police Judiciaire",
            isSectionHeader = true
        ),
        ContextMenuItem(
            id = "pj_dashboard",
            title = "Dashboard PJ",
            icon = Icons.Outlined.Dashboard
        ),
        ContextMenuItem(
            id = "pj_plainte",
            title = "Plainte",
            subtitle = "Plainte reçue",
            icon = Icons.Outlined.Description
        ),
        ContextMenuItem(
            id = "pj_registre_enquete",
            title = "Registre d'enquête",
            icon = Icons.Outlined.FindInPage
        ),
        ContextMenuItem(
            id = "pj_mandat",
            title = "Mandat",
            icon = Icons.Outlined.FilePresent
        ),
        ContextMenuItem(
            id = "pj_convocation",
            title = "Convocation",
            icon = Icons.Outlined.Email
        ),
        ContextMenuItem(
            id = "pj_arrestation",
            title = "Arrestation",
            icon = Icons.Outlined.LocalPolice
        ),
        ContextMenuItem(
            id = "pj_gav",
            title = "GAV",
            subtitle = "Garde à vue",
            icon = Icons.Outlined.ViewColumn
        ),
        ContextMenuItem(
            id = "pj_requisition",
            title = "Réquisition",
            icon = Icons.Outlined.FilePresent
        ),
        ContextMenuItem(
            id = "pj_personne_recherchee",
            title = "Personne recherchée",
            icon = Icons.Outlined.PersonSearch
        ),
        ContextMenuItem(
            id = "pj_objets",
            title = "Objets",
            icon = Icons.Outlined.Inventory2
        ),
        ContextMenuItem(
            id = "pj_registre_deferrement",
            title = "Registre de déferrement",
            icon = Icons.Outlined.Gavel
        ),
        ContextMenuItem(
            id = "pj_renseignement",
            title = "Renseignement",
            icon = Icons.Outlined.Message
        ),

        // ── Global modules ──
        ContextMenuItem(
            id = "section_global",
            title = "Modules globaux",
            isSectionHeader = true
        ),
        ContextMenuItem(
            id = "cartographie",
            title = "Cartographie",
            icon = Icons.Outlined.Map
        ),
        ContextMenuItem(
            id = "utilisateurs",
            title = "Utilisateurs",
            icon = Icons.Outlined.Badge
        ),
        ContextMenuItem(
            id = "roles",
            title = "Rôles",
            icon = Icons.Outlined.Tune
        ),

        // ── Signature ──
        ContextMenuItem(
            id = "section_signature",
            title = "Signature",
            isSectionHeader = true
        ),
        ContextMenuItem(
            id = "signature_pairing",
            title = "Tablette de signature",
            icon = Icons.Outlined.Draw
        ),

        // ── Photo ──
        ContextMenuItem(
            id = "section_photo",
            title = "Photo",
            isSectionHeader = true
        ),
        ContextMenuItem(
            id = "photo_pairing",
            title = "Capture photo",
            icon = Icons.Outlined.PhotoCamera
        )
    )

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
            "photo_pairing" to MainRoutes.PhotoPairing.route
        )
    }

    val routeToDrawerId = remember(drawerRouteMap) {
        drawerRouteMap.entries.associate { (id, route) -> route to id }
    }
    val selectedDrawerId = routeToDrawerId[currentRoute]

    val bottomNavRoutes = remember {
        BottomNavItem.items.map { it.route }.toSet()
    }

    val bottomNavSelectedRoute = if (currentRoute in bottomNavRoutes) currentRoute else null

    OpusAnimatedDrawer(
        state = drawerState,
        drawerContent = { progress ->
            OpusDrawerContent(
                items = drawerItems,
                selectedId = selectedDrawerId,
                username = homeState.username,
                subtitle = null,
                progress = progress,
                onItemClick = { item ->
                    val route = drawerRouteMap[item.id]
                    scope.launch {
                        // Close first, then navigate once the animation finished
                        // to avoid any jank or flicker during the transition.
                        drawerState.close()
                        route?.let { navController.navigateToDrawerItem(it) }
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
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                OpusBottomNavBar(
                    items = BottomNavItem.items,
                    selectedRoute = bottomNavSelectedRoute,
                    onItemSelected = { item ->
                        navController.navigateToTab(item.route)
                    },
                    fabExpanded = drawerState.isOpen,
                    onFabClick = {
                        scope.launch {
                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                        }
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = MainRoutes.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                enterTransition = {
                    fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.96f)
                },
                exitTransition = {
                    fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.02f)
                },
                popEnterTransition = {
                    fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 1.02f)
                },
                popExitTransition = {
                    fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.96f)
                }
            ) {
                composable(MainRoutes.Dashboard.route) { DashboardScreen(onLogout = onLogout) }
                composable(MainRoutes.Notifications.route) { NotificationsScreen() }
                composable(MainRoutes.Settings.route) { SettingsScreen() }
                composable(MainRoutes.Profile.route) { ProfileScreen() }

                // Sédentaire – Secrétariat
                composable(MainRoutes.SedDashboard.route) { ContextMenuItemScreens.SedDashboard() }
                composable(MainRoutes.Correspondance.route) { ContextMenuItemScreens.Correspondance() }
                composable(MainRoutes.GestionPersonnel.route) { ContextMenuItemScreens.GestionPersonnel() }
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

                // Signature pad
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
