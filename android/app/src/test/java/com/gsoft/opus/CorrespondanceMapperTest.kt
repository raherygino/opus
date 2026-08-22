package com.gsoft.opus

import com.gsoft.opus.data.api.dto.CorrespondanceAttachmentDto
import com.gsoft.opus.data.api.dto.CorrespondanceDto
import com.gsoft.opus.data.api.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CorrespondanceMapperTest {

    private fun sampleDto() = CorrespondanceDto(
        id = 7,
        dateCorrespondance = "2026-08-22",
        heureEnregistrement = "14:35:00",
        sens = "Entrant",
        reference = "REF-2026-001",
        emetteurDestinataire = "Ministère de l'Intérieur",
        objet = "Demande de renseignements",
        statut = "En traitement",
        createdBy = 3,
        createdAt = "2026-08-22 14:35:00",
        updatedAt = "2026-08-22 15:00:00",
        agentUsername = "agent_sec",
        agentPrenoms = "Marie",
        agentNom = "Rabe"
    )

    @Test
    fun `dto maps all fields to domain`() {
        val domain = sampleDto().toDomain()

        assertEquals(7, domain.id)
        assertEquals("2026-08-22", domain.dateCorrespondance)
        assertEquals("14:35:00", domain.heureEnregistrement)
        assertEquals("Entrant", domain.sens)
        assertEquals("REF-2026-001", domain.reference)
        assertEquals("Ministère de l'Intérieur", domain.emetteurDestinataire)
        assertEquals("Demande de renseignements", domain.objet)
        assertEquals("En traitement", domain.statut)
        assertEquals(3, domain.createdBy)
        assertEquals("2026-08-22 14:35:00", domain.createdAt)
        assertEquals("2026-08-22 15:00:00", domain.updatedAt)
        assertEquals("agent_sec", domain.agentUsername)
        assertEquals("Marie", domain.agentPrenoms)
        assertEquals("Rabe", domain.agentNom)
    }

    @Test
    fun `heure display truncates seconds from HH MM SS`() {
        assertEquals("14:35", sampleDto().toDomain().heureDisplay)
    }

    @Test
    fun `heure display keeps HH MM as is`() {
        val domain = sampleDto().copy(heureEnregistrement = "09:05").toDomain()
        assertEquals("09:05", domain.heureDisplay)
    }

    @Test
    fun `agent display name uses prenoms and nom when available`() {
        assertEquals("Marie Rabe", sampleDto().toDomain().agentDisplayName)
    }

    @Test
    fun `agent display name falls back to username`() {
        val domain = sampleDto().copy(agentPrenoms = null, agentNom = null).toDomain()
        assertEquals("agent_sec", domain.agentDisplayName)
    }

    @Test
    fun `agent display name is blank when no agent info`() {
        val domain = sampleDto().copy(agentPrenoms = null, agentNom = null, agentUsername = null).toDomain()
        assertEquals("", domain.agentDisplayName)
    }

    @Test
    fun `nullable agent fields map to null`() {
        val domain = sampleDto().copy(
            createdBy = null,
            agentUsername = null,
            agentPrenoms = null,
            agentNom = null
        ).toDomain()
        assertNull(domain.createdBy)
        assertNull(domain.agentUsername)
        assertNull(domain.agentPrenoms)
        assertNull(domain.agentNom)
    }

    @Test
    fun `attachment dto maps to domain`() {
        val dto = CorrespondanceAttachmentDto(
            id = 11,
            correspondanceId = 7,
            title = "Lettre scan",
            filename = "abc123.pdf",
            originalFilename = "lettre.pdf",
            mimeType = "application/pdf",
            fileSize = 2048L,
            createdAt = "2026-08-22 14:40:00"
        )
        val domain = dto.toDomain()

        assertEquals(11, domain.id)
        assertEquals(7, domain.correspondanceId)
        assertEquals("Lettre scan", domain.title)
        assertEquals("abc123.pdf", domain.filename)
        assertEquals("lettre.pdf", domain.originalFilename)
        assertEquals("application/pdf", domain.mimeType)
        assertEquals(2048L, domain.fileSize)
        assertEquals("2026-08-22 14:40:00", domain.createdAt)
    }
}
