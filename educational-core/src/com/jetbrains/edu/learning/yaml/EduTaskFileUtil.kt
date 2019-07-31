package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.coursecreator.yaml.format.TaskFileBuilder
import com.jetbrains.edu.coursecreator.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.coursecreator.yaml.format.TaskFileYamlMixin.Companion.NAME
import com.jetbrains.edu.coursecreator.yaml.format.TaskFileYamlMixin.Companion.PLACEHOLDERS
import com.jetbrains.edu.coursecreator.yaml.format.TaskFileYamlMixin.Companion.VISIBLE
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

private const val TEXT = "text"

@JsonDeserialize(builder = EduTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS,
                   TEXT)
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
class EduTaskFileYamlMixin : TaskFileYamlMixin() {
  @JsonProperty(TEXT)
  private lateinit var myText: String
}

class EduTaskFileBuilder(
  val text: String?,
  name: String?,
  placeholders: List<AnswerPlaceholder> = mutableListOf(),
  visible: Boolean = true
) : TaskFileBuilder(name, placeholders, visible) {
  override fun createTaskFile(): TaskFile {
    val taskFile = super.createTaskFile()
    taskFile.setText(text)
    return taskFile
  }
}

