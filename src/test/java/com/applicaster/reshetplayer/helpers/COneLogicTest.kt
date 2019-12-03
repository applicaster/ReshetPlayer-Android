package com.applicaster.reshetplayer.helpers


import org.mockito.runners.MockitoJUnitRunner
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.Assert.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class COneLogicTest {

    @Test fun testCOneLogic() {

        //outside window length
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0000"), 0, parseDate("2019-11-25T11:33:44+0000"), 2, 0, 3600000), false)

        //inside window length inside C+1
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0000"), 0, parseDate("2019-12-01T02:33:44+0000"), 2, 0, 3600000 * 36), true)

        //inside window length outside C+1
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0000"), 0, parseDate("2019-12-01T01:33:44+0000"), 2, 0, 3600000 * 36), false)

        //inside window length inside C+1 timezone israel
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0200"), 0, parseDate("2019-12-01T02:33:44+0200"), 0, 0, 3600000 * 36), true)

        //inside window length outside C+1 timezone israel
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0200"), 0, parseDate("2019-12-01T01:33:44+0200"), 0, 0, 3600000 * 36), false)

        //with server delta time minus one hour
        //inside window length outside C+1 timezone israel
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0200"), -3600000, parseDate("2019-12-01T01:33:44+0200"), 0, 0, 3600000 * 36), true)

        //inside window length inside C+1 timezone israel summer daytime (+0300)
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0300"), 0, parseDate("2019-12-01T02:33:44+0300"), 23, 0, 3600000 * 36), true)

        //inside window length outside C+1 timezone israel summer daytime (+0300)
        assertEquals(isInOne(parseDate("2019-12-02T02:33:44+0300"), 0, parseDate("2019-12-01T01:33:44+0300"), 23, 0, 3600000 * 36), false)

    }


    val simpleDateFormatServerTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

    private fun parseDate(str: String) : Long {
        return try {
            simpleDateFormatServerTime.parse(str).time
        } catch (e: ParseException) {
            0
        }
    }


}