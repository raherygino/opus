package com.gsoft.opus

import com.gsoft.opus.presentation.armement.ArmementFormValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArmementFormValidatorTest {

    private fun validate(
        datePerception: String = "2026-08-23",
        heurePerception: String = "18:00",
        agentSelected: Boolean = true,
        typeArme: String = "Pistolet PA",
        matriculeArme: String = "PA-0001",
        munitions: String = "30"
    ): String? = ArmementFormValidator.validate(
        datePerception = datePerception,
        heurePerception = heurePerception,
        agentSelected = agentSelected,
        typeArme = typeArme,
        matriculeArme = matriculeArme,
        munitions = munitions
    )

    @Test
    fun `valid form passes`() {
        assertNull(validate())
    }

    @Test
    fun `blank date perception is rejected`() {
        assertNotNull(validate(datePerception = ""))
    }

    @Test
    fun `invalid date perception format is rejected`() {
        assertNotNull(validate(datePerception = "23/08/2026"))
    }

    @Test
    fun `blank heure is rejected`() {
        assertNotNull(validate(heurePerception = ""))
    }

    @Test
    fun `invalid heure format is rejected`() {
        assertNotNull(validate(heurePerception = "25:00"))
        assertNotNull(validate(heurePerception = "18:00:00"))
        assertNotNull(validate(heurePerception = "9h00"))
    }

    @Test
    fun `unselected agent is rejected`() {
        assertNotNull(validate(agentSelected = false))
    }

    @Test
    fun `blank type arme is rejected`() {
        assertNotNull(validate(typeArme = ""))
        assertNotNull(validate(typeArme = "   "))
    }

    @Test
    fun `blank matricule arme is rejected`() {
        assertNotNull(validate(matriculeArme = ""))
    }

    @Test
    fun `non-numeric munitions is rejected`() {
        assertNotNull(validate(munitions = "abc"))
        assertNotNull(validate(munitions = "-5"))
        assertNotNull(validate(munitions = "3.5"))
    }

    @Test
    fun `blank munitions is allowed`() {
        assertNull(validate(munitions = ""))
    }

    // ─── Reintegration ──────────────────────────────────────────────

    private fun validateReintegration(
        heureReintegration: String = "22:30",
        etatReintegration: String = "Bon état",
        munitionsConsommees: String = "5",
        munitionsPercues: Int? = 30
    ): String? = ArmementFormValidator.validateReintegration(
        heureReintegration = heureReintegration,
        etatReintegration = etatReintegration,
        munitionsConsommees = munitionsConsommees,
        munitionsPercues = munitionsPercues
    )

    @Test
    fun `valid reintegration passes`() {
        assertNull(validateReintegration())
    }

    @Test
    fun `blank heure reintegration is rejected`() {
        assertNotNull(validateReintegration(heureReintegration = ""))
    }

    @Test
    fun `invalid heure reintegration format is rejected`() {
        assertNotNull(validateReintegration(heureReintegration = "25:00"))
    }

    @Test
    fun `blank etat reintegration is rejected`() {
        assertNotNull(validateReintegration(etatReintegration = ""))
        assertNotNull(validateReintegration(etatReintegration = "  "))
    }

    @Test
    fun `blank munitions consommees is rejected`() {
        assertNotNull(validateReintegration(munitionsConsommees = ""))
    }

    @Test
    fun `negative or non-numeric munitions consommees is rejected`() {
        assertNotNull(validateReintegration(munitionsConsommees = "-1"))
        assertNotNull(validateReintegration(munitionsConsommees = "abc"))
    }

    @Test
    fun `munitions consommees above munitions percues is rejected`() {
        assertNotNull(validateReintegration(munitionsConsommees = "31", munitionsPercues = 30))
    }

    @Test
    fun `munitions consommees equal to munitions percues passes`() {
        assertNull(validateReintegration(munitionsConsommees = "30", munitionsPercues = 30))
    }

    @Test
    fun `any munitions consommees passes when munitions percues unknown`() {
        assertNull(validateReintegration(munitionsConsommees = "500", munitionsPercues = null))
    }
}
