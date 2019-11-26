package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.codeforces.api.*
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime

class CodeforcesFormatTest: EduTestCase() {
  override fun getTestDataPath(): String = "testData/codeforces"

  fun testAvailableContests() {
    val responseString = loadJsonText()
    val mapper = CodeforcesConnector.getInstance().objectMapper
    val coursesList = mapper.readValue(responseString, ContestsList::class.java)

    assertNotNull(coursesList.contests)
    assertEquals("Incorrect number of contests", 12, coursesList.contests.size)
    assertTrue(coursesList.isOK)
  }

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
    assertEquals(ZonedDateTime.parse("2019-08-11T15:35+03:00[Europe/Moscow]"), contestInfo.startTime)
    assertEquals(ZonedDateTime.parse("2019-08-11T17:35+03:00[Europe/Moscow]"), contestInfo.endTime)
    assertEquals(Duration.ofSeconds(-782281), contestInfo.relativeTime)
  }

  @Throws(IOException::class)
  private fun loadJsonText(): String {
    return loadJsonText(getTestFile())
  }

  @Throws(IOException::class)
  private fun loadJsonText(fileName: String): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  private fun getTestFile(): String {
    return getTestName(true) + ".json"
  }
}