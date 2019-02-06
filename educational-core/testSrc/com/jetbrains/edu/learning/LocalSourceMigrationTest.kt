package com.jetbrains.edu.learning

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
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
  fun `test to 10 version`() = doTest(10)

  private fun doTest(maxVersion: Int) {
    val before = loadJsonText(beforeFileName)
    val afterExpected = loadJsonText(afterFileName)
    val jsonBefore = ObjectMapper().readTree(before) as? ObjectNode
    val jsonAfter = migrate(jsonBefore!!, maxVersion)
    var afterActual = ObjectMapper().writer(DefaultPrettyPrinter()).writeValueAsString(jsonAfter)
    afterActual = StringUtilRt.convertLineSeparators(afterActual).replace(Regex("\\n\\n"), "\n")
    assertEquals(afterExpected, afterActual)
  }

  private fun loadJsonText(path: String): String =
    FileUtil.loadFile(File(testDataPath, path), true).replace(Regex("\\n\\n"), "\n")
}
