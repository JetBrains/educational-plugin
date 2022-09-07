package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.*
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.TimeoutUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.getInEdt
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.threadLocal
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.*
import org.jetbrains.annotations.NonNls
import java.io.File
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCCreateCourseArchive : AnAction(EduCoreBundle.lazyMessage("action.create.course.archive.text")) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    if (course.hasSections && course.hasTopLevelLessons) {
      if (!askToWrapTopLevelLessons(project, (course as EduCourse))) {
        return
      }
    }

    CoroutineScope(Dispatchers.IO).launch {
      val flagCheckAllTasks = checkAllTasks(project, course)
      if (!flagCheckAllTasks) return@launch

      getInEdt {
        createSourceArchive(project, course)
      }
    }
  }

  /**
   * @return true if all tasks were checked successfully, false otherwise
   */
  private fun checkAllTasks(project: Project, course: Course): Boolean {
    val task = CheckAllTasksProgressTask(project, course)
    ProgressManager.getInstance().run(task)

    while (!task.isCancelled && !task.isFinished) { // waiting for background task to complete
      TimeoutUtil.sleep(200)
    }

    return when {
      task.isCancelled -> false
      (task.errorMessage == null) -> true
      else -> {
        getInEdt {
          Messages.showErrorDialog(project, task.errorMessage, EduCoreBundle.message("error.failed.to.create.course.archive"))
        }
        false
      }
    }
  }

  private fun createSourceArchive(project: Project, course: Course) {
    val dlg = CCCreateCourseArchiveDialog(project, course.name)
    if (!dlg.showAndGet()) {
      return
    }

    val locationPath = dlg.locationPath

    val authorName = dlg.authorName
    course.vendor = Vendor(authorName)
    PropertiesComponent.getInstance(project).setValue(AUTHOR_NAME, authorName)

    val errorMessage = createCourseArchive(project, locationPath)
    if (errorMessage == null) {
      showNotification(project, EduCoreBundle.message("action.create.course.archive.success.message"), ShowFileAction(locationPath))
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, locationPath)
      EduCounterUsageCollector.createCourseArchive()
    } else {
      Messages.showErrorDialog(project, errorMessage, EduCoreBundle.message("error.failed.to.create.course.archive"))
    }
  }


  /**
   * @return null when course archive was created successfully, non-empty error message otherwise
   */
  private fun createCourseArchive(project: Project, location: String): String? {
    FileDocumentManager.getInstance().saveAllDocuments()
    return ApplicationManager.getApplication().runWriteAction<String>(CourseArchiveCreator(project, location))
  }

  companion object {
    @NonNls
    const val LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation"

    @NonNls
    const val AUTHOR_NAME = "Edu.Author.Name"
  }

  class ShowFileAction(val path: String) : AnAction(
    EduCoreBundle.message("action.create.course.archive.open.file", RevealFileAction.getFileManagerName())) {
    override fun actionPerformed(e: AnActionEvent) {
      RevealFileAction.openFile(File(path))
    }

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      presentation.isVisible = RevealFileAction.isSupported()
    }
  }
}
