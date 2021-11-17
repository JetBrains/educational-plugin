package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.yaml.errorHandling.YamlLoadingException
import com.jetbrains.edu.learning.yaml.format.TaskChangeApplier
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ACTUAL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EXPECTED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.MESSAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.RECORD
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.STATUS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TIME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TYPE
import java.util.*

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, TAGS)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  @JsonProperty(STATUS)
  private lateinit var myStatus: CheckStatus

  @JsonProperty(FEEDBACK)
  private lateinit var myFeedback: CheckFeedback

  @JsonProperty(RECORD)
  protected open var myRecord: Int = -1
}

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(MESSAGE, TIME, EXPECTED, ACTUAL)
abstract class FeedbackYamlMixin {
  @JsonProperty(MESSAGE)
  private var message: String = ""

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(TIME)
  private var time: Date? = null

  @JsonProperty(EXPECTED)
  private var expected: String? = null

  @JsonProperty(ACTUAL)
  private var actual: String? = null
}

class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    if (existingItem.solutionHidden != deserializedItem.solutionHidden && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException("Visibility of solution can't be changed")
    }
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
        if (existingItem is RemoteEduTask) {
          existingItem.checkProfile = (deserializedItem as RemoteEduTask).checkProfile
        }
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