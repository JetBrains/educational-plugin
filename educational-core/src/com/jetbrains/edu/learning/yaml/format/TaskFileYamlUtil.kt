package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.serialization.TrueValueFilter
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>

  @JsonProperty(VISIBLE)
  private var myVisible = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(EDITABLE)
  private var myEditable = true
}

@JsonPOJOBuilder(withPrefix = "")
open class TaskFileBuilder(@JsonProperty(NAME) val name: String?,
                           @JsonProperty(PLACEHOLDERS) val placeholders: List<AnswerPlaceholder> = mutableListOf(),
                           @JsonProperty(VISIBLE) val visible: Boolean = true,
                           @JsonProperty(EDITABLE) val editable: Boolean = true) {
  @Suppress("unused") //used for deserialization
  private fun build(): TaskFile {
    if (name == null) {
      formatError(EduCoreBundle.message("yaml.editor.invalid.file.without.name"))
    }
    return createTaskFile()
  }

  protected open fun createTaskFile(): TaskFile {
    val taskFile = TaskFile()
    taskFile.name = name
    taskFile.answerPlaceholders = placeholders
    taskFile.isVisible = visible
    taskFile.isEditable = editable

    return taskFile
  }
}