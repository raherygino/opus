package com.gsoft.opus.presentation.contextmenu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
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
    fun DeclarationPerte() {
        PlaceholderScreen(
            title = "Déclaration de perte",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.FileCopy
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

    // ── Division Service Général ──

    @Composable
    fun SgDashboard() {
        PlaceholderScreen(
            title = "Dashboard SG",
            description = "Division Service Général",
            icon = Icons.Outlined.Dashboard
        )
    }

    @Composable
    fun Spa() {
        PlaceholderScreen(
            title = "SPA",
            description = "Service Prise d'Armes",
            icon = Icons.Outlined.Receipt
        )
    }

    @Composable
    fun InfoRassemblement() {
        PlaceholderScreen(
            title = "Info rassemblement",
            description = "Information communiquée durant le rassemblement",
            icon = Icons.Outlined.Info
        )
    }

    @Composable
    fun Repartition() {
        PlaceholderScreen(
            title = "Répartition",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.ViewColumn
        )
    }

    @Composable
    fun Patrouille() {
        PlaceholderScreen(
            title = "Patrouille",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Security
        )
    }

    @Composable
    fun Intervention() {
        PlaceholderScreen(
            title = "Intervention",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Shield
        )
    }

    @Composable
    fun DispositifExceptionnel() {
        PlaceholderScreen(
            title = "Dispositif exceptionnel",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.FilePresent
        )
    }

    @Composable
    fun InstructionAutorite() {
        PlaceholderScreen(
            title = "Instruction autorité",
            description = "Instruction donnée par autorité",
            icon = Icons.Outlined.Message
        )
    }

    @Composable
    fun CompteRendu() {
        PlaceholderScreen(
            title = "Compte rendu",
            description = "Avec géolocalisation",
            icon = Icons.Outlined.Description
        )
    }

    @Composable
    fun RechercheSg() {
        PlaceholderScreen(
            title = "Recherche",
            description = "Division Service Général",
            icon = Icons.Outlined.FindInPage
        )
    }

    @Composable
    fun RenseignementSg() {
        PlaceholderScreen(
            title = "Renseignement",
            description = "Division Service Général",
            icon = Icons.Outlined.Message
        )
    }

    // ── Division Police Judiciaire ──

    @Composable
    fun PjDashboard() {
        PlaceholderScreen(
            title = "Dashboard PJ",
            description = "Division Police Judiciaire",
            icon = Icons.Outlined.Dashboard
        )
    }

    @Composable
    fun Plainte() {
        PlaceholderScreen(
            title = "Plainte",
            description = "Plainte reçue",
            icon = Icons.Outlined.Description
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
    fun Convocation() {
        PlaceholderScreen(
            title = "Convocation",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.Email
        )
    }

    @Composable
    fun Arrestation() {
        PlaceholderScreen(
            title = "Arrestation",
            description = "Cette fonctionnalité sera bientôt disponible",
            icon = Icons.Outlined.LocalPolice
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

