package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCCheckAllTasksAction : AnAction(EduCoreBundle.lazyMessage("action.check.all.tasks.text")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    ProgressManager.getInstance().run(CheckAllTasksProgressTask(project, course))
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(project)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.CheckAllTasks"
  }
}