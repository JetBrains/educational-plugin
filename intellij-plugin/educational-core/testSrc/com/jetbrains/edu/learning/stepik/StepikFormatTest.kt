package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.json.migration.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.StepikNames.PYCHARM_PREFIX
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.api.JacksonStepOptionsDeserializer.Companion.migrate
import com.jetbrains.edu.learning.stepik.api.JacksonSubmissionDeserializer.Companion.migrate
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector.Companion.getInstance
import com.jetbrains.edu.learning.stepik.api.StepikReplyDeserializer.Companion.migrate
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.function.Function

class StepikFormatTest : EduTestCase() {
  override fun getTestDataPath(): String {
    return "testData/stepik/format"
  }

  @Throws(IOException::class)
  @Test
  fun testFirstVersion() {
    doStepOptionMigrationTest(2)
  }

  @Throws(IOException::class)
  @Test
  fun testSecondVersion() {
    doStepOptionMigrationTest(3)
  }

  @Throws(IOException::class)
  @Test
  fun testThirdVersion() {
    doStepOptionMigrationTest(4)
  }

  @Throws(IOException::class)
  @Test
  fun testFifthVersion() {
    doStepOptionMigrationTest(6)
  }

  @Throws(IOException::class)
  @Test
  fun testSixthVersion() {
    for (ignored in LANGUAGE_TASK_ROOTS.entries) {
      doStepOptionMigrationTest(7, getTestName(true) + ".gradle.after.json")
    }
  }

  @Throws(IOException::class)
  @Test
  fun testSixthVersionPython() {
    doStepOptionMigrationTest(7, getTestName(true) + ".after.json")
  }

  @Throws(Exception::class)
  @Test
  fun test8Version() {
    doStepOptionMigrationTest(9)
  }

  @Throws(IOException::class)
  @Test
  fun test9Version() {
    doStepOptionMigrationTest(10)
  }

  @Throws(IOException::class)
  @Test
  fun test10Version() {
    doStepOptionMigrationTest(10)
  }

  @Throws(IOException::class)
  @Test
  fun testCourseAdditionalMaterials() {
    val responseString = loadJsonText()
    val mapper = getInstance().objectMapper
    val courseAdditionalInfo = mapper.readValue(responseString, CourseAdditionalInfo::class.java)
    assertEquals(1, courseAdditionalInfo.additionalFiles.size)
    assertTrue(courseAdditionalInfo.solutionsHidden)
  }

  @Throws(IOException::class)
  @Test
  fun testLessonAdditionalMaterials() {
    val responseString = loadJsonText()
    val mapper = getInstance().objectMapper
    val lessonAdditionalInfo = mapper.readValue(responseString, LessonAdditionalInfo::class.java)
    assertEquals("renamed", lessonAdditionalInfo.customName)
    assertEquals(1, lessonAdditionalInfo.tasksInfo.size)
    val taskAdditionalInfo = lessonAdditionalInfo.tasksInfo[123]
    assertEquals("My cool task", taskAdditionalInfo!!.name)
    assertEquals("Very cool", taskAdditionalInfo.customName)
    assertEquals(3, taskAdditionalInfo.taskFiles.size)
  }

  @Throws(IOException::class)
  @Test
  fun testAdditionalMaterialsStep() {
    val responseString = loadJsonText()
    for (ignored in listOf(EduFormatNames.KOTLIN, EduFormatNames.PYTHON)) {
      val mapper = getInstance().objectMapper
      val step = mapper.readValue(responseString, StepsList::class.java).steps[0]
      val options = step.block!!.options as PyCharmStepOptions?
      assertEquals(EduNames.ADDITIONAL_MATERIALS, options!!.title)
      assertEquals("task_file.py", options.files!![0].name)
      assertEquals("test_helperq.py", options.files!![1].name)
    }
  }

  @Throws(IOException::class)
  @Test
  fun testAvailableCourses() {
    val responseString = loadJsonText()
    val mapper = getInstance().objectMapper
    val coursesList = mapper.readValue(responseString, CoursesList::class.java)
    assertNotNull(coursesList.courses)
    assertEquals("Incorrect number of courses", 4, coursesList.courses.size)
  }

  @Throws(IOException::class)
  @Test
  fun testPlaceholderSerialization() {
    val answerPlaceholder = AnswerPlaceholder()
    answerPlaceholder.offset = 1
    answerPlaceholder.length = 10
    answerPlaceholder.placeholderText = "type here"
    answerPlaceholder.possibleAnswer = "answer1"
    val mapper = getInstance().objectMapper
    val placeholderSerialization = mapper.writeValueAsString(answerPlaceholder)
    val expected = loadJsonText()
    val `object` = mapper.readTree(expected)
    assertEquals(mapper.writeValueAsString(mapper.treeToValue(`object`, AnswerPlaceholder::class.java)), placeholderSerialization)
  }

  @Throws(IOException::class)
  @Test
  fun testTokenUptoDate() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val usersList = mapper.readValue(jsonText, UsersList::class.java)
    assertNotNull(usersList)
    assertFalse(usersList.users.isEmpty())
    val user = usersList.users[0]
    assertNotNull(user)
  }

  @Throws(IOException::class)
  @Test
  fun testCourseAuthor() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val usersList = mapper.readValue(jsonText, UsersList::class.java)
    assertNotNull(usersList)
    assertFalse(usersList.users.isEmpty())
    val user = usersList.users[0]
    assertNotNull(user)
  }

  @Throws(IOException::class)
  @Test
  fun testSections() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val sectionsList = mapper.readValue(jsonText, SectionsList::class.java)
    assertNotNull(sectionsList)
    assertEquals(1, sectionsList.sections.size)
    val unitIds = sectionsList.sections[0].units
    assertEquals(10, unitIds.size)
  }

  @Throws(IOException::class)
  @Test
  fun testUnit() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val unitsList = mapper.readValue(jsonText, UnitsList::class.java)
    assertNotNull(unitsList)
    assertNotNull(unitsList)
    assertEquals(1, unitsList.units.size)
    val lesson = unitsList.units[0].lesson
    assertEquals(13416, lesson)
  }

  @Throws(IOException::class)
  @Test
  fun testLesson() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val lessonsList = mapper.readValue(jsonText, LessonsList::class.java)
    assertNotNull(lessonsList)
    assertEquals(1, lessonsList.lessons.size)
    val lesson: Lesson = lessonsList.lessons[0]
    assertNotNull(lesson)
  }

  @Throws(IOException::class)
  @Test
  fun testStep() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    assertNotNull(stepContainer)
    val step = stepContainer.steps[0]
    assertNotNull(step)
  }

  @Throws(IOException::class)
  @Test
  fun testStepBlock() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    val step = stepContainer.steps[0]
    val block = step.block
    assertNotNull(block)
    assertNotNull(block!!.options)
    assertTrue(block.name.startsWith(PYCHARM_PREFIX))
  }

  @Throws(IOException::class)
  @Test
  fun testStepBlockOptions() {
    val options: StepOptions? = stepOptions
    assertNotNull(options)
  }

  @Throws(IOException::class)
  @Test
  fun testUpdateDate() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    val step = stepContainer.steps[0]
    assertNotNull(step.updateDate)
  }

  @Throws(IOException::class)
  @Test
  fun testOptionsTitle() {
    val options = stepOptions
    assertEquals("Our first program", options!!.title)
  }

  @Throws(IOException::class)
  @Test
  fun testOptionsDescription() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    val step = stepContainer.steps[0]
    val block = step.block
    assertEquals(
      """
  
  Traditionally the first program you write in any programming language is <code>"Hello World!"</code>.
  <br><br>
  Introduce yourself to the World.
  <br><br>
  Hint: To run a script сhoose 'Run &lt;name&gt;' on the context menu. <br>
  For more information visit <a href="https://www.jetbrains.com/help/pycharm/running-and-rerunning-applications.html">our help</a>.
  
  <br>
  
  """.trimIndent(), block!!.text
    )
  }

  @Throws(IOException::class)
  @Test
  fun testOptionsFiles() {
    val options = stepOptions
    val files: List<TaskFile>? = options!!.files
    assertEquals(2, files!!.size)
    val taskFile1 = files[0]
    assertEquals("hello_world.py", taskFile1.name)
    assertEquals("print(\"Hello, world! My name is type your name\")\n", taskFile1.text)
    val taskFile2 = files[1]
    assertEquals("tests.py", taskFile2.name)
    assertEquals(
      """from test_helper import run_common_tests, failed, passed, get_answer_placeholders


def test_ASCII():
    windows = get_answer_placeholders()
    for window in windows:
        all_ascii = all(ord(c) < 128 for c in window)
        if not all_ascii:
            failed("Please use only English characters this time.")
            return
    passed()
""", taskFile2.text
    )
  }

  @get:Throws(IOException::class)
  private val stepOptions: PyCharmStepOptions?
    get() {
      val jsonText = loadJsonText()
      val mapper = getInstance().objectMapper
      val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
      val step = stepContainer.steps[0]
      val block = step.block
      return block!!.options as PyCharmStepOptions?
    }

  @Throws(IOException::class)
  @Test
  fun testOptionsPlaceholder() {
    val options = stepOptions
    val files: List<TaskFile>? = options!!.files
    val taskFile = files!![0]
    val placeholders = taskFile.answerPlaceholders
    assertEquals(1, placeholders.size)
    val offset = placeholders[0].offset
    assertEquals(32, offset)
    val length = placeholders[0].length
    assertEquals(14, length)
    assertEquals("type your name", taskFile.text.substring(offset, offset + length))
  }

  @Throws(IOException::class)
  @Test
  fun testOptionsPlaceholderDependency() {
    val options = stepOptions
    val files: List<TaskFile>? = options!!.files
    val taskFile = files!![0]
    val placeholders = taskFile.answerPlaceholders
    assertEquals(1, placeholders.size)
    val dependency = placeholders[0].placeholderDependency
    assertNotNull(dependency)
    assertEquals("mysite/settings.py", dependency!!.fileName)
    assertEquals("task1", dependency.taskName)
    assertEquals("lesson1", dependency.lessonName)
    assertEquals(1, dependency.placeholderIndex)
  }

  @Throws(IOException::class)
  @Test
  fun testTaskStatuses() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val progressesList = mapper.readValue(jsonText, ProgressesList::class.java)
    assertNotNull(progressesList)
    val progressList = progressesList.progresses
    assertNotNull(progressList)
    val statuses = progressList.map { it.isPassed }
    assertNotNull(statuses)
    assertEquals(50, statuses.size)
  }

  @Throws(IOException::class)
  @Test
  fun testAttempts() {
    val mapper = getInstance().objectMapper
    val attemptsList = mapper.readValue(loadJsonText(), AttemptsList::class.java)
    assertNotNull(attemptsList)
    val attempts = attemptsList.attempts
    assertNotNull(attempts)
    assertEquals(20, attempts.size)
    val attempt1 = attempts[0]
    assertNull(attempt1.dataset!!.options)
    val attempt2 = attempts[11]
    assertNotNull(attempt2.dataset)
  }

  @Throws(IOException::class)
  @Test
  fun testLastSubmission() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val submissionsList = mapper.readValue(jsonText, SubmissionsList::class.java)
    assertNotNull(submissionsList)
    assertNotNull(submissionsList.submissions)
    assertEquals(20, submissionsList.submissions.size)
    val reply = submissionsList.submissions[0].reply
    assertNotNull(reply)
    val solutionFiles = (reply as EduTaskReply).solution
    assertEquals(1, solutionFiles!!.size)
    assertEquals("hello_world.py", solutionFiles[0].name)
    assertEquals("print(\"Hello, world! My name is type your name\")\n", solutionFiles[0].text)
  }

  @Throws(IOException::class)
  @Test
  fun testReplyTo7Version() {
    for ((key) in LANGUAGE_TASK_ROOTS.entries) {
      doReplyMigrationTest(7, getTestName(true) + ".gradle.after.json", key)
    }
  }

  @Throws(IOException::class)
  @Test
  fun testReplyTo7VersionPython() {
    doReplyMigrationTest(7, getTestName(true) + ".after.json", EduFormatNames.PYTHON)
  }

  @Throws(IOException::class)
  @Test
  fun testReplyTo9Version() {
    doReplyMigrationTest(9)
  }

  @Throws(IOException::class)
  @Test
  fun testReplyTo10Version() {
    doReplyMigrationTest(10)
  }

  @Throws(IOException::class)
  @Test
  fun testNonEduTasks() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    assertNotNull(stepContainer)
    assertNotNull(stepContainer.steps)
    assertEquals(3, stepContainer.steps.size)
  }

  @Throws(IOException::class)
  @Test
  fun testTaskWithCustomName() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    assertNotNull(stepContainer)
    val block = stepContainer.steps[0].block
    assertNotNull(block)
    val options = block!!.options as PyCharmStepOptions?
    assertNotNull(options)
    assertEquals("custom name", options!!.customPresentableName)
  }

  @Throws(IOException::class)
  @Test
  fun testCode() {
    val jsonText = loadJsonText()
    val mapper = getInstance().objectMapper
    val stepContainer = mapper.readValue(jsonText, StepsList::class.java)
    assertNotNull(stepContainer)
    val block = stepContainer.steps[0].block
    assertNotNull(block)
    val options = block!!.options as PyCharmStepOptions?
    assertNotNull(options)
    val limits = options!!.limits
    assertNotNull(limits)
    assertEquals(1, limits!!.size)
    val java11Limit = limits["java11"]
    assertNotNull(java11Limit)
    assertEquals(8, java11Limit!!.time as Int)
    assertEquals(256, java11Limit.memory as Int)
  }

  @Throws(IOException::class)
  private fun loadJsonText(fileName: String = testFile): String {
    return FileUtil.loadFile(File(testDataPath, fileName), true)
  }

  @Throws(IOException::class)
  private fun doStepOptionMigrationTest(maxVersion: Int, afterFileName: String? = null) {
    doMigrationTest(afterFileName) { jsonBefore: ObjectNode? ->
      migrate(
        jsonBefore!!, maxVersion
      )
    }
  }

  @Throws(IOException::class)
  private fun doReplyMigrationTest(maxVersion: Int, afterFileName: String? = null, language: String? = null) {
    doMigrationTest(afterFileName) { replyObject: ObjectNode ->
      val initialVersion = replyObject.migrate(maxVersion)
      val eduTaskWrapperString = replyObject[SerializationUtils.Json.EDU_TASK].asText()
      try {
        val eduTaskWrapperObject = ObjectMapper().readTree(eduTaskWrapperString) as ObjectNode
        val eduTaskObject = eduTaskWrapperObject.get(TASK) as ObjectNode
        eduTaskObject.migrate(initialVersion, maxVersion, language)
        val eduTaskWrapperStringAfter = ObjectMapper().writeValueAsString(eduTaskWrapperObject)
        replyObject.put(SerializationUtils.Json.EDU_TASK, eduTaskWrapperStringAfter)
        return@doMigrationTest replyObject
      }
      catch (e: IOException) {
        LOG.error(e)
      }
      null
    }
  }

  @Throws(IOException::class)
  private fun doMigrationTest(
    afterFileName: String?,
    migrationAction: Function<ObjectNode, ObjectNode?>
  ) {
    val responseString = loadJsonText()
    val afterExpected: String = afterFileName?.let { loadJsonText(it) } ?: loadJsonText(getTestName(true) + ".after.json")
    val jsonBefore = ObjectMapper().readTree(responseString) as ObjectNode
    val jsonAfter = migrationAction.apply(jsonBefore)
    var afterActual = ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(jsonAfter)
    afterActual = StringUtilRt.convertLineSeparators(afterActual!!).replace("\\n\\n".toRegex(), "\n")
    assertEquals(afterExpected, afterActual)
  }

  private val testFile: String
    get() = getTestName(true) + ".json"
}
