package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import org.jetbrains.annotations.NonNls

open class TaskChangeApplier(val project: Project) : StudyItemChangeApplier<Task>() {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    @NonNls
    val messageToLog = "Project not found for ${existingItem.name}"
    val project = existingItem.project ?: error(messageToLog)
    if (existingItem.itemType != deserializedItem.itemType) {
      changeType(project, existingItem, deserializedItem)
      return
    }
    existingItem.feedbackLink = deserializedItem.feedbackLink
    @Suppress("DEPRECATION") // it's ok as we just copy value of deprecated field
    existingItem.customPresentableName = deserializedItem.customPresentableName
    existingItem.contentTags = deserializedItem.contentTags
    existingItem.solutionHidden = deserializedItem.solutionHidden
    if (deserializedItem is TheoryTask && existingItem is TheoryTask) {
      existingItem.postSubmissionOnOpen = deserializedItem.postSubmissionOnOpen
    }
    if (deserializedItem is SortingTask && existingItem is SortingTask) {
      existingItem.options = deserializedItem.options
      TaskToolWindowView.getInstance(project).updateTaskDescription()
    }
    if (deserializedItem is MatchingTask && existingItem is MatchingTask) {
      existingItem.options = deserializedItem.options
      existingItem.captions = deserializedItem.captions
      TaskToolWindowView.getInstance(project).updateTaskDescription()
    }
    if (deserializedItem is TableTask && existingItem is TableTask) {
      existingItem.rows = deserializedItem.rows
      existingItem.columns = deserializedItem.columns
      existingItem.isMultipleChoice = deserializedItem.isMultipleChoice
      TaskToolWindowView.getInstance(project).updateTaskDescription()
    }
    if (deserializedItem is ChoiceTask && existingItem is ChoiceTask) {
      existingItem.isMultipleChoice = deserializedItem.isMultipleChoice
      existingItem.choiceOptions = deserializedItem.choiceOptions
      existingItem.messageCorrect = deserializedItem.messageCorrect
      existingItem.messageIncorrect = deserializedItem.messageIncorrect
      existingItem.quizHeader = deserializedItem.quizHeader
      TaskToolWindowView.getInstance(project).updateTaskDescription()
    }
    hideOldPlaceholdersForOpenedFiles(project, existingItem)
    existingItem.applyTaskFileChanges(deserializedItem)
    paintPlaceholdersForOpenedFiles(project, existingItem)
  }

  open fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    val existingTask = existingItem as Task
    hideOldPlaceholdersForOpenedFiles(project, existingTask)

    deserializedItem.name = existingItem.name
    deserializedItem.index = existingItem.index

    val parentItem = existingTask.lesson
    parentItem.removeItem(existingItem)
    parentItem.addItemAsNew(project, deserializedItem)
  }

  private fun Task.applyTaskFileChanges(deserializedItem: Task) {
    val orderedTaskFiles = LinkedHashMap<String, TaskFile>()
    for ((name, deserializedTaskFile) in deserializedItem.taskFiles) {
      val existingTaskFile = taskFiles[name]
      val taskFile: TaskFile = if (existingTaskFile != null) {
        applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
        existingTaskFile
      }
      else {
        deserializedTaskFile
      }
      orderedTaskFiles[name] = taskFile
      deserializedTaskFile.initTaskFile(this, false)
    }
    taskFiles = orderedTaskFiles
  }

  protected open fun applyTaskFileChanges(existingTaskFile: TaskFile,
                                          deserializedTaskFile: TaskFile) {
    existingTaskFile.applyPlaceholderChanges(deserializedTaskFile)
    existingTaskFile.isVisible = deserializedTaskFile.isVisible
    existingTaskFile.isEditable = deserializedTaskFile.isEditable
    existingTaskFile.isPropagatable = deserializedTaskFile.isPropagatable
    existingTaskFile.errorHighlightLevel = deserializedTaskFile.errorHighlightLevel
    existingTaskFile.contents = deserializedTaskFile.contents
  }

  private fun TaskFile.applyPlaceholderChanges(deserializedTaskFile: TaskFile) {
    val oldPlaceholders = answerPlaceholders
    answerPlaceholders = deserializedTaskFile.answerPlaceholders
    PlaceholderHighlightingManager.hidePlaceholders(project, oldPlaceholders)
  }

  private fun paintPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedTaskFiles(project, task).forEach { PlaceholderHighlightingManager.showPlaceholders(project, it) }
  }

  private fun hideOldPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedTaskFiles(project, task).forEach { PlaceholderHighlightingManager.hidePlaceholders(project, it.answerPlaceholders) }
  }

  private fun getOpenedTaskFiles(project: Project, task: Task): List<TaskFile> {
    return FileEditorManager.getInstance(project).openFiles.mapNotNull { it.getTaskFile(project) }.filter { it.task == task }
  }
}