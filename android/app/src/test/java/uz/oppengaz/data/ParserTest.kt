package uz.oppengaz.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {

    private val stationJson = """
        [{"id":"s1","name":"Metan A","address":"Markaz","region":"fergana","latitude":40.38,"longitude":71.78,
          "is_active":true,"source":"community","confirmations_count":2,"missing_count":1,
          "confirmations_needed":3,"is_verified":false,"comments_count":4,
          "fuels":[{"fuel_type_id":"cng","status":"open","confidence":0.9,"report_count":3,
                    "last_updated":"2026-11-14T02:41:00.123456Z","queue_cars":12.5,"queue_report_count":2},
                   {"fuel_type_id":"lpg","status":"closed","confidence":1.0,"report_count":1,
                    "last_updated":null,"queue_cars":null,"queue_report_count":0}]}]
    """.trimIndent()

    @Test fun parsesStationWithVerificationAndFuels() {
        val s = Parser.stations(stationJson).single()
        assertEquals("Metan A", s.name)
        assertTrue(s.isCommunity)
        assertFalse(s.isVerified)
        assertEquals(2, s.confirmations)
        assertEquals(3, s.confirmationsNeeded)
        assertEquals(4, s.commentsCount)
        assertEquals(2, s.fuels.size)
        assertEquals(12.5, s.fuelInfo(Fuel.CNG)!!.queueCars!!, 0.001)
        assertNull(s.fuelInfo(Fuel.LPG)!!.queueCars)
        assertNull(s.fuelInfo(Fuel.LPG)!!.lastUpdatedMs)
    }

    @Test fun overallStatusPrefersOpen() {
        val s = Parser.stations(stationJson).single()
        assertEquals(Availability.OPEN, s.statusFor(null))
        assertEquals(Availability.OPEN, s.statusFor(Fuel.CNG))
        assertEquals(Availability.CLOSED, s.statusFor(Fuel.LPG))
        assertEquals(Availability.UNKNOWN, s.statusFor(Fuel.DIESEL))
    }

    @Test fun parsesIsoDatesWithOffsetAndZ() {
        assertEquals(Parser.millis("2026-11-14T02:41:00Z"), Parser.millis("2026-11-14T07:41:00+05:00"))
        assertNull(Parser.millis(null))
        assertNull(Parser.millis("not a date"))
    }

    @Test fun duplicateStationErrorCarriesStationId() {
        val body = """{"detail":{"message":"Bu yerda shahobcha allaqachon bor","station_id":"abc"}}"""
        val e = Parser.error(409, body)
        assertEquals("abc", e.stationId)
        assertEquals(409, e.status)
    }

    @Test fun translatesKnownEnglishErrors() {
        assertEquals("Siz shahobchadan juda uzoqdasiz",
            Parser.error(403, """{"detail":"Report location is too far from this station"}""").message)
        assertEquals("Email yoki parol noto'g'ri",
            Parser.error(401, """{"detail":"Email or password is incorrect"}""").message)
    }

    @Test fun validationErrorsAreFriendly() {
        val e = Parser.error(422, """{"detail":[{"loc":["body","email"],"msg":"bad"}]}""")
        assertEquals("Kiritilgan ma'lumot noto'g'ri", e.message)
    }

    @Test fun parsesComments() {
        val list = Parser.comments("""[{"id":"c1","author_name":"Ali","text":"Gaz bor","created_at":"2026-11-14T02:41:00Z"}]""")
        assertEquals("Ali", list.single().author)
        assertEquals("Gaz bor", list.single().text)
    }

    @Test fun distanceIsReasonable() {
        val km = distanceKm(40.0, 71.0, 40.1, 71.0)
        assertTrue(km in 10.9..11.3)
    }
}
