package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.VISIBLE
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(VISIBLE)
  private var myVisible = true
}

@JsonPOJOBuilder(withPrefix = "")
open class TaskFileBuilder(@JsonProperty(NAME) val name: String?,
                           @JsonProperty(PLACEHOLDERS) val placeholders: List<AnswerPlaceholder> = mutableListOf(),
                           @JsonProperty(VISIBLE) val visible: Boolean = true) {
  @Suppress("unused") //used for deserialization
  private fun build(): TaskFile {
    if (name == null) {
      formatError("File without a name not allowed")
    }
    return createTaskFile()
  }

  protected open fun createTaskFile(): TaskFile {
    val taskFile = TaskFile()
    taskFile.name = name
    taskFile.answerPlaceholders = placeholders
    taskFile.isVisible = visible

    return taskFile
  }
}