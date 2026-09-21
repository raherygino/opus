package com.gsoft.opus.presentation.contextmenu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Square
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import com.gsoft.opus.ui.components.PlaceholderScreen

/**
 * Placeholder screens for all context menu items matching the desktop app.
 * Each screen is ready for future feature implementation.
 */
object ContextMenuItemScreens {

    // ── Sédentaire – Secrétariat ──

    @Composable
    fun SedDashboard() {
        PlaceholderScreen(
            title = "Dashboard Sédentaire",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Dashboard
        )
    }

    @Composable
    fun Correspondance() {
        PlaceholderScreen(
            title = "Correspondance",
            description = "Mouvement de correspondance",
            icon = Icons.Outlined.Repeat
        )
    }

    @Composable
    fun GestionPersonnel() {
        PlaceholderScreen(
            title = "Gestion du personnel",
            description = "Gestion du personnel",
            icon = Icons.Outlined.People
        )
    }

    @Composable
    fun Rapport() {
        PlaceholderScreen(
            title = "Rapport",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Square
        )
    }

    @Composable
    fun MainCouranteSec() {
        PlaceholderScreen(
            title = "Main courante",
            description = "Secrétariat",
            icon = Icons.Outlined.NoteAlt
        )
    }

    // ── Sédentaire – Poste ──

    @Composable
    fun Passation() {
        PlaceholderScreen(
            title = "Passation",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Handshake
        )
    }

    @Composable
    fun Armement() {
        PlaceholderScreen(
            title = "Armement",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Security
        )
    }

    @Composable
    fun Materiels() {
        PlaceholderScreen(
            title = "Matériels",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Inventory
        )
    }

    @Composable
    fun SituationGav() {
        PlaceholderScreen(
            title = "Situation GAV",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.ViewColumn
        )
    }

    @Composable
    fun MainCourantePoste() {
        PlaceholderScreen(
            title = "Main courante",
            description = "Poste",
            icon = Icons.Outlined.NoteAlt
        )
    }

    @Composable
    fun RenseignementSed() {
        PlaceholderScreen(
            title = "Envoi de renseignement",
            description = "Sédentaire – Poste",
            icon = Icons.Outlined.Message
        )
    }

    // ── Division Police Judiciaire ──

    @Composable
    fun Plainte(navController: androidx.navigation.NavHostController) {
        com.gsoft.opus.presentation.plainte.PlainteScreen(
            onEntryClick = { id ->
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.PlainteDetail.createRoute(id))
            },
            onSortieClick = { id ->
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.PlainteSortieDetail.createRoute(id))
            },
            onCreateEntree = {
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.PlainteForm.createRoute(0))
            },
            onCreateSortie = {
                // For SORTIE creation, the user must first pick an existing ENTRÉE.
                // We navigate to the SORTIE form with entreeId=0; the form will
                // load the without-sortie list and prompt selection if needed.
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.PlainteSortieForm.createRoute(0, 0))
            }
        )
    }

    @Composable
    fun RegistreEnquete() {
        PlaceholderScreen(
            title = "Registre d'enquête",
            description = "Registre dossier d'enquête",
            icon = Icons.Outlined.FindInPage
        )
    }

    @Composable
    fun Mandat() {
        PlaceholderScreen(
            title = "Mandat",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.FilePresent
        )
    }

    @Composable
    fun Convocation(navController: androidx.navigation.NavHostController) {
        com.gsoft.opus.presentation.convocation.ConvocationScreen(
            onItemClick = { id ->
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.ConvocationDetail.createRoute(id))
            },
            onCreate = {
                navController.navigate(com.gsoft.opus.navigation.MainRoutes.ConvocationForm.createRoute(0))
            }
        )
    }

    @Composable
    fun Gav() {
        PlaceholderScreen(
            title = "GAV",
            description = "Garde à vue",
            icon = Icons.Outlined.ViewColumn
        )
    }

    @Composable
    fun Requisition() {
        PlaceholderScreen(
            title = "Réquisition",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.FilePresent
        )
    }

    @Composable
    fun PersonneRecherchee() {
        PlaceholderScreen(
            title = "Personne recherchée",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.PersonSearch
        )
    }

    @Composable
    fun Objets() {
        PlaceholderScreen(
            title = "Objets",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Inventory2
        )
    }

    @Composable
    fun RegistreDeferrement() {
        PlaceholderScreen(
            title = "Registre de déferrement",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Gavel
        )
    }

    @Composable
    fun RenseignementPj() {
        PlaceholderScreen(
            title = "Renseignement",
            description = "Division Police Judiciaire",
            icon = Icons.Outlined.Message
        )
    }

    // ── Global modules ──

    @Composable
    fun Cartographie() {
        PlaceholderScreen(
            title = "Cartographie",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Map
        )
    }

    @Composable
    fun Utilisateurs() {
        PlaceholderScreen(
            title = "Utilisateurs",
            description = "Gestion des utilisateurs",
            icon = Icons.Outlined.Badge
        )
    }

    @Composable
    fun Roles() {
        PlaceholderScreen(
            title = "Rôles",
            description = "Gestion des rôles",
            icon = Icons.Outlined.Tune
        )
    }
}

