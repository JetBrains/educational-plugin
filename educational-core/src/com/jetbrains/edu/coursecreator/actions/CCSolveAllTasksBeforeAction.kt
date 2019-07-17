package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.showSelectTaskDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils

@Suppress("ComponentNotRegistered")
class CCSolveAllTasksBeforeAction : DumbAwareAction("Solve All Tasks Before", "Solves all tasks in course before selected one", null) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (StudyTaskManager.getInstance(project).course !is EduCourse) return
    e.presentation.isEnabledAndVisible = !CCUtils.isCourseCreator(project) &&
                                         CCPluginToggleAction.isCourseCreatorFeaturesEnabled &&
                                         Registry.`is`(REGISTRY_KEY, false)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    val selectedTask = showSelectTaskDialog(project, course) ?: return
    navigateToSelectedTask(project, course, selectedTask)
  }

  private fun navigateToSelectedTask(project: Project, course: EduCourse, selectedTask: Task) {
    val allTasks = course.allTasks
    for ((fromTask, targetTask) in allTasks.windowed(2)) {
      if (fromTask == selectedTask) return
      if (fromTask.status == CheckStatus.Solved) continue
      val lesson = fromTask.lesson
      if (lesson is FrameworkLesson && fromTask.index < lesson.currentTaskIndex) continue
      fillAnswers(project, fromTask)
      fromTask.status = CheckStatus.Solved
      NavigationUtils.navigateToTask(project, targetTask, fromTask, false)
    }
  }

  private fun fillAnswers(project: Project, task: Task) {
    val documentManager = FileDocumentManager.getInstance()
    for ((_, taskFile) in task.taskFiles) {
      val document = taskFile.getDocument(project)
      if (document == null) {
        LOG.error("Failed to find document for `${taskFile.name}`")
        continue
      }
      for (placeholder in taskFile.answerPlaceholders) {
        if (placeholder.isInitializedFromDependency) continue
        val possibleAnswer = placeholder.possibleAnswer
        val start = placeholder.offset
        val end = placeholder.endOffset
        runUndoTransparentWriteAction {
          document.replaceString(start, end, possibleAnswer)
        }
      }
      runWriteAction {
        documentManager.saveDocument(document)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CCSolveAllTasksBeforeAction::class.java)

    const val REGISTRY_KEY = "edu.course.creator.solve.all"
  }
}
