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
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls

class CCSolveAllTasksBeforeAction : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.solve.all.tasks.before.text"),
  EduCoreBundle.lazyMessage("action.solve.all.tasks.before.description"),
  null
) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (StudyTaskManager.getInstance(project).course !is EduCourse) return
    e.presentation.isEnabledAndVisible = !CCUtils.isCourseCreator(project) &&
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
      YamlFormatSynchronizer.saveItem(selectedTask)
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
        runUndoTransparentWriteAction {
          document.replaceString(placeholder.offset, placeholder.endOffset, placeholder.possibleAnswer)
        }
      }
      runWriteAction {
        documentManager.saveDocument(document)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(CCSolveAllTasksBeforeAction::class.java)

    @NonNls
    const val REGISTRY_KEY = "edu.course.creator.solve.all"

    @NonNls
    const val ACTION_ID = "Educational.SolveAllTasksBefore"
  }
}
