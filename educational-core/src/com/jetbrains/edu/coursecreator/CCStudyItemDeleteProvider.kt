package com.jetbrains.edu.coursecreator

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.DeleteProvider
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.getDependentTasks
import com.jetbrains.edu.learning.courseFormat.ext.placeholderDependencies
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.generation.EduGradleUtils
import com.jetbrains.edu.learning.projectView.CourseViewPane
import java.util.*
import kotlin.collections.HashSet

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val studyItem = dataContext.getData(CourseViewPane.STUDY_ITEM) ?: return
    // currently, only gradle projects have module for lessons and tasks
    // so we can skip other projects
    val module = if (EduGradleUtils.isConfiguredWithGradle(project)) dataContext.getData(LangDataKeys.MODULE_CONTEXT) else null
    val itemType = when (studyItem) {
      is Section -> "Section"
      is Lesson -> "Lesson"
      is Task -> "Task"
      else -> return
    }

    val (containingTasks, dependentTasks) = when (studyItem) {
      is Section -> {
        val allTasks = studyItem.lessons.flatMapTo(HashSet(), Lesson::getTaskList)
        allTasks to allTasks.flatMapTo(HashSet(), Task::getDependentTasks) - allTasks
      }
      is Lesson -> {
        val allTasks = studyItem.getTaskList().toSet()
        allTasks to allTasks.flatMapTo(HashSet(), Task::getDependentTasks) - allTasks
      }
      is Task -> setOf(studyItem) to studyItem.getDependentTasks()
      else -> emptySet<Task>() to emptySet<Task>()
    }

    val title = IdeBundle.message("prompt.delete.elements", itemType)
    val message = buildString {
      append(IdeBundle.message("warning.delete.all.files.and.subdirectories", studyItem.name))
      if (dependentTasks.isNotEmpty()) {
        // TODO: show dependent task in more convenient way. See https://youtrack.jetbrains.com/issue/EDU-1465
        appendln()
        appendln("Note, all placeholder dependencies on removing items will be deleted as well.")
        appendln("Dependent tasks:")
        appendln()
        for (task in dependentTasks) {
          appendln("• ${taskMessageName(task)}")
        }
      }
    }

    val result = Messages.showOkCancelDialog(project, message, title, Messages.getQuestionIcon())
    if (result != Messages.OK) return

    removeDependentPlaceholders(project, dependentTasks, containingTasks)

    val modifiableModel = ModuleManager.getInstance(project).modifiableModel
    if (module != null) {
      ModuleDeleteProvider.removeModule(module, Collections.emptyList(), modifiableModel)
    }

    runWriteAction {
      modifiableModel.commit()
      CommandProcessor.getInstance().executeCommand(project, {
        virtualFile.delete(CCStudyItemDeleteProvider::class.java)
      }, "", Object())
    }
  }

  private fun removeDependentPlaceholders(project: Project,
                                          dependentTasks: Set<Task>,
                                          containingTasks: Set<Task>) {
    val course = StudyTaskManager.getInstance(project).course
    if (course != null) {
      for (task in dependentTasks) {
        for (dependency in task.placeholderDependencies) {
          if (dependency.resolve(course)?.taskFile?.task in containingTasks) {
            dependency.answerPlaceholder.placeholderDependency = null
          }
        }
      }
    }
  }

  companion object {

    @VisibleForTesting
    fun taskMessageName(task: Task): String {
      val section = task.lesson.section?.name?.let { "$it#" } ?: ""
      return "$section${task.lesson.name}#${task.name}"
    }
  }
}
