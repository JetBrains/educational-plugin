@file:JvmName("SectionYamlUtil")

package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.errorHandling.formatError
import com.jetbrains.edu.learning.yaml.errorHandling.unnamedItemAtMessage
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.TAGS

/**
 * Mixin class is used to deserialize [Section] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(CUSTOM_NAME, CONTENT, TAGS)
@JsonDeserialize(builder = SectionBuilder::class)
abstract class SectionYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPOJOBuilder(withPrefix = "")
private class SectionBuilder(
  @param:JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @param:JsonProperty(CUSTOM_NAME) val customName: String? = null,
  @param:JsonProperty(TAGS) val contentTags: List<String> = emptyList()
) {
  @Suppress("unused") //used for deserialization
  private fun build(): Section {
    val section = Section()
    val items = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        formatError(unnamedItemAtMessage(index + 1))
      }
      val titledStudyItem = TitledStudyItem(title)
      titledStudyItem.index = index + 1
      titledStudyItem
    }
    section.items = items
    section.customPresentableName = customName
    section.contentTags = contentTags
    return section
  }
}
