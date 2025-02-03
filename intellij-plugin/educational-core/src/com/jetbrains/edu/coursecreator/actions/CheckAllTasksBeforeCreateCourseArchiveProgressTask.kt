package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.IdeBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages.showYesNoCancelDialog
import com.jetbrains.edu.coursecreator.actions.checkAllTasks.checkAllTasksInItemContainer
import com.jetbrains.edu.coursecreator.actions.checkAllTasks.showFailedTasksNotification
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getInEdt
import com.jetbrains.edu.learning.messages.EduCoreBundle

/**
 * This task checks all the tasks in the course, and if at least one task fails, it asks user
 * whether they want to create the archive anyway.
 *
 * [doIfNeedToCreateTheArchive] is a function that runs in EDT after the task is competed and if
 * the archive needs to be created, either because there are no failing tasks, or because the user
 * wants to create the archive anyway.
 */
class CheckAllTasksBeforeCreateCourseArchiveProgressTask(
  project: Project,
  private val course: Course,
  private val doIfNeedToCreateTheArchive: () -> Unit
): Backgroundable(
  project,
  EduCoreBundle.message("action.create.course.archive.progress.bar"),
  true
) {

  private var failedTasks: List<Task>? = null

  /**
   * Checks all tasks and shows notification if there are any failed
   *
   * @return true if we should create archive next step, false otherwise
   */
  override fun run(indicator: ProgressIndicator) {
    indicator.isIndeterminate = false
    val prevText = indicator.text
    indicator.text = EduCoreBundle.message("progress.title.checking.all.tasks")
    failedTasks = checkAllTasksInItemContainer(project, course, course, indicator)
    indicator.text = prevText
  }

  override fun onSuccess() {
    val failedTasks = failedTasks ?: return

    val needToCreateTheArchive = failedTasks.isEmpty() || when (showFailedTasksDialog(failedTasks.size)) {
      MessageConstants.CANCEL -> false
      MessageConstants.YES -> {
        showFailedTasksNotification(project, failedTasks, failedTasks.size)
        false
      }
      else -> true
    }

    if (needToCreateTheArchive) {
      doIfNeedToCreateTheArchive()
    }
  }

  private fun showFailedTasksDialog(failedTestsNumber: Int): Int = getInEdt {
    showYesNoCancelDialog(
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.message", failedTestsNumber),
      EduCoreBundle.message("error.failed.to.create.course.archive.dialog.title"),
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.show.check.result"),
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.create.anyway"),
      IdeBundle.message("button.cancel"),
      null
    )
  }
}