package com.jetbrains.edu.coursecreator.actions.updatetester

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.archive.showNotification
import com.jetbrains.edu.learning.EduUtilsKt.getLocalCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class CCUpdateCoursePreview : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course ?: return
    if (!course.isStudy) return
    if (!course.isPreview) return
    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    recreatePreview(project)
  }

  private fun recreatePreview(project: Project) {
    val previewArchiveManager = PreviewArchiveManager.getInstance(project)

    val ccProject = previewArchiveManager.findSourceProject()
    if (ccProject == null) {
      EduNotificationManager.create(
        NotificationType.ERROR,
        EduCoreBundle.message("action.update.preview.error.title"),
        EduCoreBundle.message("action.update.preview.error.failed.to.find.source.project")
      )
      return
    }

    val previewPath = previewArchiveManager.previewLoadedFrom
    if (previewPath == null) {
      EduNotificationManager.create(
        NotificationType.ERROR,
        EduCoreBundle.message("action.update.preview.error.title"),
        EduCoreBundle.message("action.update.preview.error.no.preview.path")
      )
      return
    }

    val ccCourse = ccProject.course
    if (ccCourse == null) {
      EduNotificationManager.create(
        NotificationType.ERROR,
        EduCoreBundle.message("action.update.preview.error.title"),
        EduCoreBundle.message("action.update.preview.error.source.course")
      )
      return
    }

    if (!doRecreatePreview(ccProject, previewPath, ccCourse)) return

    val remoteCourse = getLocalCourse(previewPath.absolutePathString())
    if (remoteCourse as? EduCourse == null) {
      EduNotificationManager.create(
        NotificationType.ERROR,
        EduCoreBundle.message("action.update.preview.error.title"),
        EduCoreBundle.message("action.update.preview.error.loading.failed")
      )
      return
    }
    remoteCourse.init(false)

    previewArchiveManager.updateFromPreview(remoteCourse)
  }

  private fun doRecreatePreview(ccProject: Project, previewPath: Path, ccCourse: Course): Boolean {
    val error = CourseArchiveCreator(ccProject, previewPath).createArchive(ccCourse)
    if (error != null) {
      error.showNotification(ccProject, EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
      return false
    }
    return true
  }
}