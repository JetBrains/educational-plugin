package com.jetbrains.edu.assistant.validation.test

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.assistant.validation.util.parseCsvFile
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.io.path.Path

@RunWith(Parameterized::class)
class CodeHintCompilationTest(private val lessonName: String, private val taskName: String) : JdkCheckerTestBase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = kotlinOnboardingMockCourse.lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>().map { task ->
        arrayOf(lesson.name, task.name)
      }
    }

    private val studentSolutions = parseCsvFile(Path("../../solutionsForValidation/tt_data_for_tests_version1.csv")) { record ->
      StudentSolutionRecord(record.get(0).toInt(), record.get(1), record.get(2), record.get(3))
    } ?: error("Student solutions was not found")
  }

  @Test
  fun testCodeHintCompilation() {
    CheckActionListener.setCheckResultVerifier { _, checkResult ->
      TestCase.assertNotSame(checkResult.message, CheckUtils.COMPILATION_FAILED_MESSAGE)
    }

    // TODO: `NavigationUtils.navigateToTask(project, task)` doesn't work with framework lessons
    var task = TaskToolWindowView.getInstance(project).currentTask ?: error("Cannot get the current task")
    while (task.name != taskName || task.lesson.name != lessonName) {
      val targetTask = NavigationUtils.nextTask(task) ?: error("The next task is null")
      NavigationUtils.navigateToTask(project, targetTask, task)
      task = TaskToolWindowView.getInstance(project).currentTask ?: error("Cannot get the current task")
    }

    val state = project.eduState ?: error("Edu state is invalid")
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val studentCode = studentSolutions.filter { it.lessonName == lessonName && it.taskName == taskName }.map { it.code }.firstOrNull()
    studentCode?.let {
      downloadSolution(task, project, it)

      runInBackground(project, "Running Tests", true) {
        runBlockingCancellable {
          withContext(Dispatchers.IO) {
            val response = assistant.getHint(task, state)
            response.codeHint?.let {
              downloadSolution(task, project, it)
            }
          }
        }
      }
      myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
      checkTask(task)
    }
  }

  override fun createCourse(): Course = kotlinOnboardingMockCourse
}
