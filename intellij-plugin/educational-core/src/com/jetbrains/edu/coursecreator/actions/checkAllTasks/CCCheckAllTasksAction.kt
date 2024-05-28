package com.jetbrains.edu.coursecreator.actions.checkAllTasks

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.getStudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.NonNls

class CCCheckAllTasksAction : AnAction(EduCoreBundle.lazyMessage("action.check.tasks.text")) {
  private class CheckAllTasksProgressTask(
    project: Project,
    private val course: Course,
    private val studyItems: List<StudyItem>,
  ) : Task.Backgroundable(
    project,
    EduCoreBundle.message("progress.title.checking.tasks"),
    true) {
    override fun run(indicator: ProgressIndicator) {
      val failedTasks = checkAllStudyItems(project, course, studyItems, indicator) ?: return
      if (failedTasks.isEmpty()) {
        EduNotificationManager.showInfoNotification(
          project,
          EduCoreBundle.message("notification.title.check.finished"),
          EduCoreBundle.message("notification.content.all.tasks.solved.correctly"),
        )
      }
      else {
        val tasksNum = getNumberOfTasks(studyItems)
        showFailedTasksNotification(project, failedTasks, tasksNum)
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return

    val studyItems = selectedFiles.mapNotNull { it.getStudyItem(project) }.toList()
    if (studyItems.isEmpty()) return

    ProgressManager.getInstance().run(CheckAllTasksProgressTask(project, course, studyItems))
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT


  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = CCUtils.isCourseCreator(project)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.CheckAllTasks"
  }
}