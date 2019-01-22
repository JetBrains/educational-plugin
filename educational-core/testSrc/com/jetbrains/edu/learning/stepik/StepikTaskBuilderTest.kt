package com.jetbrains.edu.learning.stepik

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepsList
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertThat
import java.io.File

class StepikTaskBuilderTest : EduTestCase() {

  private val dataFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"

  override fun getTestDataPath(): String = "testData/stepikTaskBuilder"

  fun `test theory task`() = doTest<TheoryTask>()
  fun `test choice task`() = doTest<ChoiceTask>()
  fun `test code task`() = doTest<CodeTask>()
  fun `test edu task`() = doTest<EduTask>()
  fun `test edu theory task`() = doTest<TheoryTask>()
  fun `test output task`() = doTest<OutputTask>()
  fun `test ide task`() = doTest<IdeTask>()

  private inline fun <reified T : Task> doTest() {
    for (language in listOf(PlainTextLanguage.INSTANCE, FakeGradleBasedLanguage)) {
      val response = loadResponse()
      val params: Map<Key<*>, Any> = mapOf(StepikAuthorizer.COURSE_LANGUAGE to language.id)

      val mapper = StepikConnector.objectMapper
      val stepSource = mapper.readValue(response, StepsList::class.java).steps[0] //use params

      val course = EduCourse()
      course.language = language.id
      val lesson = Lesson()
      val task = StepikTaskBuilder(language, lesson, stepSource, -1, -1).createTask(stepSource.block?.name) ?: error("")

      assertInstanceOf(task, T::class.java)

      assertTrue(task.taskFiles.isNotEmpty())
      for ((path, taskFile) in task.taskFiles) {
        val pathPrefix = if (path.contains(TEST_FILE_PATTERN)) EduNames.TEST else EduNames.SRC
        val pathMatcher = createPathMatcher("${pathPrefix}/", language)
        assertThat(path, pathMatcher)
        assertThat(taskFile.name, pathMatcher)
      }
    }
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
