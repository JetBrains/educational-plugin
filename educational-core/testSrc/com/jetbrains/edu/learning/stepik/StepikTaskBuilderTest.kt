package com.jetbrains.edu.learning.stepik

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepsList
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertThat
import java.io.File

class StepikTaskBuilderTest : EduTestCase() {

  private val dataFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"

  override fun getTestDataPath(): String = "testData/stepikTaskBuilder"

  fun `test theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  fun `test choice task`() = doTest<ChoiceTask>(FakeGradleBasedLanguage)
  fun `test code task`() = doTest<CodeTask>(FakeGradleBasedLanguage)
  fun `test edu task`() = doTest<EduTask>(FakeGradleBasedLanguage)
  fun `test edu theory task`() = doTest<TheoryTask>(FakeGradleBasedLanguage)
  fun `test output task`() = doTest<OutputTask>(FakeGradleBasedLanguage)
  fun `test ide task`() = doTest<IdeTask>(FakeGradleBasedLanguage)

  fun `test edu task python`() = doTest<EduTask>(PlainTextLanguage.INSTANCE)
  fun `test edu theory task python`() = doTest<TheoryTask>(PlainTextLanguage.INSTANCE)
  fun `test output task python`() = doTest<OutputTask>(PlainTextLanguage.INSTANCE)
  fun `test ide task python`() = doTest<IdeTask>(PlainTextLanguage.INSTANCE)

  // EDU-2730 old way: task text is get from `text` filed that is displayed on Stepik
  fun `test edu task text from step option`() {
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
  fun `test edu task text from block`() {
    val stepSource = loadStepSource()
    val task = buildTask(stepSource, PlainTextLanguage.INSTANCE)

    val block = stepSource.block
    assertNotNull(block)
    val options = block!!.options

    assertInstanceOf(options, PyCharmStepOptions::class.java)
    assertEquals((options as PyCharmStepOptions).descriptionText, task.descriptionText)
  }

  private inline fun <reified T : Task> doTest(language: Language) {
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
  }

  private fun loadStepSource(): StepSource {
    val response = loadResponse()

    val mapper = StepikConnector.getInstance().objectMapper
    return mapper.readValue(response, StepsList::class.java).steps[0]
  }

  private fun buildTask(stepSource: StepSource, language: Language): Task {
    val course = EduCourse()
    course.language = language.id
    val lesson = Lesson()
    return StepikTaskBuilder(course, lesson, stepSource, -1, -1).createTask(stepSource.block?.name!!) ?: error("")
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
}
