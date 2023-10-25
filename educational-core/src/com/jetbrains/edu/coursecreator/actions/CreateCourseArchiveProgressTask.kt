package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageConstants
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.showYesNoCancelDialog
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction.Companion.AUTHOR_NAME
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveAction.Companion.LAST_ARCHIVE_LOCATION
import com.jetbrains.edu.coursecreator.actions.checkAllTasks.checkAllTasksInItemContainer
import com.jetbrains.edu.coursecreator.actions.checkAllTasks.createFailedTasksNotification
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.getInEdt
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.io.File

class CreateCourseArchiveProgressTask(
  project: Project,
  private val course: Course,
): Task.Backgroundable(
  project,
  EduCoreBundle.message("action.create.course.archive.progress.bar"),
  true
) {
  override fun run(indicator: ProgressIndicator) {
    val (locationPath, authorName, checkAllTasksFlag) = showCourseArchiveDialog(project, course) ?: return
    if (checkAllTasksFlag && !checkAllTasksAndShowNotificationIfNeeded(indicator)) {
      return
    }
    getInEdt {
      indicator.isIndeterminate = false
      createSourceArchive(project, course, locationPath, authorName)
    }
  }

  /**
   * Checks all tasks and shows notification if there are any failed
   *
   * @return true if we should create archive next step, false otherwise
   */
  private fun checkAllTasksAndShowNotificationIfNeeded(indicator: ProgressIndicator): Boolean {
    val prevText = indicator.text
    indicator.text = EduCoreBundle.message("progress.title.checking.all.tasks")
    val failedTasks = checkAllTasksInItemContainer(project, course, course, indicator)
    indicator.text = prevText

    if (failedTasks == null) {
      return false
    }

    if (failedTasks.isEmpty()) {
      return true
    }

    return when (showFailedTasksDialog(failedTasks.size)) {
      MessageConstants.CANCEL -> false
      MessageConstants.YES -> {
        val notification = createFailedTasksNotification(failedTasks, failedTasks.size, project)
        Notifications.Bus.notify(notification, project)
        false
      }
      else -> true
    }
  }

  private fun showFailedTasksDialog(failedTestsNumber: Int): Int = getInEdt {
    showYesNoCancelDialog(
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.message", failedTestsNumber),
      EduCoreBundle.message("error.failed.to.create.course.archive"),
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.show.check.result"),
      EduCoreBundle.message("course.creator.create.archive.failed.task.checking.create.anyway"),
      IdeBundle.message("button.cancel"),
      null
    )
  }

  private fun showCourseArchiveDialog(project: Project, course: Course): Triple<String, String, Boolean>? = getInEdt {
    val dialog = CCCreateCourseArchiveDialog(project, course.name)
    if (!dialog.showAndGet()) {
      return@getInEdt null
    }
    Triple(dialog.locationPath, dialog.authorName, dialog.checkAllTasksFlag)
  }

  private fun createSourceArchive(project: Project, course: Course, locationPath: String, authorName: String) {
    course.vendor = Vendor(authorName)
    PropertiesComponent.getInstance(project).setValue(AUTHOR_NAME, authorName)

    val errorMessage = CourseArchiveCreator(project, locationPath).createArchive()
    if (errorMessage == null) {
      CCNotificationUtils.showNotification(
        project,
        EduCoreBundle.message("action.create.course.archive.success.message"),
        ShowFileAction(locationPath)
      )
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, locationPath)
      EduCounterUsageCollector.createCourseArchive()
    } else {
      Messages.showErrorDialog(project, errorMessage, EduCoreBundle.message("error.failed.to.create.course.archive"))
    }
  }

  class ShowFileAction(val path: String) : AnAction(
    EduCoreBundle.message("action.create.course.archive.open.file", RevealFileAction.getFileManagerName())
  ) {
    override fun actionPerformed(e: AnActionEvent) {
      RevealFileAction.openFile(File(path))
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      presentation.isVisible = RevealFileAction.isSupported()
    }
  }
}