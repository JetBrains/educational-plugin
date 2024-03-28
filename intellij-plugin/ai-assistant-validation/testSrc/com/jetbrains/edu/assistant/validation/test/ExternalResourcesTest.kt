package com.jetbrains.edu.assistant.validation.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.File

abstract class ExternalResourcesTest : JdkCheckerTestBase() {

  override fun setUp() {
    super.setUp()

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

  protected fun refreshProject() {
    myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
  }

  protected fun runCheck(task: Task, assertAction: (CheckResult) -> Unit) {
    val future = ApplicationManager.getApplication().executeOnPooledThread {
      ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(
        project, EduCoreBundle.message("progress.title.checking.solution"), true
      ) {
        private val checker: TaskChecker<*>

        init {
          val configurator = task.course.configurator
          checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project) ?: error("Cannot find test configurator")
        }

        override fun run(indicator: ProgressIndicator) {
          indicator.text = "${EduAndroidAiAssistantValidationBundle.message("test.checking.task.indicator")} ${task.name}"
          val checkerResult = checker.check(indicator)
          assertAction(checkerResult)
          println(checkerResult)
          indicator.text = "${EduAndroidAiAssistantValidationBundle.message("test.check.result.indicator")} $checkerResult"
        }

      })
    }
    EduActionUtils.waitAndDispatchInvocationEvents(future)
  }
}
