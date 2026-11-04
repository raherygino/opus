package com.gsoft.opus.core

import com.gsoft.opus.domain.model.User
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 24-hour edit window.
 *
 * Business records may only be modified within 24 hours of their creation
 * (createdAt). After that they become read-only. Administrators
 * (SUPER_ADMIN / STATION_ADMIN) bypass the lock.
 *
 * This is a UX mirror of the server-side guard (api/src/Helpers/EditWindow.php)
 * which is the authoritative enforcement — the API rejects any mutation of an
 * expired record with 403 even if a client lets it through.
 */
object EditWindow {

    const val WINDOW_MS: Long = 24L * 60 * 60 * 1000

    const val CLOSED_MESSAGE =
        "Le délai de modification de 24 heures est écoulé — cet enregistrement est en lecture seule."

    fun isAdmin(user: User?): Boolean = user?.isAdminRole == true

    /**
     * True while the record is still inside its 24h edit window.
     * createdAt comes from the API as "yyyy-MM-dd HH:mm:ss" (server time);
     * a missing/unparseable value stays editable (the API decides anyway).
     */
    fun isWithinWindow(createdAt: String?): Boolean {
        if (createdAt.isNullOrBlank()) return true
        val created = parseServerDateTime(createdAt) ?: return true
        return System.currentTimeMillis() - created <= WINDOW_MS
    }

    /** Admins always pass; everyone else only inside the window. */
    fun canEdit(user: User?, createdAt: String?): Boolean =
        isAdmin(user) || isWithinWindow(createdAt)

    private fun parseServerDateTime(value: String): Long? {
        val normalized = value.trim().replace('T', ' ')
        val patterns = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US)
                fmt.isLenient = false
                val date = fmt.parse(normalized.take(pattern.length))
                if (date != null) return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }
}
