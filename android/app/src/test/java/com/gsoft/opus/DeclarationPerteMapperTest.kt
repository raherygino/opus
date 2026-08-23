package com.gsoft.opus

import com.gsoft.opus.data.api.dto.DeclarationPerteDto
import com.gsoft.opus.data.api.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeclarationPerteMapperTest {

    private fun sampleDto() = DeclarationPerteDto(
        id = 7,
        dateDeclaration = "2026-08-22",
        heureDeclaration = "14:35:00",
        identiteDeclarant = "Jean Rakoto",
        natureObjet = "Carte nationale d'identité",
        descriptionObjet = "CNI au nom du déclarant, délivrée en 2020",
        datePerte = "2026-08-21",
        lieuPerte = "Marché d'Anosy",
        numeroAttestation = "ATT-2026-001",
        nomAgent = "Agent Rasoa",
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
        assertEquals("2026-08-22", domain.dateDeclaration)
        assertEquals("14:35:00", domain.heureDeclaration)
        assertEquals("Jean Rakoto", domain.identiteDeclarant)
        assertEquals("Carte nationale d'identité", domain.natureObjet)
        assertEquals("CNI au nom du déclarant, délivrée en 2020", domain.descriptionObjet)
        assertEquals("2026-08-21", domain.datePerte)
        assertEquals("Marché d'Anosy", domain.lieuPerte)
        assertEquals("ATT-2026-001", domain.numeroAttestation)
        assertEquals("Agent Rasoa", domain.nomAgent)
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
        val domain = sampleDto().copy(heureDeclaration = "09:05").toDomain()
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
}
