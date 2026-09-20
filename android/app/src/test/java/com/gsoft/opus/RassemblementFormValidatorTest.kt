package com.gsoft.opus

import com.gsoft.opus.presentation.rassemblement.validateRassemblementForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RassemblementFormValidatorTest {

    private fun validate(
        date: String = "2026-09-20",
        heure: String = "07:30",
        brigade: String = "Brigade A",
        effectifTheorique: String = "40",
        present: String = "38",
        absent: String = "2"
    ): Map<String, String> = validateRassemblementForm(
        dateRassemblement = date,
        heureRassemblement = heure,
        brigadeService = brigade,
        effectifTheorique = effectifTheorique,
        present = present,
        absent = absent
    )

    @Test
    fun `valid form passes`() {
        assertNull(validate().takeIf { it.isNotEmpty() })
    }

    @Test
    fun `blank date is rejected`() {
        assertEquals("La date du rassemblement est requise", validate(date = "")["dateRassemblement"])
    }

    @Test
    fun `invalid date format is rejected`() {
        assertEquals(
            "La date est invalide (format attendu : AAAA-MM-JJ)",
            validate(date = "20/09/2026")["dateRassemblement"]
        )
    }

    @Test
    fun `blank heure is rejected`() {
        assertEquals("L'heure du rassemblement est requise", validate(heure = "")["heureRassemblement"])
    }

    @Test
    fun `invalid heure format is rejected`() {
        assertEquals(
            "L'heure est invalide (format attendu : HH:MM)",
            validate(heure = "25:00")["heureRassemblement"]
        )
        assertTrue(validate(heure = "9h30").containsKey("heureRassemblement"))
    }

    @Test
    fun `heure with seconds is accepted`() {
        assertTrue(validate(heure = "07:30:00").isEmpty())
    }

    @Test
    fun `blank brigade is rejected`() {
        assertEquals("La brigade de service est requise", validate(brigade = "   ")["brigadeService"])
    }

    @Test
    fun `blank situation counters are rejected`() {
        assertEquals("L'effectif théorique est requis", validate(effectifTheorique = "")["effectifTheorique"])
        assertEquals("Le nombre de présents est requis", validate(present = "")["present"])
        assertEquals("Le nombre d'absents est requis", validate(absent = "")["absent"])
    }

    @Test
    fun `negative or non numeric situation counters are rejected`() {
        assertTrue(validate(effectifTheorique = "-1").containsKey("effectifTheorique"))
        assertTrue(validate(present = "abc").containsKey("present"))
        assertTrue(validate(absent = "2.5").containsKey("absent"))
    }

    @Test
    fun `zero situation counters are accepted`() {
        assertTrue(validate(effectifTheorique = "0", present = "0", absent = "0").isEmpty())
    }
}
