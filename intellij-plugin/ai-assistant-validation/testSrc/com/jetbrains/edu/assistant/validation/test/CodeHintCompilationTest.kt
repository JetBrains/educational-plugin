package com.jetbrains.edu.assistant.validation.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.assistant.validation.util.parseCsvFile
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.progress.withBackgroundProgress
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
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

  override fun setUp() {
    super.setUp()

    // TODO: move into a new base class for tests with external files
    myCourse.lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>()
    }.map {
      val taskDir = it.getDir(project.courseDir) ?: error("Cannot find a task dir for task ${it.name}")
      File(taskDir.path).walk().forEach { file ->
        if (file.isFile) {
          VfsRootAccess.allowRootAccess(testRootDisposable, file.path)
        }
      }
    }
  }

  // TODO: probably can be also moved into to the same base class with setUp
  private fun refreshProject() {
    myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
  }

  @Test
  fun testCodeHintCompilation() {
    CheckActionListener.setCheckResultVerifier { _, checkResult ->
      TestCase.assertNotSame(checkResult.message, CheckUtils.COMPILATION_FAILED_MESSAGE)
    }

    // TODO: `NavigationUtils.navigateToTask(project, task)` doesn't work with framework lessons
    // TODO: Move into a function
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

      // TODO: cannot replace with runBlockingCancellable because of deadlocks
      runBlocking {
        withBackgroundProgress(project, "Running Tests", false) {
          val response = assistant.getHint(task, state)
          response.codeHint?.let {
            downloadSolution(task, project, it)
          }
        }
      }
      refreshProject()

      // TODO: probably can be also moved into to the same base class with setUp
      val future = ApplicationManager.getApplication().executeOnPooledThread {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
          project, EduCoreBundle.message("progress.title.checking.solution"), true
        ) {
          private val checker: TaskChecker<*>

          init {
            val configurator = task.course.configurator
            checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project) ?: error("Cannot find test configurator")
          }

          override fun run(indicator: ProgressIndicator) {
            val checkerResult = checker.check(indicator)
            // TODO: do something else?
            println(checkerResult)
          }

        })
      }
      EduActionUtils.waitAndDispatchInvocationEvents(future)
    }
  }

  override fun createCourse(): Course = kotlinOnboardingMockCourse
}
