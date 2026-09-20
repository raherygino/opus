package com.gsoft.opus

import com.gsoft.opus.data.api.dto.RassemblementJournalierDto
import com.gsoft.opus.data.api.dto.RepartitionSecteurDto
import com.gsoft.opus.data.api.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RassemblementMapperTest {

    private fun sampleDto() = RassemblementJournalierDto(
        id = 7,
        dateRassemblement = "2026-09-20",
        heureRassemblement = "07:30:00",
        brigadeService = "Brigade A",
        officierPermanence = "Cdt Rakoto",
        inspecteurPermanence = "Insp Rabe",
        chefPoste = "Adj Ranaivo",
        instructionsAutorite = "Vigilance renforcée",
        effectifTheorique = 40,
        present = 38,
        absent = 2,
        motifAbsence = "Congé | Mission",
        createdBy = 10,
        createdAt = "2026-09-20 07:30:00",
        updatedAt = "2026-09-20 07:35:00",
        agentUsername = "agent1",
        agentPrenoms = "Jean",
        agentNom = "Rakoto",
        repartitions = listOf(
            RepartitionSecteurDto(id = 1, type = "diurne", secteur = "Secteur 1", effectifEngage = "10"),
            RepartitionSecteurDto(id = 2, type = "nocturne", secteur = "Secteur 2", effectifEngage = "6")
        )
    )

    @Test
    fun `dto maps all fields to domain`() {
        val domain = sampleDto().toDomain()

        assertEquals(7, domain.id)
        assertEquals("2026-09-20", domain.dateRassemblement)
        assertEquals("07:30:00", domain.heureRassemblement)
        assertEquals("Brigade A", domain.brigadeService)
        assertEquals("Cdt Rakoto", domain.officierPermanence)
        assertEquals("Insp Rabe", domain.inspecteurPermanence)
        assertEquals("Adj Ranaivo", domain.chefPoste)
        assertEquals("Vigilance renforcée", domain.instructionsAutorite)
        assertEquals(40, domain.effectifTheorique)
        assertEquals(38, domain.present)
        assertEquals(2, domain.absent)
        assertEquals("Congé | Mission", domain.motifAbsence)
        assertEquals(10, domain.createdBy)
        assertEquals("agent1", domain.agentUsername)
    }

    @Test
    fun `heure display truncates seconds from HH MM SS`() {
        assertEquals("07:30", sampleDto().toDomain().heureDisplay)
    }

    @Test
    fun `agent display name prefers personnel names over username`() {
        assertEquals("Jean Rakoto", sampleDto().toDomain().agentDisplayName)
    }

    @Test
    fun `repartitions map with type differentiation`() {
        val domain = sampleDto().toDomain()

        assertEquals(2, domain.repartitions.size)
        assertTrue(domain.repartitions[0].isDiurne)
        assertTrue(!domain.repartitions[1].isDiurne)
        assertEquals("Secteur 1", domain.repartitions[0].secteur)
        assertEquals("nocturne", domain.repartitions[1].type)
    }

    @Test
    fun `missing repartitions default to empty list`() {
        val domain = sampleDto().copy(repartitions = null).toDomain()
        assertEquals(0, domain.repartitions.size)
    }
}
