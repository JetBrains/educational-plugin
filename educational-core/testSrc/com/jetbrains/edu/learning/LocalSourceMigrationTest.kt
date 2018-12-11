package com.jetbrains.edu.learning

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.io.File

class LocalSourceMigrationTest : LightPlatformCodeInsightFixtureTestCase() {

  private val beforeFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"
  private val afterFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".after.json"

  override fun getTestDataPath(): String = "testData/localCourses"

  fun `test kotlin sixth version`() = doTest(7)
  fun `test python sixth version`() = doTest(7)
  fun `test remote sixth version`() = doTest(7)
  fun `test to 8 version`() = doTest(8)
  fun `test to 9 version`() = doTest(9)

  private fun doTest(maxVersion: Int) {
    val before = loadJsonText(beforeFileName)
    val afterExpected = loadJsonText(afterFileName)
    val parser = JsonParser()
    val jsonBefore = parser.parse(before).asJsonObject
    val jsonAfter = SerializationUtils.Json.CourseAdapter.migrate(jsonBefore, maxVersion)

    val gson = GsonBuilder().setPrettyPrinting().create()
    val afterActual = gson.toJson(jsonAfter)
    assertEquals(afterExpected, afterActual)
  }

  private fun loadJsonText(path: String): String = FileUtil.loadFile(File(testDataPath, path))
}
