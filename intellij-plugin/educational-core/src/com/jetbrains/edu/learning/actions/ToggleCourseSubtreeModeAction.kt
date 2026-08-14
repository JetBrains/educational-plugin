package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.projectView.CourseViewVisibleItems

/**
 * Internal action to try out the course subtree mode.
 *
 * In production, the mode is enabled with the `enable_subtree_view_mode` parameter of the `openCourse` command,
 * see [com.jetbrains.edu.learning.projectView.CourseViewMetadataProcessor].
 */
class ToggleCourseSubtreeModeAction : DumbAwareToggleAction() {

  override fun isSelected(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    return CourseViewVisibleItems.getInstance(project).isSubtreeModeEnabled
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val project = e.project ?: return
    val visibleItems = CourseViewVisibleItems.getInstance(project)
    visibleItems.isSubtreeModeEnabled = state
    if (state) {
      // Otherwise, the learner may end up with an empty Course View
      project.getCurrentTask()?.let { visibleItems.markStudyItemAsVisible(it) }
    }
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.project?.isStudentProject() == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
