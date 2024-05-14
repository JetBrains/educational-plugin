package com.jetbrains.edu.assistant.validation.test

import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.assistant.validation.util.parseCsvFile
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import junit.framework.TestCase
import org.junit.Test
import org.junit.runners.Parameterized
import kotlin.io.path.Path
import org.junit.experimental.categories.Category

@Category(AiAutoQualityCodeTests::class)
class CodeHintCompilationTest(private val lessonName: String, private val taskName: String) : ExternalResourcesTest(lessonName, taskName) {

  override val course: Course = kotlinOnboardingMockCourse
  private val maxSolutions by lazy { System.getProperty("max.solutions.testing").toIntOrNull() ?: 1 }

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = kotlinOnboardingMockCourse.lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>().map { task ->
        arrayOf(lesson.name, task.name)
      }
    }

    private val studentSolutions = parseCsvFile(Path("../../solutionsForValidation/tt_data_for_tests_version1.csv")) { record ->
      StudentSolutionRecord.buildFrom(record)
    } ?: error("Student solutions were not found")
  }

  @Test
  fun testCodeHintCompilation() {
    val task = getTargetTask()
    val state = project.eduState ?: error("Edu state is invalid")
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    studentSolutions.filter { it.lessonName == lessonName && it.taskName == taskName }.shuffled().take(maxSolutions)
      .map { it.code }.firstOrNull()?.let {
      downloadSolution(task, project, it)
      getHint(task, state, assistant)
      refreshProject()
      runCheck(task) { checkerResult ->
        TestCase.assertNotSame("Compilation error in the code: $it", checkerResult.message, CheckUtils.COMPILATION_FAILED_MESSAGE)
      }
    }
  }
}
