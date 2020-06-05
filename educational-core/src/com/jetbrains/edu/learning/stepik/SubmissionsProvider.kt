package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory

abstract class SubmissionsProvider {

  fun prepareSubmissionsContent(project: Project, course: Course): ActionCallback {
    val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    if (window != null) {
      val submissionsContent = window.contentManager.findContent(SubmissionsManager.SUBMISSIONS_TAB_NAME)
      if (submissionsContent != null) {
        val submissionsPanel = submissionsContent.component
        if (submissionsPanel is AdditionalTabPanel) {
          ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel(getPlatformName()) }
        }
      }
    }
    val actionCallback = ActionCallback()
    loadAllSubmissions(project, course) { actionCallback.setDone() }
    return actionCallback
  }

  fun getSubmissions(stepId: Int, submissionsManager: SubmissionsManager): List<Submission> {
    return submissionsManager.getOrPut(stepId) { loadSubmissions(stepId, submissionsManager) }
  }

  abstract fun getSubmissions(stepIds: Set<Int>, submissionsManager: SubmissionsManager): List<Submission>?

  abstract fun loadAllSubmissions(project: Project, course: Course?, onFinish: () -> Unit)

  abstract fun loadSubmissions(stepId: Int, submissionsManager: SubmissionsManager): List<Submission>

  abstract fun submissionsCanBeShown(course: Course?): Boolean

  abstract fun isLoggedIn(): Boolean

  abstract fun getPlatformName(): String

  abstract fun doAuthorize()

  companion object {
    private val EP_NAME = ExtensionPointName.create<SubmissionsProvider>("Educational.submissionsProvider")

    @JvmStatic
    fun getSubmissionsProviderForCourse(course: Course?): SubmissionsProvider? {
      if (course == null) return null
      val submissionsProviders = EP_NAME.extensionList.filter { it.submissionsCanBeShown(course) }
      if (submissionsProviders.isEmpty()) {
        return null
      }
      if (submissionsProviders.size > 1) {
        error("Several submissionsProviders available for ${course.name}: $submissionsProviders")
      }
      return submissionsProviders[0]
    }
  }
}