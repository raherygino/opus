package com.gsoft.opus.domain.model

/**
 * Convocation — summons (Police Judiciaire).
 *
 * Two convocation types share this model, distinguished by [type]:
 *   ST_PARQUET      — ST Parquet (numero: N°.../MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/ST/MC/COV/{YY})
 *   PLAINTE_DIRECTE — Plainte directe (numero: N°.../MSP/SG/DGPN/DRSP.1/CSP/A-TRIMO/PD/MC/COV/{YY})
 *
 * Both types share the exact same fields; only the numero format differs.
 * The numero is generated server-side by PlainteSequence (COV_ST / COV_PD).
 */
data class Convocation(
    val id: Int,
    val type: String,
    val dateConvocation: String,
    val numero: String,
    val nom: String,
    val adresse: String?,
    val infraction: String?,
    val personneAccuseRecu: String?,
    val numeroDossier: String?,
    val observation: String?,
    val createdBy: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    val agentUsername: String? = null,
    val agentPrenoms: String? = null,
    val agentNom: String? = null,
    val attachments: List<ConvocationAttachment> = emptyList()
) {
    val typeLabel: String
        get() = when (type) {
            "ST_PARQUET" -> "ST Parquet"
            "PLAINTE_DIRECTE" -> "Plainte directe"
            else -> type
        }
}

data class ConvocationAttachment(
    val id: Int,
    val convocationId: Int,
    val title: String,
    val filename: String,
    val originalFilename: String,
    val mimeType: String?,
    val fileSize: Long?,
    val createdAt: String?
)
