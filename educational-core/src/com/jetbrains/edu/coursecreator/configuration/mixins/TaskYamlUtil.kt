@file:JvmName("TaskYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.TaskFile

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder("type", "task_files", "feedback_link")
abstract class TaskYamlMixin {
  @JsonProperty("type")
  fun getTaskType(): String {
    throw NotImplementedInMixin()
  }

  @JsonProperty("task_files")
  @JsonSerialize(contentConverter = TaskFileConverter::class)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty("task_files")
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
    throw NotImplementedInMixin()
  }

  @JsonSerialize(converter = FeedbackLinkToStringConverter::class)
  @JsonDeserialize(converter = StringToFeedbackLinkConverter::class)
  @JsonProperty(value = "feedback_link", access = JsonProperty.Access.READ_WRITE) lateinit var myFeedbackLink: FeedbackLink
}

private class TaskFileConverter : StdConverter<TaskFile, TaskFileWithoutPlaceholders>() {
  override fun convert(value: TaskFile): TaskFileWithoutPlaceholders {
    return TaskFileWithoutPlaceholders(value.name)
  }
}

private class TaskFileWithoutPlaceholders(@JsonProperty("name") val name: String)

private class FeedbackLinkToStringConverter: StdConverter<FeedbackLink?, String>() {
  override fun convert(value: FeedbackLink?): String? {
    if (value?.link.isNullOrBlank()) {
      return ""
    }

    return value?.link
  }
}

private class StringToFeedbackLinkConverter: StdConverter<String?, FeedbackLink>() {
  override fun convert(value: String?): FeedbackLink {
    if (value == null || value.isBlank()) {
      return FeedbackLink()
    }

    return FeedbackLink(value)
  }
}