package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.AppIcon
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.map
import com.jetbrains.edu.learning.stepik.builtInServer.EduBuiltInServerUtils
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

object HyperskillProjectOpener {

  fun openProject(projectId: Int, stageId: Int?): Result<Unit, String> {
    if (focusOpenProject(projectId, stageId)) return Ok(Unit)
    if (openRecentProject(projectId, stageId)) return Ok(Unit)
    return openNewProject(projectId, stageId)
  }

  private fun openRecentProject(courseId: Int, stageId: Int?): Boolean {
    val (_, course) = EduBuiltInServerUtils.openRecentProject { it is HyperskillCourse && it.hyperskillProject?.id == courseId }
                      ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    return true
  }

  private fun openNewProject(projectId: Int, stageId: Int?): Result<Unit, String> {
    return getHyperskillCourseUnderProgress(projectId).map { hyperskillCourse ->
      runInEdt {
        requestFocus()
        hyperskillCourse.putUserData(HYPERSKILL_STAGE, stageId)
        HyperskillJoinCourseDialog(hyperskillCourse).show()
      }
    }
  }

  private fun focusOpenProject(courseId: Int, stageId: Int?): Boolean {
    val (project, course) = EduBuiltInServerUtils.focusOpenProject { it is HyperskillCourse && it.hyperskillProject?.id == courseId }
                            ?: return false
    course.putUserData(HYPERSKILL_STAGE, stageId)
    runInEdt { openSelectedStage(course, project) }
    return true
  }

  fun getHyperskillCourseUnderProgress(projectId: Int, withStages: Boolean = true): Result<HyperskillCourse, String> {
    return ProgressManager.getInstance().run(object : Task.WithResult<Result<HyperskillCourse, String>, Exception>
                                                      (null, "Loading project", true) {
      override fun compute(indicator: ProgressIndicator): Result<HyperskillCourse, String> {
        val hyperskillProject = HyperskillConnector.getInstance().getProject(projectId) ?: return Err(FAILED_TO_CREATE_PROJECT)

        if (!hyperskillProject.useIde) {
          return Err(HYPERSKILL_PROJECT_NOT_SUPPORTED)
        }
        val languageId = HYPERSKILL_LANGUAGES[hyperskillProject.language]
        if (languageId == null) {
          return Err("Unsupported language ${hyperskillProject.language}")
        }
        val hyperskillCourse = HyperskillCourse(hyperskillProject, languageId)
        if (hyperskillCourse.configurator == null) {
          return Err("The project isn't supported (language: ${hyperskillProject.language}). " +
                     "Check if all needed plugins are installed and enabled")
        }
        if (withStages) {
          val stages = HyperskillConnector.getInstance().getStages(projectId) ?: return Err(FAILED_TO_CREATE_PROJECT)
          hyperskillCourse.stages = stages
        }
        return Ok(hyperskillCourse)
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