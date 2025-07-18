package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.archive.showNotification
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.runInBackground
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class CCUpdateCoursePreview : DumbAwareAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = UpdatePreviewActionInfo.fromEvent(e) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val (previewProject, previewCourse, ccProject, ccCourse, previewInfo) = UpdatePreviewActionInfo.fromEvent(e) ?: return

    val previewPath = previewInfo.previewLoadedFrom
    if (!recreatePreview(ccProject, previewPath, ccCourse)) return

    val remoteCourse = EduUtilsKt.getLocalCourse(previewPath.absolutePathString())
    if (remoteCourse !is EduCourse) {
      EduNotificationManager.showErrorNotification(
        previewProject,
        EduCoreBundle.message("action.update.preview.error.title"),
        EduCoreBundle.message("action.update.preview.error.loading.failed")
      )
      return
    }
    remoteCourse.init(false)

    updateFromPreview(previewProject, previewCourse, remoteCourse)
  }

  private fun recreatePreview(ccProject: Project, previewPath: Path, ccCourse: Course): Boolean {
    val error = CourseArchiveCreator(ccProject, previewPath).createArchive(ccCourse)
    if (error != null) {
      error.showNotification(ccProject, EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
      return false
    }
    return true
  }

  private fun updateFromPreview(project: Project, localCourse: EduCourse, remoteCourse: EduCourse) {
    runInBackground(title = EduCoreBundle.message("progress.loading.course")) {
      val marketplaceCourseUpdater = MarketplaceCourseUpdater(project, localCourse, remoteCourse.marketplaceCourseVersion)
      marketplaceCourseUpdater.updateCourseWithRemote(remoteCourse)
    }
  }
}

data class PreviewInfo(
  /**
   * The path to an archive, from which the preview was loaded
   */
  val previewLoadedFrom: Path,

  /**
   * The base path of the project, from which the preview was created
   */
  val sourceProjectBasePath: String?
) {
  fun findSourceProject(): Project? = ProjectManager.getInstance().openProjects.find {
    it.basePath == sourceProjectBasePath
  }
}

private data class UpdatePreviewActionInfo(
  val previewProject: Project,
  val previewCourse: EduCourse,
  val ccProject: Project,
  val ccCourse: EduCourse,
  val previewInfo: PreviewInfo
) {
  companion object {
    fun fromEvent(e: AnActionEvent): UpdatePreviewActionInfo? {
      val project = e.project ?: return null
      val course = project.course as? EduCourse ?: return null

      if (!course.isStudy) return null
      if (!course.isPreview) return null

      val previewInfo = project.previewInfo ?: return null
      val ccProject = previewInfo.findSourceProject() ?: return null
      val ccCourse = ccProject.course as? EduCourse ?: return null

      return UpdatePreviewActionInfo(project, course, ccProject, ccCourse, previewInfo)
    }
  }
}

private val previewInfoKey = Key<PreviewInfo>("Edu.course.preview.info")

var Project.previewInfo: PreviewInfo?
  get() = getUserData(previewInfoKey)
  set(value) = putUserData(previewInfoKey, value)