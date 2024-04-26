package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.message
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HIGHLIGHT_LEVEL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

/**
 * Mixin class is used to deserialize [EduFile] item.
 */
@JsonPropertyOrder(NAME, VISIBLE, EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = EduFileBuilder::class)
abstract class EduFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var name: String
}

@JsonPOJOBuilder(buildMethodName = "buildEduFile", withPrefix = "")
open class EduFileBuilder(
  @JsonProperty(NAME) val name: String?
) {
  @Suppress("unused") //used for deserialization
  fun buildEduFile(): EduFile {
    val eduFile = EduFile()
    setupEduFile(eduFile)
    return eduFile
  }

  protected open fun setupEduFile(eduFile: EduFile) {
    eduFile.name = name ?: formatError(message("yaml.editor.invalid.file.without.name"))
  }
}