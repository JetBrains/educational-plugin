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
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTestUtils.format
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepsList
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.util.*

class StepikTaskBuilderTest : EduTestCase() {
  private val mockConnector: MockStepikConnector get() = StepikConnector.getInstance() as MockStepikConnector
  private val dataFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"

  override fun getTestDataPath(): String = "testData/stepikTaskBuilder"

  @Test
  fun `test theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  @Test
  fun `test unsupported task`() = doTestIsInstance<UnsupportedTask>(FakeGradleBasedLanguage)
  @Test
  fun `test choice task`() {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      MockResponseFactory.fromString(
        when (path) {
          "/api/attempts?step=-1&user=-1" -> attemptsGet
          else -> error("Wrong path: ${path}")
        }
      )
    }
    doTest<ChoiceTask>(FakeGradleBasedLanguage)
  }

  @Test
  fun `test code task`() = doTest<CodeTask>(FakeGradleBasedLanguage)
  @Test
  fun `test edu task`() = doTest<EduTask>(FakeGradleBasedLanguage)
  @Test
  fun `test edu theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  @Test
  fun `test output task`() = doTest<OutputTask>(FakeGradleBasedLanguage)
  @Test
  fun `test ide task`() = doTest<IdeTask>(FakeGradleBasedLanguage)

  @Test
  fun `test edu task python`() = doTest<EduTask>(PlainTextLanguage.INSTANCE)
  @Test
  fun `test edu theory task python`() = doTest<TheoryTask>(PlainTextLanguage.INSTANCE)
  @Test
  fun `test output task python`() = doTest<OutputTask>(PlainTextLanguage.INSTANCE)
  @Test
  fun `test ide task python`() = doTest<IdeTask>(PlainTextLanguage.INSTANCE)

  @Test
  fun `test sorting task`() = doTest<SortingTask>(FakeGradleBasedLanguage)

  @Test
  fun `test matching task`() = doTest<MatchingTask>(FakeGradleBasedLanguage)

  @Test
  fun `test table task`() = doTest<TableTask>(FakeGradleBasedLanguage)

  // EDU-2730 old way: task text is get from `text` field that is displayed on Stepik
  @Test
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
  @Test
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
  @Test
  fun `test hyperskill edu task text from step`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE, HyperskillCourse())

    val block = stepSource.block
    assertNotNull(block)

    assertEquals(block!!.text, task.descriptionText)
  }

  @Test
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

  @Test
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
    val task = getTaskFromStep(language)

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

  private inline fun <reified T : Task> doTestIsInstance(language: Language) {
    val task = getTaskFromStep(language)
    assertInstanceOf(task, T::class.java)
  }

  private fun getTaskFromStep(language: Language): Task {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, language)
    return task
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
    course.languageId = language.id
    return StepikTaskBuilder(course, stepSource).createTask(stepSource.block?.name!!)
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
