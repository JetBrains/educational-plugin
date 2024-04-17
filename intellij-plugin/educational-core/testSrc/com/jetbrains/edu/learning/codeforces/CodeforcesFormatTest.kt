package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.codeforces.api.*
import org.junit.Test
import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime

class CodeforcesFormatTest : CodeforcesTestCase() {
  @Test
  fun testAvailableContests() {
    val responseString = loadJsonText()
    val mapper = CodeforcesConnector.getInstance().objectMapper
    val coursesList = mapper.readValue(responseString,  ContestsResponse::class.java)

    assertNotNull(coursesList.result)
    assertEquals("Incorrect number of contests", 12, coursesList.result.size)
    assertTrue(coursesList.isOK)
  }

  @Test
  fun testContestInfo() {
    val responseString = loadJsonText()
    val mapper = CodeforcesConnector.getInstance().objectMapper
    val contestInfo = mapper.readValue(responseString, ContestInfo::class.java)

    assertEquals(1200, contestInfo.id)
    assertEquals("Codeforces Round #578 (Div. 2)", contestInfo.name)
    assertEquals(ContestType.CF, contestInfo.type)
    assertEquals(ContestPhase.BEFORE, contestInfo.phase)
    assertEquals(false, contestInfo.frozen)
    assertEquals(Duration.ofSeconds(7200), contestInfo.duration)
    assertEquals(ZonedDateTime.parse("2019-08-11T15:35+03:00[Europe/Moscow]").toEpochSecond(), contestInfo.startTime.toEpochSecond())
    assertEquals(ZonedDateTime.parse("2019-08-11T17:35+03:00[Europe/Moscow]").toEpochSecond(), contestInfo.endTime.toEpochSecond())
    assertEquals(Duration.ofSeconds(-782281), contestInfo.relativeTime)
  }

  @Throws(IOException::class)
  private fun loadJsonText(): String {
    return loadText(getTestFile())
  }

  private fun getTestFile(): String {
    return getTestName(true) + ".json"
  }
}