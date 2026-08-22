package com.gsoft.opus

import com.gsoft.opus.presentation.correspondance.CorrespondanceFormValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CorrespondanceFormValidatorTest {

    private fun validate(
        date: String = "2026-08-22",
        heure: String = "14:35",
        sens: String = "Entrant",
        reference: String = "REF-001",
        emetteur: String = "Ministère",
        objet: String = "Demande"
    ): String? = CorrespondanceFormValidator.validate(
        dateCorrespondance = date,
        heure = heure,
        sens = sens,
        reference = reference,
        emetteurDestinataire = emetteur,
        objet = objet
    )

    @Test
    fun `valid entrant form passes`() {
        assertNull(validate(sens = "Entrant"))
    }

    @Test
    fun `valid sortant form passes`() {
        assertNull(validate(sens = "Sortant"))
    }

    @Test
    fun `blank date is rejected`() {
        assertNotNull(validate(date = ""))
    }

    @Test
    fun `invalid date format is rejected`() {
        assertNotNull(validate(date = "22/08/2026"))
    }

    @Test
    fun `blank heure is rejected`() {
        assertNotNull(validate(heure = ""))
    }

    @Test
    fun `invalid heure format is rejected`() {
        assertNotNull(validate(heure = "14:35:00"))
        assertNotNull(validate(heure = "25:00"))
        assertNotNull(validate(heure = "9h30"))
    }

    @Test
    fun `invalid sens is rejected`() {
        assertNotNull(validate(sens = "Interne"))
        assertNotNull(validate(sens = ""))
    }

    @Test
    fun `blank reference is rejected`() {
        assertNotNull(validate(reference = "   "))
    }

    @Test
    fun `blank emetteur destinataire is rejected`() {
        assertNotNull(validate(emetteur = ""))
    }

    @Test
    fun `blank objet is rejected`() {
        assertNotNull(validate(objet = ""))
    }
}
