package com.gsoft.opus.presentation.objet

import com.gsoft.opus.core.Constants
import com.gsoft.opus.utils.isImageFile

/** Predefined object types for OBJET SAISI. */
val OBJET_SAISI_TYPES = listOf(
    "TELEPHONE",
    "ORDINATEUR",
    "VEHICULE",
    "DOCUMENT",
    "ARGENT",
    "ARME",
    "EFFETS_PERSONNELS",
    "AUTRE"
)

val OBJET_SAISI_TYPE_LABELS = mapOf(
    "TELEPHONE" to "Téléphone",
    "ORDINATEUR" to "Ordinateur",
    "VEHICULE" to "Véhicule",
    "DOCUMENT" to "Document",
    "ARGENT" to "Argent",
    "ARME" to "Arme",
    "EFFETS_PERSONNELS" to "Effets personnels",
    "AUTRE" to "Autre"
)

/** Discovery motives for OBJET TROUVÉ. */
val OBJET_TROUVE_MOTIFS = listOf(
    "REQUISITION",
    "SUR_PERSONNE",
    "PERQUISITION"
)

val OBJET_TROUVE_MOTIF_LABELS = mapOf(
    "REQUISITION" to "Réquisition",
    "SUR_PERSONNE" to "Sur une personne",
    "PERQUISITION" to "Perquisition"
)

/** Permission module code shared by both OBJET tabs. */
const val PJ_OBJETS_MODULE = "pj_objets"

fun objetSaisiAttachmentDownloadUrl(objetId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/objets/saisi/$objetId/attachments/$attachId/download"

fun objetTrouveAttachmentDownloadUrl(objetId: Int, attachId: Int): String =
    "${Constants.BASE_URL}/api/objets/trouve/$objetId/attachments/$attachId/download"

fun isObjetImageAttachment(mimeType: String?, filename: String?): Boolean =
    isImageFile(mimeType, filename)
