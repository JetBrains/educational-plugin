@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getEduEditors
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.editor.EduEditor
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import com.jetbrains.edu.learning.yaml.errorHandling.noDirForItemMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [TaskChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK)
abstract class TaskYamlMixin {
  @JsonProperty(TYPE)
  private fun getItemType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
    throw NotImplementedInMixin()
  }

  @JsonSerialize(converter = FeedbackLinkToStringConverter::class)
  @JsonDeserialize(converter = StringToFeedbackLinkConverter::class)
  @JsonProperty(value = FEEDBACK_LINK, access = JsonProperty.Access.READ_WRITE)
  protected open lateinit var myFeedbackLink: FeedbackLink

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var myCustomPresentableName: String? = null
}

private class FeedbackLinkToStringConverter : StdConverter<FeedbackLink?, String>() {
  override fun convert(value: FeedbackLink?): String? {
    if (value?.link.isNullOrBlank()) {
      return ""
    }

    return value?.link
  }
}

private class StringToFeedbackLinkConverter : StdConverter<String?, FeedbackLink>() {
  override fun convert(value: String?): FeedbackLink {
    if (value == null || value.isBlank()) {
      return FeedbackLink()
    }

    return FeedbackLink(value)
  }
}

class TaskChangeApplier(val project: Project) : StudyItemChangeApplier<Task>() {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    val project = existingItem.project ?: error("Project not found for ${existingItem.name}")
    if (existingItem.itemType != deserializedItem.itemType) {
      changeType(project, existingItem, deserializedItem)
      return
    }
    existingItem.feedbackLink = deserializedItem.feedbackLink
    @Suppress("DEPRECATION") // it's ok as we just copy value of deprecated field
    existingItem.customPresentableName = deserializedItem.customPresentableName
    if (deserializedItem is ChoiceTask && existingItem is ChoiceTask) {
      existingItem.isMultipleChoice = deserializedItem.isMultipleChoice
      existingItem.choiceOptions = deserializedItem.choiceOptions
      existingItem.messageCorrect = deserializedItem.messageCorrect
      existingItem.messageIncorrect = deserializedItem.messageIncorrect
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
    hideOldPlaceholdersForOpenedFiles(project, existingItem)
    existingItem.applyTaskFileChanges(deserializedItem)
    paintPlaceholdersForOpenedFiles(project, existingItem)
  }

  private fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    deserializedItem.name = existingItem.name
    deserializedItem.index = existingItem.index

    val parentItem = (existingItem as Task).lesson
    parentItem.removeItem(existingItem)
    parentItem.addItemAsNew(project, deserializedItem)

    deserializedItem.taskFiles.values.forEach { taskFile ->
      taskFile.getEduEditors(project).forEach {
        PlaceholderPainter.hidePlaceholders(it.taskFile)
        it.taskFile = taskFile
      }
    }
  }

  private fun Task.applyTaskFileChanges(deserializedItem: Task) {
    val orderedTaskFiles = LinkedHashMap<String, TaskFile>()
    for ((name, deserializedTaskFile) in deserializedItem.taskFiles) {
      val existingTaskFile = taskFiles[name]
      val taskFile: TaskFile = if (existingTaskFile != null) {
        existingTaskFile.applyPlaceholderChanges(deserializedTaskFile)
        existingTaskFile.isVisible = deserializedTaskFile.isVisible
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

  private fun TaskFile.applyPlaceholderChanges(deserializedTaskFile: TaskFile) {
    PlaceholderPainter.hidePlaceholders(this)
    answerPlaceholders = deserializedTaskFile.answerPlaceholders
  }

  private fun paintPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedEduEditors(project, task).forEach { PlaceholderPainter.showPlaceholders(project, it.taskFile) }
  }

  private fun hideOldPlaceholdersForOpenedFiles(project: Project, task: Task) {
    getOpenedEduEditors(project, task).forEach { PlaceholderPainter.showPlaceholders(project, it.taskFile) }
  }

  private fun getOpenedEduEditors(project: Project, task: Task): List<EduEditor> {
    val taskDir = task.getDir(project) ?: error(noDirForItemMessage(task.name, EduNames.TASK))
    return FileEditorManager.getInstance(project).openFiles
      .filter { VfsUtil.isAncestor(taskDir, it, true) }
      .map { FileEditorManager.getInstance(project).getSelectedEditor(it) }
      .filterIsInstance(EduEditor::class.java)
  }
}