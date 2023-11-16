package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getStudyItem
import com.jetbrains.edu.learning.isFeatureEnabled
import org.jetbrains.annotations.NonNls

class CCApplyChangesToNextTasks : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null || !CCUtils.isCourseCreator(project)) return

    val selectedItems = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
    val studyItem = selectedItems?.singleOrNull()?.getStudyItem(project) ?: return
    when {
      studyItem is FrameworkLesson -> {
        val task = studyItem.taskList.firstOrNull() ?: return
        propagateChanges(project, task)
      }
      studyItem.parent is FrameworkLesson -> propagateChanges(project, studyItem as Task)
      else -> error("Apply Changes To Next Tasks action was performed from invalid place")
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    if (project == null || !CCUtils.isCourseCreator(project)) {
      return
    }

    if (!isFeatureEnabled(EduExperimentalFeatures.CC_FL_APPLY_CHANGES)) {
      return
    }

    val selectedItems = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
    if (selectedItems?.size != 1) return

    val studyItem = selectedItems.first().getStudyItem(project) ?: return

    e.presentation.isEnabledAndVisible = studyItem is FrameworkLesson || studyItem.parent is FrameworkLesson
  }

  private fun propagateChanges(project: Project, task: Task) {
    val flManager = CCFrameworkLessonManager.getInstance(project)
    flManager.propagateChanges(task)
  }

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.Educator.ApplyChangesToNextTasks"
  }
}