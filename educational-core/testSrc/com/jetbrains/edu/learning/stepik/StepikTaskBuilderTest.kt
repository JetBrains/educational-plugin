package com.jetbrains.edu.learning.stepik

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepsList
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillDownloadDatasetTest.Companion.format
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertThat
import java.io.File
import java.util.*

class StepikTaskBuilderTest : EduTestCase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector
  private val dataFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"

  override fun getTestDataPath(): String = "testData/stepikTaskBuilder"

  fun `test theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  fun `test unsupported task`() = doTest<TheoryTask>(FakeGradleBasedLanguage, false)
  fun `test choice task`() {
    mockConnector.withResponseHandler(testRootDisposable) { request ->
      MockResponseFactory.fromString(
        when (val path = request.path) {
          "/api/attempts?step=-1&user=-1" -> attemptsGet
          else -> error("Wrong path: ${path}")
        }
      )
    }
    doTest<ChoiceTask>(FakeGradleBasedLanguage)
  }

  fun `test code task`() = doTest<CodeTask>(FakeGradleBasedLanguage)
  fun `test edu task`() = doTest<EduTask>(FakeGradleBasedLanguage)
  fun `test edu theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  fun `test output task`() = doTest<OutputTask>(FakeGradleBasedLanguage)
  fun `test ide task`() = doTest<IdeTask>(FakeGradleBasedLanguage)

  fun `test edu task python`() = doTest<EduTask>(PlainTextLanguage.INSTANCE)
  fun `test edu theory task python`() = doTest<TheoryTask>(PlainTextLanguage.INSTANCE)
  fun `test output task python`() = doTest<OutputTask>(PlainTextLanguage.INSTANCE)
  fun `test ide task python`() = doTest<IdeTask>(PlainTextLanguage.INSTANCE)

  // EDU-2730 old way: task text is get from `text` field that is displayed on Stepik
  fun `test edu task text from block`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE)

    val block = stepSource.block
    assertNotNull(block)
    val options = block!!.options

    assertInstanceOf(options, PyCharmStepOptions::class.java)
    assertNull((block.options as PyCharmStepOptions).descriptionText)
    assertEquals(block.text, task.descriptionText)
  }

  // EDU-2730 new way: task text from option
  fun `test edu task text from step option`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE)

    val block = stepSource.block
    assertNotNull(block)
    val options = block!!.options

    assertInstanceOf(options, PyCharmStepOptions::class.java)
    assertEquals((options as PyCharmStepOptions).descriptionText, task.descriptionText)
  }

  // EDU-3080 the old way for hyperskill: task text from step
  fun `test hyperskill edu task text from step`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE, HyperskillCourse())

    val block = stepSource.block
    assertNotNull(block)

    assertEquals(block!!.text, task.descriptionText)
  }

  fun `test code task language specific limits`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE)

    val block = stepSource.block
    assertNotNull(block)
    val options = block!!.options

    assertInstanceOf(options, PyCharmStepOptions::class.java)
    assertTrue(task.descriptionText.contains(EduCoreBundle.message("stepik.memory.limit", 256)))
    assertTrue(task.descriptionText.contains(EduCoreBundle.message("stepik.time.limit", 8)))
  }

  fun `test code task no language specific limits`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE)

    val block = stepSource.block
    assertNotNull(block)
    val options = block!!.options

    assertInstanceOf(options, PyCharmStepOptions::class.java)
    assertTrue(task.descriptionText.contains(EduCoreBundle.message("stepik.memory.limit", 256)))
    assertTrue(task.descriptionText.contains(EduCoreBundle.message("stepik.time.limit", 5)))
  }

  private inline fun <reified T : Task> doTest(language: Language, postSubmissionOnOpen: Boolean = true) {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, language)

    assertInstanceOf(task, T::class.java)

    assertTrue(task.taskFiles.isNotEmpty())
    for ((path, taskFile) in task.taskFiles) {
      val pathPrefix = if (path.contains(TEST_FILE_PATTERN)) EduNames.TEST else EduNames.SRC
      val pathMatcher = createPathMatcher("${pathPrefix}/", language)
      assertThat(path, pathMatcher)
      assertThat(taskFile.name, pathMatcher)
    }
    if (task is TheoryTask) {
      checkPostSubmissionOnOpen(task, postSubmissionOnOpen)
    }
  }

  private fun checkPostSubmissionOnOpen(task: TheoryTask, postSubmissionOnOpen: Boolean) {
    assertEquals(postSubmissionOnOpen, task.postSubmissionOnOpen)
  }

  private fun loadStepSource(): StepSource {
    val response = loadResponse()

    val mapper = StepikConnector.getInstance().objectMapper
    return mapper.readValue(response, StepsList::class.java).steps[0]
  }

  private fun buildTask(stepSource: StepSource, language: Language, course: Course = EduCourse()): Task {
    course.language = language.id
    val lesson = Lesson()
    return StepikTaskBuilder(course, lesson, stepSource, -1).createTask(stepSource.block?.name!!) ?: error("")
  }

  private fun createPathMatcher(basePath: String, language: Language): Matcher<String> {
    var pathMatcher = CoreMatchers.startsWith(basePath)
    if (language !is FakeGradleBasedLanguage) {
      pathMatcher = CoreMatchers.not(pathMatcher)
    }
    return pathMatcher
  }

  private fun loadResponse(): String = FileUtil.loadFile(File(testDataPath, dataFileName))

  companion object {
    private val TEST_FILE_PATTERN: Regex = Regex("""Tests?\.\w*""")
  }

  @org.intellij.lang.annotations.Language("JSON")
  private val attemptsGet = """{
  "attempts": [
    {
        "dataset": {
          "is_multiple_choice": true,
          "options": [ "0", "1", "2" ]
        },
        "time": "${Date().format()}",
        "status": "active",
        "time_left": null,
        "step": -1,
        "user": -1
      }
    ]
}
  """
}
