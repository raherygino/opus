package com.gsoft.opus

import com.gsoft.opus.presentation.declarationperte.DeclarationPerteFormValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeclarationPerteFormValidatorTest {

    private fun validate(
        dateDeclaration: String = "2026-08-22",
        heure: String = "14:35",
        identiteDeclarant: String = "Jean Rakoto",
        natureObjet: String = "CNI",
        descriptionObjet: String = "Carte nationale d'identité",
        datePerte: String = "2026-08-21",
        lieuPerte: String = "Anosy",
        numeroAttestation: String = "ATT-001",
        nomAgent: String = "Agent Rasoa"
    ): String? = DeclarationPerteFormValidator.validate(
        dateDeclaration = dateDeclaration,
        heure = heure,
        identiteDeclarant = identiteDeclarant,
        natureObjet = natureObjet,
        descriptionObjet = descriptionObjet,
        datePerte = datePerte,
        lieuPerte = lieuPerte,
        numeroAttestation = numeroAttestation,
        nomAgent = nomAgent
    )

    @Test
    fun `valid form passes`() {
        assertNull(validate())
    }

    @Test
    fun `blank date declaration is rejected`() {
        assertNotNull(validate(dateDeclaration = ""))
    }

    @Test
    fun `invalid date declaration format is rejected`() {
        assertNotNull(validate(dateDeclaration = "22/08/2026"))
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
    fun `blank identite declarant is rejected`() {
        assertNotNull(validate(identiteDeclarant = "   "))
    }

    @Test
    fun `blank nature objet is rejected`() {
        assertNotNull(validate(natureObjet = ""))
    }

    @Test
    fun `blank description objet is rejected`() {
        assertNotNull(validate(descriptionObjet = ""))
    }

    @Test
    fun `blank date perte is rejected`() {
        assertNotNull(validate(datePerte = ""))
    }

    @Test
    fun `invalid date perte format is rejected`() {
        assertNotNull(validate(datePerte = "21-08-2026"))
    }

    @Test
    fun `blank lieu perte is rejected`() {
        assertNotNull(validate(lieuPerte = ""))
    }

    @Test
    fun `blank numero attestation is rejected`() {
        assertNotNull(validate(numeroAttestation = ""))
    }

    @Test
    fun `blank nom agent is rejected`() {
        assertNotNull(validate(nomAgent = ""))
    }
}
