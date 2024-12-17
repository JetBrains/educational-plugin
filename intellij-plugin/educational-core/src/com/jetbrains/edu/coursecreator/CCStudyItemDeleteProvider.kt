package com.jetbrains.edu.coursecreator

import com.intellij.ide.DeleteProvider
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.actions.ModuleDeleteProvider
import com.intellij.openapi.ui.Messages.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.getDependentTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.placeholderDependencies
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseViewPane
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.VisibleForTesting
import java.util.*

class CCStudyItemDeleteProvider : DeleteProvider {

  override fun canDeleteElement(dataContext: DataContext): Boolean = dataContext.getData(CourseViewPane.STUDY_ITEM) != null

  override fun deleteElement(dataContext: DataContext) {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
    val studyItem = dataContext.getData(CourseViewPane.STUDY_ITEM) ?: return
    // currently, only gradle projects have module for study items
    val module = dataContext.getData(LangDataKeys.MODULE_CONTEXT)
    val itemType = when (studyItem) {
      is Section -> EduCoreBundle.message("item.section.title")
      is Lesson -> EduCoreBundle.message("item.lesson.title")
      is Task -> EduCoreBundle.message("item.task.title")
      else -> return
    }

    val (containingTasks, dependentTasks) = when (studyItem) {
      is Section -> {
        val allTasks = studyItem.lessons.flatMapTo(HashSet(), Lesson::taskList)
        allTasks to allTasks.flatMapTo(HashSet(), Task::getDependentTasks) - allTasks
      }
      is Lesson -> {
        val allTasks = studyItem.taskList.toSet()
        allTasks to allTasks.flatMapTo(HashSet(), Task::getDependentTasks) - allTasks
      }
      is Task -> setOf(studyItem) to studyItem.getDependentTasks()
      else -> emptySet<Task>() to emptySet()
    }

    val title = IdeBundle.message("prompt.delete.elements", itemType)
    val message = getDeleteItemsDialogMessage(dependentTasks,
                                              IdeBundle.message("warning.delete.all.files.and.subdirectories", studyItem.name),
                                              EduCoreBundle.message("course.creator.warning.removing.dependencies"),
                                              "${EduCoreBundle.message("course.creator.warning.dependent.tasks")}:")

    val result = showOkCancelDialog(project, message, title, getOkButton(), getCancelButton(), getQuestionIcon())
    if (result != OK) return

    removeDependentPlaceholders(project, dependentTasks, containingTasks)

    val modifiableModel = ModuleManager.getInstance(project).getModifiableModel()
    if (module != null) {
      ModuleDeleteProvider.removeModule(module, Collections.emptyList(), modifiableModel)
    }

    runWriteAction {
      modifiableModel.commit()
      CommandProcessor.getInstance().executeCommand(project, {
        virtualFile.delete(CCStudyItemDeleteProvider::class.java)

        val parent = studyItem.parent
        val parentDir = parent.getDir(project.courseDir) ?: return@executeCommand
        CCUtils.updateHigherElements(parentDir.children, { parent.getItem(it.name) }, studyItem.index, -1)
      }, "", Object())
    }
  }

  private fun getDeleteItemsDialogMessage(
    dependentTasks: Set<Task>,
    allItemsDeletedWarning: @Nls String,
    removingPlaceholderDependenciesWarning: @Nls String,
    dependentTasksWarning: @Nls String
  ): String {
    return buildString {
      append(allItemsDeletedWarning)
      if (dependentTasks.isNotEmpty()) {
        // TODO: show dependent task in more convenient way. See https://youtrack.jetbrains.com/issue/EDU-1465
        appendLine()
        appendLine(removingPlaceholderDependenciesWarning)
        appendLine(dependentTasksWarning)
        appendLine()
        for (task in dependentTasks) {
          appendLine("• ${taskMessageName(task)}")
        }
      }
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

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {

    @VisibleForTesting
    fun taskMessageName(task: Task): String {
      val section = task.lesson.section?.name?.let { "$it#" } ?: ""
      return "$section${task.lesson.name}#${task.name}"
    }
  }
}
