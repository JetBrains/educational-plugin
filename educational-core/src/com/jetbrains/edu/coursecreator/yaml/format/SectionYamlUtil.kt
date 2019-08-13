@file:JvmName("SectionYamlUtil")

package com.jetbrains.edu.coursecreator.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.CONTENT
import com.jetbrains.edu.coursecreator.yaml.format.YamlMixinNames.CUSTOM_NAME
import com.jetbrains.edu.coursecreator.yaml.formatError
import com.jetbrains.edu.coursecreator.yaml.unnamedItemAtMessage
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Mixin class is used to deserialize [Section] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(CUSTOM_NAME, CONTENT)
@JsonDeserialize(builder = SectionBuilder::class)
abstract class SectionYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var myCustomPresentableName: String? = null

  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var items: List<StudyItem>
}

@JsonPOJOBuilder(withPrefix = "")
private class SectionBuilder(@JsonProperty(CONTENT) val content: List<String?> = emptyList(),
                             @JsonProperty(CUSTOM_NAME) val customName: String? = null) {
  @Suppress("unused") //used for deserialization
  private fun build(): Section {
    val section = Section()
    val items = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        throw formatError(unnamedItemAtMessage(index + 1))
      }
      val titledStudyItem = TitledStudyItem(title)
      titledStudyItem.index = index + 1
      titledStudyItem
    }
    section.items = items
    section.customPresentableName = customName
    return section
  }
}
