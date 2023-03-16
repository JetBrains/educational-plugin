@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import org.jetbrains.annotations.NonNls

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [TaskChangeApplier] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, SOLUTION_HIDDEN, TAGS)
abstract class TaskYamlMixin {
  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(FILES)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
    throw NotImplementedInMixin()
  }

  @JsonProperty(value = FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected open lateinit var feedbackLink: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>
}

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
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
    if (deserializedItem is MatchingTask && existingItem is MatchingTask) {
      existingItem.options = deserializedItem.options
      existingItem.captions = deserializedItem.captions
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
    if (deserializedItem is ChoiceTask && existingItem is ChoiceTask) {
      existingItem.isMultipleChoice = deserializedItem.isMultipleChoice
      existingItem.choiceOptions = deserializedItem.choiceOptions
      existingItem.messageCorrect = deserializedItem.messageCorrect
      existingItem.messageIncorrect = deserializedItem.messageIncorrect
      existingItem.quizHeader = deserializedItem.quizHeader
      TaskDescriptionView.getInstance(project).updateTaskDescription()
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