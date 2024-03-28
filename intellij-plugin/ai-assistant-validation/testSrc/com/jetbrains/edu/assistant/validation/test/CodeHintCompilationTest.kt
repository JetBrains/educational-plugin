package com.jetbrains.edu.assistant.validation.test

import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.assistant.validation.util.parseCsvFile
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import junit.framework.TestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.io.path.Path
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking

@RunWith(Parameterized::class)
class CodeHintCompilationTest(private val lessonName: String, private val taskName: String) : ExternalResourcesTest() {

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

  // TODO: `NavigationUtils.navigateToTask(project, task)` doesn't work with framework lessons
  private fun getTargetTask(): Task =
    generateSequence(TaskToolWindowView.getInstance(project).currentTask) {
      val nextTask = NavigationUtils.nextTask(it) ?: error("The next task is null")
      NavigationUtils.navigateToTask(project, nextTask, it)
      TaskToolWindowView.getInstance(project).currentTask
    }.firstOrNull { it.name == taskName && it.lesson.name == lessonName } ?: error("Cannot get the target task")

  private fun applyCodeHint(task: Task, state: EduState, assistant: TaskBasedAssistant) {
    // TODO: cannot replace with runBlockingCancellable because of deadlocks
    runBlocking {
      withBackgroundProgress(project, "Running Tests", false) {
        val response = assistant.getHint(task, state)
        response.codeHint?.let {
          downloadSolution(task, project, it)
        }
      }
    }
  }

  @Test
  fun testCodeHintCompilation() {
    val task = getTargetTask()
    val state = project.eduState ?: error("Edu state is invalid")
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    studentSolutions.filter { it.lessonName == lessonName && it.taskName == taskName }.map { it.code }.firstOrNull()?.let {
      downloadSolution(task, project, it)
      applyCodeHint(task, state, assistant)
      refreshProject()
      runCheck(task) { checkerResult ->
        TestCase.assertNotSame(checkerResult.message, CheckUtils.COMPILATION_FAILED_MESSAGE)
      }
    }
  }

  override fun createCourse(): Course = kotlinOnboardingMockCourse
}
