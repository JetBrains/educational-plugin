package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.yaml.format.EduFileBuilder
import com.jetbrains.edu.learning.yaml.format.EduFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_BINARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentEduFileBuilder::class)
@JsonPropertyOrder(NAME, IS_BINARY)
abstract class StudentEduFileYamlMixin : EduFileYamlMixin() {

  private val isBinary: Boolean?
    @JsonProperty(IS_BINARY)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IsBinaryFilter::class)
    get() = null
}

class StudentEduFileBuilder(
  @JsonProperty(IS_BINARY) val isBinary: Boolean? = false,
  name: String?
) : EduFileBuilder(name) {
  override fun setupEduFile(eduFile: EduFile) {
    super.setupEduFile(eduFile)

    eduFile.contents = if (isBinary == true) {
      TakeFromStorageBinaryContents
    }
    else {
      TakeFromStorageTextualContents
    }
  }
}