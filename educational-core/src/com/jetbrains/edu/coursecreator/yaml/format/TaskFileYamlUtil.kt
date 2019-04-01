package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.TaskFile

private const val NAME = "name"
private const val PLACEHOLDERS = "placeholders"

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
abstract class TaskFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var myName: String

  @JsonProperty(PLACEHOLDERS)
  private lateinit var myAnswerPlaceholders: List<AnswerPlaceholder>
}