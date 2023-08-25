package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillReplyDeserializer.Companion.migrate
import java.io.File
import java.io.IOException
import kotlin.test.assertFails

class HyperskillAPIMigrationTest: EduTestCase() {
  override fun getTestDataPath(): String {
    return "testData/stepik/hyperskill/api"
  }

  @Throws(IOException::class)
  fun testReplyTo17VersionEduTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionCodeTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionChoiceTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionSortingBasedTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionStringTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionNumberTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionDataTask() = migrateTo17Version()

  @Throws(IOException::class)
  fun testReplyTo17VersionIncorrect() {
    val responseString = loadJsonText()
    val json = ObjectMapper().readTree(responseString) as ObjectNode

    assertFails("Could not guess type of reply during migration to 17 API version") {
      json.migrate(17)
    }
  }

  private fun migrateTo17Version() {
    doMigrationTest {
      it.migrate(17)
      it
    }
  }

  @Throws(IOException::class)
  private fun loadJsonText(fileName: String = testFile): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  @Throws(IOException::class)
  private fun doMigrationTest(migrationAction: (ObjectNode) -> ObjectNode?) {
    val responseString = loadJsonText()
    val afterExpected: String = loadJsonText(getTestName(true) + ".after.json")
    val jsonBefore = ObjectMapper().readTree(responseString) as ObjectNode
    val jsonAfter = migrationAction(jsonBefore)
    var afterActual = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonAfter)
    afterActual = StringUtilRt.convertLineSeparators(afterActual!!).replace("\\n\\n".toRegex(), "\n")
    assertEquals(afterExpected, afterActual)
  }

  private val testFile: String
    get() = getTestName(true) + ".json"
}