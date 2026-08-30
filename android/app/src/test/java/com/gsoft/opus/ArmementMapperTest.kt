package com.gsoft.opus

import com.gsoft.opus.data.api.dto.ArmementAttachmentDto
import com.gsoft.opus.data.api.dto.ArmementDto
import com.gsoft.opus.data.api.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmementMapperTest {

    private fun sampleDto(
        heureReintegration: String? = null,
        etatReintegration: String? = null,
        munitionsConsommees: Int? = null
    ) = ArmementDto(
        id = 7,
        datePerception = "2026-08-23",
        heurePerception = "18:00:00",
        agentPreneurPersonnelId = 42,
        agentPreneurIm = "IM-100",
        agentPreneurGrade = "Brigadier",
        agentPreneurNom = "Jean Rakoto",
        typeArme = "Pistolet PA",
        matriculeArme = "PA-0001",
        munitions = 30,
        secteurMission = "Patrouille Centre-ville",
        etatPerception = "Bon état",
        heureReintegration = heureReintegration,
        etatReintegration = etatReintegration,
        munitionsConsommees = munitionsConsommees,
        createdBy = 1,
        createdAt = "2026-08-23 18:00:00",
        updatedAt = "2026-08-23 18:05:00"
    )

    @Test
    fun `dto maps all fields to domain`() {
        val domain = sampleDto().toDomain()
        assertEquals(7, domain.id)
        assertEquals("2026-08-23", domain.datePerception)
        assertEquals("18:00:00", domain.heurePerception)
        assertEquals(42, domain.agentPreneurPersonnelId)
        assertEquals("IM-100", domain.agentPreneurIm)
        assertEquals("Brigadier", domain.agentPreneurGrade)
        assertEquals("Jean Rakoto", domain.agentPreneurNom)
        assertEquals("Pistolet PA", domain.typeArme)
        assertEquals("PA-0001", domain.matriculeArme)
        assertEquals(30, domain.munitions)
        assertEquals("Patrouille Centre-ville", domain.secteurMission)
        assertEquals("Bon état", domain.etatPerception)
        assertNull(domain.heureReintegration)
        assertNull(domain.etatReintegration)
        assertNull(domain.munitionsConsommees)
        assertEquals(1, domain.createdBy)
    }

    @Test
    fun `reintegrated dto maps reintegration fields`() {
        val domain = sampleDto(
            heureReintegration = "22:30:00",
            etatReintegration = "Bon état",
            munitionsConsommees = 5
        ).toDomain()
        assertEquals("22:30:00", domain.heureReintegration)
        assertEquals("Bon état", domain.etatReintegration)
        assertEquals(5, domain.munitionsConsommees)
    }

    @Test
    fun `isReintegree reflects heure reintegration presence`() {
        assertFalse(sampleDto().toDomain().isReintegree)
        assertTrue(sampleDto(heureReintegration = "22:30:00").toDomain().isReintegree)
    }

    @Test
    fun `display helpers format time and identity`() {
        val domain = sampleDto().toDomain()
        assertEquals("18:00", domain.heurePerceptionDisplay)
        assertEquals("Brigadier Jean Rakoto", domain.agentPreneurDisplay)
        assertEquals("Pistolet PA — PA-0001", domain.armeDisplay)
        assertNull(domain.heureReintegrationDisplay)
    }

    @Test
    fun `munitions restantes computed when both counts known`() {
        val domain = sampleDto(heureReintegration = "22:30:00", munitionsConsommees = 5).toDomain()
        assertEquals(25, domain.munitionsRestantes)
        assertNull(sampleDto().toDomain().munitionsRestantes)
    }

    @Test
    fun `attachment dto maps to domain`() {
        val dto = ArmementAttachmentDto(
            id = 3,
            armementId = 7,
            title = "Fiche signée",
            filename = "arm_abc.pdf",
            originalFilename = "fiche.pdf",
            mimeType = "application/pdf",
            fileSize = 2345L,
            createdAt = "2026-08-23 18:10:00"
        )
        val domain = dto.toDomain()
        assertEquals(3, domain.id)
        assertEquals(7, domain.armementId)
        assertEquals("Fiche signée", domain.title)
        assertEquals("fiche.pdf", domain.originalFilename)
        assertEquals("application/pdf", domain.mimeType)
        assertEquals(2345L, domain.fileSize)
    }
}
