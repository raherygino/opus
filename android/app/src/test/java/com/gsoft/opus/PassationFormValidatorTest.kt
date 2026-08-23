package com.gsoft.opus

import com.gsoft.opus.presentation.passation.PassationFormValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PassationFormValidatorTest {

    private fun validate(
        datePassation: String = "2026-08-23",
        heurePassation: String = "18:00",
        montantVerified: Boolean = true
    ): String? = PassationFormValidator.validate(
        datePassation = datePassation,
        heurePassation = heurePassation,
        montantVerified = montantVerified
    )

    @Test
    fun `valid form with verified montant passes`() {
        assertNull(validate())
    }

    @Test
    fun `blank date passation is rejected`() {
        assertNotNull(validate(datePassation = ""))
    }

    @Test
    fun `invalid date passation format is rejected`() {
        assertNotNull(validate(datePassation = "23/08/2026"))
    }

    @Test
    fun `blank heure is rejected`() {
        assertNotNull(validate(heurePassation = ""))
    }

    @Test
    fun `invalid heure format is rejected`() {
        assertNotNull(validate(heurePassation = "25:00"))
        assertNotNull(validate(heurePassation = "18:00:00"))
        assertNotNull(validate(heurePassation = "9h00"))
    }

    @Test
    fun `unverified montant is rejected`() {
        assertNotNull(validate(montantVerified = false))
    }
}
