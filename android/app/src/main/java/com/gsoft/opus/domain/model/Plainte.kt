package com.gsoft.opus.domain.model

/**
 * Plainte ENTRÉE — incoming complaint (Police Judiciaire).
 *
 * Three complaint types share this model, distinguished by [type]:
 *   ST_PARQUET      — ST Parquet (has numeroSt + partieCivile + adressePc)
 *   PLAINTE_DIRECTE — Plainte directe (has partieCivile + adressePc, no numeroSt)
 *   RAPPORT_POLICE  — Rapport de police (no partieCivile, no adressePc, no numeroSt)
 *
 * The dossier number (numeroDossier) is generated server-side.
 * OPJ and Enquêteur are FK references to personnel (grade shown in UI).
 */
data class PlainteEntree(
    val id: Int,
    val type: String,
    val datePlainte: String,
    val numeroDossier: String,
    val numeroSt: String?,
    val opjPersonnelId: Int?,
    val enqueteurPersonnelId: Int?,
    val partieCivile: String?,
    val miseEnCause: String?,
    val adressePc: String?,
    val infraction: String?,
    val prejudice: String?,
    val lieuInfraction: String?,
    val heureInfraction: String?,
    val observation: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val opjPrenoms: String? = null,
    val opjNom: String? = null,
    val opjGrade: String? = null,
    val opjIm: String? = null,
    val enqueteurPrenoms: String? = null,
    val enqueteurNom: String? = null,
    val enqueteurGrade: String? = null,
    val enqueteurIm: String? = null,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<PlainteEntreeAttachment> = emptyList()
) {
    /** "HH:MM:SS" from the API is displayed as "HH:MM". */
    val heureDisplay: String get() = heureInfraction?.take(5) ?: "—"

    /** Display name of the OPJ (grade + name). */
    val opjDisplayName: String
        get() = listOfNotNull(opjPrenoms, opjNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { name ->
                if (name.isBlank()) "—"
                else if (!opjGrade.isNullOrBlank()) "$name ($opjGrade)"
                else name
            }

    /** Display name of the Enquêteur (grade + name). */
    val enqueteurDisplayName: String
        get() = listOfNotNull(enqueteurPrenoms, enqueteurNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { name ->
                if (name.isBlank()) "—"
                else if (!enqueteurGrade.isNullOrBlank()) "$name ($enqueteurGrade)"
                else name
            }

    /** Display name of the agent who recorded the complaint. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }

    /** Human-readable label for the complaint type. */
    val typeLabel: String
        get() = when (type) {
            "ST_PARQUET" -> "ST Parquet"
            "PLAINTE_DIRECTE" -> "Plainte directe"
            "RAPPORT_POLICE" -> "Rapport de police"
            else -> type
        }
}

data class PlainteEntreeAttachment(
    val id: Int,
    val plainteEntreeId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

/**
 * Plainte SORTIE — outgoing processing of an ENTRÉE complaint.
 *
 * Two natures share this model, distinguished by [nature]:
 *   DAT         — DAT (dateDeferrement optional)
 *   DEFERREMENT — Déferrement (dateDeferrement required)
 *
 * The sortie number (numero) is generated server-side.
 * Always linked to exactly one ENTRÉE via [plainteEntreeId].
 */
data class PlainteSortie(
    val id: Int,
    val plainteEntreeId: Int,
    val nature: String,
    val dateSortie: String,
    val numero: String,
    val numeroTtr: String?,
    val nomSubstitut: String?,
    val dateDeferrement: String?,
    val observation: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val entreeType: String? = null,
    val entreeNumeroDossier: String? = null,
    val entreeDatePlainte: String? = null,
    val entreeInfraction: String? = null,
    val entreeMiseEnCause: String? = null,
    val entreePartieCivile: String? = null,
    val entreeOpjPrenoms: String? = null,
    val entreeOpjNom: String? = null,
    val entreeOpjGrade: String? = null,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<PlainteSortieAttachment> = emptyList()
) {
    /** Human-readable label for the sortie nature. */
    val natureLabel: String
        get() = when (nature) {
            "DAT" -> "DAT"
            "DEFERREMENT" -> "Déferrement"
            else -> nature
        }

    /** Display name of the agent who recorded the sortie. */
    val agentDisplayName: String
        get() = listOfNotNull(agentPrenoms, agentNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { agentUsername ?: "" }

    /** Display name of the linked ENTRÉE's OPJ. */
    val entreeOpjDisplayName: String
        get() = listOfNotNull(entreeOpjPrenoms, entreeOpjNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { name ->
                if (name.isBlank()) "—"
                else if (!entreeOpjGrade.isNullOrBlank()) "$name ($entreeOpjGrade)"
                else name
            }

    /** Human-readable label for the linked ENTRÉE's type. */
    val entreeTypeLabel: String
        get() = when (entreeType) {
            "ST_PARQUET" -> "ST Parquet"
            "PLAINTE_DIRECTE" -> "Plainte directe"
            "RAPPORT_POLICE" -> "Rapport de police"
            else -> entreeType ?: "—"
        }
}

data class PlainteSortieAttachment(
    val id: Int,
    val plainteSortieId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)

/**
 * Lightweight ENTRÉE summary used by the SORTIE picker
 * (ENTRÉE records that have no linked SORTIE yet).
 */
data class PlainteEntreeSummary(
    val id: Int,
    val type: String,
    val numeroDossier: String,
    val datePlainte: String,
    val partieCivile: String?,
    val miseEnCause: String?,
    val infraction: String?,
    val opjPrenoms: String?,
    val opjNom: String?,
    val opjGrade: String?
) {
    val typeLabel: String
        get() = when (type) {
            "ST_PARQUET" -> "ST Parquet"
            "PLAINTE_DIRECTE" -> "Plainte directe"
            "RAPPORT_POLICE" -> "Rapport de police"
            else -> type
        }

    val opjDisplayName: String
        get() = listOfNotNull(opjPrenoms, opjNom)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .let { name ->
                if (name.isBlank()) "—"
                else if (!opjGrade.isNullOrBlank()) "$name ($opjGrade)"
                else name
            }
}
