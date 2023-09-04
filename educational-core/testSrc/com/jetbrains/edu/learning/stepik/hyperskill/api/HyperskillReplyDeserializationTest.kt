package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.api.Reply
import java.io.File
import java.io.IOException
import kotlin.test.assertFails

class HyperskillReplyDeserializationTest: EduTestCase() {
  private val objectMapper = ObjectMapper()

  init {
    val module = SimpleModule()
    module.addDeserializer(Reply::class.java, HyperskillReplyDeserializer())
    objectMapper.registerModule(module)
  }

  override fun getTestDataPath(): String {
    return "testData/stepik/hyperskill/api"
  }

  @Throws(IOException::class)
  fun testGuessReplyEduTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyCodeTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyChoiceTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplySortingBasedTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyStringTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyNumberTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyDataTask() = doDeserializeTest()

  @Throws(IOException::class)
  fun testGuessReplyIncorrect() {
    val responseString = loadJsonText()

    assertFails("Could not guess type of reply during migration to 17 API version") {
      ObjectMapper().readValue(responseString, Reply::class.java)
    }
  }

  @Throws(IOException::class)
  private fun loadJsonText(fileName: String = testFile): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  @Throws(IOException::class)
  private fun doDeserializeTest() {
    val responseString = loadJsonText()
    val afterExpected: String = loadJsonText(getTestName(true) + ".after.json")
    val deserializedObject = objectMapper.readValue(responseString, Reply::class.java)
    var afterActual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deserializedObject)
    afterActual = StringUtilRt.convertLineSeparators(afterActual!!).replace("\\n\\n".toRegex(), "\n")
    assertEquals(afterExpected, afterActual)
  }

  private val testFile: String
    get() = getTestName(true) + ".json"
}