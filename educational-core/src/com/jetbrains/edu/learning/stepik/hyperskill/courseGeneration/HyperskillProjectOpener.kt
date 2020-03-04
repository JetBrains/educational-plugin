package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.ide.RestService

object HyperskillProjectOpener {

  fun openProject(projectId: Int, stageId: Int): Boolean =
    focusOpenProject(projectId, stageId) || openRecentProject(projectId, stageId) || openNewProject(projectId, stageId)

  private fun openRecentProject(courseId: Int, stageId: Int): Boolean {
    val (_, course) = EduBuiltInServerUtils.openRecentProject { it is HyperskillCourse && it.hyperskillProject?.id == courseId }
                      ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    return true
  }

  private fun openNewProject(projectId: Int, stageId: Int): Boolean {
    val hyperskillCourse = getHyperskillCourseUnderProgress(projectId) ?: return false
    runInEdt {
      requestFocus()
      hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
      HyperskillJoinCourseDialog(hyperskillCourse).show()
    }
    return true
  }

  private fun focusOpenProject(courseId: Int, stageId: Int): Boolean {
    val (project, course) = EduBuiltInServerUtils.focusOpenProject { it is HyperskillCourse && it.hyperskillProject?.id == courseId }
                            ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    runInEdt { openSelectedStage(course, project) }
    return true
  }

  fun getHyperskillCourseUnderProgress(projectId: Int): HyperskillCourse? {
    return ProgressManager.getInstance().run(object : Task.WithResult<HyperskillCourse?, Exception>
                                                      (null, "Loading project", true) {
      override fun compute(indicator: ProgressIndicator): HyperskillCourse? {
        val hyperskillProject = HyperskillConnector.getInstance().getProject(projectId) ?: return null

        if (!hyperskillProject.useIde) {
          RestService.LOG.warn("Project in not supported yet $projectId")
          Notification(HYPERSKILL, HYPERSKILL, HYPERSKILL_PROJECT_NOT_SUPPORTED, NotificationType.WARNING,
                       HSHyperlinkListener(false)).notify(project)
          return null
        }
        val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language]
        if (languageId == null) {
          RestService.LOG.warn("Language in not supported yet ${hyperskillProject.language}")
          Notification(HYPERSKILL, HYPERSKILL, "Unsupported language ${hyperskillProject.language}",
                       NotificationType.WARNING).notify(project)
          return null
        }
        val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
        val stages = HyperskillConnector.getInstance().getStages(projectId) ?: return null
        hyperskillCourse.stages = stages
        return hyperskillCourse
      }
    })
  }

  // We have to use visible frame here because project is not yet created
  // See `com.intellij.ide.impl.ProjectUtil.focusProjectWindow` implementation for more details
  fun requestFocus() {
    val frame = WindowManager.getInstance().findVisibleFrame()
    if (frame is IdeFrame) {
      AppIcon.getInstance().requestFocus(frame)
    }
    frame.toFront()
  }
}