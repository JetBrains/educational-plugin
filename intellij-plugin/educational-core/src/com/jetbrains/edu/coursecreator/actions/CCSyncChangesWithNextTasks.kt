package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCSyncChangesWithNextTasks : DumbAwareAction() {
  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project == null || !CCUtils.isCourseCreator(project)) return

    when (val context = parseSelectedItems(project, e)) {
      is TaskContext -> propagateChanges(project, context.task)
      is LessonContext -> {
        val task = context.lesson.taskList.firstOrNull() ?: return
        propagateChanges(project, task)
      }
      is TaskFilesContext -> propagateChanges(project, context.task, context.files)
      null -> error("SyncChangesWithNextTasks action was performed from invalid place")
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

    val context = parseSelectedItems(project, e)
    if (context != null) {
      val (actionText, actionDescription) = when {
        context is LessonContext -> {
          EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Lesson.text") to EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.Lesson.description")
        }
        context is TaskFilesContext && context.files.size == 1 -> {
          EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.SingleFile.text") to EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.SingleFile.description")
        }
        else -> {
          EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.SeveralFiles.text") to EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.SeveralFiles.description")
        }
      }
      presentation.apply {
        text = actionText
        description = actionDescription
        isEnabledAndVisible = true
      }
    }
  }

  private fun propagateChanges(project: Project, task: Task, files: List<TaskFile>? = null) {
    val flManager = CCFrameworkLessonManager.getInstance(project)
    flManager.propagateChanges(task, files)
  }

  private fun parseSelectedItems(project: Project, e: AnActionEvent): SelectedContext? {
    val selectedItems = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext) ?: return null
    val (studyItems, otherFiles) = selectedItems.partition { it.getStudyItem(project) != null }

    return when {
      studyItems.isNotEmpty() && otherFiles.isNotEmpty() -> null
      studyItems.isNotEmpty() -> {
        val studyItem = selectedItems.singleOrNull()?.getStudyItem(project)

        when {
          studyItem is FrameworkLesson -> LessonContext(studyItem)
          studyItem is Task && studyItem.parent is FrameworkLesson -> TaskContext(studyItem)
          else -> null
        }
      }
      otherFiles.isNotEmpty() -> {
        val taskFiles = otherFiles.mapNotNull { it.getTaskFile(project) }

        // It means that some selected files are not task files. Do nothing in this case
        if (taskFiles.size < otherFiles.size) return null

        val task = taskFiles.map { it.task }.distinct().singleOrNull() ?: return null

        TaskFilesContext(task, taskFiles)
      }
      else -> null
    }
  }

  private sealed interface SelectedContext
  private class TaskContext(val task: Task) : SelectedContext
  private class LessonContext(val lesson: FrameworkLesson) : SelectedContext
  private class TaskFilesContext(val task: Task, val files: List<TaskFile>) : SelectedContext

  companion object {
    const val ACTION_ID: @NonNls String = "Educational.Educator.SyncChangesWithNextTasks"
  }
}