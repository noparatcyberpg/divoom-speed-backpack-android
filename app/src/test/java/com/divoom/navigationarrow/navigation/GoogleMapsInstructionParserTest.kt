package com.divoom.navigationarrow.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoogleMapsInstructionParserTest {

    @Test
    fun parsesThaiDirections() {
        assertEquals(
            NavigationManeuver.LEFT,
            GoogleMapsInstructionParser.parseManeuver("อีก 200 ม. เลี้ยวซ้ายเข้าสู่ถนนสุขุมวิท")
        )
        assertEquals(
            NavigationManeuver.RIGHT,
            GoogleMapsInstructionParser.parseManeuver("ชิดขวาเพื่อใช้ทางออก")
        )
        assertEquals(
            NavigationManeuver.STRAIGHT,
            GoogleMapsInstructionParser.parseManeuver("ตรงไปบนถนนพระราม 9")
        )
        assertEquals(
            NavigationManeuver.U_TURN,
            GoogleMapsInstructionParser.parseManeuver("กลับรถเมื่อทำได้")
        )
        assertEquals(
            NavigationManeuver.DESTINATION,
            GoogleMapsInstructionParser.parseManeuver("ถึงจุดหมายแล้ว")
        )
        assertEquals(
            NavigationManeuver.ROUNDABOUT,
            GoogleMapsInstructionParser.parseManeuver("เมื่อถึงวงเวียน ใช้ทางออกที่ 2")
        )
    }

    @Test
    fun parsesEnglishDirections() {
        assertEquals(
            NavigationManeuver.LEFT,
            GoogleMapsInstructionParser.parseManeuver("Turn left onto Main Street")
        )
        assertEquals(
            NavigationManeuver.RIGHT,
            GoogleMapsInstructionParser.parseManeuver("Keep right at the fork")
        )
        assertEquals(
            NavigationManeuver.STRAIGHT,
            GoogleMapsInstructionParser.parseManeuver("Continue straight for 2 km")
        )
        assertEquals(
            NavigationManeuver.U_TURN,
            GoogleMapsInstructionParser.parseManeuver("Make a U-turn")
        )
        assertEquals(
            NavigationManeuver.ROUNDABOUT,
            GoogleMapsInstructionParser.parseManeuver("At the roundabout, take the second exit")
        )
    }

    @Test
    fun ignoresUnrelatedGoogleMapsNotification() {
        assertNull(
            GoogleMapsInstructionParser.parseManeuver("แชร์ตำแหน่งกับเพื่อนของคุณ")
        )
    }

    @Test
    fun parsesNavigationDistanceInCommonGoogleMapsFormats() {
        assertEquals(
            200,
            GoogleMapsInstructionParser.parseDistanceMeters("อีก 200 ม. เลี้ยวซ้าย")
        )
        assertEquals(
            100,
            GoogleMapsInstructionParser.parseDistanceMeters("In 0.1 km, turn right")
        )
        assertEquals(
            91,
            GoogleMapsInstructionParser.parseDistanceMeters("In 300 ft, keep left")
        )
        assertEquals(
            100,
            GoogleMapsInstructionParser.parseDistanceMeters("อีก ๑๐๐ เมตรเลี้ยวขวา")
        )
    }

    @Test
    fun turnArrowsOnlyAppearWithinOneHundredMeters() {
        val farTurn = NavigationInstruction(
            text = "อีก 200 ม. เลี้ยวซ้าย",
            maneuver = NavigationManeuver.LEFT,
            distanceMeters = 200
        )
        val nearTurn = farTurn.copy(distanceMeters = 100)
        val immediateTurn = farTurn.copy(distanceMeters = null)
        val straight = farTurn.copy(
            maneuver = NavigationManeuver.STRAIGHT,
            distanceMeters = 500
        )

        assertEquals(false, farTurn.shouldDisplayTurnArrow())
        assertEquals(true, nearTurn.shouldDisplayTurnArrow())
        assertEquals(true, immediateTurn.shouldDisplayTurnArrow())
        assertEquals(true, straight.shouldDisplayTurnArrow())
    }

    @Test
    fun parserKeepsDistanceWithTheManeuver() {
        val instruction = GoogleMapsInstructionParser.parse(
            listOf("200 ม.", "เลี้ยวขวาเข้าสู่ถนนหลัก")
        )

        assertEquals(NavigationManeuver.RIGHT, instruction?.maneuver)
        assertEquals(200, instruction?.distanceMeters)
    }
}
