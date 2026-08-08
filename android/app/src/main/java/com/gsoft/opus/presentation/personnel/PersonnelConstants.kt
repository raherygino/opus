package com.gsoft.opus.presentation.personnel

import com.gsoft.opus.core.Constants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Same grade list as the desktop app (assets/json/grade.json). */
val GRADES = listOf(
    "Elève Agent de Police",
    "Elève Inspecteur de Police",
    "Elève Officier de Police",
    "Elève Commissaire de Police",
    "Agent de Police Stagiaire",
    "Agent de Police",
    "Sous-Brigadier de Police",
    "Brigadier de Police",
    "Brigadier Chef de Police",
    "Inspecteur de Police",
    "Inspecteur Principale de Police",
    "Inspecteur Principale de Police de Classe Exceptionnelle",
    "Officier de Police",
    "Officier Principale de Police",
    "Officier Principale de Police de Classe Exceptionnelle",
    "Commissaire de Police",
    "Commissaire Principale de Police",
    "Commissaire Divisionaire de Police",
    "Contrôle Général de Police",
    "Inspecteur Général de Police"
)

/** Same affectation list as the desktop personnel form. */
val AFFECTATIONS = listOf(
    "Service Général (SG)",
    "Police Judiciaire (PJ)",
    "Sédentaire",
    "Unité Spéciale",
    "Administration"
)

/** Same mouvement types as the desktop personnel tabs page. */
val MOUVEMENT_TYPES = listOf(
    "Congé",
    "Permission",
    "Mission",
    "Mutation",
    "Promotion",
    "Suspension",
    "Retraite",
    "Démission",
    "Détachement",
    "Repos",
    "Repos médical",
    "Absent non motivé"
)

fun personnelPhotoUrl(personnelId: Int): String =
    "${Constants.BASE_URL}/api/personnel/$personnelId/photo"

fun personnelAttachmentDownloadUrl(personnelId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/personnel/$personnelId/attachments/$attachId/download"

fun mouvementAttachmentDownloadUrl(mouvementId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/mouvements/$mouvementId/attachments/$attachId/download"

/** Formats a file size like the desktop implementation (o / Ko / Mo). */
fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0) return ""
    if (bytes < 1024) return "$bytes o"
    if (bytes < 1048576) return String.format(Locale.FRANCE, "%.1f Ko", bytes / 1024.0)
    return String.format(Locale.FRANCE, "%.1f Mo", bytes / 1048576.0)
}

/** Converts a DatePicker millis selection to an ISO date (yyyy-MM-dd). */
fun millisToIsoDate(millis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}

/** Formats an ISO date (yyyy-MM-dd) as dd/MM/yyyy for display. */
fun formatDateDisplay(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "—"
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate.take(10))
        parsed?.let { SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(it) }
    }.getOrNull() ?: isoDate
}

/** Computes the return date (date_depart + days), same rule as the desktop. */
fun computeDateRetour(dateDepart: String, days: String): String {
    if (dateDepart.isBlank() || days.isBlank()) return ""
    val daysInt = days.toIntOrNull() ?: return ""
    return runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = format.parse(dateDepart) ?: return ""
        val cal = java.util.Calendar.getInstance()
        cal.time = date
        cal.add(java.util.Calendar.DAY_OF_MONTH, daysInt)
        format.format(cal.time)
    }.getOrDefault("")
}
