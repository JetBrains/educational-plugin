package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.stepik.api.*
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass
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
  @Test
  fun testGuessReplyEduTask() = doDeserializeTest(EduTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyCodeTask() = doDeserializeTest(CodeTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyChoiceTask() = doDeserializeTest(ChoiceTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplySortingBasedTask() = doDeserializeTest(SortingBasedTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyStringTask() = doDeserializeTest(TextTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyNumberTask() = doDeserializeTest(NumberTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyDataTask() = doDeserializeTest(DataTaskReply::class)

  @Throws(IOException::class)
  @Test
  fun testGuessReplyTableTask() = doDeserializeTest(TableTaskReply::class)

  @Throws(IOException::class)
  @Test
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
  private fun doDeserializeTest(type: KClass<out Reply>) {
    val responseString = loadJsonText()
    val deserializedObject = objectMapper.readValue(responseString, Reply::class.java)
    assertTrue(type.isInstance(deserializedObject))
  }

  private val testFile: String
    get() = getTestName(true) + ".json"
}