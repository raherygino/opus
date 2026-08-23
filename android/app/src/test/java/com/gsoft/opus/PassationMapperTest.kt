package com.gsoft.opus

import com.gsoft.opus.data.api.dto.PassationDto
import com.gsoft.opus.data.api.dto.PassationAttachmentDto
import com.gsoft.opus.data.api.dto.VerifiedIdentityDto
import com.gsoft.opus.data.api.dto.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PassationMapperTest {

    private fun sampleDto() = PassationDto(
        id = 5,
        datePassation = "2026-08-23",
        heurePassation = "18:00:00",
        chefDescendantUserId = 10,
        chefDescendantGrade = "Brigadier",
        chefDescendantLastname = "Rakoto",
        chefMontantUserId = 11,
        chefMontantGrade = "Adjudant",
        chefMontantLastname = "Jean",
        instructionsAutorite = "Renforcer la patrouille",
        incidentsSurvenus = "Aucun incident",
        createdBy = 10,
        createdAt = "2026-08-23 18:00:00",
        updatedAt = "2026-08-23 18:05:00",
        chefDescendantUsername = "chef_desc",
        chefMontantUsername = "chef_mont"
    )

    @Test
    fun `dto maps all fields to domain`() {
        val domain = sampleDto().toDomain()

        assertEquals(5, domain.id)
        assertEquals("2026-08-23", domain.datePassation)
        assertEquals("18:00:00", domain.heurePassation)
        assertEquals(10, domain.chefDescendantUserId)
        assertEquals("Brigadier", domain.chefDescendantGrade)
        assertEquals("Rakoto", domain.chefDescendantLastname)
        assertEquals(11, domain.chefMontantUserId)
        assertEquals("Adjudant", domain.chefMontantGrade)
        assertEquals("Jean", domain.chefMontantLastname)
        assertEquals("Renforcer la patrouille", domain.instructionsAutorite)
        assertEquals("Aucun incident", domain.incidentsSurvenus)
        assertEquals(10, domain.createdBy)
        assertEquals("chef_desc", domain.chefDescendantUsername)
        assertEquals("chef_mont", domain.chefMontantUsername)
    }

    @Test
    fun `heure display truncates seconds from HH MM SS`() {
        assertEquals("18:00", sampleDto().toDomain().heureDisplay)
    }

    @Test
    fun `heure display keeps HH MM as is`() {
        val domain = sampleDto().copy(heurePassation = "09:05").toDomain()
        assertEquals("09:05", domain.heureDisplay)
    }

    @Test
    fun `chef descendant display uses grade and lastname`() {
        assertEquals("Brigadier Rakoto", sampleDto().toDomain().chefDescendantDisplay)
    }

    @Test
    fun `chef montant display uses grade and lastname`() {
        assertEquals("Adjudant Jean", sampleDto().toDomain().chefMontantDisplay)
    }

    @Test
    fun `chef descendant display falls back to username`() {
        val domain = sampleDto().copy(
            chefDescendantGrade = null,
            chefDescendantLastname = null
        ).toDomain()
        assertEquals("chef_desc", domain.chefDescendantDisplay)
    }

    @Test
    fun `chef montant display falls back to username`() {
        val domain = sampleDto().copy(
            chefMontantGrade = null,
            chefMontantLastname = null
        ).toDomain()
        assertEquals("chef_mont", domain.chefMontantDisplay)
    }

    @Test
    fun `nullable fields map to null`() {
        val domain = sampleDto().copy(
            chefDescendantUserId = null,
            chefMontantUserId = null,
            createdBy = null,
            createdAt = null,
            updatedAt = null,
            chefDescendantUsername = null,
            chefMontantUsername = null
        ).toDomain()
        assertNull(domain.chefDescendantUserId)
        assertNull(domain.chefMontantUserId)
        assertNull(domain.createdBy)
        assertNull(domain.createdAt)
        assertNull(domain.updatedAt)
        assertNull(domain.chefDescendantUsername)
        assertNull(domain.chefMontantUsername)
    }

    @Test
    fun `attachment dto maps to domain`() {
        val dto = PassationAttachmentDto(
            id = 1,
            passationId = 5,
            title = "Feuille signée",
            filename = "pass_abc.pdf",
            originalFilename = "passation.pdf",
            mimeType = "application/pdf",
            fileSize = 2345L,
            createdAt = "2026-08-23 18:01:00"
        )
        val domain = dto.toDomain()
        assertEquals(1, domain.id)
        assertEquals(5, domain.passationId)
        assertEquals("Feuille signée", domain.title)
        assertEquals("application/pdf", domain.mimeType)
        assertEquals(2345L, domain.fileSize)
    }

    @Test
    fun `verified identity dto maps to domain`() {
        val dto = VerifiedIdentityDto(
            id = 11,
            username = "chef_mont",
            grade = "Adjudant",
            firstname = "Jean",
            lastname = "Rakoto"
        )
        val domain = dto.toDomain()
        assertEquals(11, domain.id)
        assertEquals("chef_mont", domain.username)
        assertEquals("Adjudant", domain.grade)
        assertEquals("Jean", domain.firstname)
        assertEquals("Rakoto", domain.lastname)
    }
}
