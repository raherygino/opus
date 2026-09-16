package com.gsoft.opus.presentation.personnerecherchee

import com.gsoft.opus.core.Constants

/** Permission module code for the PERSONNE RECHERCHÉE feature. */
const val PJ_PERSONNE_RECHERCHEE_MODULE = "pj_personne_recherchee"

/** Build the download URL for a personne recherchée photo. */
fun personneRechercheePhotoDownloadUrl(personneId: Int, photoId: Int): String =
    "${Constants.BASE_URL}/api/personne-recherchee/$personneId/photos/$photoId/download"
