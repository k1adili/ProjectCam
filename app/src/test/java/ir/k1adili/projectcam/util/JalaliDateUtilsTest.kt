package ir.k1adili.projectcam.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JalaliDateUtilsTest {

    @Test
    fun `round trip conversion holds across a wide date range`() {
        var date = LocalDate.of(1950, 1, 1)
        val end = LocalDate.of(2050, 12, 31)
        while (date.isBefore(end)) {
            val j = JalaliDateUtils.toJalali(date)
            val back = JalaliDateUtils.toGregorian(j.year, j.month, j.day)
            assertEquals("round-trip failed for $date", date, back)
            date = date.plusDays(1)
        }
    }

    @Test
    fun `known reference dates convert correctly`() {
        // 24 Aug 2026 == 2 Shahrivar 1405 (verified against jdatetime reference library)
        val d1 = JalaliDateUtils.toJalali(LocalDate.of(2026, 8, 24))
        assertEquals(1405, d1.year)
        assertEquals(6, d1.month)
        assertEquals(2, d1.day)

        // 21 March 2026 == 1 Farvardin 1405 (Nowruz)
        val d2 = JalaliDateUtils.toJalali(LocalDate.of(2026, 3, 21))
        assertEquals(1405, d2.year)
        assertEquals(1, d2.month)
        assertEquals(1, d2.day)

        // 1300 AP is a leap year (30-day Esfand)
        assert(JalaliDateUtils.isLeapJalaliYear(1300))
        assertEquals(30, JalaliDateUtils.daysInJalaliMonth(1300, 12))
    }
}
