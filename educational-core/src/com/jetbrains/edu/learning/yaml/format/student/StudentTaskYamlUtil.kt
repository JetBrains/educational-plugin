package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.errorHandling.YamlLoadingException
import com.jetbrains.edu.learning.yaml.format.TaskChangeApplier
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, RECORD)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(RECORD)
  protected open var myRecord: Int = -1

  companion object {
    const val STATUS = "status"
    const val RECORD = "record"
  }
}

class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    super.applyChanges(existingItem, deserializedItem)
    if (existingItem.status != deserializedItem.status && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException("Status can't be changed")
    }
    when (existingItem) {
      is CheckiOMission -> {
        existingItem.code = (deserializedItem as CheckiOMission).code
        existingItem.secondsFromLastChangeOnServer = deserializedItem.secondsFromLastChangeOnServer
      }
      is ChoiceTask -> {
        existingItem.record = deserializedItem.record
        existingItem.selectedVariants = (deserializedItem as ChoiceTask).selectedVariants
      }
      is EduTask -> {
        existingItem.record = deserializedItem.record
      }
    }
  }

  override fun applyTaskFileChanges(existingTaskFile: TaskFile, deserializedTaskFile: TaskFile) {
    super.applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
    existingTaskFile.setText(deserializedTaskFile.text)
  }

  override fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    throw YamlLoadingException("It's not allowed to change task type")
  }
}