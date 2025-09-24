package com.jetbrains.edu.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.yaml.format.TaskFileBuilder
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.EDITABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HIGHLIGHT_LEVEL
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.IS_BINARY
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROPAGATABLE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TEXT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.VISIBLE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, HIGHLIGHT_LEVEL, TEXT, IS_BINARY, LEARNER_CREATED)
abstract class StudentTaskFileYamlMixin : TaskFileYamlMixin() {

  private val isBinary: Boolean?
    @JsonProperty(IS_BINARY)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IsBinaryFilter::class)
    get() = null

  @JsonProperty(TEXT)
  open fun getTextToSerialize(): String {
    throw NotImplementedError()
  }

  @JsonProperty(LEARNER_CREATED)
  private var isLearnerCreated = false
}

/**
 * We write only is_binary: true, and omit is_binary: false for textual files.
 */
@Suppress("EqualsOrHashCode")
class IsBinaryFilter {
  override fun equals(other: Any?) = other == false || other == null
}

@JsonPOJOBuilder(buildMethodName = "buildTaskFile")
class StudentTaskFileBuilder(
  @param:JsonProperty(TEXT) val textFromConfig: String?,
  @param:Encrypt @param:JsonProperty(ENCRYPTED_TEXT) val encryptedTextFromConfig: String?,
  @param:JsonProperty(LEARNER_CREATED) val learnerCreated: Boolean = false,
  @JsonProperty(NAME) name: String?,
  @JsonProperty(IS_BINARY) isBinary: Boolean? = false,
  @JsonSetter(contentNulls = Nulls.SKIP) @JsonProperty(PLACEHOLDERS) placeholders: List<AnswerPlaceholder> = mutableListOf(),
  @JsonProperty(VISIBLE) visible: Boolean = true,
  @JsonProperty(EDITABLE) editable: Boolean = true,
  @JsonProperty(PROPAGATABLE) propagatable: Boolean = true,
  @JsonProperty(HIGHLIGHT_LEVEL) errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
) : TaskFileBuilder(name, isBinary, placeholders, visible, editable, propagatable, errorHighlightLevel) {
  override fun setupTaskFile(taskFile: TaskFile) {
    super.setupTaskFile(taskFile)

    taskFile.contents = if (isBinary == true) {
      // binary contents are never stored in yaml
      TakeFromStorageBinaryContents
    }
    else {
      // textual contents might be stored by the older versions of the plugin
      val text = encryptedTextFromConfig ?: textFromConfig
      if (text == null) {
        TakeFromStorageTextualContents
      }
      else {
        InMemoryTextualContents(text)
      }
    }

    taskFile.isLearnerCreated = learnerCreated
  }
}

object TakeFromStorageBinaryContents : BinaryContents {
  override val bytes: ByteArray
    get() {
      logger<TakeFromStorageBinaryContents>().warning("This storage is only a marker storage and must not be used")
      return byteArrayOf()
    }
}

object TakeFromStorageTextualContents : TextualContents {
  override val text: String
    get() {
      logger<TakeFromStorageTextualContents>().warning("This storage is only a marker storage and must not be used")
      return ""
    }
}