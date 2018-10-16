package com.jetbrains.edu.learning.stepik

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.stepik.StepikWrappers.StepContainer
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
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
      val params: Map<Key<*>, Any> = mapOf(StepikConnector.COURSE_LANGUAGE to language.id)
      val stepSource = StepikClient.deserializeStepikResponse(StepContainer::class.java, response, params).steps[0]

      val course = StepikCourse()
      course.language = language.id
      val task = StepikTaskBuilder(language, stepSource, -1).createTask(stepSource.block.name)

      assertInstanceOf(task, T::class.java)

      assertTrue(task.taskFiles.isNotEmpty())
      for ((path, taskFile) in task.taskFiles) {
        val pathMatcher = createPathMatcher("${EduNames.SRC}/", language)
        assertThat(path, pathMatcher)
        assertThat(taskFile.name, pathMatcher)
      }
      for ((path, _) in task.testsText) {
        assertThat(path, createPathMatcher("${EduNames.TEST}/", language))
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
}
